package Clojure;

/*
A dynamic Clojure interpreter plugin for ImageJ(C).
Copyright (C) 2008 Albert Cardona.
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 

You may contact Albert Cardona at acardona at ini phys ethz ch.
*/
import ij.IJ;

import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Namespace;
import clojure.lang.Compiler;
import clojure.lang.LispReader;
import clojure.lang.Var;
import clojure.lang.LineNumberingPushbackReader;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.ArrayList;
import java.io.PipedWriter;
import java.io.PipedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.Reader;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.Map;

import common.AbstractInterpreter;

public class Clojure_Interpreter extends AbstractInterpreter {

	static final Symbol USER = Symbol.create("user");
	static final Symbol CLOJURE = Symbol.create("clojure.core");

	static final Var in_ns = RT.var("clojure.core", "in-ns");
	static final Var refer = RT.var("clojure.core", "refer");
	static final Var ns = RT.var("clojure.core", "*ns*");
	static final Var compile_path = RT.var("clojure.core", "*compile-path*");
	static final Var warn_on_reflection = RT.var("clojure.core", "*warn-on-reflection*");
	static final Var print_meta = RT.var("clojure.core", "*print-meta*");
	static final Var print_length = RT.var("clojure.core", "*print-length*");
	static final Var print_level = RT.var("clojure.core", "*print-level*");
	static final Var star1 = RT.var("clojure.core", "*1");
	static final Var star2 = RT.var("clojure.core", "*2");
	static final Var star3 = RT.var("clojure.core", "*3");
	static final Var stare = RT.var("clojure.core", "*e");
	static final Var out = RT.var("clojure.core", "*out*");

	static final Object EOF = new Object();

	final private ExecutorService exec = Executors.newFixedThreadPool(1);

	@Override
	public void run(String arg) {
		// Create window, threads, streams:
		super.window.setTitle("Clojure Interpreter");
		super.run(arg);
		// 
		print("Starting Clojure...");
		if (!init()) {
			p("Some error ocurred!");
			return;
		}
		println(" Ready -- have fun.\n" + getPrompt() + "\n");

		// Add crude support for closing parenthesis with control+)
		prompt.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent ke) {
				if (')' == ke.getKeyChar() && ke.isControlDown()) {
					String text = prompt.getText();
					int b = 0;
					for (int i=text.length()-1; i>-1; i--) {
						switch (text.charAt(i)) {
							case '(': b++; break;
							case ')': b--; break;
						}
					}
					if (b > 0) {
						StringBuffer sb = new StringBuffer(text);
						for (int i=0; i<b; i++) sb.append(')');
						prompt.setText(sb.toString());
					} else if (b < 0) {
						IJ.log("There are " + Math.abs(b) + " more closing parentheses than opening ones!");
					}
				}
			}
			public void keyTyped(KeyEvent ke) {}
			public void keyReleased(KeyEvent ke) {}
		});
	}

	@Override
	protected void windowClosing() {
		destroy();
	}

	public void destroy() {
		// Tell the worker Thread to forget all
		if (exec.isShutdown()) return;
		exec.shutdownNow();
	}

	/** Evaluate clojure code. */
	@Override
	protected Object eval(final String text) throws Throwable {
		return evaluate(text);
	}

	public Object evaluate(final String text) throws Throwable {
		return evaluate(new StringReader(text));
	}

	public Object evaluate(final InputStream istream) throws Throwable {
		return evaluate(new BufferedReader(new InputStreamReader(istream)));
	}

	public Object evaluate(final Reader input_reader) throws Throwable {
		Evaluator ev = new Evaluator(input_reader);
		String ret = null;
		try {
			ret = exec.submit(ev).get();
		} catch (Throwable t) {
			if (!Thread.currentThread().isInterrupted()) t.printStackTrace();
		}
		ev.throwError(); // to be printed by super class wherever appropriate
		return ret;
	}

	/** Executes the Callable @param c wrapped in a try/catch to avoid any restart of the clojure thread,
	 *  and returns the result of the execution. */
	public Object submit(final Callable c) {
		try {
			return exec.submit(new Callable() {
				public Object call() {
					try {
						return c.call();
					} catch (Throwable t) {
						t.printStackTrace();
						return null;
					}
				}
			}).get(); // wait until done
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	public boolean pushThreadBindings(Map<String,Object> vars) throws Exception {
		return pushThreadBindings("clojure.core", vars);
	}
	public boolean pushThreadBindings(final String namespace, final Map<String,Object> vars) throws Exception {
		return null != submit(new Callable() {
			public Object call() {
				try {
					for (Map.Entry<String,Object> e : vars.entrySet()) {
						Var.pushThreadBindings(RT.map(RT.var(namespace, e.getKey()), e.getValue()));
					}
					return namespace; // to return something != null
				} catch (Throwable t) {
					t.printStackTrace();
				}
				return null;
			}
		});
	}

	public boolean init() {
		// Initialize the worker Thread with a few bindings and the ImageJ's class loader
		try {
			exec.submit(new Runnable() {
				public void run() {
					try {
						ClassLoader cl = ij.IJ.getClassLoader();
						if (null != cl) {
							Thread.currentThread().setContextClassLoader(cl);
						}

						Var.pushThreadBindings(
							RT.map(ns, ns.get(),
							       warn_on_reflection, warn_on_reflection.get(),
							       print_meta, print_meta.get(),
							       print_length, print_length.get(),
							       print_level, print_level.get(),
							       compile_path, "classes",
							       star1, null,
							       star2, null,
							       star3, null,
							       stare, null,
							       out, null == Clojure_Interpreter.super.print_out ?
							                           out.get() : Clojure_Interpreter.super.print_out));

						//create and move into the user namespace
						in_ns.invoke(USER);
						refer.invoke(CLOJURE);
					} catch (Throwable t) {
						IJ.log("Could not initialize variables for the clojure worker thread!");
						t.printStackTrace();
					}
				}
			}).get(); // wait until done
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}

	/** Create an instance of Evaluator for every chunk of code you want evaluated. */
	private final class Evaluator implements Callable<String> {
		private Reader input_reader;
		private Throwable t = null;
		Evaluator(Reader input_reader) {
			this.input_reader = input_reader;
		}

		/** Returns the result of execution, in text for for printing. */
		public String call() {
			StringBuffer sb = null;
			try {
				sb = parse();
			} catch (Throwable t) {
				this.t = t;
				Var.pushThreadBindings(RT.map(stare, t));
			}
			return null == sb ? "nil" : sb.toString();
		}

		private StringBuffer parse() throws Throwable {
			// prepare input for parser
			final LineNumberingPushbackReader lnpr = new LineNumberingPushbackReader(input_reader);
			// storage for readout
			final StringWriter sw = new StringWriter();

			Object ret = null;

			final Thread thread = Thread.currentThread();

			while (!thread.isInterrupted()) {
				// read one token from the pipe
				Object r = LispReader.read(lnpr, false, EOF, false);
				if (EOF == r) {
					break;
				}
				// evaluate the tokens returned by the LispReader
				ret = Compiler.eval(r);
				// print the result in a lispy way
				RT.print(ret, sw);
				sw.write('\n');
			}

			if (thread.isInterrupted()) {
				// cleanup:
				Var.popThreadBindings();
				return null;
			}

			// update *1, *2, *3, even if null
			Object ob = star2.get();
			Var.pushThreadBindings(RT.map(star3, ob));
			ob = star1.get();
			Var.pushThreadBindings(RT.map(star2, ob));
			// The last returned object of whatever was executed gets set to star1
			Var.pushThreadBindings(RT.map(star1, ret));

			// In case it was not closed:
			try {
				input_reader.close();
			} catch (Throwable ioe) {
				ioe.printStackTrace();
			}

			return sw.getBuffer();
		}

		void throwError() throws Throwable {
			if (null != this.t) throw this.t;
		}
	}

	@Override
	protected String getLineCommentMark() {
		return ";";
	}

	@Override
	protected String getPrompt() {
		// Fetch the *ns* from the executor thread-local variable:
		try {
			return exec.submit(new Callable<String>() {
				public String call() {
					try {
						Var ns = RT.var("clojure.core", "*ns*");
						Symbol s = (Symbol) ((Namespace) ns.get()).getName();
						return s.getName() + "=>";
					} catch (Throwable t) {
						t.printStackTrace();
					}
					return Clojure_Interpreter.super.getPrompt();
				}
			}).get();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return super.getPrompt();
	}
}
