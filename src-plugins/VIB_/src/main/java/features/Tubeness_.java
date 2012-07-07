/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package features;

import ij.*;
import ij.measure.Calibration;
import ij.plugin.*;
import ij.process.*;
import ij.gui.GenericDialog;

public class Tubeness_ implements PlugIn {

	static final String PLUGIN_VERSION = "1.2";

	public void run(String ignored) {

		ImagePlus original = WindowManager.getCurrentImage();
		if (original == null) {
			IJ.error("No current image to calculate tubeness of.");
			return;
		}

		Calibration calibration = original.getCalibration();

		double minimumSeparation = 1;
		if( calibration != null )
			minimumSeparation = Math.min(calibration.pixelWidth,
						     Math.min(calibration.pixelHeight,
							      calibration.pixelDepth));

		GenericDialog gd = new GenericDialog("\"Tubeness\" Filter (version "+PLUGIN_VERSION+")");
		gd.addNumericField("Sigma: ", (calibration==null) ? 1f : minimumSeparation, 4);
		gd.addMessage("(The default value for sigma is the minimum voxel separation.)");
		gd.addCheckbox("Use calibration information", calibration!=null);

		gd.showDialog();
		if( gd.wasCanceled() )
			return;

		double sigma = gd.getNextNumber();
		if( sigma <= 0 ) {
			IJ.error("The value of sigma must be positive");
			return;
		}
		boolean useCalibration = gd.getNextBoolean();

		TubenessProcessor tp = new TubenessProcessor(sigma,useCalibration);

		ImagePlus result = tp.generateImage(original);
		result.setTitle("tubeness of " + original.getTitle());

		result.show();
	}
}
