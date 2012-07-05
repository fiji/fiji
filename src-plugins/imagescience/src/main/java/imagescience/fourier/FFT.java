package imagescience.fourier;

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

/** Computes forward and inverse Fourier transforms of images. The methods are based on the fast Fourier transform (FFT) and therefore accept only images whose size is an integer power of 2 in each direction in which the transformation is applied. Based on the algorithm described by W. H. Press, S. A. Teukolsky, W. T. Vetterling, B. P. Flannery, <a href="http://www.nr.com/" target="_new">Numerical Recipes in C: The Art of Scientific Computing</a> (2nd edition), Cambridge University Press, Cambridge, 1992, pp. 507-508. */
public class FFT {
	
	/** Default constructor. */
	public FFT() { }
	
	/** Applies the forward Fourier transform to complex-valued images along the specified axes.
		
		@param real the real part of the complex-valued input image. The image is overwritten with the real component of the forward Fourier transform. Therefore this should be a {@link FloatImage} object.
		
		@param imag the imaginary part of the complex-valued input image. The image is overwritten with the imaginary component of the forward Fourier transform. Therefore this should be a {@link FloatImage} object.
		
		@param axes the axes along which the transform is to be applied. The transform is applied to each dimension for which the corresponding boolean field of this parameter is {@code true}.
		
		@exception IllegalStateException if the images do not have the same size in each dimension or if their size is not an integer power of 2 in any of the directions in which the transform is to be applied.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public void forward(final Image real, final Image imag, final Axes axes) {
		
		messenger.log(ImageScience.prelude()+"Forward FFT");
		fft(real,imag,axes,-1);
	}
	
	/** Applies the inverse Fourier transform to complex-valued images along the specified axes.
		
		@param real the real part of the complex-valued input image. The image is overwritten with the real component of the inverse Fourier transform. Therefore this should be a {@link FloatImage} object.
		
		@param imag the imaginary part of the complex-valued input image. The image is overwritten with the imaginary component of the inverse Fourier transform. Therefore this should be a {@link FloatImage} object.
		
		@param axes the axes along which the transform is to be applied. The transform is applied to each dimension for which the corresponding boolean field of this parameter is {@code true}.
		
		@exception IllegalStateException if the images do not have the same size in each dimension or if their size is not an integer power of 2 in any of the directions in which the transform is to be applied.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public void inverse(final Image real, final Image imag, final Axes axes) {
		
		messenger.log(ImageScience.prelude()+"Inverse FFT");
		fft(real,imag,axes,+1);
	}
	
	private void fft(final Image real, final Image imag, final Axes axes, final int sign) {
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Initialize:
		check(real,imag,axes);
		final Coordinates c = new Coordinates();
		final Dimensions dims = real.dimensions();
		if (sign == -1) messenger.status("Forward FFT...");
		else messenger.status("Inverse FFT...");
		double[] re = null, im = null;
		double scale = 1;
		progressor.steps(
			(axes.x ? dims.c*dims.t*dims.z*dims.y : 0) +
			(axes.y ? dims.c*dims.t*dims.z*dims.x : 0) +
			(axes.z ? dims.c*dims.t*dims.z*dims.y : 0) +
			(axes.t ? dims.c*dims.t*dims.z*dims.y : 0) +
			(axes.c ? dims.c*dims.t*dims.z*dims.y : 0)
		);
		progressor.start();
		
		// Transform in x-dimension if active:
		if (axes.x) {
			messenger.log("   FFT in x-dimension...");
			scale *= dims.x;
			c.reset();
			real.axes(Axes.X);
			imag.axes(Axes.X);
			re = new double[dims.x];
			im = new double[dims.x];
			for (c.c=0; c.c<dims.c; ++c.c)
				for (c.t=0; c.t<dims.t; ++c.t)
					for (c.z=0; c.z<dims.z; ++c.z)
						for (c.y=0; c.y<dims.y; ++c.y) {
							real.get(c,re);
							imag.get(c,im);
							fft(re,im,sign);
							real.set(c,re);
							imag.set(c,im);
							progressor.step();
						}
		}
		
		// Transform in y-dimension if active:
		if (axes.y) {
			messenger.log("   FFT in y-dimension...");
			scale *= dims.y;
			c.reset();
			real.axes(Axes.Y);
			imag.axes(Axes.Y);
			re = new double[dims.y];
			im = new double[dims.y];
			for (c.c=0; c.c<dims.c; ++c.c)
				for (c.t=0; c.t<dims.t; ++c.t)
					for (c.z=0; c.z<dims.z; ++c.z)
						for (c.x=0; c.x<dims.x; ++c.x) {
							real.get(c,re);
							imag.get(c,im);
							fft(re,im,sign);
							real.set(c,re);
							imag.set(c,im);
							progressor.step();
						}
		}
		
		// Transform in z-dimension if active:
		if (axes.z) {
			messenger.log("   FFT in z-dimension...");
			scale *= dims.z;
			c.reset();
			real.axes(Axes.Z);
			imag.axes(Axes.Z);
			re = new double[dims.z];
			im = new double[dims.z];
			for (c.c=0; c.c<dims.c; ++c.c)
				for (c.t=0; c.t<dims.t; ++c.t)
					for (c.y=0; c.y<dims.y; ++c.y) {
						for (c.x=0; c.x<dims.x; ++c.x) {
							real.get(c,re);
							imag.get(c,im);
							fft(re,im,sign);
							real.set(c,re);
							imag.set(c,im);
						}
						progressor.step(dims.z);
					}
		}
		
		// Transform in t-dimension if active:
		if (axes.t) {
			messenger.log("   FFT in t-dimension...");
			scale *= dims.t;
			c.reset();
			real.axes(Axes.T);
			imag.axes(Axes.T);
			re = new double[dims.t];
			im = new double[dims.t];
			for (c.c=0; c.c<dims.c; ++c.c)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						for (c.x=0; c.x<dims.x; ++c.x) {
							real.get(c,re);
							imag.get(c,im);
							fft(re,im,sign);
							real.set(c,re);
							imag.set(c,im);
						}
						progressor.step(dims.t);
					}
		}
		
		// Transform in c-dimension if active:
		if (axes.c) {
			messenger.log("   FFT in c-dimension...");
			scale *= dims.c;
			c.reset();
			real.axes(Axes.C);
			imag.axes(Axes.C);
			re = new double[dims.c];
			im = new double[dims.c];
			for (c.t=0; c.t<dims.t; ++c.t)
				for (c.z=0; c.z<dims.z; ++c.z)
					for (c.y=0; c.y<dims.y; ++c.y) {
						for (c.x=0; c.x<dims.x; ++c.x) {
							real.get(c,re);
							imag.get(c,im);
							fft(re,im,sign);
							real.set(c,re);
							imag.set(c,im);
						}
						progressor.step(dims.c);
					}
		}
		
		// Scale correction in case of inverse transform:
		if (sign == 1) {
			messenger.log("   Scale correction...");
			real.divide(scale);
			imag.divide(scale);
		}
		
		messenger.log("Done");
		messenger.status("");
		progressor.stop();
		timer.stop();
	}
	
	private void check(final Image real, final Image imag, final Axes axes) {
		
		messenger.log("Real input image of type "+real.type());
		messenger.log("Imaginary input image of type "+imag.type());
		
		final Dimensions rdims = real.dimensions();
		final Dimensions idims = imag.dimensions();
		
		if (!rdims.equals(idims)) throw new IllegalStateException("Real and imaginary images have different dimensions");
		if (axes.x && !FMath.power2(rdims.x)) throw new IllegalStateException("Real and imaginary x-size not a power of 2");
		if (axes.y && !FMath.power2(rdims.y)) throw new IllegalStateException("Real and imaginary y-size not a power of 2");
		if (axes.z && !FMath.power2(rdims.z)) throw new IllegalStateException("Real and imaginary z-size not a power of 2");
		if (axes.t && !FMath.power2(rdims.t)) throw new IllegalStateException("Real and imaginary t-size not a power of 2");
		if (axes.c && !FMath.power2(rdims.c)) throw new IllegalStateException("Real and imaginary c-size not a power of 2");
	}
	
	// Modified version of the algorithm described in Numerical Recipes
	// in C: The Art of Scientific Computing, second edition, Cambridge
	// University Press, Cambridge, 1992, pp. 507-508.
	private void fft(final double[] real, final double[] imag, final int sign) {
		
		// Bit reversal:
		final int len = real.length;
		final int hlen = len/2;
		for (int i=0, j=0; i<len; ++i) {
			if (j > i) {
				final double rt = real[j]; real[j] = real[i]; real[i] = rt;
				final double it = imag[j]; imag[j] = imag[i]; imag[i] = it;
			}
			int m = hlen;
			while (m >= 2 && j >= m) { j -= m; m >>= 1; }
			j += m;
		}
		
		// Danielson-Lanczos algorithm:
		int N = 2, hN = 1;
		while (N <= len) {
			final double theta = sign*TWOPI/N;
			double tmp = Math.sin(0.5*theta);
			final double alpha = -2.0*tmp*tmp;
			final double beta = Math.sin(theta);
			double wr = 1.0;
			double wi = 0.0;
			for (int k=0; k<hN; ++k) {
				for (int i=k, j=k+hN; i<len; i+=N, j+=N) {
					final double tmpr = wr*real[j] - wi*imag[j];
					final double tmpi = wr*imag[j] + wi*real[j];
					real[j] = real[i] - tmpr;
					imag[j] = imag[i] - tmpi;
					real[i] += tmpr;
					imag[i] += tmpi;
				}
				wr += (tmp=wr)*alpha - wi*beta;
				wi += wi*alpha + tmp*beta;
			}
			hN = N; N <<= 1;
		}
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
	private static final double TWOPI = 2*Math.PI;
	
}
