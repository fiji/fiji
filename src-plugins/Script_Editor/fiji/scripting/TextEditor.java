package fiji.scripting;

import common.RefreshScripts;

import fiji.scripting.java.Refresh_Javas;

import ij.IJ;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.io.OpenDialog;
import ij.io.SaveDialog;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;

import java.net.URL;
import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import java.util.zip.ZipException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import org.fife.ui.rtextarea.RTextScrollPane;

public class TextEditor extends JFrame implements ActionListener,
	       ChangeListener {
	EditorPane editorPane;
	JTabbedPane tabbed;
	JTextArea screen;
	JMenuItem newFile, open, save, saveas, compileAndRun, debug, close,
		  undo, redo, cut, copy, paste, find, replace, selectAll,
		  autocomplete, resume, terminate, kill, gotoLine,
		  makeJar, makeJarWithSource, removeUnusedImports,
		  sortImports, removeTrailingWhitespace, findNext,
		  openHelp, addImport, clearScreen, nextError, previousError,
		  openHelpWithoutFrames, nextTab, previousTab,
		  runSelection;
	JMenu tabsMenu;
	int tabsMenuTabsStart;
	Set<JMenuItem> tabsMenuItems;
	FindAndReplaceDialog findDialog;

	String templateFolder = "templates/";
	Set<String> templatePaths;
	Languages.Language[] availableLanguages = Languages.getInstance().languages;

	Position compileStartPosition;
	ErrorHandler errorHandler;

	public TextEditor(String path) {
		super("Script Editor");
		WindowManager.addWindow(this);

		// Initialize menu
		int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		int shift = ActionEvent.SHIFT_MASK;
		JMenuBar mbar = new JMenuBar();
		setJMenuBar(mbar);

		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);
		newFile = addToMenu(file, "New",  KeyEvent.VK_N, ctrl);
		newFile.setMnemonic(KeyEvent.VK_N);
		open = addToMenu(file, "Open...",  KeyEvent.VK_O, ctrl);
		open.setMnemonic(KeyEvent.VK_O);
		save = addToMenu(file, "Save", KeyEvent.VK_S, ctrl);
		save.setMnemonic(KeyEvent.VK_S);
		saveas = addToMenu(file, "Save as...", 0, 0);
		saveas.setMnemonic(KeyEvent.VK_A);
		file.addSeparator();
		makeJar = addToMenu(file, "Export as .jar", 0, 0);
		makeJar.setMnemonic(KeyEvent.VK_E);
		makeJarWithSource = addToMenu(file, "Export as .jar (with source)", 0, 0);
		makeJarWithSource.setMnemonic(KeyEvent.VK_X);
		file.addSeparator();
		close = addToMenu(file, "Close", KeyEvent.VK_W, ctrl);

		mbar.add(file);

		JMenu edit = new JMenu("Edit");
		edit.setMnemonic(KeyEvent.VK_E);
		undo = addToMenu(edit, "Undo", KeyEvent.VK_Z, ctrl);
		redo = addToMenu(edit, "Redo", KeyEvent.VK_Y, ctrl);
		edit.addSeparator();
		selectAll = addToMenu(edit, "Select All", KeyEvent.VK_A, ctrl);
		cut = addToMenu(edit, "Cut", KeyEvent.VK_X, ctrl);
		copy = addToMenu(edit, "Copy", KeyEvent.VK_C, ctrl);
		paste = addToMenu(edit, "Paste", KeyEvent.VK_V, ctrl);
		edit.addSeparator();
		find = addToMenu(edit, "Find...", KeyEvent.VK_F, ctrl);
		find.setMnemonic(KeyEvent.VK_F);
		findNext = addToMenu(edit, "Find Next", KeyEvent.VK_F3, 0);
		findNext.setMnemonic(KeyEvent.VK_N);
		replace = addToMenu(edit, "Find and Replace...", KeyEvent.VK_H, ctrl);
		gotoLine = addToMenu(edit, "Goto line...", KeyEvent.VK_G, ctrl);
		gotoLine.setMnemonic(KeyEvent.VK_G);
		edit.addSeparator();
		clearScreen = addToMenu(edit, "Clear output panel", 0, 0);
		clearScreen.setMnemonic(KeyEvent.VK_L);
		edit.addSeparator();
		autocomplete = addToMenu(edit, "Autocomplete", KeyEvent.VK_SPACE, ctrl);
		autocomplete.setMnemonic(KeyEvent.VK_A);
		edit.addSeparator();
		addImport = addToMenu(edit, "Add import...", 0, 0);
		addImport.setMnemonic(KeyEvent.VK_I);
		removeUnusedImports = addToMenu(edit, "Remove unused imports", 0, 0);
		removeUnusedImports.setMnemonic(KeyEvent.VK_U);
		sortImports = addToMenu(edit, "Sort imports", 0, 0);
		sortImports.setMnemonic(KeyEvent.VK_S);
		removeTrailingWhitespace = addToMenu(edit, "Remove trailing whitespace", 0, 0);
		removeTrailingWhitespace.setMnemonic(KeyEvent.VK_W);
		mbar.add(edit);

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

		JMenu templates = new JMenu("Templates");
		templates.setMnemonic(KeyEvent.VK_T);
		setupTemplatePaths();
		populateTemplateMenu(templates);
		mbar.add(templates);

		JMenu run = new JMenu("Run");
		run.setMnemonic(KeyEvent.VK_R);

		compileAndRun = addToMenu(run, "Compile and Run",
				KeyEvent.VK_R, ctrl);
		compileAndRun.setMnemonic(KeyEvent.VK_R);

		runSelection = addToMenu(run, "Run selected code",
				KeyEvent.VK_R, ctrl | shift);
		runSelection.setMnemonic(KeyEvent.VK_S);

		run.addSeparator();
		nextError = addToMenu(run, "Next Error", KeyEvent.VK_F4, 0);
		nextError.setMnemonic(KeyEvent.VK_N);
		previousError = addToMenu(run, "Next Error", KeyEvent.VK_F4, shift);
		previousError.setMnemonic(KeyEvent.VK_P);
		run.addSeparator();
		debug = addToMenu(run, "Start Debugging", KeyEvent.VK_D, ctrl);
		debug.setMnemonic(KeyEvent.VK_D);

		run.addSeparator();

		kill = addToMenu(run, "Kill running script...", 0, 0);
		kill.setMnemonic(KeyEvent.VK_K);
		kill.setEnabled(false);

		run.addSeparator();

		resume = addToMenu(run, "Resume", 0, 0);
		resume.setMnemonic(KeyEvent.VK_R);
		terminate = addToMenu(run, "Terminate", 0, 0);
		terminate.setMnemonic(KeyEvent.VK_T);
		mbar.add(run);

		JMenu tools = new JMenu("Tools");
		tools.setMnemonic(KeyEvent.VK_O);
		openHelpWithoutFrames = addToMenu(tools,
			"Open Help for Class...", 0, 0);
		openHelpWithoutFrames.setMnemonic(KeyEvent.VK_O);
		openHelp = addToMenu(tools,
				"Open Help for Class (with frames)...", 0, 0);
		openHelp.setMnemonic(KeyEvent.VK_P);
		mbar.add(tools);

		tabsMenu = new JMenu("Tabs");
		tabsMenu.setMnemonic(KeyEvent.VK_A);
		nextTab = addToMenu(tabsMenu, "Next Tab",
				KeyEvent.VK_PAGE_DOWN, ctrl);
		nextTab.setMnemonic(KeyEvent.VK_N);
		previousTab = addToMenu(tabsMenu, "Previous Tab",
				KeyEvent.VK_PAGE_UP, ctrl);
		previousTab.setMnemonic(KeyEvent.VK_P);
		tabsMenu.addSeparator();
		tabsMenuTabsStart = tabsMenu.getItemCount();
		tabsMenuItems = new HashSet<JMenuItem>();
		mbar.add(tabsMenu);

		// Add the editor and output area
		tabbed = new JTabbedPane();
		tabbed.addChangeListener(this);
		open(path);

		screen = new JTextArea();
		screen.setEditable(false);
		screen.setLineWrap(true);
		Font font = new Font("Courier", Font.PLAIN, 12);
		screen.setFont(font);
		JScrollPane scroll = new JScrollPane(screen);
		scroll.setPreferredSize(new Dimension(600, 80));

		JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbed, scroll);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		panel.setResizeWeight(350.0 / 430.0);
		setContentPane(panel);

		// for Eclipse and MS Visual Studio lovers
		addAccelerator(compileAndRun, KeyEvent.VK_F11, 0, true);
		addAccelerator(compileAndRun, KeyEvent.VK_F5, 0, true);
		addAccelerator(debug, KeyEvent.VK_F11, ctrl, true);
		addAccelerator(debug, KeyEvent.VK_F5, shift, true);
		addAccelerator(nextTab, KeyEvent.VK_PAGE_DOWN, ctrl, true);
		addAccelerator(previousTab, KeyEvent.VK_PAGE_UP, ctrl, true);

		// make sure that the window is not closed by accident
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				while (tabbed.getTabCount() > 0) {
					if (!handleUnsavedChanges())
						return;
					int index = tabbed.getSelectedIndex();
					removeTab(index);
				}
				dispose();
			}

			public void windowClosed(WindowEvent e) {
				WindowManager.removeWindow(TextEditor.this);
			}
		});

		addWindowFocusListener(new WindowAdapter() {
			public void windowGainedFocus(WindowEvent e) {
				getEditorPane().checkForOutsideChanges();
			}
		});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		pack();
		getToolkit().setDynamicLayout(true);            //added to accomodate the autocomplete part
		findDialog = new FindAndReplaceDialog(this);

		setLocationRelativeTo(null); // center on screen

		editorPane.requestFocus();
	}

	public TextEditor(String title, String text) {
		this(null);
		editorPane.setText(text);
		String extension = editorPane.getExtension(title);
		editorPane.setLanguageByExtension(extension);
		setTitle();
	}

	final public RSyntaxTextArea getTextArea() {
		return getEditorPane();
	}

	public EditorPane getEditorPane() {
		return editorPane;
	}

	public Languages.Language getCurrentLanguage() {
		return getEditorPane().currentLanguage;
	}

	public JMenuItem addToMenu(JMenu menu, String menuEntry,
			int key, int modifiers) {
		JMenuItem item = new JMenuItem(menuEntry);
		menu.add(item);
		if (key != 0)
			item.setAccelerator(KeyStroke.getKeyStroke(key,
						modifiers));
		item.addActionListener(this);
		return item;
	}

	protected static class AcceleratorTriplet {
		JMenuItem component;
		int key, modifiers;
	}

	protected List<AcceleratorTriplet> defaultAccelerators =
		new ArrayList<AcceleratorTriplet>();

	public void addAccelerator(final JMenuItem component,
			int key, int modifiers) {
		addAccelerator(component, key, modifiers, false);
	}

	public void addAccelerator(final JMenuItem component,
			int key, int modifiers, boolean record) {
		if (record) {
			AcceleratorTriplet triplet = new AcceleratorTriplet();
			triplet.component = component;
			triplet.key = key;
			triplet.modifiers = modifiers;
			defaultAccelerators.add(triplet);
		}

		RSyntaxTextArea textArea = getTextArea();
		textArea.getInputMap().put(KeyStroke.getKeyStroke(key,
					modifiers), component);
		if (textArea.getActionMap().get(component) != null)
			return;
		textArea.getActionMap().put(component,
				new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (!component.isEnabled())
					return;
				ActionEvent event = new ActionEvent(component,
					0, "Accelerator");
				TextEditor.this.actionPerformed(event);
			}
		});
	}

	public void addDefaultAccelerators() {
		for (AcceleratorTriplet triplet : defaultAccelerators)
			addAccelerator(triplet.component,
					triplet.key, triplet.modifiers, false);
	}

	/**
	 * Gets the base path of the resources contained in this jar.
	 */
	private String getResourceBase() {
		return Script_Editor.class.getName().replace(".", "/")+".class";
	}

	/**
	 * Initializes a member set with paths leading to templates.
	 */
	private void setupTemplatePaths() {
		templatePaths = new HashSet<String>(); //avoid duplicates in case it is a subdirectory

		URL dirURL = Script_Editor.class.getClassLoader().getResource( getResourceBase() );

		// check if the resource has been found inside the jar
		if (dirURL == null || dirURL.getProtocol() != "jar") {
			return;
		}

		// modified version of http://www.uofr.net/~greg/java/get-resource-listing.html
		String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file

		try {
			JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
			Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar

			while(entries.hasMoreElements()) {
				String name = entries.nextElement().getName();
				if (name.startsWith(templateFolder)) { //filter according to the path
					String entry = name.substring(templateFolder.length());
					templatePaths.add(entry);
				}
			}
		}
		catch (java.io.UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Populates the given menu with template files available in the
	 * templates folder. The folder structure is reflected within the sub menus.
	 */
	private void populateTemplateMenu(JMenu menu) {
		menu.removeAll();

		// use a dictionary for keeping track of created menu items
		Dictionary<String, JMenu> menuEntries = new Hashtable<String, JMenu>();

		String[] paths =
			templatePaths.toArray(new String[templatePaths.size()]);
		Arrays.sort(paths, new Comparator<String>() {
			public int compare(String s1, String s2) {
				int slash1 = s1.lastIndexOf('/');
				int slash2 = s2.lastIndexOf('/');
				return s1.substring(slash1 + 1)
					.compareTo(s2.substring(slash2 + 1));
			}
			public boolean equals(Object o) {
				return false;
			}
		});
		for (String t : paths)
			reflectDirStructInMenu(menuEntries, menu, t, "");

		// add a „none“ item if no template was found
		if (menu.getItemCount() == 0) {
			JMenuItem none_item = new JMenuItem("(none)");
			none_item.setEnabled(false);
			menu.add(none_item);
		}
	}

	/**
	 * Adds a menu item or a sub menu to the given menu, depending
	 * on the path it should reflect.
	 */
	public void reflectDirStructInMenu(Dictionary<String, JMenu> menuEntries,
			 JMenu menu, String res, String resPath) {
		// Add menu items reflecting the files, i. e. the actual templates (typically on)
		final boolean showFiles = true;
		// Reflect tha dicetory structure within the menu
		final boolean showFolders = false;
		// The language of the template will be added the name (requires ShowFiles)
		final boolean appendLang = true;
		// Indicates that the language should be switched on template selection
		final boolean switchLang = true;
		// Show file name instead of stripped version
		final boolean showFileName = false;

		int checkSubdir = res.indexOf("/");
		if (checkSubdir >= 0) {
			// if it is a subdirectory, get the directory name
			String name = res.substring(0, checkSubdir);
			res = res.substring(checkSubdir + 1); // cut off the slash for next level

			// remember the current level
			resPath = resPath + name + "/";

			if (showFolders) {
				// create a new sub menu if not already there
				JMenu subMenu = menuEntries.get(resPath);
				if (subMenu == null) {
					subMenu = new JMenu(name);
					menuEntries.put(resPath, subMenu);
					menu.add(subMenu);
				}
				menu = subMenu;
			}

			// recursively go througn the path
			reflectDirStructInMenu(menuEntries, menu, res, resPath);
		} else {
			// res in now the file name and resPath is the path to it

			if (showFiles) {
				String name = res;
				if (!showFileName) {
					// replace uder scores with spaces
					name = name.replace("_", " ");
					// remove file extension, if any present
					int dot = name.lastIndexOf(".");
					if (dot >= 0)
						name = name.substring(0, dot);
				}

				// Get sub folder name, which should
				// represent language
				String subFolderName = "";
				int slash = resPath.indexOf("/");
				if (slash >= 0) {
					subFolderName = resPath.substring(0, slash);
				}

				// Try to mach sub folder name to
				// available languges
				Languages.Language templateLang = null;
				for (final Languages.Language l : availableLanguages) {
					// compare first sub folder (if any) to known
					// languge names
					if (l.menuLabel.equalsIgnoreCase(subFolderName)) {
						templateLang = l;
						break;
					}
				}

				// if enabled, add laguge desription to label
				if (appendLang) {
					if (templateLang != null) {
						name += " [" + templateLang.menuLabel + "]";
					} else {
						name += " [unknown]";
					}
				}

				JMenuItem item = new JMenuItem(name);
				menu.add(item);

				// create final properties for inner class
				final String resource = templateFolder + resPath + res;
				final Languages.Language linkedLang = templateLang;

				// add inner action listener class for item
				item.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// A template menu item opens a corresponding
						// template file.
						loadTemplate(resource, linkedLang, switchLang);
					}});
			}
		}
	}

	/**
	 * Loads a template file from the given resource out of the jar file and
	 * optionally switches the langunge.
	 *
	 * @param resource The resource to load.
	 * @param lang The language to optionally switch to or null
	 * @param switchLang Whether the language should be switched or not.
	 */
	public void loadTemplate(String resource, Languages.Language lang, boolean switchLang) {
		createNewDocument();

		try {
			// Load the template
			InputStream is = Script_Editor.class.getClassLoader().getResourceAsStream(resource);
			getTextArea().read(new BufferedReader(
				new InputStreamReader(is)),
				null);

			// Switch the language
			if (switchLang && lang != null) {
				setLanguage(lang);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			error("The template '" + resource + "' was not found.");
			return;
		}
	}

	public void createNewDocument() {
		open(null);
	}

	public boolean fileChanged() {
		return getEditorPane().fileChanged();
	}

	public boolean handleUnsavedChanges() {
		if (!fileChanged())
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
		if (source == newFile)
			createNewDocument();
		else if (source == open) {
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
		else if (source == makeJar)
			makeJar(false);
		else if (source == makeJarWithSource)
			makeJar(true);
		else if (source == compileAndRun)
			runText();
		else if (source == runSelection)
			runText(true);
		else if (source == nextError)
			new Thread() {
				public void run() {
					nextError(true);
				}
			}.start();
		else if (source == previousError)
			new Thread() {
				public void run() {
					nextError(false);
				}
			}.start();
		else if (source == debug) {
			try {
				getEditorPane().startDebugging();
			} catch (Exception e) {
				error("No debug support for this language");
			}
		}
		else if (source == kill)
			chooseTaskToKill();
		else if (source == close)
			if (tabbed.getTabCount() < 2)
				processWindowEvent(new WindowEvent(this,
						WindowEvent.WINDOW_CLOSING));
			else {
				if (!handleUnsavedChanges())
					return;
				int index = tabbed.getSelectedIndex();
				removeTab(index);
				if (index > 0)
					index--;
				switchTo(index);
			}
		else if (source == cut)
			getTextArea().cut();
		else if (source == copy)
			getTextArea().copy();
		else if (source == paste)
			getTextArea().paste();
		else if (source == undo)
			getTextArea().undoLastAction();
		else if (source == redo)
			getTextArea().redoLastAction();
		else if (source == find)
			findOrReplace(false);
		else if (source == findNext)
			findDialog.searchOrReplace(false);
		else if (source == replace)
			findOrReplace(true);
		else if (source == gotoLine)
			gotoLine();
		else if (source == selectAll) {
			getTextArea().setCaretPosition(0);
			getTextArea().moveCaretPosition(getTextArea().getDocument().getLength());
		}
		else if (source == addImport)
			addImport(null);
		else if (source == removeUnusedImports)
			new TokenFunctions(getTextArea()).removeUnusedImports();
		else if (source == sortImports)
			new TokenFunctions(getTextArea()).sortImports();
		else if (source == removeTrailingWhitespace)
			new TokenFunctions(getTextArea()).removeTrailingWhitespace();
		else if (source == clearScreen)
			screen.setText("");
		else if (source == autocomplete) {
			try {
				getEditorPane().autocomp.doCompletion();
			} catch (Exception e) {}
		}
		else if (source == resume)
			getEditorPane().resume();
		else if (source == terminate) {
			getEditorPane().terminate();
		}
		else if (source == openHelp)
			openHelp(null);
		else if (source == openHelpWithoutFrames)
			openHelp(null, false);
		else if (source == nextTab)
			switchTabRelative(1);
		else if (source == previousTab)
			switchTabRelative(-1);
		else if (handleTabsMenu(source))
			return;
	}

	protected boolean handleTabsMenu(Object source) {
		if (!(source instanceof JMenuItem))
			return false;
		JMenuItem item = (JMenuItem)source;
		if (!tabsMenuItems.contains(item))
			return false;
		for (int i = tabsMenuTabsStart;
				i < tabsMenu.getItemCount(); i++)
			if (tabsMenu.getItem(i) == item) {
				switchTo(i - tabsMenuTabsStart);
				return true;
			}
		return false;
	}

	public void stateChanged(ChangeEvent e) {
		int index = tabbed.getSelectedIndex();
		if (index < 0) {
			setTitle("");
			return;
		}
		editorPane = getEditorPane(index);
		editorPane.requestFocus();
		setTitle();
		String extension = editorPane.getExtension(editorPane.getFileName());
		editorPane.setLanguageByExtension(extension);
	}

	public EditorPane getEditorPane(int index) {
		RTextScrollPane scrollPane =
			(RTextScrollPane)tabbed.getComponentAt(index);
		return (EditorPane)scrollPane.getTextArea();
	}

	public void findOrReplace(boolean replace) {
		findDialog.setLocationRelativeTo(this);

		// override search pattern only if
		// there is sth. selected
		String selection = getTextArea().getSelectedText();
		if (selection != null)
			findDialog.setSearchPattern(selection);

		findDialog.show(replace);
	}

	public void gotoLine() {
		String line = JOptionPane.showInputDialog(this, "Line:",
			"Goto line...", JOptionPane.QUESTION_MESSAGE);
		try {
			gotoLine(Integer.parseInt(line));
		} catch (BadLocationException e) {
			error("Line number out of range: " + line);
		} catch (NumberFormatException e) {
			error("Invalid line number: " + line);
		}
	}

	public void gotoLine(int line) throws BadLocationException {
		getTextArea().setCaretPosition(getTextArea().getLineStartOffset(line-1));
	}

	public boolean reload() {
		return reload("Reload the file?");
	}

	public boolean reload(String message) {
		File file = getEditorPane().file;
		if (file == null || !file.exists())
			return true;

		boolean modified = getEditorPane().fileChanged();
		String[] options = { "Reload", "Do not reload" };
		if (modified)
			options[0] = "Reload (discarding changes)";
		switch (JOptionPane.showOptionDialog(this, message, "Reload",
			JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
			null, options, options[0])) {
		case 0:
			try {
				editorPane.setFile(file.getPath());
				return true;
			} catch (IOException e) {
				error("Could not reload " + file.getPath());
			}
			break;
		}
		return false;
	}

	public void open(String path) {
		try {
			editorPane = new EditorPane(this);
			tabbed.addTab("", editorPane.embedWithScrollbars());
			switchTo(tabbed.getTabCount() - 1);
			addDefaultAccelerators();
			editorPane.setFile("".equals(path) ? null : path);
			tabsMenuItems.add(addToMenu(tabsMenu,
					editorPane.getFileName(), 0, 0));
		} catch (Exception e) {
			e.printStackTrace();
			error("The file '" + path + "' was not found.");
			return;
		}
	}

	public boolean saveAs() {
		SaveDialog sd = new SaveDialog("Save as ",
				getEditorPane().getFileName() , "");
		String name = sd.getFileName();
		if (name == null)
			return false;

		String path = sd.getDirectory() + name;
		return saveAs(path, true);
	}

	public void saveAs(String path) {
		saveAs(path, true);
	}

	public boolean saveAs(String path, boolean askBeforeReplacing) {
		File file = new File(path);
		if (file.exists() && askBeforeReplacing &&
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
		File file = getEditorPane().file;
		if (file == null)
			return saveAs();
		if (!write(file))
			return false;
		setTitle();
		return true;
	}

	public boolean write(File file) {
		try {
			getEditorPane().write(file);
			return true;
		} catch (IOException e) {
			error("Could not save " + file.getName());
			e.printStackTrace();
			return false;
		}
	}

	public boolean makeJar(boolean includeSources) {
		File file = getEditorPane().file;
		Languages.Language currentLanguage = getCurrentLanguage();
		if ((file == null || currentLanguage.isCompileable())
				&& !handleUnsavedChanges())
			return false;

		String name = getEditorPane().getFileName();
		if (name.endsWith(currentLanguage.extension))
			name = name.substring(0, name.length()
				- currentLanguage.extension.length());
		name += ".jar";
		SaveDialog sd = new SaveDialog("Export ", name, ".jar");
		name = sd.getFileName();
		if (name == null)
			return false;

		String path = sd.getDirectory() + name;
		if (new File(path).exists() &&
				JOptionPane.showConfirmDialog(this,
					"Do you want to replace " + path + "?",
					"Replace " + path + "?",
					JOptionPane.YES_NO_OPTION)
				!= JOptionPane.YES_OPTION)
			return false;
		try {
			makeJar(path, includeSources);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			error("Could not write " + path
					+ ": " + e.getMessage());
			return false;
		}
	}

	public void makeJar(String path, boolean includeSources)
			throws IOException {
		List<String> paths = new ArrayList<String>();
		List<String> names = new ArrayList<String>();
		File tmpDir = null, file = getEditorPane().file;
		String sourceName = null;
		Languages.Language currentLanguage = getCurrentLanguage();
		if (currentLanguage.interpreter instanceof Refresh_Javas) try {
			String sourcePath = file.getAbsolutePath();
			Refresh_Javas java =
				(Refresh_Javas)currentLanguage.interpreter;
			tmpDir = File.createTempFile("tmp", "");
			tmpDir.delete();
			tmpDir.mkdir();
			java.compile(sourcePath, tmpDir.getAbsolutePath());
			getClasses(tmpDir, paths, names);
			if (includeSources) {
				String name = java.getPackageName(sourcePath);
				name = (name == null ? "" :
						name.replace('.', '/') + "/")
					+ file.getName();
				sourceName = name;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (e instanceof IOException)
				throw (IOException)e;
			throw new IOException(e.getMessage());
		}
		else
			sourceName = file.getName();

		OutputStream out = new FileOutputStream(path);
		JarOutputStream jar = new JarOutputStream(out);

		if (sourceName != null)
			writeJarEntry(jar, sourceName,
					getTextArea().getText().getBytes());
		for (int i = 0; i < paths.size(); i++)
			writeJarEntry(jar, names.get(i),
					readFile(paths.get(i)));

		jar.close();

		if (tmpDir != null)
			deleteRecursively(tmpDir);
	}

	static void getClasses(File directory,
			List<String> paths, List<String> names) {
		getClasses(directory, paths, names, "");
	}

	static void getClasses(File directory,
			List<String> paths, List<String> names, String prefix) {
		if (!prefix.equals(""))
			prefix += "/";
		for (File file : directory.listFiles())
			if (file.isDirectory())
				getClasses(file, paths, names,
						prefix + file.getName());
			else {
				paths.add(file.getAbsolutePath());
				names.add(prefix + file.getName());
			}
	}

	static void writeJarEntry(JarOutputStream out, String name,
			byte[] buf) throws IOException {
		try {
			JarEntry entry = new JarEntry(name);
			out.putNextEntry(entry);
			out.write(buf, 0, buf.length);
			out.closeEntry();
		} catch (ZipException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
	}

	static byte[] readFile(String fileName) throws IOException {
		File file = new File(fileName);
		InputStream in = new FileInputStream(file);
		byte[] buffer = new byte[(int)file.length()];
		in.read(buffer);
		in.close();
		return buffer;
	}

	static void deleteRecursively(File directory) {
		for (File file : directory.listFiles())
			if (file.isDirectory())
				deleteRecursively(file);
			else
				file.delete();
		directory.delete();
	}

	void setLanguage(Languages.Language language) {
		getEditorPane().setLanguage(language);
	}

	void updateLanguageMenu(Languages.Language language) {
		if (!language.item.isSelected())
			language.item.setSelected(true);

		compileAndRun.setLabel(language.isCompileable() ?
			"Compile and Run" : "Run");
		compileAndRun.setEnabled(language.isRunnable());
		runSelection.setEnabled(language.isRunnable() &&
				!language.isCompileable());
		debug.setEnabled(language.isDebuggable());
		makeJarWithSource.setEnabled(language.isCompileable());

		boolean isJava = language.menuLabel.equals("Java");
		addImport.setEnabled(isJava);
		removeUnusedImports.setEnabled(isJava);
		sortImports.setEnabled(isJava);
	}

	public void setFileName(File file) {
		getEditorPane().setFileName(file);
	}

	synchronized void setTitle() {
		boolean fileChanged = getEditorPane().fileChanged();
		String fileName = getEditorPane().getFileName();
		String title = (fileChanged ? "*" : "") + fileName
			+ (executingTasks.isEmpty() ? "" : " (Running)");
		setTitle(title);
		tabbed.setTitleAt(tabbed.getSelectedIndex(), title);
	}

	public void setTitle(String title) {
		super.setTitle(title);
		int index = tabsMenuTabsStart + tabbed.getSelectedIndex();
		if (index < tabsMenu.getItemCount()) {
			JMenuItem item = tabsMenu.getItem(index);
			if (item != null)
				item.setLabel(title);
		}
	}

	/** Using a Vector to benefit from all its methods being synchronzed. */
	private ArrayList<Executer> executingTasks = new ArrayList<Executer>();

	/** Generic Thread that keeps a starting time stamp,
	 *  sets the priority to normal and starts itself. */
	private abstract class Executer extends ThreadGroup {
		JTextAreaOutputStream output;
		Executer(final JTextAreaOutputStream output) {
			super("Script Editor Run :: " + new Date().toString());
			this.output = output;
			// Store itself for later
			executingTasks.add(this);
			setTitle();
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
						// Wait until any children threads die:
						while (Executer.this.activeCount() > 1) {
							if (isInterrupted()) break;
							try {
								Thread.sleep(500);
							} catch (InterruptedException ie) {}
						}
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						executingTasks.remove(Executer.this);
						try {
							if (null != output)
								output.shutdown();
						} catch (Exception e) {
							e.printStackTrace();
						}
						// Leave kill menu item enabled if other tasks are running
						kill.setEnabled(executingTasks.size() > 0);
						setTitle();
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

		/** Totally destroy/stop all threads in this and all recursive thread subgroups. Will remove itself from the executingTasks list. */
		void obliterate() {
			try {
				// Stop printing to the screen
				if (null != output)
					output.shutdownNow();
			} catch (Exception e) {
				e.printStackTrace();
			}
			for (Thread thread : getAllThreads()) {
				try {
					thread.interrupt();
					Thread.yield(); // give it a chance
					thread.stop();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
			executingTasks.remove(this);
			setTitle();
		}
	}

	/** Query the list of running scripts and provide a dialog to choose one and kill it. */
	public void chooseTaskToKill() {
		Executer[] executers =
			executingTasks.toArray(new Executer[0]);
		if (0 == executers.length) {
			error("\nNo tasks running!\n");
			return;
		}

		String[] names = new String[executers.length];
		for (int i = 0; i < names.length; i++)
			names[i] = executers[i].getName();

		GenericDialog gd = new GenericDialog("Kill");
		gd.addChoice("Running scripts: ",
				names, names[names.length - 1]);
		gd.addCheckbox("Kill all", false);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		Executer[] deaders = gd.getNextBoolean() ? executers :
			new Executer[] { executers[gd.getNextChoiceIndex()] };
		for (final Executer executer : deaders)
			kill(executer);
	}

	protected void kill(final Executer executer) {
		// Graceful attempt:
		executer.interrupt();
		// Give it 3 seconds. Then, stop it.
		final long now = System.currentTimeMillis();
		new Thread() {
			{ setPriority(Thread.NORM_PRIORITY); }
			public void run() {
				while (System.currentTimeMillis() - now < 3000)
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
				executer.obliterate();
			}
		}.start();
	}

	/** Run the text in the textArea without compiling it, only if it's not java. */
	public void runText() {
		runText(false);
	}

	public void runText(boolean selectionOnly) {
		Languages.Language currentLanguage = getCurrentLanguage();
		if (currentLanguage.isCompileable()) {
			if (selectionOnly) {
				error("Cannot run selection of compiled language!");
				return;
			}
			if (handleUnsavedChanges())
				runScript();
			return;
		}
		if (!currentLanguage.isRunnable()) {
			error("Select a language first!");
			// TODO guess the language, if possible.
			return;
		}

		markCompileStart();
		RSyntaxTextArea textArea = getTextArea();
		textArea.setEditable(false);
		final JTextAreaOutputStream output = new JTextAreaOutputStream(screen);
		try {
			final RefreshScripts interpreter =
				currentLanguage.interpreter;
			interpreter.setOutputStreams(output, output);

			// Pipe current text into the runScript:
			final PipedInputStream pi = new PipedInputStream();
			final PipedOutputStream po = new PipedOutputStream(pi);
			new TextEditor.Executer(output) {
				public void execute() {
					interpreter.runScript(pi);
					output.flush();
					markCompileEnd();
				}
			};
			if (selectionOnly) {
				String text = textArea.getSelectedText();
				if (text == null)
					error("Selection required!");
				else
					po.write(text.getBytes());
			}
			else
				textArea.write(new PrintWriter(po));
			po.flush();
			po.close();
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			textArea.setEditable(true);
		}
	}

	public void runScript() {
		final RefreshScripts interpreter =
			getCurrentLanguage().interpreter;

		if (interpreter == null) {
			error("There is no interpreter for this language");
			return;
		}

		markCompileStart();
		final JTextAreaOutputStream output = new JTextAreaOutputStream(screen);
		interpreter.setOutputStreams(output, output);

		final File file = getEditorPane().file;
		new TextEditor.Executer(output) {
			public void execute() {
				interpreter.runScript(file.getPath());
				output.flush();
				markCompileEnd();
			}
		};
	}

	public String getSelectedTextOrAsk(String label) {
		String selection = getTextArea().getSelectedText();
		if (selection == null) {
			selection = JOptionPane.showInputDialog(this,
				label + ":", label + "...",
				JOptionPane.QUESTION_MESSAGE);
			if (selection == null)
				return null;
		}
		return selection;
	}

	public void markCompileStart() {
		errorHandler = null;

		Document document = screen.getDocument();
		int offset = document.getLength();
		screen.insert("Started " + editorPane.getFileName() + " at "
			+ new Date() + "\n", offset);
		screen.setCaretPosition(document.getLength());
		try {
			compileStartPosition = document.createPosition(offset);
		} catch (BadLocationException e) {
			handleException(e);
		}
		ExceptionHandler.addThread(Thread.currentThread(), this);
	}

	public void markCompileEnd() {
		if (errorHandler == null)
			errorHandler = new ErrorHandler(getCurrentLanguage(),
				screen, compileStartPosition.getOffset());
	}

	public boolean nextError(boolean forward) {
		if (errorHandler != null && errorHandler.nextError(forward)) try {
			switchTo(errorHandler.getPath(),
					errorHandler.getLine());
			errorHandler.markLine();
			return true;
		} catch (Exception e) {
			IJ.handleException(e);
		}
		return false;
	}

	public void switchTo(String path, int lineNumber)
			throws BadLocationException, IOException {
		switchTo(new File(path).getCanonicalFile(), lineNumber);
	}

	public void switchTo(File file, int lineNumber)
			throws BadLocationException {
		if (!editorPaneContainsFile(editorPane, file))
			switchTo(file);
		gotoLine(lineNumber);
	}

	public void switchTo(File file) {
		for (int i = 0; i < tabbed.getTabCount(); i++)
			if (editorPaneContainsFile(getEditorPane(i), file)) {
				switchTo(i);
				return;
			}
		open(file.getPath());
	}

	public void switchTo(int index) {
		tabbed.setSelectedIndex(index);
	}

	protected void switchTabRelative(int delta) {
		int index = tabbed.getSelectedIndex();
		int count = tabbed.getTabCount();
		index = ((index + delta) % count);
		if (index < 0)
			index += count;
		switchTo(index);
	}

	protected void removeTab(int index) {
		tabbed.remove(index);
		index += tabsMenuTabsStart;
		tabsMenuItems.remove(tabsMenu.getItem(index));
		tabsMenu.remove(index);
	}

	boolean editorPaneContainsFile(EditorPane editorPane, File file) {
		try {
			return file.getCanonicalFile()
				.equals(editorPane.file.getCanonicalFile());
		} catch (IOException e) {
			return false;
		}
	}

	public File getFile() {
		return getEditorPane().file;
	}

	public File getFileForBasename(String baseName) {
		File file = getFile();
		if (file != null && file.getName().equals(baseName))
			return file;
		for (int i = 0; i < tabbed.getTabCount(); i++) {
			file = getEditorPane(i).file;
			if (file != null && file.getName().equals(baseName))
				return file;
		}
		return null;
	}

	public void addImport(String className) {
		if (className == null)
			className = getSelectedTextOrAsk("Class name");
		if (className == null)
			return;
		if (className.indexOf('.') < 0)
			className = getEditorPane().getClassNameFunctions()
				.getFullName(className);
		if (className != null)
			new TokenFunctions(getTextArea()).addImport(className);
	}

	public void openHelp(String className) {
		openHelp(className, true);
	}

	public void openHelp(String className, boolean withFrames) {
		if (className == null)
			className = getSelectedTextOrAsk("Class name");
		if (className == null)
			return;
		getEditorPane().getClassNameFunctions()
			.openHelpForClass(className, withFrames);
	}

	protected void error(String message) {
		JOptionPane.showMessageDialog(this, message);
	}

	void handleException(Throwable e) {
		ij.IJ.handleException(e);
	}
}
