package polyml;

import javax.swing.text.Position;
import org.gjt.sp.jedit.Buffer;

/**
 * A PolyML error with flexible error positions.
 * All PolyMLErrors constructed using this class gain flexible offset positions,
 * once associated with a buffer.
 */
public class ViewablePolyMLError extends PolyMLError {

	protected Buffer buffer;
	protected Position startOffsetPos;
	protected Position endOffsetPos;
	
	/**
	 * 
	 * @param kind see {@link PolyMLError#kind}
	 * @param startPos original starting offset
	 * @param endPos original ending offset
	 * @param msg see {@link PolyMLError#msg}
	 */
	ViewablePolyMLError(char kind, int startPos, int endPos, String msg){
		super(kind, startPos, endPos, msg);
		startOffsetPos = null;
		endOffsetPos = null;
	}
	
	/**
	 * Create an error from existing markup.
	 * @throws MarkupException 
	 */
	ViewablePolyMLError(PolyMarkup m) throws MarkupException {
		super(m);
		startOffsetPos = null;
		endOffsetPos = null;
	}
	
	/**
	 * Associates this error with a buffer, assigning flexible buffer-relative
	 * positions to the offsets specified in the error. 
	 */
	public void associateBuffer(Buffer b) {
		if (b != null) {
			buffer = b;
			startOffsetPos = b.createPosition(startPos);
			endOffsetPos = b.createPosition(endPos);
		} else {
			startOffsetPos = null;
			endOffsetPos = null;
		}
	}
	
	/** Returns the start position of the error */
	public int getStartPos() {
		if (startOffsetPos != null) {
			return startOffsetPos.getOffset();
		}
		return super.getStartPos();
	}
	
	/** Returns the end position of the error. */
	public int getEndPos() {
		if (endOffsetPos != null) {
			return endOffsetPos.getOffset();
		}
		return super.getEndPos();
	}
	
	/** Returns the line number */
	public Integer getLineNumber() {
		if (buffer != null) {
			return buffer.getLineOfOffset(startOffsetPos.getOffset());
		}
		return null;
	}
}