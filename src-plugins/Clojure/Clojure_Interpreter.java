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

	static final Object EOF = new Object();

	static private boolean loaded = false;

	public void run(String arg) {
		// synchronized with the destroy() method
		synchronized (EOF) {
			if (loaded) {
				IJ.showMessage("You can only have one instance of the Clojure interpreter running.");
				return;
			}

			loaded = true;
			super.screen.append("Starting Clojure...");
			final LispThread thread = LispThread.getInstance();
			if (!thread.ready()) {
				p("Some error ocurred.");
				return;
			}
			thread.setStdOut(super.print_out);
			super.screen.append(" Ready -- have fun.\n>>>\n");
			// ok create window
			super.run(arg);
			super.window.setTitle("Clojure Interpreter");

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
	}

	/** Override super. */
	protected void windowClosing() {
		LispThread instance = LispThread.getInstance();
		//if (null != instance) instance.quit(); // CAN'T QUIT, there may be other threads running it.
		loaded = false;
	}

	/** Evaluate clojure code. Calls static method evaluate(text). */ // overrides super
	protected Object eval(final String text) throws Throwable {
		return evaluate(text);
	}

	/** Calls eval(text) on the LispThread, which is then not destroyed. See the Refresh_Clojure_Scripts for an example.
	* Parses in the context of (binding [*out* (Clojure.Clojure_Interpreter/getStdOut)] &amp; body) if the PrintWriter out is not null.*/ // Overrides super method
	static public Object evaluate(final String text) throws Throwable {
		LispThread thread = LispThread.getInstance();
		Object ret = thread.eval(text);
		thread.throwError();
		return ret;
	}

	static public PrintWriter getStdOut() {
		return LispThread.getStdOut();
	}

	/** Complicated Thread setup just to be able to initialize and cleanup within the context of the same Thread, as required by Clojure. All Clojure scripts will evaluate within the context of this singleton class. */
	static private class LispThread extends Thread {

		static private LispThread instance = null;
		static private PrintWriter out = null;
		private boolean go = false;
		private String text = null;
		private String result = null;
		private Throwable error = null;
		private boolean working = false;
		private final Object RUN = new Object();
		private boolean locked = false;
		private final void lock() {
			while (locked) try { this.wait(); } catch (InterruptedException ie) {}
			locked = true;
		}
		private final void unlock() {
			locked = false;
			this.notifyAll();
		}
		private LispThread() {
			super("Clojure Thread");
			setPriority(Thread.NORM_PRIORITY);
			try { setDaemon(true); } catch (Exception e) { e.printStackTrace(); }
			start();
			while (!go) {
				try { Thread.sleep(100); } catch (InterruptedException ie) {}
			}
		}

		static public LispThread getInstance() {
			if (null == instance) instance = new LispThread();
			return instance;
		}
		static public void setStdOut(PrintWriter out_) {
			out = out_;
		}
		static public PrintWriter getStdOut() {
			return out;
		}

		boolean ready() {
			boolean b = false;
			synchronized (this) {
				lock();
				try { b = isAlive(); } catch (Throwable e) { e.printStackTrace(); }
				unlock();
			}
			return b;
		}
		void throwError() throws Throwable {
			Throwable t = null;
			synchronized (this) {
				lock();
				t = error;
				error = null;
				unlock();
			}
			if (null != t) throw t;
		}
		private void setup() {
			synchronized (this) {
				lock();
				try {
					init();
				} catch (Throwable e) {
					e.printStackTrace();
				}
				// Outside try{}catch for it must always be true on start, so the thread can start, notify and die on error.
				go = true;
				unlock();
			}
		}
		void quit() {
			go = false;
			synchronized (this) {
				instance = null;
				out = null;
				unlock();
			}
			synchronized (RUN) {
				RUN.notifyAll();
			}
		}
		/** Parsing in the context of (binding [*out* (Clojure.Clojure_Interpreter/getStdOut)] &amp; body) if this.out is not null.*/
		String eval(String text) {
			String res = null;
			try {
				boolean with_out_str = false;
				synchronized (this) {
					lock();
					//this.text = "(with-out-str\n " + text.trim() + ")";
					text = text.trim();
					/*
					if (text.matches("^.*\\bpr\\b.*$")
					 || text.matches("^.*\\bprn\\b.*$")
					 || text.matches("^.*\\bprint\\b.*$")
					 || text.matches("^.*\\bprintln\\b.*$")
					) {
						with_out_str = true;
						text = "(with-out-str\n " + text + ")";
					}
					*/
					if (null != out) {
						text = "(binding [*out* (Clojure.Clojure_Interpreter/getStdOut)]\n" + text + ")\n";
					}
					this.text = text;
					working = true;
					unlock();
				}
				Thread.yield();
				synchronized (RUN) {
					// start parsing iteration in the thread
					RUN.notifyAll();
					// wait until done
					while (working) try { RUN.wait(); } catch (InterruptedException ie) {}
					// fix result
					res = result;
					result = null;
					// equivalent to wrapping the abovew with-out-str in a (prn ...)
					// (but then of course it would not print were I can catch it)
					if (null != res && with_out_str) {
						res = res.substring(1, res.length() -1)
							 .replace("\\n", "\n")
							 .replace("\\", "");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return res;
		}
		public void run() {
			setup();
			while (go) {
				synchronized (RUN) {
					StringBuffer sb = null;
					try {
						RUN.wait();
						if (null == text) {
							continue; // goes to 'finally' clause
						}

						synchronized (this) {
							lock();
							sb = parse(text);
							if (null == sb) result = null;
							else {
								// remove last newline char, since it will be added again
								if (sb.length() > 0) sb.setLength(sb.length()-1);
								result = sb.toString();
							}
							text = null;
						}
					} catch (Throwable t) {
						error = t;
					} finally {
						// This clause gets excuted:
						//  - after a Throwable error
						//  - after calling continue and break ... inside the try { } catch, if they affect stuff outside the block
						//  - after a return call within the try { catch } block !!
						working = false;
						synchronized (this) { unlock(); }
						RUN.notifyAll();
					}
				}
			}
			cleanup();
			loaded = false;
		}
		private void cleanup() {
			Var.popThreadBindings();
		}
		private void init() throws Throwable {
			// Copying nearly literally from the clojure.lang.Repl class by Rich Hickey
			// RT.init();

			//*ns* must be thread-bound for in-ns to work
			//thread-bind *warn-on-reflection* so it can be set!
			//must have corresponding popThreadBindings in finally clause
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
				       stare, null));

			//create and move into the user namespace
			in_ns.invoke(USER);
			refer.invoke(CLOJURE);
		}

		/** Evaluates the clojure code in @param text and appends a newline char to each returned token. */
		static private StringBuffer parse(final String text) throws Throwable {
			// prepare input for parser
			final LineNumberingPushbackReader lnpr = new LineNumberingPushbackReader(new StringReader(text));
			// storage for readout
			final StringWriter sw = new StringWriter();

			while (true) {
				// read one token from the pipe
				Object r = LispReader.read(lnpr, false, EOF, false);
				if (EOF == r) {
					break;
				}
				// evaluate the tokens returned by the LispReader
				Object ret = Compiler.eval(r);
				// print the result in a lispy way
				if (null != ret) {
					RT.print(ret, sw);
					sw.write('\n');
				}
			}
			return sw.getBuffer();
		}
	}

	/** Will destroy the thread and cleanup if the interpreter is not loaded. */
	static protected void destroy() {
		// synchronized with the run(String arg) method
		synchronized (EOF) {
			LispThread thread = LispThread.getInstance();
			if (!loaded && null != thread) thread.quit();
		}
	}

	protected String getLineCommentMark() {
		return ";";
	}
}
