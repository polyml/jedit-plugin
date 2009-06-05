package pushstream;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * 
 * A thread that reads from a reader and send the characters 
 * to a character push stream. 
 * 
 * @author ldixon
 *
 */
public class InputStreamThread extends Thread implements Runnable {
	DataInputStream reader;
	PushStream<Character> pushStream;
	
	/** this is true while the output from the reader has not ended and 
	 * nothing has called pleaseStop; The variable is used by other threads 
	 * (AWT event process) which needs to check this and stop it. */
	volatile boolean mRunningQ;
	
	public InputStreamThread(DataInputStream r, PushStream<Character> s) {
		super();
		reader = r;
		pushStream = s;
		mRunningQ = false;
	}

	/** 
	 * run while we are getting input from reader
	*/
	public void run() {
		mRunningQ = true;
		try {
			while (mRunningQ) {
				// if not end of stream, add char to string
				int i = reader.readByte();
				if(i != -1) { 
					//System.err.println("InputSTreamThread: " + (char)i);
					pushStream.add((char)i, reader.available() > 0); 
				} else { 
					mRunningQ = false; 
				}
			}
		} catch (IOException e) {
			// nothing to do
		}
		pushStream.close();
		mRunningQ = false;
	}

	/**
	 * stop the listener, also closes the input stream
	 */
	public void pleaseStop() {
		mRunningQ = false;
		try { reader.close();
		} catch (IOException e) {
			System.err.println("pleaseStop: raised IO Exception on closing reader");
			e.printStackTrace();
		}
	}

	/** 
	 * @return true if running
	 */
	public boolean running() {
		return mRunningQ;
	}
}
