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

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

/**
 * @author     Lucas Dixon
 */
public class PolyMLPluginOptionsPane extends AbstractOptionPane
{
	private static final long serialVersionUID = -9030659155170934671L;

	/** the polyml command to run for the ide mode: with markup in messages, etc */
	private JTextField polyideCommand;
	/** Textfield the shell Command to start */
	private JTextField shellCommand;
	/** Extra text to put between process output and user input */
	private JTextField shellPrompt;
	private JTextField cssFile;
	/** */
	private JCheckBox outputToDebugBuffer;
	private JCheckBox useFileDir;

	private JCheckBox editableDocument;
	private JCheckBox scrollOnOutput;
	private JCheckBox refreshOnBuffer;
	
	/**
	 * Default constructor. Note that the name is important!
	 */
	public PolyMLPluginOptionsPane() {
		super(jEdit.getProperty("options.polyml-options.label", "PolyML"));
	}

	/**
	 * Create and initialise the options page with options
	 * and labels read from the properties for this plugin
	 */
	public void _init() {
		outputToDebugBuffer = new JCheckBox("Copy output to debug buffer", 
				Boolean.parseBoolean(jEdit.getProperty(PolyMLPlugin.PROPS_COPY_OUTPUT_TO_DEBUG_BUFFER)));
		addComponent(outputToDebugBuffer);
		
		useFileDir = new JCheckBox("Start ML from the files directory?", 
				Boolean.parseBoolean(jEdit.getProperty(PolyMLPlugin.PROPS_RUN_FROM_FROM_FILE_DIR)));
		useFileDir.setToolTipText("If unchecked, starts polyML from a file's project directory.");
		addComponent(useFileDir);
		
		editableDocument = new JCheckBox("State View Document editable?",
				Boolean.parseBoolean(jEdit.getProperty(PolyMLPlugin.PROPS_STATE_DOC_EDITABLE)));
		editableDocument.setToolTipText("Allow the document which displays prover state to be edited (for debugging)");
		addComponent(editableDocument);	
		
		scrollOnOutput = new JCheckBox("Scroll to bottom on output?",
				Boolean.parseBoolean(jEdit.getProperty(PolyMLPlugin.PROPS_SCROLL_ON_OUTPUT)));
		scrollOnOutput.setToolTipText("Scroll to the bottom of the status document when output changes?");
		addComponent(scrollOnOutput);
		
		refreshOnBuffer = new JCheckBox("Refresh error list on buffer change?",
				Boolean.parseBoolean(jEdit.getProperty(PolyMLPlugin.PROPS_REFRESH_ON_BUFFER)));
		refreshOnBuffer.setToolTipText("Append all relevant errors when a new buffer is viewed?");
		addComponent(refreshOnBuffer);
		
		polyideCommand = new JTextField(jEdit.getProperty(PolyMLPlugin.PROPS_POLY_IDE_COMMAND), 25);
		polyideCommand.setToolTipText("Command to start PolyML for processing edited files.");
		addComponent("PolyML IDE Command", polyideCommand);
		
		shellCommand = new JTextField(jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_COMMAND), 25);
		shellCommand.setToolTipText("Command to stary PolyML for interactive buffers.");
		addComponent("PolyML Shell Command", shellCommand);
		
		shellPrompt = new JTextField(jEdit.getProperty(PolyMLPlugin.PROPS_SHELL_PROMPT), 10);
		shellPrompt.setToolTipText("Prompt string displayed by interactive shell buffers.");
		addComponent("Interactive Shell Prompt", shellPrompt);
		
		cssFile = new JTextField(jEdit.getProperty(PolyMLPlugin.PROPS_STATE_OUTPUT_CSS_FILE), 30);
		cssFile.setToolTipText("Path of CSS file to apply to the PolyML state panel.  Panel reset required.");
		addComponent("State Panel Style file", cssFile);
	}

	/**
	 * Store the options selected on the pane back to the 
	 * jedit properties.
	 */
	public void _save() {
		jEdit.setProperty(PolyMLPlugin.PROPS_COPY_OUTPUT_TO_DEBUG_BUFFER, String.valueOf(outputToDebugBuffer.isSelected()));
		jEdit.setProperty(PolyMLPlugin.PROPS_RUN_FROM_FROM_FILE_DIR, String.valueOf(useFileDir.isSelected()));
		jEdit.setProperty(PolyMLPlugin.PROPS_POLY_IDE_COMMAND, polyideCommand.getText());
		jEdit.setProperty(PolyMLPlugin.PROPS_SHELL_COMMAND, shellCommand.getText());
		jEdit.setProperty(PolyMLPlugin.PROPS_SHELL_PROMPT, shellPrompt.getText());
		jEdit.setProperty(PolyMLPlugin.PROPS_STATE_OUTPUT_CSS_FILE, cssFile.getText());
		jEdit.setProperty(PolyMLPlugin.PROPS_STATE_DOC_EDITABLE, String.valueOf(editableDocument.isSelected()));
		jEdit.setProperty(PolyMLPlugin.PROPS_SCROLL_ON_OUTPUT, String.valueOf(scrollOnOutput.isSelected()));
		jEdit.setProperty(PolyMLPlugin.PROPS_REFRESH_ON_BUFFER, String.valueOf(refreshOnBuffer.isSelected()));
		
		if(!PolyMLPlugin.restartPolyML()) {
			JOptionPane.showMessageDialog(null, "PolyML restart failed.", 
					"The PolyML IDE-command ('" + polyideCommand.getText() 
					+ "') failed to successfully start PolyML."
					, JOptionPane.WARNING_MESSAGE);
		}
	}
	
}

