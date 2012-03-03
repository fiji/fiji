package fiji;

import ij.IJ;
import ij.Menus;
import ij.Prefs;

import ij.gui.GenericDialog;

import ij.plugin.PlugIn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** This plugin implements the Edit/Options/Memory command. */
public class Memory implements PlugIn {
	public void run(String arg) {

		long memory = maxMemory() >> 20;
		int threads = Prefs.getThreads();

		GenericDialog gd = new GenericDialog("Memory "
			+ (IJ.is64Bit() ? "(64-bit)" : "(32-bit)"));
		gd.addNumericField("Maximum Memory:", memory, 0, 5, "MB");
		gd.addNumericField("Parallel Threads for Stacks:",
				threads, 0, 5, "");
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		memory = (long)gd.getNextNumber();
		threads = (int)gd.getNextNumber();
		Prefs.setThreads(threads);

		if (gd.invalidNumber()) {
			IJ.showMessage("Memory",
					"The number entered was invalid.");
			return;
		}
		int limit = IJ.isWindows() ? 1600 : 1700;
		if (!IJ.is64Bit() && memory > limit) {
			if (!IJ.showMessageWithCancel("Memory",
					"Note: setting the memory limit to a "
					+ "value\ngreater than " + limit
					+ "MB on a 32-bit system\n"
					+ "may cause ImageJ to fail to start."))
				return;
		}

		writeFile(memory);
	}

	void writeFile(long memory) {
		String fileName;
		String pattern;

		String dir = Menus.getPlugInsPath();
		if (dir == null || dir.equals("")) {
			IJ.error("Could not find Fiji directory");
			return;
		}
		if (dir.endsWith(File.separator + "plugins" + File.separator) ||
				dir.endsWith(File.separator + "plugins"))
			dir = new File(dir).getParent();

		if (IJ.isMacOSX())
			fileName = dir + "/Contents/Info.plist";
		else
			fileName = dir + "/jvm.cfg";

		try {
			String contents = readFile(fileName);
			String mem = "" + memory + "m";

			FileOutputStream out = new FileOutputStream(fileName);
			out.write(edit(contents, mem).getBytes("UTF-8"));
			out.close();
			IJ.showMessage("Memory", "The new " + memory
				+ " limit will take effect after Fiji"
				+ " is restarted.");
		} catch (Exception e) {
			IJ.error("Could not write " + fileName);
		}
	}

	public static String readFile(String fileName) {
		try {
			File file = new File(fileName);
			if (file.exists()) {
				int size = (int)file.length();
				byte[] buffer = new byte[size];
				FileInputStream in = new FileInputStream(file);
				in.read(buffer, 0, size);
				in.close();
				return new String(buffer, 0, size, "UTF-8");
			}
		} catch(Exception e) { }
		return "";
	}

	public static String edit(String contents, String memory) {
		return IJ.isMacOSX() ?
			editInfoPlist(contents, memory) :
			editJVMOptions(contents, memory);
	}

	private static int flags = Pattern.MULTILINE | Pattern.DOTALL;

	public static String editJVMOptions(String contents, String memory) {
		Pattern p = Pattern.compile(".*-Xmx([0-9][0-9]*[kmg]?).*",
				flags);
		Matcher matcher = p.matcher(contents);
		if (matcher.matches()) {
			int start = matcher.start(1);
			int start2 = matcher.end(1);
			return contents.substring(0, start)
				+ memory + contents.substring(start2);
		}
		if (!contents.equals(""))
			return contents + " -Xmx" + memory;
		return "-Xmx" + memory;
	}

	public static String editInfoPlist(String contents, String memory) {
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

	public long maxMemory() {
			return Runtime.getRuntime().maxMemory();
	}

	public long getMemorySetting() {
		if (IJ.getApplet() != null)
			return 0L;
		return maxMemory();
	}

	public static void main(String[] args) {
		String mem = "MEMORY";

		for (int i = 0; i < args.length; i++) {
			String contents = readFile(args[i]);
			System.err.println("File: " + args[i]
				+ "\n=========\n"
				+ (args[i].endsWith(".plist") ?
					editInfoPlist(contents, mem) :
					editJVMOptions(contents, mem)));
		}
	}
}
