package polyml;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLDocument.BlockElement;

import org.gjt.sp.jedit.jEdit;

public class StateViewDocument {

	/** Internal document */
	private final HTMLDocument doc;
	/** Root to which all text will be appended for now. */
	private Element appendRoot;

	/**
	 * Defines how status messages will be displayed.
	 * TODO: allow customisation
	 */
	private static final String StyleSheet = ""+
		"body {\n"+
//		"	font-family: monospace;\n"+
//		"	font-size: 10pt;\n"+
		"	color: black;\n"+
		"	background-color: '#DDDDDD';\n"+
		"   padding: 11pt; \n"+
		"}\n"+
		"p {\n"+
		"  white-space: pre-wrap;\n"+
		"  word-wrap: break-word;\n"+
		"}\n"+
		"."+String.valueOf(CompileResult.STATUS_SUCCESS)+" {\n" +
		"	color: green;\n" +
		"}\n" +
		"."+String.valueOf(CompileResult.STATUS_PRELUDE_FAILED)+", " +
		"."+String.valueOf(CompileResult.STATUS_PARSE_FAILED)+", " +
		"."+String.valueOf(CompileResult.STATUS_TYPECHECK_FAILED)+", " +
		"."+String.valueOf(CompileResult.STATUS_EXCEPTION_RAISED)+" {\n" +
		"	color: red;\n" +
		"}\n"+
		"."+String.valueOf(CompileResult.STATUS_CANCEL)+", " +
		"."+String.valueOf(CompileResult.STATUS_BUG)+" {\n"+
		"	color: magenta;\n" +
		"}\n"+
		".gray {\n"+
		"   color: gray;\n"+
		"}\n"+
		".info {\n"+
		"   color: navy;\n"+
		"   border: solid;\n"+
		"   border-width: 2pt;\n"+
		"   border-color: black;\n"+
		"}\n"+
		".debug {\n"+
		"   font-size: 0px;\n"+
		"   margin: 0px; \n"+
		"}\n";

	/**
	 * Resets the active document on the EditorPane.
	 * Intended to destroy the old one.
	 * Sets a new root element.
	 */
	public StateViewDocument(HTMLDocument doc) {
		this.doc = doc;
		
		// Get the first otherwise valid root element:
		//   ignore Bidi stuff since we won't be generating any.
		BlockElement el = null;
		for (Element e : doc.getRootElements()) {
			if (e instanceof BlockElement) {
				el = (BlockElement) e;
				break;
			}
		}
		if (el == null) System.err.println("Couldn't find root element!");
		
		// now drill down to add our custom root element
		el = (BlockElement) getFirstChild(el, "body");
		

		/*
		try {
			this.doc.insertAfterStart(el, "<span class=\"root\"></span>");
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// retrieve that which we've just added (d'oh)
		el = (BlockElement) getFirstChild(el, "span");
		*/
		appendRoot = el;
		
		loadStyles();
	}

	/**
	 * Gets the first named child element.
	 * @param name
	 * @return
	 */
	private Element getFirstChild(Element el, String name) {
		for (int i = 0; i < el.getElementCount(); i++) {
			Element e = el.getElement(i);
			if (e.getName() == name) {
				return e;
			}
		}
		if (el == null)	System.err.println("Couldn't find matching child element "+name+".");
		return null;
	}
	
	/**
	 * Appends pure HTML crudely to the current panel.
	 * TODO: Don't think special characters are escaped.
	 * @param text the HTML to append.
	 */
	public synchronized void appendHTML(String text) {
		// initialise stuff
		try {
			doc.insertAfterStart(appendRoot, text);
			//doc.insertBeforeEnd(appendRoot, text);
		} catch (BadLocationException e) {
			System.err.println("BadLocation writing data to HTML panel");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Appends HTML crudely to the current panel, suffixing a newline.
	 * TODO: Neither special characters nor HTML is escaped just now.
	 * @param text a line of HTML to append.
	 * @param cls the class attribute to give the element.
	 */
	public void appendPar(String text, String cls) {
		//text.replaceAll("\\n", "<br/>\n");
		if (cls == null || cls.equals("")) {
			cls = "";
		} else {
			cls = " class=\""+cls+"\"";
		}
		appendHTML("<p"+cls+">"+text+"</p>");
	}

	/**
	 * Shamelessly convenience method for {@link #appendPar(String, String)}.
	 */
	public void appendPar(String msg, char c) {
		appendPar(msg, String.valueOf(c));
	}
	
	/**
	 * Turns a JEdit font preference string into some CSS which can
	 * be used by the {@link StateViewDocument}.
	 * @param preference the preference string
	 * @return some CSS
	 */
	private String fontPreferenceToCSS(String preference) {
		String family = "monospace";
		int size = 10;
		try {
			Font monoFont = jEdit.getFontProperty(preference);
			family = monoFont.getFamily();
			size = monoFont.getSize();
		} catch (Exception e) {
			System.err.println("Could not get font preference "+preference+".");
		}
		return "body { \n"+
			"  font-family: \""+family+"\";\n"+
			"  font-size: "+size+"pt;\n"+
			"}\n";
	}
	
	/**
	 * Loads styles.
	 * Note that this does not remove old styles, so should be run only once per document.
	 */
	private void loadStyles() {
		/* doesn't work...
		// remove old styles
		Enumeration<?> styles = doc.getStyleSheet().getStyleNames();
		while (styles != null && styles.hasMoreElements()) {
			String s = styles.nextElement().toString();
			System.out.println("Removing style "+s);
			doc.getStyleSheet().removeStyle(s);
		}*/

		// built-in customisation.
		doc.getStyleSheet().addRule(StyleSheet);
		// get jEdit textarea preferences and apply them to the body.   
		doc.getStyleSheet().addRule(fontPreferenceToCSS("view.font")); // view.font
		String report = "";
		
        // Add external stylesheet if defined
		String stylePath = jEdit.getProperty(PolyMLPlugin.PROPS_STATE_OUTPUT_CSS_FILE);
		if (! (stylePath == null || stylePath.equals("") )) {
			StringBuilder rules = new StringBuilder();
			BufferedReader reader;
			try {
				reader = new BufferedReader(new FileReader(jEdit.getProperty(PolyMLPlugin.PROPS_STATE_OUTPUT_CSS_FILE)));
				String line = null;
				while (( line = reader.readLine()) != null) {
				    rules.append(line + System.getProperty("line.separator"));
				}
			} catch (FileNotFoundException e) {
				report = e.getMessage();
			} catch (IOException e) {
				report = e.getMessage();
			}
			if (rules.toString().length() == 0) {
				System.err.println("PolyML Plugin: Specified external stylesheet not found or not read: "+report);
			} else {
				doc.getStyleSheet().addRule(rules.toString());
			}
		}
	}

}
