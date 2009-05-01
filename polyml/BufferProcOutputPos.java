package polyml;
 
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;
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
	ShellBuffer mShellBuffer;
	String mPrompt;
	//boolean mIsProcessOut;

	public BufferProcOutputPos(ShellBuffer buf, String prompt){
		mShellBuffer = buf;
		mBuffer = mShellBuffer.getBufferEditor().getBuffer();
		mPos = mBuffer.getLength();
		// mPrompt = prompt;
		mBuffer.addBufferListener(this);
		//mIsProcessOut = false;
	}

	/** check if the "currently a process is sending output stuff" is true.*/
	//public boolean isProcessOut() {
	//	return mIsProcessOut;
	//}
	/** change the state of "currently a process is sending output stuff" */
	//public void setIsProcessOut(boolean b) {
	//	mIsProcessOut = b;
	//}
	
	public synchronized int getPos() {
		//System.err.println("getPos: " + mPos);
		//return 0;
		return mPos;
	}

	public synchronized void movePosFwd(int i) {
		setPos(mPos + i);
	}
	
	/**
	 * set pos, ensures that pos is within the buffer, 
	 * and if buffer length is greater than 0, then it's at least 
	 * one character before the last one. 
	 * @param pos
	 */
	public synchronized void setPos(int pos) {
		mShellBuffer.invalidateTextAreas();
		int	l = mBuffer.getLength();
		//System.err.println("setPos: l: " + l + "; mPos " + mPos + "; pos" + pos);
		if(pos >= 0 && pos < l) {
			mPos = pos;
		} else if(pos >= l && l > 0){
			mPos = l - 1;
		} else {
			mPos = 0;
		}
		mShellBuffer.invalidateTextAreas();
	}
	
	/** get effective pos: pos + if there's a prompt, then after that. */
	public synchronized int getPostPromptPos(){
		int l = mBuffer.getLength();
		
		String prompt = jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_PROMPT);
		int promptlen = Math.max(1,prompt.length());
		
		//System.err.println("getPostPromptPos: l=" + l + "; promptlen=" + promptlen + "; mPos=" + mPos);

		/* quite complicated! must give position within the file; assumes l=0 xor mPos < l */
		if(l == 0){ 
			return 0; 
		} else if( mPos + promptlen <= l) {
			if(mBuffer.getText(mPos, promptlen).compareTo(prompt) == 0){
				return mPos + promptlen;
			} else {
				return mPos + 1;
			}
		} else if(mPos >= l) {
			return l;
		} else { 
			return mPos + 1;
		}
	}
	
	//public String getPrompt() {
	//	String s;
	//	synchronized(mPrompt){
	//		s = mPrompt;
	//	}
	//	return s;
	//}

	//public void setPrompt(String s) {
	//	synchronized(mPrompt){ mPrompt = s; }
	//}
	
	//void dbgMsg(String s) { 
	//	System.err.println("BufferEditor:" + s);
	//}
	
	public synchronized void bufferLoaded(JEditBuffer buffer) {
		mBuffer = buffer;
		setPos(mBuffer.getLength());
	}

	/** Update the process position marker when stuff is added to the buffer. */
	public synchronized void contentInserted(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
		/* if a process is sending output, then it's always to the left of getPos() so we should always 
		 * update the pos; otherwise only do it if it's user input before the current pos; */
		//if(isProcessOut() || offset < getPos())
		if(offset <= getPos()){ movePosFwd(length); }
		System.err.println("content inserted, offset: " + offset + "; len " + length + "; newpos" + getPos());
	}

	/** move mPos back if cut after it and overlaps with it. Else move it appropriately. */
	public synchronized void contentRemoved(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
		// removal includes current position; so need to adjust it
		if(offset <= mPos){
			/* to the left of pos, update it... */
			if(offset + length > mPos) {
				/* if we also cut over pos, then move pos back to cut start */
				if(offset > 0) {
					mPos = offset - 1;
				} else {
					mPos = 0;
				}
			} else {
				/* move pos left by length */
				mPos = mPos - length;
			}
			mShellBuffer.invalidateTextAreas();
		}
		System.err.println("content removed, offset: " + offset + "; len " + length + "; newpos" + getPos());
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