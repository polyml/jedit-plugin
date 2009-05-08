package polyml;

import java.io.File;

import org.gjt.sp.jedit.Buffer;

import errorlist.DefaultErrorSource;
import errorlist.ErrorSource;
import pushstream.PushStream;

public class PolyMarkupPushStream implements PushStream<PolyMarkup> {

	Buffer buffer; 
	DefaultErrorSource errorSource;
	String heap;
	String fileName;
	
	public PolyMarkupPushStream(DefaultErrorSource e) {
		buffer = null;
		errorSource = e;
	}
	
	public synchronized void setCompileInfo(String h, Buffer b) {
		System.err.println("setCompileInfo");
		buffer = b;
		heap = h;
	}
	
	public synchronized void add(PolyMarkup m) {
		if(buffer == null) {
			System.err.println("PolyMarkupPushStream:add: CompileInfo not setup");
			return;
		}
		
		fileName = buffer.getPath();
		CompileResult r = new CompileResult(heap, fileName, m);
		
		errorSource.removeFileErrors(fileName);
		if(r.isBug()) {
			errorSource.addError(new DefaultErrorSource.DefaultError(
					errorSource, ErrorSource.ERROR, fileName, 0,
					0, 0, "BUG: Failed to check using PolyML."));
		} else {
			if(r.status == CompileResult.STATUS_LOAD_FAILED) {
				errorSource.addError(new DefaultErrorSource.DefaultError(
						errorSource, ErrorSource.ERROR, fileName, 0,
						0, 0, "Failed to load heap: '" + r.heapName + "'"));
			}
			
			if(r.heapName == null) {
				errorSource.addError(new DefaultErrorSource.DefaultError(
						errorSource, ErrorSource.WARNING, fileName, 0,
						0, 0, "No heap file found."));
			}
			
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
				if(e.kind == PolyMLError.KIND_FATAL){
					errorKind = ErrorSource.ERROR;
				} else {
					errorKind = ErrorSource.WARNING;
				}
				
				errorSource.addError(new DefaultErrorSource.DefaultError(
						errorSource, errorKind, fileName, line,
						line_offset, end_offset, e.message));
			}
		}
	}

	public void add(PolyMarkup c, boolean isMore) { add(c); }

	public void close() { 
		// nothing special to do 
	}
		
}

