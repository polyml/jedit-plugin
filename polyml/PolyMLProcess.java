package polyml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import org.gjt.sp.jedit.Buffer;

import polyml.ShellBuffer.PushStringToBuffer;
import pushstream.CopyPushStream;
import pushstream.PushStream;
import pushstream.TimelyCharToStringStream;
import pushstream.ReaderThread;

import errorlist.DefaultErrorSource;

public class PolyMLProcess {
	static final char ESC = 0x1b;
	static final char EOT = 0x04;
	static final String POLY_SAVE_DIR = ".polysave";
	
	Process process;
	BufferedWriter writer;
	BufferedReader reader;
	ReaderThread polyListener; 
	DefaultErrorSource errorSource;
	PolyMarkupPushStream errorPushStream;
	
	//
	public PolyMLProcess(List<String> cmd, DefaultErrorSource err) throws IOException {
		super();
		errorSource = err;
		startProcessFromComannd(cmd);
	}

	public PolyMLProcess(DefaultErrorSource err) throws IOException {
		super();
		errorSource = err;
		List<String> cmd = new LinkedList<String>();
		cmd.add("poly");
		cmd.add("--ideprotocol");
		startProcessFromComannd(cmd);
	}
	

	

	public class PushStringToDebugBuffer implements PushStream<String> {

		public PushStringToDebugBuffer() { }
		
		public void add(String s) { 
			PolyMLPlugin.debugMessage(s);
		}

		public void add(String s, boolean isMore) { add(s); }

		public void close() {
			PolyMLPlugin.debugMessage("<EOF>");
		}
		
	}
	
	
	public synchronized void startProcessFromComannd(List<String> cmd) throws IOException {
		
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(true);
		try {
			process = pb.start();
		} catch (IOException e) {
			System.err.println("PolyMLProcess:" + "Failed to start process: " + cmd);
			process = null;
			throw e;
		}
		reader = new BufferedReader(new InputStreamReader(process
				.getInputStream()));
		
	    errorPushStream = new PolyMarkupPushStream(errorSource);
		
		polyListener = 
			new ReaderThread(reader, 
				new CopyPushStream<Character>(
						new TimelyCharToStringStream(new PushStringToDebugBuffer(), 100),
						new PolyMarkup(errorPushStream)
				)
			);
		polyListener.start();
		writer = new BufferedWriter(new OutputStreamWriter(process
				.getOutputStream()));
	}
	
	public void restartProcessWithCommand(List<String> cmd) throws IOException {
		closeProcess();
		startProcessFromComannd(cmd);
	}
	
	public synchronized void closeProcess() {
		if(process != null) {
			sendToPoly("" + EOT);
			polyListener.pleaseStop();
			process.destroy();
			process = null;
		}
	}
	
	/**
	 * @param heap
	 * @param srcFileName
	 * @param startPos
	 * @param src
	 */
	public void compile(String heap, String srcFileName, 
			int startPos, String src) {
		String loadHeap;
		if(heap != null) { loadHeap = heap; } else { loadHeap = ""; }

		String compile_cmd = ESC + "R" + loadHeap + ESC + "," + srcFileName + ESC + 
			"," + startPos + ESC + "," + src + ESC + 'r';
		
		sendToPoly(compile_cmd);
	}
	
	/**
	 * Compile the string
	 * @param srcFileName
	 * @param src
	 */
	public void compile(String srcFileName, String src) {
		compile(null, srcFileName, 0, src);
	}
	
	public class PolyMLSaveDir implements FileFilter {
		@Override
		
		public boolean accept(File f) {
			File savedir = new File(f.getParent() + File.separator + POLY_SAVE_DIR);
			//System.err.println("PolyMLSaveDir: \n  " + f + "\n  " + savedir);
			return (f.compareTo(savedir) == 0);
		}
	}
	
	
	public File searchForProjectDir(Buffer b){
		File p = new File(b.getDirectory()).getAbsoluteFile();
		File projectDir = null;
		
		while(projectDir == null && p != null) {
			//System.err.println("looking for .polysave in:  " + p);
			File[] polysavedir = p.listFiles(new PolyMLSaveDir());
			if(polysavedir.length != 0) {
				//System.err.println("Found .polysave in:  " + p);
				 projectDir = new File(p.getAbsolutePath() + File.separator + POLY_SAVE_DIR);
				//System.err.println("Looking for:  " + heapFile);
				if(! projectDir.exists()){
					projectDir = null;
				}
			}
			p = p.getParentFile();
		}
		
		return projectDir;
	}
	
	
	public String searchForBufferHeapFile(Buffer b){
		String s = b.getPath();
		File p = new File(b.getDirectory()).getAbsoluteFile();
		String heap = null;
		boolean noProject = true;
		
		while(noProject && p != null) {
			//System.err.println("looking for .polysave in:  " + p);
			File[] polysavedir = p.listFiles(new PolyMLSaveDir());
			if(polysavedir.length != 0) {
				//System.err.println("Found .polysave in:  " + p);
				File heapFile = 
					new File(p.getAbsolutePath() + File.separator + POLY_SAVE_DIR 
						+ File.separator + s.substring(p.getPath().length()) + ".save");
				//System.err.println("Looking for:  " + heapFile);
				if(heapFile.exists()){
					heap = heapFile.getAbsolutePath();
					noProject = false;
				}
			}
			p = p.getParentFile();
		}
		
		return heap;
	}
	
	public void compileBuffer(Buffer b) {
		String p = b.getPath();
		String src = b.getText(0, b.getLength());
		
		String preSetupString = new String();
		File projectDir = searchForProjectDir(b);
		if(projectDir != null){
			p = projectDir.getParent();
			if(p != null) {
				preSetupString  = "OS.FileSys.chDir \"" + p + "\"; ";
			}
		} else {
			File bFile = new File(p);
			p = bFile.getParent();
			if(p != null) {
				preSetupString  = "OS.FileSys.chDir \"" + p + "\"; ";
			}
		}
		System.err.println("PreStup String: " + preSetupString);
		

		String heap = searchForBufferHeapFile(b);
		errorPushStream.setCompileInfo(heap, b);
		
		compile(heap, p, 0 - preSetupString.length(), preSetupString + src);
	}
	
	public synchronized void sendToPoly(String command) {
		if(writer != null){
			try {
				writer.write(command);
				writer.newLine(); // CHECK: is this ok? 
				writer.flush();
			} catch (IOException e) {
				System.err.println("Exit value from polyml: "
						+ process.exitValue());
				e.printStackTrace();
			}
		}
	}

	
	//0x1b
	
	//ESC 'R' loadfile ESC ',' sourcename ESC ',' startposition ESC ',' source ESC 'r'
}
