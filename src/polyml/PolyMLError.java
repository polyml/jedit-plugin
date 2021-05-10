package polyml;

import java.util.Iterator;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;

//enum PolyErrorKind {
//	KIND_EXCEPTION('X', "Exception"),
//	KIND_PRELUDE_FAILURE('L', "Heap loading failed:"),
//	KIND_FATAL('E', "Error"),
//	KIND_WARNING('W', "Warning");
//	private char tag;
//	private String desc;
//	PolyErrorKind(char tag, String desc) { this.tag = tag; this.desc = desc; }
//	public String toString() { return String.valueOf(tag); }
//  public String description() { return this.desc; }
//}

/**
 * A PolyML error.
 * So that logical and physical error positions can be connected in the editor
 */
public class PolyMLError extends FlexibleLocationInfo {
	static char KIND_EXCEPTION = 'X';
	static char KIND_PRELUDE_FAILURE = 'L';
	static char KIND_FATAL = 'E';
	static char KIND_WARNING = 'W';
	
	int kind;
	int startPos;
	int endPos;
	public String message;
	public String fileName;
	public String randomValue;
	
	protected PolyMLError(char kind, int startPos, int endPos, String msg){
		this.kind = kind;
		this.startPos = startPos;
		this.endPos = endPos;
		this.message = msg;
		fileName = null;
		randomValue = null;
	}
	
	/**
	 * Constructor for exception messages
	 */
	public PolyMLError(int startOffset, int finalOffset, String exception_message) {
		this(KIND_EXCEPTION, startOffset, finalOffset, exception_message);
	}
	
	/**
	 * Constructor for prelude messages
	 */
	public PolyMLError(String exception_message) {
		this(KIND_PRELUDE_FAILURE, 0, 0, exception_message);
	}
	
	/**
	 * Generic constructor for ML Markup messages
	 */
	public PolyMLError(PolyMarkup m) throws MarkupException{
		Iterator<PolyMarkup> i = m.getSubs().iterator();
		String s = i.next().getContent();
		if(s.length() != 1){ throw new MarkupException("PolyMLError: bad error kind length", m); }
		char c = s.charAt(0);
		if(c == KIND_FATAL || c == KIND_WARNING) {
			kind = c;
			
			fileName = i.next().getContent();
			
			randomValue = i.next().getContent();
			
			startPos = Integer.parseInt(i.next().getContent());
			
			endPos = Integer.parseInt(i.next().getContent());
			
			PolyMarkup m2 = i.next();
			
			System.err.println("MARKUP DEBUG!: " + m2.toXMLString() );
			
			m2.recChangeLocationFieldsToHTML(); // to make nicer output for ref locations
			m2.recFlattenDefaultFieldsToContent(); // anything else gets XML tags. 
			message = m2.getContent();
			
			if(message == null) {
				throw new MarkupException("PolyMLError: null message given", m);
			}
		} else {
			throw new MarkupException("PolyMLError: bad error kind", m);
		}
		
	}
	
	public String stringOfError() {
		String statusString, locationString;
	
		if(kind == KIND_FATAL) {
			statusString = "Error:";
		} else if(kind == KIND_WARNING) {
			statusString = "Warning:";
		} else if(kind == KIND_PRELUDE_FAILURE) {
			statusString = "Heap loading failed:";
		} else if(kind == KIND_EXCEPTION) {
			statusString = "Exception:";
		} else {
			statusString = "Unkown Status: " + kind;
		}
		
		locationString = " in: " + startPos + "-" + endPos;
		
		return statusString + locationString + "\n" + message + "\n\n";
	}
	
	public boolean isFatal(){
		return (kind == KIND_FATAL || kind == KIND_EXCEPTION || kind == KIND_PRELUDE_FAILURE);
	}
	
	/** Returns the start position of the error */
	public int getStartPos() {
		return startPos;
	}
	
	/** Returns the end position of the error. */
	public int getEndPos() {
		return endPos;
	}
	
	/**
	 * Utility method to associage all errors in a compileresult
	 * with the given buffer.
	 * @param r the result whose errors we must associate.
	 */
	static void associateAllErrors(CompileResult r, String filename) {
		Buffer b = jEdit.getBuffer(filename);
		if (b != null && r != null) {
			for (PolyMLError e : r.errors) {
				e.associateWithBuffer(b);
			}
		}
	}
	
}