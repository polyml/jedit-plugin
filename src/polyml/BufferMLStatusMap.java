/**
 * 
 */
package polyml;

import java.util.HashMap;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.jEdit;

/**
 * Provides the mapping between buffers and their compile results, for
 * retrieval and display by (for example) a StateViewDocument.
 * 
 * TODO: Ensure this is loaded early enough to catch all Poly errors.
 *       I'm hoping that the static instance will ensure this. 
 */
public class BufferMLStatusMap implements EBComponent {

	private static BufferMLStatusMap instance;
	HashMap<Buffer, CompileResult> buffers;
	
	public BufferMLStatusMap() {
		// add ourselves to the bus from the start.
		buffers = new HashMap<Buffer, CompileResult>();
		BufferMLStatusMap.unregister();
		instance = this;
		EditBus.addToBus(instance);
	}
	
	/**
	 * Removes this from the editbus.
	 * Be sure that you no longer require the instance before doing this...
	 */
	static void unregister() {
		if (instance != null) {
			EditBus.removeFromBus(instance);
			instance = null;
		}
	}
	
	/**
	 * Gets the compileResult provided for a buffer, if it exists.
	 */
	public CompileResult getResultFor(Buffer b) {
		if (buffers.containsKey(b)) {
			return buffers.get(b);
		} else {
			return null;
		}
	}
	
	/**
	 * Sets a new compile result for a buffer and fires off an event to mark this.
	 * @param b buffer to set or reset
	 * @param cr the new compile result, or null to remove the compile data.
	 */
	public void setResultFor(Buffer b, CompileResult cr) {
		if (cr == null) {
			buffers.remove(b);
		} else if (!cr.equals(getResultFor(b))) {
			buffers.put(b, cr);
			// fire off an update
			new PolyEBMessage(this, PolyMsgType.BUFFER_UPDATE, b).send();
		}
	}
	
	/**
	 * Sets a new compile result for a file, firing off an event to mark this.
	 * Might throw an error if a buffer does not exist for the given file.
	 */
	public void setResultFor(String filename, CompileResult cr) {
		setResultFor(jEdit.getBuffer(filename), cr);
	}
	
	/**
	 * Receives an updates map (probably from a {@link PolyMarkupPushStream}) on the bus.
	 */
	public void handleMessage(EBMessage message) {
		// we're only interested in one type of message
		if (message instanceof PolyEBMessage) {
			PolyEBMessage msg = (PolyEBMessage) message;
			if (msg.getType() == PolyMsgType.COMPILE_RESULT) {
				CompileResult cr = (CompileResult) msg.getPayload();
				for (PolyMLError e : cr.errors) {
					// TODO: more robust filename / buffer retrieval
					if (e.fileName != null && jEdit.getBuffer(e.fileName) != null) {
						setResultFor(jEdit.getBuffer(e.fileName), cr);
						break;
					}
				}
				new PolyEBMessage(this, PolyMsgType.INFORMATION, "Could not extract filename from "+cr+".").send();
			}
		}
	}

	
}
