package fiji.plugin.nperry;

import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.*;
import ij.process.ImageProcessor;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;

public class Embryo_Tracker implements ExtendedPlugInFilter {
	/** Class/Instance variables */
	// Imglib variables
	protected Image<T> img;
	
	// Variables required by ExtendedPlugInFilter
	private ImagePlus imp;								// the ImagePlus of the setup call
	private int flags = DOES_ALL|NO_CHANGES|NO_UNDO;	// the flags returned by setup(). For now, NO_CHANGES is set because I will make a new image and not change the original
	private int nPasses = 0;							// for progress bar, how many images to process (sequentially or parallel threads)
	
	/** 1. Setup (first method called by ExtendedPlugInFilter) */
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return flags;
	}
	
	/** 2. Show the dialogue box */
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		// Setup the dialogue window
		GenericDialog gd = new GenericDialog(command);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return DONE;  // if canceled, return DONE so that the run() method isn't executed.
		}
		return flags;
	}
	
	/** 3. Set the number of passes */
	public void setNPasses(int nPasses) {
		this.nPasses = nPasses;
	}
	
	/** 4. Run it! */
	public void run(ImagePlus imp) {
		// test code
		Object[] result = exec(imp);
		
		if (null != result) {
			ImagePlus scaled = (ImagePlus) result[1];
			scaled.show();
		}
	}
	
	public Object[] exec(ImagePlus imp) {
		//img = ImagePlusAdapter.wrap(imp);
		
		// test code
		ImageProcessor ip = imp.getProcessor();
		ImagePlus newImage = new ImagePlus("new image", ip);
		return new Object[]{"new image", newImage};
	}
}
