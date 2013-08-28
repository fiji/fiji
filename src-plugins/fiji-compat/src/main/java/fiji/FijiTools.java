package fiji;

import ij.IJ;
import ij.Menus;
import imagej.legacy.LegacyExtensions;

import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuContainer;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class FijiTools {

	private static Field menuEntry2jarFile;
	private static Field menuInstance;

	/**
	 * Get the path of the Fiji directory
	 *
	 * @Deprecated
	 */
	public static String getFijiDir() {
		return getImageJDir();
	}

	public static String getImageJDir() {
		String path = System.getProperty("ij.dir");
		if (path != null)
			return path;
		final String prefix = "file:";
		final String suffix = "/jars/fiji-compat.jar!/fiji/FijiTools.class";
		path = FijiTools.class
			.getResource("FijiTools.class").getPath();
		if (path.startsWith(prefix))
			path = path.substring(prefix.length());
		if (path.endsWith(suffix))
			path = path.substring(0,
				path.length() - suffix.length());
		return path;
	}

	public static boolean isFijiDeveloper() {
		try {
			return new File(getImageJDir(), "ImageJ.c").exists();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean openStartupMacros() {
		File macros = new File(getFijiDir(), "macros");
		File txt = new File(macros, "StartupMacros.txt");
		File ijm = new File(macros, "StartupMacros.ijm");
		File fiji = new File(macros, "StartupMacros.fiji.ijm");
		if (txt.exists()) {
			if (openEditor(txt, fiji))
				return true;
		}
		else if (ijm.exists() || fiji.exists()) {
			if (openEditor(ijm, fiji))
				return true;
		}
		return false;
	}

	public static boolean openEditor(File file, File templateFile) {
		try {
			Class<?> clazz = IJ.getClassLoader().loadClass("fiji.scripting.TextEditor");
			Constructor<?> ctor = clazz.getConstructor(new Class[] { File.class, File.class });
			Frame frame = (Frame)ctor.newInstance(new Object[] { file, templateFile });
			frame.setVisible(true);
			return true;
		} catch (Exception e) {
			IJ.handleException(e);
		}
		return false;
	}

	public static boolean openFijiEditor(String title, String body) {
		try {
			Class<?> textEditor = ij.IJ.getClassLoader().loadClass("fiji.scripting.TextEditor");
			Constructor<?> ctor = textEditor.getConstructor(String.class, String.class);
			Frame frame = (Frame)ctor.newInstance(title, body);
			if (frame == null) return false;
			frame.setVisible(true);
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}

	public static boolean openIJ1Editor(String title, String body) {
		try {
			Class<?> clazz = IJ.getClassLoader().loadClass("ij.plugin.frame.Editor");
			Constructor<?> ctor = clazz.getConstructor(new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE });
			Object ed = ctor.newInstance(new Object[] { 16, 60, 0, 3 });
			Method method = clazz.getMethod(title.endsWith(".ijm") ? "createMacro" : "create", new Class[] { String.class, String.class });
			method.invoke(ed, new Object[] { title, body });
			return true;
		} catch (Exception e) {
			IJ.handleException(e);
		}

		return false;
	}

	public static boolean openEditor(String title, String body) {
		if (openFijiEditor(title, body)) return true;
		return openIJ1Editor(title, body);
	}

	/**
	 * Calls the Fiji Script Editor for text files.
	 * 
	 * A couple of sanity checks are needed, e.g. that the script editor is in the class path
	 * and that it agrees that the file is binary, that there is no infinite loop ponging back
	 * and forth between the TextEditor's and the Opener's open() methods.
	 * 
	 * @param path the path to the candidate file
	 * @return whether we opened it in the script editor
	 */
	public static boolean maybeOpenEditor(String path) {
		try {
			Class<?> textEditor = ij.IJ.getClassLoader().loadClass("fiji.scripting.TextEditor");
			if (path.indexOf("://") < 0 &&
					!getFileExtension(path).equals("") &&
					!((Boolean)textEditor.getMethod("isBinary", new Class[] { String.class }).invoke(null, path)).booleanValue() &&
					!stackTraceContains("fiji.scripting.TextEditor.open(") &&
					IJ.runPlugIn("fiji.scripting.Script_Editor", path) != null)
				return true;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}

	public static boolean openFijiEditor(final File file) {
		try {
			Class<?> textEditor = ij.IJ.getClassLoader().loadClass("fiji.scripting.TextEditor");
			Constructor<?> ctor = textEditor.getConstructor(String.class);
			Frame frame = (Frame)ctor.newInstance(file.getAbsolutePath());
			if (frame == null) return false;
			frame.setVisible(true);
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}

	public static String getFileExtension(String path) {
		int dot = path.lastIndexOf('.');
		if (dot < 0)
			return "";
		int slash = path.lastIndexOf('/');
		int backslash = path.lastIndexOf('\\');
		if (dot < slash || dot < backslash)
			return "";
		return path.substring(dot + 1);
	}

	public static boolean stackTraceContains(String needle) {
		final StringWriter writer = new StringWriter();
		final PrintWriter out = new PrintWriter(writer);
		new Exception().printStackTrace(out);
		out.close();
		return writer.toString().indexOf(needle) >= 0;
	}

	@Deprecated
	public static boolean handleNoSuchMethodError(NoSuchMethodError error) {
		return LegacyExtensions.handleNoSuchMethodError(error);
	}

	/**
	 * Get the MenuItem instance for a given menu path
	 *
	 * @param menuPath the menu path, e.g. File>New>Bio-Formats
	 */
	public static MenuItem getMenuItem(String menuPath) {
		return getMenuItem(Menus.getMenuBar(), menuPath, false);
	}

	/**
	 * Get the MenuItem instance for a given menu path
	 *
	 * If the menu item was not found, create a {@link Menu} for the given path.
	 *
	 * @param container an instance of {@link MenuBar} or {@link Menu}
	 * @param menuPath the menu path, e.g. File>New>Bio-Formats
	 * @param createMenuIfNecessary if the menu item was not found, create a menu
	 */
	public static MenuItem getMenuItem(MenuContainer container,
			String menuPath, boolean createMenuIfNecessary) {
		String name;
		MenuBar menuBar = (container instanceof MenuBar) ?
			(MenuBar)container : null;
		Menu menu = (container instanceof Menu) ?
			(Menu)container : null;
		while (menuPath.endsWith(">"))
			menuPath = menuPath.substring(0, menuPath.length() - 1);
		while (menuPath != null && menuPath.length() > 0) {
			int croc = menuPath.indexOf('>');
			if (croc < 0) {
				name = menuPath;
				menuPath = null;
			}
			else {
				name = menuPath.substring(0, croc);
				menuPath = menuPath.substring(croc + 1);
			}
			MenuItem current = getMenuItem(menuBar, menu, name,
				createMenuIfNecessary);
			if (current == null || menuPath == null)
				return current;
			menuBar = null;
			menu = (Menu)current;
		}
		return null;
	}

	/*
	 * Get the item with the given name either from the menuBar, or if
	 * that is null, from the menu.
	 */
	protected static MenuItem getMenuItem(MenuBar menuBar, Menu menu,
			String name, boolean createIfNecessary) {
		if (menuBar == null && menu == null)
			return null;
		if (menuBar != null && name.equals("Help")) {
			menu = menuBar.getHelpMenu();
			if (menu == null && createIfNecessary) {
				menu = new PopupMenu("Help");
				menuBar.setHelpMenu(menu);
			}
			return menu;
		}

		int count = menuBar != null ?
			menuBar.getMenuCount() : menu.getItemCount();
		for (int i = 0; i < count; i++) {
			MenuItem current = menuBar != null ?
				menuBar.getMenu(i) : menu.getItem(i);
			if (name.equals(current.getLabel()))
				return current;
		}

		if (createIfNecessary) {
			Menu newMenu = new PopupMenu(name);
			if (menuBar != null)
				menuBar.add(newMenu);
			else
				menu.add(newMenu);
			return newMenu;
		}
		else
			return null;
	}

	/**
	 * Install a single menu item
	 *
	 * @param menuPath the menu into which to install it
	 * @param name the label of the menu item
	 * @param command the command to run (as per the plugins.config)
	 * @return the added menu item
	 */
	public static MenuItem installPlugin(String menuPath, String name,
			String command) {
		return installPlugin(menuPath, name, command, null);
	}

	/**
	 * Install a single menu item
	 *
	 * @param menuPath the menu into which to install it
	 * @param name the label of the menu item
	 * @param command the command to run (as per the plugins.config)
	 * @param file the source file
	 * @return the added menu item
	 */
	/* TODO: sorted */
	@SuppressWarnings("unchecked")
	public static MenuItem installPlugin(String menuPath, String name,
			String command, File jarFile) {
		if (Menus.getCommands().get(name) != null) {
			IJ.log("The user plugin " + name
				+ (jarFile == null ? "" : " (in " + jarFile + ")")
				+ " would override an existing command!");
			return null;
		}

		MenuItem item = null;
		if (IJ.getInstance() != null) {
			Menu menu = getMenu(menuPath);
			item = new MenuItem(name);
			menu.add(item);
			item.addActionListener(IJ.getInstance());
		}
		Menus.getCommands().put(name, command);

		if (jarFile != null) {
			if (menuEntry2jarFile == null) try {
				final Field instanceField = Menus.class.getDeclaredField("instance");
				instanceField.setAccessible(true);
				final Field field = Menus.class.getDeclaredField("menuEntry2jarFile");
				field.setAccessible(true);
				menuInstance = instanceField;
				menuEntry2jarFile = field;
			} catch (Throwable t) {
				// be nice to ImageJ older than 1.43h
//					if (IJ.debug)
					t.printStackTrace();
			}

			if (menuEntry2jarFile != null) try {
				Map<String, String> map = (Map<String, String>) menuEntry2jarFile.get(menuInstance.get(null));
				map.put(name, jarFile.getPath());
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		return item;
	}

	public static Menu getMenu(String menuPath) {
		return (Menu)getMenuItem(Menus.getMenuBar(), menuPath, true);
	}

}
