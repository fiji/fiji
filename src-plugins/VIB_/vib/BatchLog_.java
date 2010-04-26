/*
 * Call this plugin to display a (short) message.
 *
 * When called in batch mode, it opens a window which will be reused in
 * subsequent calls.
 *
 * When not run in batch mode, it just calls IJ.showStatus().
 *
 */

package vib;

import ij.IJ;
import ij.Macro;
import ij.plugin.PlugIn;

/*
 * TODO:
 * add progress bar
 * add stop button
 */
public class BatchLog_ implements PlugIn {
	public void run(String message) {
		if (message.equals(""))
			message = Macro.getOptions();
		appendText(message);
	}

	final public static void appendText(String message) {
		VIB.println(message);
	}
}

