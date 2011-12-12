/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import distance.PixelPairs;
import java.util.StringTokenizer;
import math3d.Point3d;

public class TransformedImage {
	public InterpolatedImage orig, transform;
	FastMatrix fromOrig;
	private FastMatrix transformation;
	FastMatrix toTransform;

	public PixelPairs measure;

	/*
	 * This matrix contains the first 3 rows of the uniform matrix which
	 * transforms pixel coordinates in the original onto pixel
	 * coordinates in the "transformed" stack.
	 */
	public FastMatrix matrix;

	public TransformedImage(ImagePlus orig, ImagePlus transform) {
                this.orig = new InterpolatedImage(orig);
		this.transform = new InterpolatedImage(transform);

		fromOrig = FastMatrix.fromCalibration(orig);
		toTransform = FastMatrix.fromCalibration(transform).inverse();

		x0 = y0 = z0 = 0;
		x1 = this.orig.w; y1 = this.orig.h; z1 = this.orig.d;
		
		// Insist that both images are of the same bit depth,
		// anyway:

		if( orig.getType() != transform.getType() )
			throw new RuntimeException("TransformedImage needs two images of the same bit depth.");

                imageType = orig.getType();
		switch(imageType) {
		case ImagePlus.GRAY8:
		case ImagePlus.COLOR_256:
		case ImagePlus.GRAY16:
			break;
		default:
			throw new RuntimeException("Not yet implemented! (Bit depths != 8 or 16 in TransformedImage.)");
		}
	}
        int imageType = -1;
        
        public ImagePlus getTemplate() {
		return orig.getImage();
        }

	/* We need to know the range of values in both images for
	 * setting up the MutualInformation measure. */

	public float [] getValuesRange() {

		int origStackSize      = orig.image.getStackSize();
		int transformStackSize = transform.image.getStackSize();

		ImageStack origStack      = orig.image.getStack();
		ImageStack transformStack = transform.image.getStack();

		int origBitDepth      = orig.image.getBitDepth();
		int transformBitDepth = transform.image.getBitDepth();

		float minValue = Float.MAX_VALUE;
		float maxValue = Float.MIN_VALUE;

		int z;
		for( z = 0; z < origStackSize; ++z ) {
			if( 8 == origBitDepth) {
				byte [] pixels = (byte[])origStack.getPixels(z+1);
				for( int i = 0; i < pixels.length; ++i ) {
					int value = pixels[i] & 0xFF;
					if( value > maxValue )
						maxValue = value;
					if( value < minValue )
						minValue = value;
				}
			} else if( 16 == origBitDepth ) {
				short [] pixels = (short[])origStack.getPixels(z+1);
				for( int i = 0; i < pixels.length; ++i ) {
					short value = pixels[i];
					if( value > maxValue )
						maxValue = value;
					if( value < minValue )
						minValue = value;
				}
			}
		}
		for( z = 0; z < transformStackSize; ++z ) {
			if( 8 == transformBitDepth ) {
				byte [] pixels = (byte[])transformStack.getPixels(z+1);
				for( int i = 0; i < pixels.length; ++i ) {
					int value = pixels[i] & 0xFF;
					if( value > maxValue )
						maxValue = value;
					if( value < minValue )
						minValue = value;
				}
			} else if( 16 == transformBitDepth ) {
				short [] pixels = (short[])transformStack.getPixels(z+1);
				for( int i = 0; i < pixels.length; ++i ) {
					short value = pixels[i];
					if( value > maxValue )
						maxValue = value;
					if( value < minValue )
						minValue = value;
				}
			}
		}

		float [] result = new float[2];

		result[0] = minValue;
		result[1] = maxValue;

		return result;
	}

	public void setTransformation(FastMatrix matrix) {
		transformation = matrix.inverse();
		this.matrix = toTransform.times(transformation.times(fromOrig));
	}

	public TransformedImage resample(int factor) {
		ImagePlus resOrig, resTran;
		
		if (measure instanceof distance.MutualInformation) {
			resOrig = NaiveResampler.resampleMinEnt(orig.image, factor);
			resTran = NaiveResampler.resampleMinEnt(transform.image,
							   factor);
		} else {
			resOrig = NaiveResampler.resample(orig.image, factor);
			resTran = NaiveResampler.resample(transform.image, factor);
		}
		TransformedImage res = new TransformedImage(resOrig, resTran);
		res.transformation = transformation;
		res.measure = measure;
		res.x0 = x0 / factor;
		res.y0 = y0 / factor;
		res.z0 = z0 / factor;
		res.x1 = (x1 + factor - 1) / factor;
		res.y1 = (y1 + factor - 1) / factor;
		res.z1 = (z1 + factor - 1) / factor;
		return res;
	}

	public class Iterator implements java.util.Iterator {
		// these are the original coordinates
		public int i, j, k;
		// these are the transformed coordinates
		public double x, y, z;

		public boolean showProgress;
		public int x0, y0, z0, x1, y1, z1, xd, zd;
		protected boolean isIdentity;

		public Iterator(boolean showProgress, int x0, int y0, int z0,
				int x1, int y1, int z1) {
			this.showProgress = showProgress;
			this.x0 = x0; this.y0 = y0; this.z0 = z0;
			this.x1 = x1; this.y1 = y1; this.z1 = z1;
			xd = x1 - x0; zd = z1 - z0;
			i = x1; j = y0 - 1; k = z0;
			isIdentity = matrix.isIdentity();
		}

                public boolean hasNext() {
                        return i + 1 < x1 || j + 1 < y1 || k + 1 < z1;
                }

		private Point3d start, stop;

		/* The transformation is always linear in this class,
		   so each line in the template maps back to a line in
		   the model.  In order to reduce the calculations per
		   iteration, only two points from the template space
		   are mapped back to the model space for each line.
		   (They're calculated whenever i gets to the end of a
		   row.)  These points correspond to i = 0 (start) and
		   i = x1 (stop) and the method interpolates based on
		   these to find the other points on the line. */
                public Object next() {
                        if (++i >= x1) {
                                i = x0;
                                if (++j >= y1) {
                                        j = y0;
                                        if (++k >= z1)
                                                return null;
                                        if (showProgress)
                                                IJ.showProgress(k - z0 + 1, zd);
                                }
				if (!isIdentity) {
					matrix.apply(0, j, k);
					start = matrix.getResult();
					matrix.apply(x1, j, k);
					stop = matrix.getResult().minus(start);
				}
			}
			if (isIdentity) {
				x = i; y = j; z = k;
			} else {
				x = start.x + stop.x * i / x1;
				y = start.y + stop.y * i / x1;
				z = start.z + stop.z * i / x1;
			}
			return this;
                }

                public void remove() {
                        throw new UnsupportedOperationException();
                }
	}

        public Iterator iterator() {
                return iterator(false);
        }

        public Iterator iterator(boolean showProgress) {
                return iterator(showProgress, 0, 0, 0, orig.w, orig.h, orig.d);
        }

        public Iterator iterator(boolean showProgress,
				 int x0, int y0, int z0, int x1, int y1, int z1) {
                return new Iterator(showProgress, x0, y0, z0, x1, y1, z1);
        }

	// the bounding box for the distance calculation
	int x0, y0, z0, x1, y1, z1;

	public float getDistance() {
		measure.reset();
		Iterator iter = new Iterator(false, x0, y0, z0, x1, y1, z1);
		while (iter.next() != null) {
			float v1 = -1;
			switch(imageType) {
			case ImagePlus.GRAY8:
                        case ImagePlus.COLOR_256:
				v1 = orig.getNoInterpol(iter.i, iter.j, iter.k);
                                break;
			case ImagePlus.GRAY16:
				v1 = orig.getNoInterpolShort(iter.i, iter.j, iter.k);
                                break;
                        }
			float v2 = (float)transform.interpol.get(iter.x,
								 iter.y, iter.z);
			measure.add(v1, v2);
		}
		return measure.distance();
	}

	public ImagePlus getTransformed() {
		InterpolatedImage result = orig.cloneDimensionsOnly();

		Iterator iter = iterator();
		switch(imageType) {
		case ImagePlus.GRAY8:
		case ImagePlus.COLOR_256:
			while (iter.next() != null)
				result.set(iter.i, iter.j, iter.k,
					   transform.getNoInterpol((int)iter.x,
								   (int)iter.y, (int)iter.z));
                        break;
		case ImagePlus.GRAY16:
			while (iter.next() != null)
				result.setShort(iter.i, iter.j, iter.k,
					   transform.getNoInterpolShort((int)iter.x,
									(int)iter.y, (int)iter.z));
                        break;
                }
		result.image.setTitle("transformed");
		return result.image;
	}

	public ImagePlus getDifferenceImage() {

		int typeToCreate = -1;
		switch(imageType) {
		case ImagePlus.GRAY8:
		case ImagePlus.COLOR_256:
			typeToCreate = ImagePlus.GRAY8;
			break;
		case ImagePlus.GRAY16:
			typeToCreate = ImagePlus.GRAY16;
			break;
		}

		// FIXME: typeToCreate should depend on the measure
		// used as well as (possibly) the source image's bit
		// depth.

		InterpolatedImage result =
			InterpolatedImage.cloneDimensionsOnly(orig.image,
							      typeToCreate);

		PixelPairs differenceMeasure = null;

		if( measure instanceof distance.MutualInformation ||
			measure instanceof distance.Correlation ) {
			/* Using the MutualInformation metric for
			   difference images is impossibly slow, and
			   the correlation between two points is
			   always NaN. */
			differenceMeasure = new distance.Euclidean();
		} else {
			differenceMeasure = measure;
		}

		Iterator iter = iterator(false, x0, y0, z0, x1, y1, z1);
		while (iter.next() != null) {
			differenceMeasure.reset();
			switch(imageType) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
				differenceMeasure.add(orig.getNoInterpol(iter.i, iter.j, iter.k),
					    transform.getNoInterpol((int)iter.x,
								    (int)iter.y, (int)iter.z));
				break;
			case ImagePlus.GRAY16:
				differenceMeasure.add(orig.getNoInterpolShort(iter.i, iter.j, iter.k),
					    transform.getNoInterpolShort((int)iter.x,
									 (int)iter.y, (int)iter.z));
				break;
			}
			switch(typeToCreate) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
				result.set(iter.i, iter.j, iter.k,
					   (byte)(int)differenceMeasure.distance());
				break;
			case ImagePlus.GRAY16:
				result.setShort(iter.i, iter.j, iter.k,
						(short)differenceMeasure.distance());
				break;
			}
		}

		result.image.setTitle("difference");
		return result.image;
	}

	public void narrowBBox(int x0, int x1, int y0, int y1, int z0, int z1) {
		System.err.println("bbox: " + x0 + " " + x1 + " " + y0 + " " + y1 + " " + z0 + " " + z1);
		if (x0 < 0)
			x0 = 0;
		else if (x0 >= orig.w)
			x0 = orig.w - 1;
		if (y0 < 0)
			y0 = 0;
		else if (y0 >= orig.h)
			y0 = orig.h - 1;
		if (z0 < 0)
			z0 = 0;
		else if (z0 >= orig.d)
			z0 = orig.d - 1;
		if (x1 < 1)
			x1 = 1;
		else if (x1 > orig.w)
			x1 = orig.w;
		if (y1 < 1)
			y1 = 1;
		else if (y1 > orig.h)
			y1 = orig.h;
		if (z1 < 1)
			z1 = 1;
		else if (z1 > orig.d)
			z1 = orig.d;
		this.x0 = x0;
		this.x1 = x1;
		this.y0 = y0;
		this.y1 = y1;
		this.z0 = z0;
		this.z1 = z1;
	}

	public void narrowSearchToMaterial(int value, int border) {
		x0 = orig.w; y0 = orig.h; z0 = orig.d;
		x1 = y1 = z1 = 0;
		for (int k = 0; k < orig.d; k++)
			for (int j = 0; j < orig.h; j++)
				for (int i = 0; i < orig.w; i++) {
					int v = orig.getNoInterpol(i, j, k);
					if (v != value)
						continue;
					if (x0 > i) x0 = i;
					else if (x1 < i) x1 = i;
					if (y0 > j) y0 = j;
					else if (y1 < j) y1 = j;
					if (z0 > k) z0 = k;
					else if (z1 < k) z1 = k;
				}
		x0 -= border; y0 -= border; z0 -= border;
		x1 += border + 1; y1 += border + 1; z1 += border + 1;
		if (x0 < 0) x0 = 0;
		if (y0 < 0) y0 = 0;
		if (z0 < 0) z0 = 0;
		if (x1 > orig.w) x1 = orig.w;
		if (y1 > orig.h) y1 = orig.h;
		if (z1 > orig.d) z1 = orig.d;
	}

	/* package methods */
	int getOrig(int x, int y, int z) {
		return orig.getNoCheck(x, y, z);
	}

	float getTransformed(double x, double y, double z) {
		matrix.apply(x, y, z);
		return (float)transform.interpol.get(
			matrix.x, matrix.y, matrix.z);
	}

	float getTransformedNoInterpol(double x, double y, double z) {
		matrix.apply(x, y, z);
		return (float)transform.getNoInterpol(
			(int)Math.round(matrix.x),
			(int)Math.round(matrix.y),
			(int)Math.round(matrix.z));
	}
}
