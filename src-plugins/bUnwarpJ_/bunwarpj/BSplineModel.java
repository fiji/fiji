package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ(C).
 * Copyright (C) 2005-2010 Ignacio Arganda-Carreras and Jan Kybic 
 *
 * More information at http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

import ij.IJ;
import ij.process.ImageProcessor;

import java.util.Stack;

/*====================================================================
|   BSplineModel
\===================================================================*/

/*------------------------------------------------------------------*/
/**
 * Class for representing the images and the deformations by cubic B-splines:
 * <p>
 * <ul>
 * 		<li>Both, images and deformations, are stored in multi-resolution pyramids.</li>
 * 		<li>The pyramids are not calculated in the constructors but in the method <code>startPyramids</code>.</li>
 * 		<li>For images, even if they are set to be scaled, the original image information is stored.</li>
 * 		<li>The information corresponding to the output window size is also stored at any time.</li>
 * </ul>
 */
public class BSplineModel implements Runnable
{ /* begin class BSplineModel */

	// Some constants
	/** maximum output window dimensions */
	public static int MAX_OUTPUT_SIZE = 1024;
	
	/** minimum image size */
	private static int min_image_size = 4;
	
	/** image information (after corresponding scaling) */
	private ImageProcessor ip = null;

	// Thread
	/** thread to create the model */
	private Thread t = null;

	// Stack for the pyramid of images/coefficients
	/** stack of coefficients pyramid */
	private final Stack cpyramid   = new Stack();
	/** stack of image pyramid */
	private final Stack imgpyramid = new Stack();

	// Original image, image spline coefficients, and gradient
	/** original image, full-size without scaling */
	private double[] original_image = null;
	/** working image at maximum resolution (after scaling) */
	private double[] image = null;
	/** image spline coefficients */
	private double[] coefficient = null;

	// Current image (the size might be different from the original)
	/** current image (at the current resolution level) */
	private double[] currentImage;
	/** current image spline coefficients */
	private double[] currentCoefficient;
	/** current image width */
	private int      currentWidth;
	/** current image height */
	private int      currentHeight;

	// Size and other information
	/** working image/coefficients width (after scaling) */
	private int     width;
	/** working image/coefficients height (after scaling) */
	private int     height;

	/** resolution pyramid depth */
	private int     pyramidDepth;
	/** current pyramid depth*/
	private int     currentDepth;
	/** smallest image width */
	private int     smallestWidth;
	/** smallest image height */
	private int     smallestHeight;
	/** flag to check target image */
	private boolean isTarget;
	/** flag to check if the coefficients are mirrored */
	private boolean coefficientsAreMirrored;
	
	/** sub-sampling factor at highest image resolution level (always a power of 2) */
	private int maxImageSubsamplingFactor = 1;

	// Some variables to speedup interpolation
	// All these information is set through prepareForInterpolation()

	// Point to interpolate	
	/** x component of the point to interpolate */
	//private double   x;
	/** y component of the point to interpolate */
	//private double   y;

	// Indexes related
	/** x index */
	public  int      xIndex[];
	/** y index */
	public  int      yIndex[];

	// Weights of the splines related
	/** x component of the weight of the spline */
	private double   xWeight[];
	/** y component of the weight of the spline */
	private double   yWeight[];

	// Weights of the derivatives splines related
	/** x component of the weight of derivative spline */
	private double   dxWeight[];
	/** y component of the weight of derivative spline */
	private double   dyWeight[];

	// Weights of the second derivatives splines related
	/** x component of the weight of second derivative spline */
	private double   d2xWeight[];
	/** y component of the weight of second derivative spline */
	private double   d2yWeight[];

	/** Interpolation source (current or original) */
	private boolean  fromCurrent;

	// Size of the image used for the interpolation
	/** width of the image used for the interpolation */
	private int      widthToUse;
	/** height of the image used for the interpolation */
	private int      heightToUse;
	
	// Subsampled output image
	/** flag for using subsampled output image */
	private boolean bSubsampledOutput = false;
	/** width of the subsampled output image */
	private int subWidth = 0;
	/** height of the subsampled output image */
	private int subHeight = 0;
	/** subsampled output image B-spline coefficients */
	private double[] subCoeffs = null;
	/** subsampled output image */
	private double[] subImage = null;

	// Some variables to speedup interpolation (precomputed)
	// All these information is set through prepareForInterpolation()
	// Indexes related
	/** precomputed x index */
	public  int      prec_xIndex[][];
	/** precomputed y index */
	public  int      prec_yIndex[][];
	// Weights of the splines related
	/** precomputed x component of the weight of the spline */
	private double   prec_xWeight[][];
	/** precomputed y component of the weight of the spline */
	private double   prec_yWeight[][];
	// Weights of the derivatives splines related
	/** precomputed x component of the weight of derivative spline */
	private double   prec_dxWeight[][];
	/** precomputed y component of the weight of derivative spline */
	private double   prec_dyWeight[][];
	// Weights of the second derivatives splines related
	/** precomputed x component of the weight of second derivative spline */
	private double   prec_d2xWeight[][];
	/** precomputed y component of the weight of second derivative spline */
	private double   prec_d2yWeight[][];

	/** original image width (at full-resolution, without scaling) */
	private int originalWidth = 0;
	/** original image height (at full-resolution, without scaling) */
	private int originalHeight = 0;

	/*....................................................................
       Public methods
    ....................................................................*/

	//------------------------------------------------------------------
	/**
	 * Create image model for image processor: image and coefficient pyramid.
	 * When calling this constructor, the thread is not started, to do so, 
	 * startPyramids needs to be called.
	 *
	 * @param ip image pointer (ImageProcessor)
	 * @param isTarget enables the computation of the derivative or not
	 * @param maxImageSubsamplingFactor sub-sampling factor at highest resolution level
	 */
	public BSplineModel (
			final ImageProcessor ip,
			final boolean isTarget,
			final int maxImageSubsamplingFactor)
	{	
		this.ip = ip;
		
		// Get image information
		this.isTarget = isTarget;
		this.maxImageSubsamplingFactor = maxImageSubsamplingFactor;
		width         = ip.getWidth();
		height        = ip.getHeight();
		coefficientsAreMirrored = true;

		// Resize the speedup arrays
		xIndex    = new int[4];
		yIndex    = new int[4];
		xWeight   = new double[4];
		yWeight   = new double[4];
		dxWeight  = new double[4];
		dyWeight  = new double[4];
		d2xWeight = new double[4];
		d2yWeight = new double[4];
	} // end BSplineModel

	//------------------------------------------------------------------
	/**
	 * Create image model without image pixel information (for landmark only 
	 * registration): empty image and full coefficient pyramid.
	 * When calling this constructor, the thread is not started, to do so, 
	 * startPyramids needs to be called.
	 *
	 * @param width image width 
	 * @param height image height
	 * @param maxImageSubsamplingFactor sub-sampling factor at highest resolution level
	 */
	public BSplineModel (
			final int width,
			final int height,
			final int maxImageSubsamplingFactor)
	{	
		this.ip = null;
		
		// Get image information
		this.isTarget = false;
		this.maxImageSubsamplingFactor = maxImageSubsamplingFactor;
		this.width = this.originalWidth = width;
		this.height = this.originalHeight = height;
		coefficientsAreMirrored = true;

		// Resize the speedup arrays
		xIndex    = new int[4];
		yIndex    = new int[4];
		xWeight   = new double[4];
		yWeight   = new double[4];
		dxWeight  = new double[4];
		dyWeight  = new double[4];
		d2xWeight = new double[4];
		d2yWeight = new double[4];
	} // end BSplineModel	
	
	//------------------------------------------------------------------
	/**
	 * The same as before, but take the image from an array.
	 *
	 * @param img image in a double array
	 * @param isTarget enables the computation of the derivative or not
	 */
	public BSplineModel (
			final double [][]img,
			final boolean isTarget)
	{
		// Initialize thread
		t = new Thread(this);
		t.setDaemon(true);

		// Get image information
		this.isTarget = isTarget;
		width         = img[0].length;
		height        = img.length;
		coefficientsAreMirrored = true;

		// Copy the pixel array
		int k = 0;
		this.image = new double[width * height];
		for (int y = 0; (y < height); y++)
			for (int x = 0; (x < width); x++, k++)
				this.image[k] = img[y][x];

		this.original_image = this.image;
		this.originalWidth = this.width;
		this.originalHeight = this.height;
		
		// Resize the speedup arrays
		xIndex    = new int[4];
		yIndex    = new int[4];
		xWeight   = new double[4];
		yWeight   = new double[4];
		dxWeight  = new double[4];
		dyWeight  = new double[4];
		d2xWeight = new double[4];
		d2yWeight = new double[4];
	} // end BSplineModel

	//------------------------------------------------------------------
	/**
	 * Initialize the B-spline model from a set of coefficients.
	 *
	 * @param c Set of B-spline coefficients
	 */
	public BSplineModel (final double [][]c)
	{
		// Get the size of the input array
		this.currentHeight      = height      = c.length;
		this.currentWidth       = width       = c[0].length;
		this.coefficientsAreMirrored = false;

		// Copy the array of coefficients
		coefficient = new double[height*width];
		int k=0;
		for (int y=0; y<height; y++, k+= width)
			System.arraycopy(c[y], 0, coefficient, k, width);

		// Resize the speedup arrays
		xIndex    = new int[4];
		yIndex    = new int[4];
		xWeight   = new double[4];
		yWeight   = new double[4];
		dxWeight  = new double[4];
		dyWeight  = new double[4];
		d2xWeight = new double[4];
		d2yWeight = new double[4];
	} // end BSplineModel

	//------------------------------------------------------------------
	/**
	 * Initialize the B-spline model from a set of coefficients.
	 * The same as the previous function but now the coefficients
	 * are in a single row.
	 *
	 * @param c Set of B-spline coefficients
	 * @param Ydim Y-dimension of the set of coefficients
	 * @param Xdim X-dimension of the set of coefficients
	 * @param offset Offset of the beginning of the array with respect to the origin of c
	 */
	public BSplineModel (
			final double []c,
			final int Ydim,
			final int Xdim,
			final int offset)
	{
		// Get the size of the input array
		currentHeight      = height      = Ydim;
		currentWidth       = width       = Xdim;
		coefficientsAreMirrored = false;

		// Copy the array of coefficients
		coefficient=new double[height*width];
		System.arraycopy(c, offset, coefficient, 0, height*width);

		// Resize the speedup arrays
		xIndex    = new int[4];
		yIndex    = new int[4];
		xWeight   = new double[4];
		yWeight   = new double[4];
		dxWeight  = new double[4];
		dyWeight  = new double[4];
		d2xWeight = new double[4];
		d2yWeight = new double[4];
	}// end BSplineModel    

	//------------------------------------------------------------------
	/**
	 * Start coefficient and image pyramids
	 */
	public void startPyramids()
	{			
		this.subWidth = this.width;
		this.subHeight = this.height;
		
		// Output window must have a maximum size
		if(this.width > BSplineModel.MAX_OUTPUT_SIZE 
			|| this.height > BSplineModel.MAX_OUTPUT_SIZE)
		{
			this.bSubsampledOutput = true;			
			// Calculate subsampled dimensions
			do{
				this.subWidth /= 2;
				this.subHeight /= 2;
			}while(this.subWidth > BSplineModel.MAX_OUTPUT_SIZE 
					|| this.subHeight > BSplineModel.MAX_OUTPUT_SIZE);	
			
			//IJ.log("subWidth =" + this.subWidth);
		}
		else
			this.bSubsampledOutput = false;
			
		// Initialize thread
		t = new Thread(this);
		t.setDaemon(true);
		// And start it
		t.start();
	} // end startPyramids
	
	//------------------------------------------------------------------
	/**
	 * Clear the pyramid.
	 */
	public void clearPyramids ()
	{
		cpyramid.removeAllElements();
		imgpyramid.removeAllElements();
	} /* end clearPyramid */

	
	//------------------------------------------------------------------
	/**
	 * Get current height.
	 *
	 * @return the current height of the image/coefficients
	 */
	public int getCurrentHeight() {return currentHeight;}

	//------------------------------------------------------------------
	/**
	 * Get current image.
	 *
	 * @return the current image of the image/coefficients
	 */
	public double[] getCurrentImage() {return currentImage;}

	//------------------------------------------------------------------
	/**
	 * Get current width.
	 *
	 * @return the current width of the image/coefficients
	 */
	public int getCurrentWidth () {return currentWidth;}

	//------------------------------------------------------------------
	/**
	 * Get factor height.
	 *
	 * @return the relationship between the current size of the image
	 *         and the original size
	 */
	public double getFactorHeight () {return (double)currentHeight/height;}

	//------------------------------------------------------------------
	/**
	 * Get fact or width.
	 *
	 * @return the relationship between the current size of the image
	 *         and the original size.
	 */
	public double getFactorWidth () {return (double)currentWidth/width;}

	//------------------------------------------------------------------
	/**
	 * Get current depth.
	 *
	 * @return the current depth of the image/coefficients
	 */
	public int getCurrentDepth() {return currentDepth;}

	//------------------------------------------------------------------
	/**
	 * Get height.
	 *
	 * @return the full-size image height.
	 */
	public int getHeight () {return(height);}

	//------------------------------------------------------------------
	/**
	 * Get image (at the maximum resolution size determined by the scaling).
	 *
	 * @return the less scaled image.
	 */
	public double[] getImage () {return image;}
	
	//------------------------------------------------------------------
	/**
	 * Get original image.
	 *
	 * @return the original full-size image.
	 */
	public double[] getOriginalImage () {return original_image;}
	

	//------------------------------------------------------------------
	/**
	 * Get original image width.
	 *
	 * @return the original full-size image width.
	 */
	public int getOriginalImageWidth () {return originalWidth;}
	//------------------------------------------------------------------
	/**
	 * Get original image height.
	 *
	 * @return the original full-size image height.
	 */
	public int getOriginalImageHeight () {return originalHeight;}
	
	//------------------------------------------------------------------
	/**
	 * Get subsampled output image.
	 *
	 * @return the subsumpled (to show) output image.
	 */
	public double[] getSubImage () {return this.subImage;}
	//------------------------------------------------------------------
	/**
	 * Get b-spline coefficients.
	 *
	 * @return the full-size B-spline coefficients
	 */
	public double[] getCoefficients () {return this.coefficient;}

	//------------------------------------------------------------------
	/**
	 * Get the pixel value from the image pyramid.
	 *
	 * @param x x-coordinate of the pixel
	 * @param y y-coordinate of the pixel
	 * @return pixel value
	 */
	public double getPixelValFromPyramid(
			int x,   // Pixel location
			int y)
	{
		return currentImage[y*currentWidth+x];
	}

	//------------------------------------------------------------------
	/**
	 * Get pyramid depth.
	 *
	 * @return the depth of the image pyramid. A depth 1 means
	 *         that one coarse resolution level is present in the stack.
	 *         The full-size level is not placed on the stack
	 */
	public int getPyramidDepth () {return(pyramidDepth);}

	//------------------------------------------------------------------
	/**
	 * Get smallest height.
	 *
	 * @return the height of the smallest image in the pyramid
	 */
	public int getSmallestHeight () {return(smallestHeight);}

	//------------------------------------------------------------------
	/**
	 * Get smallest width.
	 *
	 * @return the width of the smallest image in the pyramid
	 */
	public int getSmallestWidth () {return(smallestWidth);}

	//------------------------------------------------------------------
	/**
	 * Get thread.
	 *
	 * @return the thread associated
	 */
	public Thread getThread () {return(t);}

	//------------------------------------------------------------------
	/**
	 * Get width.
	 *
	 * @return the full-size image width
	 */
	public int getWidth () {return(width);}

	//------------------------------------------------------------------
	/**
	 * Get weight dx.
	 *
	 * @return the weight of the coefficient l,m (yWeight, dxWeight) in the
	 *         image interpolation
	 */
	public double getWeightDx(int l, int m) {return yWeight[l]*dxWeight[m];}

	//------------------------------------------------------------------
	/**
	 * Get weight dxdx.
	 *
	 * @return the weight of the coefficient l,m (yWeight, d2xWeight) in the
	 *         image interpolation
	 */
	public double getWeightDxDx(int l, int m) {return yWeight[l]*d2xWeight[m];}

	//------------------------------------------------------------------
	/**
	 * Get weight dxdy.
	 *
	 * @return the weight of the coefficient l,m (dyWeight, dxWeight) in the
	 * image interpolation
	 */
	public double getWeightDxDy(int l, int m) {return dyWeight[l]*dxWeight[m];}

	//------------------------------------------------------------------
	/**
	 * Get weight dy.
	 *
	 * @return the weight of the coefficient l,m (dyWeight, xWeight) in the
	 *         image interpolation
	 */
	public double getWeightDy(int l, int m) {return dyWeight[l]*xWeight[m];}

	//------------------------------------------------------------------
	/**
	 * Get weight dydy.
	 *
	 * @return the weight of the coefficient l,m (d2yWeight, xWeight) in the
	 *         image interpolation
	 */
	public double getWeightDyDy(int l, int m) {return d2yWeight[l]*xWeight[m];}

	//------------------------------------------------------------------
	/**
	 * Get image coefficient weight.
	 *
	 * @return the weight of the coefficient l,m (yWeight, xWeight) in the
	 *         image interpolation
	 */
	public double getWeightI(int l, int m) {return yWeight[l]*xWeight[m];}

	//------------------------------------------------------------------
	/**
	 * There are two types of interpolation routines. Those that use
	 * precomputed weights and those that don't.
	 * An example of use of the ones without precomputation is the
	 * following:
	 *    // Set of B-spline coefficients
	 *    double [][]c;
	 *
	 *    // Set these coefficients to an interpolator
	 *    BSplineModel sw = new BSplineModel(c);
	 *
	 *    // Compute the transformation mapping
	 *    for (int v=0; v<ImageHeight; v++) {
	 *       final double tv = (double)(v * intervals) / (double)(ImageHeight - 1) + 1.0F;
	 *       for (int u = 0; u<ImageeWidth; u++) {
	 *          final double tu = (double)(u * intervals) / (double)(ImageWidth - 1) + 1.0F;
	 *          sw.prepareForInterpolation(tu, tv, ORIGINAL);
	 *          interpolated_val[v][u] = sw.interpolateI();
	 *       }
	 */
	//------------------------------------------------------------------
	//------------------------------------------------------------------
	/**
	 * Interpolate the X and Y derivatives of the image at a
	 * given point.
	 *
	 * @param D output, interpolation the X and Y derivatives of the image
	 */
	public void interpolateD(double []D)
	{
		// Only SplineDegree=3 is implemented
		D[0]=D[1]=0.0F;
		for (int j = 0; j<4; j++) {
			double sx=0.0F, sy=0.0F;
			int iy=yIndex[j];
			if (iy!=-1) {
				int p=iy*widthToUse;
				for (int i=0; i<4; i++) {
					int ix=xIndex[i];
					if (ix!=-1) {
						double c;
						if (fromCurrent) c=currentCoefficient[p + ix];
						else             c=coefficient[p + ix];
						sx += dxWeight[i]*c;
						sy +=  xWeight[i]*c;
					}
				}
				D[0]+= yWeight[j] * sx;
				D[1]+=dyWeight[j] * sy;
			}
		}
	} /* end Interpolate D */

	//------------------------------------------------------------------
	/**
	 * Interpolate the XY, XX and YY derivatives of the image at a
	 * given point.
	 *
	 * @param D2 output, interpolation of the XY, XX and YY derivatives of the image
	 */
	public void interpolateD2 (double []D2)
	{
		// Only SplineDegree=3 is implemented
		D2[0]=D2[1]=D2[2]=0.0F;
		for (int j = 0; j<4; j++) {
			double sxy=0.0F, sxx=0.0F, syy=0.0F;
			int iy=yIndex[j];
			if (iy!=-1) {
				int p=iy*widthToUse;
				for (int i=0; i<4; i++) {
					int ix=xIndex[i];
					if (ix!=-1) {
						double c;
						if (fromCurrent) c=currentCoefficient[p + ix];
						else             c=coefficient[p + ix];
						sxy +=  dxWeight[i]*c;
						sxx += d2xWeight[i]*c;
						syy +=   xWeight[i]*c;
					}
				}
				D2[0]+= dyWeight[j] * sxy;
				D2[1]+=  yWeight[j] * sxx;
				D2[2]+=d2yWeight[j] * syy;
			}
		}
	} /* end Interpolate dxdy, dxdx and dydy */

	//------------------------------------------------------------------
	/**
	 * Interpolate the X derivative of the image at a given point.
	 *
	 * @return dx interpolation
	 */
	public double interpolateDx () {
		// Only SplineDegree=3 is implemented
		double ival=0.0F;
		for (int j = 0; j<4; j++) {
			double s=0.0F;
			int iy=yIndex[j];
			if (iy!=-1) {
				int p=iy*widthToUse;
				for (int i=0; i<4; i++) {
					int ix=xIndex[i];
					if (ix!=-1)
						if (fromCurrent) s += dxWeight[i]*currentCoefficient[p + ix];
						else             s += dxWeight[i]*coefficient[p + ix];
				}
				ival+=yWeight[j] * s;
			}
		}
		return ival;
	} /* end Interpolate Dx */

	//------------------------------------------------------------------
	/**
	 * Interpolate the X derivative of the image at a given point.
	 *
	 * @return dxdx interpolation
	 */
	public double interpolateDxDx ()
	{
		// Only SplineDegree=3 is implemented
		double ival=0.0F;
		for (int j = 0; j<4; j++) {
			double s=0.0F;
			int iy=yIndex[j];
			if (iy!=-1) {
				int p=iy*widthToUse;
				for (int i=0; i<4; i++) {
					int ix=xIndex[i];
					if (ix!=-1)
						if (fromCurrent) s += d2xWeight[i]*currentCoefficient[p + ix];
						else             s += d2xWeight[i]*coefficient[p + ix];
				}
				ival+=yWeight[j] * s;
			}
		}
		return ival;
	} /* end Interpolate DxDx */

	//------------------------------------------------------------------
	/**
	 * Interpolate the X derivative of the image at a given point.
	 *
	 * @return dxdy interpolation
	 */
	public double interpolateDxDy () {
		// Only SplineDegree=3 is implemented
		double ival=0.0F;
		for (int j = 0; j<4; j++) {
			double s=0.0F;
			int iy=yIndex[j];
			if (iy!=-1) {
				int p=iy*widthToUse;
				for (int i=0; i<4; i++) {
					int ix=xIndex[i];
					if (ix!=-1)
						if (fromCurrent) s += dxWeight[i]*currentCoefficient[p + ix];
						else             s += dxWeight[i]*coefficient[p + ix];
				}
				ival+=dyWeight[j] * s;
			}
		}
		return ival;
	} /* end Interpolate DxDy */

	//------------------------------------------------------------------
	/**
	 * Interpolate the Y derivative of the image at a given point.
	 *
	 * @return dy interpolation
	 */
	public double interpolateDy ()
	{
		// Only SplineDegree=3 is implemented
		double ival=0.0F;
		for (int j = 0; j<4; j++) {
			double s=0.0F;
			int iy=yIndex[j];
			if (iy!=-1) {
				int p=iy*widthToUse;
				for (int i=0; i<4; i++) {
					int ix=xIndex[i];
					if (ix!=-1)
						if (fromCurrent) s += xWeight[i]*currentCoefficient[p + ix];
						else             s += xWeight[i]*coefficient[p + ix];
				}
				ival+=dyWeight[j] * s;
			}
		}
		return ival;
	} /* end Interpolate Dy */

	//------------------------------------------------------------------
	/**
	 * Interpolate the X derivative of the image at a given point.
	 *
	 * @return dydy interpolation
	 */
	public double interpolateDyDy()
	{
		// Only SplineDegree=3 is implemented
		double ival=0.0F;
		for (int j = 0; j<4; j++) {
			double s=0.0F;
			int iy=yIndex[j];
			if (iy!=-1) {
				int p=iy*widthToUse;
				for (int i=0; i<4; i++) {
					int ix=xIndex[i];
					if (ix!=-1)
						if (fromCurrent) s += xWeight[i]*currentCoefficient[p + ix];
						else             s += xWeight[i]*coefficient[p + ix];
				}
				ival+=d2yWeight[j] * s;
			}
		}
		return ival;
	} /* end Interpolate DyDy */

	//------------------------------------------------------------------
	/**
	 * Interpolate the image at a given point.
	 *
	 * @return image interpolation
	 */
	public double interpolateI ()
	{
		// Only SplineDegree=3 is implemented
		double ival=0.0F;
		for (int j = 0; j<4; j++) {
			double s=0.0F;
			int iy=yIndex[j];
			if (iy!=-1) {
				int p=iy*widthToUse;
				for (int i=0; i<4; i++) {
					int ix=xIndex[i];
					if (ix!=-1)
						if (fromCurrent) s += xWeight[i]*currentCoefficient[p + ix];
						else             s += xWeight[i]*coefficient[p + ix];
				}
				ival+=yWeight[j] * s;
			}
		}
		return ival;
	} /* end Interpolate Image */

	//------------------------------------------------------------------
	/**
	 * Check if the coefficients pyramid is empty.
	 *
	 * @return true when the coefficients pyramid is empty
	 *         false if not
	 */
	public boolean isFinest() {return cpyramid.isEmpty();}

	//------------------------------------------------------------------
	/**
	 * Pop one element from the coefficients and image pyramids.
	 */
	public void popFromPyramid()
	{
		// Pop coefficients
		if (cpyramid.isEmpty()) 
		{
			currentWidth       = width;
			currentHeight      = height;
			currentCoefficient = coefficient;
		} 
		else 
		{
			currentWidth       = ((Integer)cpyramid.pop()).intValue();
			currentHeight      = ((Integer)cpyramid.pop()).intValue();
			currentCoefficient = (double [])cpyramid.pop();
		}

		if (currentDepth > 0) 
			currentDepth--;

		// Pop image
		if (isTarget && !imgpyramid.isEmpty()) 
		{
			if (currentWidth != ((Integer)imgpyramid.pop()).intValue())
				System.out.println("I cannot understand");
			if (currentHeight != ((Integer)imgpyramid.pop()).intValue())
				System.out.println("I cannot understand");
			currentImage = (double [])imgpyramid.pop();
		} else currentImage = image;
	}
	//------------------------------------------------------------------
	/**
	 * fromCurrent=true  --> The interpolation is prepared to be done
	 *                       from the current image in the pyramid.
	 * fromCurrent=false --> The interpolation is prepared to be done
	 *                       from the original image.
	 *
	 * @param x x- point coordinate
	 * @param y y- point coordinate
	 * @param fromCurrent flag to determine the image to do the interpolation from
	 */
	public void prepareForInterpolation(
			double x,
			double y,
			boolean fromCurrent)
	{
		// Remind this point for interpolation
		//this.x = x;
		//this.y = y;
		this.fromCurrent = fromCurrent;

		if (fromCurrent)
		{
			widthToUse = currentWidth;
			heightToUse = currentHeight;
		}
		else
		{
			widthToUse = width;
			heightToUse = height;
		}

		int ix=(int)x;
		int iy=(int)y;

		int twiceWidthToUse =2*widthToUse;
		int twiceHeightToUse=2*heightToUse;

		// Set X indexes
		// p is the index of the rightmost influencing spline
		int p = (0.0 <= x) ? (ix + 2) : (ix + 1);
		for (int k = 0; k<4; p--, k++) {
			if (coefficientsAreMirrored) {
				int q = (p < 0) ? (-1 - p) : (p);
				if (twiceWidthToUse <= q) q -= twiceWidthToUse * (q / twiceWidthToUse);
				xIndex[k] = (widthToUse <= q) ? (twiceWidthToUse - 1 - q) : (q);
			} else
				xIndex[k] = (p<0 || p>=widthToUse) ? (-1):(p);
		}

		// Set Y indexes
		p = (0.0 <= y) ? (iy + 2) : (iy + 1);
		for (int k = 0; k<4; p--, k++) {
			if (coefficientsAreMirrored) {
				int q = (p < 0) ? (-1 - p) : (p);
				if (twiceHeightToUse <= q) q -= twiceHeightToUse * (q / twiceHeightToUse);
				yIndex[k] = (heightToUse <= q) ? (twiceHeightToUse - 1 - q) : (q);
			} else
				yIndex[k] = (p<0 || p>=heightToUse) ? (-1):(p);
		}

		// Compute how much the sample depart from an integer position
		double ex = x - ((0.0 <= x) ? (ix) : (ix - 1));
		double ey = y - ((0.0 <= y) ? (iy) : (iy - 1));

		// Set X weights for the image and derivative interpolation
		double s = 1.0F - ex;
		dxWeight[0] = 0.5F * ex * ex;
		xWeight[0]  = ex * dxWeight[0] / 3.0F; // Bspline03(x-ix-2)
		dxWeight[3] = -0.5F * s * s;
		xWeight[3]  = s * dxWeight[3] / -3.0F; // Bspline03(x-ix+1)
		dxWeight[1] = 1.0F - 2.0F * dxWeight[0] + dxWeight[3];
		//xWeight[1]  = 2.0F / 3.0F + (1.0F + ex) * dxWeight[3]; // Bspline03(x-ix-1);
		xWeight[1]  = MathTools.Bspline03(x-ix-1);
		dxWeight[2] = 1.5F * ex * (ex - 4.0F/ 3.0F);
		xWeight[2]  = 2.0F / 3.0F - (2.0F - ex) * dxWeight[0]; // Bspline03(x-ix)

		d2xWeight[0] = ex;
		d2xWeight[1] = s-2*ex;
		d2xWeight[2] = ex-2*s;
		d2xWeight[3] = s;

		// Set Y weights for the image and derivative interpolation
		double t = 1.0F - ey;
		dyWeight[0] = 0.5F * ey * ey;
		yWeight[0]  = ey * dyWeight[0] / 3.0F;
		dyWeight[3] = -0.5F * t * t;
		yWeight[3]  = t * dyWeight[3] / -3.0F;
		dyWeight[1] = 1.0F - 2.0F * dyWeight[0] + dyWeight[3];
		yWeight[1]  = 2.0F / 3.0F + (1.0F + ey) * dyWeight[3];
		dyWeight[2] = 1.5F * ey * (ey - 4.0F/ 3.0F);
		yWeight[2]  = 2.0F / 3.0F - (2.0F - ey) * dyWeight[0];

		d2yWeight[0] = ey;
		d2yWeight[1] = t-2*ey;
		d2yWeight[2] = ey-2*t;
		d2yWeight[3] = t;
	} /* prepareForInterpolation */
	
	//------------------------------------------------------------------
	/**
	 * Prepare for interpolation and interpolate 
	 * 
	 * fromSub = true --> The interpolation is done from the subsampled
	 *                    version of the image
	 * else:
	 * 
	 * fromCurrent=true  --> The interpolation is done
	 *                       from the current image in the pyramid.
	 * fromCurrent=false --> The interpolation is done
	 *                       from the original image.
	 *
	 * @param x x- point coordinate
	 * @param y y- point coordinate
	 * @param fromSub flat to determine to do the interpolation from the subsampled version of the image 
	 * @param fromCurrent flag to determine the image to do the interpolation from
	 * 		   interpolated value
	 */
	public double prepareForInterpolationAndInterpolateI(
			double x,
			double y,
			boolean fromSub,
			boolean fromCurrent)
	{

		int widthToUse = 0;
		int heightToUse = 0;
		final int[] xIndex = new int[4];
		final int[] yIndex = new int[4];
		final double[] xWeight = new double[4];
		final double[] dxWeight = new double[4];
		//double[] d2xWeight = new double[4];
		final double[] yWeight = new double[4];
		final double[] dyWeight = new double[4];
		//double[] d2yWeight = new double[4];
		
		if (fromSub && this.subCoeffs != null)
		{
			widthToUse = this.subWidth;
			heightToUse = this.subHeight;
		}
		else if (fromCurrent)
		{
			widthToUse = currentWidth;
			heightToUse = currentHeight;
		}
		else 
		{
			widthToUse = width;
			heightToUse = height;
		}

		int ix=(int)x;
		int iy=(int)y;

		int twiceWidthToUse =2*widthToUse;
		int twiceHeightToUse=2*heightToUse;

		// Set X indexes
		// p is the index of the rightmost influencing spline
		int p = (0.0 <= x) ? (ix + 2) : (ix + 1);
		for (int k = 0; k<4; p--, k++) {
			if (coefficientsAreMirrored) {
				int q = (p < 0) ? (-1 - p) : (p);
				if (twiceWidthToUse <= q) q -= twiceWidthToUse * (q / twiceWidthToUse);
				xIndex[k] = (widthToUse <= q) ? (twiceWidthToUse - 1 - q) : (q);
			} else
				xIndex[k] = (p<0 || p>=widthToUse) ? (-1):(p);
		}

		// Set Y indexes
		p = (0.0 <= y) ? (iy + 2) : (iy + 1);
		for (int k = 0; k<4; p--, k++) {
			if (coefficientsAreMirrored) {
				int q = (p < 0) ? (-1 - p) : (p);
				if (twiceHeightToUse <= q) q -= twiceHeightToUse * (q / twiceHeightToUse);
				yIndex[k] = (heightToUse <= q) ? (twiceHeightToUse - 1 - q) : (q);
			} else
				yIndex[k] = (p<0 || p>=heightToUse) ? (-1):(p);
		}

		// Compute how much the sample depart from an integer position
		double ex = x - ((0.0 <= x) ? (ix) : (ix - 1));
		double ey = y - ((0.0 <= y) ? (iy) : (iy - 1));

		// Set X weights for the image and derivative interpolation
		double s = 1.0F - ex;
		dxWeight[0] = 0.5F * ex * ex;
		xWeight[0]  = ex * dxWeight[0] / 3.0F; // Bspline03(x-ix-2)
		dxWeight[3] = -0.5F * s * s;
		xWeight[3]  = s * dxWeight[3] / -3.0F; // Bspline03(x-ix+1)
		dxWeight[1] = 1.0F - 2.0F * dxWeight[0] + dxWeight[3];
		//xWeight[1]  = 2.0F / 3.0F + (1.0F + ex) * dxWeight[3]; // Bspline03(x-ix-1);
		xWeight[1]  = MathTools.Bspline03(x-ix-1);
		dxWeight[2] = 1.5F * ex * (ex - 4.0F/ 3.0F);
		xWeight[2]  = 2.0F / 3.0F - (2.0F - ex) * dxWeight[0]; // Bspline03(x-ix)

		//d2xWeight[0] = ex;
		//d2xWeight[1] = s-2*ex;
		//d2xWeight[2] = ex-2*s;
		//d2xWeight[3] = s;

		// Set Y weights for the image and derivative interpolation
		double t = 1.0F - ey;
		dyWeight[0] = 0.5F * ey * ey;
		yWeight[0]  = ey * dyWeight[0] / 3.0F;
		dyWeight[3] = -0.5F * t * t;
		yWeight[3]  = t * dyWeight[3] / -3.0F;
		dyWeight[1] = 1.0F - 2.0F * dyWeight[0] + dyWeight[3];
		yWeight[1]  = 2.0F / 3.0F + (1.0F + ey) * dyWeight[3];
		dyWeight[2] = 1.5F * ey * (ey - 4.0F/ 3.0F);
		yWeight[2]  = 2.0F / 3.0F - (2.0F - ey) * dyWeight[0];

		//d2yWeight[0] = ey;
		//d2yWeight[1] = t-2*ey;
		//d2yWeight[2] = ey-2*t;
		//d2yWeight[3] = t;
		
		// Only SplineDegree=3 is implemented
		double ival=0.0F;
		for (int j = 0; j<4; j++) {
			s=0.0F;
			iy=yIndex[j];
			if (iy!=-1) {
				p = iy*widthToUse;
				for (int i=0; i<4; i++) {
					ix=xIndex[i];
					if (ix!=-1)
					{
						if (fromSub && this.subCoeffs != null)  
							s += xWeight[i]*this.subCoeffs[p + ix];
						else if (fromCurrent)
							s += xWeight[i]*currentCoefficient[p + ix];							
						else             
							s += xWeight[i]*coefficient[p + ix];
					}
				}
				ival+=yWeight[j] * s;
			}
		}
		return ival;
	} /* prepareForInterpolationAndInterpolateI */

	
	//------------------------------------------------------------------
	/**
	 * Prepare for interpolation and interpolate the image value and its
	 * derivatives
	 * 
	 * fromSub = true --> The interpolation is done from the subsampled
	 *                    version of the image
	 * else:
	 * 
	 * fromCurrent=true  --> The interpolation is done
	 *                       from the current image in the pyramid.
	 * fromCurrent=false --> The interpolation is done
	 *                       from the original image.
	 *
	 * @param x x- point coordinate
	 * @param y y- point coordinate
	 * @param D output, interpolation the X and Y derivatives of the image
	 * @param fromSub flat to determine to do the interpolation from the subsampled version of the image 
	 * @param fromCurrent flag to determine the image to do the interpolation from
	 * 		   interpolated value
	 */
	public double prepareForInterpolationAndInterpolateIAndD(
			double x,
			double y,
			double D[],
			boolean fromSub,
			boolean fromCurrent)
	{

		int widthToUse = 0;
		int heightToUse = 0;
		final int[] xIndex = new int[4];
		final int[] yIndex = new int[4];
		final double[] xWeight = new double[4];
		final double[] dxWeight = new double[4];
		//double[] d2xWeight = new double[4];
		final double[] yWeight = new double[4];
		final double[] dyWeight = new double[4];
		//double[] d2yWeight = new double[4];
		
		if (fromSub && this.subCoeffs != null)
		{
			widthToUse = this.subWidth;
			heightToUse = this.subHeight;
		}
		else if (fromCurrent)
		{
			widthToUse = currentWidth;
			heightToUse = currentHeight;
		}
		else 
		{
			widthToUse = width;
			heightToUse = height;
		}

		// integer x and y
		int ix = (int)x;
		int iy = (int)y;

		int twiceWidthToUse  = 2 * widthToUse;
		int twiceHeightToUse = 2 * heightToUse;

		// Set X indexes
		// p is the index of the rightmost influencing spline
		int p = (0.0 <= x) ? (ix + 2) : (ix + 1);
		for (int k = 0; k<4; p--, k++) {
			if (coefficientsAreMirrored) {
				int q = (p < 0) ? (-1 - p) : (p);
				if (twiceWidthToUse <= q) q -= twiceWidthToUse * (q / twiceWidthToUse);
				xIndex[k] = (widthToUse <= q) ? (twiceWidthToUse - 1 - q) : (q);
			} else
				xIndex[k] = (p<0 || p>=widthToUse) ? (-1):(p);
		}

		// Set Y indexes
		p = (0.0 <= y) ? (iy + 2) : (iy + 1);
		for (int k = 0; k<4; p--, k++) {
			if (coefficientsAreMirrored) {
				int q = (p < 0) ? (-1 - p) : (p);
				if (twiceHeightToUse <= q) q -= twiceHeightToUse * (q / twiceHeightToUse);
				yIndex[k] = (heightToUse <= q) ? (twiceHeightToUse - 1 - q) : (q);
			} else
				yIndex[k] = (p<0 || p>=heightToUse) ? (-1):(p);
		}

		// Compute how much the sample depart from an integer position
		double ex = x - ((0.0 <= x) ? (ix) : (ix - 1));
		double ey = y - ((0.0 <= y) ? (iy) : (iy - 1));

		// Set X weights for the image and derivative interpolation
		double s = 1.0F - ex;
		dxWeight[0] = 0.5F * ex * ex;
		xWeight[0]  = ex * dxWeight[0] / 3.0F; // Bspline03(x-ix-2)
		dxWeight[3] = -0.5F * s * s;
		xWeight[3]  = s * dxWeight[3] / -3.0F; // Bspline03(x-ix+1)
		dxWeight[1] = 1.0F - 2.0F * dxWeight[0] + dxWeight[3];
		//xWeight[1]  = 2.0F / 3.0F + (1.0F + ex) * dxWeight[3]; // Bspline03(x-ix-1);
		xWeight[1]  = MathTools.Bspline03(x-ix-1);
		dxWeight[2] = 1.5F * ex * (ex - 4.0F/ 3.0F);
		xWeight[2]  = 2.0F / 3.0F - (2.0F - ex) * dxWeight[0]; // Bspline03(x-ix)

		//d2xWeight[0] = ex;
		//d2xWeight[1] = s-2*ex;
		//d2xWeight[2] = ex-2*s;
		//d2xWeight[3] = s;

		// Set Y weights for the image and derivative interpolation
		double t = 1.0F - ey;
		dyWeight[0] = 0.5F * ey * ey;
		yWeight[0]  = ey * dyWeight[0] / 3.0F;
		dyWeight[3] = -0.5F * t * t;
		yWeight[3]  = t * dyWeight[3] / -3.0F;
		dyWeight[1] = 1.0F - 2.0F * dyWeight[0] + dyWeight[3];
		yWeight[1]  = 2.0F / 3.0F + (1.0F + ey) * dyWeight[3];
		dyWeight[2] = 1.5F * ey * (ey - 4.0F/ 3.0F);
		yWeight[2]  = 2.0F / 3.0F - (2.0F - ey) * dyWeight[0];

		//d2yWeight[0] = ey;
		//d2yWeight[1] = t-2*ey;
		//d2yWeight[2] = ey-2*t;
		//d2yWeight[3] = t;
		
		// Image value: Only SplineDegree=3 is implemented
		double ival=0.0F;
		for (int j = 0; j<4; j++) 
		{
			s = 0.0F;
			iy = yIndex[j];
			if (iy!=-1) 
			{
				p = iy*widthToUse;
				for (int i=0; i<4; i++) 
				{
					ix = xIndex[i];
					if (ix!=-1)
					{
						if (fromSub && this.subCoeffs != null)  
							s += xWeight[i] * this.subCoeffs[p + ix];
						else if (fromCurrent)
							s += xWeight[i] * currentCoefficient[p + ix];							
						else             
							s += xWeight[i] * coefficient[p + ix];
					}
				}
				ival+=yWeight[j] * s;
			}
		}
		
		// Derivatives: Only SplineDegree=3 is implemented
		D[0] = D[1] = 0.0F;
		for (int j = 0; j<4; j++) 
		{
			double sx = 0.0F, sy = 0.0F;
			iy = yIndex[j];
			if (iy!=-1) 
			{
				p = iy * widthToUse;
				for (int i=0; i<4; i++) 
				{
					ix = xIndex[i];
					if (ix!=-1) 
					{
						double c;
						if (fromSub && this.subCoeffs != null)  
							c = this.subCoeffs[p + ix];
						else if (fromCurrent) 
							c = currentCoefficient[p + ix];
						else             
							c = coefficient[p + ix];
						sx += dxWeight[i]*c;
						sy +=  xWeight[i]*c;
					}
				}
				D[0]+= yWeight[j] * sx;
				D[1]+=dyWeight[j] * sy;
			}
		}
		
		return ival;
	} /* prepareForInterpolationAndInterpolateIAndD */	
	
	//------------------------------------------------------------------
	/**
	 * Get width of precomputed vectors.
	 *
	 * @return the width of the precomputed vectors
	 */
	public int precomputed_getWidth() {return prec_yWeight.length;}

	//------------------------------------------------------------------
	/**
	 * Get precomputed weight dx.
	 *
	 * @param l
	 * @param m
	 * @param u
	 * @param v
	 * @return the weight of the coefficient l,m (yIndex, xIndex) in the
	 * image interpolation
	 */
	public double precomputed_getWeightDx(int l, int m, int u, int v)
	{return prec_yWeight[v][l]*prec_dxWeight[u][m];}

	//------------------------------------------------------------------
	/**
	 * Get precomputed weight dxdx
	 * 
	 * @param l
	 * @param m
	 * @param u
	 * @param v
	 * @return the weight of the coefficient l,m (prec_yWeight, prec_d2xWeight)
	 * in the image interpolation
	 */
	public double precomputed_getWeightDxDx(int l, int m, int u, int v)
	{return prec_yWeight[v][l]*prec_d2xWeight[u][m];}

	//------------------------------------------------------------------
	/**
	 * Get precomputed weight dxdy
	 * 
	 * @param l
	 * @param m
	 * @param u
	 * @param v
	 * @return the weight of the coefficient l,m (prec_dyWeight, prec_dxWeight)
	 * in the image interpolation
	 */
	public double precomputed_getWeightDxDy(int l, int m, int u, int v)
	{return prec_dyWeight[v][l]*prec_dxWeight[u][m];}

	//------------------------------------------------------------------
	/**
	 * Get precomputed weight dy
	 * 
	 * @param l
	 * @param m
	 * @param u
	 * @param v
	 * @return the weight of the coefficient l,m (prec_dyWeight, prec_xWeight) 
	 * in the image interpolation
	 */
	public double precomputed_getWeightDy(int l, int m, int u, int v)
	{return prec_dyWeight[v][l]*prec_xWeight[u][m];}

	//------------------------------------------------------------------
	/**
	 * Get precomputed weight dydy
	 * 
	 * @param l
	 * @param m
	 * @param u
	 * @param v
	 * @return the weight of the coefficient l,m (prec_d2yWeight, prec_xWeight)
	 * in the image interpolation
	 */
	public double precomputed_getWeightDyDy(int l, int m, int u, int v)
	{return prec_d2yWeight[v][l]*prec_xWeight[u][m];}

	//------------------------------------------------------------------
	/**
	 * Get precomputed weight of coefficient l,m 
	 * @param l
	 * @param m
	 * @param u
	 * @param v
	 * @return the weight of the coefficient l,m (prec_yWeight, prec_xWeight) 
	 * in the image interpolation
	 */
	public double precomputed_getWeightI(int l, int m, int u, int v)
	{return prec_yWeight[v][l]*prec_xWeight[u][m];}

	//------------------------------------------------------------------
	/**
	 * Interpolate the X and Y derivatives of the image at a
	 * given point.
	 *
	 * @param D output, X and Y derivatives of the image
	 * @param u x- point coordinate
	 * @param v y- point coordinate
	 */
	public void precomputed_interpolateD(double []D, int u, int v)
	{
		// Only SplineDegree=3 is implemented
		D[0]=D[1]=0.0F;
		for (int j = 0; j<4; j++) {
			double sx=0.0F, sy=0.0F;
			int iy=prec_yIndex[v][j];
			if (iy!=-1) {
				int p=iy*widthToUse;
				for (int i=0; i<4; i++) {
					int ix=prec_xIndex[u][i];
					if (ix!=-1) {
						double c;
						if (fromCurrent) c=currentCoefficient[p + ix];
						else             c=coefficient[p + ix];
						sx += prec_dxWeight[u][i]*c;
						sy +=  prec_xWeight[u][i]*c;
					}
				}
				D[0]+= prec_yWeight[v][j] * sx;
				D[1]+=prec_dyWeight[v][j] * sy;
			}
		}
	} /* end Interpolate D */

	//------------------------------------------------------------------
	/**
	 * Interpolate the XY, XX and YY derivatives of the image at a
	 * given point.
	 *
	 * @param D2 output, XY, XX and YY derivatives of the image
	 * @param u x- point coordinate
	 * @param v y- point coordinate
	 */
	public void precomputed_interpolateD2 (double []D2, int u, int v) {
		// Only SplineDegree=3 is implemented
		D2[0]=D2[1]=D2[2]=0.0F;
		for (int j = 0; j<4; j++) {
			double sxy=0.0F, sxx=0.0F, syy=0.0F;
			int iy=prec_yIndex[v][j];
			if (iy!=-1) {
				int p=iy*widthToUse;
				for (int i=0; i<4; i++) {
					int ix=prec_xIndex[u][i];
					if (ix!=-1) {
						double c;
						if (fromCurrent) c=currentCoefficient[p + ix];
						else             c=coefficient[p + ix];
						sxy +=  prec_dxWeight[u][i]*c;
						sxx += prec_d2xWeight[u][i]*c;
						syy +=   prec_xWeight[u][i]*c;
					}
				}
				D2[0]+= prec_dyWeight[v][j] * sxy;
				D2[1]+=  prec_yWeight[v][j] * sxx;
				D2[2]+=prec_d2yWeight[v][j] * syy;
			}
		}
	} /* end Interpolate dxdy, dxdx and dydy */

	//------------------------------------------------------------------
	/**
	 * Interpolate the image (or deformation) at a given point using 
	 * the precomputed weights.
	 *
	 * @param u x- point coordinate
	 * @param v y- point coordinate
	 */
	public double precomputed_interpolateI (int u, int v)
	{
		// Only SplineDegree=3 is implemented
		double ival = 0.0F;
		for (int j = 0; j<4; j++) 
		{
			double s = 0.0F;
			int iy = prec_yIndex[v][j];
			if (iy != -1) 
			{
				int p = iy * widthToUse;
				for (int i=0; i<4; i++) 
				{
					int ix = prec_xIndex[u][i];
					if (ix!=-1)
						if (fromCurrent) s += prec_xWeight[u][i] * currentCoefficient[p + ix];
						else             s += prec_xWeight[u][i] * coefficient[p + ix];
				}
				ival += prec_yWeight[v][j] * s;
			}
		}
		return ival;
	} /* end Interpolate Image */

	//------------------------------------------------------------------
	/**
	 * Prepare precomputations for a given image size. It calls
	 * prepareForInterpolation with ORIGINAL flag.
	 *
	 * @param Ydim y- image dimension
	 * @param Xdim x- image dimension
	 * @param intervals intervals in the deformation
	 */
	public void precomputed_prepareForInterpolation(int Ydim, int Xdim, int intervals)
	{
		// Ask for memory
		prec_xIndex   =new int   [Xdim][4];
		prec_yIndex   =new int   [Ydim][4];
		prec_xWeight  =new double[Xdim][4];
		prec_yWeight  =new double[Ydim][4];
		prec_dxWeight =new double[Xdim][4];
		prec_dyWeight =new double[Ydim][4];
		prec_d2xWeight=new double[Xdim][4];
		prec_d2yWeight=new double[Ydim][4];

		boolean ORIGINAL = false;
		// Fill the precomputed weights and indexes for the Y axis
		for (int v=0; v<Ydim; v++) 
		{
			// Express the current point in Spline units
			final double tv = (double)(v * intervals) / (double)(Ydim - 1) + 1.0F;
			final double tu = 1.0F;

			// Compute all weights and indexes
			prepareForInterpolation(tu, tv, ORIGINAL);

			// Copy all values
			for (int k=0; k<4; k++) 
			{
				prec_yIndex   [v][k]=  yIndex [k];
				prec_yWeight  [v][k]=  yWeight[k];
				prec_dyWeight [v][k]= dyWeight[k];
				prec_d2yWeight[v][k]=d2yWeight[k];
			}
		}

		// Fill the precomputed weights and indexes for the X axis
		for (int u=0; u<Xdim; u++)
		{
			// Express the current point in Spline units
			final double tv = 1.0F;
			final double tu = (double)(u * intervals) / (double)(Xdim - 1) + 1.0F;

			// Compute all weights and indexes
			prepareForInterpolation(tu,tv,ORIGINAL);

			// Copy all values
			for (int k=0; k<4; k++)
			{
				prec_xIndex   [u][k]=  xIndex [k];
				prec_xWeight  [u][k]=  xWeight[k];
				prec_dxWeight [u][k]= dxWeight[k];
				prec_d2xWeight[u][k]=d2xWeight[k];
			}
		}
	}

	//------------------------------------------------------------------
	/**
	 * Start the image pre-computations. The computation of the B-spline
	 * coefficients of the full-size image is not interruptible; all other
	 * methods are.
	 */
	public void run ()
	{
		if(image == null && ip != null)
		{
			// Original image
			this.original_image = new double[width * height];
			this.originalHeight = this.height;
			this.originalWidth = this.width;
			
			MiscTools.extractImage(ip, this.original_image);
			
			// Copy the pixel array and scale if necessary
			if(this.maxImageSubsamplingFactor != 0)
			{
				final float scaleFactor = (float) (1.0f / this.maxImageSubsamplingFactor);
				this.ip = MiscTools.scale(ip, scaleFactor);
				this.width = ip.getWidth();
				this.height = ip.getHeight();				
			}
			this.image = new double[width * height];
			MiscTools.extractImage(ip, this.image);
						
			
			// update sub-sampled output version information if necessary
			if(this.width <= this.subWidth)
			{
				this.subWidth = this.width;
				this.subHeight = this.height;
				this.subImage = this.image;
			}
		}
		coefficient = getBasicFromCardinal2D();
		
		if(coefficient != null)
			buildCoefficientPyramid();
		else 
			buildEmptyCoefficientPyramid();
		
		if (isTarget || this.bSubsampledOutput) 
			buildImagePyramid();
	} // end run 

	//------------------------------------------------------------------
	/**
	 * Set spline coefficients. Copy coefficients to the model array.
	 *
	 * @param c Set of B-spline coefficients
	 * @param Ydim Y-dimension of the set of coefficients
	 * @param Xdim X-dimension of the set of coefficients
	 * @param offset Offset of the beginning of the array with respect to the origin of c
	 */
	public void setCoefficients (
			final double []c,
			final int Ydim,
			final int Xdim,
			final int offset)
	{
		// Copy the array of coefficients
		System.arraycopy(c, offset, coefficient, 0, Ydim*Xdim);
	}

	//------------------------------------------------------------------
	/**
	 * Sets the depth up to which the pyramids should be computed.
	 *
	 * @param pyramidDepth pyramid depth to be set
	 */
	public void setPyramidDepth (final int pyramidDepth)
	{
		int proposedPyramidDepth = pyramidDepth;

		// Check what is the maximum depth allowed by the image
		int currentWidth = width;
		int currentHeight = height;
		int scale = 0;
		while (currentWidth>=min_image_size && currentHeight>=min_image_size) 
		{
			currentWidth /= 2;
			currentHeight /= 2;
			scale++;
		}
		scale--;

		if (proposedPyramidDepth > scale) 
			proposedPyramidDepth = scale;

		this.pyramidDepth = proposedPyramidDepth;
	} /* end setPyramidDepth */

	//------------------------------------------------------------------
	/**
	 * Get subsampled output flag
	 * 
	 * @return true if the output needs to be subsampled
	 */
	boolean isSubOutput()
	{
		return this.bSubsampledOutput;
	}
	
	//------------------------------------------------------------------
	/**
	 * Set subsampled output flag
	 * 
	 * @param b new subsampled output flag
	 */
	void setSubOutput(boolean b)
	{
		this.bSubsampledOutput = b;
	}
	
	//------------------------------------------------------------------
	/**
	 * Get subsampled output height
	 * 
	 * @return subsampled output height
	 */
	int getSubHeight()
	{
		return this.subHeight;
	}
	//------------------------------------------------------------------
	/**
	 * Get subsampled output width
	 * 
	 * @return subsampled output width
	 */
	int getSubWidth()
	{
		return this.subWidth;
	}

	/*....................................................................
       Private methods
    ....................................................................*/

	//------------------------------------------------------------------
	/**
	 *
	 * @param h
	 * @param c
	 * @param s
	 */
	private void antiSymmetricFirMirrorOffBounds1D (
			final double[] h,
			final double[] c,
			final double[] s)
	{
		if (2 <= c.length) {
			s[0] = h[1] * (c[1] - c[0]);
			for (int i = 1; (i < (s.length - 1)); i++) {
				s[i] = h[1] * (c[i + 1] - c[i - 1]);
			}
			s[s.length - 1] = h[1] * (c[c.length - 1] - c[c.length - 2]);
		} else s[0] = 0.0;
	} /* end antiSymmetricFirMirrorOffBounds1D */

	//------------------------------------------------------------------
	/**
	 * Pass from basic to cardinal.
	 *
	 * @param basic basic (standard B-splines) 2D array
	 * @param cardinal cardinal (sampled signal) 2D array
	 * @param width 2D signal width
	 * @param height 2D signal height
	 * @param degree B-splines degree
	 */
	private void basicToCardinal2D (
			final double[] basic,
			final double[] cardinal,
			final int width,
			final int height,
			final int degree)
	{
		final double[] hLine = new double[width];
		final double[] vLine = new double[height];
		final double[] hData = new double[width];
		final double[] vData = new double[height];
		double[] h = null;
		switch (degree) {
		case 3:
			h = new double[2];
			h[0] = 2.0 / 3.0;
			h[1] = 1.0 / 6.0;
			break;
		case 7:
			h = new double[4];
			h[0] = 151.0 / 315.0;
			h[1] = 397.0 / 1680.0;
			h[2] = 1.0 / 42.0;
			h[3] = 1.0 / 5040.0;
			break;
		default:
			h = new double[1];
		h[0] = 1.0;
		}
		for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
			extractRow(basic, y, hLine);
			symmetricFirMirrorOffBounds1D(h, hLine, hData);
			putRow(cardinal, y, hData);
		}
		for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
			extractColumn(cardinal, width, x, vLine);
			symmetricFirMirrorOffBounds1D(h, vLine, vData);
			putColumn(cardinal, width, x, vData);
		}
	} /* end basicToCardinal2D */

	//------------------------------------------------------------------
	/**
	 * Build the coefficients pyramid.
	 */
	 private void buildCoefficientPyramid ()
	{
		int fullWidth;
		int fullHeight;
		double[] fullDual = new double[width * height];
		int halfWidth = width;
		int halfHeight = height;
		basicToCardinal2D(coefficient, fullDual, width, height, 7);						
		 
		// We compute the coefficients pyramid 
		for (int depth = 1; ((depth <= pyramidDepth) && (!t.isInterrupted())); depth++) 
		{
			IJ.showStatus("Building coefficients pyramid...");
			IJ.showProgress((double) depth / pyramidDepth );
			fullWidth = halfWidth;
			fullHeight = halfHeight;
			halfWidth /= 2;
			halfHeight /= 2;
			
			// If the image is too small, we push the previous version of the coefficients
			if(fullWidth <= BSplineModel.min_image_size || fullHeight <= BSplineModel.min_image_size)
			{				 
				if(this.bSubsampledOutput)
					IJ.log("Coefficients pyramid " + fullWidth + "x" + fullHeight);
				cpyramid.push(fullDual);
				cpyramid.push(new Integer(fullHeight));
				cpyramid.push(new Integer(fullWidth));
				halfWidth *= 2;
				halfHeight *= 2;
				continue;
			}
						
			// Otherwise, we reduce the coefficients by 2
			final double[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
			final double[] halfCoefficient = getBasicFromCardinal2D(halfDual, halfWidth, halfHeight, 7);

			
			if(this.bSubsampledOutput)
				IJ.log("Coefficients pyramid " + halfWidth + "x" + halfHeight);
			cpyramid.push(halfCoefficient);
			cpyramid.push(new Integer(halfHeight));
			cpyramid.push(new Integer(halfWidth));
			

			fullDual = halfDual;
			
			// We store the coefficients of the corresponding subsampled
			// output if it exists.
			if(this.bSubsampledOutput && halfWidth == this.subWidth)
			{
				this.subCoeffs = halfCoefficient;
			}
		}		
		smallestWidth  = halfWidth;
		smallestHeight = halfHeight;
		currentDepth = pyramidDepth+1;
		
		//if(this.bSubsampledOutput && this.subCoeffs != null)
		//	System.out.println(" subCoeffs.length = " + this.subCoeffs.length);
	} /* end buildCoefficientPyramid */

		//------------------------------------------------------------------
		/**
		 * Build an empty coefficient pyramid (for only-landmark registration).
		 */
		 private void buildEmptyCoefficientPyramid ()
		{
			int fullWidth;
			int fullHeight;

			int halfWidth = width;
			int halfHeight = height;						
			final double[] fullDual = new double[]{};
			final double[] halfCoefficient = new double[]{};
			
			// We compute the coefficients pyramid 
			for (int depth = 1; ((depth <= pyramidDepth) && (!t.isInterrupted())); depth++) 
			{
				IJ.showStatus("Building coefficients pyramid...");
				IJ.showProgress((double) depth / pyramidDepth );
				fullWidth = halfWidth;
				fullHeight = halfHeight;
				halfWidth /= 2;
				halfHeight /= 2;
				
				// If the image is too small, we push the previous version of the coefficients
				if(fullWidth <= BSplineModel.min_image_size || fullHeight <= BSplineModel.min_image_size)
				{				 
					if(this.bSubsampledOutput)
						IJ.log("Coefficients pyramid " + fullWidth + "x" + fullHeight);
					
					cpyramid.push(fullDual);
					cpyramid.push(new Integer(fullHeight));
					cpyramid.push(new Integer(fullWidth));
					halfWidth *= 2;
					halfHeight *= 2;
					continue;
				}
							
				// Otherwise, we reduce the coefficients by 2			
				if(this.bSubsampledOutput)
					IJ.log("Coefficients pyramid " + halfWidth + "x" + halfHeight);
				cpyramid.push(halfCoefficient);
				cpyramid.push(new Integer(halfHeight));
				cpyramid.push(new Integer(halfWidth));
								
				// We store the coefficients of the corresponding subsampled
				// output if it exists.
				if(this.bSubsampledOutput && halfWidth == this.subWidth)
				{
					this.subCoeffs = halfCoefficient;
				}
			}		
			smallestWidth  = halfWidth;
			smallestHeight = halfHeight;
			currentDepth = pyramidDepth+1;
			
			//if(this.bSubsampledOutput && this.subCoeffs != null)
			//	System.out.println(" subCoeffs.length = " + this.subCoeffs.length);
		} /* end buildCoefficientPyramid */	 
	 
	//------------------------------------------------------------------
	/**
	 * Build the image pyramid.
	 */
	 private void buildImagePyramid ()
	 {
		 int fullWidth;
		 int fullHeight;
		 double[] fullDual = new double[width * height];
		 int halfWidth = width;
		 int halfHeight = height;
		 cardinalToDual2D(image, fullDual, width, height, 3);
		 		 		 
		 
		 for (int depth = 1; depth <= pyramidDepth  && !t.isInterrupted(); depth++) 
		 {			 
			 IJ.showStatus("Building image pyramid...");
		     IJ.showProgress((double) depth / pyramidDepth);
				
			 fullWidth = halfWidth;
			 fullHeight = halfHeight;			 			 
			 
			 halfWidth /= 2;
			 halfHeight /= 2;
			 
			 if(fullWidth <= BSplineModel.min_image_size || fullHeight <= BSplineModel.min_image_size)
			 {				 
				 if(this.bSubsampledOutput)
						IJ.log(" Image pyramid " + fullWidth + "x" + fullHeight);
				 imgpyramid.push(fullDual);
				 imgpyramid.push(new Integer(fullHeight));
				 imgpyramid.push(new Integer(fullWidth));
				 halfWidth *= 2;
				 halfHeight *= 2;
				 continue;
			 }
			 
			 final double[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
			 final double[] halfImage = new double[halfWidth * halfHeight];
			 dualToCardinal2D(halfDual, halfImage, halfWidth, halfHeight, 3);
			 
			
			 if(this.bSubsampledOutput)
				 IJ.log(" Image pyramid " + halfWidth + "x" + halfHeight);
			 imgpyramid.push(halfImage);
			 imgpyramid.push(new Integer(halfHeight));
			 imgpyramid.push(new Integer(halfWidth));
			 
			 fullDual = halfDual;
			 
			 if(this.bSubsampledOutput && halfWidth == this.subWidth)
			 {
				 this.subImage = halfDual;
				 //IJ.log("sub image set");
			 }
		 }
		 
		 // If the output sub-image has not been set yet, we keep reducing the image
		 while(halfWidth > this.subWidth)
		 {
			 fullWidth = halfWidth;
			 fullHeight = halfHeight;			 			 
			 
			 halfWidth /= 2;
			 halfHeight /= 2;
			 
			 final double[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
			 final double[] halfImage = new double[halfWidth * halfHeight];
			 dualToCardinal2D(halfDual, halfImage, halfWidth, halfHeight, 3);
			 
			 fullDual = halfDual;
			 // We store the image version that matches the sub-sampled output (if necessary)
			 if(this.bSubsampledOutput && halfWidth == this.subWidth)
			 {
				 this.subImage = halfDual;
				 //IJ.log("sub image set");
			 }
		 }
		 
		 //System.out.println(" subImage.length = " + this.subImage.length);
		 
	 } /* end buildImagePyramid */

	 //------------------------------------------------------------------
	 /**
	  * Passes from cardinal to dual (2D).
	  *
	  * @param cardinal
	  * @param dual
	  * @param width
	  * @param height
	  * @param degree
	  */
	 private void cardinalToDual2D (
			 final double[] cardinal,
			 final double[] dual,
			 final int width,
			 final int height,
			 final int degree)
	 {
		 basicToCardinal2D(getBasicFromCardinal2D(cardinal, width, height, degree),
				 dual, width, height, 2 * degree + 1);
	 } /* end cardinalToDual2D */

	 //------------------------------------------------------------------
	 /**
	  * Pass coefficients to gradient (1D).
	  *
	  * @param c coefficients
	  */
	 private void coefficientToGradient1D (final double[] c)
	 {
		 final double[] h = {0.0, 1.0 / 2.0};
		 final double[] s = new double[c.length];
		 antiSymmetricFirMirrorOffBounds1D(h, c, s);
		 System.arraycopy(s, 0, c, 0, s.length);
	 } /* end coefficientToGradient1D */

	 //------------------------------------------------------------------
	 /**
	  * Pass coefficients to samples.
	  *
	  * @param c coefficients
	  */
	 private void coefficientToSamples1D (final double[] c)
	 {
		 final double[] h = {2.0 / 3.0, 1.0 / 6.0};
		 final double[] s = new double[c.length];
		 symmetricFirMirrorOffBounds1D(h, c, s);
		 System.arraycopy(s, 0, c, 0, s.length);
	 } /* end coefficientToSamples1D */

	 //------------------------------------------------------------------
	 /**
	  * Pass coefficients to x,y gradient 2D
	  * 
	  * @param basic
	  * @param xGradient
	  * @param yGradient
	  * @param width
	  * @param height
	  */	 
	private void coefficientToXYGradient2D (
			 final double[] basic,
			 final double[] xGradient,
			 final double[] yGradient,
			 final int width,
			 final int height)
	 {
		 final double[] hLine = new double[width];
		 final double[] hData = new double[width];
		 final double[] vLine = new double[height];
		 for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
			 extractRow(basic, y, hLine);
			 System.arraycopy(hLine, 0, hData, 0, width);
			 coefficientToGradient1D(hLine);
			 coefficientToSamples1D(hData);
			 putRow(xGradient, y, hLine);
			 putRow(yGradient, y, hData);
		 }
		 for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
			 extractColumn(xGradient, width, x, vLine);
			 coefficientToSamples1D(vLine);
			 putColumn(xGradient, width, x, vLine);
			 extractColumn(yGradient, width, x, vLine);
			 coefficientToGradient1D(vLine);
			 putColumn(yGradient, width, x, vLine);
		 }
	 } /* end coefficientToXYGradient2D */

	 //------------------------------------------------------------------
	 /**
	  * Pass from dual to cardinal (2D).
	  *
	  * @param dual
	  * @param cardinal
	  * @param width
	  * @param height
	  * @param degree
	  */
	 private void dualToCardinal2D (
			 final double[] dual,
			 final double[] cardinal,
			 final int width,
			 final int height,
			 final int degree)
	 {
		 basicToCardinal2D(getBasicFromCardinal2D(dual, width, height, 2 * degree + 1),
				 cardinal, width, height, degree);
	 } /* end dualToCardinal2D */

	 //------------------------------------------------------------------
	 /**
	  * Extract a column from the array.
	  *
	  * @param array
	  * @param width of the position of the column in the array
	  * @param x column position in the array
	  * @param column output, extracted column
	  */
	 private void extractColumn (
			 final double[] array,
			 final int width,
			 int x,
			 final double[] column)
	 {
		 for (int i = 0; (i < column.length); i++, x+=width)
			 column[i] = (double)array[x];
	 } /* end extractColumn */

	 //------------------------------------------------------------------
	 /**
	  * Extract a row from the array .
	  *
	  * @param array
	  * @param y row position in the array
	  * @param row output, extracted row
	  */
	 private void extractRow (
			 final double[] array,
			 int y,
			 final double[] row)
	 {
		 y *= row.length;
		 for (int i = 0; (i < row.length); i++)
			 row[i] = (double)array[y++];
	 } /* end extractRow */

	 //------------------------------------------------------------------
	 /**
	  * Get basic from cardinal: convert the 2D image from regular samples
	  * to standard B-spline coefficients.
	  * 
	  * @return array of standard B-spline coefficients
	  */
	 private double[] getBasicFromCardinal2D ()
	 {
		 if(this.image == null)
			 return null;
		 
		 final double[] basic = new double[width * height];
		 final double[] hLine = new double[width];
		 final double[] vLine = new double[height];
		 for (int y = 0; (y < height); y++) {
			 extractRow(image, y, hLine);
			 samplesToInterpolationCoefficient1D(hLine, 3, 0.0);
			 putRow(basic, y, hLine);
		 }
		 for (int x = 0; (x < width); x++) {
			 extractColumn(basic, width, x, vLine);
			 samplesToInterpolationCoefficient1D(vLine, 3, 0.0);
			 putColumn(basic, width, x, vLine);
		 }
		 return(basic);
	 } /* end getBasicFromCardinal2D */

	 //------------------------------------------------------------------
	 /**
	  * Get basic from cardinal (2D): convert a 2D signal from regular 
	  * samples to standard B-spline coefficients.
	  *
	  * @param cardinal sampled 2D signal
	  * @param width signal width
	  * @param height signal height
	  * @param degree B-spline degree
	  * 
	  * @return array of standard B-spline coefficients
	  */
	 private double[] getBasicFromCardinal2D (
			 final double[] cardinal,
			 final int width,
			 final int height,
			 final int degree)
	 {
		 final double[] basic = new double[width * height];
		 final double[] hLine = new double[width];
		 final double[] vLine = new double[height];
		 for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
			 extractRow(cardinal, y, hLine);
			 samplesToInterpolationCoefficient1D(hLine, degree, 0.0);
			 putRow(basic, y, hLine);
		 }
		 for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
			 extractColumn(basic, width, x, vLine);
			 samplesToInterpolationCoefficient1D(vLine, degree, 0.0);
			 putColumn(basic, width, x, vLine);
		 }
		 return(basic);
	 } /* end getBasicFromCardinal2D */

	 //------------------------------------------------------------------
	 /**
	  * Get half dual (2D).
	  *
	  * @param fullDual full coefficients
	  * @param fullWidth full coefficients width
	  * @param fullHeight full coefficients height
	  */
	 private double[] getHalfDual2D (
			 final double[] fullDual,
			 final int fullWidth,
			 final int fullHeight)
	 {
		 final int halfWidth = fullWidth / 2;
		 final int halfHeight = fullHeight / 2;
		 final double[] hLine = new double[fullWidth];
		 final double[] hData = new double[halfWidth];
		 final double[] vLine = new double[fullHeight];
		 final double[] vData = new double[halfHeight];
		 final double[] demiDual = new double[halfWidth * fullHeight];
		 final double[] halfDual = new double[halfWidth * halfHeight];
		 for (int y = 0; ((y < fullHeight) && (!t.isInterrupted())); y++) {
			 extractRow(fullDual, y, hLine);
			 reduceDual1D(hLine, hData);
			 putRow(demiDual, y, hData);
		 }
		 for (int x = 0; ((x < halfWidth) && (!t.isInterrupted())); x++) {
			 extractColumn(demiDual, halfWidth, x, vLine);
			 reduceDual1D(vLine, vData);
			 putColumn(halfDual, halfWidth, x, vData);
		 }
		 return(halfDual);
	 } /* end getHalfDual2D */

	 //------------------------------------------------------------------
	 /**
	  * Get initial anti-causal coefficients mirror of bounds.
	  *
	  * @param c coefficients
	  * @param z
	  * @param tolerance
	  */
	 private double getInitialAntiCausalCoefficientMirrorOffBounds (
			 final double[] c,
			 final double z,
			 final double tolerance)
	 {
		 return(z * c[c.length - 1] / (z - 1.0));
	 } /* end getInitialAntiCausalCoefficientMirrorOffBounds */

	 //------------------------------------------------------------------
	 /**
	  * Get initial causal coefficients mirror of bounds.
	  *
	  * @param c coefficients
	  * @param z
	  * @param tolerance
	  */
	 private double getInitialCausalCoefficientMirrorOffBounds (
			 final double[] c,
			 final double z,
			 final double tolerance)
	 {
		 double z1 = z;
		 double zn = Math.pow(z, c.length);
		 double sum = (1.0 + z) * (c[0] + zn * c[c.length - 1]);
		 int horizon = c.length;
		 if (0.0 < tolerance) {
			 horizon = 2 + (int)(Math.log(tolerance) / Math.log(Math.abs(z)));
			 horizon = (horizon < c.length) ? (horizon) : (c.length);
		 }
		 zn = zn * zn;
		 for (int n = 1; (n < (horizon - 1)); n++) {
			 z1 = z1 * z;
			 zn = zn / z;
			 sum = sum + (z1 + zn) * c[n];
		 }
		 return(sum / (1.0 - Math.pow(z, 2 * c.length)));
	 } /* end getInitialCausalCoefficientMirrorOffBounds */

	 //------------------------------------------------------------------
	 /**
	  * Put a column in the array.
	  *
	  * @param array
	  * @param width of the position of the column in the array
	  * @param x column position in the array
	  * @param column column to be put
	  */
	 private void putColumn (
			 final double[] array,
			 final int width,
			 int x,
			 final double[] column)
	 {
		 for (int i = 0; (i < column.length); i++, x+=width)
			 array[x] = (double)column[i];
	 } /* end putColumn */

	 //------------------------------------------------------------------
	 /**
	  * Put a row in the array.
	  *
	  * @param array
	  * @param y row position in the array
	  * @param row row to be put
	  */
	 private void putRow (
			 final double[] array,
			 int y,
			 final double[] row)
	 {
		 y *= row.length;
		 for (int i = 0; (i < row.length); i++)
			 array[y++] = (double)row[i];
	 } /* end putRow */

	 //------------------------------------------------------------------
	 /**
	  * Reduce dual (1D).
	  *
	  * @param c
	  * @param s
	  */
	 private void reduceDual1D (
			 final double[] c,
			 final double[] s)
	 {
		 final double h[] = {6.0 / 16.0, 4.0 / 16.0, 1.0 / 16.0};
		 if (2 <= s.length) {
			 s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2]);
			 for (int i = 2, j = 1; (j < (s.length - 1)); i += 2, j++) {
				 s[j] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
				 + h[2] * (c[i - 2] + c[i + 2]);
			 }
			 if (c.length == (2 * s.length)) {
				 s[s.length - 1] = h[0] * c[c.length - 2] + h[1] * (c[c.length - 3] + c[c.length - 1])
				 + h[2] * (c[c.length - 4] + c[c.length - 1]);
			 }
			 else {
				 s[s.length - 1] = h[0] * c[c.length - 3] + h[1] * (c[c.length - 4] + c[c.length - 2])
				 + h[2] * (c[c.length - 5] + c[c.length - 1]);
			 }
		 }
		 else {
			 switch (c.length) {
			 case 3:
				 s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2]);
				 break;
			 case 2:
				 s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + 2.0 * h[2] * c[1];
				 break;
			 default:
			 }
		 }
	 } /* end reduceDual1D */

	 //------------------------------------------------------------------
	 /**
	  * Samples to interpolation coefficient (1D).
	  *
	  * @param c coefficients
	  * @param degree
	  * @param tolerance
	  */
	 private void samplesToInterpolationCoefficient1D (
			 final double[] c,
			 final int degree,
			 final double tolerance)
	 {
		 double[] z = new double[0];
		 double lambda = 1.0;
		 switch (degree) {
		 case 3:
			 z = new double[1];
			 z[0] = Math.sqrt(3.0) - 2.0;
			 break;
		 case 7:
			 z = new double[3];
			 z[0] = -0.5352804307964381655424037816816460718339231523426924148812;
			 z[1] = -0.122554615192326690515272264359357343605486549427295558490763;
			 z[2] = -0.0091486948096082769285930216516478534156925639545994482648003;
			 break;
		 default:
		 }
		 // special case required by mirror boundaries
		 if (c.length == 1) 
		 {
			 return;
		 }
		 // compute the overall gain
		 for (int k = 0; (k < z.length); k++) 
		 {
			 lambda *= (1.0 - z[k]) * (1.0 - 1.0 / z[k]);
		 }
		 // apply the gain
		 for (int n = 0; (n < c.length); n++) 
		 {
			 c[n] *= lambda;
		 }
		 // loop over all poles
		 for (int k = 0; (k < z.length); k++) 
		 {
			 // causal initialization
			 c[0] = getInitialCausalCoefficientMirrorOffBounds(c, z[k], tolerance);
			 // causal recursion
			 for (int n = 1; (n < c.length); n++) 
			 {
				 c[n] += z[k] * c[n - 1];
			 }
			 // anticausal initialization
			 c[c.length - 1] = getInitialAntiCausalCoefficientMirrorOffBounds(c, z[k], tolerance);
			 // anticausal recursion
			 for (int n = c.length - 2; (0 <= n); n--) 
			 {
				 c[n] = z[k] * (c[n+1] - c[n]);
			 }
		 }
	 } /* end samplesToInterpolationCoefficient1D */

	 //------------------------------------------------------------------
	 /**
	  * Symmetric FIR filter with mirror off bounds (1D) conditions.
	  *
	  * @param h
	  * @param c
	  * @param s
	  */
	 private void symmetricFirMirrorOffBounds1D (
			 final double[] h,
			 final double[] c,
			 final double[] s)
	 {
		 switch (h.length) {
		 case 2:
			 if (2 <= c.length) {
				 s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]);
				 for (int i = 1; (i < (s.length - 1)); i++) {
					 s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1]);
				 }
				 s[s.length - 1] = h[0] * c[c.length - 1]
				                            + h[1] * (c[c.length - 2] + c[c.length - 1]);
			 }
			 else {
				 s[0] = (h[0] + 2.0 * h[1]) * c[0];
			 }
			 break;
		 case 4:
			 if (6 <= c.length) {
				 s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
				 + h[3] * (c[2] + c[3]);
				 s[1] = h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[0] + c[3])
				 + h[3] * (c[1] + c[4]);
				 s[2] = h[0] * c[2] + h[1] * (c[1] + c[3]) + h[2] * (c[0] + c[4])
				 + h[3] * (c[0] + c[5]);
				 for (int i = 3; (i < (s.length - 3)); i++) {
					 s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
					 + h[2] * (c[i - 2] + c[i + 2]) + h[3] * (c[i - 3] + c[i + 3]);
				 }
				 s[s.length - 3] = h[0] * c[c.length - 3]
				                            + h[1] * (c[c.length - 4] + c[c.length - 2])
				                            + h[2] * (c[c.length - 5] + c[c.length - 1])
				                            + h[3] * (c[c.length - 6] + c[c.length - 1]);
				 s[s.length - 2] = h[0] * c[c.length - 2]
				                            + h[1] * (c[c.length - 3] + c[c.length - 1])
				                            + h[2] * (c[c.length - 4] + c[c.length - 1])
				                            + h[3] * (c[c.length - 5] + c[c.length - 2]);
				 s[s.length - 1] = h[0] * c[c.length - 1]
				                            + h[1] * (c[c.length - 2] + c[c.length - 1])
				                            + h[2] * (c[c.length - 3] + c[c.length - 2])
				                            + h[3] * (c[c.length - 4] + c[c.length - 3]);
			 }
			 else {
				 switch (c.length) {
				 case 5:
					 s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
					 + h[3] * (c[2] + c[3]);
					 s[1] = h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[0] + c[3])
					 + h[3] * (c[1] + c[4]);
					 s[2] = h[0] * c[2] + h[1] * (c[1] + c[3])
					 + (h[2] + h[3]) * (c[0] + c[4]);
					 s[3] = h[0] * c[3] + h[1] * (c[2] + c[4]) + h[2] * (c[1] + c[4])
					 + h[3] * (c[0] + c[3]);
					 s[4] = h[0] * c[4] + h[1] * (c[3] + c[4]) + h[2] * (c[2] + c[3])
					 + h[3] * (c[1] + c[2]);
					 break;
				 case 4:
					 s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
					 + h[3] * (c[2] + c[3]);
					 s[1] = h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[0] + c[3])
					 + h[3] * (c[1] + c[3]);
					 s[2] = h[0] * c[2] + h[1] * (c[1] + c[3]) + h[2] * (c[0] + c[3])
					 + h[3] * (c[0] + c[2]);
					 s[3] = h[0] * c[3] + h[1] * (c[2] + c[3]) + h[2] * (c[1] + c[2])
					 + h[3] * (c[0] + c[1]);
					 break;
				 case 3:
					 s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
					 + 2.0 * h[3] * c[2];
					 s[1] = h[0] * c[1] + (h[1] + h[2]) * (c[0] + c[2])
					 + 2.0 * h[3] * c[1];
					 s[2] = h[0] * c[2] + h[1] * (c[1] + c[2]) + h[2] * (c[0] + c[1])
					 + 2.0 * h[3] * c[0];
					 break;
				 case 2:
					 s[0] = (h[0] + h[1] + h[3]) * c[0] + (h[1] + 2.0 * h[2] + h[3]) * c[1];
					 s[1] = (h[0] + h[1] + h[3]) * c[1] + (h[1] + 2.0 * h[2] + h[3]) * c[0];
					 break;
				 case 1:
					 s[0] = (h[0] + 2.0 * (h[1] + h[2] + h[3])) * c[0];
					 break;
				 default:
				 }
			 }
			 break;
		 default:
		 }
	 } /* end symmetricFirMirrorOffBounds1D */
	 
 	//------------------------------------------------------------------
	 /**
	  * Reduce coefficients by a factor of 2 (beta)
	  * @param c
	  * @param width
	  * @param height
	  */
	 public double[] reduceCoeffsBy2(double[]c, int width, int height)
	 {
		 double[] fullDual = new double[width * height];
		 int halfWidth = width / 2;
		 int halfHeight = height / 2;
		 basicToCardinal2D(c, fullDual, width, height, 7);		 
		 final double[] halfDual = getHalfDual2D(fullDual, width, height);
		 final double[] halfCoefficient = getBasicFromCardinal2D(halfDual, halfWidth, halfHeight, 7);
		 return halfCoefficient;
	 }
	 
	 //------------------------------------------------------------------
	 /**
	  * Set maximum sub-sampling factor
	  * 
	  * @param maxImageSubsamplingFactor
	  */
	 public void setSubsamplingFactor(int maxImageSubsamplingFactor) 
	 {
		 this.maxImageSubsamplingFactor = maxImageSubsamplingFactor;		
	 }
	 

} /* end class BSplineModel */
