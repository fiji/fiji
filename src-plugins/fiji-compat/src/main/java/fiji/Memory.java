package fiji;

import ij.IJ;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/** This plugin implements the Edit/Options/Memory & Threads... command. */
public class Memory implements PlugIn {
	public void run(String arg) {

		final File configFile = new File(FijiTools.getFijiDir(), "ImageJ.cfg");
		if (!configFile.exists()) try {
			final PrintStream out = new PrintStream(new FileOutputStream(configFile));
			out.println(".");
			out.println(IJ.isWindows() ? "jre\\bin\\javaw.exe" : "jre/bin/java");
			out.println("-Xmx" +(maxMemory() >> 20) + "  -cp ij.jar ij.ImageJ");
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
}
