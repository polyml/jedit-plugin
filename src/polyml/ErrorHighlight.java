/*
 * ErrorHighlight.java - Highlights error locations in text area
 *
 * Copyright (C) 1999, 2003 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package polyml;

import javax.swing.text.Segment;
import java.awt.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;

/**
 * Adapted from the ErrorList class of the same name.
 */
public class ErrorHighlight extends ErrorVisualisation {

	private final Segment seg;
	private final Point point;

	public ErrorHighlight(EditPane editPane) {
		super(editPane);
		seg = new Segment();
		point = new Point();
	}
	
	public void addToPane() {
		editPane.getTextArea().getPainter().addExtension(this);
		editPane.getTextArea().putClientProperty("ErrorHighlight", this);
	}
	
	public static void removeFromPane(EditPane pane) {
		if (pane != null) {
			TextArea ta = pane.getTextArea();
			if (ta != null) {
				TextAreaExtension te = (TextAreaExtension) ta.getClientProperty("ErrorHighlight");
				if (te != null) {
					ta.getPainter().removeExtension(te);
					ta.putClientProperty("ErrorHighlight",null);
				}
			}
		}
	}

	/**
	 * Highlights erroneous regions of the text.
	 * @see TextAreaExtension#paintValidLine(Graphics2D, int, int, int, int, int)
	 */
	public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y) {
		FontMetrics fm = editPane.getTextArea().getPainter().getFontMetrics();

		// TODO: make this more selective.
		for (PolyMLError e : getBufferInfo()) {
			paintError(e, gfx, physicalLine, start, end, y + fm.getAscent());
		}
	}

	/**
	 * Returns a tooltip when hovering over an error.
	 * @see TextAreaExtension#getToolTipText(int, int)
	 */
	public String getToolTipText(int x, int y) {
		Buffer buffer = editPane.getBuffer();
		if ( buffer == null || !(buffer.isLoaded()) ) {
			return null;
		}

		JEditTextArea textArea = editPane.getTextArea();
		int offset = textArea.xyToOffset(x,y);
		if (offset == -1) {
			return null;
		}

		for (PolyMLError e : getBufferInfo()) {
			int start = e.getStartPos();
			int end = e.getEndPos();

			if ((offset >= start && offset <= end) || (start == 0 && end == 0)) {
				return e.stringOfError();
			}
		}
		return null;
	}

	/**
	 * Paints an error over a given region.
	 * @param error the error itself
	 * @param gfx the graphics context
	 * @param line the physical line on which to paint.
	 * @param _start start of x-constraint
	 * @param _end end of x-constraint
	 * @param y-constraint relative to line
	 */
	private void paintError(PolyMLError error, Graphics2D gfx, int line,
			int _start, int _end, int y) {
		JEditTextArea textArea = editPane.getTextArea();

		int lineStart = textArea.getLineStartOffset(line);

		int start = error.getStartPos();
		int end = error.getEndPos();

		if (start == 0 && end == 0) {
			textArea.getLineText(line, seg);
			for (int j = 0; j < seg.count; j++) {
				if (Character.isWhitespace(seg.array[seg.offset + j]))
					start++;
				else
					break;
			}

			end = seg.count;
		}

		if (start + lineStart >= _end || end + lineStart <= _start)
			return;

		int startX;

		if (start + lineStart >= _start)
			startX = textArea.offsetToXY(line, start, point).x;
		else
			startX = 0;

		int endX;

		if (end + lineStart >= _end)
			endX = textArea.offsetToXY(line, _end - lineStart - 1, point).x;
		else
			endX = textArea.offsetToXY(line, end, point).x;

		gfx.setColor(ErrorVisualisation.COLOUR_MAP.get(error.kind));
		gfx.drawLine(startX, y + 1, endX, y + 1);
	}


}
