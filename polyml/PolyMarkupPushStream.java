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
	
	public synchronized void add(PolyMarkup m) {
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
		} else if(m.kind == PolyMarkup.INKIND_DEC_LOCATION) {
			System.err.println("PolyMarkupPushStream.add: Not yet implemented kind: " + m.kind);
		} else if(m.kind == PolyMarkup.INKIND_LOC_OF_PARENT_STRUCT) {
			System.err.println("PolyMarkupPushStream.add: Not yet implemented kind: " + m.kind);
		} else if(m.kind == PolyMarkup.INKIND_LOC_WHERE_OPENED) {
			System.err.println("PolyMarkupPushStream.add: Not yet implemented kind: " + m.kind);
		} else if(m.kind == PolyMarkup.INKIND_MOVE_TO_FIRST_CHILD) {
			System.err.println("PolyMarkupPushStream.add: Not yet implemented kind: " + m.kind);
		} else if(m.kind == PolyMarkup.INKIND_MOVE_TO_NEXT) {
			System.err.println("PolyMarkupPushStream.add: Not yet implemented kind: " + m.kind);
		} else if(m.kind == PolyMarkup.INKIND_MOVE_TO_PARENT) {
			System.err.println("PolyMarkupPushStream.add: Not yet implemented kind: " + m.kind);
		} else if(m.kind == PolyMarkup.INKIND_MOVE_TO_PREVIOUS) {
			System.err.println("PolyMarkupPushStream.add: Not yet implemented kind: " + m.kind);
		} else if(m.kind == PolyMarkup.INKIND_PROPERTIES) {
			Iterator<PolyMarkup> i = m.getSubs().iterator();
			@SuppressWarnings("unused")
			String request_id = i.next().getContent();
			String parse_id = i.next().getContent();
			int start = Integer.parseInt(i.next().getContent());
			int end = Integer.parseInt(i.next().getContent());
			
			BufferParseInfo pInfo = parseInfo.getFromParseID(parse_id);
			
			pInfo.editPane.getTextArea().setSelection(new Selection.Range(start,end));
			//System.err.println("PolyMarkupPushStream.add: Not yet implemented kind: " + m.kind);
		} else if(m.kind == PolyMarkup.INKIND_TYPE_INFO) {
			
		}
	}

	public void add(PolyMarkup c, boolean isMore) { add(c); }

	public void close() { 
		// nothing special to do 
	}
		
}

