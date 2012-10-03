/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/**
A modified version of Albert Cardona's Refresh_Jython_List plugin,
for subclassing to do the same for arbitrary languages and directories.

This can now search the whole plugin tree to find scripts and insert
them at the corresponding place in the "Plugins" menu hierarchy.

------------------------------------------------------------------------

Based on the Jython utility plugin for ImageJ(C).
Copyright (C) 2005 Albert Cardona.
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

package common;

import fiji.User_Plugins;

import ij.IJ;
import ij.ImageJ;
import ij.Macro;
import ij.Menus;

import ij.plugin.PlugIn;

import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.util.Arrays;

/**
 *  This class looks through the plugins directory for files with a
 *  particular extension (e.g. ".rb" for JRuby, ".py" for Jython) and
 *  for each one that it finds:
 *
 *   - Makes sure that the submenu corresponding to the subdirectory
 *     containing that file exists, creating submenus if necessary.
 *
 *   - Removes any existing script with that filename from the submenu.
 *
 *   - Adds the plugin as a handler for the action of clicking on that
 *     menu item.  (The plugin will the run the script if runScript is
 *     implemented correctly.)
 *
 *  Note that this does not currently notice that you have removed
 *  scripts from the plugins directory; it will leave stale menu
 *  entries for those scripts.
 *
 *  --------------------------------------------------------------------
 *
 * 	To create a shortcut to a Jython plugin a macro can be done to
 * 	pass appropriate arguments to the Launch_Python_Script class,
 * 	or tweak ImageJ, or a thousand not-so-straighforward ways.
 *
 */
abstract public class RefreshScripts implements PlugIn {
	public static final String magicMenuPrefix = "Plugins>Scripts>";
	protected static ImageJ ij;
	static {
		ij = IJ.getInstance();
		if (ij != null)
			System.setProperty("java.class.path",
					getPluginsClasspath());
	}

	protected String scriptExtension;
	protected String languageName;

	/** Default values: the system's. */
	protected OutputStream out = System.out,
		               err = System.err;

	public void setOutputStreams(OutputStream out, OutputStream err) {
		if (null != out) this.out = out;
		if (null != err) this.err = err;
	}

	public void setLanguageProperties( String scriptExtension, String languageName ) {
		this.scriptExtension = scriptExtension;
		this.languageName = languageName;
	}

	boolean verbose = false;

	protected void setVerbose(boolean v) { verbose = v; }

	File script_dir;

	/*
	 * This is called by addFromDirectory when it finds a file
	 * that we might want to add - check the extension, etc. and
	 * add it from this method.
	 *
	 * Unfortunately, we cannot use the getMenu() method of ij.Menus.
	 * Fortunately, we can use fiji.User_Plugins' getMenuItem() method.
	 */
	private void maybeAddFile(String topLevelDirectory, String subDirectory, String filename) {
		if (verbose) {
			System.out.println("maybeAddFile:");
			System.out.println("  t: "+topLevelDirectory);
			System.out.println("  s: "+subDirectory);
			System.out.println("  f: "+filename);
		}

		if (!filename.endsWith(scriptExtension))
			return;
		if (filename.indexOf("_") < 0)
			return;

		String newLabel = strip(filename);
		if (!addMenuItem(subDirectory, newLabel,filename))
			return;

		String fullPath = topLevelDirectory + File.separator
			+ (0 == subDirectory.length() ?
				"" : subDirectory + File.separator)
			+ filename;
		String newCommand = getClass().getName() + "(\""
			+ fullPath + "\")";
		Menus.getCommands().put(newLabel, newCommand);
	}

	protected boolean addMenuItem(String subDirectory, String label, String filename) {
		String command = (String)Menus.getCommands().get(label);
		if (command == null) {
			if (ij != null) {
				String menuPath = "Plugins>" + subDirectory.replace(File.separator.charAt(0), '>').replace('_', ' ');
				if (menuPath.startsWith(magicMenuPrefix))
					menuPath = menuPath.substring(magicMenuPrefix.length());
				Menu menu = (Menu)User_Plugins.getMenuItem(Menus.getMenuBar(), menuPath, true);
				MenuItem item = new MenuItem(label);
				menu.add(item);
				item.addActionListener(IJ.getInstance());
			}
			return true;
		}

		if (scriptExtension.equals(".java"))
			return true;

		// Allow overriding previously added scripts
		// and macros and Javascripts added by ImageJ
		if (isThisLanguage(command))
			return true;

		IJ.log("The script " + filename + " would override an existing menu entry; skipping");

		return false;
	}

	/**
	   This will find all the files under topLevelDirectory and
	   call maybeAddFile on each.  If you want to recurse to
	   unlimited depth, set maxDepth to -1.  To only look in the
	   specified topLevelDirectory and not recurse into any
	   subdirectories, set maxDepth to 1.
	 */
	private void addFromDirectory( String topLevelDirectory, int maxDepth ) {
		addFromDirectory( topLevelDirectory, "", 0, maxDepth );
	}

	// This is just for recursion; call the addFromDirectory(String,int)
	// method instead
	private void addFromDirectory( String topLevelDirectory, String subPath, int depth, int maxDepth ) {
		if (subPath.equals(".rsrc") || subPath.endsWith("/.rsrc"))
			return;
		File f = new File(topLevelDirectory + File.separator + subPath );
		if (f.isDirectory() ) {
			if (maxDepth >= 0 && depth >= maxDepth)
				return;
			String [] entries = f.list();
			Arrays.sort(entries);
			for( int i = 0; i < entries.length; ++i )
				if( ! (entries[i].equals(".")||entries[i].equals("..")) ) {
					String newSubPath = subPath;
					if( newSubPath.length() > 0 )
						newSubPath += File.separator;
					newSubPath += entries[i];
					addFromDirectory( topLevelDirectory,
							  newSubPath,
							  depth + 1,
							  maxDepth );
				}
		} else {
			String filename = f.getName();
			int n = filename.length();
			int toTrim = (subPath.length() > n) ? n + 1 : n ;
			String subDirectory = subPath.substring(0,subPath.length()-toTrim);
			maybeAddFile(topLevelDirectory,subDirectory,f.getName());
		}
	}

	// Removes all entries that refer to scripts with the current extension
	protected void removeFromMenu(Menu menu) {
		int count = menu.getItemCount();
		for (int i = count - 1; i >= 0; i--) {
			MenuItem item = menu.getItem(i);
			if (item instanceof Menu) {
				removeFromMenu((Menu)item);
				if (((Menu)item).getItemCount() == 0)
					menu.remove(item);
				continue;
			}
			String label = item.getLabel();
			String command = (String)Menus.getCommands().get(label);
			if (!isThisLanguage(command))
				continue;
			menu.remove(i);
			Menus.getCommands().remove(label);
		}
	}

	/**
	 * Test whether a command is handled by this class
	 */
	protected boolean isThisLanguage(String command) {
		return command != null &&
		    command.startsWith(getClass().getName() + "(\""
			+ Menus.getPlugInsPath()) &&
		    command.endsWith(scriptExtension + "\")");
	}

	public void run(String arg) {

		if( arg != null && ! arg.equals("") ) {
			String path = arg;
			if (path.startsWith("jar:")) {
				path = path.substring(4);
				if (!path.startsWith("/"))
					path = "/" + path;
				InputStream input = getClass().getResourceAsStream(path);
				if (input == null) {
					IJ.error("Did not find resource '" + path + "'");
					return;
				}
				if (IJ.shiftKeyDown())
					IJ.error("Opening resources in the script editor is not yet implemented!");
				else {
					IJ.showStatus("Running resource " + path);
					runScript(input, path);
				}
				return;
			}

			if (!new File(path).isAbsolute())
				path = new StringBuffer(Menus.getPlugInsPath()).append(path).toString(); // blackslash-safe

			if (IJ.shiftKeyDown()) {
				IJ.showStatus("Opening " + path);
				IJ.runPlugIn("fiji.scripting.Script_Editor", path);
				return;
			}
			else
				IJ.showStatus("Running " + path);

			/* set the default class loader to ImageJ's PluginClassLoader */
			Thread.currentThread()
				.setContextClassLoader(IJ.getClassLoader());
			runScript(path);
			return;
		}

		if( scriptExtension == null || languageName == null ) {
			IJ.error("BUG: setLanguageProperties must have been called (with non-null scriptExtension and languageName");
			return;
		}

		String pluginsPath = Menus.getPlugInsPath();
		if (pluginsPath == null)
			return; // most likely we're running via Java WebStart

		script_dir = new File(pluginsPath);

		// Find files with the correct extension
		if (!script_dir.exists()) {
			IJ.error("The plugins directory '"+script_dir+"' did not exist (!)");
			return;
		}

		MenuBar menuBar = Menus.getMenuBar();
		// In headless mode, there is no menu bar
		if (menuBar != null)
			for (int i = 0; i < menuBar.getMenuCount(); i++)
				removeFromMenu(menuBar.getMenu(i));

		addFromDirectory( Menus.getPlugInsPath(), -1 );
	}

	/** Converts 'My_python_script.py' to 'My python script'*/
	private String strip(String file_name) {
		if (file_name.endsWith(scriptExtension))
			file_name = file_name.substring(0,
				file_name.length() - scriptExtension.length());
		return file_name.replace('_',' ');
	}

	/** Run the script of the given name in a new thread. */
	public void runScript(InputStream istream, String fileName) {
		// by default, ignore the file name
		runScript(istream);
	}

	/** Run the script in a new thread. */
	abstract public void runScript(InputStream istream);

	/** Run the script in a new thread. */
	abstract public void runScript(String filename);

	static public void printError(Throwable t) {
		if (t instanceof RuntimeException && t.getMessage() == Macro.MACRO_CANCELED) {
			IJ.showStatus("Macro/script aborted");
			return;
		}
		IJ.handleException(t);
	}

	// TODO rename to readText
        static public final String openTextFile(final String path) {
                if (null == path || !new File(path).exists()) return null;
		try {
			// Stream will be closed in overloaded method:
                        return openTextFile(new BufferedInputStream(new FileInputStream(path)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/** Will consume and close the stream. */
        static public final String openTextFile(final InputStream istream) {
                final StringBuffer sb = new StringBuffer();
		BufferedReader r = null;
                try {
                        r = new BufferedReader(new InputStreamReader(istream));
                        while (true) {
                                String s = r.readLine();
                                if (null == s) break;
                                sb.append(s).append('\n'); // I am sure the reading can be done better
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                } finally {
                        if (null != r) try { r.close(); } catch (java.io.IOException ioe) { ioe.printStackTrace(); }
		}
                return sb.toString();
        }

	protected static String getPluginsClasspath() {
		String classPath = System.getProperty("java.class.path");
		if (classPath == null)
			return "";

		// strip out all plugin .jar files (to keep classPath short)
		String pluginsPath = System.getProperty("ij.dir") + "/plugins";
		if (pluginsPath == null)
			return "";

		for (int i = 0; i >= 0; i =
				classPath.indexOf(File.pathSeparator, i + 1)) {
			while (classPath.substring(i).startsWith(pluginsPath)) {
				int j = classPath.indexOf(File.pathSeparator,
					i + 1);
				classPath = classPath.substring(0, i)
					+ (j < 0 ? "" :
						classPath.substring(j + 1));
			}
		}
		String jarsPath = System.getProperty("ij.dir") + "/jars";

		// append the plugin .jar files
		try {
			classPath = appendToPath(classPath,
					discoverJars(pluginsPath));
			classPath = appendToPath(classPath,
					discoverJars(jarsPath));
		} catch (IOException e) { e.printStackTrace(); }
		return classPath;
	}

	protected static String appendToPath(String path, String append) {
		if (append != null && !path.equals("")) {
			if (!path.equals(""))
				path += File.pathSeparator;
			return path + append;
		}
		return path;
	}

	protected static String discoverJars(String path) throws IOException {
		if (path.equals(".rsrc") || path.endsWith("/.rsrc"))
			return "";
		if (path.endsWith(File.separator))
			path = path.substring(0, path.length() - 1);
                File file = new File(path);
                if (file.isDirectory()) {
			String result = "";
			String[] paths = file.list();
			Arrays.sort(paths);
                        for (int i = 0; i < paths.length; i++) {
				String add = discoverJars(path
						+ File.separator + paths[i]);
				if (add == null || add.equals(""))
					continue;
				if (!result.equals(""))
					result += File.pathSeparator;
                                result += add;
			}
			return result;
		}
                else if (path.endsWith(".jar"))
			return path;
		return null;
        }
}
