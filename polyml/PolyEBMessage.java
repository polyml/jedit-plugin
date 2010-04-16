/**
 * 
 */
package polyml;

import javax.swing.SwingUtilities;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;

/**
 * Types of PolyEB message.  A bit less boilerplate than a type hierarchy.
 */
enum PolyMsgType {
	/* boolean; notifies of poly activity (or inactivity) */
	POLY_WORKING,
	/* notifies a state view that a buffer's status has been updated */
	BUFFER_UPDATE,
	/* returns a compile result object for storage */
	COMPILE_RESULT,
	/* transient information / warning message. TODO: consider removal. */
	INFORMATION,
	/* TRANSITIONAL message type to replace errors.  TODO: remove this. */
	TRANSITIONAL,
}

/**
 * A message internal to the PolyML plugin which notifies of (or contains)
 * PolyML interaction status information.
 * 
 * A very generic carrier format until we find that we need something
 * more specialised.
 */
public class PolyEBMessage extends EBMessage {

	private final PolyMsgType typ;
	private final Object payload;
	// silly scoping trick for send()
	final PolyEBMessage me = this;

	/**
	 * @param source the component source.
	 */
	public PolyEBMessage(EBComponent source, PolyMsgType type, Object pkg) {
		super(source);
		typ = type;
		payload = pkg;
	}

	/**
	 * @param source the component source.
	 */
	public PolyEBMessage(Object source, PolyMsgType type, Object pkg) {
		super(source);
		typ = type;
		payload = pkg;
	}

	public PolyMsgType getType() {
		return typ;
	}

	/**
	 * Gets object's payload.  I suppose we could do this 
	 */
	public Object getPayload() {
		return payload;
	}

	/** Generates and sends the error in a background thread. */
	public void send() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				EditBus.send(me);
			}
		});
	}
	
	/**
	 * Augmented string representation of the message.
	 */
	public String toString() {
		return super.toString()+":"+typ+" (payload="+payload+")";
	}

}
