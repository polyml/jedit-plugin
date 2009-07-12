package polyml;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.zip.ZipFile;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.TextArea;

import pushstream.CopyPushStream;
import pushstream.InputStreamThread;
import pushstream.TimelyCharToStringStream;
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

	List<String> polyProcessCmd; // polyML command
	Process process; // the polyML Process
	DataOutputStream writer; // basic writing to polyML
	DataInputStream reader; // basic reading from polyML

	InputStreamThread polyListener; // thread that listens to polyML
	PolyMarkupPushStream errorPushStream; // listens to PolyML and deals with markup.
	
	DefaultErrorSource errorSource; // jEdit error list
	
	File ideHeapFile;
	int msgID; // counter for messages sent to poly

	// all known parse query statuses, and last compile request ID
	CompileInfos compileInfos; 
	
	Queue<CompileRequest> pendingCompiles;
	
	// running
	volatile boolean mRunningQ;

	// -
	public PolyMLProcess(List<String> cmd, DefaultErrorSource err) throws IOException {
		super();
		msgID = 0;
		mRunningQ = false;
		errorSource = err;
		ideHeapFile = null;
		process = null;
		writer = null;
		reader = null;
		polyListener = null;
		errorPushStream = null;
		compileInfos = new CompileInfos();
		//pendingCompiles = new LinkedList<CompileRequest>();
		polyProcessCmd = cmd;
	}

	// -
	public PolyMLProcess(DefaultErrorSource err) throws IOException {
		super();
		msgID = 0;
		mRunningQ = false;
		errorSource = err;
		ideHeapFile = null;
		process = null;
		writer = null;
		reader = null;
		polyListener = null;
		errorPushStream = null;
		compileInfos = new CompileInfos();
		//pendingCompiles = new LinkedList<CompileRequest>();
		polyProcessCmd = new LinkedList<String>();
		polyProcessCmd.add("poly");
		//polyProcessCmd.add("--ideprotocol --with-markup");
	}


	public void setCmd(List<String> polyIDECmd) {
		polyProcessCmd = polyIDECmd;
	}

	
	public boolean checkAndCreatePolyIDE() {
		String settingsDir = jEdit.getSettingsDirectory();
		if (settingsDir != null) {
			ideHeapFile = new File(settingsDir + File.separator + "ide.polysave");
			long zipTime = jEdit.getPlugin("polyml.PolyMLPlugin").getPluginJAR().getFile().lastModified();

			if ((! ideHeapFile.exists()) || (ideHeapFile.lastModified() < zipTime)) {
				//System.err.println("compiling IDE ML Code. ");
				try {
					File ideSrc = File.createTempFile("poly_ide", ".sml");
					ZipFile zip = jEdit.getPlugin("polyml.PolyMLPlugin").getPluginJAR().getZipFile();
					InputStream in = zip.getInputStream(zip.getEntry("ide.sml"));
					OutputStream out = new FileOutputStream(ideSrc);

					// Transfer bytes from in to out
					byte[] buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
					in.close();
					out.close();

					syncCompile(new CompileRequest("", ideSrc.getPath(), 
							"use \"" + ideSrc.getPath() + "\"; \n" 
							+ "PolyML.SaveState.saveState \"" + ideHeapFile + "\"; \n"));

					// We tell the polyMLProcess that we now have a valid heap
					// IMPROVE: deal with compile result? 
					if(! ideHeapFile.exists()){ 
						// r.requestID.equals(PolyMLPlugin.IDEPolyHeapFile) && r.isSuccess())
						System.err.println("Failed to make IDE heap.");
						ideHeapFile = null;
						return false;
					}
					return true;
				} catch (IOException e) {
					System.err.println("restartPolyML: failed to copy ide.sml");
					e.printStackTrace();
					return false;
				}
			} else {
				return true;
			}
		} else {
			ideHeapFile = null;
			System.err.println("PolyML needs to compile IDE ML code in " +
					"the settings directory, but no settings directory if being used. " +
			"You will not be able to use the IDE features of the polyml Plugin ");
			return false;
		}
	}
	
	
	
	/* 
	public synchronized CompileRequest pollPendingCompile() {
		return pendingCompiles.poll();
	}
	
	public synchronized void queueCompileRequest(CompileRequest c) {
		pendingCompiles.add(c);
	}
	
	public synchronized boolean isPendingCompile() {
		return (!pendingCompiles.isEmpty());
	}
	
	public synchronized void run() {
		mRunningQ = true;		
		while(mRunningQ){
			CompileRequest c = pollPendingCompile();
			if(c == null){
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				synchronized(c){
					sendCompileRequest(c);
				}
				c.notify(); // in case a thread was waiting on this to be finished.
			}
		}			
	}
	*/
	
	/**
	 * restart the process using the given command
	 * 
	 * @param cmd
	 * @throws IOException
	 */
	public synchronized void restartProcess()
			throws IOException {
		closeProcess();

		//System.err.println("restartProcessFromCmd:" + polyProcessCmd);

		ProcessBuilder pb = new ProcessBuilder(polyProcessCmd);
		if(pb == null) {
			System.err.println("PolyMLProcess:" + "Failed to start process: "
					+ polyProcessCmd);
			return;
		}
		pb.redirectErrorStream(true);
		try {
			System.err.println("PolyMLProcess:" + "start called: " + polyProcessCmd);
			process = pb.start();
			
			reader = new DataInputStream(process.getInputStream());
			writer = new DataOutputStream(process.getOutputStream());

			Object helloLock = new Object();
			errorPushStream = new PolyMarkupPushStream(errorSource, compileInfos, helloLock);

			// setup and start listening thread.
			polyListener = new InputStreamThread(reader,
					new CopyPushStream<Character>(new TimelyCharToStringStream(
							new PushStringToDebugBuffer(), 100),
							new PolyMarkup(errorPushStream)));
			
			//polyListener = new InputStreamThread(reader, new PolyMarkup(errorPushStream));
			
			polyListener.start();
			
			synchronized(helloLock) {
				// get poly to start in IDE protocol mode
				sendToPoly("val _ = (print \"Starting IDE protocol...\"; PolyML.IDEInterface.runIDEProtocol());");
				try {
					long delay = 5000;
					long t = (new Date()).getTime() + delay;
					System.err.println("restartProcess: wiating for hello...");
					helloLock.wait(5000);
					if(t <= (new Date()).getTime()) {
						System.err.println("restartProcess: ran out of time waiting for ML to start, pretending it worked...");
						closeProcess();
					} else {
						mRunningQ = true; // got hello back, so we are running
						System.err.println("restartProcess: got hello!");			
					}
				} catch (InterruptedException e) {
					closeProcess();
					System.err.println("restartProcess: got interupted when waiting hello response.");
					e.printStackTrace();
				}
			}
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
		mRunningQ = false;
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
			msgID = 0;
			compileInfos.deleteAll();
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

	public synchronized void sendPolyQuery(EditPane p, char c, String more) {
		String lastParseID = compileInfos.parseIDOfBuffer(p.getBuffer());

		if (lastParseID == null) {
			System.err.println("PolyMLProcess:makePolyQuery: no last parse ID");
			return;
		}
		
		String requestid = Integer.toString(msgID++);
		String cmd = ESC + Character.toString(c) + requestid 
				+ ESC_COMMA + lastParseID
				+ ESC_COMMA + getOffsetsString(p);
		
		if(more != null) {
			cmd += more;
		}
		
		cmd += ESC + Character.toString(Character.toLowerCase(c));
		
		//System.err.println("sendPolyQuery: " + PolyMarkup.explicitEscapes(cmd));
		sendToPoly(cmd);
	}
	
	/**
	 * get properties, type etc, of current selection/cursor in the text area
	 */
	public void sendGetProperies(EditPane p) {
		sendPolyQuery(p, PolyMarkup.KIND_PROPERTIES, null);
	}

	public void sendGetType(EditPane p) {
		sendPolyQuery(p, PolyMarkup.KIND_TYPE_INFO, null);
	}

	
	public void sendMoveToParent(EditPane p) {
		sendPolyQuery(p, PolyMarkup.KIND_MOVE, ESC_COMMA + "U");
	}
	
	public void sendMoveToFirstChild(EditPane p) {
		sendPolyQuery(p, PolyMarkup.KIND_MOVE, ESC_COMMA + "C");
	}
	
	public void sendMoveToNext(EditPane p) {
		sendPolyQuery(p, PolyMarkup.KIND_MOVE, ESC_COMMA + "N");
	}
	
	public void sendMoveToPrevious(EditPane p) {
		sendPolyQuery(p, PolyMarkup.KIND_MOVE, ESC_COMMA + "P");
	}
	

	public void sendLocationOfParentStructure(EditPane p) {
		sendPolyQuery(p, PolyMarkup.KIND_LOC, ESC_COMMA + "S");
	}
	public void sendLocationDeclared(EditPane p) {
		sendPolyQuery(p, PolyMarkup.KIND_LOC, ESC_COMMA + "I");
	}
	public void sendLocationOpened(EditPane p) {
		sendPolyQuery(p, PolyMarkup.KIND_LOC, ESC_COMMA + "J");
	}
	
	/**
	 */
	public synchronized void sendCancelCompile(String requestID) {
		String compile_cmd = ESC + "K" + requestID + ESC + "k";
		sendToPoly(compile_cmd);
	}
	
	public synchronized void sendCancelLastCompile() {
		String lastRequestID = compileInfos.getLastCompileRequestID();
		if(lastRequestID != null) {
			sendCancelCompile(lastRequestID);
		}
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
	synchronized void sendCompileRequest(String requestid, CompileRequest compileRequest) {		
		
		String lastRequestID = compileInfos.getLastCompileRequestID();
		if(lastRequestID != null) {
			//System.err.println("sending cancel compile.");
			sendCancelCompile(lastRequestID);
		}
		
		compileInfos.compilingRequest(compileRequest, requestid);
		
		lastRequestID = requestid;
		String compile_cmd = ESC_START + requestid // request id
			+ ESC_COMMA + compileRequest.fileName // filename for err msgs
			+ ESC_COMMA + "0" // start offset
			+ ESC_COMMA + compileRequest.prelude.length() // length of prelude 
			+ ESC_COMMA + compileRequest.src.length() // length of src
			+ ESC_COMMA + compileRequest.prelude //prelude
			+ ESC_COMMA + compileRequest.src // src
			+ ESC_END;

		//System.err.println("CompileCmd: '" + compile_cmd + "';");
		sendToPoly(compile_cmd);
		//System.err.println("sent.");
	}
	
	
	// IMPROVE: put request ID directly in message? 
	public synchronized void sendCompileRequest(CompileRequest compileRequest) {
		String requestid = Integer.toString(msgID++);
		sendCompileRequest(requestid, compileRequest);
	}
	
	
	public synchronized void syncCompile(CompileRequest compileRequest) {
		synchronized(compileRequest) {
			sendCompileRequest(compileRequest);
			try {
				compileRequest.wait();
				//compileRequest.wait(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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

	/**
	 * compile a buffer
	 * 
	 * @param b
	 */
	public void sendCompileBuffer(Buffer b, EditPane e) {
		String src = b.getText(0, b.getLength());
		
		// change directory to project directory, if there is a project
		// directory.
		String preSetupString = new String();
		String projectPath = ProjectTools.searchForProjectDir(b);
	
		// load heap if there is one to be loaded
		String heap = ProjectTools.searchForBufferHeapFile(b);
		boolean haveIDEHeap = checkAndCreatePolyIDE();
		// FIXME: tell user when the heap is older than the IDE heap 
		// - they may have the wrong use function		
		if (heap != null) {
			preSetupString += "let val p = ! PolyML.IDEInterface.parseTree in \n" +
					"(PolyML.SaveState.loadState \"" + heap + "\"; \n" +
					" PolyML.IDEInterface.parseTree := p) end; \n";
			preSetupString += "IDE.setProjectDir \"" + projectPath + "\";\n";
		} else if(haveIDEHeap) {  // else try default heap
			preSetupString += "PolyML.SaveState.loadState \"" + ideHeapFile + "\";\n";
			preSetupString += "IDE.setProjectDir \"" + projectPath + "\";\n";
		} else {
			// no heap means that we are not setup for IDE use;
			errorSource.removeFileErrors(b.getPath());
			errorSource.addError(new DefaultErrorSource.DefaultError(errorSource,
					ErrorSource.ERROR, b.getPath(), 0, 0, 0, "No IDE heap file :( "));
			return;
		}
		
		// if set to run from file's dir - change to it. 
		if(Boolean.parseBoolean(jEdit.getProperty(PolyMLPlugin.PROPS_RUN_FROM_FROM_FILE_DIR))) {
			preSetupString += "OS.FileSys.chDir \"" + b.getDirectory() + "\";\n";
		}
		
		//System.err.println("\nML Prelude: \n " + preSetupString + "\n;\n");
		
		sendCompileRequest(new CompileRequest(preSetupString, b.getPath(), src));
	}

	/**
	 * Send string to PolyML
	 * 
	 * @param command
	 */
	public synchronized void sendToPoly(String command) {
		
		//System.err.println("makePolyQuery: " + PolyMarkup.explicitEscapes(command));
		PolyMLPlugin.debugMessage("\n--- START OF SENT ---\n" + command + "\n--- END OF SENT ---\n");
		
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

}
