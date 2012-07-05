/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

import ij.IJ;
import ij.io.SaveDialog;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.text.TextWindow;
import ij.WindowManager;

import amira.AmiraParameters;
import amira.AmiraTableEncoder;

public class AmiraTableWriter_ implements PlugIn {

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Choose Window");
		if (!AmiraParameters.addAmiraTableList(gd, "window"))
			// addAmiraTableList reports errors
			return;
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String title = gd.getNextChoice();
		Object frame = WindowManager.getFrame(title);
		if (frame == null) {
			IJ.error("[BUG] No window from WindowManager.getFrame()");
			return;
		}

		SaveDialog od = new SaveDialog("AmiraFile", null, ".am");
		String dir=od.getDirectory();
		String name=od.getFileName();
		if(name==null) {
			IJ.error("No name was chosen: not saved");
			return;
		}

		if (!(frame instanceof TextWindow)) {
			IJ.error("[BUG] frame wasn't an instance of TextWindow");
			return;
		}

		TextWindow t = (TextWindow)frame;
		AmiraTableEncoder e = new AmiraTableEncoder(t);
		if (!e.write(dir + name))
			IJ.error("Could not write to " + dir + name);
		return;
	}
}
