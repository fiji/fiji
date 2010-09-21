package imagescience.segment;

import imagescience.image.Axes;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;

/** Detects zero-crossings in images. */
public class ZeroCrosser {
	
	/** Default constructor. */
	public ZeroCrosser() { }
	
	/** Detects zero-crossings in an image.
		
		@param image the input image in which zero-crossings are to be detected. The image is overwritten with the detection results. To determine whether a zero-crossing is present, this method compares the signs of neighboring image elements, and if they are different, it uses a linear interpolation scheme to determine the element whose center is closest to the location of the crossing. This element is then set to {@code 255}. Other elements are set to {@code 0}. If the size of the image in the z-dimension equals {@code 1}, this method compares neighboring elements in 2D (x-y space), otherwise it compares neighboring elements in 3D (x-y-z space). The algorithm is applied to every x-y(-z) subimage in a 5D image.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public void run(final Image image) {
		
		messenger.log(ImageScience.prelude()+"ZeroCrosser");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Initialize:
		final Dimensions dims = image.dimensions();
		messenger.log("Input image dimensions: (x,y,z,t,c) = ("+dims.x+","+dims.y+","+dims.z+","+dims.t+","+dims.c+")");
		
		messenger.status("Detecting zero crossings...");
		
		// Detect:
		if (dims.z == 1) { // 2D case
			
			messenger.log("Detecting 2D zero crossings");
			progressor.steps(dims.c*dims.t*dims.y);
			progressor.start();
			image.axes(Axes.X);
			final Coordinates crds0 = new Coordinates();
			final Coordinates crds1 = new Coordinates();
			double[] img0 = new double[dims.x];
			double[] img1 = new double[dims.x];
			double[] ltmp = null;
			double[] zer0 = new double[dims.x];
			double[] zer1 = new double[dims.x];
			double[] ztmp = null;
			final int dimsxm1 = dims.x - 1;
			final int dimsym1 = dims.y - 1;
			
			for (crds0.c=crds1.c=0; crds0.c<dims.c; ++crds0.c, ++crds1.c)
				for (crds0.t=crds1.t=0; crds0.t<dims.t; ++crds0.t, ++crds1.t) {
					crds1.y=0; image.get(crds1,img1);
					for (int x=0; x<dims.x; ++x) zer1[x] = OFF;
					// Top and intermediate rows:
					for (crds0.y=0, crds1.y=1; crds0.y<dimsym1; ++crds0.y, ++crds1.y) {
						ltmp=img0; img0=img1; img1=ltmp; image.get(crds1,img1);
						ztmp=zer0; zer0=zer1; zer1=ztmp; for (int x=0; x<dims.x; ++x) zer1[x] = OFF;
						for (int x=0, xp1=1; x<dimsxm1; ++x, ++xp1) {
							// Horizontal zero or sign changes:
							if (img0[x]*img0[xp1] <= 0 && (img0[x]-img0[xp1]) != 0)
								if (img0[x]/(img0[x]-img0[xp1]) < 0.5) zer0[x] = ON;
								else zer0[xp1] = ON;
							// Vertical zero or sign changes:
							if (img0[x]*img1[x] <= 0 && (img0[x]-img1[x]) != 0)
								if (img0[x]/(img0[x]-img1[x]) < 0.5) zer0[x] = ON;
								else zer1[x] = ON;
						}
						// Last column vertical zero or sign changes:
						if (img0[dimsxm1]*img1[dimsxm1] <= 0 && (img0[dimsxm1]-img1[dimsxm1]) != 0)
							if (img0[dimsxm1]/(img0[dimsxm1]-img1[dimsxm1]) < 0.5) zer0[dimsxm1] = ON;
							else zer1[dimsxm1] = ON;
						image.set(crds0,zer0);
						progressor.step();
					}
					// Last row horizontal zero or sign changes:
					for (int x=0, xp1=1; x<dimsxm1; ++x, ++xp1)
						if (img1[x]*img1[xp1] <= 0 && (img1[x]-img1[xp1]) != 0)
							if (img1[x]/(img1[x]-img1[xp1]) < 0.5) zer1[x] = ON;
							else zer1[xp1] = ON;
					image.set(crds0,zer1);
					progressor.step();
				}
			
		} else { // 3D case
			
			messenger.log("Detecting 3D zero crossings");
			progressor.steps(dims.c*dims.t*dims.z*dims.y);
			progressor.start();
			image.axes(Axes.X+Axes.Y);
			final Coordinates crds0 = new Coordinates();
			final Coordinates crds1 = new Coordinates();
			double[][] img0 = new double[dims.y][dims.x];
			double[][] img1 = new double[dims.y][dims.x];
			double[][] zer0 = new double[dims.y][dims.x];
			double[][] zer1 = new double[dims.y][dims.x];
			double[][] ltmp, ztmp;
			double[] img0y, img0yp1, img1y, img1yp1;
			double[] zer0y, zer0yp1, zer1y, zer1yp1;
			final int dimsxm1 = dims.x - 1;
			final int dimsym1 = dims.y - 1;
			final int dimszm1 = dims.z - 1;
			
			for (crds0.c=crds1.c=0; crds0.c<dims.c; ++crds0.c, ++crds1.c)
				for (crds0.t=crds1.t=0; crds0.t<dims.t; ++crds0.t, ++crds1.t) {
					crds1.z=0; image.get(crds1,img1);
					for (int y=0; y<dims.y; ++y)
						for (int x=0; x<dims.x; ++x) zer1[y][x] = OFF;
					// Front and intermediate slices:
					for (crds0.z=0, crds1.z=1; crds0.z<dimszm1; ++crds0.z, ++crds1.z) {
						ltmp=img0; img0=img1; img1=ltmp; image.get(crds1,img1);
						ztmp=zer0; zer0=zer1; zer1=ztmp;
						for (int y=0; y<dims.y; ++y)
							for (int x=0; x<dims.x; ++x) zer1[y][x] = OFF;
						// Top and intermediate rows:
						for (int y=0, yp1=1; y<dimsym1; ++y, ++yp1) {
							img0y=img0[y]; img0yp1=img0[yp1]; img1y=img1[y];
							zer0y=zer0[y]; zer0yp1=zer0[yp1]; zer1y=zer1[y];
							for (int x=0, xp1=1; x<dimsxm1; ++x, ++xp1) {
								// Horizontal zero or sign changes:
								if (img0y[x]*img0y[xp1] <= 0 && (img0y[x]-img0y[xp1]) != 0)
									if (img0y[x]/(img0y[x]-img0y[xp1]) < 0.5) zer0y[x] = ON;
									else zer0y[xp1] = ON;
								// Vertical zero or sign changes:
								if (img0y[x]*img0yp1[x] <= 0 && (img0y[x]-img0yp1[x]) != 0)
									if (img0y[x]/(img0y[x]-img0yp1[x]) < 0.5) zer0y[x] = ON;
									else zer0yp1[x] = ON;
								// Depth zero or sign changes:
								if (img0y[x]*img1y[x] <= 0 && (img0y[x]-img1y[x]) != 0)
									if (img0y[x]/(img0y[x]-img1y[x]) < 0.5) zer0y[x] = ON;
									else zer1y[x] = ON;
							}
							// Last column vertical zero or sign changes:
							if (img0y[dimsxm1]*img0yp1[dimsxm1] <= 0 && (img0y[dimsxm1]-img0yp1[dimsxm1]) != 0)
								if (img0y[dimsxm1]/(img0y[dimsxm1]-img0yp1[dimsxm1]) < 0.5) zer0y[dimsxm1] = ON;
								else zer0yp1[dimsxm1] = ON;
							// Last column depth zero or sign changes:
							if (img0y[dimsxm1]*img1y[dimsxm1] <= 0 && (img0y[dimsxm1]-img1y[dimsxm1]) != 0)
								if (img0y[dimsxm1]/(img0y[dimsxm1]-img1y[dimsxm1]) < 0.5) zer0y[dimsxm1] = ON;
								else zer1y[dimsxm1] = ON;
							progressor.step();
						}
						img0y = img0[dimsym1]; img1y = img1[dimsym1];
						zer0y = zer0[dimsym1]; zer1y = zer1[dimsym1];
						// Last row horizontal zero or sign changes:
						for (int x=0, xp1=1; x<dimsxm1; ++x, ++xp1)
							if (img0y[x]*img0y[xp1] <= 0 && (img0y[x]-img0y[xp1]) != 0)
								if (img0y[x]/(img0y[x]-img0y[xp1]) < 0.5) zer0y[x] = ON;
								else zer0y[xp1] = ON;
						// Last row depth zero or sign changes:
						for (int x=0; x<dims.x; ++x)
							if (img0y[x]*img1y[x] <= 0 && (img0y[x]-img1y[x]) != 0)
								if (img0y[x]/(img0y[x]-img1y[x]) < 0.5) zer0y[x] = ON;
								else zer1y[x] = ON;
						image.set(crds0,zer0);
						progressor.step();
					} // end z-loop
					// Last slice:
					for (int y=0, yp1=1; y<dimsym1; ++y, ++yp1) {
						img1y=img1[y]; img1yp1=img1[yp1];
						zer1y=zer1[y]; zer1yp1=zer1[yp1];
						for (int x=0, xp1=1; x<dimsxm1; ++x, ++xp1) {
							// Horizontal zero or sign changes:
							if (img1y[x]*img1y[xp1] <= 0 && (img1y[x]-img1y[xp1]) != 0)
								if (img1y[x]/(img1y[x]-img1y[xp1]) < 0.5) zer1y[x] = ON;
								else zer1y[xp1] = ON;
							// Vertical zero or sign changes:
							if (img1y[x]*img1yp1[x] <= 0 && (img1y[x]-img1yp1[x]) != 0)
								if (img1y[x]/(img1y[x]-img1yp1[x]) < 0.5) zer1y[x] = ON;
								else zer1yp1[x] = ON;
						}
						// Last column vertical zero or sign changes:
						if (img1y[dimsxm1]*img1yp1[dimsxm1] <= 0 && (img1y[dimsxm1]-img1yp1[dimsxm1]) != 0)
							if (img1y[dimsxm1]/(img1y[dimsxm1]-img1yp1[dimsxm1]) < 0.5) zer1y[dimsxm1] = ON;
							else zer1yp1[dimsxm1] = ON;
						progressor.step();
					}
					img1y=img1[dimsym1]; zer1y=zer1[dimsym1];
					// Last row horizontal zero or sign changes:
					for (int x=0, xp1=1; x<dimsxm1; ++x, ++xp1)
						if (img1y[x]*img1y[xp1] <= 0 && (img1y[x]-img1y[xp1]) != 0)
							if (img1y[x]/(img1y[x]-img1y[xp1]) < 0.5) zer1y[x] = ON;
							else zer1y[xp1] = ON;
					image.set(crds0,zer1);
					progressor.step();
				} // end t-loop
		}
		
		// Finish up:
		image.name(image.name() + " zero-crossings");
		messenger.status("");
		progressor.stop();
		timer.stop();
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
	private final static double ON = 255;
	private final static double OFF = 0;
	
}
