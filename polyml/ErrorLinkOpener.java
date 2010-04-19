package polyml;

import java.awt.Cursor;
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
			System.err.println("Attempting to replace "+theURL);
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
		private void openBufferForURL(URL theUrl, boolean checkProtocol) {
		if (theUrl == null) {
			PolyMLPlugin.debugMessage("Refusing to process null URL.");
			return;
		} else if (checkProtocol && !theUrl.getProtocol().equals(ERROR_PROTOCOL)) {
			PolyMLPlugin.debugMessage("Refusing to process URL with "+theUrl.getProtocol()+".");
			return;
		}
		// Open the file.
		final Buffer buffer;
		String file = theUrl.getPath();
		if (jEdit.getBuffer(file) == null) {
			buffer = jEdit.openFile(view,file);
			if (buffer == null) return;
		} else if (file != null) {
			buffer = jEdit.getBuffer(file);
		} else {
			PolyMLPlugin.debugMessage("Refusing to process URL "+theUrl.toString()+" with no file part!");
			return;
		}
		
		// Now attempt to seek to the appropriate location.
		if (theUrl.getQuery() == null) {
			// no line or selection data; we can stop here.
			return;
		}
		
		// process arguments
		HashMap<String,String> args = new HashMap<String,String>(ErrorLinkOpener.args);
		
		ArrayList<String> parts = new ArrayList<String>(Arrays.asList(theUrl.getQuery().split("&")));
		for (String p : parts) {
			String[] kv = p.split("=");
			try {
				if (args.containsKey(kv[0])) {
					args.put(kv[0], kv[1]);
				} else {
					PolyMLPlugin.debugMessage("Found unexpected pmjp argument "+kv[0]+".");
				}
			} catch (NullPointerException e) {
				PolyMLPlugin.debugMessage("Could not parse URI "+theUrl.toString()+"due to "+e+".");
			} 
		}
		
		if (args.containsKey("line")) {
			try {
				buffer.setProperty(Buffer.SCROLL_VERT, Integer.parseInt(args.get("line")));
			} catch (NumberFormatException e) {
				PolyMLPlugin.debugMessage("Failed to set line number for "+theUrl.toString()+" : "+e+".");
			} 
		} if (args.containsKey("start") && args.containsKey("end")) {
			try {
				buffer.setProperty( Buffer.SELECTION, new Range(Integer.parseInt(args.get("start")), Integer.parseInt(args.get("end"))) );
			} catch (NumberFormatException e) {
				PolyMLPlugin.debugMessage("Failed to set selection for "+theUrl.toString()+" : "+e+".");
			} 
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
