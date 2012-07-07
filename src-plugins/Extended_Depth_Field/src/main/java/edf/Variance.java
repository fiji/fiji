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

public class Variance {

	/**
	 * Compute the local variance for each pixel position in a square window
	 * of size windowSize.
	 * @param input
	 * @param windowSize
	 * @return  an ImageAccess object containing the variance.
	 */
	static public ImageWare compute(ImageWare input, int windowSize) {

		int nx = input.getWidth();
		int ny = input.getHeight();

		ImageWare output = Builder.create(nx, ny,1,ImageWare.FLOAT);

		float[][] buf = new float[windowSize][windowSize];
		int wlen = windowSize * windowSize;
		float[] arr = new float[wlen];

		int x, y, j;
		float ave,var,temp;

		// Loop through the image.
		for (x=0; x<nx; x++)	{
			for (y=0; y<ny; y++)	{

				ave = 0;
				var = 0;

				input.getNeighborhoodXY(x, y, 0, buf, ImageWare.MIRROR);

				//Transform neighborghood to 1-D array.
				for(j=0; j<windowSize; j++){
					System.arraycopy(buf[j],0,arr,j*windowSize,windowSize);
				}

				//compute average.
				for (j=0; j<wlen; j++){
					ave += arr[j];
				}
				ave /= wlen;

				//variance.
				for (j=0; j<wlen;j++)	{
					temp = arr[j]-ave;
					var += temp*temp;
				}

				output.putPixel(x, y, 0, var);
			}
		}
		return output;
	}

}
