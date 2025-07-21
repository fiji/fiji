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
import ij.Prefs;
import ij.gui.GenericDialog;
import org.scijava.launcher.Config;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.StringJoiner;

/**
 * {@code Edit>Options>Memory & Threads...} code for Jaunch's fiji.cfg.
 *
 * @author Mark Hiner
 */
class MemoryJaunch {
	public static final String FIJI_HEAP_KEY = "max-heap";
	public static final long FIJI_MIN_MB = 100;

	public static void run(String appDir) {
		long memory = maxMemory() >> 20;
		int threads = Prefs.getThreads();

		// Attempt to load existing config settings
		final File fijiCfg = new File(
				new StringJoiner(File.separator).add(appDir)
						.add("config").add("jaunch").add("fiji.cfg").toString());

		if (fijiCfg.exists()) {
			try {
				final Map<String, String> config = Config.load(fijiCfg);
				if (config.containsKey(FIJI_HEAP_KEY)) {
					String memSetting = config.get(FIJI_HEAP_KEY);
					// Record and pop off the suffix
					final char suffix = memSetting.toLowerCase().charAt(memSetting.length() - 1);
					memSetting = memSetting.substring(0, memSetting.length() - 1);
					// Use the memory setting if it was set in GB or MB.
					long memConfig = Long.parseLong(memSetting);
					switch (suffix) {
						case 'g': memConfig *= 1024;
						case 'm': memory = memConfig; break;
						default:
							IJ.error("Ignoring unrecognized memory setting: " + memSetting);
					}
				}
			}
			catch (IOException e) {
				IJ.error(
						"Could not read existing config file: " + fijiCfg.getAbsolutePath());
				return;
			}
		}

		final long lastMemory = memory;
		final GenericDialog gd = new GenericDialog("Memory "
			+ (IJ.is64Bit() ? "(64-bit)" : "(32-bit)"));
		gd.addNumericField("Maximum Memory:", memory, 0, 5, "MB");
		gd.addNumericField("Parallel Threads for Stacks:",
				threads, 0, 5, "");
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		if (gd.invalidNumber()) {
			IJ.showMessage("Memory",
					"The number entered was invalid.");
			return;
		}

		memory = (long)gd.getNextNumber();
		threads = (int)gd.getNextNumber();

		if (memory < FIJI_MIN_MB) {
			IJ.showMessage("Memory",
					"Invalid memory setting. Must be above " + FIJI_MIN_MB + "MB.");
			return;
		}
		Prefs.setThreads(threads);

		// Update the config file with the new memory setting, in MB
		try {
			Config.update(fijiCfg, FIJI_HEAP_KEY, String.valueOf(memory) + "m");
		}
		catch (IOException e) {
			IJ.error(
				"Could not write to existing config file: " + fijiCfg.getAbsolutePath());
			return;
		}

		if (lastMemory != memory) {
			IJ.showMessage("Memory",
					"The new " + memory + "MB setting will take effect after restarting.");
		}

		final int limit = 1700;
		if (!IJ.is64Bit() && memory > limit) {
			if (!IJ.showMessageWithCancel("Memory",
					"Note: setting the memory limit to a "
					+ "value\ngreater than " + limit
					+ "MB on a 32-bit system\n"
					+ "may cause ImageJ to fail to start."))
				return;
		}
	}

	private static long maxMemory() {
			return Runtime.getRuntime().maxMemory();
	}
}
