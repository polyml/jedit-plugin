package pushstream;

import java.util.Date;

/**
 * 
 * a character push stream that sends strings on to a string push stream. 
 * It does this when a time limit is hit, or when no more data is waiting to be pushed. 
 * 
 * @author ldixon
 *
 */
public class TimelyCharToStringStream implements PushStream<Character>{

	int timeLimit;
	Long pushTime;
	String pushString;
	PushStream<String> stream;
	
	public TimelyCharToStringStream(PushStream<String> s, int t) {
		timeLimit = t;
		pushString = "";
		stream = s;
		pushTime = null;
	}
	
	public void resetPushTime() {
		pushTime = (new Date()).getTime() + timeLimit;
	}
	
	public void add(Character c, boolean isMore) {
		// add character to string
		pushString += c;
		//System.err.println("TimelyCharToStringStream.add: " + c);
		
		// if there was delay set before, make the new push time
		if(! isMore) { // no more data waiting, push this string on. 
			//System.err.println("no more: \'" + pushString + "\'");
			stream.add(pushString);
			pushString = "";
			pushTime = null;
		} else if(pushTime == null) { // no time limit set, set new time limit, no push
			//System.err.println("resetting push time... ");
			resetPushTime(); 
			//System.err.println("reset push time to: " + pushTime);
		} else if(pushTime < (new Date()).getTime()) { // time limit hit; push string onwards
			//System.err.println("Hit time limit: \'" + pushString + "\'");
			stream.add(pushString);
			pushString = "";
			if(isMore) { resetPushTime(); }
			else { pushTime = null; }
		}
	}

	public void add(Character c) { add(c, false); }
	
	public void close() {
		stream.add(pushString);
		stream.close();
		pushString = null;
		pushTime = null;
	}
}
