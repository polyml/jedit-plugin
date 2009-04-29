package polyml;
 
import org.gjt.sp.jedit.buffer.BufferChangeAdapter;
import org.gjt.sp.jedit.buffer.BufferListener;
import org.gjt.sp.jedit.buffer.JEditBuffer;

/**
 * Manages a position in a buffer that gets moved when stuff is inserted or removed before that position. 
 * 
 * @author ldixon
 *
 */
public class BufferProcOutputPos extends BufferChangeAdapter implements BufferListener {
	private int mPos;
	JEditBuffer mBuffer;
	String mPrompt;
	boolean mIsProcessOut;

	public BufferProcOutputPos(JEditBuffer buf, String prompt){
		mPos = buf.getLength();
		mBuffer = buf;
		mPrompt = prompt;
		mBuffer.addBufferListener(this);
		mIsProcessOut = false;
	}

	/** check if the "currently a process is sending output stuff" is true.*/
	public boolean isProcessOut() {
		return mIsProcessOut;
	}
	/** change the state of "currently a process is sending output stuff" */
	public void setIsProcessOut(boolean b) {
		mIsProcessOut = b;
	}
	
	public synchronized int getPos() {
		//System.err.println("getPos: " + mPos);
		//return 0;
		return mPos;
	}

	public synchronized void setPos(int pos) {
		if(pos > mBuffer.getLength()) {
			mPos = mBuffer.getLength();
		} else if(pos < 0) {
			mPos = 0;
		} else {
			mPos = pos;
		}
	}
	
	
	public String getPrompt() {
		String s;
		synchronized(mPrompt){
			s = mPrompt;
		}
		return s;
	}

	public void setPrompt(String s) {
		synchronized(mPrompt){ mPrompt = s; }
	}
	
	//void dbgMsg(String s) { 
	//	System.err.println("BufferEditor:" + s);
	//}
	
	public void bufferLoaded(JEditBuffer buffer) {
		// TODO Auto-generated method stub
		mBuffer = buffer;
		setPos(mBuffer.getLength());
	}

	/** Update the process position marker when stuff is added to the buffer. */
	public void contentInserted(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
		/* if a process is sending output, then it's always to the left of getPos() so we should always 
		 * update the pos; otherwise only do it if it's user input before the current pos; */
		if(isProcessOut() || offset < getPos()){
			setPos(getPos() + length);
		}
		//System.err.println("content inserted, offset: " + offset + "; len " + length + "; newpos" + getPos());
	}

	/** move mPos back if cut if after it and overlaps with it. Else move it appropriately. */
	public void contentRemoved(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
		if(offset < getPos()){
			/* to the left of pos, update it... */
			if(offset + length > getPos()) {
				/* if we also cut over pos, then move pos back to cut start */
				setPos(offset);
			} else {
				/* move pos left by length */
				setPos(getPos() - length);
			}
		}
		//System.err.println("content removed, offset: " + offset + "; len " + length + "; newpos" + getPos());
	}

	public void foldHandlerChanged(JEditBuffer buffer) {
		// TODO Auto-generated method stub

	}

	public void foldLevelChanged(JEditBuffer buffer, int startLine, int endLine) {
		// TODO Auto-generated method stub

	}

	public void preContentInserted(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
		// TODO Auto-generated method stub

	}

	public void preContentRemoved(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
		// TODO Auto-generated method stub

	}

	public void transactionComplete(JEditBuffer buffer) {
		// TODO Auto-generated method stub
	}	
}