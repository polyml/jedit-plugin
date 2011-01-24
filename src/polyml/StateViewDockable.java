package polyml;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.Selection.Range;

/**
 * Dockable window with rendered state output
 *
 * @author Lucas Dixon
 * @author Graham Dutton
 */
public class StateViewDockable extends JPanel implements EBComponent {

	private static final long serialVersionUID = 8805835774111662901L;
	/** Default size */
	public static final Dimension DefaultSize = new Dimension(500, 250);
	/** Given view (context). */
	private final View view;
	/** Where it all happens */
	private final JEditorPane panel;
		
	/** The document to which we append. */
	private StateViewDocument doc;
	
	/** Latest instance to be placed on the bus (only the most recent will listen) */
	private static StateViewDockable busInstance = null;
	
	/** Whether output should be suspended */
	// private boolean suspended;
	
	/**
	 * A simple HyperlinkListener which causes mouseover cursor effects.
	 */
	class HoverListener implements HyperlinkListener {
		final JEditorPane pane;
		HoverListener(final JEditorPane pane) {
			this.pane = pane;
		}
		public void hyperlinkUpdate(HyperlinkEvent e) {
	        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
	            pane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	            System.out.println("type == HyperlinkEvent.EventType.ENTERED");
	            
	         } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
	            pane.setCursor(Cursor.getDefaultCursor());
	            System.out.println("type == HyperlinkEvent.EventType.EXITED");
	         }
		}
	}
	
	/**
	 * An Action with a button pre-associated.
	 */
	interface ButtonedAction extends Action {
		/** Returns a button associated with this action. */
		public AbstractButton getButton();
	}
	
	/** Friendly name for separators */
	static final ButtonedAction separator = null;
	
	/**
	 * Convenient implementation of an Action which sets name properties in advance.
	 */
	abstract class NamedAction extends AbstractAction implements ButtonedAction {		
		JButton btn;
		public NamedAction(String myname, String mytip) {
			putValue(NAME, myname);
			putValue(TOOL_TIP_TEXT_KEY, mytip);
		}
		public JButton getButton() {
			if (btn == null) {
				btn = new JButton((String)getValue(NAME));
				btn.setAction(this);
			}
			return btn;
		}
	}
	
	/** add prover state view, a special button with class access */
	private NamedAction stateButton = new NamedAction("(ML Status)", "PolyML status - activity not yet detected") {
		private Color running = new Color(200, 180, 180, 255);
		private Color idle = new Color(0, 50, 0, 35);
		public void actionPerformed(ActionEvent e) {
			PolyMLPlugin.sendCancelToPolyML();
		}
		public void setEnabled(boolean b) {
			AbstractButton btn = getButton();
			if (b) {
				btn.setText("ML: Running");
				btn.setBackground(running);
				btn.setToolTipText("Click to cancel current PolyML execution");
			} else {
				btn.setText("ML: Idle");
				btn.setBackground(idle);
				btn.setToolTipText("PolyML is not currently processing a buffer");
			}
			btn.setEnabled(b);
		};
	};
	
	/**
	 * All Toolbar items to be listed (if not defined) in this array.
	 */
	private final ButtonedAction[] toolbarDef = new ButtonedAction[]{
		new NamedAction("Export", "Export contents of this log to a debug buffer") {
			public void actionPerformed(ActionEvent e) {
				String text = panel.getText();
				Buffer newbuffer = jEdit.newFile(view);
				newbuffer.insert(0, text);
		}},
		new NamedAction("Clear", "Erase the contents of this log.") {
			public void actionPerformed(ActionEvent e) {
				newDocument();
		}},
		separator,
		new NamedAction("Compile", "Process Buffer in ML") {
			public void actionPerformed(ActionEvent e) {
				jEdit.getAction("polyml-menu.mitem-process_buffer").invoke(view);
		}},
		new NamedAction("Refresh Errors", "Displays stored status for the status buffer.") {
			public void actionPerformed(ActionEvent e) {
					displayResultFor(view.getBuffer());
		}},
		new NamedAction("Print All Status", "Dumps all status messages into the status buffer") {
			public void actionPerformed(ActionEvent e) {
				for (CompileResult r : PolyMLPlugin.polyMLProcess.compileInfos.getResults()) {
					displayResult(r);
				}
				//for (Buffer b : PolyMLPlugin.compileMap.getBuffers()) {
				//	displayResultFor(b);
				//}
		}},	
		separator,
		stateButton // defined above.
	};
	
	/**
	 * Convenience method for dockable launching purposes.
	 * @param view the view into which to provide this dockable
	 * @throws InstantiationException 
	 */
	public StateViewDockable(View view) throws InstantiationException {
		
		this(view, "default");
	}
	
	/**
	 * Launch this dockable.
	 * @param view the view into which to provide this dockable
	 * @param position the dockable's position within the editor 
	 * @throws InstantiationException 
	 */
	public StateViewDockable(View view, String position) throws InstantiationException {
		// add us to the editbus
		if (StateViewDockable.busInstance != null) {
			EditBus.removeFromBus(busInstance);
		}
		busInstance = this;
		EditBus.addToBus(busInstance);
		
		// set the parent view and position.
		this.view = view;

		// outer panel
		if (position == DockableWindowManager.FLOATING) {
			setPreferredSize(StateViewDockable.DefaultSize);
		}
				
		// establish the panel and its toolbar
		setLayout(new BorderLayout());
		// Toolbar (whose actions defined above)
		JToolBar tb = new JToolBar();
		for (ButtonedAction a : toolbarDef) {
			if (a == null) {
				tb.addSeparator();
			} else {
				tb.add(a.getButton());
			}
		}
		
		// configure the state button
		stateButton.getButton().setEnabled(false);

		add(tb, BorderLayout.SOUTH);
		tb.validate();
		
		// Content goes here.
		panel = new JEditorPane();
		// panel.setEditable(false); // TODO: enable.
		panel.setEditorKit(new HTMLEditorKit());
		// now set event listener for document clicks.
		panel.addHyperlinkListener(new ErrorLinkOpener(view));
		panel.addHyperlinkListener(new HoverListener(panel));
		
		// and add things.
		add(new JScrollPane(panel), BorderLayout.CENTER);
		newDocument();
	}	
	
	/**
	 * Reset the document.
	 */
	public void newDocument() {
		HTMLDocument newDoc = (HTMLDocument) panel.getEditorKit().createDefaultDocument(); // sets up parser, etc.
		doc = new StateViewDocument( newDoc );
		panel.setDocument(newDoc);
		// set editability according to preference.
		panel.setEditable(Boolean.parseBoolean(jEdit.getProperty(PolyMLPlugin.PROPS_STATE_DOC_EDITABLE)));
		if (panel.isEditable()) {
			doc.appendPar("WARNING: This buffer is editable: hyperlinks are therefore not clickable.  " +
					"Please change this in PolyML preferences and reset the document to re-enable clickable hyperlinks.", "info");
			doc.appendHTML("<hr/>");
		}
		scrollToBottom(false);
	}

	/**
	 * Force focus on ErrorList.
	 * This method is used by the 'errorlist-focus' action?
	 * TODO: remove when no longer dependent on errorlist.
	 */
	public void focus() {
		panel.requestFocus();
	}
	
	/**
	 * Displays a given CompileResult. 
	 * @param r the result.
	 */
	public void displayResult(CompileResult r) {
		doc.appendPar(r.stringOfResult(), r.status);
		List<PolyMLError> errors = r.errors;
		if (errors != null) {
			String fullMsg = "";
			for (PolyMLError er : errors) {
				if (er != null) {
					fullMsg += er.message;
				} else {
					PolyMLPlugin.debugMessage("Null error list on compile result"+r+"?");
				}
			}
			if (!fullMsg.equals("")) {
				doc.appendPar(fullMsg, r.status);
			}
		}
		scrollToBottom(false);

		/*msg += " at <a href=\"";
		msg += StateViewDockable.uriof(er);
		msg += "\">";
		msg += er.getFileName();
		msg += ":";
		msg += er.getLineNumber();
		msg += "</a>.";
		if (er.getErrorType() == ErrorSource.ERROR) {
			doc.appendPar(msg, CompileResult.STATUS_EXCEPTION_RAISED);
		} else {
			doc.appendPar(msg, CompileResult.STATUS_SUCCESS);
		}*/
	}
	
	/**
	 * Scrolls the panel to the bottom (hack?).
	 * Can be replaced by a content-related scroll in future.
	 * @param force whether to ignore the scroll-to-bottom option
	 */
	public void scrollToBottom(boolean force) {
		if (force || Boolean.parseBoolean(jEdit.getProperty(PolyMLPlugin.PROPS_SCROLL_ON_OUTPUT))) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					Rectangle target = new Rectangle(0, panel.getBounds().height, 1, 100);
					panel.scrollRectToVisible(target);
				}
			});
		}
	}
	
	/**
	 * Appends all errors relating to the given buffer, to the document
	 * Appends a warning if no result is available
	 * @param path the path
	 */
	public void displayResultFor(String path) {
		try {
			CompileRequest q = PolyMLPlugin.polyMLProcess.compileInfos.getFromPath(path);
			CompileResult r = q.getResult();
			doc.appendPar("Status for "+path, "info");
			displayResult(r);
		} catch (NullPointerException e) {
			doc.appendPar("No Status for file "+(path==null?"(null)":path)+".", PolyMLError.KIND_WARNING);
			scrollToBottom(false);
		}
	}
	
	/**
	 * @see #displayResultFor(Buffer)
	 * @param b the buffer
	 */
	public void displayResultFor(Buffer b) {
		displayResultFor(b.getPath());
	}
	
	/**
	 * Returns a property map with properties sufficient to point
	 * a JEdit buffer at the specified location.
	 */
	public static Map<String,Object> propsForLocation(int offset, int length) {
		Map<String,Object> props = new HashMap<String,Object>();
		// TODO: calculate caret and selection from offset/length?
		props.put(Buffer.CARET, offset);
		Selection sel = new Range(offset, offset+length);
		props.put(Buffer.SELECTION, sel);
		return props;		
	}
	
	/**
	 * Returns a property map with properties sufficient to point
	 * a JEdit buffer at the specified line number.
	 * TODO: take line offset into account.
	 */
	public static Map<String, Object> propsForLine(int lineNo, int lineOff) {
		Map<String, Object> props = new HashMap<String,Object>();
		props.put(Buffer.SCROLL_VERT, lineNo);
		return props;
	}
	
	/*
	 * Returns a property map with properties sufficient to open the file
	 * specified by the given error.
	 * @deprecated
	 *
	public boolean openBufferForError(Error e) {
		//Map<String, Object> props = new HashMap<String,Object>();
		//props.put(Buffer.SCROLL_VERT, e.getLineNumber());
		//props.put(Buffer.SELECTION, new Range(e.getStartOffset(), e.getEndOffset()));
		//jEdit.openFile(view, null, path, false, props);

		String path = e.getFilePath(); 
		if (path == null || path.equals("")) {
			return false;
		}
		Buffer newBuffer = jEdit.openFile(view, path);
		if (newBuffer == null) {
			return false;
		}
		// Move to the error.
		newBuffer.setProperty(Buffer.SCROLL_VERT, e.getLineNumber());
		newBuffer.setProperty(Buffer.SELECTION, new Range(e.getStartOffset(), e.getEndOffset()));		
		return true;
	}*/

	/**
	 * Catches errors passed on to the error bus for appending.
	 * Watch only for our own errors.
	 */ @Override
	public void handleMessage(EBMessage message) {
		doc.appendPar("MSG["+message.toString()+"]", "debug"); // DEBUG
		 
		// check for buffer update notifications, which will trigger a reload.
		if (message instanceof PolyEBMessage) {
			PolyEBMessage msg = (PolyEBMessage) message;
			if (msg.getType() == PolyMsgType.COMPILE_RESULT) {
				try {
					CompileRequest req = (CompileRequest) msg.getPayload();
					displayResult(req.getResult());
				} catch (NullPointerException e) {
					doc.appendPar("Ignored BUFFER_UPDATE with no payload from "+msg.getSource(), "gray");
				}
			} else if (msg.getType() == PolyMsgType.POLY_WORKING) {
				// update prover status live.
				if (msg.getPayload() == null) {
					// something went wrong
					stateButton.setEnabled(false);
					stateButton.getButton().setText("ML: Failed?");
				} else {
					stateButton.setEnabled((Boolean) msg.getPayload());
				}
			// ----------------
			// Informational and Transitional errors can be ignored (and indeed removed).  Just watching for interest.
			} else if (msg.getType() == PolyMsgType.INFORMATION) {
				doc.appendPar(msg.getPayload().toString(), CompileResult.STATUS_SUCCESS);
				scrollToBottom(false);
			} else if (msg.getType() == PolyMsgType.TRANSITIONAL) {
				// Append transitional errors straight to the buffer
				doc.appendPar(msg.getPayload().toString(), "gray");
				scrollToBottom(false);
			// ----------------
			}			
		// if user has switched buffer, change our prover output?
		// EditPaneUpdate[what=BUFFER_CHANGED,source=org.gjt.sp.jedit.EditPane[active,global]]
		} else if (message instanceof EditPaneUpdate) {
			// query the mapping for all the latest information on this buffer
			EditPaneUpdate msg = (EditPaneUpdate) message;
			if (msg.getWhat() == EditPaneUpdate.BUFFER_CHANGED) {
				if (jEdit.getProperty(PolyMLPlugin.PROPS_BUFFER_CHANGE).equals(PolyMLPlugin.PROPS_BUFFER_CHANGE_NOWT)) {
					// do nothing
				} else {
					// process all previous status for this buffer, clearing or marking the new buffer as appropriate.
					if (jEdit.getProperty(PolyMLPlugin.PROPS_BUFFER_CHANGE).equals(PolyMLPlugin.PROPS_BUFFER_CHANGE_CLEAR)) {
						newDocument();
					} else {
						doc.appendHTML("<hr/>");
						// doc.appendPar("Switching to "+msg.getEditPane().getBuffer()+".", "info"); // DEBUG
						scrollToBottom(false); // a bit redundant, but...
					}
					Buffer buffer = msg.getEditPane().getBuffer();
					if (buffer.equals(view.getBuffer())) {
						displayResultFor(buffer);
					} else if (buffer.getPath() != null) {
						doc.appendPar("Buffer update message for other buffer "+buffer.getPath()+" received.", "gray");
					} else {
						doc.appendPar("Buffer update message for null buffer received.", "gray");
					}
					scrollToBottom(false);
				}
				// experiment with adding markers, which are, sadly, probably not what we want.
				// msg.getEditPane().getBuffer().addMarker('c', 10);
		//	} else {
		//		doc.appendPar("Ignored EditPaneUpdate "+msg.getWhat()+" from "+msg.getSource()+".", "gray"); // DEBUG
			}
		//} else {// everything else, for now.
		//	doc.appendPar("(Ignored "+message+")", "gray"); // DEBUG
		}
	}


}