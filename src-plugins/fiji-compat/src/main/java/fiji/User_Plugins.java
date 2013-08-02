package fiji;

import ij.Menus;
import ij.plugin.PlugIn;
import imagej.legacy.CodeHacker;
import imagej.legacy.LegacyInjector;

import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuContainer;
import java.awt.MenuItem;
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

/**
 * A class to find user plugins, i.e. plugins not inside Fiji.app/plugins/
 * 
 * This plugin looks through all files in a given directory (default:
 * $ROOT/user-plugins/, where $ROOT is the parent directory of
 * jars/ij-launcher.jar) and inserts the found plugins into a given menu
 * (default: Plugins>User).
 * 
 * @deprecated superseded by ImageJ2's {@link LegacyInjector} (which calls
 *             {@link CodeHacker#addExtraPlugins()}.)
 *
 * @author Johannes Schindelin
 */
public class User_Plugins implements PlugIn {

	private String path, menuPath;
	private boolean stripPluginsPrefix;

	/**
	 * Default constructor
	 */
	public User_Plugins() {
		this(true);
	}

	/**
	 * Construct an instance which looks in the default places and strips the plugin prefix
	 *
	 * @param stripPluginsPrefix whether to delete "Plugins>" from the original plugin paths
	 */
	public User_Plugins(boolean stripPluginsPrefix) {
		this(getDefaultPath(), getDefaultMenuPath(), stripPluginsPrefix);
	}

	/**
	 * Construct an instance that looks in an arbitrary place
	 *
	 * @param path the top directory being searched
	 * @param menuPath the menu into which the plugins will be installed
	 * @param stripPluginsPrefix whether to delete "Plugins>" from the original plugin paths
	 */
	public User_Plugins(String path, String menuPath, boolean stripPluginsPrefix) {
		this.path = path;
		if (menuPath.endsWith(">"))
			menuPath = menuPath.substring(0, menuPath.length() - 1);
		this.menuPath = menuPath;
		this.stripPluginsPrefix = stripPluginsPrefix;
	}

	/**
	 * Install the plugins now
	 */
	public void run(String arg) {
		if ("update".equals(arg)) {
			Menus.updateImageJMenus();
		}
		new MenuRefresher().run(arg);

		installPlugins(path, "", menuPath);
	}

	/**
	 * Install the plugins (default path, default menu)
	 * 
	 * @deprecated
	 */
	public static void install() {
		new User_Plugins().run(null);
	}

	/**
	 * Run the command associated with a menu label if there is one
	 *
	 * @param menuLabel the label of the menu item to run
	 * @deprecated Use {@link Main#runGently(String)} instead
	 */
	public static void runPlugIn(String menuLabel) {
		Main.runGently(menuLabel);
	}

	/**
	 * Install the scripts in Fiji.app/plugins/
	 * @deprecated Use {@link MenuRefresher#installScripts()} instead
	 */
	public static void installScripts() {
		MenuRefresher.installScripts();
	}

	/**
	 * Install one or more plugins
	 *
	 * @param dir the directory where to look
	 * @param name the name of a file or directory
	 * @param menuPath the menu into which to put the discovered plugins
	 */
	public void installPlugins(String dir, String name, String menuPath) {
		File file = new File(dir, name);
		if (file.isDirectory()) {
			if (!name.equals(".") && !name.equals(""))
				menuPath = ("".equals(menuPath) ? "" : menuPath + ">") + name;
			dir = file.getPath();
			String[] list = file.list();
			Arrays.sort(list);
			for (int i = 0; i < list.length; i++)
				installPlugins(dir, list[i], menuPath);
		}
		else if (name.endsWith(".class")) {
			name = name.substring(0, name.length() - 5);
			FijiTools.installPlugin(menuPath, makeLabel(name), name, file);
		}
		else if (name.endsWith(".jar")) try {
			List<String[]> plugins = getJarPluginList(file, menuPath);
			Iterator<String[]> iter = plugins.iterator();
			while (iter.hasNext()) {
				String[] item = (String[])iter.next();
				if (item[1].equals("-"))
					FijiTools.getMenu(item[0]).addSeparator();
				else
					FijiTools.installPlugin(item[0], item[1],
							item[2], file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parse the plugins.config for a given .jar file
	 *
	 * If there is no plugins.config, this method lists all the classes whose
	 * file names have underscores , putting the menu items into the menu
	 * specified by a menu path.
	 *
	 * @param jarFile the .jar file
	 * @param menuPath the menu into which the discovered plugins are put
	 */
	public List<String[]> getJarPluginList(File jarFile, String menuPath)
			throws IOException {
		List<String[]> result = new ArrayList<String[]>();
		JarFile jar = new JarFile(jarFile);
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = (JarEntry)entries.nextElement();
			String name = entry.getName();
			if (name.endsWith("plugins.config"))
				return parsePluginsConfig(jar
					.getInputStream(entry), menuPath);
			if (name.indexOf('_') < 0 || name.indexOf('$') >= 0)
				continue;
			if (name.endsWith(".class"))
				name = name.substring(0, name.length() - 6).replace('/', '.');
			else
				continue;
			String[] item = new String[3];
			item[0] = menuPath;
			item[1] = makeLabel(name);
			item[2] = name;
			result.add(item);
		}
		return result;
	}

	protected List<String[]> parsePluginsConfig(InputStream in, String menuPath)
			throws IOException {
		List<String[]> result = new ArrayList<String[]>();
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

	protected String makeMenuPath(String original, String menuPath) {
		if (!stripPluginsPrefix)
			return original;
		if (original.equals("Plugins"))
			return menuPath;
		if (original.startsWith("Plugins>"))
			original = original.substring(8);
		if (menuPath.equals(""))
			return original.equals("") ? "Plugins" : original;
		if (original.equals(""))
			return menuPath;
		return menuPath + ">" + original;
	}

	/**
	 * Install a single menu item
	 *
	 * @param menuPath the menu into which to install it
	 * @param name the label of the menu item
	 * @param command the command to run (as per the plugins.config)
	 * @return the added menu item
	 * @deprecated Use {@link FijiTools#installPlugin(String,String,String)} instead
	 */
	public static MenuItem installPlugin(String menuPath, String name,
			String command) {
				return FijiTools.installPlugin(menuPath, name, command);
			}

	/**
	 * Install a single menu item
	 *
	 * @param menuPath the menu into which to install it
	 * @param name the label of the menu item
	 * @param command the command to run (as per the plugins.config)
	 * @param file the source file
	 * @return the added menu item
	 * @deprecated Use {@link FijiTools#installPlugin(String,String,String,File)} instead
	 */
	public static MenuItem installPlugin(String menuPath, String name,
			String command, File jarFile) {
				return FijiTools
						.installPlugin(menuPath, name, command, jarFile);
			}

	/**
	 * @deprecated Use {@link FijiTools#getMenu(String)} instead
	 */
	public static Menu getMenu(String menuPath) {
		return FijiTools.getMenu(menuPath);
	}

	/**
	 * Get the MenuItem instance for a given menu path
	 *
	 * @param menuPath the menu path, e.g. File>New>Bio-Formats
	 * @deprecated Use {@link FijiTools#getMenuItem(String)} instead
	 */
	public static MenuItem getMenuItem(String menuPath) {
		return FijiTools.getMenuItem(menuPath);
	}

	/**
	 * Get the MenuItem instance for a given menu path
	 *
	 * If the menu item was not found, create a {@link Menu} for the given path.
	 *
	 * @param container an instance of {@link MenuBar} or {@link Menu}
	 * @param menuPath the menu path, e.g. File>New>Bio-Formats
	 * @param createMenuIfNecessary if the menu item was not found, create a menu
	 * @deprecated Use {@link FijiTools#getMenuItem(MenuContainer,String,boolean)} instead
	 */
	public static MenuItem getMenuItem(MenuContainer container,
			String menuPath, boolean createMenuIfNecessary) {
				return FijiTools.getMenuItem(container, menuPath,
						createMenuIfNecessary);
			}

	/*
	 * Get the item with the given name either from the menuBar, or if
	 * that is null, from the menu.
	 */
	/**
	 * @deprecated Use {@link FijiTools#getMenuItem(MenuBar,Menu,String,boolean)} instead
	 */
	protected static MenuItem getMenuItem(MenuBar menuBar, Menu menu,
			String name, boolean createIfNecessary) {
				return FijiTools.getMenuItem(menuBar, menu, name,
						createIfNecessary);
			}


	/* defaults */

	/**
	 * Get the default path to the plugins searched outside Fiji.app
	 */
	public static String getDefaultPath() {
		try {
			return new File(System.getProperty("user.home"), ".plugins").getAbsolutePath();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Get the default menu path where the user plugins will be installed
	 */
	public static String getDefaultMenuPath() {
		return "";
	}
}
