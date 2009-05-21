package polyml;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CompileResult {
	static char STATUS_SUCCESS = 'S';
	static char STATUS_PRELUDE_FAILED = 'L';
	static char STATUS_PARSE_FAILED = 'P';
	static char STATUS_TYPECHECK_FAILED = 'F';
	static char STATUS_EXCEPTION_RAISED = 'X';
	static char STATUS_CANCEL = 'C';
	static char STATUS_BUG = 'B';
	
	char status;
	public int finalOffset;
	public String requestID;
	public String parseID;
	public List<PolyMLError> errors;
	
	public CompileResult(String rid, String pid, String filename, char st, int e, List<PolyMLError> errs){
		status = st;
		finalOffset = e;
		errors = errs;
		requestID = rid;
		parseID = pid;
	}
	
	// dummy Compile Result for bugs. 
	public boolean isBug() {
		return status == STATUS_BUG;
	}
	
	public CompileResult(PolyMarkup m) {
		errors = new LinkedList<PolyMLError>();
		
		if(m != null) {
			try {
				Iterator<PolyMarkup> i = m.getSubs().iterator();
				
				// get IDE id
				requestID = i.next().getContent();
				
				// get ML parse id
				parseID = i.next().getContent();
				
				// get result status
				String s = i.next().getContent();
				if (s.length() != 1) {
					throw new MarkupException(
							"CompileResult:status char wrong size", m);
				}
				status = s.charAt(0);
				if (! (status == STATUS_SUCCESS 
						|| status == STATUS_PRELUDE_FAILED
						|| status == STATUS_PARSE_FAILED
						|| status == STATUS_TYPECHECK_FAILED
						|| status == STATUS_EXCEPTION_RAISED  
						|| status == STATUS_CANCEL)){
					throw new MarkupException("CompileResult:bad status", m);
				}

				// get offset to parsed point
				s = i.next().getContent();
				// convert ML minus to standard minus, so that integer can be parsed. 
				String s2 = s.replace('~', '-');
				finalOffset = Integer.parseInt(s2);
				
				if (status == STATUS_EXCEPTION_RAISED) {
					String exception_message = i.next().getContent();
					errors.add(new PolyMLError(PolyMLError.KIND_EXCEPTION, finalOffset, finalOffset, exception_message));
				} else if (status == STATUS_PRELUDE_FAILED) {
					String error_reason = i.next().getContent();
					errors.add(new PolyMLError(PolyMLError.KIND_PRELUDE_FAILURE, 0, 0, error_reason));
				} else if(status == STATUS_PARSE_FAILED
						|| status == STATUS_TYPECHECK_FAILED) {
					while (i.hasNext()) {
						PolyMarkup m2 = i.next();
						if (m2.getKind() == 'E') {
							errors.add(new PolyMLError(m2));
						} else {
							throw new MarkupException(
									"CompileResult:un-expected kind: " + m2.getKind(), m2);
						}
					}
				}


				
			} catch (MarkupException e) {
				e.printMarkupException();
				// System.err.println(e.toString());
				status = STATUS_BUG;
				finalOffset = 0;
			}
		} else { // if given null markup, it was a bug
			System.err.println("CompileResult: Given null markup.");
			status = STATUS_BUG;
			finalOffset = 0;
		}
	}

	public String stringOfResult() {
		String s, statusString, finalOffsetString;
		s = new String();
		
		if (status == STATUS_SUCCESS) {
			statusString = "Compiled Successfully";
		} else if(status == STATUS_PRELUDE_FAILED) {
			statusString = "Failed to load heap";
		} else if(status == STATUS_PARSE_FAILED) {
			statusString = "Parse Error(s)";
		} else if(status == STATUS_TYPECHECK_FAILED) {
			statusString = "Type Error(s)";
		} else if(status == STATUS_EXCEPTION_RAISED) {
			statusString = "Exception raised";
		} else if(status == STATUS_BUG) {
			statusString = "Bug";
		} else {
			statusString = "Unkown bad/status: " + status;
		}
		
		s += "\n* Status: " + statusString + "\n";
		
		s += "\nrequest ID: " + requestID + "\n";
		s += "\nparse ID: " + parseID + "\n";
				
		finalOffsetString = "Checked file up to: " + finalOffset + "\n\n";
		
		s += finalOffsetString;
		
		for(PolyMLError e : errors) {
			s +=  "\n" + e.stringOfError();
		}
		
		return s;
	}

	public boolean isSuccess() {
		return (status == STATUS_SUCCESS);
	}
}
	