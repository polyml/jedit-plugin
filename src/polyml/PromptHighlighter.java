package polyml;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;

public class PromptHighlighter extends TextAreaExtension {
	
	TextArea textArea;
	BufferProcOutputPos pos;
	
	/**
	 * Assume that pos.buffer = t.buffer! Because pos is for ShellBuffer of this buffer
	 * @param mPos
	 * @param t
	 */
	public PromptHighlighter(BufferProcOutputPos p, TextArea t) {
		super();
		textArea = t;
		pos = p;
		//System.err.println("PromptHighlighter created");
	}
	public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y) {
		int p = pos.getPrePromptPos();
		int p2 = pos.getPostPromptPos();
		
		//System.err.println("p:" + p + "; p2: " + p2);
		
		int line = textArea.getScreenLineOfOffset(p);
		
		// avoid doing anything if we are not on the right line. 
		if(line != screenLine) { return; }
		
		// get start and end x positions for start-to-end range on this line
		int[] xs = getOffsets(screenLine, p, p2);
		int x1 = xs[0]; 
		int x2 = xs[1];
		x2 = Math.max(x2, x1 + 2); // at least 2 pixels wide

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
