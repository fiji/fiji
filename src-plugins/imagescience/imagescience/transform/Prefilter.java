package imagescience.transform;

import imagescience.image.Axes;
import imagescience.image.Borders;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;

/** Prefilters images for different interpolation schemes. */
public class Prefilter {
	
	private static final double BSPLINE3POLE1 = -0.267949192431f;
	private static final double BSPLINE3SCALE = 6;
	
	private static final double BSPLINE5POLE1 = -0.4305753471f;
	private static final double BSPLINE5POLE2 = -0.0430962882033f;
	private static final double BSPLINE5SCALE = 120;
	
	private static final double OMOMS3POLE1 = -0.344131154255f;
	private static final double OMOMS3SCALE = 5.25f;
	
	private static final int HORIZON = 25;
	
	/** Default constructor. */
	public Prefilter() { }
	
	/** Applies cubic B-spline prefiltering to an array.
		
		@param array the array to be prefiltered. The array contents will be replaced by the result of the prefiltering.
		
		@param border the size of the borders at the beginning and end of the array. The borders are ignored in the prefiltering.
		
		@exception ArrayIndexOutOfBoundsException if {@code border} is less than {@code 0}.
		
		@exception NullPointerException if {@code array} is {@code null}.
	*/
	public void bspline3(final double[] array, final int border) {
		
		if ((array.length - 2*border) > 1) {
			causalanticausal(array,border,BSPLINE3POLE1);
			scale(array,border,BSPLINE3SCALE);
		}
	}
	
	/** Applies cubic B-spline prefiltering to an image.
		
		@param image the image to be prefiltered. If the image is of type {@link FloatImage}, it is overwritten with the prefiltering results and returned. Otherwise it is left unaltered.
		
		@param axes the axes along which prefiltering is applied. The image is prefiltered in each dimension for which the corresponding boolean field of this parameter is {@code true}.
		
		@param borders the size of the borders at the beginning and end of the image in each dimension. These borders are ignored in the prefiltering.
		
		@return a prefiltered version of the input image. The returned image is always of type {@link FloatImage}.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public Image bspline3(final Image image, final Axes axes, final Borders borders) {
		
		final Dimensions dims = image.dimensions();
		final Coordinates min = new Coordinates(borders.x,borders.y,borders.z,borders.t,borders.c);
		final Coordinates max = new Coordinates(dims.x-borders.x-1,dims.y-borders.y-1,dims.z-borders.z-1,dims.t-borders.t-1,dims.c-borders.c-1);
		final Image prefimg = (image instanceof FloatImage) ? image : new FloatImage(image);
		
		// Prefilter in the x-dimension if requested:
		if (axes.x && (dims.x - 2*borders.x > 1)) {
			prefimg.axes(Axes.X);
			final double[] array = new double[dims.x];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.t=min.t; coords.t<=max.t; ++coords.t)
					for (coords.z=min.z; coords.z<=max.z; ++coords.z)
						for (coords.y=min.y; coords.y<=max.y; ++coords.y) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.x,BSPLINE3POLE1);
							scale(array,borders.x,BSPLINE3SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the y-dimension if requested:
		if (axes.y && (dims.y - 2*borders.y > 1)) {
			prefimg.axes(Axes.Y);
			final double[] array = new double[dims.y];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.t=min.t; coords.t<=max.t; ++coords.t)
					for (coords.z=min.z; coords.z<=max.z; ++coords.z)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.y,BSPLINE3POLE1);
							scale(array,borders.y,BSPLINE3SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the z-dimension if requested:
		if (axes.z && (dims.z - 2*borders.z > 1)) {
			prefimg.axes(Axes.Z);
			final double[] array = new double[dims.z];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.t=min.t; coords.t<=max.t; ++coords.t)
					for (coords.y=min.y; coords.y<=max.y; ++coords.y)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.z,BSPLINE3POLE1);
							scale(array,borders.z,BSPLINE3SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the t-dimension if requested:
		if (axes.t && (dims.t - 2*borders.t > 1)) {
			prefimg.axes(Axes.T);
			final double[] array = new double[dims.t];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.z=min.z; coords.z<=max.z; ++coords.z)
					for (coords.y=min.y; coords.y<=max.y; ++coords.y)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.t,BSPLINE3POLE1);
							scale(array,borders.t,BSPLINE3SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the c-dimension if requested:
		if (axes.c && (dims.c - 2*borders.c > 1)) {
			prefimg.axes(Axes.C);
			final double[] array = new double[dims.c];
			final Coordinates coords = new Coordinates();
			for (coords.t=min.t; coords.t<=max.t; ++coords.t)
				for (coords.z=min.z; coords.z<=max.z; ++coords.z)
					for (coords.y=min.y; coords.y<=max.y; ++coords.y)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.c,BSPLINE3POLE1);
							scale(array,borders.c,BSPLINE3SCALE);
							prefimg.set(coords,array);
						}
		}
		
		return prefimg;
	}
	
	/** Applies cubic O-MOMS prefiltering to an array.
		
		@param array the array to be prefiltered. The array contents will be replaced by the result of the prefiltering.
		
		@param border the size of the borders at the beginning and end of the array. The borders are ignored in the prefiltering.
		
		@exception ArrayIndexOutOfBoundsException if {@code border} is less than {@code 0}.
		
		@exception NullPointerException if {@code array} is {@code null}.
	*/
	public void omoms3(final double[] array, final int border) {
		
		if ((array.length - 2*border) > 1) {
			causalanticausal(array,border,OMOMS3POLE1);
			scale(array,border,OMOMS3SCALE);
		}
	}
	
	/** Applies cubic O-MOMS prefiltering to an image.
		
		@param image the image to be prefiltered. If the image is of type {@link FloatImage}, it is overwritten with the prefiltering results and returned. Otherwise it is left unaltered.
		
		@param axes the axes along which prefiltering is applied. The image is prefiltered in each dimension for which the corresponding boolean field of this parameter is {@code true}.
		
		@param borders the size of the borders at the beginning and end of the image in each dimension. These borders are ignored in the prefiltering.
		
		@return a prefiltered version of the input image. The returned image is always of type {@link FloatImage}.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public Image omoms3(final Image image, final Axes axes, final Borders borders) {
		
		final Dimensions dims = image.dimensions();
		final Coordinates min = new Coordinates(borders.x,borders.y,borders.z,borders.t,borders.c);
		final Coordinates max = new Coordinates(dims.x-borders.x-1,dims.y-borders.y-1,dims.z-borders.z-1,dims.t-borders.t-1,dims.c-borders.c-1);
		final Image prefimg = (image instanceof FloatImage) ? image : new FloatImage(image);
		
		// Prefilter in the x-dimension if requested:
		if (axes.x && (dims.x - 2*borders.x > 1)) {
			prefimg.axes(Axes.X);
			final double[] array = new double[dims.x];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.t=min.t; coords.t<=max.t; ++coords.t)
					for (coords.z=min.z; coords.z<=max.z; ++coords.z)
						for (coords.y=min.y; coords.y<=max.y; ++coords.y) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.x,OMOMS3POLE1);
							scale(array,borders.x,OMOMS3SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the y-dimension if requested:
		if (axes.y && (dims.y - 2*borders.y > 1)) {
			prefimg.axes(Axes.Y);
			final double[] array = new double[dims.y];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.t=min.t; coords.t<=max.t; ++coords.t)
					for (coords.z=min.z; coords.z<=max.z; ++coords.z)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.y,OMOMS3POLE1);
							scale(array,borders.y,OMOMS3SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the z-dimension if requested:
		if (axes.z && (dims.z - 2*borders.z > 1)) {
			prefimg.axes(Axes.Z);
			final double[] array = new double[dims.z];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.t=min.t; coords.t<=max.t; ++coords.t)
					for (coords.y=min.y; coords.y<=max.y; ++coords.y)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.z,OMOMS3POLE1);
							scale(array,borders.z,OMOMS3SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the t-dimension if requested:
		if (axes.t && (dims.t - 2*borders.t > 1)) {
			prefimg.axes(Axes.T);
			final double[] array = new double[dims.t];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.z=min.z; coords.z<=max.z; ++coords.z)
					for (coords.y=min.y; coords.y<=max.y; ++coords.y)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.t,OMOMS3POLE1);
							scale(array,borders.t,OMOMS3SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the c-dimension if requested:
		if (axes.c && (dims.c - 2*borders.c > 1)) {
			prefimg.axes(Axes.C);
			final double[] array = new double[dims.c];
			final Coordinates coords = new Coordinates();
			for (coords.t=min.t; coords.t<=max.t; ++coords.t)
				for (coords.z=min.z; coords.z<=max.z; ++coords.z)
					for (coords.y=min.y; coords.y<=max.y; ++coords.y)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.c,OMOMS3POLE1);
							scale(array,borders.c,OMOMS3SCALE);
							prefimg.set(coords,array);
						}
		}
		
		return prefimg;
	}
	
	/** Applies quintic B-spline prefiltering to an array.
		
		@param array the array to be prefiltered. The array contents will be replaced by the result of the prefiltering.
		
		@param border the size of the borders at the beginning and end of the array. The borders are ignored in the prefiltering.
		
		@exception ArrayIndexOutOfBoundsException if {@code border} is less than {@code 0}.
		
		@exception NullPointerException if {@code array} is {@code null}.
	*/
	public void bspline5(final double[] array, final int border) {
		
		if ((array.length - 2*border) > 1) {
			causalanticausal(array,border,BSPLINE5POLE1);
			causalanticausal(array,border,BSPLINE5POLE2);
			scale(array,border,BSPLINE5SCALE);
		}
	}
	
	/** Applies quintic B-spline prefiltering to an image.
		
		@param image the image to be prefiltered. If the image is of type {@link FloatImage}, it is overwritten with the prefiltering results and returned. Otherwise it is left unaltered.
		
		@param axes the axes along which prefiltering is applied. The image is prefiltered in each dimension for which the corresponding boolean field of this parameter is {@code true}.
		
		@param borders the size of the borders at the beginning and end of the image in each dimension. These borders are ignored in the prefiltering.
		
		@return a prefiltered version of the input image. The returned image is always of type {@link FloatImage}.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public Image bspline5(final Image image, final Axes axes, final Borders borders) {
		
		final Dimensions dims = image.dimensions();
		final Coordinates min = new Coordinates(borders.x,borders.y,borders.z,borders.t,borders.c);
		final Coordinates max = new Coordinates(dims.x-borders.x-1,dims.y-borders.y-1,dims.z-borders.z-1,dims.t-borders.t-1,dims.c-borders.c-1);
		final Image prefimg = (image instanceof FloatImage) ? image : new FloatImage(image);
		
		// Prefilter in the x-dimension if requested:
		if (axes.x && (dims.x - 2*borders.x > 1)) {
			prefimg.axes(Axes.X);
			final double[] array = new double[dims.x];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.t=min.t; coords.t<=max.t; ++coords.t)
					for (coords.z=min.z; coords.z<=max.z; ++coords.z)
						for (coords.y=min.y; coords.y<=max.y; ++coords.y) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.x,BSPLINE5POLE1);
							causalanticausal(array,borders.x,BSPLINE5POLE2);
							scale(array,borders.x,BSPLINE5SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the y-dimension if requested:
		if (axes.y && (dims.y - 2*borders.y > 1)) {
			prefimg.axes(Axes.Y);
			final double[] array = new double[dims.y];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.t=min.t; coords.t<=max.t; ++coords.t)
					for (coords.z=min.z; coords.z<=max.z; ++coords.z)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.y,BSPLINE5POLE1);
							causalanticausal(array,borders.y,BSPLINE5POLE2);
							scale(array,borders.y,BSPLINE5SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the z-dimension if requested:
		if (axes.z && (dims.z - 2*borders.z > 1)) {
			prefimg.axes(Axes.Z);
			final double[] array = new double[dims.z];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.t=min.t; coords.t<=max.t; ++coords.t)
					for (coords.y=min.y; coords.y<=max.y; ++coords.y)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.z,BSPLINE5POLE1);
							causalanticausal(array,borders.z,BSPLINE5POLE2);
							scale(array,borders.z,BSPLINE5SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the t-dimension if requested:
		if (axes.t && (dims.t - 2*borders.t > 1)) {
			prefimg.axes(Axes.T);
			final double[] array = new double[dims.t];
			final Coordinates coords = new Coordinates();
			for (coords.c=min.c; coords.c<=max.c; ++coords.c)
				for (coords.z=min.z; coords.z<=max.z; ++coords.z)
					for (coords.y=min.y; coords.y<=max.y; ++coords.y)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.t,BSPLINE5POLE1);
							causalanticausal(array,borders.t,BSPLINE5POLE2);
							scale(array,borders.t,BSPLINE5SCALE);
							prefimg.set(coords,array);
						}
		}
		
		// Prefilter in the c-dimension if requested:
		if (axes.c && (dims.c - 2*borders.c > 1)) {
			prefimg.axes(Axes.C);
			final double[] array = new double[dims.c];
			final Coordinates coords = new Coordinates();
			for (coords.t=min.t; coords.t<=max.t; ++coords.t)
				for (coords.z=min.z; coords.z<=max.z; ++coords.z)
					for (coords.y=min.y; coords.y<=max.y; ++coords.y)
						for (coords.x=min.x; coords.x<=max.x; ++coords.x) {
							prefimg.get(coords,array);
							causalanticausal(array,borders.c,BSPLINE5POLE1);
							causalanticausal(array,borders.c,BSPLINE5POLE2);
							scale(array,borders.c,BSPLINE5SCALE);
							prefimg.set(coords,array);
						}
		}
		
		return prefimg;
	}
	
	private void causalanticausal(final double[] array, final int border, final double pole) {
		
		final int iTotSize = array.length;
		final int iSize = iTotSize - 2*border;
		
		final int iMin = border;
		final int iMax = iMin + iSize - 1;
		
		double fS0,fS1,fZi,fZ2i;
		final double fS2 = -pole/(1-(pole*pole));
		final double fInvPole = 1/pole;
		
		// If the length of the array is smaller than the specified horizon, a
		// different initialization for the causal filter must be applied. In
		// order to avoid in-loop "if" statements, the two cases are treated
		// separately:
		if (iSize < HORIZON) {
			
			// Store original value at maximum for initialization of anti-causal filter:
			fS1 = array[iMax];
			
			// Initialize causal filter:
			fZi = pole;
			fZ2i = Math.pow(pole,iSize - 1);
			fS0 = array[iMin] + fZ2i*array[iMax];
			fZ2i = fZ2i * fZ2i * fInvPole;
			for (int i=iMin+1; i<iMax; ++i) {
				fS0 += (fZi + fZ2i)*array[i];
				fZi *= pole;
				fZ2i *= fInvPole;
			}
			fS0 /= (1 - fZi*fZi);
			
			// Apply causal filter:
			array[iMin] = fS0;
			for (int i=iMin+1; i<=iMax; ++i)
				array[i] += pole*array[i-1];
			
			// Initialize anti-causal filter:
			array[iMax] = fS2*(2*array[iMax] - fS1);
			
			// Apply anti-causal filter:
			for (int i=iMax-1; i>=iMin; --i)
				array[i] = pole*(array[i+1] - array[i]);
			
		} else {
			
			// Store original value at maximum for initialization of anti-causal filter:
			fS1 = array[iMax];
			
			// Initialize causal filter:
			fS0 = array[iMin];
			fZi = pole;
			for (int i=iMin+1; i<iMin+HORIZON; ++i) {
				fS0 += fZi * array[i];
				fZi *= pole;
			}
			
			// Apply causal filter:
			array[iMin] = fS0;
			for (int i=iMin+1; i<=iMax; ++i)
				array[i] += pole*array[i-1];
			
			// Initialize anti-causal filter:
			array[iMax] = fS2*(2*array[iMax] - fS1);
			
			// Apply anti-causal filter:
			for (int i=iMax-1; i>=iMin; --i)
				array[i] = pole*(array[i+1] - array[i]);
		}
	}
	
	private void scale(final double[] array, final int border, final double factor) {
		
		final int iMin = border;
		final int iMax = array.length - border - 1;
		
		for (int i=iMin; i<=iMax; ++i) array[i] *= factor;
	}
	
}
