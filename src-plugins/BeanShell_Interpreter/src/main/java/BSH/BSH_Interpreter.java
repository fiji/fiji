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

import bsh.EvalError;

import ij.IJ;

import bsh.Interpreter;

import common.AbstractInterpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;


public class BSH_Interpreter extends AbstractInterpreter {

	private Interpreter interp;

	synchronized public void run(String arg) {
		super.window.setTitle("BeanShell Interpreter");
		super.run(arg);
		interp = new Interpreter();
		println("Starting BeanShell...");
		if (out != null) {
			PrintStream out = new PrintStream(this.out);
			interp.setOut(out);
			interp.setErr(out);
		}
		importAll();
		println("Ready -- have fun.\n>>>");
	}

	protected Object eval(final String text) throws Throwable {
		return interp.eval(text);
	}

	protected String getImportStatement(String packageName, Iterable<String> classNames) {
		StringBuffer sb = new StringBuffer();
		if (!"".equals(packageName))
			packageName += ".";
		for (String className : classNames)
			sb.append("import ").append(packageName)
				.append(className).append(";\n");
		return sb.toString();
	}

	protected String getLineCommentMark() {
		return "//";
	}

	public static String execute(String... args) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			execute(out, args);
		} catch (Exception e) {
			String output = out.toString();
			if (!"".equals(output))
				IJ.log("Got output before exception:\n" + output);
			throw e;
		}
		return out.toString();
	}

	public static void execute(OutputStream out, String... args) throws Exception {
		try {
			PrintStream printStream = new PrintStream(out);
			Interpreter interp = new Interpreter((Reader)null, printStream, printStream, false);
			String path = args[0];
			String[] bshArgs = new String[args.length - 1];
			System.arraycopy(args, 1, bshArgs, 0, bshArgs.length);
			interp.set("bsh.args", bshArgs);
			interp.eval(new FileReader(path));
		} catch (EvalError e) {
			e.setMessage(e.toString());
			throw e;
		}
	}
}