package CLI;

/**
 * Executor of ImageJ macros (for the Fiji scripting framework).
 *
 * Copyright (C) 2010 Johannes E. Schindelin.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import common.RefreshScripts;

import ij.macro.Interpreter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Refresh_Macros extends RefreshScripts {
	@Override
	public void run(String arg) {
		setLanguageProperties(".ijm", "ImageJ Macro");
		setVerbose(false);
		super.run(arg);
	}

	/** Run an ImageJ Macro. */
	@Override
	public void runScript(String path) {
		try {
			InputStream s = new FileInputStream(new File(path));
			// runScript(InputStream) will close the stream
			runScript(new BufferedInputStream(s));
		} catch (Throwable t) {
			printError(t);
		}
	}

	/** Will consume and close the stream. */
	@Override
	public void runScript(InputStream in) {
		try {
			new Interpreter().run(read(in));
		} catch (Throwable t) {
			printError(t);
		} finally {
			try {
				in.close();
			} catch (Exception e) {
				System.out.println("runScript() could not close"
					+ " the stream!");
				e.printStackTrace();
			}
		}
	}

	@Override
	protected boolean isThisLanguage(String command) {
		return super.isThisLanguage(command) ||
			(command != null &&
			 command.startsWith("ij.plugin.Macro_Runner(\"") &&
			 (command.endsWith(".ijm\")") || command.endsWith(".txt\")")));
	}

	/** Read the complete input stream, return as a String */
	protected String read(InputStream in) throws IOException {
		StringBuffer buffer = new StringBuffer();
		byte[] buf = new byte[16384];
		for (;;) {
			int count = in.read(buf);
			if (count < 0)
				break;
			buffer.append(new String(buf, 0, count));
		}
		in.close();
		return buffer.toString();
	}
}