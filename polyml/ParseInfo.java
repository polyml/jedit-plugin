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
		Buffer buffer;
		EditPane editPane;
		String lastParseID;
		CompileResult r;
		
		public BufferParseInfo(Buffer b, EditPane p, String pid) {
			buffer = b;
			editPane = p;
			lastParseID = pid;
			r = null;
		}
	}
	
	
	public ParseInfo(){
		locMap = new Hashtable<String, BufferParseInfo>();
		parseMap = new Hashtable<String, BufferParseInfo>();
	}

	public synchronized void parsingBuffer(Buffer b, EditPane e) {
		BufferParseInfo i = locMap.get(b.getPath());
		if(i == null){
			i = new BufferParseInfo(b, e, null);
			locMap.put(i.buffer.getPath(), i);
		} else {
			parseMap.remove(i.lastParseID);
			i.editPane = e;
		}
	}
	
	public synchronized BufferParseInfo parseComplete(CompileResult r) {
		BufferParseInfo i = locMap.get(r.requestID);
		if(i == null){
			System.err.println("parsedBuffer: no such known buffer for compile result: " + r.stringOfResult());
			return null;
		} else {
			i.r = r;
			i.lastParseID = r.parseID;
			parseMap.put(i.lastParseID, i);
			return i;
		}
	}

	public synchronized BufferParseInfo getFromPath(String path) {
		return locMap.get(path);
	}
	
	public synchronized BufferParseInfo getFromParseID(String pid) {
		return parseMap.get(pid);
	}

	public synchronized BufferParseInfo getFromBuffer(Buffer buffer) {
		return getFromPath(buffer.getPath());
	}

	public synchronized void deleteAll() {
		locMap.clear();
		parseMap.clear();
	}
	
}
