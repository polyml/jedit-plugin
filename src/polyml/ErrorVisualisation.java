package polyml;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.TextAreaExtension;

/**
 * Provides methods common to PolyML's TextAreaExtension classes.
 */
public abstract class ErrorVisualisation extends TextAreaExtension {

	/* Some common values */
	protected static final ImageIcon ERROR_ICON = new ImageIcon(PolyMLPlugin.class.getResource("error.png"));
	protected static final ImageIcon WARNING_ICON = new ImageIcon(PolyMLPlugin.class.getResource("warning.png"));
	protected static final ImageIcon INFO_ICON = new ImageIcon(PolyMLPlugin.class.getResource("info.png"));
	//private static final ImageIcon OK_ICON = new ImageIcon(PolyMLPlugin.class.getResource("ok.png"));
	/*protected static final Map<Character, ImageIcon> ICON_MAP = new HashMap<Character, ImageIcon>(){{
		put(PolyMarkup.KIND_TYPE_INFO, INFO_ICON);
		put(PolyMLError.KIND_EXCEPTION, ERROR_ICON);
		put(PolyMLError.KIND_FATAL, ERROR_ICON);
		put(PolyMLError.KIND_PRELUDE_FAILURE, ERROR_ICON);
		put(PolyMLError.KIND_WARNING, WARNING_ICON);
	}};*/
	private static Color WARNING_COLOUR = GUIUtilities.parseColor(jEdit.getProperty("options.polyml.warningColor"));
	private static Color ERROR_COLOUR = GUIUtilities.parseColor(jEdit.getProperty("options.polyml.warningColor"));
	private static Color INFO_COLOUR = GUIUtilities.parseColor(jEdit.getProperty("options.polyml.warningColor"));
	protected static final Map<Character, Color> COLOUR_MAP = new HashMap<Character, Color>(){{
		put(PolyMarkup.KIND_TYPE_INFO, INFO_COLOUR);
		put(PolyMLError.KIND_EXCEPTION, ERROR_COLOUR);
		put(PolyMLError.KIND_FATAL, ERROR_COLOUR);
		put(PolyMLError.KIND_PRELUDE_FAILURE, ERROR_COLOUR);
		put(PolyMLError.KIND_WARNING, WARNING_COLOUR);
	}};
	
	protected EditPane editPane;
	
	/**
	 * Creates the extension, associating it with a given EditPane.
	 * @param editPane
	 */
	protected ErrorVisualisation(EditPane editPane) {
		this.editPane = editPane;
	}
	
	/**
	 * @return information for the current buffer, or an empty list if none.
	 */
	protected List<PolyMLError> getBufferInfo() {
		List<PolyMLError> list = new ArrayList<PolyMLError>(0);
		if (editPane != null && editPane.getBuffer() != null) {
			BufferMLStatusMap map = PolyMLPlugin.compileMap;
			Buffer b = editPane.getBuffer();
			if (map.buffers.containsKey(b) && map.getResultFor(b) != null) {
				return map.getResultFor(b).errors;
			}
		}
		return list;
	}
	
	/**
	 * Adds this extension to the editPane.
	 * Gutter Extension extensions to an editpane
	 * @param pane the pane to which to add extensions
	 */
	public abstract void addToPane();

	/**
	 * Adds our extensions to an editpane
	 * @param editPane the pane to which to add extensions
	 */
	public void removeTextAreaExtensions() {


	}

}
