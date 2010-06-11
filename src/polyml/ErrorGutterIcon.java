package polyml;

import java.awt.Graphics2D;

import java.awt.Point;

import javax.swing.ImageIcon;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.textarea.Gutter;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;

/**
 * Adapted from the ErrorList class of the same name.
 */
public class ErrorGutterIcon extends ErrorVisualisation {

	private static final int FOLD_MARKER_SIZE = 12;
	
	public ErrorGutterIcon(EditPane editPane) {
		super(editPane);
	}
	
	public void addToPane() {
		Gutter gutter = editPane.getTextArea().getGutter();
		gutter.addExtension(this);
		gutter.putClientProperty("ErrorHighlight", this);
	}

	public static void removeFromPane(EditPane pane) {
		Gutter gutter = pane.getTextArea().getGutter();
		TextAreaExtension te = (TextAreaExtension) gutter.getClientProperty("ErrorHighlight");
		if (te != null) {
			gutter.removeExtension(te);
			gutter.putClientProperty("ErrorHighlight",null);
		}
	}
	
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
		int line = textArea.getLineOfOffset(offset);

		StringBuffer errMsg = new StringBuffer();
		
		for (PolyMLError e : getInfoOnLine(line)) {
			errMsg.append("<br>");
			errMsg.append(e.stringOfError());
			errMsg.append("<br>");
		}

		if (errMsg.length() > 0) {
			return "<html>"+errMsg.toString()+"</html>";
		}
		return null;
	}

	public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y) {
		
		boolean isFatal = false;
		boolean isWarning = false;

		for (PolyMLError e : getInfoOnLine(physicalLine)) {
			// order of severity
			if (e.kind == PolyMLError.KIND_EXCEPTION 
					|| e.kind == PolyMLError.KIND_FATAL
					|| e.kind == PolyMLError.KIND_PRELUDE_FAILURE
					|| e.isFatal()) {
				isFatal = true;
				break;
			} else if (e.kind == PolyMLError.KIND_WARNING) {
				isWarning = true;				
			}
			
			JEditTextArea textArea = editPane.getTextArea();
			ImageIcon icon = isFatal ? ERROR_ICON : (isWarning ? WARNING_ICON : INFO_ICON);
			
			// Center the icon in the gutter line
			int lineHeight = textArea.getPainter().getFontMetrics().getHeight();
			Point iconPos = new Point(
					(FOLD_MARKER_SIZE - icon.getIconWidth()) / 2,
					y + (lineHeight - icon.getIconHeight()) / 2);
			gfx.drawImage(icon.getImage(), iconPos.x, iconPos.y, null);
		}
	}

}
