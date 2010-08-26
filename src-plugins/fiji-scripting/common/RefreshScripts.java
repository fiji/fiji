/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/**
A modified version of Albert Cardona's Refresh_Jython_List plugin,
for subclassing to do the same for arbitrary languages and directories.

This can now search the whole plugin tree to find scripts and insert
them at the corresponding place in the "Plugins" menu hierarchy.

------------------------------------------------------------------------

A Jython utility plugin for ImageJ(C).
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

import ij.IJ;
import ij.ImageJ;
import ij.Menus;
import ij.plugin.PlugIn;

import java.awt.Menu;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.awt.MenuBar;

import java.util.ArrayList;
import java.util.Arrays;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

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
	static {
		if (IJ.getInstance() != null)
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

	Menu pluginsMenu;

	/* This is called by addFromDirectory when it finds a file
	   that we might want to add - check the extension, etc. and
	   add it from this method. */
	private void maybeAddFile( String topLevelDirectory,
				   String subDirectory,
				   String filename ) {
		if (verbose) {
			System.out.println("maybeAddFile:");
			System.out.println("  t: "+topLevelDirectory);
			System.out.println("  s: "+subDirectory);
			System.out.println("  f: "+filename);
		}

		if( ! filename.endsWith(scriptExtension) )
			return;
		if( filename.indexOf("_") < 0 )
			return;

		Menu m;
		if( subDirectory.length() == 0 )
			m = pluginsMenu;
		else
			m = ensureSubMenu(subDirectory);

		String newLabel = strip(filename);
		if (m != null) {
			int n = m.getItemCount();
			MenuItem[] items = new MenuItem[n];
			for (int i=0; i<n; i++) {
				items[i] = m.getItem(i);
			}
			if (!addMenuItem(m, newLabel, filename))
				return;
		}

		String fullPath = topLevelDirectory + File.separator
			+ (0 == subDirectory.length() ?
				"" : subDirectory + File.separator)
			+ filename;
		String newCommand = getClass().getName() + "(\""
			+ fullPath + "\")";
		Menus.getCommands().put(newLabel, newCommand);
	}

	protected boolean addMenuItem(Menu m, String label, String filename) {
		String command = (String)Menus.getCommands().get(label);
		if (command == null) {
			// Now add the command:
			MenuItem item = new MenuItem(label);
			// storing the name of the script file as the action
			// command. The label is stripped!
			m.add(item);
			item.addActionListener(IJ.getInstance());

			return true;
		}

		// Allow overriding JavaScripts added by ImageJ
		if (scriptExtension.equals(".js") &&
				command.endsWith(".js\")") &&
				command.startsWith("ij.plugin.Macro_Runner("))
			return true;

		if (scriptExtension.equals(".java"))
			return true;

		if (command.startsWith(getClass().getName() + "("))
			return true;

		IJ.log("The script " + filename + " would override "
			+ "an existing menu entry; skipping");

		return false;
	}

	/** Split subDirectory by File.separator and make sure submenus
	    corresponding to those exist, creating them if necessary.
	    Then return the final submenu.

	    FIXME: Johannes points out that there's probably an ImageJ / ImageJA function to do
	    this more simply
	*/
	protected Menu ensureSubMenu( String subDirectory ) {

		boolean topMenu = true;

		boolean imageJA = false;
		ImageJ instance = IJ.getInstance();
		if (instance == null)
			return null;
		if( instance != null &&
				instance.getTitle().indexOf("ImageJA") >= 0 )
			imageJA = true;

		String separatorRegularExpression;
		/* Have I missed something obvious, or is Java really
		   missing a method to escape a string safely for
		   inclusion in a regular expression? */
		if( File.separator.equals("/") )
			separatorRegularExpression = "/";
		else if( File.separator.equals("\\") )
			separatorRegularExpression = "\\\\";
		else {
			IJ.error("BUG: unknown File.separator \""+File.separator+"\"");
			return null;
		}

		String [] parts = subDirectory.split(separatorRegularExpression);

		for (int i=0; i<parts.length; i++) {
			parts[i] = parts[i].replace('_', ' ');
		}

		Menu m = pluginsMenu;

		for( int i = 0; i < parts.length; ++i ) {
			String subMenuName = parts[i];
			int idealPosition = 0;
			boolean found = false;
			boolean afterSeparator = false;
			for( int j = 0; j < m.getItemCount(); ++j ) {
				MenuItem item=m.getItem(j);
				String n = item.getLabel();
				if( (! topMenu || afterSeparator) && (subMenuName.compareTo(n) > 0) ) {
					idealPosition = j + 1;
				}
				if( n.equals("-") && topMenu ) {
					afterSeparator = true;
					idealPosition = j + 1;
				}
				if( n.equals(subMenuName) ) {
					if (item instanceof Menu)  {
						m = (Menu)item;
						found = true;
						break;
					}
				}
			}
			if( ! found ) {
				/* Create a new subMenu, and insert it
				   at the "ideal position": */
				Menu newSubMenu = null;
				if (imageJA)
					newSubMenu = new PopupMenu(subMenuName);
				else
					newSubMenu = new Menu(subMenuName);
				m.insert(newSubMenu,idealPosition);
				m = newSubMenu;
			}
			topMenu = false;
		}

		return m;
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
	private void removeFromMenu(Menu menu) {
		int count = menu.getItemCount();
		for (int i = count - 1; i >= 0; i--) {
			MenuItem item = menu.getItem(i);
			if (item instanceof Menu) {
				removeFromMenu((Menu)item);
				continue;
			}
			String label = item.getLabel();
			String command = (String)Menus.getCommands().get(label);
			if (command == null ||
			    !command.startsWith(getClass().getName() + "(\""
				+ Menus.getPlugInsPath()) ||
			    !command.endsWith(scriptExtension + "\")"))
				continue;
			menu.remove(i);
			Menus.getCommands().remove(label);
		}
	}

	public void run(String arg) {

		if( arg != null && ! arg.equals("") ) {
			/* set the default class loader to ImageJ's PluginClassLoader */
			Thread.currentThread()
				.setContextClassLoader(IJ.getClassLoader());

			String path = arg;
			if (!new File(path).isAbsolute())
				path = new StringBuffer(Menus.getPlugInsPath()).append(path).toString(); // blackslash-safe
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

		MenuBar menu_bar = Menus.getMenuBar();
		// In headless mode, there is no menu bar
		if (menu_bar != null) {
			int n = menu_bar.getMenuCount();
			for (int i=0; i<n; i++) {
				Menu menu = menu_bar.getMenu(i);
				if (menu.getLabel().equals("Plugins")) {
					pluginsMenu = menu;
					break;
				}
			}
		}

		if (pluginsMenu != null)
			removeFromMenu( pluginsMenu );
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
		String pluginsPath = Menus.getPlugInsPath();
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
		String jarsPath = System.getProperty("fiji.dir") + "/jars";

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
