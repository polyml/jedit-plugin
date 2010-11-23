/*
 *  ------------------------------------------------------------------------------------
 */

package polyml;

import java.io.IOException;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EBPlugin;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.textarea.TextArea;

/**
 * Plugin class the PolyML Plugin for jedit.
 * 
 * @author Lucas Dixon
 * @created 03 November 2007
 * @version 0.2.0
 */
public class PolyMLPlugin extends EBPlugin {

	public static final String NAME = "PolyML";
	public static final String OPTION_PREFIX = "options.polyml.";

	public static final String PROPS_POLY_IDE_COMMAND = "options.polyml.polyide_command";
	public static final String PROPS_SHELL_COMMAND = "options.polyml.shell_command";
	public static final String PROPS_SHELL_PROMPT = "options.polyml.shell_prompt";
	public static final String PROPS_SHELL_MAX_HISTORY = "options.polyml.max_history";
	public static final String PROPS_COPY_OUTPUT_TO_DEBUG_BUFFER = "options.polyml.copy_output_to_debug_buffer";
	public static final String PROPS_RUN_FROM_FROM_FILE_DIR = "options.polyml.run_from_file_dir";
	public static final String PROPS_STATE_OUTPUT_CSS_FILE = "options.polyml.state_output_css_file";
	public static final String PROPS_STATE_DOC_EDITABLE = "options.polyml.state_doc_editable";
	public static final String PROPS_SCROLL_ON_OUTPUT = "options.polyml.scroll_on_output";
	public static final String PROPS_BUFFER_CHANGE = "options.polyml.buffer_change_behaviour";
	public static final String PROPS_BUFFER_CHANGE_CLEAR = "Clear and write new status";
	public static final String PROPS_BUFFER_CHANGE_APPEND = "Append new status";
	public static final String PROPS_BUFFER_CHANGE_NOWT = "Do nothing";
	public static final String[] PROPS_BUFFER_CHANGE_OPTIONS = new String[] {
		PROPS_BUFFER_CHANGE_CLEAR, PROPS_BUFFER_CHANGE_APPEND, PROPS_BUFFER_CHANGE_NOWT
	};
	
	/** Associates Buffers to Processes that output to the buffer */
	static Map<Buffer, ShellBuffer> shells;
	static final BufferMLStatusMap compileMap = new BufferMLStatusMap();
	
	static PolyMLProcess polyMLProcess;
	static BufferEditor debugBuffer;
	static PolyMLPlugin jEditGUILock;
	static Integer shellBufferNameCount;
	static Integer debugBufferNameCount;

	public PolyMLPlugin() {
		super();
		shells = new Hashtable<Buffer, ShellBuffer>();
		polyMLProcess = null;
		debugBuffer = null;
		jEditGUILock = this;
		shellBufferNameCount = 0;
		debugBufferNameCount = 0;
		// System.err.println("PolyMLPlugin: started!");
	}

	/**
	 * Safe get ShellBuffer of Buffer
	 */
	public static ShellBuffer shellBufferOfBuffer(Buffer b) {
		if (b == null) {
			return null;
		} else {
			return shells.get(b);
		}
	}

	/**
	 * Gets the text area associated with this buffer.
	 * @param b the buffer to find.
	 * @return associated textarea.
	 */
	public static TextArea getTextAreaOfMLBuffer(Buffer b) {
		for (View v : jEdit.getViews()) {
			if (v.getBuffer() == b) {
				return v.getTextArea();
			}
		}
		return null;
	}

	/**
	 * Append a message to the debug buffer, creating said buffer if required.
	 */
	public static void debugMessage(String s) {
		if (Boolean.parseBoolean(jEdit
				.getProperty(PROPS_COPY_OUTPUT_TO_DEBUG_BUFFER))) {
			if (debugBuffer == null) {
				// creates a new buffer in the view (why not newDebugShellBuffer()..?)
				debugBuffer = new BufferEditor(newDebugBufferFile(null, null));
				// debugBuffer.getBuffer().setNewFile(false);
			}
			debugBuffer.append(s);
		}
	}

	/**
	 * Gets the command required to launch Poly-IDE.
	 * List length always == 1 at present.
	 * @return the command as a list of strings.
	 */
	public static List<String> getPolyIDECmd() {
		String s = PolyMLPlugin.getPolyIDECmdString();
		List<String> cmd = new LinkedList<String>();
		// for (String s2 : s.split(" ")) {
		// cmd.add(s2);
		// }
		cmd.add(s);
		return cmd;
	}

	/**
	 * Gets the command required to launch Poly-IDE.
	 * @return the command as a string.
	 */	
	public static String getPolyIDECmdString() {
		return jEdit.getProperty(PROPS_POLY_IDE_COMMAND);
	}

	/** 
	 * called when plugin is loaded/added
	 * @see EBPlugin#start()
	 */
	public void start() {
		// System.err.println("PolyMLPlugin: start called.");
		// TODO: add TextAreaExtension decorations to existing buffers?
	}

	/**
	 * called when plugin is un-loaded/removed
	 * @see EBPlugin#stop()
	 */
	public void stop() {
		stopAllShellBuffers();
		// could remove BufferMLStatusMap from EditBus here.
		if (polyMLProcess != null) {
			polyMLProcess.closeProcess();
		}
		BufferMLStatusMap.unregister();
	}

	/**
	 * Makes attempts to start or restart PolyML.
	 * @return true of the process is running (or has been restarted)
	 */
	static public boolean tryEnsurePolyMLStarted() {
		if (polyMLProcess == null) {
			return restartPolyML();
		} else if (!polyMLProcess.mRunningQ) {
			try {
				polyMLProcess.restartProcess();
			} catch (IOException e) {
				return false;
				// e.printStackTrace();
			}
			return polyMLProcess.mRunningQ;
		} else {
			return true;
		}
	}

	/**
	 * Restarts PolyML
	 * @return false if an error has occurred during restart.
	 */
	static public boolean restartPolyML() {
		// System.err.println("restarting polyml...");
		try {
			if (polyMLProcess == null) {
				polyMLProcess = new PolyMLProcess(getPolyIDECmd(), compileMap);
			} else {
				polyMLProcess.setCmd(getPolyIDECmd());
			}
			polyMLProcess.restartProcess();
			return true;
		} catch (IOException e) {
			// System.err.println("PolyMLPlugin: Failed to restart PolyML!");
			// e.printStackTrace();
			polyMLProcess = null;
			return false;
		}
	}

	/**
	 * (re) create the IDE heap file for working with projects
	 * @return true if managed to create the IDE heap
	 */
	static public boolean rebuildIDEHeap() {
		boolean result;
		if(restartPolyML()) {
			result = polyMLProcess.createPolyIDEheap();
		} else {
			result = false;
		}
		System.err.println("rebuildIDEHeap done.");
		return result;
	}
	
	/**
	 * Pops up a warning that PolML could not be (re)started, and
	 * offers some advice.
	 */
	public static void showMLFailed() {
		JOptionPane.showMessageDialog(
				null,
				"Failed to (re)start PolyML from command: "
						+ jEdit.getProperty(PROPS_POLY_IDE_COMMAND)
						+ "\nChange the command in the Plugin Options menu",
				"PolyML not running", JOptionPane.WARNING_MESSAGE);
	}
	
	/**
	 * If in a shell buffer, process rest of the input; else we treat the buffer
	 * as an ML buffer and send buffer to ML and process contents
	 * 
	 * @param b
	 */
	static public void sendBufferToPolyML(Buffer b, EditPane pane) {
		try {
			// System.err.println("called processShellBufferToEOF");
			ShellBuffer s = shellBufferOfBuffer(b);
			if (s == null) {
				// if not a shell buffer, treat as an ML file and compile it.
				if (tryEnsurePolyMLStarted()) {
					// errorSource.clear();
					synchronized (jEditGUILock) {
						if (jEdit.getActiveView().getBuffer() != b) {
							System.err.print("Action Script oddity! buffer is not the active buffer! Will use the active buffer instead.");
							b = jEdit.getActiveView().getBuffer();
						}

						// fire off compile message.
						compileMap.setResultFor(b, null); // clear compile status (eventually necessary to clear errors, I suppose)
						new PolyEBMessage(null, PolyMsgType.POLY_WORKING, true).send();
						/* errorSource.removeFileErrors(b.getPath());
						 * errorSource.addError(new DefaultErrorSource.DefaultError(
										errorSource, ErrorSource.WARNING, b
												.getPath(), 0, 0, 0,
										"Compiling ML ... "));*/
						polyMLProcess.sendCompileBuffer(b, pane);
					}
					
					System.err.println("compiled buffer: " + b.getPath());
				} else {
					showMLFailed();
				}
			} else {
				s.sendBufferTextToEOF();
			}
		} catch (IOException e) {
			// e.printStackTrace();
			System.err.println(e.toString());
		}
	}

	/**
	 * Cancel a compilation
	 */
	static public void sendCancelToPolyML() {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendCancelLastCompile();
		} else {
			showMLFailed();
		}
	}

	/**
	 * get possible operations
	 */
	static public void sendGetProperies(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendGetProperies(e);
		} else {
			showMLFailed();
		}
	}

	static public void sendGetType(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendGetType(e);
		} else {
			showMLFailed();
		}
	}

	/**
	 * Location requests
	 */
	static public void sendLocationDeclared(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendLocationDeclared(e);
		} else {
			showMLFailed();
		}
	}

	static public void sendLocationOpened(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendLocationOpened(e);
		} else {
			showMLFailed();
		}
	}

	static public void sendLocationOfParentStructure(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendLocationOfParentStructure(e);
		} else {
			showMLFailed();
		}
	}

	/**
	 * Moving around parse tree
	 */
	static public void sendMoveToParent(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendMoveToParent(e);
		} else {
			showMLFailed();
		}
	}

	static public void sendMoveToFirstChild(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendMoveToFirstChild(e);
		} else {
			showMLFailed();
		}
	}

	static public void sendMoveToNext(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendMoveToNext(e);
		} else {
			showMLFailed();
		}
	}

	static public void sendMoveToPrevious(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendMoveToPrevious(e);
		} else {
			showMLFailed();
		}
	}

	static public ShellBuffer newShellBuffer() {
		synchronized (jEditGUILock) {
			View v = jEdit.getActiveView();
			// TextArea a = v.getTextArea();
			// start off not showing the new buffer
			Buffer fromBuffer = v.getBuffer();
			Buffer b = newShellBufferFile(null, fromBuffer.getDirectory());
			// b.setNewFile(false);
			// System.err.println("newShellBuffer");

			ShellBuffer s;
			try {
				BufferEditor be = new BufferEditor(b);
				s = new ShellBuffer(be);
				shells.put(b, s);
				// show buffer after adding to shell list so that buffer
				// changed events trigger use of text area extensions.
				v.showBuffer(b);

				String heap = ProjectTools.searchForBufferHeapFile(fromBuffer);
				if (heap != null) {
					be.append(ProjectTools.MLStringForLoadHeap(heap));
				}

				// s.showInTextArea(a); // done event in showBuffer
				return s;
			} catch (IOException e) {
				// e.printStackTrace();
				System.err.println(e.toString());
				return null;
			}
		}
	}

	/**
	 * start and restart are the same: they restart shell in the current buffer
	 * @see #restartShellInBuffer(Buffer)
	 */
	static public void startShellInBuffer(Buffer b) {
		// System.err.println("startShellInBuffer");
		restartShellInBuffer(b);
	}

	static public void prevCommand(Buffer b) {
		ShellBuffer sb = shellBufferOfBuffer(b);
		if (sb != null) {
			sb.prevCommand();
		}
	}

	static public void nextCommand(Buffer b) {
		ShellBuffer sb = shellBufferOfBuffer(b);
		if (sb != null) {
			sb.nextCommand();
		}
	}

	static public void restartShellInBuffer(Buffer b) {
		// System.err.println("restartShellInBuffer");
		ShellBuffer sb = shellBufferOfBuffer(b);
		if (sb != null) {
			sb.restartProcess();
		} else {
			try {
				ShellBuffer s = new ShellBuffer(new BufferEditor(b));
				shells.put(b, s);
				
				// turn on extra text area extensions for all views of this
				// shell buffer
				s.showInAllTextAreas();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println(e.toString());
			}
		}
	}

	static public void stopShellInBuffer(Buffer b) {
		ShellBuffer sb = shellBufferOfBuffer(b);
		if (sb != null) {
			sb.stopProcess();
		}
	}

	static public void stopAllShellBuffers() {
		for (ShellBuffer sb : shells.values()) {
			sb.stopProcess();
		}
	}

	/**
	 * when an edit pane shows a ShellBuffer, add the shell Buffer's
	 * TextAreaExtension. (For showing the cool red prompt)
	 * 
	 * @param editPane
	 */
	public void usingShellBufferTextArea(EditPane editPane) {
		synchronized (jEditGUILock) {
			Buffer b = editPane.getBuffer();
			ShellBuffer s = shellBufferOfBuffer(b);
			// System.err.println("usingShellBufferTextArea");
			if (s != null) {
				s.showInTextArea(editPane.getTextArea());
			}
		}
	}

	/**
	 * when an edit pane stops showing a ShellBuffer, remove the shell Buffer's
	 * TextAreaExtension.
	 * 
	 * @param editPane
	 */
	public void unusingShellBufferTextArea(EditPane editPane) {
		synchronized (jEditGUILock) {
			Buffer b = editPane.getBuffer();
			ShellBuffer s = shellBufferOfBuffer(b);
			if (s != null) {
				s.unShowInTextArea(editPane.getTextArea());
			}
		}
	}

	/**
	 * Handles EventBus events.  Specifically:
	 * <li>handle buffer closing events to close associated process.
	 * <li>add and remove error-highlighting textarea extensions 
	 */
	public void handleMessage(EBMessage msg) {
		/*
		 * if (msg instanceof PluginUpdate) { if(((PluginUpdate)msg).getWhat()
		 * == PluginUpdate.LOADED && ((PluginUpdate)msg).getPluginJAR() ==
		 * this.getPluginJAR()) { restartPolyML(); } } else
		 */
		if (msg instanceof BufferUpdate) {
			BufferUpdate bufferUpdate = (BufferUpdate) msg;
			// if a buffer is closed; close its associated shell
			if (bufferUpdate.getWhat() == BufferUpdate.CLOSING) {
				Buffer b = bufferUpdate.getBuffer();
				ShellBuffer s = shellBufferOfBuffer(b);
				if (s != null) {
					s.stopProcess(); // note: this unshows text area extensions
					// too
					shells.remove(b);
				}
				if (debugBuffer != null && b == debugBuffer.mBuffer) {
					debugBuffer = null;
				}
			}
			// else if(bufferUpdate.getWhat() == BufferUpdate.SAVED) {
			// Dummy stub: maybe do something when PolyML buffer is saved
			// Buffer b = bufferUpdate.getBuffer();
			// Mode m = b.getMode();
			// if(m.getName() == "PolyML Mode") { // Hacky: better way to check
			// mode?
			// System.err.println("PolyML Mode Buffer Saved! do something? ");
			// }
			// }
		} else if (msg instanceof EditPaneUpdate) {
			// handle creation/changing of shell buffers:
			//   add / remove extra painting extensions when needed.
			EditPaneUpdate editPaneUpdate = (EditPaneUpdate) msg;
			EditPane pane = editPaneUpdate.getEditPane();
			if (editPaneUpdate.getWhat() == EditPaneUpdate.CREATED) {
				usingShellBufferTextArea(pane);
				//new ErrorHighlight(pane).addToPane(); // TODO: add
				//new ErrorGutterIcon(pane).addToPane();// TODO: add
			} else if (editPaneUpdate.getWhat() == EditPaneUpdate.BUFFER_CHANGING) {
				unusingShellBufferTextArea(pane);
			} else if (editPaneUpdate.getWhat() == EditPaneUpdate.BUFFER_CHANGED) {
				usingShellBufferTextArea(pane);
			} else if (editPaneUpdate.getWhat() == EditPaneUpdate.DESTROYED) {
				unusingShellBufferTextArea(pane);
				//ErrorHighlight.removeFromPane(pane);  // TODO: add
				//ErrorGutterIcon.removeFromPane(pane); // TODO: add
			}
		}
	}
	
	static Buffer newMLTxtBufferFile(View view, String dir, String name,
			Integer count) {
		count++;
		return jEdit.openFile(view, dir, name + "-" + count + ".ml.txt", true,
				null);
	}
	
	static Buffer newShellBufferFile(View view, String dir) {
		return newMLTxtBufferFile(view, dir, "ShellBuffer",
				shellBufferNameCount);
	}

	static Buffer newDebugBufferFile(View view, String dir) {
		return newMLTxtBufferFile(view, dir, "DebugBuffer",
				debugBufferNameCount);
	}

	/**
	 * Gets or creates the a new Debug Shell buffer.
	 * @return
	 */
	static public BufferEditor newDebugShellBuffer() {
		synchronized (jEditGUILock) {
			debugBuffer = new BufferEditor(newDebugBufferFile(null, null));
		}
		return debugBuffer;
	}

	static public void test() {
		synchronized (jEditGUILock) {
			jEdit.newFile(jEdit.getActiveView());
		}
	}

}
