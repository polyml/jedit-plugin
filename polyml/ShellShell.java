/* -----------------------------------------------------------------------------
 *       Copyright
 * 
 *       (C) 2007 Lucas Dixon
 * 
 *       License
 * 
 *       This program is free software; you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation; either version 1, or (at your option)
 *       any later version.
 * 
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 * 
 * -------------------------------------------------------------------------- */

package polyml;

import java.awt.Color;
import java.io.IOException;

import console.Console;
import console.Output;
import console.Shell;

/**
 * SmlShell is integrated in jEdit by SMLPlugin.
 * 
 * @author <a href="mailto:l.dixon@ed.ac.uk">Lucas Dixon</a>
 * @created 03 November 2007
 * @version 1.0
 */
public final class ShellShell extends Shell {

	ShellBuffer mShellBuffer;

	/**
	 * a new SmlShell with the name "SML"
	 */
	public ShellShell() {
		super("ShellShell");
		mShellBuffer = PolyMLPlugin.newShellBuffer();
	}

	/**
	 * Description of the Method
	 * 
	 * @param console
	 *            is the console this shell lives in and is displayed in.
	 * @param output
	 *            is the stdout output from this shell
	 * @param input
	 *            something to do with stdin ? typed by the user?
	 * @param command
	 *            some kind of internal command information?
	 * @param error
	 *            is for error messages from this shell
	 */
	public void execute(Console console, java.lang.String input, Output output,
			Output error, java.lang.String command) {
		if (mShellBuffer == null) {
			output.print(Color.RED, "mShellBuffer is null - failed to start command? cmd: " + command
					+ "; input: " + input + ".");
			output.commandDone();
		} else {
			output.print(Color.BLUE, "got command: " + command
					+ "; with input: " + input + ".");
			try {
				mShellBuffer.send(command);
			} catch (IOException e) {
				error.print(Color.RED, "Process failed to send to shell:"
						+ command + "\n it raised an IOException: "
						+ e.toString());
			}
			output.print(Color.BLUE, "send over. ");
			output.commandDone();
		}
	}

	/**
	 * Stops the currently executing command, if any.
	 */
	public void stop() {
		mShellBuffer.stopProcess();
	}

	public void stop(Console console) {
		stop();
	}
}
