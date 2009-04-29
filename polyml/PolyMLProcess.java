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
import java.util.LinkedList;
import java.util.List;

import org.gjt.sp.jedit.Buffer;

public class PolyMLProcess {
	static final char ESC = 0x1b;
	static final char EOT = 0x04;
	static final String POLY_SAVE_DIR = ".polysave";
		
	Process process;
	BufferedWriter writer;
	BufferedReader reader;
	
	//
	public PolyMLProcess(List<String> cmd) throws IOException {
		super();
		startProcessFromComannd(cmd);
	}

	public PolyMLProcess() throws IOException {
		super();
		List<String> cmd = new LinkedList<String>();
		cmd.add("poly");
		cmd.add("--ideprotocol");
		startProcessFromComannd(cmd);
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
			process.destroy();
			process = null;
		}
	}
	
	/**
	 * 
	 * @param heap
	 * @param srcFileName
	 * @param startPos
	 * @param src
	 * @return
	 * @throws MarkupException
	 * @throws IOException
	 */
	public synchronized CompileResult compile(String heap, String srcFileName, int startPos, String src) {
		String loadHeap;
		if(heap != null) { loadHeap = heap; } else { loadHeap = ""; }
		
		String compile_cmd = ESC + "R" + loadHeap + ESC + "," + srcFileName + ESC + 
			"," + startPos + ESC + "," + src + ESC + 'r';
		sendToPoly(compile_cmd);
		PolyMarkup m = null;
		try {
			m = PolyMarkup.readPolyMarkup(reader);
		} catch (MarkupException e) {
			e.printMarkupException();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new CompileResult(heap, m);
	}
	
	/**
	 * Compile the string
	 * @param srcFileName
	 * @param src
	 * @return
	 * @throws MarkupException
	 * @throws IOException
	 */
	public CompileResult compile(String srcFileName, String src) {
		return compile(null, srcFileName, 0, src);
	}
	
	public class PolyMLSaveDir implements FileFilter {
		@Override
		
		
		public boolean accept(File f) {
			File savedir = new File(f.getParent() + File.separator + POLY_SAVE_DIR);
			//System.err.println("PolyMLSaveDir: \n  " + f + "\n  " + savedir);
			return (f.compareTo(savedir) == 0);
		}
	}
	
	
	public CompileResult compileBuffer(Buffer b) {
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
		
		return compile(heap, s, 0,  b.getText(0, b.getLength()));
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
