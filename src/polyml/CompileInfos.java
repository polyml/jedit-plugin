package polyml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import org.gjt.sp.jedit.Buffer;
/*
 * Information about a compilation requests.
 */
public class CompileInfos { // implements EBComponent
	
	/** from buffer location (used as internal ID) to parse ID */
	private final Map<String, CompileRequest> filenameToReq;
	private final Map<String, CompileRequest> parseidToReq;
	private String lastRequestID;
	
	public CompileInfos(){
		// goes from filename to Compile Request for that filename
		filenameToReq = new Hashtable<>();
		// from parse id to the request (for looking up by parse id)
		parseidToReq = new Hashtable<>();
		lastRequestID = null;
	}

	public synchronized String getLastRequestID(){
		return lastRequestID;
	}
	
	/**
	 * Maintains consistency of buffers by putting compiles into both tables.
	 */
	private void storeCompileRequest(CompileRequest cr, String parseId) {
		parseidToReq.put(parseId, cr);
		filenameToReq.put(cr.fileName, cr);
	}
	
	/**
	 * Gets all stored compile requests.
	 */
	public Collection<CompileRequest> getRequests() {
		return filenameToReq.values();
	}
	
	/**
	 * Gets all valid compile results.
	 */
	public Collection<CompileResult> getResults() {
		Collection<CompileResult> out = new ArrayList<>();
		for (CompileRequest r : getRequests()) {
			if (r.getResult() != null) {
				out.add(r.getResult());
			}
		}
		return out;
	}

	/**
	 * called when a buffer is sent to be parsed
	 */
	public synchronized void compilingRequest(CompileRequest newRequest, String requestID) {
		// set last request ID
		lastRequestID = requestID;
		
		if (filenameToReq.containsKey(newRequest.fileName)) {
			CompileRequest prevRequest = filenameToReq.get(newRequest.fileName);
			prevRequest.freshRequest(newRequest, requestID);
			// add new compile request to parse Map
			parseidToReq.put(requestID, prevRequest);
		} else {
			// first time this buffer/location has been parsed, add it to the locMap.
			newRequest.sentParseID = requestID;
			storeCompileRequest(newRequest, requestID);
		}
	}
	
	/**
	 * Called when a result is received for parsing a buffer.
	 * Finds the request associated with this result, attaches the result to
	 * the request then returns the request with embedded result..
	 * @param r the compileresult, which is attached to the appropriate request.
	 * @return the request now attached to this result
	 * @see CompileRequest#result
	 */
	public synchronized CompileRequest compileCompleted(CompileResult r) {
		if(r.requestID.equals(lastRequestID)) {
			lastRequestID = null;
		}
		CompileRequest cr = parseidToReq.get(r.requestID);
		if(cr == null){
			System.err.println("compileCompleted: no known compile request for result: " + r.stringOfResult());
			return null;
		} else {
			cr.setResult(r);
			// update table from parse ID's to parse result, if we successfully parsed file.
			// any new messages will have the new parse current ID, 
			// which is already in the parse ID table
			//System.err.println("cr.filename: " + cr.fileName);
			//System.err.println("cr.sentParseID: " + cr.sentParseID + "; r.parseID: " + r.parseID);
			//System.err.println("cr.lastParseID: " + cr.lastParseID + "; cr.prevParseID: " + cr.prevParseID);
			if(cr.sentParseID.equals(r.parseID)) {
				if(cr.prevParseID != null) {
					parseidToReq.remove(cr.prevParseID);
				}
				cr.prevParseID = cr.lastParseID;
				cr.lastParseID = r.parseID;
				//System.err.println("cr.lastParseID2: " + cr.lastParseID + "; cr.prevParseID2: " + cr.prevParseID);
				
			} else { // parse failed; so no other message will refer to requestID... fixme: resent request???
				parseidToReq.remove(cr.sentParseID);
			}
			return cr;
		}
	}

	/* */
	public synchronized CompileRequest getFromPath(String path) {
		return filenameToReq.get(path);
	}
	public synchronized CompileRequest getFromBuffer(Buffer buffer) {
		return getFromPath(buffer.getPath());
	}
	
	public synchronized CompileRequest getFromParseID(String pid) {
		return parseidToReq.get(pid);
	}

	public synchronized void deleteAll() {
		filenameToReq.clear();
		parseidToReq.clear();
	}

	/* */
	public String parseIDOfBuffer(Buffer buffer) {
		CompileRequest cr = getFromBuffer(buffer);
		if(cr != null){
			return cr.lastParseID;
		} else {
			return null;
		}
	}
	
}
