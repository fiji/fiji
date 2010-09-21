package imagescience.segment;

import imagescience.image.Axes;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;
import java.util.Vector;

/** Finds local extrema in images. */
public class Extremizer {
	
	/** The detection mode. */
	public static final int DETECT = 1;
	
	/** The extraction mode. */
	public static final int EXTRACT = 2;
	
	/** The maxima type. */
	public static final int MAXIMA = 4;
	
	/** The minima type. */
	public static final int MINIMA = 8;
	
	/** Default constructor. */
	public Extremizer() { }
	
	/** Finds local extrema in an image. A local extremum is defined as an image element whose value is either larger (maximum) or smaller (minimum) than those of all its neighboring elements. If the size of the image in the z-dimension equals {@code 1}, this method considers 8-connected neighbors in x-y space, otherwise it considers 26-connected neighbors in x-y-z space. For border elements, neighboring positions outside the image are ignored. The method searches for local extrema in every x-y(-z) subimage in a 5D image.
		
		@param image the input image in which local extrema are to be found.
		
		@param type the type of extrema to be found. Can be any or both (by addition) of {@link #MAXIMA} or {@link #MINIMA}.
		
		@param mode determines how the found extrema are stored and returned. Can be any or both (by addition) of {@link #DETECT} or {@link #EXTRACT}.
		
		@return if {@code mode} includes {@code DETECT}, the {@code image} is overwritten with the detection results: local maxima are set to {@code 255}, local minima to {@code 127}, and all other elements to {@code 0}. Otherwise the image is left unaltered. If {@code mode} includes {@code EXTRACT}, a new two-element {@code Vector} of {@code Vector<Coordinates>} objects is returned, containing the coordinates of all found local maxima (element {@code 0}) and local minima (element {@code 1}). Otherwise the method returns {@code null}. 
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Vector<Vector<Coordinates>> run(final Image image, final int type, final int mode) {
		
		messenger.log(ImageScience.prelude()+"Extremizer");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Initialize:
		final Dimensions dims = image.dimensions();
		messenger.log("Input image dimensions: (x,y,z,t,c) = ("+dims.x+","+dims.y+","+dims.z+","+dims.t+","+dims.c+")");
		
		final boolean detect = (mode & DETECT) > 0;
		final boolean extract = (mode & EXTRACT) > 0;
		String detex = null;
		if (detect) { detex = extract ? "Detecting and extracting" : "Detecting"; }
		else if (extract) { detex = "Extracting"; }
		else { messenger.log("Neither detection nor extraction mode was selected"); return null; }
		
		final boolean maxima = (type & MAXIMA) > 0;
		final boolean minima = (type & MINIMA) > 0;
		String maxmin = null;
		if (maxima) { maxmin = minima ? "maxima and minima" : "maxima"; }
		else if (minima) { maxmin = "minima"; }
		else { messenger.log("Neither maxima nor minima type was selected"); return null; }
		
		final String demm = detex + " " + maxmin;
		
		final Vector<Coordinates> camax = extract ? new Vector<Coordinates>(dims.x,dims.x) : null;
		final Vector<Coordinates> camin = extract ? new Vector<Coordinates>(dims.x,dims.x) : null;
		
		messenger.status("Finding extrema...");
		
		// Find extrema:
		if (dims.z == 1) { // 2D case
			
			messenger.log(demm+" in 2D using 8-connectivity");
			progressor.steps(dims.c*dims.t*dims.y);
			progressor.start();
			image.axes(Axes.X);
			final Coordinates c0 = new Coordinates(); c0.x = -1;
			final Coordinates c1 = new Coordinates(); c1.x = -1;
			double[] mm = new double[dims.x+2];
			double[] a0 = new double[dims.x+2];
			double[] a1 = new double[dims.x+2];
			double[] a2 = new double[dims.x+2];
			double[] at = null;
			final int dimsxp1 = dims.x + 1;
			final int dimsym1 = dims.y - 1;
			final boolean dimsyis1 = (dims.y == 1);
			
			for (c0.c=c1.c=0; c0.c<dims.c; ++c0.c, ++c1.c) {
				for (c0.t=c1.t=0; c0.t<dims.t; ++c0.t, ++c1.t) {
					for (c0.y=0, c1.y=1; c0.y<dims.y; ++c0.y, ++c1.y) {
						set(mm,NEX);
						if (c0.y == 0) {
							image.get(c0,a1);
							if (!dimsyis1) image.get(c1,a2);
						} else {
							at=a0; a0=a1; a1=a2; a2=at;
							if (c0.y < dimsym1) image.get(c1,a2);
						}
						// Find maxima:
						if (maxima) {
							if (c0.y == 0) { set(a0,DMIN); if (dimsyis1) set(a2,DMIN); }
							else if (c0.y == dimsym1) set(a2,DMIN);
							border(a0,DMIN); border(a1,DMIN); border(a2,DMIN);
							for (int x=1, xm1=0, xp1=2; x<dimsxp1; ++x, ++xm1, ++xp1) {
								final double vx = a1[x];
								if (vx > a0[xm1] && vx > a0[x] && vx > a0[xp1] &&
									vx > a2[xm1] && vx > a2[x] && vx > a2[xp1] &&
									vx > a1[xm1] && vx > a1[xp1]) {
									if (extract) camax.add(new Coordinates(xm1,c0.y,0,c0.t,c0.c));
									mm[x] = MAX;
								}
							}
						}
						// Find minima:
						if (minima) {
							if (c0.y == 0) { set(a0,DMAX); if (dimsyis1) set(a2,DMAX); }
							else if (c0.y == dimsym1) set(a2,DMAX);
							border(a0,DMAX); border(a1,DMAX); border(a2,DMAX);
							for (int x=1, xm1=0, xp1=2; x<dimsxp1; ++x, ++xm1, ++xp1) {
								final double vx = a1[x];
								if (vx < a0[xm1] && vx < a0[x] && vx < a0[xp1] &&
									vx < a2[xm1] && vx < a2[x] && vx < a2[xp1] &&
									vx < a1[xm1] && vx < a1[xp1]) {
									if (extract) camin.add(new Coordinates(xm1,c0.y,0,c0.t,c0.c));
									mm[x] = MIN;
								}
							}
						}
						if (detect) image.set(c0,mm);
						progressor.step();
					}
				}
			}
		} else { // 3D case
			
			messenger.log(demm+" in 3D using 26-connectivity");
			final int ysteps = (maxima?dims.y:0) + (minima?dims.y:0);
			progressor.steps(dims.c*dims.t*dims.z*ysteps);
			progressor.start();
			image.axes(Axes.X+Axes.Y);
			final Coordinates c0 = new Coordinates(); c0.x = c0.y = -1;
			final Coordinates c1 = new Coordinates(); c1.x = c1.y = -1;
			double[][] mm = new double[dims.y+2][dims.x+2];
			double[][] a0 = new double[dims.y+2][dims.x+2];
			double[][] a1 = new double[dims.y+2][dims.x+2];
			double[][] a2 = new double[dims.y+2][dims.x+2];
			double[][] at = null;
			final int dimsxp1 = dims.x + 1;
			final int dimsyp1 = dims.y + 1;
			final int dimszm1 = dims.z - 1;
			
			for (c0.c=c1.c=0; c0.c<dims.c; ++c0.c, ++c1.c) {
				for (c0.t=c1.t=0; c0.t<dims.t; ++c0.t, ++c1.t) {
					for (c0.z=0, c1.z=1; c0.z<dims.z; ++c0.z, ++c1.z) {
						set(mm,NEX);
						if (c0.z == 0) {
							image.get(c0,a1);
							image.get(c1,a2);
						} else {
							at=a0; a0=a1; a1=a2; a2=at;
							if (c0.z < dimszm1) image.get(c1,a2);
						}
						// Find maxima:
						if (maxima) {
							if (c0.z == 0) set(a0,DMIN);
							else if (c0.z == dimszm1) set(a2,DMIN);
							border(a0,DMIN); border(a1,DMIN); border(a2,DMIN);
							for (int y=1, ym1=0, yp1=2; y<dimsyp1; ++y, ++ym1, ++yp1) {
								final double[] a0ym1 = a0[ym1], a0y = a0[y], a0yp1 = a0[yp1];
								final double[] a1ym1 = a1[ym1], a1y = a1[y], a1yp1 = a1[yp1];
								final double[] a2ym1 = a2[ym1], a2y = a2[y], a2yp1 = a2[yp1];
								final double[] mmy = mm[y];
								for (int x=1, xm1=0, xp1=2; x<dimsxp1; ++x, ++xm1, ++xp1) {
									final double vx = a1y[x];
									if (vx > a0ym1[xm1] && vx > a0ym1[x] && vx > a0ym1[xp1] &&
										vx > a0y[xm1]   && vx > a0y[x]   && vx > a0y[xp1]   &&
										vx > a0yp1[xm1] && vx > a0yp1[x] && vx > a0yp1[xp1] &&
										vx > a1ym1[xm1] && vx > a1ym1[x] && vx > a1ym1[xp1] &&
										vx > a1y[xm1]   && vx > a1y[xp1] &&
										vx > a1yp1[xm1] && vx > a1yp1[x] && vx > a1yp1[xp1] &&
										vx > a2ym1[xm1] && vx > a2ym1[x] && vx > a2ym1[xp1] &&
										vx > a2y[xm1]   && vx > a2y[x]   && vx > a2y[xp1]   &&
										vx > a2yp1[xm1] && vx > a2yp1[x] && vx > a2yp1[xp1]) {
										if (extract) camax.add(new Coordinates(xm1,ym1,c0.z,c0.t,c0.c));
										mmy[x] = MAX;
									}
								}
								progressor.step();
							}
						}
						// Find minima:
						if (minima) {
							if (c0.z == 0) set(a0,DMAX);
							else if (c0.z == dimszm1) set(a2,DMAX);
							border(a0,DMAX); border(a1,DMAX); border(a2,DMAX);
							for (int y=1, ym1=0, yp1=2; y<dimsyp1; ++y, ++ym1, ++yp1) {
								final double[] a0ym1 = a0[ym1], a0y = a0[y], a0yp1 = a0[yp1];
								final double[] a1ym1 = a1[ym1], a1y = a1[y], a1yp1 = a1[yp1];
								final double[] a2ym1 = a2[ym1], a2y = a2[y], a2yp1 = a2[yp1];
								final double[] mmy = mm[y];
								for (int x=1, xm1=0, xp1=2; x<dimsxp1; ++x, ++xm1, ++xp1) {
									final double vx = a1y[x];
									if (vx < a0ym1[xm1] && vx < a0ym1[x] && vx < a0ym1[xp1] &&
										vx < a0y[xm1]   && vx < a0y[x]   && vx < a0y[xp1]   &&
										vx < a0yp1[xm1] && vx < a0yp1[x] && vx < a0yp1[xp1] &&
										vx < a1ym1[xm1] && vx < a1ym1[x] && vx < a1ym1[xp1] &&
										vx < a1y[xm1]   && vx < a1y[xp1] &&
										vx < a1yp1[xm1] && vx < a1yp1[x] && vx < a1yp1[xp1] &&
										vx < a2ym1[xm1] && vx < a2ym1[x] && vx < a2ym1[xp1] &&
										vx < a2y[xm1]   && vx < a2y[x]   && vx < a2y[xp1]   &&
										vx < a2yp1[xm1] && vx < a2yp1[x] && vx < a2yp1[xp1]) {
										if (extract) camin.add(new Coordinates(xm1,ym1,c0.z,c0.t,c0.c));
										mmy[x] = MIN;
									}
								}
								progressor.step();
							}
						}
						if (detect) image.set(c0,mm);
					}
				}
			}
		}
		
		// Finish up:
		messenger.status("");
		progressor.stop();
		timer.stop();
		
		if (detect) image.name(image.name() + " extrema");
		
		final Vector<Vector<Coordinates>> vvc = new Vector<Vector<Coordinates>>(2);
		vvc.add(camax); vvc.add(camin);
		return vvc;
	}
	
	private void set(final double[] a, final double v) {
		
		for (int i=0; i<a.length; ++i) a[i] = v;
	}
	
	private void set(final double[][] a, final double v) {
		
		final int ylen = a.length;
		final int xlen = a[0].length;
		for (int y=0; y<ylen; ++y) {
			final double[] ay = a[y];
			for (int x=0; x<xlen; ++x) {
				ay[x] = v;
			}
		}
	}
	
	private void border(final double[] a, final double v) {
		
		a[0] = a[a.length-1] = v;
	}
	
	private void border(final double[][] a, final double v) {
		
		// Initialize:
		final int ylenm1 = a.length - 1;
		final int xlen = a[0].length;
		final int xlenm1 = xlen - 1;
		final double[] ay0 = a[0];
		final double[] aylenm1 = a[ylenm1];
		
		// Left-right:
		for (int y=1; y<ylenm1; ++y) {
			a[y][0] = a[y][xlenm1] = v;
		}
		
		// Top-bottom:
		for (int x=0; x<xlen; ++x) {
			ay0[x] = aylenm1[x] = v;
		}
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
	private final static double DMAX = Double.MAX_VALUE;
	private final static double DMIN = -Double.MAX_VALUE;
	
	private final static double MAX = 255; // Local maximum indicator
	private final static double MIN = 127; // Local minimum indicator
	private final static double NEX = 0; // No extremum indicator
	
}
