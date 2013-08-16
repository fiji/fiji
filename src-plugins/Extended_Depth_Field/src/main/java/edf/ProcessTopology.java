//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Alex Prudencio, Niels Quack
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

package edf;

import ij.ImagePlus;
import imageware.Builder;
import imageware.ImageWare;

public class ProcessTopology {

	private ImagePlus topology;
	private boolean close;
	private boolean open;
	private boolean smooth;
	private double sigma;


	/**
	 */
	public ProcessTopology (ImagePlus topology,boolean close, boolean open, double sigma) {
		this.topology = topology;
		this.close = close;
		this.open = open;
		this.smooth = true;
		this.sigma = sigma;
	}

	/**
	 */
	public ProcessTopology (ImagePlus topology, boolean close, boolean open) {
		this.topology = topology;
		this.open = open;
		this.close = close;
		this.smooth = false;
	}

	/**
	 */
	public void process () {

		ImageWare iw = Builder.wrap(topology);
		if(close) {
			iw.copy(MorphologicalOperators.doClose(iw));
		}
		if(open) {
			iw.copy(MorphologicalOperators.doOpen(iw));
		}
		if(smooth) {
			iw.smoothGaussian(sigma);
		}
		topology.show();
		topology.updateAndDraw();
	}
}
