//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Niels Quack, Daniel Sage
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

public class EdfSobel extends AbstractEdfAlgorithm {

	public EdfSobel() {
	}

	/**
	 *
	 */
	public ImageWare[] process(ImageWare imageStack) {
		LogSingleton log = LogSingleton.getInstance();

		int nx = imageStack.getSizeX();
		int ny = imageStack.getSizeY();
		int nz = imageStack.getSizeZ();

		int i,j,k;

		float newval, oldval;

		ImageWare topology, res;
		ImageWare slice, temp, sharpness;

		slice = Builder.create(nx,ny,1,ImageWare.FLOAT);
		temp = Builder.create(nx,ny,1,ImageWare.FLOAT);
		sharpness = Builder.create(nx,ny,1,ImageWare.FLOAT);

		topology = Builder.create(nx,ny,1,ImageWare.FLOAT);
		topology.add(1);

		imageStack.getXY(0,0,0,slice);
		res = slice.duplicate();

		for (k=0; k<nz; k++){
			log.setProgessLength(15+ k*(65/nz));

			imageStack.getXY(0,0,k,slice);
			sharpness = Sobel.compute(slice);
			for(i = 0; i<nx; i++){
				for(j = 0; j < ny; j++){
					newval = (float)sharpness.getPixel(i,j,0);
					oldval = (float)temp.getPixel(i,j,0);
					if( oldval < newval ){
						temp.putPixel(i,j,0,newval);
						topology.putPixel(i,j,0,k+1);
						res.putPixel(i,j,0,slice.getPixel(i,j,0));
					}
				}
			}
			System.gc();
		}

		return new ImageWare[]{res, topology};

	}
}
