package fiji.plugin.nperry;

import ij.plugin.PlugIn;

public class Embryo_Tracker implements PlugIn {
	
	/** Ask for parameters and then execute.*/
	public void run(String arg) {
		// 1 - Obtain the currently active image:
		ImagePlus imp = IJ.getImage();
		if (null == imp) return;
 
		// 2 - Ask for parameters:
		GenericDialog gd = new GenericDialog("Scale");
		gd.addNumericField("width:", imp.getWidth(), 0);
		gd.addNumericField("height:", imp.getHeight(), 0);
		gd.addStringField("name:", imp.getTitle());
		gd.showDialog();
		if (gd.wasCanceled()) return;
 
		// 3 - Retrieve parameters from the dialog
		int width = (int)gd.getNextNumber();
		int height = (int)gd.getNextNumber();
		String name = gd.getNextString();
 
		// 4 - Execute!
		Object[] result = exec(imp, name, width, height);
 
		// 5 - If all went well, show the image:
		if (null != result) {
			ImagePlus scaled = (ImagePlus) result[1];
			scaled.show();
		}
	}
}
