/* -----------------------------------------------------------------------------
 *       Copyright
 * 
 *       (C) 2007 Lucas Dixon
 * 
 *       License
 * 
 *       This program is free software; you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation; either version 1, or (at your option)
 *       any later version.
 * 
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 * 
 * -------------------------------------------------------------------------- */

package polyml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.TextAreaExtension;

/**
 * A Buffer that takes input to some shell, and shows the output from that shell
 * 
 * @author <a href="mailto:l.dixon@ed.ac.uk">Lucas Dixon</a>
 * @created 03 November 2007
 * @version 1.0
 */
public final class ShellBuffer extends Object {

	BufferEditor mOutputBuffer;
	ShellListenThread mShellListenThread;
	Process mShellProcess;
	BufferedWriter mShellWriter;
	BufferProcOutputPos mPos;
	History mHistory;
	TextRange mHighlightRange;
	
	//OutputStreamWriter mShellWriter;
	
	void dbgMsg(String s) { 
		System.err.println("ShellBuffer:" + s);
	}
	
	/**
	 * a new SmlShell with the name "SML"
	 */
	public ShellBuffer(BufferEditor b) throws IOException {
		super();
		mShellListenThread = null;
		mShellProcess = null;
		mShellWriter = null;
		mOutputBuffer = b;

		mPos = new BufferProcOutputPos(b.getBuffer(), 
				jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_PROMPT));
		mHighlightRange = new TextRange(0,0,0,0);
		
		mHistory = new History(jEdit.getIntegerProperty(PolyMLPlugin.PROPS_SHELL_MAX_HISTORY, 50));
		
		restartProcess();
		
		//try {		
		//} catch(IOException e) {
		//	e.printStackTrace();
		//	dbgMsg("Failed to start shell process: " + e.toString());
		//}
		//mOutputBuffer.addBufferListener(this);
		//dbgMsg("shell started!");
	}
	
	/** When we throw IOException everything is correctly closed/null. */
	void restartProcess() throws IOException {
		//dbgMsg("startProcess:Starting new process");
		stopProcess();
		String cmd = jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_COMMAND);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(true);
		try {
			mShellProcess = pb.start();
		} catch (IOException e) {
			System.err.println("ShellBuffer:" + "Failed to start process: " + cmd);
			//dbgMsg("**** Failed to start process!!! ***");
			mShellProcess = null;
			throw e;
		}
		mShellListenThread = new ShellListenThread(mShellProcess
				.getInputStream(), mOutputBuffer, mPos);
		mShellListenThread.start();
		mShellWriter = new BufferedWriter(new OutputStreamWriter(mShellProcess
				.getOutputStream()));
		//mShellWriter = new OutputStreamWriter(mShellProcess.getOutputStream());
		
		//dbgMsg("startProcess:started.");
	}

	/**
	 *
	 */
	public void send(String command) throws IOException {
		dbgMsg("checking process != null");
		if (mShellProcess == null) {
			//dbgMsg("process was null...");
			restartProcess();
			//dbgMsg("restarted");
		}
		//dbgMsg("sending command: " + command);
		mShellWriter.write(command);
		mShellWriter.newLine();
		mShellWriter.flush();
		//dbgMsg("sent.");
	}

	
	public void prevCommand() {
		String newcur = mHistory.prev(getCurInput());
		mOutputBuffer.replaceFromTo(getPostPromptPos(), mOutputBuffer.getLength(), newcur);
	}
	
	public void nextCommand() {
		String newcur = mHistory.next(getCurInput());
		mOutputBuffer.replaceFromTo(getPostPromptPos(), mOutputBuffer.getLength(), newcur);
	}

	/** get effective pos: pos + if there's a prompt, then after that. */
	public int getPostPromptPos(){
		Buffer b = mOutputBuffer.getBuffer();
		int p = mPos.getPos();
		int l = b.getLength();
		int endlen = l - p;
		String prompt = mPos.getPrompt();
		int promptlen = prompt.length();
		
		if( endlen < promptlen) {
			return p;
		} else {
			if(b.getText(p, promptlen).equals(prompt)){
				return p + promptlen;
			} else {
				return p;
			}
		}
	}
	
	/**
	 * Get the text that will be sent to the buffer: the text after the 
	 * ouput position (skipping over a prompt if there is one)
	 */
	public String getCurInput() {
		Buffer b = mOutputBuffer.getBuffer();
		int l = b.getLength();
		int postPromptPos = getPostPromptPos();
		return b.getText(postPromptPos, l - postPromptPos);
	}
	
	/**
	 * Send the rest of the text in the buffer, after the marked position 
	 * to the shell buffer process.
	 */
	public void sendBufferTextToEOF() throws IOException {
		int l = mOutputBuffer.getBuffer().getLength();
		String s = getCurInput();
		
		/* set position to end of buffer */
		mPos.setPos(l);
		/* add a new prompt */
		mOutputBuffer.insert(l, mPos.getPrompt());
		/* update history */
		mHistory.add(s);
		
		/* sends stuff to process */
		send(s);
	}
	

	/**
	 * Stops the currently executing command, if any.
	 */
	public void stopProcess() {
		if (mShellWriter != null) {
			//dbgMsg("stopProcess: closing old mShellWriter.");
			try{
				mShellWriter.close();
			} catch(IOException e) {
				e.printStackTrace();
				System.err.println("ShellBuffer:" + "stopProcess " + e.toString());
				//dbgMsg(e.toString());
			}
			mShellWriter = null;
		}
		if (mShellListenThread != null) {
			//dbgMsg("stopProcess: stopping old mShellListenThread.");
			mShellListenThread.pleaseStop();
			mShellListenThread = null;
		}
		if (mShellProcess != null) {
			//dbgMsg("stopProcess: stopping old mShellProcess.");
			mShellProcess.destroy();
			mShellProcess = null;
		}
	}

	public BufferEditor getBufferEditor() {
		return mOutputBuffer;
	}

	public void setBufferEditor(BufferEditor outputBuffer) {
		mOutputBuffer = outputBuffer;
	}

	public ShellListenThread getShellListenThread() {
		return mShellListenThread;
	}

	public void setShellListenThread(ShellListenThread shellListenThread) {
		mShellListenThread = shellListenThread;
	}
}
