/*
 *  ShellBufferOptionPane.java
 *  Copyright (c) 2007 Lucas Dixon
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
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

/**
 * Options pane displayed when Character Map
 * is selected in the Plugin Options... tree
 * Allows the user to customize the appearence of the
 * character map plugin.
 *
 * @author     Lucas Dixon
 * @version    1.0
 */
public class ShellBufferOptionPane extends AbstractOptionPane
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -9030659155170934671L;
	
	
	
	/** Textfield the shell Command to start */
	private JTextArea shellCommand;
	/** Extra text to put between process output and user input */
	private JTextArea shellPrompt;
	/** Panel containing components controllong the Shell command */
	private JPanel shellCommandPanel;

	
	public static final String PROPS_SHELL_COMMAND = "options.polyml.shell-command";
	public static final String PROPS_SHELL_PROMPT = "options.polyml.shell-prompt";
	public static final String PROPS_MAX_HISTORY = "options.polyml.max-history";;
	
	/**
	 * Default constructor. Note that the name is important!
	 */
	public ShellBufferOptionPane() {
		super("Shell Buffer Options");
	}

	/**
	 * Create and initialise the options page with options
	 * and labels read from the properties for this plugin
	 */
	public void _init() {
		shellCommand = new JTextArea(jEdit.getProperty(PROPS_SHELL_COMMAND), 3, 50);
		shellCommandPanel = createLabelledComponent("Shell Command: ", shellCommand);
		addComponent(shellCommandPanel);
		
		shellPrompt = new JTextArea(jEdit.getProperty(PROPS_SHELL_PROMPT), 3, 50);
		shellCommandPanel = createLabelledComponent("Shell Prompt: ", shellPrompt);
		addComponent(shellCommandPanel);
	}

	/**
	 * Store the options selected on the pane back to the 
	 * jedit properties.
	 */
	public void _save() {
		jEdit.setProperty(PROPS_SHELL_COMMAND, shellCommand.getText());
		jEdit.setProperty(PROPS_SHELL_PROMPT, shellPrompt.getText());
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

