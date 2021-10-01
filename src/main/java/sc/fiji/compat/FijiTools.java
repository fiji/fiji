/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2007 - 2021 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package sc.fiji.compat;

import ij.IJ;
import ij.Menus;
import ij.plugin.PlugIn;

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

import javax.swing.SwingUtilities;

import org.scijava.util.FileUtils;

public class FijiTools {

	private static Field menuEntry2jarFile;
	private static Field menuInstance;

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

	public static boolean openFijiEditor(final String title, final String body) {
		try {
			Class<?> textEditor = ij.IJ.getClassLoader().loadClass("fiji.scripting.TextEditor");
			final Constructor<?> ctor = textEditor.getConstructor(String.class, String.class);
			final Runnable run = new Runnable() {
				@Override
				public void run() {
					try {
						Frame frame = (Frame)ctor.newInstance(title, body);
						if (frame == null) Thread.currentThread().interrupt();
						frame.setVisible(true);
					} catch (Exception e) {
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}
				}
			};
			if (SwingUtilities.isEventDispatchThread()) {
				run.run();
			} else try {
				SwingUtilities.invokeAndWait(run);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
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

	// NB: Invoked by macros/StartupMacros.fiji.ijm
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
					!FileUtils.getExtension(path).equals("") &&
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
			final Constructor<?> ctor = textEditor.getConstructor(String.class);
			final Runnable run = new Runnable() {
				@Override
				public void run() {
					try {
						Frame frame = (Frame)ctor.newInstance(file.getAbsolutePath());
						if (frame == null) Thread.currentThread().interrupt();
						frame.setVisible(true);
					} catch (Exception e) {
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}
				}
			};
			if (SwingUtilities.isEventDispatchThread()) {
				run.run();
			} else try {
				SwingUtilities.invokeAndWait(run);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}

	public static boolean stackTraceContains(String needle) {
		final StringWriter writer = new StringWriter();
		final PrintWriter out = new PrintWriter(writer);
		new Exception().printStackTrace(out);
		out.close();
		return writer.toString().indexOf(needle) >= 0;
	}

	/**
	 * Get the MenuItem instance for a given menu path
	 *
	 * @param menuPath the menu path, e.g. {@code File>New>Bio-Formats}
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
	 * @param menuPath the menu path, e.g. {@code File>New>Bio-Formats}
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
	 * @param jarFile the source file
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

	/**
	 * Runs a plug-in with an optional argument.
	 * 
	 * @param className the plugin class
	 * @param arg the argument (use "" if you do not want to pass anything)
	 */
	public static void runPlugInGently(String className, String arg) {
		try {
			Class<?> clazz = IJ.getClassLoader()
				.loadClass(className);
			if (clazz != null) {
				PlugIn plugin = (PlugIn)clazz.newInstance();
				plugin.run(arg);
			}
		}
		catch (NoClassDefFoundError e) { }
		catch (ClassNotFoundException e) { }
		catch (InstantiationException e) { }
		catch (IllegalAccessException e) { }
	}

	public static void runUpdater() {
		System.setProperty("fiji.main.checksUpdaterAtStartup", "true");
		runPlugInGently("fiji.updater.UptodateCheck", "quick");
	}

	/**
	 * Runs the command associated with a menu label if there is one.
	 *
	 * @param menuLabel the label of the menu item to run
	 * @param arg the arg to pass to the plugin's run() (or setup()) method
	 */
	public static void runGently(String menuLabel, final String arg) {
		String className = (String)Menus.getCommands().get(menuLabel);
		if (className != null)
			IJ.runPlugIn(className, null);
	}

	/**
	 * Runs the command associated with a menu label if there is one.
	 *
	 * @param menuLabel the label of the menu item to run
	 */
	public static void runGently(String menuLabel) {
		runGently(menuLabel, "");
	}

}
