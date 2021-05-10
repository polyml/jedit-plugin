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

/**
 * Opens hyperlinks which follow a plugin-internal URL scheme
 */
public class ErrorLinkOpener implements HyperlinkListener {

	/** The recognised protocol string. */
	public static final String ERROR_PROTOCOL = "pmjp";
	/** All possible query arguments for an error URL */
	private static final HashMap<String,Integer> intArgs = new HashMap<String, Integer>(){{
		put("line", null);
		put("start", null);
		put("end", null);
	}};
	
	private final View view;
		
	/**
	 * A new link opener. 
	 * @param view the JEdit view in which to perform buffer operations.
	 */
	public ErrorLinkOpener(View view) {
		this.view = view;
	}
	
	/**
	 * Process clicks and other events which request the opening of a hyperlink.
	 * @see javax.swing.event.HyperlinkListener#hyperlinkUpdate(javax.swing.event.HyperlinkEvent)
	 */
	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == EventType.ACTIVATED) {
            try {
            	openBufferForURL(e.getDescription());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
	}

	/**
	 * Opens a buffer at the specified location, as specifically as can be specified.
	 * @param file the file path.  mandatory.
	 * @param line the line number.  optional.
	 * @param start the (file-absolute) selection start-offset.  optional.
	 * @param end the (file-absolute) selection end-offset.  optional.
	 */
	private void openBufferAt(String file, Integer line, Integer start, Integer end) {

		final Buffer buffer;
		System.err.println(" - File is "+file+".");
		
		// TODO: set a search path for relative paths?  Important for, e.g. ./basis URLs.
		if (jEdit.getBuffer(file) == null) {
			buffer = jEdit.openFile(view,file);
			if (buffer == null) return;
		} else if (file != null) {
			buffer = jEdit.getBuffer(file);
		} else {
			buffer = null;
		}
		
		// no line or selection data; we can stop here.
		if (buffer == null) return;
		
		// Now attempt to seek to the appropriate location.
		JEditTextArea ta = view.getTextArea();
		int preferredCaretOffset = -1;
		Selection sel = null;
		if (start != null) {
			if (end == null) end = start;
			try {
				CompileInfos c = PolyMLPlugin.polyMLProcess.compileInfos;
				sel = c.getFromPath(file).getResult().getRealPositionOf(start, end);
				preferredCaretOffset = sel.getEnd();
			} catch (NumberFormatException | NullPointerException e) {
				PolyMLPlugin.debugMessage("Failed to set selection for "+file+": "+e+".");
			}
		} else if (line != null) {
			try {
				preferredCaretOffset = ta.getLineStartOffset(line);
			} catch (NullPointerException | NumberFormatException e) {
				PolyMLPlugin.debugMessage("Failed to set line number for "+file+": "+e+".");
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
	 * Opens a buffer for the given URL.
	 * @param theUrl the URL to open in an editor pane.
	 * @param checkProtocol false to ignore if the protocol doesn't match. HACK: Needed since URL can't parse 'pjmp' at the moment.
	 */
	private void openBufferForURL(final URL theUrl, final boolean checkProtocol) {
		// Some basic checks.
		if (theUrl == null || (checkProtocol && !theUrl.getProtocol().equals(ERROR_PROTOCOL))) {
			return;
		}
		System.err.println("Attempting to open "+ theUrl +"...");

		// Open the file.
		final String file = theUrl.getPath();
		final HashMap<String,Integer> theArgs = new HashMap<>(ErrorLinkOpener.intArgs);

		// collect query arguments
		if (theUrl.getQuery() != null) {
			final ArrayList<String> parts = new ArrayList<>(Arrays.asList(theUrl.getQuery().split("&")));
			for (String p : parts) {
				String[] kv = p.split("=");
				try {
					if (theArgs.containsKey(kv[0])) {
						theArgs.put(kv[0], Integer.parseInt(kv[1]));
					}
				} catch (NullPointerException e) {
					PolyMLPlugin.debugMessage(" -- Could not parse "+kv[0]+" argument for "+ theUrl + " (" + e + ").");
					break;
				}
			}
		}
		openBufferAt(file, theArgs.get("line"), theArgs.get("start"), theArgs.get("end"));
	}
	
	/**
	 * Opens a buffer at the required location, as represented by a pmjp URI.
	 * HACK: Needed since URL can't parse 'pjmp' at the moment.
	 */
	private void openBufferForURL(String theURL) {
		try {
			if (theURL.startsWith(ERROR_PROTOCOL)) {
				theURL = theURL.replaceFirst("^"+ERROR_PROTOCOL, "http");
				openBufferForURL(new URL(theURL), false);
			}
		} catch (MalformedURLException e) {
			PolyMLPlugin.debugMessage("Could not create URL for "+theURL+".");
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
	 * Opens a buffer pointing at the location of the given error.
	 * @param e the PolyMLError to process.
	 */
	public void openBufferForError(PolyMLError e) {
		openBufferAt(e.fileName, null, e.startPos, e.endPos);
	}
	
	/**
	 * Turns a FlexibleLocationInfo into a URI. 
	 * Uses the static, original locations of the flexiblelocationinfo
	 * since this is going to be used to retrieve the actual location at
	 * click-time.
	 */
	public static String uriof(FlexibleLocationInfo l) {
		StringBuilder uri = new StringBuilder();
		uri.append(ERROR_PROTOCOL+"://"); // jerr://
		uri.append(l.buffer.getPath());
		String sep = "?";
		// not sure we care about the line just now.
		if (l.getLineNumber() != null) { // ?line=
			uri.append(sep+"line="+l.getLineNumber());
			sep = "&";
		}
		uri.append(sep+"start="+l.startPos);
		sep = "&";
		if (l.endPos > l.startPos) { // [&]start=
			uri.append(sep+"end="+l.endPos);
		} else {
			// is this what we want?
			uri.append(sep+"end="+l.startPos+1);
		}
		return uri.toString();
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
