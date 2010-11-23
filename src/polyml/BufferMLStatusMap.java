/**
 * 
 */
package polyml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import java.util.HashMap;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.Selection;

/**
 * Provides the mapping between buffers and their compile results, for
 * retrieval and display by (for example) a StateViewDocument.
 * 
 * TODO: Ensure this is loaded early enough to catch all Poly errors.
 *       I'm hoping that the static instance will ensure this. 
 */
public class BufferMLStatusMap implements EBComponent {

	private static BufferMLStatusMap instance;
	/** List of errors for each buffer */
	HashMap<Buffer, CompileResult> buffers;
	/** List of additional type requests stored against a buffer (possibly after the intial result) */
	public HashMap<Buffer, Collection<PolyMLError>> typeinfos;
	
	public BufferMLStatusMap() {
		// add ourselves to the bus from the start.
		buffers = new HashMap<Buffer, CompileResult>();
		typeinfos = new HashMap<Buffer, Collection<PolyMLError>>();
		BufferMLStatusMap.register(this);
	}

	/**
	 * Returns a list of buffers stored in this map.
	 */
	public Collection<Buffer> getBuffers() {
		return buffers.keySet();
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
	 * Registers a new instance with the EditBus.
	 * Removes any previous instance
	 * @see #unregister()
	 */
	private static void register(BufferMLStatusMap in) {
		unregister();
		instance = in;
		EditBus.addToBus(instance);
	}
	
	/**
	 * Returns the flexible selection associated with the current buffer and given error locations.
	 * @param b buffer
	 * @param start original start offset
	 * @param end original end offset
	 * @return a selection representing the flexible location of the offsets, or the given parameters if not found.
	 */
	public Selection getSelectionFor(Buffer b, int start, int end) {
		if (b == null) return null;
		//String path = b.getSymlinkPath();
		CompileResult r = getResultFor(b);
		if (r == null) {
			return null;
		}
		for (PolyMLError e : r.errors) {
			ViewablePolyMLError ve = (ViewablePolyMLError) e; 
			System.err.println("Looking in error "+e.stringOfError());
			if (e.startPos == start && e.endPos == end) {
				return new Selection.Range(e.getStartPos(), e.getEndPos());
			}
		}
		System.err.println("No matching position was stored.");
		return new Selection.Range(start, end);
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
	 * Gets all errors and type information associated with the given buffer
	 * @param b the buffer for which to return information
	 * @param types return type info?
	 * @return a list as requested.
	 */
	public Collection<PolyMLError> getErrorsFor(Buffer b, boolean types) {
		Collection<PolyMLError> out = new HashSet<PolyMLError>();
		if (buffers.containsKey(b)) {
			out.addAll(buffers.get(b).errors);
		}
		if (types && typeinfos.containsKey(b)) {
			out.addAll(typeinfos.get(b));
		}
		return out;
	}
	
	/**
	 * Gets the current error line numbers for a given buffer 
	 * @deprecated probably.
	 */
	public Collection<Integer> getErrorPositionsFor(Buffer b) {
		Collection<Integer> errors = new ArrayList<Integer>(); 
		if (buffers.containsKey(b)) { 
			for (PolyMLError e : buffers.get(b).errors) {
				ViewablePolyMLError ve = (ViewablePolyMLError) e;
				errors.add(ve.getLineNumber());
			}
		}
		return errors;
	}
		
	/**
	 * Sets a new compile result for a buffer and fires off an event to mark this.
	 * Clears stored type information.
	 * @param b buffer to set or reset
	 * @param cr the new compile result, or null to remove the compile data.
	 */
	public void setResultFor(Buffer b, CompileResult cr) {
		if (b == null) {
			System.err.println("Hmm, being asked to store null buffer...");
		} else {
			System.err.println("Asked to store result for buffer "+b.getName()+"...");
		}
		if (typeinfos.containsKey(b)) {
			typeinfos.remove(b);
		}
		if (cr == null) {
			buffers.remove(b);
		} else if (!cr.equals(getResultFor(b))) {
			buffers.put(b, cr);
			// associate buffer for flexible error positions
			for (PolyMLError e : cr.errors) {
				if (e instanceof ViewablePolyMLError) {
					((ViewablePolyMLError) e).associateBuffer(b);
				}
			}
			// fire off an update
			new PolyEBMessage(this, PolyMsgType.BUFFER_UPDATE, b).send();
		}
	}
	
	/**
	 * Sets a new compile result for a file, firing off an event to mark this.
	 * Might throw an error if a buffer does not exist for the given file.
	 * @param filename the filename for which to find a buffer
	 * @param cr the result to set -- see {@link #setResultFor(Buffer, CompileResult)}.
	 */
	public void setResultFor(String filename, CompileResult cr) {
		setResultFor(jEdit.getBuffer(filename), cr);
	}
	
	/**
	 * Adds the detail of a supplemental CompileResult (i.e. type check)
	 * @param b the buffer associated with this result.
	 * @param cr the result itself.
	 */
	public void addTypeInfo(Buffer b, CompileResult cr) {
		Collection<PolyMLError> el;
		if (typeinfos.containsKey(b)) {
			el = typeinfos.get(b); 
		} else {
			el = new HashSet<PolyMLError>();
		}

		for (PolyMLError e : cr.errors) {
			if (e.kind == PolyMarkup.KIND_TYPE_INFO) {
				el.add(e);
			}
		}
	}
	
	/**
	 * Receives an updates map (probably from a {@link PolyMarkupPushStream}) on the bus.
	 * @see EBComponent#handleMessage(EBMessage).
	 */
	public void handleMessage(EBMessage message) {
		// we're only interested in one type of message
		if (message instanceof PolyEBMessage) {
			PolyEBMessage msg = (PolyEBMessage) message;
			if (msg.getType() == PolyMsgType.COMPILE_RESULT) {
				CompileResult cr = (CompileResult) msg.getPayload();
				if (msg.getSource() instanceof PolyMarkupPushStream) {
					String fname = ((PolyMarkupPushStream) msg.getSource()).compileInfos.getFromParseID(cr.parseID).fileName;
					System.err.println("** Got compile result for "+fname);
					setResultFor(jEdit.getBuffer(fname), cr);
				} else {
					for (PolyMLError e : cr.errors) {
						// TODO: more robust filename / buffer retrieval
						if (e.fileName != null && jEdit.getBuffer(e.fileName) != null) {
							setResultFor(jEdit.getBuffer(e.fileName), cr);
							new PolyEBMessage(this, PolyMsgType.INFORMATION, "Updated compileresult for "+e.fileName+".").send();
							break;
						}
					}
				}
			}
		}
	}

	
}
