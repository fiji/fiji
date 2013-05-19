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
//==============================================================================

package edf;

import imageware.Builder;
import imageware.ImageWare;

public class Sobel {

	 /**
	  *
	  */
	static public ImageWare compute(ImageWare input) {
		int nx = input.getWidth();
		int ny = input.getHeight();

		ImageWare gx = Builder.create(nx, ny, 1, ImageWare.FLOAT);
		ImageWare gy = Builder.create(nx, ny, 1, ImageWare.FLOAT);
		float rowin[]  = new float[nx];
		float rowout[] = new float[nx];
		for (int y=0; y<ny; y++) {
			input.getX(0,y,0,rowin);
			sobelDifference(rowin, rowout);
			gx.putX(0,y,0,rowout);
			sobelAverage(rowin, rowout);
			gy.putX(0,y,0,rowout);
		}
		float colin[]  = new float[ny];
		float colout[] = new float[ny];
		for (int x=0; x<nx; x++) {
			gx.getY(x,0,0, colin);
			sobelAverage(colin, colout);
			gx.putY(x,0,0, colout);
			gy.putY(x,0,0, colin);
			sobelDifference(colin, colout);
			gy.putY(x,0,0, colout);
		}

		gx.pow(2);
		gy.pow(2);
		gx.add(gy);
		gx.sqrt();
		return gx;
	}

	/**
	* Implements an 1D sobel difference filter.
	* The kernel is: [-1, 0, 1]
	*
	* @param in       	input, array which should be filtered
	* @param out		output, filtered array of the type float
	*/
	static private void sobelDifference(float in[], float out[]) {
		int n = in.length;
		out[0] = 0;
		for (int k = 1; k < n - 1; k++) {
			out[k] = in[k+1] - in[k-1];
		}
		out[n-1] = 0;
	}

	/**
	* Implements an 1D sobel average filter.
	* The kernel is: [1, 2, 1]
	*
	* @param in       	input, array which should be filtered
	* @param out		output, filtered array of the type float
	*/
	static private void sobelAverage(float in[], float out[]) {
		int n = in.length;
		out[0] = 2 * in[0] + 2 * in[1];
		for (int k = 1; k < n - 1; k++) {
			out[k] = in[k-1] + 2*in[k] + in[k+1];
		}
		out[n-1] = 2 * in[n-2] + 2 * in[n-1];
	}

}