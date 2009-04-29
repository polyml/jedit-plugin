package polyml;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CompileResult {
	static char STATUS_SUCCESS = 'S';
	static char STATUS_LOAD_FAILED = 'L';
	static char STATUS_PARSE_FAILED = 'P';
	static char STATUS_TYPECHECK_FAILED = 'F';
	static char STATUS_EXCEPTION_RAISED = 'X';
	static char STATUS_BUG = 'B';
	
	char status;
	public int finalOffset;
	public List<PolyMLError> errors;
	public String heapName;
	
	public CompileResult(String heap, char st, int e, List<PolyMLError> errs){
		status = st;
		finalOffset = e;
		errors = errs;
		heapName = heap;
	}
	
	// dummy Compile Result for bugs. 
	public boolean isBug() {
		return status == STATUS_BUG;
	}
	
	public CompileResult(String heap, PolyMarkup m) {
		errors = new LinkedList<PolyMLError>();
		heapName = heap;
		if(m != null) {
			try {
				Iterator<PolyMarkup> i = m.getSubs().iterator();
				String s = i.next().getContent();
				if (s.length() != 1) {
					throw new MarkupException(
							"CompileResult:status char wrong size", m);
				}
				char c = s.charAt(0);
				if (c == STATUS_SUCCESS 
						|| c == STATUS_LOAD_FAILED
						|| c == STATUS_PARSE_FAILED
						|| c == STATUS_TYPECHECK_FAILED
						|| c == STATUS_EXCEPTION_RAISED) {
					status = c;

					s = i.next().getContent();
					finalOffset = Integer.parseInt(s);

					while (i.hasNext()) {
						PolyMarkup m2 = i.next();
						if (m2.getKind() == 'e') {
							errors.add(new PolyMLError(m2));
						} else {
							throw new MarkupException(
									"CompileResult:un-expected kind", m2);
						}
					}

				} else {
					throw new MarkupException("CompileResult:bad status", m);
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
		String s, statusString, heapString, finalOffsetString;
		s = new String();
		
		if (status == STATUS_SUCCESS) {
			statusString = "Compiled Successfully";
		} else if(status == STATUS_LOAD_FAILED) {
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
		
		s += statusString;
		
		if(heapName == null) {
			heapString = "No heap found, running with defaut Poly heap.";
		} else {
			heapString = "Running from heap: " + heapName;
		}
		
		s += "\n" + heapString + "";
		
		finalOffsetString = "Got to: " + finalOffset + "\n\n";
		
		s += finalOffsetString;
		
		for(PolyMLError e : errors) {
			s +=  "\n" + e.stringOfError();
		}
		
		return s;
	}
}
	