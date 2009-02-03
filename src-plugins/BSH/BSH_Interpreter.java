package BSH;

/*
 * A dynamic BeanShell interpreter plugin for ImageJ(C).
 * Copyright (C) 2008 Johannes Schindelin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation 
 * (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 
*/
import ij.IJ;

import bsh.Interpreter;

import common.AbstractInterpreter;

public class BSH_Interpreter extends AbstractInterpreter {

	static private boolean loaded = false;

	private Interpreter interp;

	synchronized public void run(String arg) {
		if (loaded) {
			IJ.showMessage("BSH Interpreter already started.");
			return;
		}

		interp = new Interpreter();
		loaded = true;
		super.screen.append("Starting BeanShell...");
		super.screen.append(" Ready -- have fun.\n>>>");
		// ok create window
		super.run(arg);
		super.window.setTitle("BeanShell Interpreter");
	}

	/** Evaluate clojure code. */
	protected Object eval(final String text) throws Throwable {
		Object ret = interp.eval(text);
		return ret;
	}

	protected String getLineCommentMark() {
		return "//";
	}
}
