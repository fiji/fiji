/***************************************************************
 *
 * DiffusionInterpol2
 *
 * ported from Amira module
 *
 ***************************************************************/

package vib;

import amira.AmiraParameters;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class DiffusionInterpol2_ extends DiffusionInterpol2
		implements PlugInFilter {
	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_8C;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("DiffusionInterpol2");
		if (!AmiraParameters.addAmiraLabelsList(gd, "TemplateLabels"))
			return;
		if (!AmiraParameters.addAmiraMeshList(gd, "Model"))
			return;
		if (savedDisplace != null)
			gd.addCheckbox("reuseDistortion", true);
		gd.addCheckbox("rememberDistortion", false);
		gd.addStringField("LabelTransformationList",
							"1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1");
		gd.addNumericField("tolerance", 0.5, 2);

		gd.showDialog();
		if (gd.wasCanceled())
			return;
		
		template = new InterpolatedImage(image);
		templateLabels = new InterpolatedImage(
				WindowManager.getImage(gd.getNextChoice()));
		model = new InterpolatedImage(
			WindowManager.getImage(gd.getNextChoice()));
		reuse = gd.getNextBoolean();
		remember = gd.getNextBoolean();
		labelTransformations =FloatMatrix.parseMatrices(gd.getNextString());
		tolerance = (float)gd.getNextNumber();
		doit();
	}
}
