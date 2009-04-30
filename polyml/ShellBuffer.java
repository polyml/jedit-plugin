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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.StructureMatcher;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;
import org.gjt.sp.jedit.textarea.StructureMatcher.Match;

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
	//PromptHighlighter mPromptHighlighter;
	Map<TextArea,PromptHighlighter> mShownInTextAreas;
	
	//TextRange mHighlightRange;
	//Chunk mPreProcOutputChunk;
	
	//OutputStreamWriter mShellWriter;
	
	
	public class PromptHighlighter extends TextAreaExtension {
		TextArea textArea;
		public PromptHighlighter(TextArea t) {
			super();
			textArea = t;
			System.out.println("PromptHighlighter created");
		}
		public void paintValidLine(Graphics2D gfx, int screenLine,
				int physicalLine, int start, int end, int y) {
			int p = mPos.getPos();
			int p2 = getPostPromptPos();
			int line = textArea.getScreenLineOfOffset(p);
			
			// avoid doing anything if we are not on the right line. 
			if(line != screenLine) { return; }
						
			// get start and end x positions for start-to-end range on this line
			int[] xs = getOffsets(screenLine, p, p2);
			int x1 = xs[0]; 
			int x2 = xs[1];
			
			// get font height
			FontMetrics fm = textArea.getPainter().getFontMetrics();
			int height = fm.getHeight();
			
			// draw box
			Composite c = gfx.getComposite();
			gfx.setColor(Color.red);
			gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)0.5));
			gfx.fillRect(x1, y, x2 - x1, height);
			gfx.setComposite(c);
		} 
		
		/**
		 * 
		 * given a screen line, a start and end offset (within buffer), 
		 * returns the start "x1" and end "x2" horizontal drawing positions; 
		 * If start is on an earlier line, gives start of line, and if end 
		 * is on a later line, givens end of line. 
		 * 
		 * @param screenLine
		 * @param start
		 * @param end
		 * @return array of size 2, with x1 and x2 positions. 
		 */
		int[] getOffsets(int screenLine, int start, int end)
		{
			int x1, x2;

			int startLine = textArea.getScreenLineOfOffset(start);
			int endLine = textArea.getScreenLineOfOffset(end);

			if(startLine == screenLine) { x1 = start; }
			else { x1 = textArea.getScreenLineStartOffset(screenLine); }

			if(endLine == screenLine) { x2 = end; }
			else { x2 = textArea.getScreenLineEndOffset(screenLine) - 1; }

			return new int[] { textArea.offsetToXY(x1).x, textArea.offsetToXY(x2).x };
		}
	}
	
	
	void dbgMsg(String s) { 
		System.err.println("ShellBuffer:" + s);
	}
	
	/**
	 * a new SmlShell with the name "SML"
	 */
	public ShellBuffer(BufferEditor b) throws IOException {
		super();
		mShownInTextAreas = new HashMap<TextArea,PromptHighlighter>();
		mShellListenThread = null;
		mShellProcess = null;
		mShellWriter = null;
		mOutputBuffer = b;
		
		mPos = new BufferProcOutputPos(b.getBuffer(), 
				jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_PROMPT));

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
		//dbgMsg("checking process != null");
		if (mShellProcess == null) {
			dbgMsg("process was null...");
			restartProcess();
			dbgMsg("restarted");
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
		String prompt = jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_PROMPT);
		
		// mPos.getPrompt();
		int promptlen = Math.max(1,prompt.length());
		
		if( endlen < promptlen) {
			return Math.min(p + 1,l);
		} else {
			if(b.getText(p, promptlen).compareTo(prompt) == 0){
				return p + promptlen;
			} else {
				return p + 1;
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
		String prompt = jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_PROMPT);
		
		System.err.println("prompt is: " + prompt);
		
		/* set position to end of buffer */
		mPos.setPos(l);
		/* add a new prompt */
		mOutputBuffer.append(prompt);
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
	
	public void showInTextArea(TextArea textArea) {
		PromptHighlighter promptHighlighter = new PromptHighlighter(textArea);
		mShownInTextAreas.put(textArea, promptHighlighter);
		textArea.getPainter().addExtension(promptHighlighter);
	}
	
	public void unShowInTextArea(TextArea textArea) {
		PromptHighlighter promptHighlighter = mShownInTextAreas.get(textArea);
		if(promptHighlighter != null) {
			textArea.getPainter().removeExtension(promptHighlighter);
			mShownInTextAreas.remove(textArea);
		}
	}
	
	public void unShowInAllTextAreas() {
		for(TextArea t : mShownInTextAreas.keySet()){
			t.getPainter().removeExtension(mShownInTextAreas.get(t));
			mShownInTextAreas.remove(t);
		}
	}
}
