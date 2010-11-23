package polyml;

import java.util.Hashtable;
import java.util.Map;
import org.gjt.sp.jedit.Buffer;


/*
 * Information about a compilation requests.
 */
public class CompileInfos {
	
	/**
	 * from buffer location (used as internal ID) to parse ID
	 */
	Map<String, CompileRequest> filenameToReq;
	Map<String, CompileRequest> parseidToReq;
	String lastRequestID;
	
	public CompileInfos(){
		// goes from filename to Compile Request for that filename
		filenameToReq = new Hashtable<String, CompileRequest>();
		// from parse id to the request (for looking up by parse id)
		parseidToReq = new Hashtable<String, CompileRequest>();
		lastRequestID = null;
	}

	public synchronized String getLastCompileRequestID(){
		return lastRequestID;
	}
	
	/**
	 * called when a buffer is sent to be parsed
	 * @param b
	 * @param e
	 * @param rid
	 */
	public synchronized void compilingRequest(CompileRequest compileRequest, String requestID) {
		// set last request ID
		lastRequestID = requestID;
		
		CompileRequest cr = filenameToReq.get(compileRequest.fileName);
		// first time this buffer/location has been parsed, add it to the locMap. 
		
		if(cr == null){
			compileRequest.sentParseID = requestID;
			filenameToReq.put(compileRequest.fileName, compileRequest);
			// add new entry for old compileRequest
			parseidToReq.put(requestID, compileRequest);
		} else {
			cr.freshRequest(compileRequest, requestID);
			// add new compile request to parse Map
			parseidToReq.put(requestID, cr);
		}
	}
	
	/**
	 * called when a result is received for parsing a buffer
	 * @param r
	 * @return
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
			cr.result = r;
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
