package polyml;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

//import org.xhtmlrenderer.simple.XHTMLPanel;
//import org.xhtmlrenderer.simple.FSScrollPane;
//import org.xhtmlrenderer.context.AWTFontResolver;
//import org.xhtmlrenderer.layout.SharedContext;
//import org.xhtmlrenderer.extend.TextRenderer;
//import org.xhtmlrenderer.swing.Java2DTextRenderer;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.DefaultFocusComponent;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.Selection.Range;

import errorlist.ErrorSource;
import errorlist.ErrorSource.Error;

/*
 * Dockable window with rendered state output
 *
 * @author Lucas Dixon
 * @author Graham Dutton
 */
public class StateViewDockable extends JPanel {

	private static final long serialVersionUID = 8805835774111662901L;
	/** Default size */
	public static final Dimension DefaultSize = new Dimension(500, 250);
	/** URI scheme for errors */
	public static final String ErrLineSep = "!";
	public static final String ErrScheme = "jerr://";
	/** Given view (context). */
	private final View view;
	/** Where it all happens */
	private final JEditorPane panel;
	
	/** The document to which we append. */
	private StateViewDocument doc;
	
	/**
	 * An Action with a button pre-associated.
	 */
	interface ButtonedAction extends Action {
		/** Returns a button associated with this action. */
		public AbstractButton getButton();
	}
	
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
	
	/**
	 * All Toolbar items. 
	 */
	private final ButtonedAction[] toolbarDef = new ButtonedAction[]{
		new NamedAction("Export", "Export contents of this log to a new buffer") {
			public void actionPerformed(ActionEvent e) {
				Buffer newbuffer = jEdit.newFile(view);
				newbuffer.insert(0, panel.getText());
		}},
		new NamedAction("Clear", "Erase the contents of this log.") {
			public void actionPerformed(ActionEvent e) {
				newDocument();
		}},
		new NamedAction("Good", "Append some other text.") {
			public void actionPerformed(ActionEvent e) {
				doc.appendPar("Woo.\n That's what I said.", String.valueOf(CompileResult.STATUS_SUCCESS));
		}},
		new NamedAction("Neutral", "Append some test text.") {
			public void actionPerformed(ActionEvent e) {
				doc.appendPar("Woo.\n That's what I said.", null);
		}},
		new NamedAction("Bad", "Append some other text.") {
			public void actionPerformed(ActionEvent e) {
				doc.appendPar("Woo.\n That's what I said.", String.valueOf(CompileResult.STATUS_EXCEPTION_RAISED));
		}},
		new NamedAction("Errors", "Get errors.") {
			public void actionPerformed(ActionEvent e) {
				Error[] errors = PolyMLPlugin.errorSource.getAllErrors();
				PolyMLPlugin.errorSource.clear();
				if (errors != null) {
					for (Error er : errors) {
						if (er != null) {
							String msg = er.getErrorMessage();
							msg += " at <a href=\"";
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
							}
						} else {
							System.err.println("Null error?");
						}
				}
			}
		}}

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
			tb.add(a.getButton());
		}
		add(tb, BorderLayout.SOUTH);
		tb.validate();
		
		// Content goes here.
		// Depending on how we get on with the JEditor,
		// we might want to revert to... 
		// panel = new XHTMLPanel(new UserAgent());
		panel = new JEditorPane();
		// panel.setEditable(false); // TODO: enable.
		panel.setEditorKit(new HTMLEditorKit());
		
		// and add things.
		add(new JScrollPane(panel), BorderLayout.CENTER);
		newDocument();
	}

	/**
	 * Reset the document.
	 */
	public void newDocument() {
		HTMLDocument newDoc = (HTMLDocument) panel.getEditorKit().createDefaultDocument(); // sets up parser, etc.
		panel.setDocument(newDoc);
		doc = new StateViewDocument( newDoc );
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
	
	/**
	 * Returns a property map with properties sufficient to open the file
	 * specified by the error given.
	 */
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
	}
	
	/**
	 * Turns an error into an error URI which might later be somehow interpreted.
	 * Transitional method; hopefully will not be needed when ErrorList dependency is removed.
	 * @see PolyMarkup#toURIString()
	 */
	public static String uriof(Error e) {
		StringBuffer uri = new StringBuffer();
		uri.append(ErrScheme); // jerr://
		uri.append(e.getFilePath()); // /path/to/file
		if (e.getLineNumber() > 0) { // :000
			uri.append(ErrLineSep);
			uri.append(e.getLineNumber());
			if (e.getStartOffset() > 0) { // :00
				uri.append(ErrLineSep);
				uri.append(e.getStartOffset());
			}
		}
		return uri.toString();
	}

}