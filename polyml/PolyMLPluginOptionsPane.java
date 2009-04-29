/*
 *  ShellBufferOptionPane.java
 *  Copyright (c) 2009 Lucas Dixon
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package polyml;

import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

/**
 *
 * @author     Lucas Dixon
 * @version    1.0
 */
public class PolyMLPluginOptionsPane extends AbstractOptionPane
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -9030659155170934671L;
	
	/** the polyml command to run for the ide mode: with markup in messages, etc */
	private JTextArea polyideCommand;
	/** Textfield the shell Command to start */
	private JTextArea shellCommand;
	/** Extra text to put between process output and user input */
	private JTextArea shellPrompt;
	
	/**
	 * Default constructor. Note that the name is important!
	 */
	public PolyMLPluginOptionsPane() {
		super("PolyML Plugin Options");
	}

	/**
	 * Create and initialise the options page with options
	 * and labels read from the properties for this plugin
	 */
	public void _init() {
		/** Panel containing components controlling the Shell command */
		JPanel p;
		
		polyideCommand = new JTextArea(jEdit.getProperty(PolyMLPlugin.PROPS_POLY_IDE_COMMAND), 3, 50);
		p = createLabelledComponent("PolyML IDE Command: ", polyideCommand);
		addComponent(p);
		
		shellCommand = new JTextArea(jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_COMMAND), 3, 50);
		p = createLabelledComponent("PolyML Shell Command: ", shellCommand);
		addComponent(p);
		
		shellPrompt = new JTextArea(jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_PROMPT), 3, 50);
		p = createLabelledComponent("Interactive Shell Prompt: ", shellPrompt);
		addComponent(p);
	}

	/**
	 * Store the options selected on the pane back to the 
	 * jedit properties.
	 */
	public void _save() {
		jEdit.setProperty(PolyMLPlugin.PROPS_POLY_IDE_COMMAND, polyideCommand.getText());
		jEdit.setProperty(PolyMLPlugin.PROPS_SHELL_COMMAND, shellCommand.getText());
		jEdit.setProperty(PolyMLPlugin.PROPS_SHELL_PROMPT, shellPrompt.getText());
		
		if(!PolyMLPlugin.restartPolyML()) {
			JOptionPane.showMessageDialog(null, "PolyML restart failed.", 
					"The PolyML IDE-command ('" + polyideCommand.getText() 
					+ "') failed to successfully start PolyML."
					, JOptionPane.WARNING_MESSAGE);
		}
	}

	/** 
	 * Create a JLabel containing the given string and put it together
	 * with the given component into a panel The two are separated by a colon,
	 * and are layed out using the flow layout manager.
	 *
	 * @param label label to the left of the component.
	 * @param component appears to the right of the label.
	 * @return panel holding labele and component.
	 */
	private JPanel createLabelledComponent(String label, JComponent component) {
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());
		p.add(new JLabel(label));
		p.add(component);
		return p;
	}
}

