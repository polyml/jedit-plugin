package polyml;

import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;
import org.gjt.sp.util.SyntaxUtilities;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides methods common to PolyML's TextAreaExtension classes.
 */
public abstract class ErrorVisualisation extends TextAreaExtension {

    /* Some common values */
    protected static final ImageIcon ERROR_ICON = new ImageIcon(PolyMLPlugin.class.getResource("img/error.png"));
    protected static final ImageIcon WARNING_ICON = new ImageIcon(PolyMLPlugin.class.getResource("img/warning.png"));
    protected static final ImageIcon INFO_ICON = new ImageIcon(PolyMLPlugin.class.getResource("img/info.png"));
    //private static final ImageIcon OK_ICON = new ImageIcon(PolyMLPlugin.class.getResource("ok.png"));
	/*protected static final Map<Character, ImageIcon> ICON_MAP = new HashMap<Character, ImageIcon>(){{
		put(PolyMarkup.KIND_TYPE_INFO, INFO_ICON);
		put(PolyMLError.KIND_EXCEPTION, ERROR_ICON);
		put(PolyMLError.KIND_FATAL, ERROR_ICON);
		put(PolyMLError.KIND_PRELUDE_FAILURE, ERROR_ICON);
		put(PolyMLError.KIND_WARNING, WARNING_ICON);
	}};*/
    private static final Color WARNING_COLOUR = SyntaxUtilities.parseColor(jEdit.getProperty("options.polyml.warningColor"), Color.black);
    private static final Color ERROR_COLOUR = SyntaxUtilities.parseColor(jEdit.getProperty("options.polyml.warningColor"), Color.black);
    private static final Color INFO_COLOUR = SyntaxUtilities.parseColor(jEdit.getProperty("options.polyml.warningColor"), Color.black);
    protected static final Map<Character, Color> COLOUR_MAP = new HashMap<Character, Color>() {{
        put(PolyMarkup.KIND_TYPE_INFO, INFO_COLOUR);
        put(PolyMLError.KIND_EXCEPTION, ERROR_COLOUR);
        put(PolyMLError.KIND_FATAL, ERROR_COLOUR);
        put(PolyMLError.KIND_PRELUDE_FAILURE, ERROR_COLOUR);
        put(PolyMLError.KIND_WARNING, WARNING_COLOUR);
    }};

    protected EditPane editPane;

    /**
     * Creates the extension, associating it with a given EditPane.
     */
    protected ErrorVisualisation(EditPane editPane) {
        this.editPane = editPane;
    }

    /**
     * @return information for the current buffer, or an empty list if none.
     */
    protected List<PolyMLError> getBufferInfo() {
        try {
            String path = editPane.getBuffer().getPath();
            CompileRequest rq = PolyMLPlugin.polyMLProcess.compileInfos.getFromPath(path);
            CompileResult rs = rq.getResult();
            return rs.errors;
        } catch (NullPointerException e) {
            return new ArrayList<>(0);
        }
    }

    /**
     * Gets all errors on a specified line.
     */
    protected List<PolyMLError> getInfoOnLine(int line) {
        JEditTextArea area = editPane.getTextArea();

        List<PolyMLError> errs = new ArrayList<>();
        for (PolyMLError e : getBufferInfo()) {
            int lineNo = area.getLineOfOffset(e.getStartPos());
            if (lineNo != line) continue;
            errs.add(e);
        }
        return errs;
    }

    /**
     * Adds this extension to the editPane.
     * Gutter Extension extensions to an editpane
     */
    public abstract void addToPane();

    /**
     * Adds our extensions to an editpane
     */
    public void removeTextAreaExtensions() {
    }

}
