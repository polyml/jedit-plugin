package polyml;

import java.util.Iterator;

public class PolyMLError {
	static char KIND_EXCEPTION = 'X';
	static char KIND_PRELUDE_FAILURE = 'L';
	static char KIND_FATAL = 'E';
	static char KIND_WARNING = 'W';
	
	int kind;
	public int startPos;
	public int endPos;
	public String message;
	public String fileName;
	public String randomValue;
	
	private PolyMLError(char k, int s, int e, String m){
		kind = k;
		startPos = s;
		endPos = e;
		message = m;
		fileName = null;
		randomValue = null;
	}
	
	/**
	 * Static constructor for exception messages
	 * @param startOffset
	 * @param finalOffset
	 * @param exception_message
	 * @return
	 */
	static public PolyMLError newExceptionError(int startOffset, int finalOffset, String exception_message) {
		return new PolyMLError(KIND_EXCEPTION, startOffset, finalOffset, exception_message);
	}
	
	/**
	 * Static constructor for prelude messages
	 * @param exception_message
	 * @return
	 */
	static public PolyMLError newPreludeError(String exception_message) {
		return new PolyMLError(KIND_PRELUDE_FAILURE, 0, 0, exception_message);
	}
	
	/**
	 * Generic constructor for ML Markup messages
	 * @param m
	 * @throws MarkupException
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
}