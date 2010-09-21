package imagescience.feature;

import imagescience.image.Aspects;
import imagescience.image.Axes;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.utility.FMath;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;

/** Computes Gaussian derivatives of images. */
public class Differentiator {
	
	/** The largest supported order of differentiation. Currently this is {@code 10}. */
	public static final int MAX_ORDER = 10;
	
	/** Default constructor. */
	public Differentiator() { }
	
	/** Computes derivatives of images.
		
		@param image the input image to be differentiated. If it is of type {@link FloatImage}, it will be overwritten with the differentiation results. Otherwise it will be left unaltered.
		
		@param scale the smoothing scale at which derivatives are to be computed. The scale is equal to the standard deviation of the Gaussian smoothing kernel and must be larger than {@code 0}. In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect-ratio value) in that dimension.
		
		@param xorder {@code yorder} - {@code zorder} - the order of differentiation in the x-, y-, and z-dimension. The largest supported order of differentiation is {@link #MAX_ORDER}. If the order is {@code 0} in any dimension, the method applies just smoothing in that dimension, at the given {@code scale}.
		
		@return the derivative image. The returned image is always of type {@link FloatImage}. If the input image is also of that type, the returned image is the same object, overwritten with the differentiation results.
		
		@exception IllegalArgumentException if {@code scale} is less than or equal to {@code 0}, or if any of {@code xorder}, {@code yorder}, or {@code zorder} is less than {@code 0} or larger than {@link #MAX_ORDER}.
		
		@exception IllegalStateException if the size of the image elements (aspect-ratio value) is less than or equal to {@code 0} in the x-, y-, or z-dimension.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Image run(final Image image, final double scale, final int xorder, final int yorder, final int zorder) {
		
		messenger.log(ImageScience.prelude()+"Differentiator");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Initialize:
		check(scale,xorder,yorder,zorder);
		
		final Dimensions dims = image.dimensions();
		messenger.log("Input image dimensions: (x,y,z,t,c) = ("+dims.x+","+dims.y+","+dims.z+","+dims.t+","+dims.c+")");
		
		final Aspects asps = image.aspects();
		messenger.log("Element aspect-ratios: ("+asps.x+","+asps.y+","+asps.z+","+asps.t+","+asps.c+")");
		if (asps.x <= 0) throw new IllegalStateException("Aspect-ratio value in x-dimension less than or equal to 0");
		if (asps.y <= 0) throw new IllegalStateException("Aspect-ratio value in y-dimension less than or equal to 0");
		if (asps.z <= 0) throw new IllegalStateException("Aspect-ratio value in z-dimension less than or equal to 0");
		
		final Image deriv = (image instanceof FloatImage) ? image : new FloatImage(image);
		
		progressor.steps(
			(dims.x > 1 ? dims.c*dims.t*dims.z*dims.y : 0) +
			(dims.y > 1 ? dims.c*dims.t*dims.z*dims.x : 0) +
			(dims.z > 1 ? dims.c*dims.t*dims.z*dims.y : 0)
		);
		progressor.start();
		
		// Differentiation in x-dimension:
		if (dims.x == 1) {
			if (xorder == 0) {
				messenger.log("No operation in x-dimension");
			} else {
				messenger.log("Zeroing in x-dimension");
				deriv.set(0);
			}
		} else {
			final double xscale = scale/asps.x;
			messenger.log("Operating at scale "+scale+"/"+asps.x+" = "+xscale+" pixels");
			logstatus(info(xorder)+" in x-dimension...");
			final double[] kernel = kernel(xscale,xorder,dims.x);
			final int klenm1 = kernel.length - 1;
			final double[] ain = new double[dims.x + 2*klenm1];
			final double[] aout = new double[dims.x];
			final Coordinates coords = new Coordinates();
			deriv.axes(Axes.X);
			for (coords.c=0; coords.c<dims.c; ++coords.c)
				for (coords.t=0; coords.t<dims.t; ++coords.t)
					for (coords.z=0; coords.z<dims.z; ++coords.z)
						for (coords.y=0; coords.y<dims.y; ++coords.y) {
							coords.x = -klenm1; deriv.get(coords,ain);
							convolve(ain,aout,kernel);
							coords.x = 0; deriv.set(coords,aout);
							progressor.step();
						}
		}
		
		// Differentiation in y-dimension:
		if (dims.y == 1) {
			if (yorder == 0) {
				messenger.log("No operation in y-dimension");
			} else {
				messenger.log("Zeroing in y-dimension");
				deriv.set(0);
			}
		} else {
			final double yscale = scale/asps.y;
			messenger.log("Operating at scale "+scale+"/"+asps.y+" = "+yscale+" pixels");
			logstatus(info(yorder)+" in y-dimension...");
			final double[] kernel = kernel(yscale,yorder,dims.y);
			final int klenm1 = kernel.length - 1;
			final double[] ain = new double[dims.y + 2*klenm1];
			final double[] aout = new double[dims.y];
			final Coordinates coords = new Coordinates();
			deriv.axes(Axes.Y);
			for (coords.c=0; coords.c<dims.c; ++coords.c)
				for (coords.t=0; coords.t<dims.t; ++coords.t)
					for (coords.z=0; coords.z<dims.z; ++coords.z)
						for (coords.x=0; coords.x<dims.x; ++coords.x) {
							coords.y = -klenm1; deriv.get(coords,ain);
							convolve(ain,aout,kernel);
							coords.y = 0; deriv.set(coords,aout);
							progressor.step();
						}
		}
		
		// Differentiation in z-dimension:
		if (dims.z == 1) {
			if (zorder == 0) {
				messenger.log("No operation in z-dimension");
			} else {
				messenger.log("Zeroing in z-dimension");
				deriv.set(0);
			}
		} else {
			final double zscale = scale/asps.z;
			messenger.log("Operating at scale "+scale+"/"+asps.z+" = "+zscale+" slices");
			logstatus(info(zorder)+" in z-dimension...");
			final double[] kernel = kernel(zscale,zorder,dims.z);
			final int klenm1 = kernel.length - 1;
			final double[] ain = new double[dims.z + 2*klenm1];
			final double[] aout = new double[dims.z];
			final Coordinates coords = new Coordinates();
			deriv.axes(Axes.Z);
			for (coords.c=0; coords.c<dims.c; ++coords.c)
				for (coords.t=0; coords.t<dims.t; ++coords.t)
					for (coords.y=0; coords.y<dims.y; ++coords.y) {
						for (coords.x=0; coords.x<dims.x; ++coords.x) {
							coords.z = -klenm1; deriv.get(coords,ain);
							convolve(ain,aout,kernel);
							coords.z = 0; deriv.set(coords,aout);
						}
						progressor.step(dims.z);
					}
		}
		
		messenger.status("");
		progressor.stop();
		timer.stop();
		
		deriv.name(image.name()+" dx"+xorder+" dy"+yorder+" dz"+zorder);
		
		return deriv;
	}
	
	private double[] kernel(final double s, final int d, final int m) {
		
		// Initialize:
		double r = 5;
		if (d == 0) r = 3;
		else if (d <= 2) r = 4;
		int h = (int)(s*r) + 1;
		if (h > m) h = m;
		final double[] kernel = new double[h];
		kernel[0] = (d == 0) ? 1 : 0;
		
		// Compute kernel:
		if (h > 1) {
			final double is2 = 1/(s*s);
			final double is4 = is2*is2;
			final double is6 = is4*is2;
			final double is8 = is6*is2;
			final double is10 = is8*is2;
			final double mis2 = -0.5*is2;
			final double sq2pi = Math.sqrt(2*Math.PI);
			switch (d) {
				case 0: {
					double integral = 0;
					for (int k=0; k<h; ++k) {
						kernel[k] = Math.exp(k*k*mis2);
						integral += kernel[k];
					}
					integral *= 2.0;
					integral -= kernel[0];
					for (int k=0; k<h; ++k)
						kernel[k] /= integral;
					break;
				}
				case 1: {
					final double c = -is2/(sq2pi*s);
					for (int k=1; k<h; ++k) {
						final double k2 = k*k;
						kernel[k] = c*k*Math.exp(k2*mis2);
					}
					break;
				}
				case 2: {
					final double c = is2/(sq2pi*s);
					for (int k=0; k<h; ++k) {
						final double k2 = k*k;
						kernel[k] = c*(k2*is2 - 1)*Math.exp(k2*mis2);
					}
					break;
				}
				case 3: {
					final double c = -is4/(sq2pi*s);
					for (int k=1; k<h; ++k) {
						final double k2 = k*k;
						kernel[k] = c*k*(k2*is2 - 3)*Math.exp(k2*mis2);
					}
					break;
				}
				case 4: {
					final double c = is4/(sq2pi*s);
					for (int k=0; k<h; ++k) {
						final double k2 = k*k;
						kernel[k] = c*(k2*k2*is4 - 6*k2*is2 + 3)*Math.exp(k2*mis2);
					}
					break;
				}
				case 5: {
					final double c = -is6/(sq2pi*s);
					for (int k=1; k<h; ++k) {
						final double k2 = k*k;
						kernel[k] = c*k*(k2*k2*is4 - 10*k2*is2 + 15)*Math.exp(k2*mis2);
					}
					break;
				}
				case 6: {
					final double c = is6/(sq2pi*s);
					for (int k=0; k<h; ++k) {
						final double k2 = k*k;
						final double k4 = k2*k2;
						kernel[k] = c*(k4*k2*is6 - 15*k4*is4 + 45*k2*is2 - 15)*Math.exp(k2*mis2);
					}
					break;
				}
				case 7: {
					final double c = -is8/(sq2pi*s);
					for (int k=1; k<h; ++k) {
						final double k2 = k*k;
						final double k4 = k2*k2;
						kernel[k] = c*k*(k4*k2*is6 - 21*k4*is4 + 105*k2*is2 - 105)*Math.exp(k2*mis2);
					}
					break;
				}
				case 8: {
					final double c = is8/(sq2pi*s);
					for (int k=0; k<h; ++k) {
						final double k2 = k*k;
						final double k4 = k2*k2;
						kernel[k] = c*(k4*k4*is8 - 28*k4*k2*is6 + 210*k4*is4 - 420*k2*is2 + 105)*Math.exp(k2*mis2);
					}
					break;
				}
				case 9: {
					final double c = -is10/(sq2pi*s);
					for (int k=1; k<h; ++k) {
						final double k2 = k*k;
						final double k4 = k2*k2;
						kernel[k] = c*k*(k4*k4*is8 - 36*k4*k2*is6 + 378*k4*is4 - 1260*k2*is2 + 945)*Math.exp(k2*mis2);
					}
					break;
				}
				case 10: {
					final double c = is10/(sq2pi*s);
					for (int k=0; k<h; ++k) {
						final double k2 = k*k;
						final double k4 = k2*k2;
						final double k6 = k4*k2;
						kernel[k] = c*(k6*k4*is10 - 45*k4*k4*is8 + 630*k6*is6 - 3150*k4*is4 + 4725*k2*is2 - 945)*Math.exp(k2*mis2);
					}
					break;
				}
			}
		}
		
		return kernel;
	}
	
	private void convolve(final double[] ain, final double[] aout, final double[] kernel) {
		
		// Mirror borders in input array:
		final int khlenm1 = kernel.length - 1;
		final int aolenm1 = aout.length - 1;
		for (int k=0, lm=khlenm1, lp=khlenm1, hm=khlenm1+aolenm1, hp=khlenm1+aolenm1; k<khlenm1; ++k) {
			ain[--lm] = ain[++lp];
			ain[++hp] = ain[--hm];
		}
		
		// Convolve with kernel:
		final double sign = (kernel[0] == 0) ? -1 : 1;
		for (int io=0, ii=khlenm1; io<=aolenm1; ++io, ++ii) {
			double convres = ain[ii]*kernel[0];
			for (int k=1, iimk=ii, iipk=ii; k<=khlenm1; ++k)
				convres += (ain[--iimk] + sign*ain[++iipk])*kernel[k];
			aout[io] = convres;
		}
	}
	
	private String info(final int d) {
		
		String info = null;
		switch (d) {
			case  0: info = "Smoothing"; break;
			case  1: info = "First"; break;
			case  2: info = "Second"; break;
			case  3: info = "Third"; break;
			case  4: info = "Fourth"; break;
			case  5: info = "Fifth"; break;
			case  6: info = "Sixth"; break;
			case  7: info = "Seventh"; break;
			case  8: info = "Eighth"; break;
			case  9: info = "Nineth"; break;
			case 10: info = "Tenth"; break;
		}
		if (d > 0) info += "-order differentiation";
		return info;
	}
	
	private void check(final double scale, final int xorder, final int yorder, final int zorder) {
		
		messenger.log("Checking arguments");
		
		if (scale <= 0) throw new IllegalArgumentException("Smoothing scale less than or equal to 0");
		if (xorder < 0 || xorder > MAX_ORDER) throw new IllegalArgumentException("Differentiation order out of range in x-dimension");
		if (yorder < 0 || yorder > MAX_ORDER) throw new IllegalArgumentException("Differentiation order out of range in y-dimension");
		if (zorder < 0 || zorder > MAX_ORDER) throw new IllegalArgumentException("Differentiation order out of range in z-dimension");
	}
	
	private void logstatus(final String s) {
		
		messenger.log(s);
		messenger.status(s);
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
}
