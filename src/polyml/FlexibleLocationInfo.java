package polyml;

import javax.swing.text.Position;
import org.gjt.sp.jedit.Buffer;

/**
 * An object whose physical position can be determined with reference
 * to its original logical position and its owning buffer.
 */
public abstract class FlexibleLocationInfo {
	
	Buffer buffer;
	Position startOffsetPos;
	Position endOffsetPos;
	int startPos;
	int endPos;
	
	/**
	 * Associates this error with a buffer, assigning flexible buffer-relative
	 * positions to the offsets specified in the error. 
	 */
	public void associateWithBuffer(Buffer b) {
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
		return startPos;
	}
	
	/** Returns the end position of the error. */
	public int getEndPos() {
		if (endOffsetPos != null) {
			return endOffsetPos.getOffset();
		}
		return endPos;
	}
	
	/** Returns the line number */
	public Integer getLineNumber() {
		if (buffer != null) {
			return buffer.getLineOfOffset(startOffsetPos.getOffset());
		}
		return null;
	}
	
	public Integer getEndLineNumber() {
		if (buffer != null) {
			return buffer.getLineOfOffset(endOffsetPos.getOffset());
		}
		return null;
	}
	
	/** Offset on the line of the position */ 
	public Integer getLineOffset() {
		if (buffer != null) {
			return getStartPos()-buffer.getLineStartOffset(getLineNumber());
		}
		return null;
	}
	/** Line of the endpoint */
	public Integer getEndLine() {
		if (buffer != null) {
			return buffer.getLineOfOffset(getEndPos());
		}
		return null;		
	}
	/** Offset of the endpoint on its line */ 
	public Integer getEndLineOffset() {
		if (buffer != null) {
			int end_offset = 0;
			int end_line = getEndLine();
			if (end_line == getLineNumber()) {
				end_offset = endPos - buffer.getLineStartOffset(end_line);
			}
			return end_offset;
		}
		return null;
	}
	
}
