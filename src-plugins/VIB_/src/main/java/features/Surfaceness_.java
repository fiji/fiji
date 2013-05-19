/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package features;

import ij.*;
import ij.measure.Calibration;
import ij.plugin.*;
import ij.process.*;
import ij.gui.GenericDialog;

public class Surfaceness_ implements PlugIn {

	public void run(String ignored) {

		ImagePlus original = WindowManager.getCurrentImage();
		if (original == null) {
			IJ.error("No current image to calculate surfaceness of.");
			return;
		}

                if( original.getStackSize() == 1 ) {
                        IJ.error("It only makes sense to look for Sufaceness of 3D images (stacks)");
                        return;
                }

		Calibration calibration = original.getCalibration();

		double minimumSeparation = 1;
		if( calibration != null )
			minimumSeparation = Math.min(calibration.pixelWidth,
						     Math.min(calibration.pixelHeight,
							      calibration.pixelDepth));

		GenericDialog gd = new GenericDialog("\"Surfaceness\" Filter");
		gd.addNumericField("Sigma: ", (calibration==null) ? 1f : minimumSeparation, 4);
		gd.addMessage("(The default value for sigma is the pixel width.)");
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

		SurfacenessProcessor sp = new SurfacenessProcessor(sigma,useCalibration);

		ImagePlus result = sp.generateImage(original);
		result.setTitle("surfaceness of " + original.getTitle());

		result.show();
	}
}
