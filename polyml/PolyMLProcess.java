package polyml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.TextArea;

import polyml.ShellBuffer.PushStringToBuffer;
import pushstream.CopyPushStream;
import pushstream.InputStreamThread;
import pushstream.PushStream;
import pushstream.TimelyCharToStringStream;
import pushstream.ReaderThread;

import errorlist.DefaultErrorSource;
import errorlist.ErrorSource;

public class PolyMLProcess {
	static final char ESC_CHAR = 0x1b;
	static final char EOT = 0x04;
	static final String ESC = Character.toString(ESC_CHAR);
	static final String ESC_COMMA = ESC + ",";
	static final String ESC_START = ESC + "R";
	static final String ESC_END = ESC + "r";
	static final String POLY_SAVE_DIR = ".polysave";

	Process process;

	DataOutputStream writer;
	DataInputStream reader;

	InputStreamThread polyListener;
	DefaultErrorSource errorSource;
	PolyMarkupPushStream errorPushStream;
	
	List<String> polyProcessCmd;

	// String lastParseID;
	// Buffer lastParsedBuffer;
	ParseInfo parseInfo;

	// -
	public PolyMLProcess(List<String> cmd, DefaultErrorSource err)
			throws IOException {
		super();
		errorSource = err;
		process = null;
		writer = null;
		reader = null;
		polyListener = null;
		errorPushStream = null;

		parseInfo = new ParseInfo();
		
		polyProcessCmd = cmd;
		// lastParseID = null;
		// lastParsedBuffer = null;
		restartProcess();
	}

	// -
	public PolyMLProcess(DefaultErrorSource err) throws IOException {
		super();
		errorSource = err;
		process = null;
		writer = null;
		reader = null;
		polyListener = null;
		errorPushStream = null;
		parseInfo = new ParseInfo();
		polyProcessCmd = new LinkedList<String>();
		polyProcessCmd.add("poly");
		polyProcessCmd.add("--ideprotocol --with-markup");
		restartProcess();
	}


	public void setCmd(List<String> polyIDECmd) {
		polyProcessCmd = polyIDECmd;
	}

	
	/**
	 * sub class that can be called by other threads to add a string to the
	 * debug buffer
	 * 
	 * @author ldixon
	 */
	public class PushStringToDebugBuffer implements PushStream<String> {

		public PushStringToDebugBuffer() {
		}

		public void add(String s) {
			PolyMLPlugin.debugMessage(s);
		}

		public void add(String s, boolean isMore) {
			add(s);
		}

		public void close() {
			PolyMLPlugin.debugMessage("<EOF>");
		}

	}

	/**
	 * restart the process using the given command
	 * 
	 * @param cmd
	 * @throws IOException
	 */
	public synchronized void restartProcess()
			throws IOException {
		closeProcess();

		System.err.println("restartProcessFromCmd:" + polyProcessCmd);

		ProcessBuilder pb = new ProcessBuilder(polyProcessCmd);
		pb.redirectErrorStream(true);
		try {
			System.err.println("PolyMLProcess:" + "start called: " + polyProcessCmd);
			process = pb.start();

			reader = new DataInputStream(process.getInputStream());
			writer = new DataOutputStream(process.getOutputStream());

			errorPushStream = new PolyMarkupPushStream(errorSource, parseInfo);

			// setup and start listening thread.
			polyListener = new InputStreamThread(reader,
					new CopyPushStream<Character>(new TimelyCharToStringStream(
							new PushStringToDebugBuffer(), 100),
							new PolyMarkup(errorPushStream)));
			polyListener.start();

		} catch (IOException e) {
			System.err.println("PolyMLProcess:" + "Failed to start process: "
					+ polyProcessCmd);
			process = null;
			throw e;
		}
	}

	/**
	 * stop the ML process
	 */
	public synchronized void closeProcess() {
		if (process != null) {
			try {
				writer.close();
				reader.close();
			} catch (IOException e) {
				System.err.println("error in PolyMLProcess:closeProcess");
				e.printStackTrace();
			}
			polyListener.pleaseStop();

			// int i = process.exitValue();
			// if(i != 0) {
			// System.err.println(
			// "PolyMLProcess:destorying poly-process: exit value: " + i);
			// process.destroy();
			// }
			parseInfo.deleteAll();
			process = null;
			writer = null;
			reader = null;
			polyListener = null;
			errorPushStream = null;
		}
	}

	/*
	 * internal function to get offsets of interest from a text-area
	 */
	int[] getOffset(EditPane p) {
		int[] startend = new int[2];
		TextArea t = p.getTextArea();
		if (t.getSelectionCount() > 0) {
			Selection s = t.getSelection(0);
			startend[0] = s.getStart();
			startend[1] = s.getEnd();
		} else {
			startend[0] = startend[1] = t.getCaretPosition();
		}
		return startend;
	}

	String getOffsetsString(EditPane t) {
		int[] offsets = getOffset(t);
		return Integer.toString(offsets[0]) + ESC_COMMA + Integer.toString(offsets[1]);
	}

	
	public void makePolyQuery(EditPane p, char c) {
		String lastParseID = parseInfo.getFromBuffer(p.getBuffer()).lastParseID;

		if (lastParseID == null) {
			System.err.println("PolyMLProcess:getProperiesAtCursor: no last parse ID");
			return;
		}
		
		String requestid = "";
		String cmd = ESC + Character.toString(c) + requestid + ESC_COMMA + lastParseID
				+ ESC_COMMA + getOffsetsString(p) + ESC
				+ Character.toString(Character.toLowerCase(c));
		
		System.err.println("makePolyQuery: " + PolyMarkup.explicitEscapes(cmd));
		sendToPoly(cmd);
	}
	
	/**
	 * get properties, type etc, of current selection/cursor in the text area
	 */
	public void sendGetProperies(EditPane p) {
		makePolyQuery(p, PolyMarkup.INKIND_PROPERTIES);
	}

	public void sendGetType(EditPane p) {
		makePolyQuery(p, PolyMarkup.INKIND_TYPE_INFO);
	}

	
	public void sendMoveToParent(EditPane p) {
		makePolyQuery(p, PolyMarkup.INKIND_MOVE_TO_PARENT);
	}
	
	public void sendMoveToFirstChild(EditPane p) {
		makePolyQuery(p, PolyMarkup.INKIND_MOVE_TO_FIRST_CHILD);
	}
	
	public void sendMoveToNext(EditPane p) {
		makePolyQuery(p, PolyMarkup.INKIND_MOVE_TO_NEXT);
	}
	
	public void sendMoveToPrevious(EditPane p) {
		makePolyQuery(p, PolyMarkup.INKIND_MOVE_TO_PREVIOUS);
	}
	

	public void sendLocationOfParentStructure(EditPane p) {
		makePolyQuery(p, PolyMarkup.INKIND_LOC_OF_PARENT_STRUCT);
	}
	public void sendLocationDeclared(EditPane p) {
		makePolyQuery(p, PolyMarkup.INKIND_LOC_DECLARED);
	}
	public void sendLocationOpened(EditPane p) {
		makePolyQuery(p, PolyMarkup.INKIND_LOC_WHERE_OPENED);
	}
	
	
	
	
	/**
	 */
	public void cancelCompile() {
		String requestid = "a";
		String compile_cmd = ESC + "K" + requestid + ESC + "k";
		sendToPoly(compile_cmd);
	}

	/**
	 * WARNING: this does not set the parseInfo - this needs to be done, and is
	 * done by the public version that works on buffers.
	 * 
	 * @param heap
	 * @param srcFileName
	 * @param startPos
	 * @param src
	 */
	void compile(String prelude, String srcFileName, String src) {

		String requestid = srcFileName;
		String compile_cmd = ESC_START + requestid + ESC_COMMA + srcFileName
				+ ESC_COMMA + "0" + ESC_COMMA + prelude.length() + ESC_COMMA
				+ src.length() + ESC_COMMA + prelude + ESC_COMMA + src
				+ ESC_END;

		// System.err.println("CompileCmd: '" + compile_cmd + "';");

		sendToPoly(compile_cmd);
	}

	/**
	 * Compile the string
	 * 
	 * @param srcFileName
	 * @param src
	 */
	//void compile(String srcFileName, String src) {
	//	compile(srcFileName, srcFileName, src);
	//}

	public class PolyMLSaveDir implements FileFilter {
		@Override
		public boolean accept(File f) {
			File savedir = new File(f.getParent() + File.separator
					+ POLY_SAVE_DIR);
			// System.err.println("PolyMLSaveDir: \n  " + f + "\n  " + savedir);
			return (f.compareTo(savedir) == 0);
		}
	}

	public File searchForProjectDir(Buffer b) {
		File p = new File(b.getDirectory()).getAbsoluteFile();
		File projectDir = null;

		while (projectDir == null && p != null) {
			// System.err.println("looking for .polysave in:  " + p);
			File[] polysavedir = p.listFiles(new PolyMLSaveDir());
			if (polysavedir.length != 0) {
				// System.err.println("Found .polysave in:  " + p);
				projectDir = new File(p.getAbsolutePath() + File.separator
						+ POLY_SAVE_DIR);
				// System.err.println("Looking for:  " + heapFile);
				if (!projectDir.exists()) {
					projectDir = null;
				}
			}
			p = p.getParentFile();
		}

		return projectDir;
	}

	public String searchForBufferHeapFile(Buffer b) {
		String s = b.getPath();
		File p = new File(b.getDirectory()).getAbsoluteFile();
		String heap = null;
		boolean noProject = true;

		while (noProject && p != null) {
			// System.err.println("looking for .polysave in:  " + p);
			File[] polysavedir = p.listFiles(new PolyMLSaveDir());
			if (polysavedir.length != 0) {
				// System.err.println("Found .polysave in:  " + p);
				File heapFile = new File(p.getAbsolutePath() + File.separator
						+ POLY_SAVE_DIR + File.separator
						+ s.substring(p.getPath().length()) + ".save");
				// System.err.println("Looking for:  " + heapFile);
				if (heapFile.exists()) {
					heap = heapFile.getAbsolutePath();
					noProject = false;
				}
			}
			p = p.getParentFile();
		}

		return heap;
	}

	/**
	 * compile a buffer
	 * 
	 * @param b
	 */
	public void compileBuffer(Buffer b, EditPane e) {
		String p = b.getPath();
		String src = b.getText(0, b.getLength());

		parseInfo.parsingBuffer(b, e);
		
		// change directory to project directory, if there is a project
		// directory.
		String preSetupString = new String();
		File projectDir = searchForProjectDir(b);
		if (projectDir != null) {
			p = projectDir.getParent();
			if (p != null) {
				preSetupString = "OS.FileSys.chDir \"" + p + "\"; ";
			}
		} else {
			File bFile = new File(p);
			p = bFile.getParent();
			if (p != null) {
				preSetupString = "OS.FileSys.chDir \"" + p + "\"; ";
			}
		}
		System.err.println("PreStup String: " + preSetupString);

		// load heap if there is one to be loaded.
		String heap = searchForBufferHeapFile(b);
		if (heap != null) {
			preSetupString += "PolyML.SaveState.loadState \"" + heap + "\";";
		}

		errorSource.addError(new DefaultErrorSource.DefaultError(errorSource,
				ErrorSource.WARNING, b.getPath(), 0, 0, 0, "Compiling ML ... "));

		compile(preSetupString, b.getPath(), src);
	}

	/**
	 * Send string to PolyML
	 * 
	 * @param command
	 */
	public synchronized void sendToPoly(String command) {
		
		//System.err.println("makePolyQuery: " + PolyMarkup.explicitEscapes(command));
		
		// if process variable is not null, we should be running, check this
		if (process != null) {
			try {
				int i = process.exitValue();
				System.err.println("PolyML unexpectidly quit with value:" + i + "; trying to restart" );
				restartProcess();
			} catch (IllegalThreadStateException e) {
				// do nothing; process is still running fine
			} catch (IOException e) {
				System.err.println("PolyML restart failed." );
				e.printStackTrace();
			}
		}

		// make sure output and process are not null: we are running correctly
		if (writer != null && process != null) {
			try {
				writer.writeBytes(command);
				writer.flush();
			} catch (IOException e) {
				System.err.println("Exit value from polyml: "
						+ process.exitValue());
				e.printStackTrace();
			}
		} else {
			System.err.println("PolyProcess: writer is null! ");
		}
	}

	// 0x1b

	// ESC 'R' loadfile ESC ',' sourcename ESC ',' startposition ESC ',' source
	// ESC 'r'
}
