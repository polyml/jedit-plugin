package polyml;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;

import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.Buffer;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyleConstants;
import javax.swing.JTextField;


/** 
 * This is a thread safe class for working with Buffers. It pushes actions onto the AWT event queue. 
 * 
 * @author ldixon
 */

public class BufferEditor {
	Buffer mBuffer;

	//void dbgMsg(String s) { 
	//	System.err.println("BufferEditor:" + s);
	//}
	
	public BufferEditor(Buffer buf) {
		mBuffer = buf;
	}
	
	public Buffer getBuffer() {
		return mBuffer;
	}

	public int getLength(){ return mBuffer.getLength(); }
	
	/** Replace string in range "from" to "to" with "s" */
	public void replaceFromTo(int from, int to, String s) {
		SwingUtilities.invokeLater(new BEditReplaceFromTo(from, to, s));	
	}
	
	/** Insert string "s" into buffer at "pos" */
	public void insert(int pos, String s) {
		SwingUtilities.invokeLater(new BEditInsert(pos, s));
	}

	/** Append output from reader, which has listening thread "th", at position "p" */
	public void appendReader(BufferedReader reader, ShellListenThread th, BufferProcOutputPos p) {
		SwingUtilities.invokeLater(new BEditAppendReader(reader, th, p));
	}
	
	/** Class wrapper for AWT thread safe insertion */
	class BEditInsert implements Runnable {
		String mStr; int mPos;
		public BEditInsert(int pos, String s) { mPos = pos; mStr = s; }
		public void run() { mBuffer.insert(mPos, mStr); }
	}
	
	/** Class wrapper for AWT thread safe replacement */
	class BEditReplaceFromTo implements Runnable {
		String mStr; int mFrom; int mTo;
		public BEditReplaceFromTo(int from, int to, String s) { mFrom = from; mTo = to; mStr = s; }
		public void run() { 
			mBuffer.remove(mFrom, mTo - mFrom);
			mBuffer.insert(mFrom, mStr); 
		}
	}

	/** Append output from reader, which has listening thread "th", at position "pos" */
	class BEditAppendReader implements Runnable {
		BufferedReader mReader;
		ShellListenThread mThread;
		BufferProcOutputPos mPos;
		
		/** This class is used for action to insert content from a Process' reader that 
		 * was actived by a ShellListenThread, and which has a ouput position marker of 
		 * pos */
		public BEditAppendReader(BufferedReader reader, ShellListenThread th,
				BufferProcOutputPos pos) 
		{ mReader = reader; mThread = th; mPos = pos; }
		public void run() {
			//dbgMsg("BEditAppendReader:run-start");
			String s = new String();
			try {
				int i = 0;
				/** never take more than 100ms in one go, else we'd lock AWT up. 
				 * Another event will be raised later to pick up the rest of the 
				 * stuff in the reader, but we should stop to allow oher AWT 
				 * events to get some time.
				 * */
				long timelimit = (new Date()).getTime() + 100; 
				while(i != -1 // EOF 
						&& (timelimit > (new Date()).getTime()) // stop if we take too long
						&& mReader.ready() // more stuff to print out 
						&& mThread.running()){ // not been told to quit
					//dbgMsg("reading...");
					i = mReader.read();
					//dbgMsg("read:" + i);
					if(i == -1) {
						s += "<EOF>";
						mThread.pleaseStop();
					} else {
						s += (char)i;
					}
				}
				// dbgMsg("stopped reading.");
				
				int p = mPos.getPos();
				
				// Add in prompt if it's not already there. 
				int l = mBuffer.getLength();
				boolean addedPromptQ = false;
				String prompt = "# ";
				//dbgMsg("len: " + l + "; promptlen: " + prompt.length());
				if(l >= p + prompt.length()){
					String lastoutputstr = mBuffer.getText(p, prompt.length());
					// dbgMsg("enough extra len, laststr: " + lastoutputstr);
					if (! lastoutputstr.equals(prompt)){
						s += prompt;
						addedPromptQ = true;
					}
				} else {
					s += prompt;
					addedPromptQ = true;
				}
				
				mPos.setIsProcessOut(true);
				mBuffer.insert(p, s);
				mPos.setIsProcessOut(false);
				if(addedPromptQ){
					mPos.setPos(mPos.getPos() - prompt.length());
				}
				
				//dbgMsg("done insert");
				mThread.setNotUpdating();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("BufferEditor:" + "BEditAppendReader:run:IOException:" + e.toString());
				//dbgMsg("BEditAppendReader:run:IOException:" + e.toString());
			}
			//dbgMsg("BEditAppendReader:run-end:" + s);
		}
	}

}