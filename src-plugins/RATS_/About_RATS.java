import ij.IJ;

import ij.plugin.PlugIn;

public class About_RATS implements PlugIn {
	public void run(String arg) {
		IJ.showMessage("Robust Automatic Threshold Selection (RATS)\n"
			+ " \n"
			+ "This algorithm determines local thresholds based\n"
			+ "on the pixel values and their gradient.\n"
			+ " \n"
			+ "The implementation was done by Ben Tupper and\n"
			+ "Mike Sieracki at Bigelow Laboratory, W. Boothbay Harbor, Maine.\n"
			+ " \n"
			+ "The algorithm is based on:\n"
			+ "Wilkinson MHF (1998) Segmentation techniques in image analysis of microbes.\n"
			+ "Chapter 6 IN: Wilkinson MHF, Schut F (eds.) Digital Image Analysis of Microbes:\n"
			+ "Imaging, Morphometry, Fluorometry and Motility Techniques and Applications,\n"
			+ "John Wiley & Sons, New York.");
	}
}
