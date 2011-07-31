//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Daniel Sage
//
// Organization: Biomedical Imaging Group (BIG)
// Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland
//
// Information: http://bigwww.epfl.ch/demo/edf/
//
// Reference: B. Forster, D. Van De Ville, J. Berent, D. Sage, M. Unser
// Complex Wavelets for Extended Depth-of-Field: A New Method for the Fusion
// of Multichannel Microscopy Images, Microscopy Research and Techniques,
// 65(1-2), pp. 33-42, September 2004.
//
// Conditions of use: You'll be free to use this software for research purposes,
// but you should not redistribute it without our consent. In addition, we
// expect you to include a citation or acknowledgment whenever you present or
// publish results that are based on it.
//
// History:
// - Updated (Daniel Sage, 21 December 2010)
//
//==============================================================================

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;

import java.applet.Applet;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.net.URL;

/**
 * This class is an applet demonstration of the EDF. It loads a image stack
 * and present the easy dialog.
 */
public class Demo extends Applet {

	private EDF_Easy_ edf;

	/**
	 * Initialization of the applet.
	 * Load the image stack soudure.
	 */
	public void init() {
		ImageStack stack = new ImageStack(512, 512);
		for(int i=2; i<9; i++) {
			ColorProcessor cp1 = (ColorProcessor)(new ImagePlus("",
					loadImageFile("soudure/s000" + i + ".jpg"))).getProcessor();
			stack.addSlice("", cp1);
		}

		ImagePlus imp = new ImagePlus("z-stack of images", stack);

		imp.show();
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension window = imp.getWindow().getSize();
		if (window.width==0)
			return;
		int left = screen.width/4-window.width/2;
		int top = (screen.height-window.height)/4;
		if (top<0) top = 0;
		imp.getWindow().setLocation(left, top);

		edf = new EDF_Easy_();
		edf.run("applet");

		edf.dl.setJSliderQuality(2);
		edf.dl.setJSliderRegularization(4);
		edf.dl.setJCheckBoxShow3D(true);
		edf.dl.setJCheckBoxShowTopology(true);
	}

	/**
	 * Load an image from a URL.
	 */
	public Image loadImageURL(String urltext) {
		Image image = null;
		try {
			URL url = new URL(urltext);
			MediaTracker mtracker = new MediaTracker(this);
			image = getImage(url);
			mtracker.addImage(image, 0);
			mtracker.waitForAll();
		}
		catch  (Exception e) {
			System.out.println("Exeception" + e);
		}
		return image;
	}

	/**
	 * Load an image from a local file.
	 */
	public Image loadImageFile(String filename) {
		Image image=null;
		MediaTracker mtracker = new MediaTracker(this);
		image = getImage(this.getDocumentBase(), filename);
		mtracker.addImage(image, 0);
		try {
			mtracker.waitForAll();
		}
		catch (InterruptedException ie) {
			System.out.println("Bad loading of an image.");
		}
		return image;
	}

}