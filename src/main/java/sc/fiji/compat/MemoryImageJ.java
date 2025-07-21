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
import ij.plugin.PlugIn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** {@code Edit>Options>Memory & Threads...} code for legacy ImageJ.cfg. */
class MemoryImageJ {
	public static void run(String arg) {
		if (IJ.isMacOSX()) {
			// Unfortunately, ImageJ 1.x' Memory & Threads makes way too many assumptions to be
			// reused on MacOSX. A pity.
			runMacOSX();
			return;
		}

		final File configFile = new File(FijiTools.getImageJDir(), "ImageJ.cfg");
		if (!configFile.exists()) try {
			final PrintStream out = new PrintStream(new FileOutputStream(configFile));
			out.println(".");
			out.println(IJ.isWindows() ? "jre\\bin\\javaw.exe" : "jre/bin/java");
			out.println("-Xmx" +(maxMemory() >> 20) + "m -cp ij.jar ij.ImageJ");
			out.close();
		} catch (final IOException e) {
			IJ.error("Could not write initial ImageJ.cfg!");
			return;
		}
		new ij.plugin.Memory().run(arg);
	}

	private static long maxMemory() {
			return Runtime.getRuntime().maxMemory();
	}

	private static void runMacOSX() {
		long memory = maxMemory() >> 20;
		int threads = Prefs.getThreads();

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
		Prefs.setThreads(threads);

		final int limit = 1700;
		if (!IJ.is64Bit() && memory > limit) {
			if (!IJ.showMessageWithCancel("Memory",
					"Note: setting the memory limit to a "
					+ "value\ngreater than " + limit
					+ "MB on a 32-bit system\n"
					+ "may cause ImageJ to fail to start."))
				return;
		}

		writeFileMacOSX(memory);
	}

	private static void writeFileMacOSX(final long memory) {
		final File infoPList = new File(FijiTools.getImageJDir(), "Contents/Info.plist");

		try {
			String contents = readFile(infoPList);
			String mem = "" + memory + "m";

			FileOutputStream out = new FileOutputStream(infoPList);
			out.write(editInfoPList(contents, mem).getBytes("UTF-8"));
			out.close();
			IJ.showMessage("Memory", "The new " + memory
				+ " limit will take effect after Fiji"
				+ " is restarted.");
		} catch (Exception e) {
			IJ.error("Could not write " + infoPList);
		}
	}

	private static String readFile(final File file) throws IOException {
		if (!file.exists()) return "";

		byte[] buffer = new byte[(int)file.length()];
		FileInputStream in = new FileInputStream(file);
		in.read(buffer, 0, buffer.length);
		in.close();
		return new String(buffer, 0, buffer.length, "UTF-8");
	}

	private static int flags = Pattern.MULTILINE | Pattern.DOTALL;

	private static String editInfoPList(String contents, String memory) {
		String key = "\n\t\t<key>memory</key>"
			+ "\n\t\t<string>" + memory + "</string>" ;

		Pattern p = Pattern.compile(".*<key>fiji</key>[^<]*"
				+ "<dict>(.*)</dict>.*", flags);
		Matcher matcher = p.matcher(contents);
		if (matcher.matches()) {
			int start = matcher.start(1),
			    end = matcher.end(1);
			String inner = contents.substring(start, end);
			p = Pattern.compile(".*<key>(heap|mem|memory)</key>"
					+ "[^<]*<string>([^<]*)</string>.*",
					flags);
			Matcher matcher2 = p.matcher(inner);
			if (matcher2.matches()) {
				int start2 = start + matcher2.end(2);
				start += matcher2.start(2);
				return contents.substring(0, start)
					+ memory
					+ contents.substring(start2);
			}
			return contents.substring(0, start)
				+ key
				+ contents.substring(start);
		}
		String dict = "\t<key>fiji</key>\n"
			+ "\t<dict>" + key + "\n\t</dict>\n";

		p = Pattern.compile(".*(</dict>).*", flags);
		matcher = p.matcher(contents);
		if (matcher.matches()) {
			int start = matcher.start(1);
			return contents.substring(0, start) + dict
				+ contents.substring(start);
		}
		return "<plist version=\"1.0\">\n"
			+ "<dict>\n" + dict + "</dict>\n"
			+ "</plist>";
	}
}
