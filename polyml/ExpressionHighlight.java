package polyml;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;

public class ExpressionHighlight extends TextAreaExtension {
	private TextArea mTextArea;
	boolean mShow;
	TextRange mRange;
	
	public ExpressionHighlight(TextArea textArea, TextRange r) {
		this.mTextArea = textArea;
		this.mShow = true;
		this.mRange = r;
	}
	
	public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y) {
		if (!mShow) return;

		if (mRange != null) {
			paintHighlight(gfx, screenLine, start, end, y);
		}
	}

	private int[] getOffsets(int screenLine) {
		int x1, x2;

		int matchStartLine = mTextArea.getScreenLineOfOffset(mRange.start);
		int matchEndLine = mTextArea.getScreenLineOfOffset(mRange.end);

		if (matchStartLine == screenLine) {
			x1 = mRange.start;
		} else {
			x1 = mTextArea.getScreenLineStartOffset(screenLine);
		}

		if (matchEndLine == screenLine) {
			x2 = mRange.end;
		} else {
			x2 = mTextArea.getScreenLineEndOffset(screenLine) - 1;
		}

		return new int[] { mTextArea.offsetToXY(x1).x, mTextArea.offsetToXY(x2).x };
	}

	private void paintHighlight(Graphics gfx, int screenLine, int start,
			int end, int y) {
		if (!mShow) return;

		if (mRange.start >= end || mRange.end < start) {
			return;
		}

		int matchStartLine = mTextArea.getScreenLineOfOffset(mRange.start);
		int matchEndLine = mTextArea.getScreenLineOfOffset(mRange.end);

		FontMetrics fm = mTextArea.getPainter().getFontMetrics();
		int height = fm.getHeight();

		int[] offsets = getOffsets(screenLine);
		int x1 = offsets[0];
		int x2 = offsets[1];

		gfx.setColor(mTextArea.getPainter().getStructureHighlightColor());

		gfx.drawLine(x1, y, x1, y + height - 1);
		gfx.drawLine(x2, y, x2, y + height - 1);

		if (matchStartLine == screenLine || screenLine == 0)
			gfx.drawLine(x1, y, x2, y);
		else {
			offsets = getOffsets(screenLine - 1);
			int prevX1 = offsets[0];
			int prevX2 = offsets[1];

			gfx.drawLine(Math.min(x1, prevX1), y, Math.max(x1, prevX1), y);
			gfx.drawLine(Math.min(x2, prevX2), y, Math.max(x2, prevX2), y);
		}

		if (matchEndLine == screenLine) {
			gfx.drawLine(x1, y + height - 1, x2, y + height - 1);
		}
	}
}
