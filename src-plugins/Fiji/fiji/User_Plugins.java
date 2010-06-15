package fiji;

import ij.IJ;
import ij.ImageJ;
import ij.Menus;

import ij.plugin.PlugIn;

import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuContainer;
import java.awt.MenuItem;
import java.awt.PopupMenu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/*
 * This plugin looks through all files in a given directory (default:
 * $ROOT/user-plugins/, where $ROOT is the parent directory of jars/Fiji.jar)
 * and inserts the found plugins into a given menu (default: Plugins>User).
 */
public class User_Plugins implements PlugIn {
	public String path, menuPath;

	public User_Plugins() {
		this(getDefaultPath(), getDefaultMenuPath());
	}

	public User_Plugins(String path, String menuPath) {
		this.path = path;
		if (menuPath.endsWith(">"))
			menuPath = menuPath.substring(0, menuPath.length() - 1);
		this.menuPath = menuPath;
	}

	public void run(String arg) {
		if ("update".equals(arg))
			Menus.updateImageJMenus();
		FijiClassLoader classLoader = new FijiClassLoader(true);
		try {
			classLoader.addPath(path);
		} catch (IOException e) {}

		try {
			// IJ.setClassLoader(classLoader);
			Class ij = Class.forName("ij.IJ");
			java.lang.reflect.Method method =
				ij.getDeclaredMethod("setClassLoader",
					new Class[] { ClassLoader.class });
			method.setAccessible(true);
			method.invoke(null, new Object[] { classLoader });
		} catch (Exception e) { e.printStackTrace(); }

		installScripts();
		installPlugins(path, ".", menuPath);
		/* make sure "Update Menus" runs _this_ plugin */
		Menus.getCommands().put("Update Menus",
			"fiji.User_Plugins(\"update\")");
		Menus.getCommands().put("Refresh Menus",
			"fiji.User_Plugins(\"update\")");
		if (IJ.getInstance() != null) {
			Menu help = Menus.getMenuBar().getHelpMenu();
			for (int i = help.getItemCount() - 1; i >= 0; i--) {
				MenuItem item = help.getItem(i);
				String name = item.getLabel();
				if (name.equals("Update Menus"))
					item.setLabel("Refresh Menus");
			}
		}

		// make sure "Edit>Options>Memory & Threads runs Fiji's plugin
		Menus.getCommands().put("Memory & Threads...", "fiji.Memory");

		SampleImageLoader.install();
		Main.installRecentCommands();
	}

	public static void install() {
		new User_Plugins().run(null);
	}

	public static void runPlugIn(String menuLabel) {
		String className = (String)Menus.getCommands().get(menuLabel);
		if (className != null)
			IJ.runPlugIn(className, null);
	}

	public static void installScripts() {
		if (System.getProperty("jnlp") != null)
			return;
		runPlugIn("Refresh Javas");
		String[] languages = {
			"Jython", "JRuby", "Clojure", "BSH", "Javascript"
		};
		for (int i = 0; i < languages.length; i++)
			runPlugIn("Refresh " + languages[i] + " Scripts");
	}

	public void installPlugins(String dir, String name, String menuPath) {
		File file = new File(dir, name);
		if (file.isDirectory()) {
			if (!name.equals("."))
				menuPath = menuPath + ">" + name;
			dir = file.getPath();
			String[] list = file.list();
			Arrays.sort(list);
			for (int i = 0; i < list.length; i++)
				installPlugins(dir, list[i], menuPath);
		}
		else if (name.endsWith(".class")) {
			name = name.substring(0, name.length() - 5);
			installPlugin(menuPath, makeLabel(name), name);
		}
		else if (name.endsWith(".jar")) try {
			List plugins = getJarPluginList(file, menuPath);
			Iterator iter = plugins.iterator();
			while (iter.hasNext()) {
				String[] item = (String[])iter.next();
				if (item[1].equals("-"))
					getMenu(item[0]).addSeparator();
				else
					installPlugin(item[0], item[1],
							item[2]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List getJarPluginList(File jarFile, String menuPath)
			throws IOException {
		List result = new ArrayList();
		JarFile jar = new JarFile(jarFile);
		Enumeration entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = (JarEntry)entries.nextElement();
			String name = entry.getName();
			if (name.endsWith("plugins.config"))
				return parsePluginsConfig(jar
					.getInputStream(entry), menuPath);
			if (name.indexOf('_') < 0)
				continue;
			String[] item = new String[3];
			item[0] = menuPath;
			item[1] = makeLabel(name);
			item[2] = name;
			result.add(item);
		}
		return result;
	}

	protected List parsePluginsConfig(InputStream in, String menuPath)
			throws IOException {
		List result = new ArrayList();
		BufferedReader reader =
			new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#"))
				continue;
			int quote = line.indexOf('"'),
				quote2 = line.indexOf('"', quote + 1);
			if (quote2 < 0)
				continue;
			int comma = line.indexOf(',');
			if (comma < 0)
				comma = quote;
			String[] item = new String[3];
			item[0] = makeMenuPath(line.substring(0, comma).trim(),
				menuPath);
			item[1] = line.substring(quote + 1, quote2);
			item[2] = line.substring(quote2 + 1).trim();
			if (item[2].startsWith(","))
				item[2] = item[2].substring(1).trim();
			result.add(item);
		}
		in.close();
		return result;
	}


	/* menu stuff */

	protected static String makeLabel(String className) {
		return className.replace('_', ' ');
	}

	protected static String makeMenuPath(String original, String menuPath) {
		if (original.equals("Plugins"))
			return menuPath;
		if (original.startsWith("Plugins>"))
			original = original.substring(8);
		if (original.equals(""))
			return menuPath;
		return menuPath + ">" + original;
	}

	/* TODO: sorted */
	protected void installPlugin(String menuPath, String name,
			String command) {
		if (Menus.getCommands().get(name) != null) {
			IJ.log("The user plugin " + name
				+ " would override an existing command!");
			return;
		}

		int croc = menuPath.lastIndexOf('>');
		Menu menu = getMenu(menuPath);
		MenuItem item = new MenuItem(name);
		menu.add(item);
		item.addActionListener(IJ.getInstance());
		Menus.getCommands().put(name, command);
	}

	protected static Menu getMenu(String menuPath) {
		return (Menu)getMenuItem(Menus.getMenuBar(), menuPath, true);
	}

	public static MenuItem getMenuItem(String menuPath) {
		return getMenuItem(Menus.getMenuBar(), menuPath, false);
	}

	public static MenuItem getMenuItem(MenuContainer container,
			String menuPath, boolean createIfNecessary) {
		String name;
		MenuBar menuBar = (container instanceof MenuBar) ?
			(MenuBar)container : null;
		Menu menu = (container instanceof Menu) ?
			(Menu)container : null;
		while (menuPath != null) {
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
				createIfNecessary);
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


	/* defaults */

	public static String getDefaultPath() {
		try {
			return FijiTools.getFijiDir() + "/user-plugins";
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String getDefaultMenuPath() {
		return "Plugins>User";
	}
}
