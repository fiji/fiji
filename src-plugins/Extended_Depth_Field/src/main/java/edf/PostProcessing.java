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

import ij.ImageStack;
import ij.process.ColorProcessor;
import imageware.Builder;
import imageware.ImageWare;

public class PostProcessing {

	/**
	 *
	 */
	public static ImageWare reassignment(ImageWare res, ImageWare stack){

		int nx = stack.getSizeX();
		int ny = stack.getSizeY();
		int nz = stack.getSizeZ();
		double stackval, pixelval, temp, diff, finalpixelval;
		float finalPos;

		ImageWare topology = Builder.create(nx,ny,1,ImageWare.FLOAT);

		for(int i = 0; i < nx; i++){
			for(int j = 0 ; j < ny; j++){
				temp = Double.MAX_VALUE;
				diff = 0.0;
				finalpixelval = 0.0;
				finalPos = 0;
				for(int k = 0; k < nz; k++){
					stackval = (double)stack.getPixel(i,j,k);
					pixelval = (double)res.getPixel(i,j,0);
					diff = Math.abs(stackval - pixelval);
					if (diff < temp){
						temp = diff;
						finalpixelval = stackval;
						finalPos = (float)(k + 1);
					}
				}
				res.putPixel(i,j,0,finalpixelval);
				topology.putPixel(i,j,0,finalPos);
			}
		}
		return topology;
	}

	/**
	 *
	 */
	public static ColorProcessor reassignmentColor(ImageWare topology, ImageStack stack){

		int nx = topology.getSizeX();
		int ny = topology.getSizeY();

		ColorProcessor cp = new ColorProcessor(nx, ny);

		int color = 0;
		int index;

		for (int x=0; x<nx; x++){
			for (int y=0; y<ny; y++){
				index = (int)topology.getPixel(x, y, 0);
				color = ((ColorProcessor)stack.getProcessor(index)).getPixel(x, y);
				cp.putPixel(x, y, color);
			}
		}
		return cp;
	}

}
