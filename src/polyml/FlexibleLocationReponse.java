package polyml;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;

import polyml.PolyMarkupPushStream.FullLocationResponse;
import polyml.PolyMarkupPushStream.LocationResponse;

public class FlexibleLocationReponse extends FlexibleLocationInfo {

	/**
	 * Create a flexible location object from a location response.
	 * @param l the location response.
	 * @param cr optional fallback compilerequest, for filename determination.
	 */
	public FlexibleLocationReponse(FullLocationResponse l, CompileRequest cr) {
		Buffer srcBuffer = jEdit.getBuffer(cr.fileName);
		Buffer locBuffer = jEdit.getBuffer(l.filenameLoc);
		if (l.locGiven()) {
			startPos = l.startLoc;
			endPos = l.endLoc;
		} else {
			startPos = l.start;
			endPos = l.end;
		}
		if (locBuffer != null) {
			associateWithBuffer(locBuffer);
		} else {
			associateWithBuffer(srcBuffer);
		}
	}

	/**
	 * This constructor results in a LocationInfo implementation which is _not_
	 * flexible by default.  We need to associate the buffer afterwards.   
	 * @param l location response
	 * @param cr the compilerequest associated with this locationresonse
	 */
	public FlexibleLocationReponse(LocationResponse l, CompileRequest cr) {
		this(l.start, l.end, jEdit.getBuffer(cr.fileName));
	}

	public FlexibleLocationReponse(int start, int end, Buffer buffer) {
		startPos = start;
		endPos = end;
		associateWithBuffer(buffer);
	}
}
