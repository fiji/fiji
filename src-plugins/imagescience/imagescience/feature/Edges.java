package imagescience.feature;

import imagescience.array.DoubleArray;
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

/** Detects edges in images. */
public class Edges {
	
	/** Default constructor. */
	public Edges() { }
	
	/** Detects edges in images.
		
		@param image the input image in which edges are to be detected. If it is of type {@link FloatImage}, it will be used to store intermediate results. Otherwise it will be left unaltered. If the size of the image in the z-dimension equals {@code 1}, this method will compute, for every image element, the magnitude of the two-dimensional (2D) gradient vector. Otherwise it will compute for every image element the magnitude of the full three-dimensional (3D) gradient vector. These computations are performed on every x-y(-z) subimage in a 5D image.
		
		@param scale the smoothing scale at which the required image derivatives are computed. The scale is equal to the standard deviation of the Gaussian kernel used for differentiation and must be larger than {@code 0}. In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect-ratio value) in that dimension.
		
		@param nonmaxsup determines whether locally non-maximal gray-values are suppressed. To determine whether the gray-value of an image element is a local maximum, this method applies linear interpolation in the direction of the gradient vector to compute gray-values at approximately one sample distance on each side of the given element, which are subsequently compared to the gray-value of the given element.
		
		@return an image showing the locations of edges according to the algorithm. The returned image is always of type {@link FloatImage}.
		
		@exception IllegalArgumentException if {@code scale} is less than or equal to {@code 0}.
		
		@exception IllegalStateException if the size of the image elements (aspect-ratio value) is less than or equal to {@code 0} in the x-, y-, or z-dimension.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Image run(final Image image, final double scale, final boolean nonmaxsup) {
		
		messenger.log(ImageScience.prelude()+"Edges");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Initialize:
		messenger.log("Checking arguments");
		if (scale <= 0) throw new IllegalArgumentException("Smoothing scale less than or equal to 0");
		
		final Dimensions dims = image.dimensions();
		messenger.log("Input image dimensions: (x,y,z,t,c) = ("+dims.x+","+dims.y+","+dims.z+","+dims.t+","+dims.c+")");
		
		final Aspects asps = image.aspects();
		messenger.log("Element aspect-ratios: ("+asps.x+","+asps.y+","+asps.z+","+asps.t+","+asps.c+")");
		if (asps.x <= 0) throw new IllegalStateException("Aspect-ratio value in x-dimension less than or equal to 0");
		if (asps.y <= 0) throw new IllegalStateException("Aspect-ratio value in y-dimension less than or equal to 0");
		if (asps.z <= 0) throw new IllegalStateException("Aspect-ratio value in z-dimension less than or equal to 0");
		
		final String name = image.name();
		
		Image edgeImage = (image instanceof FloatImage) ? image : new FloatImage(image);
		
		differentiator.messenger.log(messenger.log());
		differentiator.progressor.parent(progressor);
		
		// Detect edges:
		if (dims.z == 1) { // 2D case
			
			double[] pls = {0, 0.5, 0.98, 1}; int pl = 0;
			if (nonmaxsup) pls = new double[] {0, 0.46, 0.9, 0.92, 1};
			
			// Compute gradient vector:
			logstatus("Computing Ix"); progressor.range(pls[pl],pls[++pl]);
			final Image Ix = differentiator.run(edgeImage.duplicate(),scale,1,0,0);
			logstatus("Computing Iy"); progressor.range(pls[pl],pls[++pl]);
			final Image Iy = differentiator.run(edgeImage,scale,0,1,0);
			
			// Compute gradient magnitude (Ix is reused to save memory in case non-maxima suppression is not applied):
			logstatus("Computing gradient magnitude");
			progressor.range(pls[pl],pls[++pl]);
			progressor.steps(dims.c*dims.t*dims.y);
			edgeImage = nonmaxsup ? new FloatImage(dims) : Ix;
			Ix.axes(Axes.X); Iy.axes(Axes.X); edgeImage.axes(Axes.X);
			final double[] aIx = new double[dims.x];
			final double[] aIy = new double[dims.x];
			final Coordinates coords = new Coordinates();
			
			progressor.start();
			for (coords.c=0; coords.c<dims.c; ++coords.c)
				for (coords.t=0; coords.t<dims.t; ++coords.t)
					for (coords.y=0; coords.y<dims.y; ++coords.y) {
						Ix.get(coords,aIx); Iy.get(coords,aIy);
						for (int x=0; x<dims.x; ++x)
							aIx[x] = Math.sqrt(aIx[x]*aIx[x] + aIy[x]*aIy[x]);
						edgeImage.set(coords,aIx);
						progressor.step();
					}
			progressor.stop();
			
			// Apply non-maxima suppression if requested (using mirror-boundary conditions and linear interpolation):
			if (nonmaxsup) {
				logstatus("Suppressing non-maxima");
				progressor.steps(dims.c*dims.t*dims.y);
				progressor.range(pls[pl],pls[++pl]);
				final Image supImage = Ix;
				final double[][] gm = new double[3][dims.x+2];
				final Coordinates cgm = new Coordinates();
				cgm.x = -1; coords.reset();
				
				progressor.start();
				if (dims.y == 1) {
					for (coords.c=0, cgm.c=0; coords.c<dims.c; ++coords.c, ++cgm.c)
						for (coords.t=0, cgm.t=0; coords.t<dims.t; ++coords.t, ++cgm.t) {
							// One row only:
							Ix.get(coords,aIx); Iy.get(coords,aIy);
							edgeImage.get(cgm,gm[0]); gm[2] = gm[1] = gm[0];
							suppress2D(gm,aIx,aIy);
							supImage.set(coords,aIx);
							progressor.step();
						}
				} else {
					double[] atmp = null;
					final int dimsym1 = dims.y - 1;
					for (coords.c=0, cgm.c=0; coords.c<dims.c; ++coords.c, ++cgm.c)
						for (coords.t=0, cgm.t=0; coords.t<dims.t; ++coords.t, ++cgm.t) {
							// First row:
							coords.y = 0; Ix.get(coords,aIx); Iy.get(coords,aIy);
							cgm.y = 0; edgeImage.get(cgm,gm[1]);
							cgm.y = 1; edgeImage.get(cgm,gm[0]); edgeImage.get(cgm,gm[2]);
							suppress2D(gm,aIx,aIy);
							supImage.set(coords,aIx);
							progressor.step();
							// Intermediate rows:
							for (coords.y=1, cgm.y=2; coords.y<dimsym1; ++coords.y, ++cgm.y) {
								Ix.get(coords,aIx); Iy.get(coords,aIy);
								atmp=gm[0]; gm[0]=gm[1]; gm[1]=gm[2]; gm[2]=atmp;
								edgeImage.get(cgm,gm[2]);
								suppress2D(gm,aIx,aIy);
								supImage.set(coords,aIx);
								progressor.step();
							}
							// Last row:
							Ix.get(coords,aIx); Iy.get(coords,aIy);
							atmp=gm[0]; gm[0]=gm[1]; gm[1]=gm[2]; gm[2]=atmp;
							cgm.y = dims.y-2; edgeImage.get(cgm,gm[2]);
							suppress2D(gm,aIx,aIy);
							supImage.set(coords,aIx);
							progressor.step();
						}
				}
				progressor.stop();
				edgeImage = supImage;
			}
			
		} else { // 3D case
			
			double[] pls = {0, 0.35, 0.7, 0.98, 1}; int pl = 0;
			if (nonmaxsup) pls = new double[] {0, 0.32, 0.64, 0.9, 0.92, 1};
			
			// Compute gradient vector:
			logstatus("Computing Ix"); progressor.range(pls[pl],pls[++pl]);
			final Image Ix = differentiator.run(edgeImage.duplicate(),scale,1,0,0);
			logstatus("Computing Iy"); progressor.range(pls[pl],pls[++pl]);
			final Image Iy = differentiator.run(edgeImage.duplicate(),scale,0,1,0);
			logstatus("Computing Iz"); progressor.range(pls[pl],pls[++pl]);
			final Image Iz = differentiator.run(edgeImage,scale,0,0,1);
			
			// Compute gradient magnitude (Ix is reused to save memory in case non-maxima suppression is not applied):
			logstatus("Computing gradient magnitude");
			progressor.steps(dims.c*dims.t*dims.z*dims.y);
			progressor.range(pls[pl],pls[++pl]);
			edgeImage = nonmaxsup ? new FloatImage(dims) : Ix;
			Ix.axes(Axes.X); Iy.axes(Axes.X); Iz.axes(Axes.X); edgeImage.axes(Axes.X);
			final double[] aIx = new double[dims.x];
			final double[] aIy = new double[dims.x];
			final double[] aIz = new double[dims.x];
			final Coordinates coords = new Coordinates();
			
			progressor.start();
			for (coords.c=0; coords.c<dims.c; ++coords.c)
				for (coords.t=0; coords.t<dims.t; ++coords.t)
					for (coords.z=0; coords.z<dims.z; ++coords.z)
						for (coords.y=0; coords.y<dims.y; ++coords.y) {
							Ix.get(coords,aIx); Iy.get(coords,aIy); Iz.get(coords,aIz);
							for (int x=0; x<dims.x; ++x)
							aIx[x] = Math.sqrt(aIx[x]*aIx[x] + aIy[x]*aIy[x] + aIz[x]*aIz[x]);
							edgeImage.set(coords,aIx);
							progressor.step();
						}
			progressor.stop();
			
			// Apply non-maxima suppression if requested (using mirror-boundary conditions and linear interpolation):
			if (nonmaxsup) {
				logstatus("Suppressing non-maxima");
				progressor.steps(dims.c*dims.t*dims.z);
				progressor.range(pls[pl],pls[++pl]);
				Ix.axes(Axes.X+Axes.Y);
				Iy.axes(Axes.X+Axes.Y);
				Iz.axes(Axes.X+Axes.Y);
				final Image supImage = Ix;
				edgeImage.axes(Axes.X+Axes.Y);
				final double[][][] gm = new double[3][dims.y+2][dims.x+2];
				final double[][] aaIx = new double[dims.y][dims.x];
				final double[][] aaIy = new double[dims.y][dims.x];
				final double[][] aaIz = new double[dims.y][dims.x];
				final Coordinates cgm = new Coordinates();
				cgm.y = cgm.x = -1; coords.reset();
				final int dimszm1 = dims.z - 1;
				double[][] atmp = null;
				
				progressor.start();
				for (coords.c=0, cgm.c=0; coords.c<dims.c; ++coords.c, ++cgm.c)
					for (coords.t=0, cgm.t=0; coords.t<dims.t; ++coords.t, ++cgm.t) {
						// First slice:
						coords.z = 0; Ix.get(coords,aaIx); Iy.get(coords,aaIy); Iz.get(coords,aaIz);
						cgm.z = 0; edgeImage.get(cgm,gm[1]);
						cgm.z = 1; edgeImage.get(cgm,gm[0]); edgeImage.get(cgm,gm[2]);
						suppress3D(gm,aaIx,aaIy,aaIz);
						supImage.set(coords,aaIx);
						progressor.step();
						// Intermediate slices:
						for (coords.z=1, cgm.z=2; coords.z<dimszm1; ++coords.z, ++cgm.z) {
							Ix.get(coords,aaIx); Iy.get(coords,aaIy); Iz.get(coords,aaIz);
							atmp=gm[0]; gm[0]=gm[1]; gm[1]=gm[2]; gm[2]=atmp;
							edgeImage.get(cgm,gm[2]);
							suppress3D(gm,aaIx,aaIy,aaIz);
							supImage.set(coords,aaIx);
							progressor.step();
						}
						// Last slice:
						Ix.get(coords,aaIx); Iy.get(coords,aaIy); Iz.get(coords,aaIz);
						atmp=gm[0]; gm[0]=gm[1]; gm[1]=gm[2]; gm[2]=atmp;
						cgm.z = dims.z-2; edgeImage.get(cgm,gm[2]);
						suppress3D(gm,aaIx,aaIy,aaIz);
						supImage.set(coords,aaIx);
						progressor.step();
					}
				progressor.stop();
				edgeImage = supImage;
			}
		}
		
		messenger.status("");
		
		timer.stop();
		
		edgeImage.name(name+" edges");
		
		return edgeImage;
	}
	
	private void suppress2D(final double[][] gm, final double[] aIx, final double[] aIy) {
		
		// Initialize:
		final int dimsx = aIx.length;
		final int dimsxp1 = dimsx + 1;
		final int dimsxm1 = dimsx - 1;
		double rx, ry, fx, fy, gmval, gmval1, gmval2;
		double fdx, fdy, f1mdx, f1mdy;
		int ix, iy, ixp1, iyp1;
		
		// Mirror x-borders:
		if (dimsx == 1) {
			gm[0][0] = gm[0][1]; gm[0][dimsxp1] = gm[0][dimsx];
			gm[1][0] = gm[1][1]; gm[1][dimsxp1] = gm[1][dimsx];
			gm[2][0] = gm[2][1]; gm[2][dimsxp1] = gm[2][dimsx];
		} else {
			gm[0][0] = gm[0][2]; gm[0][dimsxp1] = gm[0][dimsxm1];
			gm[1][0] = gm[1][2]; gm[1][dimsxp1] = gm[1][dimsxm1];
			gm[2][0] = gm[2][2]; gm[2][dimsxp1] = gm[2][dimsxm1];
		}
		
		// Suppress non-maxima:
		for (int x=0, xp1=1; x<dimsx; ++x, ++xp1) {
			
			gmval = gm[1][xp1];
			if (gmval == 0) aIx[x] = 0;
			else {
				// Compute direction vector:
				rx = 0.7f*aIx[x]/gmval;
				ry = 0.7f*aIy[x]/gmval;
				
				// Compute gradient magnitude in one direction:
				fx = xp1 + rx; fy = 1 + ry;
				ix = FMath.floor(fx); iy = FMath.floor(fy);
				ixp1 = ix + 1; iyp1 = iy + 1;
				fdx = fx - ix; fdy = fy - iy;
				f1mdx = 1 - fdx; f1mdy = 1 - fdy;
				
				gmval1 = (
					f1mdy*f1mdx*gm[iy][ix] +
					f1mdy*fdx*gm[iy][ixp1] +
					fdy*f1mdx*gm[iyp1][ix] +
					fdy*fdx*gm[iyp1][ixp1]
				);
				
				// Compute gradient magnitude in opposite direction:
				fx = xp1 - rx; fy = 1 - ry;
				ix = FMath.floor(fx); iy = FMath.floor(fy);
				ixp1 = ix + 1; iyp1 = iy + 1;
				fdx = fx - ix; fdy = fy - iy;
				f1mdx = 1 - fdx; f1mdy = 1 - fdy;
				
				gmval2 = (
					f1mdy*f1mdx*gm[iy][ix] +
					f1mdy*fdx*gm[iy][ixp1] +
					fdy*f1mdx*gm[iyp1][ix] +
					fdy*fdx*gm[iyp1][ixp1]
				);
				
				// Suppress current gradient magnitude if non-maximum:
				if (gmval1 >= gmval || gmval2 >= gmval) aIx[x] = 0;
				else aIx[x] = gmval;
			}
		}
	}
	
	private void suppress3D(final double[][][] gm, final double[][] aaIx, final double[][] aaIy, final double[][] aaIz) {
		
		// Initialize:
		final int dimsy = aaIx.length;
		final int dimsyp1 = dimsy + 1;
		final int dimsym1 = dimsy - 1;
		final int dimsx = aaIx[0].length;
		final int dimsxp1 = dimsx + 1;
		final int dimsxm1 = dimsx - 1;
		double rx, ry, rz, fx, fy, fz, gmval, gmval1, gmval2;
		double fdx, fdy, fdz, f1mdx, f1mdy, f1mdz;
		int ix, iy, iz, ixp1, iyp1, izp1;
		
		// Mirror x-borders:
		if (dimsx == 1) for (int z=0; z<3; ++z) {
			final double[][] slice = gm[z];
			for (int y=1; y<dimsyp1; ++y) { slice[y][0] = slice[y][1]; slice[y][dimsxp1] = slice[y][dimsx]; }
		} else for (int z=0; z<3; ++z) {
			final double[][] slice = gm[z];
			for (int y=1; y<dimsyp1; ++y) { slice[y][0] = slice[y][2]; slice[y][dimsxp1] = slice[y][dimsxm1]; }
		}
		
		// Mirror y-borders:
		if (dimsy == 1) for (int z=0; z<3; ++z) {
			final double[] y0 = gm[z][0];
			final double[] y1 = gm[z][1];
			final double[] y2 = gm[z][2];
			for (int x=0; x<=dimsxp1; ++x) y0[x] = y2[x] = y1[x];
		} else for (int z=0; z<3; ++z) {
			final double[] y0 = gm[z][0];
			final double[] y2 = gm[z][2];
			final double[] ydimsym1 = gm[z][dimsym1];
			final double[] ydimsyp1 = gm[z][dimsyp1];
			for (int x=0; x<=dimsxp1; ++x) { y0[x] = y2[x]; ydimsyp1[x] = ydimsym1[x]; }
		}
		
		// Suppress non-maxima:
		final double[][] gm1 = gm[1];
		
		for (int y=0, yp1=1; y<dimsy; ++y, ++yp1) {
			
			final double[] gm1yp1 = gm1[yp1];
			final double[] aIx = aaIx[y];
			final double[] aIy = aaIy[y];
			final double[] aIz = aaIz[y];
			
			for (int x=0, xp1=1; x<dimsx; ++x, ++xp1) {
				
				gmval = gm1yp1[xp1];
				if (gmval == 0) aIx[x] = 0;
				else {
					// Compute direction vector:
					rx = 0.7f*aIx[x]/gmval;
					ry = 0.7f*aIy[x]/gmval;
					rz = 0.7f*aIz[x]/gmval;
					
					// Compute gradient magnitude in one direction:
					fx = xp1 + rx; fy = yp1 + ry; fz = 1 + rz;
					ix = FMath.floor(fx); iy = FMath.floor(fy); iz = FMath.floor(fz);
					ixp1 = ix + 1; iyp1 = iy + 1; izp1 = iz + 1;
					fdx = fx - ix; fdy = fy - iy; fdz = fz - iz;
					f1mdx = 1 - fdx; f1mdy = 1 - fdy; f1mdz = 1 - fdz;
					
					gmval1 = (
						f1mdz*f1mdy*f1mdx*gm[iz][iy][ix] +
						f1mdz*f1mdy*fdx*gm[iz][iy][ixp1] +
						f1mdz*fdy*f1mdx*gm[iz][iyp1][ix] +
						f1mdz*fdy*fdx*gm[iz][iyp1][ixp1] +
						fdz*f1mdy*f1mdx*gm[izp1][iy][ix] +
						fdz*f1mdy*fdx*gm[izp1][iy][ixp1] +
						fdz*fdy*f1mdx*gm[izp1][iyp1][ix] +
						fdz*fdy*fdx*gm[izp1][iyp1][ixp1]
					);
					
					// Compute gradient magnitude in opposite direction:
					fx = xp1 - rx; fy = yp1 - ry; fz = 1 - rz;
					ix = FMath.floor(fx); iy = FMath.floor(fy); iz = FMath.floor(fz);
					ixp1 = ix + 1; iyp1 = iy + 1; izp1 = iz + 1;
					fdx = fx - ix; fdy = fy - iy; fdz = fz - iz;
					f1mdx = 1 - fdx; f1mdy = 1 - fdy; f1mdz = 1 - fdz;
					
					gmval2 = (
						f1mdz*f1mdy*f1mdx*gm[iz][iy][ix] +
						f1mdz*f1mdy*fdx*gm[iz][iy][ixp1] +
						f1mdz*fdy*f1mdx*gm[iz][iyp1][ix] +
						f1mdz*fdy*fdx*gm[iz][iyp1][ixp1] +
						fdz*f1mdy*f1mdx*gm[izp1][iy][ix] +
						fdz*f1mdy*fdx*gm[izp1][iy][ixp1] +
						fdz*fdy*f1mdx*gm[izp1][iyp1][ix] +
						fdz*fdy*fdx*gm[izp1][iyp1][ixp1]
					);
					
					// Suppress current gradient magnitude if non-maximum:
					if (gmval1 >= gmval || gmval2 >= gmval) aIx[x] = 0;
					else aIx[x] = gmval;
				}
			}
		}
	}
	
	private void logstatus(final String s) {
		
		messenger.log(s);
		messenger.status(s+"...");
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
	/** The object used for image differentiation. */
	public final Differentiator differentiator = new Differentiator();
	
}
