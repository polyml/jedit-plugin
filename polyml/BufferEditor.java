package polyml;

import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.Buffer;


/** 
 * This is a thread safe class for working with Buffers. It pushes actions onto the AWT event queue. 
 * 
 * @author ldixon
 */

public class BufferEditor {
	Buffer mBuffer;

	//void dbgMsg(String s) { 
	//System.err.println("BufferEditor:" + s);
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
	
	/** Insert string "s" into buffer at "pos" */
	public void insertAtBPos(BufferProcOutputPos pos, String s) {
		SwingUtilities.invokeLater(new BEditInsertAtBPos(pos, s));
	}
	
	
	/** Insert string "s" into buffer at "pos" */
	public void appendPrompt(BufferProcOutputPos promptPos, String s) {
		SwingUtilities.invokeLater(new BEditAppendPrompt(promptPos, s));
	}
	
	
	/** Insert string "s" into buffer at "pos" */
	public void append(String s) {
		SwingUtilities.invokeLater(new BEditAppend(s));
	}
	
	/** Class wrapper for AWT thread safe insertion */
	class BEditAppendPrompt implements Runnable {
		String mStr; BufferProcOutputPos mPos;
		public BEditAppendPrompt(BufferProcOutputPos pos, String s) { mPos = pos; mStr = s; }
		public void run() { 
			int l = mBuffer.getLength();
			mBuffer.insert(l, mStr); 
			mPos.setPrePromptPos(l);
		}
	}
	
	/** Class wrapper for AWT thread safe append */
	class BEditAppend implements Runnable {
		String mStr;
		public BEditAppend(String s) { mStr = s; }
		public void run() { mBuffer.insert(mBuffer.getLength(), mStr); }
	}
	
	/** Class wrapper for AWT thread safe insertion */
	class BEditInsert implements Runnable {
		String mStr; int mPos; 
		public BEditInsert(int pos, String s) { mPos = pos; mStr = s; }
		public void run() { mBuffer.insert(mPos, mStr); }
	}
	
	/** Class wrapper for AWT thread safe insertion */
	class BEditInsertAtBPos implements Runnable {
		String mStr; BufferProcOutputPos mPos; 
		public BEditInsertAtBPos(BufferProcOutputPos pos, String s) { mPos = pos; mStr = s; }
		public void run() { mBuffer.insert(mPos.getPrePromptPos(), mStr); }
	}
	
	/** Class wrapper for AWT thread safe replacement */
	class BEditReplaceFromTo implements Runnable {
		String mStr; int mFrom; int mTo;
		public BEditReplaceFromTo(int from, int to, String s) { mFrom = from; mTo = to; mStr = s; }
		public void run() { 
			mBuffer.beginCompoundEdit();
			mBuffer.remove(mFrom, mTo - mFrom);
			mBuffer.insert(mFrom, mStr); 
			mBuffer.endCompoundEdit();
		}
	}
}