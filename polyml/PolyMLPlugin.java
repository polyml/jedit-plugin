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

import errorlist.DefaultErrorSource;
import errorlist.ErrorSource;

/**
 * Plugin class the PolyML Plugin for jedit.
 * 
 * @author Lucas Dixon
 * @created 03 November 2007
 * @version 0.2.0
 */
public class PolyMLPlugin extends EBPlugin {

	public static final String NAME = "PolyML-Plugin";
	public static final String OPTION_PREFIX = "options.polyml.";

	public static final String PROPS_POLY_IDE_COMMAND = "options.polyml.polyide_command";
	public static final String PROPS_SHELL_COMMAND = "options.polyml.shell_command";
	public static final String PROPS_SHELL_PROMPT = "options.polyml.shell_prompt";
	public static final String PROPS_SHELL_MAX_HISTORY = "options.polyml.max_history";
	public static final String PROPS_COPY_OUTPUT_TO_DEBUG_BUFFER = "options.polyml.copy_output_to_debug_buffer";
	public static final String PROPS_RUN_FROM_FROM_FILE_DIR = "options.polyml.run_from_file_dir";

	/** Associates Buffers to Processes that output to the buffer */
	static Map<Buffer, ShellBuffer> shells;
	static DefaultErrorSource errorSource;
	static PolyMLProcess polyMLProcess;
	static BufferEditor debugBuffer;
	static PolyMLPlugin jEditGUILock;
	static Integer shellBufferNameCount;
	static Integer debugBufferNameCount;

	public PolyMLPlugin() {
		super();
		shells = new Hashtable<Buffer, ShellBuffer>();
		errorSource = null;
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

	public static TextArea getTextAreaOfMLBuffer(Buffer b) {
		for (View v : jEdit.getViews()) {
			if (v.getBuffer() == b) {
				return v.getTextArea();
			}
		}
		return null;
	}

	public static void debugMessage(String s) {
		if (Boolean.parseBoolean(jEdit
				.getProperty(PROPS_COPY_OUTPUT_TO_DEBUG_BUFFER))) {
			if (debugBuffer == null) {
				// creates a new buffer in the view
				debugBuffer = new BufferEditor(newDebugBufferFile(null, null));
				// debugBuffer.getBuffer().setNewFile(false);
			}
			debugBuffer.append(s);
		}
	}

	public static List<String> getPolyIDECmd() {
		String s = jEdit.getProperty(PROPS_POLY_IDE_COMMAND);
		List<String> cmd = new LinkedList<String>();
		// for (String s2 : s.split(" ")) {
		// cmd.add(s2);
		// }
		cmd.add(s);
		return cmd;
	}

	public static String getPolyIDECmdString() {
		return jEdit.getProperty(PROPS_POLY_IDE_COMMAND);
	}

	// called when plugin is loaded/added
	public void start() {
		// System.err.println("PolyMLPlugin: start called.");
		errorSource = new DefaultErrorSource(NAME);
		DefaultErrorSource.registerErrorSource(errorSource);
	}

	// called when plugin is un-loaded/removed
	public void stop() {
		stopAllShellBuffers();
		DefaultErrorSource.unregisterErrorSource(errorSource);
		if (polyMLProcess != null) {
			polyMLProcess.closeProcess();
		}
	}

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

	static public boolean restartPolyML() {
		// System.err.println("restarting polyml...");
		try {
			if (polyMLProcess == null) {
				polyMLProcess = new PolyMLProcess(getPolyIDECmd(), errorSource);
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

						errorSource.removeFileErrors(b.getPath());
						errorSource
								.addError(new DefaultErrorSource.DefaultError(
										errorSource, ErrorSource.WARNING, b
												.getPath(), 0, 0, 0,
										"Compiling ML ... "));

						polyMLProcess.sendCompileBuffer(b, pane);
					}
					
					System.err.println("compiled buffer: " + b.getPath());
				} else {
					JOptionPane
							.showMessageDialog(
									null,
									"Failed to (re)start PolyML from command: "
											+ jEdit
													.getProperty(PROPS_POLY_IDE_COMMAND)
											+ "\nChange the command in the Plugin Options menu",
									"PolyML not running",
									JOptionPane.WARNING_MESSAGE);
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
	 * 
	 * @param b
	 */
	static public void sendCancelToPolyML() {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendCancelLastCompile();
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Failed to (re)start PolyML from command: "
									+ jEdit.getProperty(PROPS_POLY_IDE_COMMAND)
									+ "\nChange the command in the Plugin Options menu",
							"PolyML not running", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * get possible operations
	 * 
	 * @param b
	 */
	static public void sendGetProperies(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendGetProperies(e);
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Failed to (re)start PolyML from command: "
									+ jEdit.getProperty(PROPS_POLY_IDE_COMMAND)
									+ "\nChange the command in the Plugin Options menu",
							"PolyML not running", JOptionPane.WARNING_MESSAGE);
		}
	}

	static public void sendGetType(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendGetType(e);
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Failed to (re)start PolyML from command: "
									+ jEdit.getProperty(PROPS_POLY_IDE_COMMAND)
									+ "\nChange the command in the Plugin Options menu",
							"PolyML not running", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * Location requests
	 * 
	 * @param b
	 */
	static public void sendLocationDeclared(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendLocationDeclared(e);
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Failed to (re)start PolyML from command: "
									+ jEdit.getProperty(PROPS_POLY_IDE_COMMAND)
									+ "\nChange the command in the Plugin Options menu",
							"PolyML not running", JOptionPane.WARNING_MESSAGE);
		}
	}

	static public void sendLocationOpened(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendLocationOpened(e);
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Failed to (re)start PolyML from command: "
									+ jEdit.getProperty(PROPS_POLY_IDE_COMMAND)
									+ "\nChange the command in the Plugin Options menu",
							"PolyML not running", JOptionPane.WARNING_MESSAGE);
		}
	}

	static public void sendLocationOfParentStructure(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendLocationOfParentStructure(e);
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Failed to (re)start PolyML from command: "
									+ jEdit.getProperty(PROPS_POLY_IDE_COMMAND)
									+ "\nChange the command in the Plugin Options menu",
							"PolyML not running", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * Moving around parse tree
	 * 
	 * @param b
	 */
	static public void sendMoveToParent(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendMoveToParent(e);
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Failed to (re)start PolyML from command: "
									+ jEdit.getProperty(PROPS_POLY_IDE_COMMAND)
									+ "\nChange the command in the Plugin Options menu",
							"PolyML not running", JOptionPane.WARNING_MESSAGE);
		}
	}

	static public void sendMoveToFirstChild(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendMoveToFirstChild(e);
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Failed to (re)start PolyML from command: "
									+ jEdit.getProperty(PROPS_POLY_IDE_COMMAND)
									+ "\nChange the command in the Plugin Options menu",
							"PolyML not running", JOptionPane.WARNING_MESSAGE);
		}
	}

	static public void sendMoveToNext(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendMoveToNext(e);
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Failed to (re)start PolyML from command: "
									+ jEdit.getProperty(PROPS_POLY_IDE_COMMAND)
									+ "\nChange the command in the Plugin Options menu",
							"PolyML not running", JOptionPane.WARNING_MESSAGE);
		}
	}

	static public void sendMoveToPrevious(EditPane e) {
		if (tryEnsurePolyMLStarted()) {
			polyMLProcess.sendMoveToPrevious(e);
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Failed to (re)start PolyML from command: "
									+ jEdit.getProperty(PROPS_POLY_IDE_COMMAND)
									+ "\nChange the command in the Plugin Options menu",
							"PolyML not running", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * 
	 * @return
	 */
	static public BufferEditor newDebugShellBuffer() {
		synchronized (jEditGUILock) {
			debugBuffer = new BufferEditor(newDebugBufferFile(null, null));
		}
		return debugBuffer;
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

	/* start and restart are the same: they restart shell in the current buffer */
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

	/** handle buffer closing events to close associated process. */
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
			// handle creation/changing of shell buffers: add on extra painting
			// extension when needed.
			EditPaneUpdate editPaneUpdate = (EditPaneUpdate) msg;
			if (editPaneUpdate.getWhat() == EditPaneUpdate.CREATED) {
				usingShellBufferTextArea(editPaneUpdate.getEditPane());
			} else if (editPaneUpdate.getWhat() == EditPaneUpdate.BUFFER_CHANGING) {
				unusingShellBufferTextArea(editPaneUpdate.getEditPane());
			} else if (editPaneUpdate.getWhat() == EditPaneUpdate.BUFFER_CHANGED) {
				usingShellBufferTextArea(editPaneUpdate.getEditPane());
			} else if (editPaneUpdate.getWhat() == EditPaneUpdate.DESTROYED) {
				unusingShellBufferTextArea(editPaneUpdate.getEditPane());
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

	/* start and restart are the same: they restart shell in the current buffer */
	static public void test() {
		synchronized (jEditGUILock) {
			jEdit.newFile(jEdit.getActiveView());
		}
	}

}
