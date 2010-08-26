package common;

import fiji.InspectJar;

import ij.plugin.PlugIn;
import ij.IJ;
import ij.gui.GenericDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultFocusManager;
import javax.swing.FocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.Toolkit;
import java.awt.FileDialog;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.Writer;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.Set;

public abstract class AbstractInterpreter implements PlugIn {

	final protected JFrame window = new JFrame("Interpreter");
	final protected JTextArea screen = new JTextArea();
	final protected JTextArea prompt = new JTextArea(1, 60);//new JTextField(60);
	protected int active_line = 0;
	final protected ArrayList al_lines = new ArrayList();
	final protected ArrayList<Boolean> valid_lines = new ArrayList<Boolean>();
	private PipedOutputStream pout = null;
	private BufferedReader readin = null;
	protected BufferedOutputStream out = null;
	protected PrintWriter print_out = null;
	Thread reader, writer;
	protected JPopupMenu popup_menu;
	String last_dir = System.getProperty("user.dir");
	{
		try {
			last_dir = ij.Menus.getPlugInsPath();//ij.Prefs.getString(ij.Prefs.DIR_IMAGE);
		} catch (Exception e) {
			System.out.println("Could not retrieve Menus.getPlugInsPath()");
		}
	}
	protected ExecuteCode runner;

	static final protected Hashtable<Class,AbstractInterpreter> instances = new Hashtable<Class,AbstractInterpreter>();

	static {
		/* set the default class loader to ImageJ's PluginClassLoader */
		if (IJ.getInstance() != null)
			Thread.currentThread()
				.setContextClassLoader(IJ.getClassLoader());

		// Save history of all open interpreters even in the case of a call to System.exit(0),
		// which doesn't spawn windowClosing events.
		Runtime.getRuntime().addShutdownHook(new Thread() { public void run() {
			for (Map.Entry<Class,AbstractInterpreter> e : new HashSet<Map.Entry<Class,AbstractInterpreter>>(instances.entrySet())) {
				e.getValue().closingWindow();
			}
		}});
	}

	/** Convenient System.out.prinln(text); */
	protected void p(String msg) {
		System.out.println(msg);
	}

	protected void setTitle(String title) {
		window.setTitle(title);
	}

	public void run(String arghhh) {
		AbstractInterpreter instance = instances.get(getClass());
		if (null != instance) {
			instance.window.setVisible(true);
			instance.window.toFront();
			/*
			String name = instance.getClass().getName();
			int idot = name.lastIndexOf('.');
			if (-1 != idot) name = name.substring(idot);
			IJ.showMessage("The " + name.replace('_', ' ') + " is already open!");
			*/
			return;
		}
		instances.put(getClass(), this);

		System.out.println("Open interpreters:");
		for (Map.Entry<Class,AbstractInterpreter> e : instances.entrySet()) {
			System.out.println(e.getKey() + " -> " + e.getValue());
		}

		ArrayList[] hv = readHistory();
		al_lines.addAll(hv[0]);
		valid_lines.addAll(hv[1]);
		active_line = al_lines.size();
		if (al_lines.size() != valid_lines.size()) {
			IJ.log("ERROR in parsing history!");
			al_lines.clear();
			valid_lines.clear();
			active_line = 0;
		}

		runner = new ExecuteCode();

		// Wait until runner is alive (then piped streams will exist)
		while (!runner.isAlive() || null == pout) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException ie) {}
		}

		// start thread to write stdout and stderr to the screen
		reader = new Thread("out_reader") {
			public void run() {
				{
					try {
						readin = new BufferedReader(new InputStreamReader(new PipedInputStream(pout)));
					} catch (Exception ioe) {
						ioe.printStackTrace();
					}
				}
				setPriority(Thread.NORM_PRIORITY);
				while (!isInterrupted()) {
					try {
						// Will block until it can print a full line:
						String s = new StringBuilder(readin.readLine()).append('\n').toString();
						if (!window.isVisible()) continue;
						screen.append(s);
						screen.setCaretPosition(screen.getDocument().getLength());
					} catch (IOException ioe) {
						// Write end dead
						p("Out reader quit reading.");
						return;
					} catch (Throwable e) {
						if (!isInterrupted() && window.isVisible()) e.printStackTrace();
						else {
							p("Out reader terminated.");
							return;
						}
					}
				}
			}
		};
		reader.start();

		// make GUI
		makeGUI();
	}

	protected void makeGUI() {
		//JPanel panel = new JPanel();
		//panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		screen.setEditable(false);
		screen.setLineWrap(true);
		Font font = new Font("Courier", Font.PLAIN, 12);
		screen.setFont(font);
		popup_menu = new JPopupMenu();
		ActionListener menu_listener = new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					String selection = screen.getSelectedText();
					if (null == selection) return;
					String sel = filterSelection();
					String command = ae.getActionCommand();
					if (command.equals("Copy")) {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						Transferable transfer = new StringSelection(sel);
						cb.setContents(transfer, (ClipboardOwner)transfer);
					} else if (command.equals("Execute")) {
						runner.execute(sel);
					} else if (command.equals("Save")) {
						FileDialog fd = new FileDialog(window, "Save", FileDialog.SAVE);
						fd.setDirectory(last_dir);
						fd.setVisible(true);
						if (null != last_dir) last_dir = fd.getDirectory();
						String file_name = fd.getFile();
						if (null != file_name) {
							String path = last_dir + file_name;
							try {
								File file = new File(path);
								//this check is done anyway by the FileDialog, but just in case in some OSes it doesn't:
								while (file.exists()) {
									GenericDialog gd = new GenericDialog("File exists!");
									gd.addMessage("File exists! Choose another name or overwrite.");
									gd.addStringField("New file name: ", file_name);
									gd.addCheckbox("Overwrite", false);
									gd.showDialog();
									if (gd.wasCanceled()) return;
									file = new File(last_dir + gd.getNextString());
								}
								DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file), sel.length()));
								dos.writeBytes(sel);
								dos.flush();
							} catch (Exception e) {
								IJ.log("ERROR: " + e);
							}
						}
						
					}
				}
		};
		addMenuItem(popup_menu, "Copy", menu_listener);
		addMenuItem(popup_menu, "Execute", menu_listener);
		addMenuItem(popup_menu, "Save", menu_listener);
		JScrollPane scroll_prompt = new JScrollPane(prompt);
		scroll_prompt.setPreferredSize(new Dimension(440, 35));
		scroll_prompt.setVerticalScrollBarPolicy(JScrollPane
			.VERTICAL_SCROLLBAR_ALWAYS);
		prompt.setFont(font);
		prompt.setLineWrap(true);

		prompt.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "down");
		prompt.getActionMap().put("down",
				new AbstractAction("down") {
					public void actionPerformed(ActionEvent ae) {
						int position = cursorUpDown(true);
						if (position < 0) {
							trySetNextPrompt();
						} else {
							// Move down one line within a multiline prompt
							prompt.setCaretPosition(position);
						}
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke("UP"), "up");
		prompt.getActionMap().put("up",
				new AbstractAction("up") {
					public void actionPerformed(ActionEvent ae) {
						int position = cursorUpDown(false);
						if (position < 0) {
							trySetPreviousPrompt();
						} else {
							// Move down one line within a multiline prompt
							prompt.setCaretPosition(position);
						}
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK), "ctrl+p");
		prompt.getActionMap().put("ctrl+p",
				new AbstractAction("ctrl+p") {
					public void actionPerformed(ActionEvent ae) {
						trySetPreviousPrompt();
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK), "ctrl+n");
		prompt.getActionMap().put("ctrl+n",
				new AbstractAction("ctrl+n") {
					public void actionPerformed(ActionEvent ae) {
						trySetNextPrompt();
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ActionEvent.SHIFT_MASK), "shift+down");
		prompt.getActionMap().put("shift+down",
				new AbstractAction("shift+down") {
					public void actionPerformed(ActionEvent ae) {
						//enable to scroll within lines when the prompt consists of multiple lines.
						doArrowDown();
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.SHIFT_MASK), "shift+up");
		prompt.getActionMap().put("shift+up",
				new AbstractAction("shift+up") {
					public void actionPerformed(ActionEvent ae) {
						//enable to scroll within lines when the prompt consists of multiple lines.
						doArrowUp();
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
		prompt.getActionMap().put("enter",
				new AbstractAction("enter") {
					public void actionPerformed(ActionEvent ae) {
						runner.executePrompt();
					}
				});
		prompt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.SHIFT_MASK), "shift+enter");
		prompt.getActionMap().put("shift+enter",
				new AbstractAction("shift+enter") {
					public void actionPerformed(ActionEvent ae) {
						// allow multiline input on shift+enter
						int cp = prompt.getCaretPosition();
						prompt.insert("\n", cp);
					}
				});
		DefaultFocusManager manager = new DefaultFocusManager() {
			public void processKeyEvent(Component focusedComponent, KeyEvent ke) {
				if (focusedComponent == window &&
						ke.getKeyCode() ==
							KeyEvent.VK_TAB) {
					//cancelling TAB actions on focus issues
					return;
				}
				//for others call super
				super.processKeyEvent(focusedComponent, ke);
			}
		};
		FocusManager.setCurrentManager(manager);
		prompt.getInputMap().put(KeyStroke.getKeyStroke("TAB"), "tab");
		prompt.getActionMap().put("tab",
				new AbstractAction("tab") {
					public void actionPerformed(ActionEvent ae) {
						doTab(ae);
					}
				});
		screen.addMouseListener(
				new MouseAdapter() {
					public void mouseReleased(MouseEvent me) {
						String selection = screen.getSelectedText();
						//show popup menu
						if (null != selection && 0 < selection.length()) {
							popup_menu.show(screen, me.getX(), me.getY());
						}
						//set focus to prompt
						prompt.requestFocus();
					}
				});
		//make scroll for the screen
		JScrollPane scroll = new JScrollPane(screen);
		scroll.setPreferredSize(new Dimension(440,400));
		//set layout

		JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, scroll_prompt);
		panel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
		//add the panel to the window
		window.getContentPane().add(panel);
		//setup window display
		window.setSize(450, 450);
		window.pack();
		//set location to bottom right corner
		Rectangle screenBounds = window.getGraphicsConfiguration().getBounds();
		int x = screenBounds.width - window.getWidth() - 35;
		int y = screenBounds.height - window.getHeight() - 35;
		window.setLocation(x, y);
		//add windowlistener
		window.addWindowListener(
				new WindowAdapter() {
					public void windowClosing(WindowEvent we) {
						closingWindow();
					}
					public void windowClosed(WindowEvent we) {
						closingWindow();
					}
				}
		);
		//show the window
		window.setVisible(true);
		//set the focus to the input prompt
		prompt.requestFocus();
	}

	/** Store current prompt content if not empty and is different than current active line;
	 *  will also set active line to last. */
	private void tryStoreCurrentPrompt() {
		final String txt = prompt.getText();
		if (null != txt && txt.trim().length() > 0) {
			if (active_line < al_lines.size() && txt.equals((String)al_lines.get(active_line))) return;
			al_lines.add(txt);
			valid_lines.add(false); // because it has never been executed yet
			// set active line to last, since we've added a new entry
			active_line = al_lines.size() -1;
		}
	}

	private void trySetPreviousPrompt() {
		// Try to set the previous prompt text
		final int size = al_lines.size();
		if (0 == size) return;

		tryStoreCurrentPrompt();

		if (active_line > 0) {
			if (prompt.getText().equals("") && size -1 == active_line) {
				active_line = size - 1;
			} else {
				active_line--;
			}
		}
		prompt.setText((String)al_lines.get(active_line));
	}

	private void trySetNextPrompt() {
		// Try to set the next prompt text
		int size = al_lines.size();
		if (0 == size) return;

		tryStoreCurrentPrompt();

		if (active_line < size -1) {
			active_line++;
		} else if (active_line == size -1) {
			prompt.setText(""); //clear
			return;
		}
		final String text = (String)al_lines.get(active_line);
		prompt.setText(text);
	}

	/** get the position when moving one visible line forward or backward */
	private int cursorUpDown(boolean forward) {
		try {
			int position = prompt.getCaretPosition();
			int columns = prompt.getColumns();
			int line = prompt.getLineOfOffset(position);
			int start = prompt.getLineStartOffset(line);
			int end = prompt.getLineEndOffset(line);
			int column = (position - start) % columns;

			int wrappedLineCount =
				(end + columns - 1 - start) / columns;
			int currentWrappedLine =
				(position - start) / columns;

			if (forward) {
				if ((position - start) / columns <
						(end - start) / columns)
					return Math.min(position + columns,
							end);

				start = prompt.getLineStartOffset(line + 1);
				end = prompt.getLineEndOffset(line + 1);
				return Math.min(start + column, end - 1);
			}

			// backward
			if ((position - start) / columns > 0)
				return position - columns;

			start = prompt.getLineStartOffset(line - 1);
			end = prompt.getLineEndOffset(line - 1);
			int endColumn = (end - start) % columns;
			return end - Math.max(1, endColumn - column);
		} catch (Exception e) {
			return -1;
		}
	}

	/** Move the prompt caret down one line in a multiline prompt, if possible. */
	private void doArrowDown() {
		int position = cursorUpDown(true);
		if (position >= 0)
			prompt.setCaretPosition(position);
	}

	/** Move the prompt caret up one line in a multiline prompt, if possible. */
	private void doArrowUp() {
		int position = cursorUpDown(false);
		if (position >= 0)
			prompt.setCaretPosition(position);
	}

	private void closingWindow() {
		// Check if not closed already
		if (!instances.containsKey(getClass())) {
			return;
		}
		// Before any chance to fail, remove from hashtable of instances:
		instances.remove(getClass());
		// ... and store history
		saveHistory();
		//
		AbstractInterpreter.this.windowClosing();
		runner.quit();
		Thread.yield();
		reader.interrupt();
	}

	void addMenuItem(JPopupMenu menu, String label, ActionListener listener) {
		JMenuItem item = new JMenuItem(label);
		item.addActionListener(listener);
		menu.add(item);
	}

	private class ExecuteCode extends Thread {
		private Object lock = new Object();
		private boolean go = true;
		private String text = null;
		private boolean store = false; // only true when invoked from a prompt
		ExecuteCode() {
			setPriority(Thread.NORM_PRIORITY);
			try { setDaemon(true); } catch (Exception e) { e.printStackTrace(); }
			start();
		}
		public void quit() {
			go = false;
			interrupt();
			synchronized (this) { notify(); }
		}
		public void execute(String text) {
			this.text = text;
			this.store = false;
			synchronized (this) { notify(); }
		}
		public void executePrompt() {
			prompt.setEnabled(false);
			this.text = prompt.getText();
			this.store = true;
			synchronized (this) { notify(); }
		}
		public void run() {
			try {
				pout = new PipedOutputStream();
				out = new BufferedOutputStream(pout);
				print_out = new PrintWriter(out);
			} catch (Exception e) {
				e.printStackTrace();
			}
			AbstractInterpreter.this.threadStarting();
			while (go) {
				if (isInterrupted()) return;
				try {
					synchronized (this) { wait(); }
					if (!go) return;
					AbstractInterpreter.this.execute(text, store);
				} catch (InterruptedException ie) {
					return; 
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (!go) return; // this statement is reached when returning from the middle of the try/catch!
					window.setVisible(true);
					if (store) {
						prompt.setEnabled(true);
						prompt.requestFocus();
						// set caret position at the end of the prompt tabs
						String mb = prompt.getText();
						prompt.setCaretPosition(null == mb ? 0 : mb.length());
					}
					text = null;
					store = false;
				 }
			}
			AbstractInterpreter.this.threadQuitting();
			try {
				print_out.flush();
				print_out.close();
			} catch (Exception e) {}
		}
	}

	protected String getPrompt() {
		return ">>>";
	}

	boolean previous_line_empty = false;

	protected void execute(String text, boolean store) {
		if (null == text) return;
		int len = text.length();
		if (len <= 0) {
			println(getPrompt());
			return;
		}
		// store text
		if (len > 0 && store) {
			// only if different than last line
			if (al_lines.isEmpty() || !al_lines.get(al_lines.size()-1).equals(text)) {
				al_lines.add(text);
				valid_lines.add(false);
			}
			active_line = al_lines.size() -1;
		}
		// store in multiline if appropriate for later execution
		/*
		int i_colon = text.lastIndexOf(':');
		if ((len > 0 && i_colon == len -1) || 0 != multiline.length()) {
			multiline +=  text + "\n";
			// adjust indentation in prompt
			int n_tabs = 0;
			for (int i=0; i<len; i++) {
				if ('\t' != text.charAt(i)) {	
					break;
				}
				n_tabs++;
			}
			// indent when sentence ends with a ':'
			if (-1 != i_colon) {
				n_tabs++;
			}
			if (1 == n_tabs) {
				prompt.setText("\t");
			} else if (n_tabs > 1) {
				char[] tabs = new char[n_tabs];
				for (int i=0; i <n_tabs; i++) {
					tabs[i] = '\t';
				}
				prompt.setText(new String(tabs));
			}
			// print to screen
			println("... " + fix(text));
			// remove tabs from line:
			text = text.replaceAll("\\t", "");
			len = text.length(); // refresh length
			// test for text contents
			if (0 == len) {
				if (previous_line_empty) {
					text = multiline; //execute below
					multiline = "";
					previous_line_empty = false;
				} else {
					previous_line_empty = true;
				}
			} else {
				//don't eval/exec yet
				return;
			}
		} else {
		*/
			print(new StringBuilder(getPrompt()).append(' ').append(text).append('\n').toString());
		/*
		}
		*/
		try {
			Object ob = eval(text);
			if (null != ob) {
				println(ob.toString());
			}
			// if no error, mark as valid
			valid_lines.set(valid_lines.size() -1, true);
		} catch (Throwable e) {
			e.printStackTrace(print_out);
		} finally {
			print_out.write('\n');
			print_out.flush();
			//remove tabs from prompt
			prompt.setText("");
			// reset tab expansion
			last_tab_expand = null;
		}
	}

	/** Prints to screen: will append a newline char to the text, and also scroll down. */
	protected void println(String text) {
		print(text + "\n");
	}

	/** Prints to screen and scrolls down. */
	protected void print(String text) {
		screen.append(text);
		screen.setCaretPosition(screen.getDocument().getLength());
	}

	abstract protected Object eval(String text) throws Throwable;

	/** Expects a '#' for python and ruby, a ';' for lisp, a '//' for javascript, etc. */
	abstract protected String getLineCommentMark();

	/** Executed when the interpreter window is being closed. */
 	protected void windowClosing() {}

	/** Executed inside the executer thread before anything else. */
	protected void threadStarting() {}

	/** Executed inside the executer thread right before the thread will die. */
	protected void threadQuitting() {}

	/** Insert a tab in the prompt (in replacement for Component focus)*/
	synchronized protected void doTab(ActionEvent ae) {
		String prompt_text = prompt.getText();
		int cp = prompt.getCaretPosition();
		if (cp > 0) {
			char cc = prompt_text.charAt(cp-1);
			if ('t' == cc || '\n' == cc) {
				prompt.setText(prompt_text.substring(0, cp) + "\t" + prompt_text.substring(cp));
				return;
			}
		}
		int len = prompt_text.length();
		boolean add_tab = true;
		for (int i=0; i<len; i++) {
			char c = prompt_text.charAt(i);
			if ('\t' != c) {
				add_tab = false;
				break;
			}
		}
		if (add_tab) {
			prompt.append("\t");
		} else {
			// attempt to expand the variable name, if possible
			expandName(prompt_text, ae);
		}
	}

	/** Optional word expansion. */
	protected ArrayList expandStub(String stub) {
		return new ArrayList(); // empty
	}

	private String extractWordStub(final String prompt_text, final int caret_position) {
		final char[] c = new char[]{' ', '.', '(', ',', '['};
		final int[] cut = new int[c.length];
		for (int i=0; i<cut.length; i++) {
			cut[i] = prompt_text.lastIndexOf(c[i], caret_position);
		}
		Arrays.sort(cut);
		int ifirst = cut[cut.length-1] + 1;
		if (-1 == ifirst) return null;
		//p(ifirst + "," + caret_position + ", " + prompt_text.length());
		return prompt_text.substring(ifirst, caret_position);
	}

	private void expandName(String prompt_text, ActionEvent ae) {
		if (null != last_tab_expand) {
			last_tab_expand.cycle(ae);
			return;
		}
		if (null == prompt_text) prompt_text = prompt.getText();
		int ilast = prompt.getCaretPosition() -1;
		// check preconditions
		if (ilast <= 0) return;
		char last = prompt_text.charAt(ilast);
		if (' ' == last || '\t' == last) {
			p("last char is space or tab");
			return;
		}
		// parse last word stub
		String stub = extractWordStub(prompt_text, ilast+1);
		ArrayList al = expandStub(stub);
		if (al.size() > 0) {
			last_tab_expand = new TabExpand(al, ilast - stub.length() + 1, stub);
		} else {
			last_tab_expand = null;
		}
	}

	private TabExpand last_tab_expand = null;

	private class TabExpand {
		ArrayList al = new ArrayList();
		int i = 0;
		int istart; // stub starting index
		int len_prev; // length of previously set word
		String stub;
		TabExpand(ArrayList al, int istart, String stub) {
			this.al.addAll(al);
			this.istart = istart;
			this.stub = stub;
			this.len_prev = stub.length();
			cycle(null);
		}
		void cycle(ActionEvent ae) {
			if (null == ae) {
				// first time
				set();
				return;
			}

			/*
			p("##\nlen_prev: " + len_prev);
			p("i : " + i);
			p("prompt.getText(): " + prompt.getText());
			p("prompt.getText().length(): " + prompt.getText().length());
			p("istart: " + istart + "\n##");
			*/

			int plen = prompt.getText().length();
			String stub = extractWordStub(prompt.getText(), this.istart + len_prev > plen ? plen : this.istart + len_prev); // may be null
			if (this.stub.equals(stub) || al.get(i).equals(stub)) {
				// ok
			} else {
				// can't expand, remake
				last_tab_expand = null;
				expandName(prompt.getText(), ae);
				return;
			}

			// check preconditions
			if (0 == al.size()) {
				p("No elems to expand to");
				return;
			}

			// ok set prompt to next
			i += ( 0 != (ae.getModifiers() & ActionEvent.SHIFT_MASK) ? -1 : 1);
			if (al.size() == i) i = 0;
			if (-1 == i) i = al.size() -1;
			set();
		}
		private void set() {
			String pt = prompt.getText();
			if (i > 0) p("set to " + al.get(i));
			prompt.setText(pt.substring(0, istart) + al.get(i).toString() + pt.substring(istart + len_prev));
			len_prev = ((String)al.get(i)).length();
		}
	}

	private String filterSelection() {
		String sel = screen.getSelectedText().trim();

		StringBuffer sb = new StringBuffer();
		int istart = 0;
		int inl = sel.indexOf('\n');
		int len = sel.length();
		String sprompt = getPrompt();
		Pattern pat = Pattern.compile("^" + sprompt + " .*$");

		while (true) {
			if (-1 == inl) inl = len -1;
			// process line:
			String line = sel.substring(istart, inl+1);
			if (pat.matcher(line).matches()) {
				line = line.substring(sprompt.length() + 1); // + 1 to reach the first char after the space after the prompt text.
			}
			sb.append(line);
			// quit if possible
			if (len -1 == inl) break;
			// prepate next
			istart = inl+1;
			inl = sel.indexOf('\n', istart);
		};

		if (0 == sb.length()) return sel;
		return sb.toString();
	}

	private void saveHistory() {
		String path = ij.Prefs.getPrefsDir() + "/" + getClass().getName() + ".log";
		File f = new File(path);
		if (!f.getParentFile().canWrite()) {
			IJ.log("Could not save history for " + getClass().getName() + "\nat path: " + path);
			return;
		}
		Writer writer = null;
		try {
			writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(f)), "8859_1");

			final int MAX_LINES = 2000;

			// Write all lines up to MAX_LINES
			int first = al_lines.size() - MAX_LINES;
			if (first < 0) first = 0;
			String rep = new StringBuffer().append('\n').append(getLineCommentMark()).toString();
			String separator = getLineCommentMark() + "\n";
			for (int i=first; i<al_lines.size(); i++) {
				// Separate executed code blocks with a empty comment line:
				writer.write(separator);
				String block = (String)al_lines.get(i);
				// If block threw an Exception when executed, save it as commented out:
				if (!valid_lines.get(i)) {
					block = getLineCommentMark() + block;
					block = block.replaceAll("\n", rep);
				}
				if (!block.endsWith("\n")) block += "\n";
				writer.write(block);
			}
			writer.flush();
		} catch (Throwable e) {
			IJ.log("Could NOT save history log file!");
			IJ.log(e.toString());
		} finally {
			try {
				writer.close();
			} catch (java.io.IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	private ArrayList[] readHistory() {
		String path = ij.Prefs.getPrefsDir() + "/" + getClass().getName() + ".log";
		File f = new File(path);
		ArrayList blocks = new ArrayList();
		ArrayList valid = new ArrayList();
		if (!f.exists()) {
			System.out.println("No history exists yet for " + getClass().getName());
			return new ArrayList[]{blocks, valid};
		}
		final String sep = getLineCommentMark() + "\n";
		Scanner scanner = null;
		try {
			scanner = new Scanner(new File(path), "8859_1").useDelimiter(sep);
			while (scanner.hasNext()) {
				String block = scanner.next();
				int inl = block.lastIndexOf('\n');
				int end = block.length() == inl + 1 ? inl : block.length();
				if (0 == block.indexOf(sep)) block = block.substring(sep.length(), end);
				else block = block.substring(0, end);
				blocks.add(block);
				valid.add(true); // all valid, even if they were not: the invalid ones are commented out
			}
		} catch (Throwable e) {
			IJ.log("Could NOT read history log file!");
			IJ.log(e.toString());
		} finally {
			scanner.close();
		}
		return new ArrayList[]{blocks, valid};
	}

	protected static boolean hasPrefix(String subject, Set<String> prefixes) {
		for (String prefix : prefixes)
			if (subject.startsWith(prefix))
				return true;
		return false;
	}

	public static Map<String, List<String>> getDefaultImports() {
		final String[] classNames = {
			"ij.IJ", "java.lang.String", "ini.trakem2.Project"
		};
		InspectJar inspector = new InspectJar();
		for (String className : classNames) try {
			String baseName = className.substring(className.lastIndexOf('.') + 1);
			URL url = Class.forName(className).getResource(baseName + ".class");
			inspector.addJar(url);
		} catch (Exception e) {
			if (IJ.debugMode)
				IJ.log("Warning: class " + className
						+ " was not found!");
		}
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		Set<String> prefixes = new HashSet<String>();
		for (String className : classNames)
			prefixes.add(className.substring(0, className.lastIndexOf('.')));
		for (String className : inspector.classNames(true)) {
			if (!hasPrefix(className, prefixes))
				continue;
			int dot = className.lastIndexOf('.');
			String packageName = dot < 0 ? "" : className.substring(0, dot);
			String baseName = className.substring(dot + 1);
			List<String> list = result.get(packageName);
			if (list == null) {
				list = new ArrayList<String>();
				result.put(packageName, list);
			}
			list.add(baseName);
		}
		return result;
	}
}
