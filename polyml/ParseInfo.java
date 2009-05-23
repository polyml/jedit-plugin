package polyml;

import java.util.Hashtable;
import java.util.Map;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;

public class ParseInfo {
	
	/**
	 * from buffer location (used as internal ID) to parse ID
	 */
	Map<String, BufferParseInfo> locMap;
	Map<String, BufferParseInfo> parseMap;
	String lastRequestID;
	Thread waitingThread;
	
	/**
	 * Parse Info for a single buffer
	 * @author ldixon
	 */
	public class BufferParseInfo {
		//Buffer buffer; // buffer
		//EditPane editPane; // edit pane containing buffer; or null if buffer is not in an edit pane.
		String sentParseID; // id of current version of buffer sent for compilation
		String lastParseID; // id of last parsed version of the buffer, same as curParseID if currently parsed
		String prevParseID; // id of previous parsed entry; so we can remove it from table when redundent
		CompileResult r; // last compile result
		String fileName;
		
		/**
		 * Create a new parse info for a buffer in an edit pane. 
		 * Initially assumed to be unparsed. 
		 * 
		 * @param b
		 * @param p 
		 * @param pid id for this version of the parse
		 */
		public BufferParseInfo(String f, String pid) {
		//public BufferParseInfo(Buffer b, EditPane p, String pid) {
			//buffer = b;
			//editPane = p;
			fileName = f;
			sentParseID = pid;
			lastParseID = null;
			prevParseID = null;
			r = null;
		}
	}
	
	
	public ParseInfo(){
		locMap = new Hashtable<String, BufferParseInfo>();
		parseMap = new Hashtable<String, BufferParseInfo>();
		waitingThread = null;
		lastRequestID = null;
	}

	public synchronized String getLastCompileRequestID(){
		return lastRequestID;
	}
	
	/**
	 * called when a buffer is sent ot be parsed
	 * @param b
	 * @param e
	 * @param rid
	 */
	public synchronized void parsingBuffer(String fileName, String rid) {
		// Buffer b, EditPane e,
		lastRequestID = rid;
		//BufferParseInfo i = locMap.get(b.getPath());
		BufferParseInfo i = locMap.get(fileName);
		if(i == null){
			i = new BufferParseInfo(fileName, rid);
			//locMap.put(i.buffer.getPath(), i);
			locMap.put(fileName, i);
		} else {
			i.sentParseID = rid;
			//i.editPane = e;
		}
		parseMap.put(i.sentParseID, i);
	}
	
	synchronized void wakeSleepingThread() {
		if(waitingThread != null) {
			waitingThread.notify();
			waitingThread = null;
		}
	}
	
	/**
	 * called when a result is received for parsing a buffer
	 * @param r
	 * @return
	 */
	public synchronized BufferParseInfo parseComplete(CompileResult r) {
		if(r.requestID.equals(lastRequestID)) {
			wakeSleepingThread();
			lastRequestID = null;
		}
		BufferParseInfo i = parseMap.get(r.requestID);
		if(i == null){
			System.err.println("parsedBuffer: no such known buffer for compile result: " + r.stringOfResult());
			wakeSleepingThread();
			return null;
		} else {
			i.r = r;
			// update table from parse ID's to parse result, if we successfully parsed file.
			// any new messages will have the new parse current ID, 
			// which is already in the parse ID table
			//System.err.println("i.sentParseID: " + i.sentParseID + "; r.parseID: " + r.parseID);
			if(i.sentParseID.equals(r.parseID)) {
				if(i.prevParseID != null) {
					parseMap.remove(i.prevParseID);
				}
				i.prevParseID = i.lastParseID;
				i.lastParseID = r.parseID;
				//System.err.println("i.lastParseID: " + i.lastParseID + "; i.prevParseID: " + i.prevParseID);
			} else { // parse failed; so no other message will refer to requestID
				parseMap.remove(i.sentParseID);
			}
			return i;
		}
	}

	/* */
	public synchronized BufferParseInfo getFromPath(String path) {
		return locMap.get(path);
	}
	public synchronized BufferParseInfo getFromBuffer(Buffer buffer) {
		return getFromPath(buffer.getPath());
	}
	
	public synchronized BufferParseInfo getFromParseID(String pid) {
		return parseMap.get(pid);
	}

	public synchronized void deleteAll() {
		locMap.clear();
		parseMap.clear();
	}

	/* */
	public String parseIDOfBuffer(Buffer buffer) {
		BufferParseInfo pInfo = getFromBuffer(buffer);
		if(pInfo != null){
			return pInfo.lastParseID;
		} else {
			return null;
		}
	}

	/* IMPROVE: lastRequestID gets set by both compile and by this */
	public void notifyOnCompileResult(String requestid, Thread currentThread) {
		lastRequestID = requestid;
		waitingThread = currentThread;
	}
	
}
