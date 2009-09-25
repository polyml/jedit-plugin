//package polyml;
//
//import javax.swing.JPanel;
//import java.awt.BorderLayout;
//import java.awt.Dimension;
//import javax.swing.JButton;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//
//import org.xhtmlrenderer.simple.XHTMLPanel;
//import org.xhtmlrenderer.simple.FSScrollPane;
//import org.xhtmlrenderer.context.AWTFontResolver;
//import org.xhtmlrenderer.layout.SharedContext;
//import org.xhtmlrenderer.extend.TextRenderer;
//import org.xhtmlrenderer.swing.Java2DTextRenderer;
//
//import org.gjt.sp.jedit.jEdit;
//import org.gjt.sp.jedit.View;
//import org.gjt.sp.jedit.gui.DockableWindowManager;
//import org.gjt.sp.jedit.textarea.AntiAlias;
//
//
//
///*
// * Dockable window with rendered state output
// *
// * @author Lucas Dixon
// */
//
//public class StateViewDockable extends JPanel {
//
//	public StateViewDockable(View view, String position) {
//
//		  // outer panel
//		  if (position == DockableWindowManager.FLOATING)
//		    setPreferredSize(new Dimension(500, 250));
//		  setLayout(new BorderLayout());
//
//
//		  // XHTML panel
//		  XHTMLPanel panel = new XHTMLPanel(new UserAgent());
//
//
//		  // anti-aliasing
//		  // TODO: proper EditBus event handling
//		  {
//		    val aa = jEdit.getProperty("view.antiAlias")
//		    if (aa != null && aa != AntiAlias.NONE) {
//		      panel.getSharedContext.setTextRenderer(
//		        {
//		          val renderer = new Java2DTextRenderer
//		          renderer.setSmoothingThreshold(0)
//		          renderer
//		        })
//		    }
//		  }
//
//		  
//		  // copy & paste
//		  (new SelectionActions).install(panel)
//
//
//		  // scrolling
//		  add(new FSScrollPane(panel), BorderLayout.CENTER);
//		  
//
//		  private val fontResolver =
//		    panel.getSharedContext.getFontResolver.asInstanceOf[AWTFontResolver]
//		  if (Isabelle.plugin.font != null)
//		    fontResolver.setFontMapping("Isabelle", Isabelle.plugin.font)
//
//		  Isabelle.plugin.font_changed += (font => {
//		    if (Isabelle.plugin.font != null)
//		      fontResolver.setFontMapping("Isabelle", Isabelle.plugin.font)
//
//		    panel.relayout()
//		  })
//	}
//	
//}

