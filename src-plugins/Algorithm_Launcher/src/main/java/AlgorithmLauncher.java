//
// Algorithm_Launcher.java
//

/*
Simple imglib algorithm launcher.
Copyright (c) 2010, UW-Madison LOCI.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the UW-Madison LOCI nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY UW-MADISON LOCI ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL UW-MADISON LOCI BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.IOException;
import java.util.ArrayList;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.imageplus.ImagePlusContainer;
import mpicbg.imglib.exception.ImgLibException;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;

/**
 * Algorithm Launcher is a simple plugin to execute imglib algorithms.
 * It prompts for parameters in a dialog box, and works with macros as well.
 * The algorithms available are specified in the algorithms.config file.
 * 
 * @author Curtis Rueden ctrueden at wisc.edu
 *
 * @param <T>
 */
public class AlgorithmLauncher<T extends RealType<T>>
	implements PlugInFilter
{

	// -- Constants --

	private static final String CONFIG_FILE = "algorithms.config";

	// -- Fields --

	private ImagePlus image;

	// -- PlugInFilter API methods --

	//@Override
	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_ALL;
	}

	//@Override
	public void run(ImageProcessor ip) {
		run(image);
		image.updateAndDraw();
	}

	// -- Algorithm_Launcher API methods --

	public void run(ImagePlus image) {
		Image<T> img = ImagePlusAdapter.wrap(image);

		// populate list of algorithms with compatible ones only
		ArrayList<AlgorithmInfo<T>> algorithms = parseAlgorithms(CONFIG_FILE);
		if (algorithms == null) return;

		// prompt for choice of algorithm
		AlgorithmInfo<T> algInfo = promptWhich(algorithms);
		if (algInfo == null) return;

		// run algorithm
		Image<T> result = algInfo.run(img);
		if (result == null) return;

		// transform result back to ImagePlus...
		display(result);
	}

	// -- Helper methods --

	private ArrayList<AlgorithmInfo<T>> parseAlgorithms(String configFile) {
		try {
			AlgorithmParser<T> parser = new AlgorithmParser<T>();
			return parser.parse(configFile);
		}
		catch (IOException exc) {
			IJ.handleException(exc);
			return null;
		}
	}

	private AlgorithmInfo<T> promptWhich(
		ArrayList<AlgorithmInfo<T>> algorithms)
	{
		String[] algorithmLabels = new String[algorithms.size()];
		for (int i=0; i<algorithmLabels.length; i++) {
			algorithmLabels[i] = algorithms.get(i).getLabel();
		}
		GenericDialog gd = new GenericDialog("Choose an algorithm");
		gd.addChoice("Algorithm", algorithmLabels, algorithmLabels[0]);
		gd.showDialog();
		if (gd.wasCanceled()) return null;
		int index = gd.getNextChoiceIndex();
		return algorithms.get(index);
	}

	private void display(Image<T> img) {
		ImagePlus imp = null;
		Container c = img.getContainer();
		if (c instanceof ImagePlusContainer) {
			ImagePlusContainer ipc = (ImagePlusContainer) c;
			try {
				imp = ipc.getImagePlus();
			}
			catch (ImgLibException exc) {
				IJ.log("Warning: " + exc.getMessage());
			}
		}
		if (imp == null) {
			imp = ImageJFunctions.copyToImagePlus(img);
		}
		imp.show();
	}

}
