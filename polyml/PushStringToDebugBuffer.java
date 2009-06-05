package polyml;

import pushstream.PushStream;

/**
 * push stream string class that can be called by other threads to add a string to the
 * debug buffer
 * 
 * @author ldixon
 */
public class PushStringToDebugBuffer implements PushStream<String> {

	public PushStringToDebugBuffer() {
	}

	public void add(String s) { 
		//System.err.println("PushStringToDebugBuffer.add:" + s);
		PolyMLPlugin.debugMessage(s);
	}

	public void add(String s, boolean isMore) {
		add(s);
	}

	public void close() {
		PolyMLPlugin.debugMessage("<EOF>");
	}

}
