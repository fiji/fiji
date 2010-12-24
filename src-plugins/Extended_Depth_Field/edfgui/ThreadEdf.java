//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Alex Prudencio
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
//==============================================================================

package edfgui;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

public class ThreadEdf extends Thread {

	Parameters parameters;

	public ThreadEdf(Parameters parameters){
		super();
		this.parameters = parameters;
	}

	public void run(){
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.error("The input image is not a z-stack of images.");
			return;
		}
		ExtendedDepthOfField edf = new ExtendedDepthOfField(imp, parameters);
		edf.process();
		System.gc();
	}

}
