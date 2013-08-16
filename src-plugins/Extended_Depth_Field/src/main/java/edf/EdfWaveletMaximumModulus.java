//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Alex Prudencio, Daniel Sage
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

public abstract class EdfWaveletMaximumModulus extends AbstractEdfAlgorithm {

	/**
	 *
	 */
	abstract public ImageWare[] process(ImageWare imageStack);

	/**
	 *
	 */
	protected void majorityConsistencyCheck(ImageWare map, int windowSize, int nz) {
		this.majCCSubBand(map, windowSize, nz, 0);
		this.majCCSubBand(map, windowSize, nz, 1);
		this.majCCSubBand(map, windowSize, nz, 2);
	}


	/**
	 *
	 */
	protected void majCCSubBand(ImageWare map, int windowSize, int nz, int subBand) {
		ImageWare scale = null;
		int i, j, k, mx, my, x, y, startx, starty;
		int nx = map.getHeight();
		int ny = map.getWidth();
		short[][] arr = new short[windowSize][windowSize];
		int p, l;
		int size = windowSize*windowSize;
		short[] buf = new short[size];
		int count[] = new int[nz];
		int out = 0;

		for (i=0; i<3; i++)	{
			j=1;
			for (k=0; k<i; k++)
				j*=2;

			mx = nx/j/2;
			my = ny/j/2;

			switch(subBand){
			case 0 :
				startx = 0;
				starty = my;
				break;
			case 1:
				startx = mx;
				starty = 0;
				break;
			case 2:
				startx = mx;
				starty = my;
				break;
			default:
				throw new RuntimeException("Invalid SubBand");
			}

			short[][] arrtemp = new short[mx][my];

			map.getBoundedXY(startx, starty, 0, arrtemp);
			scale = Builder.create(arrtemp);

			for (x = 0; x<mx; x++) {
				for ( y = 0; y<my; y++) {
					scale.getNeighborhoodXY(x,y,0,arr,ImageWare.MIRROR);
					for(p=0; p<windowSize; p++) {
						System.arraycopy(arr[p],0,buf,p*windowSize,windowSize);
					}
					out = (int)buf[size/2];
					for (l=0; l<nz; l++) {
						count[l]=0;
					}
					for (p=0; p<size; p++)	{
						for (l=0 ; l<nz; l++) {
							if (buf[p] == l)
								count[l]++;
						}
					}
					for (l=0; l<nz; l++) {
						if (count[l] > size/2){
							out = l;
						}
					}
					arrtemp[x][y] = (short)out;
				}
			}
			map.putBoundedXY(startx,starty,0,arrtemp);
		}
	}

}
