package polyml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class ShellListenThread extends Thread implements Runnable {
	BufferedReader mReader;
	BufferEditor mTarget;
	BufferProcOutputPos mPos;
	
	/** this is true while the output from the reader has not ended and 
	 * none has called pleaseQuit; The variable is used by other threads 
	 * (AWT event process) which needs to check this and stop is it's 
	 * been told to. */
	volatile boolean mRunningQ;

	/** used to avoid adding multiple actions to the event queue - 
	 * we only add a new action if the old one is (being or has been)
	 *  processed. */
	volatile boolean mUpdatingQ; 
	
	//void dbgMsg(String s) {
	//	System.err.println("ShellListenThread:" + s);
	//}
	
	public ShellListenThread(InputStream s, BufferEditor t, BufferProcOutputPos pos) {
		super();
		mReader = new BufferedReader(new InputStreamReader(s));
		mTarget = t;
		mPos = pos;
		mRunningQ = false;
		mUpdatingQ = false;
		//dbgMsg("SmlListenThread has been made!");
	}

	/** 
	 * run while we are getting input from a process
	*/
	public void run() {
		//dbgMsg("run: start.");
		try {
			mRunningQ = true;
			mUpdatingQ = false;
			while (mRunningQ) {
				
				if(mReader.ready()){
					//dbgMsg("got stuff!");
					if(mUpdatingQ == false) {
						mUpdatingQ = true;
						// this also updates pos
						mTarget.appendReader(mReader, this, mPos);
					} else {
						//dbgMsg("mUpdatingQ = true!.. waiting to send more...");
					}
				} else {
					//dbgMsg("nothing to read...");
				}
				try {
					sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.err.println("ShellListenThread:run:sleep interrupted:" + e.toString());
					//dbgMsg("sleep interrupted: " + e.toString());
				}
			}
			mReader.close();
		} catch (IOException e) {
			// e.printStackTrace();
		}
		//dbgMsg("run: end.");
	}

	public void pleaseStop() {
		//try {
		mRunningQ = false;
		mUpdatingQ = false;
			//mReader.close();
		//} catch (IOException e) {
			// e.printStackTrace();
		//}
		//dbgMsg("pleaseStop called.");
	}

	public boolean running() {
		return mRunningQ;
	}
	
	public void setNotUpdating() {
		mUpdatingQ = false;
	}
}
