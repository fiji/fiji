package fiji.scripting;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JMenuBar;
import javax.swing.BorderFactory;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.InputMethodListener;
import java.awt.event.WindowListener;
import javax.swing.event.ChangeListener;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.CaretEvent;
import javax.swing.text.Document;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.LineNumberReader;
import java.awt.image.BufferedImage;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import javax.imageio.ImageIO;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.ToolTipSupplier;
import org.fife.ui.rtextarea.IconGroup;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import common.RefreshScripts;
import com.sun.jdi.connect.VMStartException;

import fiji.scripting.completion.ClassCompletionProvider;
import fiji.scripting.completion.DefaultProvider;


class TextEditor extends JFrame implements ActionListener, ItemListener,
		ChangeListener, MouseMotionListener, MouseListener,
		CaretListener, InputMethodListener, DocumentListener,
		WindowListener {
	boolean fileChanged = false;
	File file;
	RSyntaxTextArea textArea;
	JTextArea screen = new JTextArea();
	JMenuItem new_file, open, save, saveas, compileAndRun, debug, quit,
		  undo, redo, cut, copy, paste, find, replace, selectAll,
		  autocomplete, resume, terminate, kill;
	FindAndReplaceDialog replaceDialog;
	AutoCompletion autocomp;
	Languages.Language currentLanguage;
	ClassCompletionProvider provider;
	StartDebugging debugging;
	Gutter gutter;
	IconGroup iconGroup;

	public TextEditor(String path1) {
		JPanel cp = new JPanel(new BorderLayout());
		textArea = new RSyntaxTextArea();
		textArea.addCaretListener(this);
		provider = new ClassCompletionProvider(new DefaultProvider(),
				textArea, null);
		autocomp = new AutoCompletion(provider);

		autocomp.setListCellRenderer(new CCellRenderer());
		autocomp.setShowDescWindow(true);
		autocomp.setParameterAssistanceEnabled(true);
		autocomp.install(textArea);
		textArea.setToolTipSupplier((ToolTipSupplier)provider);
		ToolTipManager.sharedInstance().registerComponent(textArea);
		textArea.getDocument().addDocumentListener(this);
		RTextScrollPane sp = new RTextScrollPane(textArea);
		sp.setPreferredSize(new Dimension(600, 350));
		sp.setIconRowHeaderEnabled(true);
		gutter = sp.getGutter();
		iconGroup = new IconGroup("bullets", "images/", null, "png", null);
		gutter.setBookmarkIcon(iconGroup.getIcon("var"));
		gutter.setBookmarkingEnabled(true);
		screen.setEditable(false);
		screen.setLineWrap(true);
		Font font = new Font("Courier", Font.PLAIN, 12);
		screen.setFont(font);
		JScrollPane scroll = new JScrollPane(screen);
		scroll.setPreferredSize(new Dimension(600, 80));
		JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sp, scroll);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		panel.setResizeWeight(350.0 / 430.0);
		setContentPane(panel);
		addWindowListener(this);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		JMenuBar mbar = new JMenuBar();
		setJMenuBar(mbar);

		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);
		new_file = addToMenu(file, "New", 0, KeyEvent.VK_N, ActionEvent.CTRL_MASK);
		open = addToMenu(file, "Open...", 0, KeyEvent.VK_O, ActionEvent.CTRL_MASK);
		save = addToMenu(file, "Save", 0, KeyEvent.VK_S, ActionEvent.CTRL_MASK);
		saveas = addToMenu(file, "Save as...", 1, 0, 0);
		file.addSeparator();
		quit = addToMenu(file, "Quit", 0, KeyEvent.VK_X, ActionEvent.ALT_MASK);

		mbar.add(file);

		JMenu edit = new JMenu("Edit");
		edit.setMnemonic(KeyEvent.VK_E);
		undo = addToMenu(edit, "Undo", 0, KeyEvent.VK_Z, ActionEvent.CTRL_MASK);
		redo = addToMenu(edit, "Redo", 0, KeyEvent.VK_Y, ActionEvent.CTRL_MASK);
		edit.addSeparator();
		cut = addToMenu(edit, "Cut", 0, KeyEvent.VK_X, ActionEvent.CTRL_MASK);
		copy = addToMenu(edit, "Copy", 0, KeyEvent.VK_C, ActionEvent.CTRL_MASK);
		paste = addToMenu(edit, "Paste", 0, KeyEvent.VK_V, ActionEvent.CTRL_MASK);
		edit.addSeparator();
		find = addToMenu(edit, "Find...", 0, KeyEvent.VK_F, ActionEvent.CTRL_MASK);
		replace = addToMenu(edit, "Find and Replace...", 0, KeyEvent.VK_H, ActionEvent.CTRL_MASK);
		edit.addSeparator();
		// TODO: this belongs higher, no?
		selectAll = addToMenu(edit, "Select All", 0, KeyEvent.VK_A, ActionEvent.CTRL_MASK);
		mbar.add(edit);

		JMenu options = new JMenu("Options");
		options.setMnemonic(KeyEvent.VK_O);
		// TODO: CTRL, ALT
		autocomplete = addToMenu(options, "Autocomplete", 0, KeyEvent.VK_SPACE, ActionEvent.CTRL_MASK);
		options.addSeparator();

		mbar.add(options);

		JMenu languages = new JMenu("Language");
		languages.setMnemonic(KeyEvent.VK_L);
		ButtonGroup group = new ButtonGroup();
		for (final Languages.Language language :
		                Languages.getInstance().languages) {
			JRadioButtonMenuItem item =
			        new JRadioButtonMenuItem(language.menuLabel);
			if (language.shortCut != 0)
				item.setMnemonic(language.shortCut);
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setLanguage(language);
				}
			});

			group.add(item);
			languages.add(item);
			language.item = item;
		}
		mbar.add(languages);

		JMenu run = new JMenu("Run");
		run.setMnemonic(KeyEvent.VK_R);
		// TODO: allow outside-of-plugins/ sources

		compileAndRun = addToMenu(run, "Compile and Run", 0, KeyEvent.VK_F11, 0);

		run.addSeparator();
		debug = addToMenu(run, "Start Debugging", 0, KeyEvent.VK_F11, ActionEvent.CTRL_MASK);
		mbar.add(run);

		run.addSeparator();

		kill = addToMenu(run, "Kill running script...", 1, 0, 0);
		kill.setEnabled(false);

		JMenu breakpoints = new JMenu("Breakpoints");
		breakpoints.setMnemonic(KeyEvent.VK_B);
		resume = addToMenu(breakpoints, "Resume", 1, 0, 0);
		terminate = addToMenu(breakpoints, "Terminate", 1, 0, 0);
		mbar.add(breakpoints);

		pack();
		getToolkit().setDynamicLayout(true);            //added to accomodate the autocomplete part

		setLanguage(null);
		setTitle();

		setLocationRelativeTo(null); // center on screen
		setVisible(true);
		if (path1 != null && !path1.equals(""))
			open(path1);
	}

	public JMenuItem addToMenu(JMenu menu, String menuEntry, int keyEvent, int keyevent, int actionevent) {
		JMenuItem item = new JMenuItem(menuEntry);
		menu.add(item);
		if (keyEvent == 0) // == 0?  Not != 0?
			item.setAccelerator(KeyStroke.getKeyStroke(keyevent, actionevent));
		item.addActionListener(this);
		return item;
	}

	public void createNewDocument() {
		textArea.setText("");
		file = null;
		fileChanged = false;
		setTitle();
	}

	public boolean handleUnsavedChanges() {
		if (!fileChanged)
			return true;

		switch (JOptionPane.showConfirmDialog(this,
				"Do you want to save changes?")) {
		case JOptionPane.NO_OPTION:
			return true;
		case JOptionPane.YES_OPTION:
			if (save())
				return true;
		}

		return false;
	}

	public void actionPerformed(ActionEvent ae) {
		final Object source = ae.getSource();
		if (source == new_file) {
			if (!handleUnsavedChanges())
				return;
			createNewDocument();
		}
		else if (source == open) {
			if (!handleUnsavedChanges())
				return;

			OpenDialog dialog = new OpenDialog("Open..", "");
			String name = dialog.getFileName();
			if (name != null)
				open(dialog.getDirectory() + name);
			return;
		}
		else if (source == save)
			save();
		else if (source == saveas)
			saveAs();
		else if (source == compileAndRun)
			runText();
		else if (source == debug) {
			BreakpointManager manager = new BreakpointManager(gutter, textArea, iconGroup);
			debugging = new StartDebugging(file.getPath(), manager.findBreakpointsLineNumber());

			try {
				System.out.println(debugging.startDebugging().exitValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (source == kill)
			chooseTaskToKill();
		else if (source == quit)
			processWindowEvent( new WindowEvent(this, WindowEvent.WINDOW_CLOSING) );
		else if (source == cut)
			textArea.cut();
		else if (source == copy)
			textArea.copy();
		else if (source == paste)
			textArea.paste();
		else if (source == undo)
			textArea.undoLastAction();
		else if (source == redo)
			textArea.redoLastAction();
		else if (source == find)
			setFindAndReplace(false);
		else if (source == replace) {
			try {
				setFindAndReplace(true);
			} catch (Exception e) {
				e.printStackTrace(); // TODO: huh?
			}
		}
		else if (source == selectAll) {
			textArea.setCaretPosition(0);
			textArea.moveCaretPosition(textArea.getDocument().getLength());
		}
		else if (source == autocomplete) {
			try {
				autocomp.doCompletion();
			} catch (Exception e) {}
		}
		else if (source == resume)
			debugging.resumeVM();
		else if (source == terminate) {
			// TODO not implemented
		}

	}

	protected RSyntaxDocument getDocument() {
		return (RSyntaxDocument)textArea.getDocument();
	}

	// TODO: nonono.
	public void setFindAndReplace(boolean ifReplace) {
		if (replaceDialog != null) {						//here should the code to close all other dialog boxes
			if (replaceDialog.isReplace() != ifReplace) {
				replaceDialog.dispose();
				replaceDialog = null;
			}
		}
		if (replaceDialog == null) {
			replaceDialog = new FindAndReplaceDialog(this, textArea, ifReplace);
			replaceDialog.setResizable(true);
			replaceDialog.pack();
			replaceDialog.setLocationRelativeTo(this);
		}
		replaceDialog.show();
		replaceDialog.toFront();
	}

	public void open(String path) {
		try {
			file = new File(path);
		} catch (Exception e) {
			System.out.println("problem in opening");
		}
		try {
			if (file != null) {
				FileInputStream fin = new FileInputStream(file);
				BufferedReader din = new BufferedReader(new InputStreamReader(fin));
				StringBuilder text = new StringBuilder();
				String line;
				while ((line = din.readLine()) != null)
					text.append(line).append("\n");
				textArea.setText(text.toString());
				fin.close();
				fileChanged = false;
				setFileName(file);
			} else {
				// TODO: unify error handling.  Don't mix JOptionPane with IJ.error as if we had no clue what we want
				JOptionPane.showMessageDialog(this, "The file name " + file.getName() + " not found.");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public boolean saveAs() {
		SaveDialog sd = new SaveDialog("Save as ", getFileName() , "");
		String name = sd.getFileName();
		if (name == null)
			return false;

		String path = sd.getDirectory() + name;
		return saveAs(path, checkForReplace(sd.getDirectory(), name));
	}

	// TODO: this is racy at best
	public boolean checkForReplace(String directory, String name) {
		return(new File(directory, name).exists());

	}

	public void saveAs(String path) {
		saveAs(path, true);
	}

	public boolean saveAs(String path, boolean askBeforeReplacing) {
		file = new File(path);
		if (file.exists() && !askBeforeReplacing &&
				JOptionPane.showConfirmDialog(this,
					"Do you want to replace " + path + "?",
					"Replace " + path + "?",
					JOptionPane.YES_NO_OPTION)
				!= JOptionPane.YES_OPTION)
			return false;
		if (!write(file))
			return false;
		setFileName(file);
		return true;
	}

	public boolean save() {
		if (file == null)
			return saveAs();
		if (!write(file))
			return false;
		setTitle();
		return true;
	}

	public boolean write(File file) {
		try {
			BufferedWriter outFile =
				new BufferedWriter(new FileWriter(file));
			outFile.write(textArea.getText());
			outFile.close();
			fileChanged = false;
			return true;
		} catch (IOException e) {
			IJ.error("Could not save " + file.getName());
			e.printStackTrace();
			return false;
		}
	}

	public static String getExtension(String fileName) {
		int dot = fileName.lastIndexOf(".");
		return dot < 0 ?  "" : fileName.substring(dot);
	}

	private void setLanguageByExtension(String extension) {
		setLanguage(Languages.get(extension));
	}

	protected void setLanguage(Languages.Language language) {
		if (language == null)
			language = Languages.get("");

		if (file != null) {
			String name = file.getName();
			if (!name.endsWith(language.extension) &&
					currentLanguage != null) {
				String ext = currentLanguage.extension;
				if (name.endsWith(ext))
					name = name.substring(0, name.length()
							- ext.length());
				file = new File(file.getParentFile(),
						name + language.extension);
				fileChanged = true;
			}
		}
		currentLanguage = language;
		setTitle();

		if (!language.item.isSelected())
			language.item.setSelected(true);

		compileAndRun.setLabel(language.isCompileable() ?
			"Compile and Run" : "Run");
		compileAndRun.setEnabled(language.isRunnable());
		debug.setEnabled(language.isDebuggable());

		provider.setProviderLanguage(language.menuLabel);

		// TODO: these should go to upstream RSyntaxTextArea
		if (language.syntaxStyle != null)
			textArea.setSyntaxEditingStyle(language.syntaxStyle);
		else if (language.extension.equals(".clj"))
			getDocument().setSyntaxStyle(new ClojureTokenMaker());
		else if (language.extension.equals(".m"))
			getDocument().setSyntaxStyle(new MatlabTokenMaker());
	}

	public void setFileName(File file) {
		setTitle();
		setLanguageByExtension(getExtension(file.getName()));
	}

	protected String getFileName() {
		return file == null ?
			"New_" + currentLanguage.extension : file.getName();
	}

	private void setTitle() {
		String title = (fileChanged ? "*" : "") + getFileName();
		setTitle(title);
	}

	/** Using a Vector to benefit from all its methods being synchronzed. */
	private Vector<Executer> executing_tasks = new Vector<Executer>();

	/** Generic Thread that keeps a starting time stamp,
	 *  sets the priority to normal and starts itself. */
	private abstract class Executer extends ThreadGroup {
		Executer() {
			super("Script Editor Run :: " + new Date().toString());
			// Store itself for later
			executing_tasks.add(this);
			// Enable kill menu
			kill.setEnabled(true);
			// Fork a task, as a part of this ThreadGroup
			new Thread(this, getName()) {
				{
					setPriority(Thread.NORM_PRIORITY);
					start();
				}
				public void run() {
					try {
						execute();
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						executing_tasks.remove(Executer.this);
						// Leave kill menu item enabled if other tasks are running
						kill.setEnabled(executing_tasks.size() > 0);
					}
				}
			};
		}
		
		/** The method to extend, that will do the actual work. */
		abstract void execute();

		/** Fetch a list of all threads from all thread subgroups, recursively. */
		List<Thread> getAllThreads() {
			ArrayList<Thread> threads = new ArrayList<Thread>();
			ThreadGroup[] tgs = new ThreadGroup[activeGroupCount() * 2 + 100];
			this.enumerate(tgs);
			for (ThreadGroup tg : tgs) {
				if (null == tg) continue;
				Thread[] ts = new Thread[tg.activeCount() * 2 + 100];
				tg.enumerate(ts);
				for (Thread t : ts) {
					if (null == t) continue;
					threads.add(t);
				}
			}
			return threads;
		}

		/** Totally destroy/stop all threads in this and all recursive thread subgroups. Will remove itself from the executing_tasks list. */
		void obliterate() {
			for (Thread thread : getAllThreads()) {
				try {
					thread.interrupt();
					Thread.yield(); // give it a chance
					thread.stop();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
			executing_tasks.remove(this);
		}
	}

	/** Query the list of running scripts and provide a dialog to choose one and kill it. */
	public void chooseTaskToKill() {
		
		if (0 == executing_tasks.size()) {
			IJ.log("\nNo tasks running!\n");
			return;
		}

		final Executer[] executers = (Executer[]) executing_tasks.toArray(new Executer[0]);
		if (0 == executers.length) {
			IJ.log("\nNo tasks to kill\n");
			return;
		}
		String[] names = new String[executers.length];
		for (int i=0; i<executers.length; i++) {
			names[i] = executers[i].getName();
		}

		GenericDialog gd = new GenericDialog("Kill");
		gd.addChoice("Running scripts: ", names, names[names.length - 1]);
		gd.addCheckbox("Kill all", false);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		ArrayList<Executer> deaders = new ArrayList<Executer>();
		if (gd.getNextBoolean()) {
			// kill all
			for (Executer ex : executers)
				deaders.add(ex);
		} else {
			deaders.add(executers[gd.getNextChoiceIndex()]);
		}

		for (final Executer ex : deaders) {
			// Graceful attempt:
			ex.interrupt();
			// Give it 3 seconds. Then, stop it.
			final long onset = System.currentTimeMillis();
			new Thread() {
				{ setPriority(Thread.NORM_PRIORITY); }
				public void run() {
					while (true) {
						if (System.currentTimeMillis() - onset > 3000) break;
						try {
							Thread.sleep(100);
						} catch (InterruptedException ie) {}
					}
					ex.obliterate();
				}
			}.start();
		}
	}

	/** Run the text in the textArea without compiling it, only if it's not java. */
	public void runText() {

		if (currentLanguage.isCompileable()) {
			if (handleUnsavedChanges())
				runScript();
			return;
		}
		if (!currentLanguage.isRunnable()) {
			JOptionPane.showMessageDialog(this,
					"Select a language first!");
			// TODO guess the language, if possible.
			return;
		}

		textArea.setEditable(false);
		try {
			final RefreshScripts interpreter =
				currentLanguage.interpreter;

			// Pipe current text into the runScript:
			final PipedInputStream pi = new PipedInputStream();
			final PipedOutputStream po = new PipedOutputStream(pi);

			// Start reading, should block until writing starts
			new TextEditor.Executer() {
				public void execute() {

					// Output to the screen:
					PipedInputStream in =
						new PipedInputStream();
					PipedOutputStream out = null;
					try {
						out = new PipedOutputStream(in);
						interpreter.setOutputStreams( out, out );
					} catch (Exception e) {
						e.printStackTrace();
						IJ.log("Could not connect stdout!");
						return;
					}

					final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

					Thread out_thread = new Thread() {
						{
							setPriority(Thread.NORM_PRIORITY);
							try { setDaemon(true); } catch (Exception e) { e.printStackTrace(); }
							start();
						}
						public void run() {
							while (!isInterrupted()) {
								try {
									// Will block until it can print a full line:
									screen.append(new StringBuilder(reader.readLine()).append('\n').toString());
									// Scroll to the end
									screen.setCaretPosition(screen.getDocument().getLength());
								} catch (Exception e) {
									break;
								}
							}
						}
					};

					interpreter.runScript(pi);

					try {
						out.flush(); // write output to JTextArea screen
						out.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			// Now write to it:
			textArea.write(new PrintWriter(po));
			// ... and trigger full reading from PipedInputStream by flushing and closing it:
			po.flush();
			po.close();

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			textArea.setEditable(true);
		}
	}

	public void runScript() {
		String ext = getExtension(file.getName());
		final RefreshScripts interpreter =
		        Languages.getInstance().get(ext).interpreter;

		if (interpreter == null) {
			IJ.error("There is no interpreter for " + ext
			         + " files!");
			return;
		}

		new TextEditor.Executer() {
			public void execute() {
				interpreter.runScript(file.getPath());
			}
		};
	}

	public void windowClosing(WindowEvent e) {
		if (!handleUnsavedChanges())
			return;
		dispose();
	}

	//next function is for the InputMethodEvent changes
	public void inputMethodTextChanged(InputMethodEvent event) {
		updateStatusOnChange();
	}
	public void caretPositionChanged(InputMethodEvent event) {
		updateStatusOnChange();
	}

	public void insertUpdate(DocumentEvent e) {
		updateStatusOnChange();
	}
	public void removeUpdate(DocumentEvent e) {
		updateStatusOnChange();
	}

	// TODO: rename into "markDirty"
	private void updateStatusOnChange() {
		fileChanged = true;
		setTitle();
	}

	// TODO: use an anonymous WindowAdapter, MouseAdapter, etc instead
	public void windowClosed(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	public void itemStateChanged(ItemEvent ie) {}
	public void stateChanged(ChangeEvent e) {}
	public void mouseMoved(MouseEvent me) {}
	public void mouseClicked(MouseEvent me) {}
	public void mouseEntered(MouseEvent me) {}
	public void mouseExited(MouseEvent me) {}
	public void mouseDragged(MouseEvent me) {}
	public void mouseReleased(MouseEvent me) {}
	public void mousePressed(MouseEvent me) {}
	public void caretUpdate(CaretEvent ce) {}
	public void changedUpdate(DocumentEvent e) {}
	public void windowActivated(WindowEvent e) {}
}
// TODO: check all files for whitespace issues
