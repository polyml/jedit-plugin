package polyml;

import java.net.MalformedURLException;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.Selection.Range;

/**
 * Opens hyperlinks which follow a plugin-internal URL scheme
 */
public class ErrorLinkOpener implements HyperlinkListener {

	/** The recognised protocol string. */
	public static final String ERROR_PROTOCOL = "pmjp";
	/** All possible query arguments for an error URL */
	private static final HashMap<String,String> args = new HashMap<String, String>(){{
		put("line", null);
		put("start", null);
		put("end", null);
	}};
	
	private View view;
		
	/**
	 * A new link opener. 
	 * @param view the JEdit view in which to perform buffer operations.
	 */
	public ErrorLinkOpener(View view) {
		this.view = view;
	}
	
	/**
	 * @see javax.swing.event.HyperlinkListener#hyperlinkUpdate(javax.swing.event.HyperlinkEvent)
	 */
	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == EventType.ACTIVATED) {
        	//System.err.println("*** event"+e+": ("+e.getDescription()+") type '"+e.getEventType()+"'");
        	//System.err.println("*** URL "+g.getURL());
            try {
            	openBufferForURL(e.getDescription());
            	// openBufferForURL(e.getDescription()); // TODO: make this work
                //pane.setPage(e.getURL());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
	}

	/**
	 * Opens a buffer at the required location, as represented by a pmjp URI.
	 * HACK: Needed since URL can't parse 'pjmp' at the moment.
	 */
	private void openBufferForURL(String theURL) {
		try {
			if (theURL.substring(0, ERROR_PROTOCOL.length()).equals(ERROR_PROTOCOL)) {
				theURL = theURL.replaceFirst("^"+ERROR_PROTOCOL, "http");
				openBufferForURL(new URL(theURL), false);
			}
		} catch (MalformedURLException e) {
			PolyMLPlugin.debugMessage("Could not create URL for "+theURL+".");
		}
		
		// some rudimentary string hacking to get the required bits out of our URI.
		/*
		ArrayList<String> parts = new ArrayList<String>(Arrays.asList(errorURI.split("[?&]")));
		// hmm. actually, the custom protocol might as well be optional...
		String file = parts.get(0).replace("^pmjp:" , "file:");
		parts.remove(0);
			
		if (file == null) return;
		*/		
	}
	
	/**
	 * Opens a buffer for the given URL.
	 * HACK: Needed since URL can't parse 'pjmp' at the moment.
	 * @param theUrl the URL to open in an editor pane.
	 * @param checkProtocol false to ignore if the protocol doesn't match.
	 */
	private void openBufferForURL(final URL theUrl, final boolean checkProtocol) {
		// Some basic checks.
		if (theUrl == null || (checkProtocol && !theUrl.getProtocol().equals(ERROR_PROTOCOL))) {
			return;
		}
		System.err.println("Attempting to open "+theUrl.toString()+"...");
		
		// TODO: set a search path for relative paths?  Important for, e.g. ./basis URLs.
		
		// Open the file.
		final Buffer buffer;
		final String file = theUrl.getPath();
		System.err.println(" - File is "+file+".");
		if (jEdit.getBuffer(file) == null) {
			buffer = jEdit.openFile(view,file);
			if (buffer == null) return;
		} else if (file != null) {
			buffer = jEdit.getBuffer(file);
		} else {
			buffer = null;
		}
		
		// no line or selection data; we can stop here.
		if (buffer == null || theUrl.getQuery() == null) {
			return;
		}
		
		// Now attempt to seek to the appropriate location.
		System.err.println(" - Attempting to parse arguments "+theUrl.getQuery()+".");
		// collect arguments
		final HashMap<String,String> args = new HashMap<String,String>(ErrorLinkOpener.args);
		final ArrayList<String> parts = new ArrayList<String>(Arrays.asList(theUrl.getQuery().split("&")));
		for (String p : parts) {
			String[] kv = p.split("=");
			try {
				if (args.containsKey(kv[0])) {
					args.put(kv[0], kv[1]);
				}
			} catch (NullPointerException e) {
				PolyMLPlugin.debugMessage(" -- Could not parse URI "+theUrl.toString()+"due to "+e+".");
				return;
			}
		}

		// now attempt to apply the changes
		JEditTextArea ta = view.getTextArea();
		int preferredCaretOffset = -1;
		Selection sel = null;
		if (args.containsKey("start") && args.containsKey("end")) {
			try {
				sel = PolyMLPlugin.compileMap.getSelectionFor(buffer, Integer.parseInt(args.get("start")), Integer.parseInt(args.get("end")));
				preferredCaretOffset = sel.getEnd();
			} catch (NumberFormatException e) {
				PolyMLPlugin.debugMessage("Failed to set selection for "+theUrl.toString()+" : "+e+".");
			}
		} else if (args.containsKey("line")) {
			try {
				preferredCaretOffset = ta.getLineStartOffset(Integer.parseInt(args.get("line")));
			} catch (NumberFormatException e) {
				PolyMLPlugin.debugMessage("Failed to set line number for "+theUrl.toString()+" : "+e+".");
			}
		}
		// finally, set horizontalOffset, caret and selection.  TODO: make at least the caret optional?
		if (preferredCaretOffset >= 0) {
			ta.setHorizontalOffset(ta.getScreenLineOfOffset(preferredCaretOffset));
			ta.setCaretPosition(preferredCaretOffset);
		}
		if (sel != null) {
			ta.setSelection(sel);
		}

	}
	
	/**
	 * Public facing version of {@link #openBufferForURL(URL, boolean)}
	 * @param theUrl the URL to check
	 */
	public void openBufferForURL(URL theUrl) {
		openBufferForURL(theUrl, true);
	}

	/**
	 * Turns an error into an error URI which might later be somehow interpreted.
	 * Transitional method; hopefully will not be needed when ErrorList dependency is removed.
	 * @see PolyMarkup#toURIString()
	 *
	public static String uriof(Error e) {
		StringBuffer uri = new StringBuffer();
		uri.append(ErrScheme); // jerr://
		uri.append(e.getFilePath()); // /path/to/file
		String sep = "?";
		if (e.getLineNumber() > 0) { // ?line=
			uri.append(sep+"line="+e.getLineNumber());
			sep = "&";
		}
		if (e.getStartOffset() > 0) { // [?&]start=
			uri.append(sep+"start="+e.getStartOffset());
			sep = "&";
			if (e.getEndOffset() > 0) { // [&]start=
				uri.append(sep+"end="+e.getEndOffset());
			}
		}
		return uri.toString();
	}*/
	
}
