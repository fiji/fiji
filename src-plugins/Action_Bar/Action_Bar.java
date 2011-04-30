import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.macro.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import javax.swing.*;

/**
 * @author jerome.mutterer(at)ibmp.fr
 *
 */

public class Action_Bar implements PlugIn, ActionListener {

	// private static final long serialVersionUID = 7436194992984622141L;
	String macrodir = IJ.getDirectory("macros");
	String name, title, path;
	String startupAction = "";
	String codeLibrary = "";
	String separator = System.getProperty("file.separator");
	JFrame frame = new JFrame();
	Frame frontframe;
	int xfw = 0;
	int yfw = 0;
	int wfw = 0;
	int hfw = 0;
	JToolBar toolBar = null;
	boolean tbOpenned = false;
	boolean grid = true;
	boolean visible = true;
	boolean shouldExit = false;
	JButton button = null;
	private boolean isPopup = false;
	private boolean isSticky = false;
	int nButtons = 0;

	protected static String ijHome = IJ.versionLessThan("1.43d") ?
		IJ.getDirectory("plugins") + "../" :
		IJ.getDirectory("imagej");

	public void run(String s) {
		// s used if called from another plugin, or from an installed command.
		// arg used when called from a run("command", arg) macro function
		// if both are empty, we choose to run the assistant "createAB.txt"

		String arg = Macro.getOptions();

		if (arg == null && s.equals("")) {
			try {
				runMacro("createAB.txt");
				return;
			} catch (Exception e) {
				IJ.error("createAB.txt file not found");
			}

		} else if (arg == null) { // call from an installed command
			path = getURL(s);
			try {
				name = path.substring(path.lastIndexOf("/") + 1);
			} catch (Exception e) {
			}
		} else { // called from a macro by run("Action Bar",arg)
			path = getURL(arg.trim());
			try {
				if (path.endsWith(".txt"))
					path = path.substring(0, path.indexOf(".txt") + 4);
				name = path.substring(path.lastIndexOf("/") + 1);
			} catch (Exception e) {
			}
		}

		// title = name.substring(0, name.indexOf("."));
		title = name.substring(0, name.indexOf(".")).replaceAll("_", " ")
		.trim();
		frame.setTitle(title);

		if (WindowManager.getFrame(title) != null) {
			WindowManager.getFrame(title).toFront();
			return;
		}
		// this listener will save the bar's position and close it.
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				rememberXYlocation();
				e.getWindow().dispose();
			}

			public void windowClosed(WindowEvent e) {
				WindowManager.removeWindow((Frame) frame);
			}
		});
		frontframe = WindowManager.getFrontWindow();
		if (frontframe != null){
		xfw = frontframe.getLocation().x;
		yfw = frontframe.getLocation().y;
		wfw = frontframe.getWidth();
		hfw = frontframe.getHeight();
		}
		// toolbars will be added as lines in a n(0) rows 1 column layout
		frame.getContentPane().setLayout(new GridLayout(0, 1));

		// sets the bar's default icon to imagej icon
		frame.setIconImage(IJ.getInstance().getIconImage());

		// read the config file, and add toolbars to the frame
		designPanel();

		// captures the ImageJ KeyListener
		frame.setFocusable(true);
		frame.addKeyListener(IJ.getInstance());

		// setup the frame, and display it
		frame.setResizable(false);

		if (!isPopup) {
			frame.setLocation((int) Prefs
					.get("actionbar" + title + ".xloc", 10), (int) Prefs.get(
							"actionbar" + title + ".yloc", 10));
			WindowManager.addWindow(frame);
		}

		else {
			frame.setLocation(MouseInfo.getPointerInfo().getLocation());
			frame.setUndecorated(true);
			frame.addKeyListener(new KeyListener() {
				public void keyReleased(KeyEvent e) {
				}

				public void keyTyped(KeyEvent e) {
				}

				public void keyPressed(KeyEvent e) {
					int code = e.getKeyCode();
					if (code == KeyEvent.VK_ESCAPE) {
						frame.dispose();
						WindowManager.removeWindow(frame);
					}
				}
			});
		}

		if (isSticky) {
			frame.setUndecorated(true);
		}
		frame.pack();
		frame.setVisible(true);
		if (startupAction != "")
			try {
				new MacroRunner(startupAction + "\n" + codeLibrary);
			} catch (Exception fe) {
			}

			WindowManager.setWindow(frontframe);

			if (isSticky) {
				stickToActiveWindow();
				while ((shouldExit==false)&& (frame.getTitle()!="xxxx")){
					try {

						ImageWindow fw = WindowManager.getCurrentWindow();
						if (fw == null)
							frame.setVisible(false);
						if ((fw != null) && (fw.getLocation().x != xfw)
								|| (fw.getLocation().y != yfw)
								|| (fw.getWidth() != wfw)
								|| (fw.getHeight() != hfw)) {
							xfw = fw.getLocation().x;
							yfw = fw.getLocation().y;
							wfw = fw.getWidth();
							hfw = fw.getHeight();
							stickToActiveWindow();
						}
					} catch (Exception e) {
						// TODO: handle exception
					}
					IJ.wait(20);
				}
				if (frame.getTitle()=="xxxx") closeActionBar();
				if ((shouldExit)) return;
			}

	}

	protected String getURL(String path) {
		if (path.startsWith("/")) {
			String fullPath = ijHome + path;
			if (new File(fullPath).exists())
				return fullPath;
			if (path.startsWith("/plugins/"))
				path = path.substring(8);
		}
		URL url = getClass().getResource(path);
		if (url == null) {
			url = getClass().getResource("/ActionBar/" + path);
			if (url == null)
				throw new RuntimeException("Cannot find resource '" + path + "'");
		}
		return url.toString();
	}

	protected void runMacro(String path) {
		String url = getURL(path);
		if (url.startsWith("jar:")) try {
			String macro = readString(new URL(url).openStream());
			new MacroRunner(macro);
			return;
		} catch (Exception e) {
			IJ.handleException(e);
		}
		else {
			File macro = new File(url);
			new MacroRunner(macro);
		}
	}

	protected String readString(InputStream in) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuffer buffer = new StringBuffer();
		char[] buf = new char[16384];
		try {
			for (;;) {
				int count = reader.read(buf);
				if (count < 0)
					break;
				buffer.append(buf, 0, count);
			}
			reader.close();
		} catch (IOException e) {
			IJ.handleException(e);
		}
		return buffer.toString();
	}

	private void stickToActiveWindow() {
		ImageWindow fw = WindowManager.getCurrentWindow();
		try {
			if (fw != null) {
				if (!frame.isVisible())
					frame.setVisible(true);
				frame.toFront();
				frame.setLocation(fw.getLocation().x + fw.getWidth(), fw
						.getLocation().y);
				fw.toFront();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	private void closeActionBar() {
		frame.dispose();
		WindowManager.removeWindow(frame);
		WindowManager.setWindow(frontframe);
		shouldExit = true;
	}

	private void designPanel() {
		try {
			Reader reader;
			if (path.startsWith("jar:"))
				reader = new InputStreamReader(new URL(path).openStream());
			else {
				File file = new File(path);
				if (!file.exists()) {
					frame.dispose();
					IJ.error("Config File not found");
				}
				reader = new FileReader(file);
			}
			BufferedReader r = new BufferedReader(reader);
			while (true) {
				String s = r.readLine();
				if (s.equals(null)) {
					r.close();
					closeToolBar();
					break;
				} else if (s.startsWith("<main>")) {
					setABasMain();
					hideIJ();
				} else if (s.startsWith("<popup>")) {
					isPopup = true;
				} else if (s.startsWith("<sticky>")) {
					isSticky = true;
				} else if (s.startsWith("<DnD>")) {
					setABDnD();
				} else if (s.startsWith("<onTop>")) {
					setABonTop();
				} else if (s.startsWith("<startupAction>")) {
					String code = "";
					while (true) {
						String sc = r.readLine();
						if (sc.equals(null)) {
							break;
						}
						if (!sc.startsWith("</startupAction>")) {
							code = code + "" + sc;
						} else {
							startupAction = code;
							break;
						}
					}
				} else if (s.startsWith("<codeLibrary>")) {
					String code = "";
					while (true) {
						String sc = r.readLine();
						if (sc.equals(null)) {
							break;
						}
						if (!sc.startsWith("</codeLibrary>")) {
							code = code + "" + sc;
						} else {
							codeLibrary = code;
							break;
						}
					}
				} else if (s.startsWith("<icon>")) {
					String frameiconName = r.readLine().substring(5);
					setABIcon(frameiconName);
				} else if (s.startsWith("<noGrid>") && tbOpenned == false) {
					grid = false;
				} else if (s.startsWith("<text>") && tbOpenned == false) {
					frame.getContentPane().add(new JLabel(s.substring(6)));
				} else if (s.startsWith("<line>") && tbOpenned == false) {
					toolBar = new JToolBar();
					nButtons = 0;
					tbOpenned = true;
				} else if (s.startsWith("</line>") && tbOpenned == true) {
					closeToolBar();
					tbOpenned = false;
				} else if (s.startsWith("<separator>") && tbOpenned == true) {
					toolBar.addSeparator();
				} else if (s.startsWith("<button>") && tbOpenned == true) {
					String label = r.readLine().substring(6);
					String icon = r.readLine().substring(5);
					String arg = r.readLine().substring(4);
					if (arg.startsWith("<macro>")) {
						String code = "";
						while (true) {
							String sc = r.readLine();
							if (sc.equals(null)) {
								break;
							}
							if (!sc.startsWith("</macro>")) {
								code = code + "" + sc;
							} else {
								arg = code;
								break;
							}
						}
					} else if (arg.startsWith("<tool>")) {
						String code = "<tool>\n";
						while (true) {
							String sc = r.readLine();
							if (sc.equals(null)) {
								break;
							}
							if (!sc.startsWith("</tool>")) {
								code = code + "" + sc;
							} else {
								arg = code;
								break;
							}
						}
					} else if (arg.startsWith("<hide>")) {
						arg = "<hide>";
					} else if (arg.startsWith("<close>")) {
						arg = "<close>";
					}
					button = makeNavigationButton(icon, arg, label, label);
					toolBar.add(button);
					nButtons++;
				}
			}
			r.close();
		} catch (Exception e) {
		}
	}

	private void closeToolBar() {
		toolBar.setFloatable(false);
		if (grid)
			toolBar.setLayout(new GridLayout(1, nButtons));
		frame.getContentPane().add(toolBar);
		tbOpenned = false;
	}

	protected JButton makeNavigationButton(String imageName,
			String actionCommand, String toolTipText, String altText) {

		String imgLocation = "icons/" + imageName;
		URL imageURL = Action_Bar.class.getResource(imgLocation);
		JButton button = new JButton();
		button.setActionCommand(actionCommand);
		button.setMargin(new Insets(2, 2, 2, 2));
		// button.setBorderPainted(true);
		button.addActionListener(this);
		button.setFocusable(true);
		button.addKeyListener(IJ.getInstance());
		if (imageURL != null) {
			button.setIcon(new ImageIcon(imageURL, altText));
			button.setToolTipText(toolTipText);
		} else {
			button.setText(altText);
			button.setToolTipText(toolTipText);
		}
		return button;
	}

	public void actionPerformed(ActionEvent e) {

		String cmd = e.getActionCommand();
		if (((e.getModifiers() & e.ALT_MASK) * ((e.getModifiers() & e.CTRL_MASK))) != 0) {
			IJ.run("Edit...", "open=[" + path + "]");
			return;
		}
		if (((e.getModifiers() & e.ALT_MASK)) != 0) {
			closeActionBar();
			return;
		}
		if (cmd.startsWith("<hide>")) {
			toggleIJ();
		} else if (cmd.startsWith("<close>")) {
			closeActionBar();
			return;
		} else if (cmd.startsWith("<tool>")) {
			// install this tool and select it
			String tool = cmd.substring(6, cmd.length());
			String toolname = ((JButton) e.getSource()).getToolTipText();
			tool = "macro '" + toolname + " Tool - C000'{\n" + tool + "\n}\n"
			+ codeLibrary;
			new MacroInstaller().install(tool);
			Toolbar.getInstance().setTool(10);
			IJ.showStatus(toolname + " Tool installed");

		} else {
			try {
				new MacroRunner(cmd + "\n" + codeLibrary);
			} catch (Exception fe) {
				IJ.error("Error in macro command");
			}
		}
		frame.repaint();
		if (isPopup) {
			frame.dispose();
			WindowManager.removeWindow(frame);
			WindowManager.setWindow(frontframe);
		}

	}

	private void toggleIJ() {
		IJ.getInstance().setVisible(!IJ.getInstance().isVisible());
		visible = IJ.getInstance().isVisible();
	}

	private void hideIJ() {
		IJ.getInstance().setVisible(false);
		visible = false;
	}

	protected void rememberXYlocation() {
		Prefs.set("actionbar" + title + ".xloc", frame.getLocation().x);
		Prefs.set("actionbar" + title + ".yloc", frame.getLocation().y);
	}

	private void setABasMain() {
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				rememberXYlocation();
				e.getWindow().dispose();
				IJ.run("Quit");
			}
		});
	}

	private void setABDnD() {
		DropTarget dt = new DropTarget(frame, IJ.getInstance().getDropTarget());
	}

	private void setABIcon(String s) {
		String imgLocation = "icons/" + s;
		try {
			URL imageURL = Action_Bar.class.getResource(imgLocation);
			frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(
					imageURL));
		} catch (Exception fe) {
			IJ.error("Error creating the bar's icon");
		}
	}

	private void setABonTop() {
		frame.setAlwaysOnTop(true);
	}

	public static void callFunctionFinder() {
		new FunctionFinder();
	}

	public static void pasteIntoEditor(String text) {
		Component component = getTextArea();
		if (component == null)
			return;
		if (component instanceof TextArea) {
			TextArea textArea = (TextArea)component;
			int begin = textArea.getSelectionStart();
			int end = textArea.getSelectionEnd();
			int pos;
			if (begin < 0) {
				pos = textArea.getCaretPosition();
				textArea.insert(text, pos);
				pos += text.length();
			}
			else {
				try {
					textArea.replaceRange(text, begin, end);
					pos = begin + text.length();
				}
				catch (Exception e2) {
					return;
				}
			}
			textArea.setCaretPosition(pos);
		}
		else if (component instanceof JTextArea) {
			JTextArea textArea = (JTextArea)component;
			int begin = textArea.getSelectionStart();
			int end = textArea.getSelectionEnd();
			int pos;
			if (begin < 0) {
				pos = textArea.getCaretPosition();
				textArea.insert(text, pos);
				pos += text.length();
			}
			else {
				try {
					textArea.replaceRange(text, begin, end);
					pos = begin + text.length();
				}
				catch (Exception e2) {
					return;
				}
			}
			textArea.setCaretPosition(pos);
		}
	}

	protected static Component getTextArea() {
		Frame front = WindowManager.getFrontWindow();
		Component result = getTextArea(front);
		if (result != null)
			return result;

		// look at the other frames
		Frame[] frames = WindowManager.getNonImageWindows();
		for (int i = frames.length - 1; i >= 0; i--) {
			result = getTextArea(frames[i]);
			if (result != null)
				return result;
		}
		return null;
	}

	protected static Component getTextArea(Container container) {
		return getTextArea(container, 6);
	}

	protected static Component getTextArea(Container container, int maxDepth) {
		if (container == null)
			return null;
		for (Component component : container.getComponents()) {
			if ((component instanceof TextArea || component instanceof JTextArea) && component.isVisible())
				return component;
			if (maxDepth > 0 && (component instanceof Container)) {
				Component result = getTextArea((Container)component);
				if (result != null)
					return result;
			}
		}
		return null;
	}
}