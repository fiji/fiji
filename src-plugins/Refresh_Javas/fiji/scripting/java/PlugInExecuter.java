package fiji.scripting.java;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;

import ij.text.TextWindow;

import ij.util.Tools;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

/*
 * This class should have been public instead of being hidden in
 * ij/plugin/Compiler.java.
 */
public class PlugInExecuter {

	/** Create a new object that runs the specified plugin
		in a separate thread. */
	public void runThreaded(final String plugin) {
		Thread thread = new Thread() {
			public void run() {
				PlugInExecuter.this.run(plugin);
			}
		};
		thread.setPriority(Math.max(thread.getPriority()-2,
					Thread.MIN_PRIORITY));
		thread.start();
	}

	public void run(String plugin) {
		try {
			ImageJ ij = IJ.getInstance();
			IJ.resetEscape();
			if (ij!=null) ij.runUserPlugIn(plugin, plugin, "", true);
		} catch(Throwable e) {
			IJ.showStatus("");
			IJ.showProgress(1.0);
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp!=null) imp.unlock();
			String msg = e.getMessage();
			if (e instanceof RuntimeException && msg!=null &&
					msg.equals(Macro.MACRO_CANCELED))
				return;
			CharArrayWriter caw = new CharArrayWriter();
			PrintWriter pw = new PrintWriter(caw);
			e.printStackTrace(pw);
			String s = caw.toString();
			if (IJ.isMacintosh())
				s = Tools.fixNewLines(s);
			new TextWindow("Exception", s, 350, 250);
		}
	}
}
