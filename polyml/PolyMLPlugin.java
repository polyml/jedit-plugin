/*
 *  ------------------------------------------------------------------------------------
 */

package polyml;

import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EBPlugin;
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.msg.BufferUpdate;

import errorlist.DefaultErrorSource;
import errorlist.ErrorSource;
import java.util.regex.Pattern;

/**
 * Plugin class the PolyML Plugin for jedit.
 *
 * @author     Lucas Dixon
 * @created    03 November 2007
 * @version    0.2.0
 */
public class PolyMLPlugin extends EBPlugin {
	
	public static final String NAME = "PolyML Plugin";
	public static final String OPTION_PREFIX = "options.polyml.";

	public static final String PROPS_POLY_IDE_COMMAND = "options.polyml.polyide-command";
	public static final String PROPS_SHELL_COMMAND = "options.polyml.shell-command";
	public static final String PROPS_SHELL_PROMPT = "options.polyml.shell-prompt";
	public static final String PROPS_SHELL_MAX_HISTORY = "options.polyml.max-history";
	
	
	
	/** Associates Buffers to Processes that output to the buffer */
	static Map<Buffer,ShellBuffer> shells;
	static DefaultErrorSource errorSource;
	static PolyMLProcess polyMLProcess;
	
	public PolyMLPlugin() {
		super();
		shells = new Hashtable<Buffer, ShellBuffer>();
		System.err.println("PolyMLPlugin: started!");
	}
	
	public static List<String> getPolyIDECmd(){
		String s = jEdit.getProperty(PROPS_POLY_IDE_COMMAND);
		List<String> cmd = new LinkedList<String>();
		for(String s2 : s.split(" ")){ cmd.add(s2); }
		return cmd;
	}
	
	public static String getPolyIDECmdString(){
		return jEdit.getProperty(PROPS_POLY_IDE_COMMAND);
	}
	
	// called when plugin is loaded/added
	public void start() {
		System.err.println("PolyMLPlugin: start called.");
		errorSource = new DefaultErrorSource(NAME);
		DefaultErrorSource.registerErrorSource(errorSource);
		
		try {
			polyMLProcess = new PolyMLProcess(getPolyIDECmd());
		} catch (IOException e) {
			polyMLProcess = null;
			System.err.println("Failed to start PolyML: make sure the command ('" 
					+ getPolyIDECmdString() + "') is in your path.");
			//e.printStackTrace();
		}
	}
	
	// called when plugin is un-loaded/removed
	public void stop() {
		stopAllShellBuffers(); 
		DefaultErrorSource.unregisterErrorSource(errorSource);
		if(polyMLProcess != null) { polyMLProcess.closeProcess(); }
	}
	
	static public boolean restartPolyML() {
		try { 
			List<String> cmd = getPolyIDECmd();
			if(polyMLProcess == null) { 
				polyMLProcess = new PolyMLProcess(cmd);
			} else {
				polyMLProcess.restartProcessWithCommand(cmd);
			}
			return true;
		} catch (IOException e) {
			//e.printStackTrace();
			System.err.println("PolyMLPlugin: Failed to restart PolyML!");
			polyMLProcess = null;
			return false;
		}
	}
	
	/** 
	 * send buffer to ML and process contents
	 * @param b
	 */
	static public void sendBufferToPolyML(Buffer b) {
		/* for debugging
		 * ShellBuffer sb = newShellBuffer(); String ESC = "" + PolyMarkup.ESC;
		 * 
		 * System.err.println("sendBufferToPolyML: called. "); String
		 * compile_cmd = ESC + "R" + "" + ESC + "," + b.getPath() + ESC + "," +
		 * "0" + ESC + "," + b.getText(0, b.getLength()) + ESC + 'r';
		 * polyMLProcess.send(compile_cmd);
		 * 
		 * PolyMarkup m = null;
		 * 
		 * m = PolyMarkup.readPolyMarkup(polyMLProcess.reader);
		 * sb.getBufferEditor().insert(sb.getPostPromptPos(), m.toXMLString());
		 * 
		 * CompileResult r = new CompileResult(m);
		 */
		CompileResult r = null;
		r = polyMLProcess.compile(b.getPath(), b.getText(0, b.getLength()));

		errorSource.removeFileErrors(b.getPath());
		
		if(r.isBug()) {
			errorSource.addError(new DefaultErrorSource.DefaultError(
					errorSource, ErrorSource.ERROR, b.getPath(), 0,
					0, 0, "BUG: Failed to check using PolyML."));
		} else {
			if(r.status == CompileResult.STATUS_LOAD_FAILED) {
				errorSource.addError(new DefaultErrorSource.DefaultError(
						errorSource, ErrorSource.ERROR, b.getPath(), 0,
						0, 0, "Failed to load heap: '" + r.heapName + "'"));
			}
			
			for (PolyMLError e : r.errors) {
				int line = b.getLineOfOffset(e.startPos);
				int line_offet = e.startPos - b.getLineStartOffset(line);
				int end_line = b.getLineOfOffset(e.endPos);
				int end_offset = 0;
				if (end_line == line) {
					end_offset = e.endPos - b.getLineStartOffset(end_line);
				}
				
				int errorKind;
				if(e.kind == PolyMLError.KIND_FATAL){
					errorKind = ErrorSource.ERROR;
				} else {
					errorKind = ErrorSource.WARNING;
				}
				
				errorSource.addError(new DefaultErrorSource.DefaultError(
						errorSource, errorKind, b.getPath(), line,
						line_offet, end_offset, e.message));
			}
		}
	}
	
	
	static public ShellBuffer newShellBuffer() {
		Buffer b = jEdit.newFile(jEdit.getFirstView());
		ShellBuffer s;
		try {
			s = new ShellBuffer(new BufferEditor(b));
			shells.put(b, s);
			return s;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(e.toString());
			return null;
		}
	}
	
	
	static public void processShellBufferToEOF(Buffer b) {
		try {
			System.err.println("called processShellBufferToEOF");
			ShellBuffer s = shells.get(b);
			if(s == null) {
				System.err.println("Not a ShellBuffer!");
			} else {
				s.sendBufferTextToEOF();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(e.toString());
		}
	}
	
	
	/* start and restart are the same: they restart shell in the current buffer */
	static public void startShellInBuffer(Buffer b) {
		restartShellInBuffer(b);
	}
	
	
	static public void prevCommand(Buffer b) {
		ShellBuffer sb = shells.get(b);
		if(sb != null){
			sb.prevCommand();
		}
	}
	
	static public void nextCommand(Buffer b) {
		ShellBuffer sb = shells.get(b);
		if(sb != null){
			sb.nextCommand();
		}
	}
	
	
	static public void restartShellInBuffer(Buffer b) {
		ShellBuffer sb = shells.get(b);
		if(sb != null){
			try {
				sb.restartProcess();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println(e.toString());
			}
		} else {
			try {
				shells.put(b, new ShellBuffer(new BufferEditor(b)));
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println(e.toString());
			}
		}
	}
	
	static public void stopShellInBuffer(Buffer b) {
		ShellBuffer sb = shells.get(b);
		if(sb != null){
			sb.stopProcess();
		}
	}
	
	static public void stopAllShellBuffers() {
		for(ShellBuffer sb : shells.values()) {
			sb.stopProcess();
		}
	}
	
	/** handle buffer closing events to close associated process. */
	public void handleMessage(EBMessage msg){
		if(msg instanceof BufferUpdate) {
			if(((BufferUpdate)msg).getWhat() == BufferUpdate.CLOSING) {
				Buffer b = ((BufferUpdate)msg).getBuffer();
				ShellBuffer s = shells.get(b);
				if(s != null){
					s.stopProcess();
					shells.remove(b);
				}
			} else if(((BufferUpdate)msg).getWhat() == BufferUpdate.SAVED) {
				Buffer b = ((BufferUpdate)msg).getBuffer();
				Mode m = b.getMode();
				if(m.getName() == "PolyML Mode") {
					System.err.println("PolyML Mode Buffer Saved!");
				}
			}
		}
	}
}
