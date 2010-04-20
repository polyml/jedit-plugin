package polyml;
 
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
	private int mPrePromptPos;
	private int mPostPromptPos;
	JEditBuffer mBuffer;
	ShellBuffer mShellBuffer;
	String mPrompt;
	//boolean mIsProcessOut;

	public BufferProcOutputPos(ShellBuffer buf){
		mShellBuffer = buf;
		mBuffer = mShellBuffer.getBufferEditor().getBuffer();
		mPrePromptPos = mBuffer.getLength();
		mPostPromptPos = mBuffer.getLength();
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
	
	/**
	 * Get position of start of prompt; always within buffer size
	 */
	public synchronized int getPrePromptPos() {
		//System.err.println("getPos: " + mPos);
		//return 0;
		return (Math.min(mPrePromptPos, mBuffer.getLength()));
	}
	
	/**
	 * Get position of end of prompt; always within buffer size
	 */
	public synchronized int getPostPromptPos() {
		//System.err.println("getPos: " + mPos);
		//return 0;
		return (Math.min(mPostPromptPos, mBuffer.getLength()));
	}
	
	/** 
	 * Move prompt forward
	 * @param i
	 */
	public synchronized void movePosFwd(int i) {
		//setPostPromptPos(mPostPromptPos + i);
		setPostPromptPos(mPostPromptPos + i);
	}
	
	/**
	 * Set position of start of prompt
	 * ensures that pos is within the buffer, 
	 * and if buffer length is greater than 0, then it's at least 
	 * one character before the last one. 
	 * @param pos
	 */
	public synchronized void setPrePromptPos(int pos) {
		mShellBuffer.invalidateTextAreas();
		int	l = mBuffer.getLength();
		//System.err.println("setPos: l: " + l + "; mPos " + mPos + "; pos" + pos);
		String prompt = jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_PROMPT);
		int promptlen = prompt.length(); // Math.max(1,prompt.length());

		mPrePromptPos = pos;
		
		if( mPrePromptPos + promptlen <= l) {
			if(mBuffer.getText(mPrePromptPos, promptlen).compareTo(prompt) == 0){
				mPostPromptPos = mPrePromptPos + promptlen;
			} else {
				mPostPromptPos = mPrePromptPos;
			}
		} else {
			mPostPromptPos = mPrePromptPos;
		}
		
		mShellBuffer.invalidateTextAreas();
	}
	
	
	/*
	 * set position of end of prompt, ensures that pos is within the buffer, 
	 * @param pos
	 */
	public synchronized void setPostPromptPos(int pos) {
		String prompt = jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_PROMPT);
		int promptlen = prompt.length(); // Math.max(1,prompt.length());

		mShellBuffer.invalidateTextAreas();

		mPostPromptPos = pos;
		
		if( mPostPromptPos - promptlen >= 0) {
			mPrePromptPos = mPostPromptPos - promptlen;
			if(mBuffer.getText(mPrePromptPos, promptlen).compareTo(prompt) == 0){
				return;
			} else {
				mPrePromptPos = mPostPromptPos;
			}
		} else {
			mPrePromptPos = mPostPromptPos;
		}
		
		mShellBuffer.invalidateTextAreas();
	}
	
	
	/** get effective pos: pos + if there's a prompt, then after that. */
	/* public synchronized int getPostPromptPos(){
		int l = mBuffer.getLength();
		
		String prompt = jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_PROMPT);
		int promptlen = Math.max(1,prompt.length());
		
		//System.err.println("getPostPromptPos: l=" + l + "; promptlen=" + promptlen + "; mPos=" + mPos);

		// quite complicated! must give position within the file; assumes l=0 xor mPos < l
		if( mPrePromptPos + promptlen <= l) {
			if(mBuffer.getText(mPrePromptPos, promptlen).compareTo(prompt) == 0){
				return mPrePromptPos + promptlen;
			} else {
				return mPrePromptPos + 1;
			}
		} else if(mPrePromptPos >= l) {
			return l;
		} else { 
			return mPrePromptPos + 1;
		}
	} */
	
	/* get position of end of prompt. */
	/* public synchronized int getPostPromptPos(){
		return mPostPromptPos;
	}
	*/
	
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
	//System.err.println("BufferEditor:" + s);
	//}
	
	public synchronized void bufferLoaded(JEditBuffer buffer) {
		mBuffer = buffer;
		setPrePromptPos(mBuffer.getLength());
	}

	/** Update the process position marker when stuff is added to the buffer. */
	public synchronized void contentInserted(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
		/* if a process is sending output, then it's always to the left of getPos() so we should always 
		 * update the pos; otherwise only do it if it's user input before the current pos; */
		//if(isProcessOut() || offset < getPos())
		if(offset < getPostPromptPos()){ movePosFwd(length); }
		//System.err.println("content inserted, offset=" + offset + "; len=" + length + "; newpos=" + mPrePromptPos);
	}

	/** move mPos back if cut after it and overlaps with it. Else move it appropriately. */
	public synchronized void contentRemoved(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
		// removal includes current position; so need to adjust it
		if(offset < mPostPromptPos){
			/* to the left of pos, update it... */
			if(offset + length >= mPostPromptPos) {
				/* we also cut over pos, then move pos back to cut start */
				setPostPromptPos(offset);
			} else {
				/* move pos left by length */
				setPostPromptPos(mPostPromptPos - length);
			}
			
			// mShellBuffer.invalidateTextAreas();  NOT needed as cut will do it for us
		}
		//System.err.println("content removed, offset: " + offset + "; len " + length + "; newpos" + mPrePromptPos);
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