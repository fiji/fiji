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
import java.awt.image.BufferedImage;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.IJ;
import ij.Prefs;
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


class TextEditor extends JFrame implements ActionListener, ItemListener, ChangeListener, MouseMotionListener, MouseListener, CaretListener, InputMethodListener, DocumentListener, WindowListener {

	// TODO: clean up unnecessary variables
	boolean fileChanged = false;
	boolean isFileUnnamed = true;
	String title = "";
	String language = new String();
	InputMethodListener l;
	File file, f;
	CompletionProvider provider1;
	RSyntaxTextArea textArea;
	JTextArea screen = new JTextArea();
	Document doc;
	JMenuItem new1, open, save, saveas, compileAndRun, debug, quit, undo, redo, cut, copy, paste, find, replace, selectAll, autocomplete, resume, terminate;
	JRadioButtonMenuItem[] lang = new JRadioButtonMenuItem[8];
	FileInputStream fin;
	// TODO: fix (enableReplace(boolean))
	FindAndReplaceDialog replaceDialog;
	AutoCompletion autocomp;
	// TODO: probably language can go
	ClassCompletionProvider provider;
	StartDebugging debugging;
	Gutter gutter;
	IconGroup iconGroup;

	public TextEditor(String path1) {
		JPanel cp = new JPanel(new BorderLayout());
		title = "Text Editor for Fiji";
		textArea = new RSyntaxTextArea();
		textArea.addInputMethodListener(l);
		textArea.addCaretListener(this);
		// TODO: is this necessary?
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
		// TODO: much better naming required
		// TODO: remove unnecessary curly brackets
		if (provider1 == null) {
			provider1 = createCompletionProvider();
		}
		autocomp = new AutoCompletion(provider1);
		// TODO: is this really needed?
		autocomp.setListCellRenderer(new CCellRenderer());
		autocomp.setShowDescWindow(true);
		autocomp.setParameterAssistanceEnabled(true);
		autocomp.install(textArea);
		textArea.setToolTipSupplier((ToolTipSupplier)provider);
		ToolTipManager.sharedInstance().registerComponent(textArea);
		// TODO: do we need doc?
		doc = textArea.getDocument();
		doc.addDocumentListener(this);
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
		// TODO: Unnamed
		setTitle(title);
		addWindowListener(this);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		/*********** Creating the menu options in the text editor ****************/

		JMenuBar mbar = new JMenuBar();
		// TODO: is this not too early?
		setJMenuBar(mbar);

		/*******  creating the menu for the File option **********/
		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);
		// TODO: this cannot work, new1 must be assigned
		addToMenu(file, new1, "New", 0, KeyEvent.VK_N, ActionEvent.CTRL_MASK);
		addToMenu(file, open, "Open...", 0, KeyEvent.VK_O, ActionEvent.CTRL_MASK);
		addToMenu(file, save, "Save", 0, KeyEvent.VK_S, ActionEvent.CTRL_MASK);
		addToMenu(file, save, "Save as...", 1, 0, 0);
		file.addSeparator();
		addToMenu(file, quit, "Quit", 0, KeyEvent.VK_X, ActionEvent.ALT_MASK);

		mbar.add(file);

		/********The file menu part ended here  ***************/

		/*********The Edit menu part starts here ***************/

		JMenu edit = new JMenu("Edit");
		addToMenu(edit, undo, "Undo", 0, KeyEvent.VK_Z, ActionEvent.CTRL_MASK);
		addToMenu(edit, redo, "Redo", 0, KeyEvent.VK_Y, ActionEvent.CTRL_MASK);
		edit.addSeparator();
		addToMenu(edit, cut, "Cut", 0, KeyEvent.VK_X, ActionEvent.CTRL_MASK);
		addToMenu(edit, copy, "Copy", 0, KeyEvent.VK_C, ActionEvent.CTRL_MASK);
		addToMenu(edit, paste, "Paste", 0, KeyEvent.VK_V, ActionEvent.CTRL_MASK);
		edit.addSeparator();
		addToMenu(edit, find, "Find...", 0, KeyEvent.VK_F, ActionEvent.CTRL_MASK);
		addToMenu(edit, replace, "Find and Replace...", 0, KeyEvent.VK_H, ActionEvent.CTRL_MASK);
		edit.addSeparator();
		// TODO: this belongs higher, no?
		addToMenu(edit, selectAll, "Select All", 0, KeyEvent.VK_A, ActionEvent.CTRL_MASK);
		mbar.add(edit);

		/******** The Edit menu part ends here *****************/

		// TODO: remove useless comments
		/********The options menu part starts here**************/
		// TODO: add accelerator keys for the menus, too
		JMenu options = new JMenu("Options");
		// TODO: CTRL, ALT
		addToMenu(options, autocomplete, "Autocomplete", 0, KeyEvent.VK_SPACE, ActionEvent.CTRL_MASK);
		options.addSeparator();

		mbar.add(options);

		/*********The Language parts starts here********************/
		JMenu languages = new JMenu("Language");
		ButtonGroup group = new ButtonGroup();
		for (Languages.Language language :
		                Languages.getInstance().languages) {
			JRadioButtonMenuItem item =
			        new JRadioButtonMenuItem(language.menuLabel);
			if (language.shortCut != 0)
				item.setMnemonic(language.shortCut);
			item.addActionListener(this);
			item.setActionCommand(language.extension);

			group.add(item);
			languages.add(item);
			language.item = item;
		}
		Languages.getInstance().get("").item.setSelected(true);
		mbar.add(languages);

		JMenu run = new JMenu("Run");
		// TODO: allow outside-of-plugins/ sources
		addToMenu(run, compileAndRun, "Compile and Run", 0, KeyEvent.VK_F11, ActionEvent.CTRL_MASK);
		run.addSeparator();
		addToMenu(run, debug, "Start Debugging", 0, KeyEvent.VK_F11, 0);
		mbar.add(run);

		JMenu breakpoints = new JMenu("Breakpoints");
		addToMenu(breakpoints, resume, "Resume", 1, 0, 0);
		addToMenu(breakpoints, terminate, "Terminate", 1, 0, 0);
		mbar.add(breakpoints);




		/*********** The menu part ended here    ********************/

		pack();
		getToolkit().setDynamicLayout(true);            //added to accomodate the autocomplete part
		// TODO: is this needed?
		setLocationRelativeTo(null);
		setVisible(true);
		if (!(path1.equals("") || path1 == null)) {
			open(path1);
		}
	}

	public void addToMenu(JMenu menu, JMenuItem menuitem, String menuEntry, int keyEvent, int keyevent, int actionevent) {
		menuitem = new JMenuItem(menuEntry);
		menu.add(menuitem);
		if (keyEvent == 0) // == 0?  Not != 0?
			menuitem.setAccelerator(KeyStroke.getKeyStroke(keyevent, actionevent));
		menuitem.addActionListener(this);
	}

	public void createNewDocument() {
		//TODO: Hmm.
		doc.removeDocumentListener(this);
		textArea.setText("");
		setTitle("TextEditor for Fiji");
		isFileUnnamed = true;
		fileChanged = false;
		doc.addDocumentListener(this);
	}

	public int handleUnsavedChanges() {
		if (fileChanged == true) {
			// TODO: saveChangeDialog() is useless outside of this function; merge the code
			// TODO: use switch()
			int val = saveChangeDialog();
			if (val == JOptionPane.CANCEL_OPTION) {
				return 0;
			} else if (val == JOptionPane.YES_OPTION) {
				if (save() != JFileChooser.APPROVE_OPTION)
					return 0;
			} else if (val != JOptionPane.NO_OPTION) {
				return 0;
			}
		}
		return 1;
	}

	public int saveChangeDialog() {
		return(JOptionPane.showConfirmDialog(this, "Do you want to save changes??"));
	}


	public void actionPerformed(ActionEvent ae) {
		String command = ae.getActionCommand();
		// TODO: NO!!!!
		if (command.equals("New")) {
			if (handleUnsavedChanges() == 0)
				return;
			// TODO: NO!!!!
			else
				createNewDocument();
		}

		if (command.equals("Open...")) {
			// TODO: handle == 0 by returning!
			if (handleUnsavedChanges() != 0) {
				OpenDialog dialog = new OpenDialog("Open..", "");
				String directory = dialog.getDirectory();
				String name = dialog.getFileName();
				String path = "";
				if (name != null) {
					path = directory + name;
					boolean fullPath = path.startsWith("/") || path.startsWith("\\") || path.indexOf(":\\") == 1 || path.startsWith("http://");
					if (!fullPath) {
						String workingDir = OpenDialog.getDefaultDirectory();
						if (workingDir != null)
							path = workingDir + path;
					}
					open(path);
				}
			}
		}

		if (command.equals("Save")) {
			save();
		}
		if (command.equals("Save as..."))  {
			// TODO: fix "funny" naming
			saveasaction();
		}
		if (command.equals("Compile and Run")) {
			// TODO: s/Script//
			runScript();
		}
		if (command.equals("Start Debugging")) {
			BreakpointManager manager = new BreakpointManager(gutter, textArea, iconGroup);
			debugging = new StartDebugging(file.getPath(), manager.findBreakpointsLineNumber());

			try {
				System.out.println(debugging.startDebugging().exitValue());
				// TODO: at least use printStackTrace()
			} catch (Exception e) {}
		}
		if (command.equals("Quit")) {
			processWindowEvent( new WindowEvent(this, WindowEvent.WINDOW_CLOSING) );
		}

		if (command.equals("Cut")) {
			textArea.cut();
		}
		if (command.equals("Copy")) {
			textArea.copy();
		}
		if (command.equals("Paste")) {
			textArea.paste();
		}
		if (command.equals("Undo")) {
			textArea.undoLastAction();
		}
		if (command.equals("Redo")) {
			textArea.redoLastAction();
		}
		if (command.equals("Find...")) {

			setFindAndReplace(false);
		}
		if (command.equals("Find and Replace...")) {						//here should the code to close all other dialog boxes
			try {
				setFindAndReplace(true);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		if (command.equals("Select All")) {
			textArea.setCaretPosition(0);
			textArea.moveCaretPosition(textArea.getDocument().getLength());
		}

		if (command.equals("Autocomplete")) {
			try {
				autocomp.doCompletion();
			} catch (Exception e) {}
		}

		//setting actionPerformed for language menu
		// TODO: handle "None"
		if (command.startsWith("."))
			setLanguageByExtension(command);
		if (command.equals("Resume"))
			debugging.resumeVM();
		if (command.equals("Terminate")) { }

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
		// TODO: Why?
		doc.removeDocumentListener(this);
		try {
			if (file != null) {
				fileChanged = false;
				setFileName(file);
				// TODO: urgh. no member variable; close
				fin = new FileInputStream(file);
				BufferedReader din = new BufferedReader(new InputStreamReader(fin));
				String s = "";
				textArea.setText("");
				while (true) {
					s = din.readLine();
					if (s == null) {
						break;
					}
					// TODO: use StringBuilder instead!!!
					textArea.append(s + "\n");
				}
			} else {
				// TODO: unify error handling.  Don't mix JOptionPane with IJ.error as if we had no clue what we want
				JOptionPane.showMessageDialog(this, "The file name " + file.getName() + " not found.");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (null != fin) {
				try {
					fin.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
			doc.addDocumentListener(this);
		}

	}

	public int saveasaction() {
		SaveDialog sd = new SaveDialog("Save as ", "new", "");
		String name = sd.getFileName();
		if (name != null) {
			String path = sd.getDirectory() + name;
			return(saveAs(path, checkForReplace(sd.getDirectory(), name)));

		} else {
			return -1;
		}
	}

	// TODO: this is racy at best
	public boolean checkForReplace(String directory, String name) {
		return(new File(directory, name).exists());

	}

	public void saveAs(String path) {
		saveAs(path, true);
	}

	public int saveAs(String path, boolean ifReplaceFile) {

		file = new File(path);

		try {
			if (ifReplaceFile) {
				int val = JOptionPane.showConfirmDialog(this, "Do you want to replace " + file.getName() + "??", "Do you want to replace " + file.getName() + "??", JOptionPane.YES_NO_OPTION);
				if (val != JOptionPane.YES_OPTION)
					return -1;
			}
			writeToFile(file);
			setFileName(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return JFileChooser.APPROVE_OPTION;
	}

	public int save() {

		if (isFileUnnamed) {
			return(saveasaction());
		} else {
			writeToFile(file);
			// TODO: handle more elegantly
			if (fileChanged)
				setTitle(getTitle().substring(1));
			return JFileChooser.APPROVE_OPTION;
		}
	}

	public void writeToFile(File file) {
		try {
			// TODO: get a grip on the style to use
			fileChanged = false;
			BufferedWriter outFile = new BufferedWriter( new FileWriter( file ) );
			outFile.write( textArea.getText( ) ); //put in textfile
			outFile.flush( ); // redundant, done by close()
			outFile.close( );
			// TODO: at least printStackTrace
		} catch (Exception e) {}
	}

	public static String getExtension(String fileName) {
		int dot = fileName.lastIndexOf(".");
		return dot < 0 ?  "" : fileName.substring(dot);
	}

	private void setLanguageByExtension(String extension) {
		Languages.Language info = Languages.getInstance().get(extension);

		// TODO: these should go to upstream RSyntaxTextArea
		if (extension.equals(".clj"))
			getDocument().setSyntaxStyle(new ClojureTokenMaker());
		else if (extension.equals(".m"))
			getDocument().setSyntaxStyle(new MatlabTokenMaker());
		else
			textArea.setSyntaxEditingStyle(info.syntaxStyle);
		provider.setProviderLanguage(info.menuLabel);

		info.item.setSelected(true);
	}

	public void setFileName(File file) {
		isFileUnnamed = false;
		title = file.getName();
		setTitle(title);
		setLanguageByExtension(getExtension(title));
	}

	public void runScript() {
		if (fileChanged || isFileUnnamed) {
			int val = JOptionPane.showConfirmDialog(this, "You must save the changes before running.Do you want to save changes??", "Select an Option", JOptionPane.YES_NO_OPTION);
			if (val != JOptionPane.YES_OPTION)
				return;
			else {
				int temp = save();
				if (temp != JFileChooser.APPROVE_OPTION)
					return;
			}
		}
		runSavedScript();
	}

	// TODO: do not require saving
	public void runSavedScript() {
		String ext = getExtension(file.getName());
		final RefreshScripts interpreter =
		        Languages.getInstance().get(ext).interpreter;
		if (interpreter == null)
			IJ.error("There is no interpreter for " + ext
			         + " files!");
		else {
			new Thread() {
				public void run() {
					interpreter.runScript(file.getPath());
				}
			}.start();
		}
	}

	// TODO: saveChangeDialog() should check fileChanged itself
	public void windowClosing(WindowEvent e) {

		if (fileChanged) {
			int val = saveChangeDialog();
			if (val == JOptionPane.CANCEL_OPTION) {
				setVisible(true);
				return;
			}
			if (val == JOptionPane.YES_OPTION) {
				if (save() != JFileChooser.APPROVE_OPTION) {
					return;
				}
			}
		}
		this.dispose();

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
		if (!fileChanged)
			setTitle("*" + getTitle());
		fileChanged = true;
	}

	private CompletionProvider createCompletionProvider() {
		// TODO: why the member variable?
		provider = new ClassCompletionProvider(new DefaultProvider(), textArea, language);
		return provider;

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
