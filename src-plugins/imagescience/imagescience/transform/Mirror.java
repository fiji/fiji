package imagescience.transform;

import imagescience.image.Axes;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;

/** Mirrors images. */
public class Mirror {
	
	/** Default constructor. */
	public Mirror() { }
	
	/** Mirrors an image.
		
		@param image the input image to be mirrored. The image is overwritten with the results of mirroring.
		
		@param axes indicates the axes along which to mirror. The image is mirrored in each dimension for which the corresponding boolean field of this parameter is {@code true}.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public void run(final Image image, final Axes axes) {
		
		messenger.log(ImageScience.prelude()+"Mirror");
		
		// Initialize timer:
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Check parameters:
		messenger.log("Checking parameters");
		final Dimensions dims = image.dimensions();
		messenger.log("Input image dimensions: (x,y,z,t,c) = ("+dims.x+","+dims.y+","+dims.z+","+dims.t+","+dims.c+")");
		
		// Mirror input image:
		messenger.log("Mirroring "+image.type());
		progressor.steps(
			(axes.x ? dims.c*dims.t*dims.z : 0) +
			(axes.y ? dims.c*dims.t*dims.z : 0) +
			(axes.z ? dims.c*dims.t*(1+(dims.z-1)/2) : 0) +
			(axes.t ? dims.c*(1+(dims.t-1)/2)*dims.z : 0) +
			(axes.c ? (1+(dims.c-1)/2)*dims.t*dims.z : 0)
		);
		progressor.start();
		image.axes(Axes.X);
		
		// Mirror in x-dimension if requested:
		if (axes.x) {
			messenger.log("Mirroring in x-dimension");
			messenger.status("Mirroring in x-dimension...");
			final Coordinates c = new Coordinates();
			final double[] a = new double[dims.x];
			final int dimsxm1 = dims.x - 1;
			final int maxx = dimsxm1/2;
			for (c.c=0; c.c<dims.c; ++c.c) {
				for (c.t=0; c.t<dims.t; ++c.t) {
					for (c.z=0; c.z<dims.z; ++c.z) {
						for (c.y=0; c.y<dims.y; ++c.y) {
							image.get(c,a);
							for (int x1=0, x2=dimsxm1; x1<=maxx; ++x1, --x2) {
								final double tmp = a[x2];
								a[x2] = a[x1];
								a[x1] = tmp;
							}
							image.set(c,a);
						}
						progressor.step();
					}
				}
			}
		}
		
		// Mirror in y-dimension if requested:
		if (axes.y) {
			messenger.log("Mirroring in y-dimension");
			messenger.status("Mirroring in y-dimension...");
			final Coordinates c1 = new Coordinates();
			final Coordinates c2 = new Coordinates();
			final double[] a1 = new double[dims.x];
			final double[] a2 = new double[dims.x];
			final int dimsym1 = dims.y - 1;
			final int maxy = dimsym1/2;
			for (c1.c=c2.c=0; c1.c<dims.c; ++c1.c, ++c2.c) {
				for (c1.t=c2.t=0; c1.t<dims.t; ++c1.t, ++c2.t) {
					for (c1.z=c2.z=0; c1.z<dims.z; ++c1.z, ++c2.z) {
						for (c1.y=0, c2.y=dimsym1; c1.y<=maxy; ++c1.y, --c2.y) {
							image.get(c1,a1);
							image.get(c2,a2);
							image.set(c1,a2);
							image.set(c2,a1);
						}
						progressor.step();
					}
				}
			}
		}
		
		// Mirror in z-dimension if requested:
		if (axes.z) {
			messenger.log("Mirroring in z-dimension");
			messenger.status("Mirroring in z-dimension...");
			final Coordinates c1 = new Coordinates();
			final Coordinates c2 = new Coordinates();
			final double[] a1 = new double[dims.x];
			final double[] a2 = new double[dims.x];
			final int dimszm1 = dims.z - 1;
			final int maxz = dimszm1/2;
			for (c1.c=c2.c=0; c1.c<dims.c; ++c1.c, ++c2.c) {
				for (c1.t=c2.t=0; c1.t<dims.t; ++c1.t, ++c2.t) {
					for (c1.z=0, c2.z=dimszm1; c1.z<=maxz; ++c1.z, --c2.z) {
						for (c1.y=c2.y=0; c1.y<dims.y; ++c1.y, ++c2.y) {
							image.get(c1,a1);
							image.get(c2,a2);
							image.set(c1,a2);
							image.set(c2,a1);
						}
						progressor.step();
					}
				}
			}
		}
		
		// Mirror in t-dimension if requested:
		if (axes.t) {
			messenger.log("Mirroring in t-dimension");
			messenger.status("Mirroring in t-dimension...");
			final Coordinates c1 = new Coordinates();
			final Coordinates c2 = new Coordinates();
			final double[] a1 = new double[dims.x];
			final double[] a2 = new double[dims.x];
			final int dimstm1 = dims.t - 1;
			final int maxt = dimstm1/2;
			for (c1.c=c2.c=0; c1.c<dims.c; ++c1.c, ++c2.c) {
				for (c1.t=0, c2.t=dimstm1; c1.t<=maxt; ++c1.t, --c2.t) {
					for (c1.z=c2.z=0; c1.z<dims.z; ++c1.z, ++c2.z) {
						for (c1.y=c2.y=0; c1.y<dims.y; ++c1.y, ++c2.y) {
							image.get(c1,a1);
							image.get(c2,a2);
							image.set(c1,a2);
							image.set(c2,a1);
						}
						progressor.step();
					}
				}
			}
		}
		
		// Mirror in c-dimension if requested:
		if (axes.c) {
			messenger.log("Mirroring in c-dimension");
			messenger.status("Mirroring in c-dimension...");
			final Coordinates c1 = new Coordinates();
			final Coordinates c2 = new Coordinates();
			final double[] a1 = new double[dims.x];
			final double[] a2 = new double[dims.x];
			final int dimscm1 = dims.c - 1;
			final int maxc = dimscm1/2;
			for (c1.c=0, c2.c=dimscm1; c1.c<=maxc; ++c1.c, --c2.c) {
				for (c1.t=c2.t=0; c1.t<dims.t; ++c1.t, ++c2.t) {
					for (c1.z=c2.z=0; c1.z<dims.z; ++c1.z, ++c2.z) {
						for (c1.y=c2.y=0; c1.y<dims.y; ++c1.y, ++c2.y) {
							image.get(c1,a1);
							image.get(c2,a2);
							image.set(c1,a2);
							image.set(c2,a1);
						}
						progressor.step();
					}
				}
			}
		}
		
		// Finish up:
		image.name(image.name()+" mirrored");
		messenger.status("");
		progressor.stop();
		timer.stop();
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
}
