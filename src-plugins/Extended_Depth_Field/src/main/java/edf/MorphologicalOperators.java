//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Alex Prudencio, Jesse Berent, Niels Quack, Daniel Sage
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

import imageware.FMath;
import imageware.ImageWare;

public class MorphologicalOperators {

	/**
	 * Implements "dilation" method for 4-connected pixels of an
	 * ImageAccess object.
	 * For each pixel, the maximum value of the gray levels of
	 * its 3x3 local neighborhood which is 4-connected is found.
	 * The result is returned by the same ImageAccess object.
	 *
	 * @param img       	an ImageAccess object
	 */
	static public ImageWare doDilation(ImageWare img) {

		int nx = img.getWidth()-1;
		int ny = img.getHeight()-1;
		ImageWare out = img.duplicate();
		double arr[][] = new double[3][3];
		double max, temp;

		for (int x=0; x<nx; x++)
			for (int y=0; y<ny; y++) {
				img.getNeighborhoodXY(x, y, 0, arr, ImageWare.MIRROR);
				max = -Double.MAX_VALUE;
				for (int k=0; k<3; k++)
					for (int l=0; l<3; l++) {
						temp = arr[k][l];
						if (temp > max) {
							max = temp;
						}
					}
				out.putPixel(x, y, 0, max);
			}
		return out;
	}

	/**
	 * Implements "Erosion" method for 4-connected pixels of an
	 * ImageAccess object.
	 */
	static public ImageWare doErosion(ImageWare img) {

		int nx = img.getWidth()-1;
		int ny = img.getHeight()-1;
		ImageWare out = img.duplicate();
		double arr[][] = new double[3][3];
		double min, temp;

		for (int x=1; x<nx; x++)
			for (int y=1; y<ny; y++) {
				img.getNeighborhoodXY(x, y, 0, arr, ImageWare.MIRROR);
				min = Double.MAX_VALUE;
				for (int k=0; k<3; k++)
					for (int l=0; l<3; l++) {
						temp = arr[k][l];
						if (temp < min) {
							min = temp;
						}
					}
				out.putPixel(x, y, 0, min);
			}

		return out;
	}

	/**
	 * Implements "Open" method for an
	 * ImageAccess object.
	 */
	static public ImageWare doOpen(ImageWare img) {
		ImageWare out = doErosion(img);
		return doDilation(out);
	}

	/**
	 * Implements "Close" method for an
	 * ImageAccess object.
	 */
	static public ImageWare doClose(ImageWare img) {
		ImageWare out = doDilation(img);
		return doErosion(out);
	}

	/**
	 *
	 */
	static public ImageWare doMedian(ImageWare img, int size) {

		int s2 = (size+1) / 2;
		int nx = img.getWidth() - s2;
		int ny = img.getHeight() - s2;
		ImageWare out = img.duplicate();
		double arr[][] = new double[size][size];
		double arr2[] = new double[size*size];
		double median;

		int i, j, x, y;
		for (x=s2; x<nx; x++)
			for (y=s2; y<ny; y++) {
				img.getNeighborhoodXY(x, y, 0, arr, ImageWare.MIRROR);
				for ( i=0; i < size; i++ )
					for( j=0; j < size; j++ )
						arr2[i*size+j] = arr[i][j];
				sort(arr2);
				median = arr2[FMath.floor(size*size/2)];
				out.putPixel(x, y, 0, median);
			}

		return out;
	}

	/**
	 *
	 */
    private static void sort(double a[], int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;
        if (lo >= hi) {
            return;
        }
        double mid = a[(lo + hi) / 2];
        while (lo < hi) {
            while (lo<hi && a[lo] < mid) {
                lo++;
            }
            while (lo<hi && a[hi] >= mid) {
                hi--;
            }
            if (lo < hi) {
                double T = a[lo];
                a[lo] = a[hi];
                a[hi] = T;
            }
        }
        if (hi < lo) {
            int T = hi;
            hi = lo;
            lo = T;
        }
        sort(a, lo0, lo);
        sort(a, lo == lo0 ? lo+1 : lo, hi0);
    }

    /**
     *
     */
    private static void sort(double a[]) {
        sort(a, 0, a.length-1);
    }

} // end of class