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
	
	/**
	 * Parse Info for a single buffer
	 * @author ldixon
	 */
	public class BufferParseInfo {
		Buffer buffer; // buffer
		EditPane editPane; // edit pane containing buffer; or null if buffer is not in an edit pane.
		String sentParseID; // id of current version of buffer sent for compilation
		String lastParseID; // id of last parsed version of the buffer, same as curParseID if currently parsed
		String prevParseID; // id of previous parsed entry; so we can remove it from table when redundent
		CompileResult r; // last compile result
		
		/**
		 * Create a new parse info for a buffer in an edit pane. 
		 * Initially assumed to be unparsed. 
		 * 
		 * @param b
		 * @param p 
		 * @param pid id for this version of the parse
		 */
		public BufferParseInfo(Buffer b, EditPane p, String pid) {
			buffer = b;
			editPane = p;
			sentParseID = pid;
			lastParseID = null;
			prevParseID = null;
			r = null;
		}
	}
	
	
	public ParseInfo(){
		locMap = new Hashtable<String, BufferParseInfo>();
		parseMap = new Hashtable<String, BufferParseInfo>();
	}

	/**
	 * called when a buffer is sent ot be parsed
	 * @param b
	 * @param e
	 * @param rid
	 */
	public synchronized void parsingBuffer(Buffer b, EditPane e, String rid) {
		BufferParseInfo i = locMap.get(b.getPath());
		if(i == null){
			i = new BufferParseInfo(b, e, rid);
			locMap.put(i.buffer.getPath(), i);
		} else {
			i.sentParseID = rid;
			i.editPane = e;
		}
		parseMap.put(i.sentParseID, i);
	}
	
	/**
	 * called when a result is received for parsing a buffer
	 * @param r
	 * @return
	 */
	public synchronized BufferParseInfo parseComplete(CompileResult r) {
		BufferParseInfo i = parseMap.get(r.requestID);
		if(i == null){
			System.err.println("parsedBuffer: no such known buffer for compile result: " + r.stringOfResult());
			return null;
		} else {
			i.r = r;
			// update table from parse ID's to parse result, if we successfully parsed file.
			// any new messages will have the new parse current ID, 
			// which is already in the parse ID table
			System.err.println("i.sentParseID: " + i.sentParseID + "; r.parseID: " + r.parseID);
			if(i.sentParseID.equals(r.parseID)) {
				if(i.prevParseID != null) {
					parseMap.remove(i.prevParseID);
				}
				i.prevParseID = i.lastParseID;
				i.lastParseID = r.parseID;
				System.err.println("i.lastParseID: " + i.lastParseID + "; i.prevParseID: " + i.prevParseID);
			} else { // parse failed; so no other message will refer to requestID
				parseMap.remove(i.sentParseID);
			}
			return i;
		}
	}

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
	
}
