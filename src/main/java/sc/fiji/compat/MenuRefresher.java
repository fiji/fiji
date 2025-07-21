/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2007 - 2025 Fiji developers.
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
import ij.Prefs;
import ij.plugin.PlugIn;

import java.awt.Menu;
import java.util.Hashtable;

import fiji.Main;

public class MenuRefresher implements PlugIn, Runnable {

	/**
	 * The entry point of the {@link PlugIn}.
	 * 
	 * @param arg "update" if called from ij-legacy-patched ImageJ 1.x when the
	 *          user called {@code Help>Refresh Menus}.
	 */
	@Override
	public void run(final String arg) {
		run();
	}

	@Override
	public void run() {
		overrideCommands();
		Main.installRecentCommands();
	}

	@SuppressWarnings("unchecked")
	static void overrideCommands() {
		final Hashtable<String, String> commands = Menus.getCommands();
		if (!commands.containsKey("Install PlugIn...")) {
			commands.put("Install PlugIn...", "sc.fiji.compat.PlugInInstaller");
			if (IJ.getInstance() != null) {
				final Menu plugins = FijiTools.getMenu("Plugins");
				if (plugins != null)
					for (int i = 0; i < plugins.getItemCount(); i++)
						if (plugins.getItem(i).getLabel().equals("-")) {
							plugins.insert("Install PlugIn...", i);
							plugins.getItem(i).addActionListener(
									IJ.getInstance());
							break;
						}
			}
		}
		commands.put("Compile and Run...", "sc.fiji.compat.Compile_and_Run");
		// make sure "Edit>Options>Memory & Threads runs Fiji's plugin
		commands.put("Memory & Threads...", "sc.fiji.compat.Memory");

		// disable the Bio-Formats upgrade check
		Prefs.set("bioformats.upgradeCheck", false);
	}

}
