package polyml;

import java.io.File;
import java.util.Iterator;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.TextArea;

import errorlist.DefaultErrorSource;
import errorlist.ErrorSource;
import polyml.ParseInfo.BufferParseInfo;
import pushstream.PushStream;

public class PolyMarkupPushStream implements PushStream<PolyMarkup> {

	ParseInfo parseInfo;
	DefaultErrorSource errorSource;

	public PolyMarkupPushStream(DefaultErrorSource e, ParseInfo p) {
		errorSource = e;
		parseInfo = p;
	}
	
	/**
	 * create initial location response from PolyMarkup
	 */
	class LocationResponse {
		String parseID;
		String reqID;
		int start;
		int end;
		Iterator<PolyMarkup> markup;
		
		public LocationResponse(PolyMarkup m){
			markup = m.getSubs().iterator();
			reqID = markup.next().getContent();
			parseID = markup.next().getContent();
			start = Integer.parseInt(markup.next().getContent());
			end = Integer.parseInt(markup.next().getContent());
		}
	}
	
	/**
	 * initial location followed by a file and a location in that file. 
	 */
	class FullLocationResponse extends LocationResponse {
		
		String filenameLoc;
		int lineLoc; 
		int startLoc;
		int endLoc;
		
		public FullLocationResponse(PolyMarkup m){
			super(m);
			filenameLoc = markup.next().getContent();
			lineLoc = Integer.parseInt(markup.next().getContent());
			startLoc = Integer.parseInt(markup.next().getContent());
			endLoc = Integer.parseInt(markup.next().getContent());
		}
	}
	
	public synchronized void add(PolyMarkup m) {
		PolyMLPlugin.debugMessage("\n\n"); 
		// to make output buffer more readable; add new lines after each bit of markup is successfully added. 
		
		if(m.kind == PolyMarkup.INKIND_COMPILE) {
			CompileResult r = new CompileResult(m);
			
			BufferParseInfo i = parseInfo.parseComplete(r);
			
			String fileName = i.buffer.getPath();
			Buffer buffer = i.buffer;
			
			errorSource.removeFileErrors(fileName);
			if(r.isBug()) {
				errorSource.addError(new DefaultErrorSource.DefaultError(
						errorSource, ErrorSource.ERROR, fileName, 0,
						0, 0, "BUG: Failed to check using PolyML."));
			} else {
				for (PolyMLError e : r.errors) {
					
					System.err.println("PolyMarkupPushStream: " + e.startPos + ":" + e.endPos);
					
					int line = buffer.getLineOfOffset(e.startPos);
					int line_offset = e.startPos - buffer.getLineStartOffset(line);
					int end_line = buffer.getLineOfOffset(e.endPos);
					int end_offset = 0;
					if (end_line == line) {
						end_offset = e.endPos - buffer.getLineStartOffset(end_line);
					}
					
					int errorKind;
					if(e.kind == PolyMLError.KIND_FATAL || e.kind == PolyMLError.KIND_EXCEPTION){
						errorKind = ErrorSource.ERROR;
					} else {
						errorKind = ErrorSource.WARNING;
					}
					
					errorSource.addError(new DefaultErrorSource.DefaultError(
							errorSource, errorKind, fileName, line,
							line_offset, end_offset, e.message));
				}
			}
		} else if(m.kind == PolyMarkup.INKIND_PROPERTIES) {
			LocationResponse l = new LocationResponse(m);
			BufferParseInfo pInfo = parseInfo.getFromParseID(l.parseID);
			pInfo.editPane.getTextArea().setSelection(new Selection.Range(l.start,l.end));
		} else if(m.kind == PolyMarkup.INKIND_TYPE_INFO) {
			LocationResponse l = new LocationResponse(m);
			BufferParseInfo pInfo = parseInfo.getFromParseID(l.parseID);
			String type_string = l.markup.next().getContent();
			
			pInfo.editPane.getTextArea().setSelection(new Selection.Range(l.start,l.end));
			
			Buffer buffer = pInfo.editPane.getBuffer();
			int line = buffer.getLineOfOffset(l.start);
			int line_offset = l.start - buffer.getLineStartOffset(line);
			int end_line = buffer.getLineOfOffset(l.end);
			int end_offset = 0;
			if (end_line == line) {
				end_offset = l.end - buffer.getLineStartOffset(end_line);
			}
			
			errorSource.addError(new DefaultErrorSource.DefaultError(
					errorSource, ErrorSource.WARNING, buffer.getPath(), line,
					line_offset, end_offset, type_string));
			
			System.err.println("PolyMarkupPushStream.add: Not yet fully implemented kind: " + m.kind);
		} 
		// Location responses
		else if(m.kind == PolyMarkup.INKIND_LOC_DECLARED) {
			LocationResponse l = new FullLocationResponse(m);
			BufferParseInfo pInfo = parseInfo.getFromParseID(l.parseID);
			pInfo.editPane.getTextArea().setSelection(new Selection.Range(l.start,l.end));
			// FIXME: complete. 
			System.err.println("PolyMarkupPushStream.add: Not yet fully implemented kind: " + m.kind);
		} else if(m.kind == PolyMarkup.INKIND_LOC_OF_PARENT_STRUCT) {
			LocationResponse l = new FullLocationResponse(m);
			BufferParseInfo pInfo = parseInfo.getFromParseID(l.parseID);
			pInfo.editPane.getTextArea().setSelection(new Selection.Range(l.start,l.end));
			// FIXME: complete. 
			System.err.println("PolyMarkupPushStream.add: Not yet fully implemented kind: " + m.kind);
		} else if(m.kind == PolyMarkup.INKIND_LOC_WHERE_OPENED) {
			LocationResponse l = new FullLocationResponse(m);
			BufferParseInfo pInfo = parseInfo.getFromParseID(l.parseID);
			pInfo.editPane.getTextArea().setSelection(new Selection.Range(l.start,l.end));
			// FIXME: complete. 
			System.err.println("PolyMarkupPushStream.add: Not yet fully implemented kind: " + m.kind);
		} 
		// movement responses (all the same)
		else if(m.kind == PolyMarkup.INKIND_MOVE_TO_FIRST_CHILD) {
			LocationResponse l = new LocationResponse(m);
			BufferParseInfo pInfo = parseInfo.getFromParseID(l.parseID);
			pInfo.editPane.getTextArea().setSelection(new Selection.Range(l.start,l.end));
		} else if(m.kind == PolyMarkup.INKIND_MOVE_TO_NEXT) {
			LocationResponse l = new LocationResponse(m);
			BufferParseInfo pInfo = parseInfo.getFromParseID(l.parseID);
			pInfo.editPane.getTextArea().setSelection(new Selection.Range(l.start,l.end));
		} else if(m.kind == PolyMarkup.INKIND_MOVE_TO_PARENT) {
			LocationResponse l = new LocationResponse(m);
			BufferParseInfo pInfo = parseInfo.getFromParseID(l.parseID);
			pInfo.editPane.getTextArea().setSelection(new Selection.Range(l.start,l.end));
		} else if(m.kind == PolyMarkup.INKIND_MOVE_TO_PREVIOUS) {
			LocationResponse l = new LocationResponse(m);
			BufferParseInfo pInfo = parseInfo.getFromParseID(l.parseID);
			pInfo.editPane.getTextArea().setSelection(new Selection.Range(l.start,l.end));
		} 
	}

	public void add(PolyMarkup c, boolean isMore) { add(c); }

	public void close() { 
		// nothing special to do 
	}
		
}

