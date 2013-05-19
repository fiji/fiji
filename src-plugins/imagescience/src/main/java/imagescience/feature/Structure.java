package imagescience.feature;

import imagescience.image.Aspects;
import imagescience.image.Axes;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;
import java.util.Vector;

/** Computes eigenimages of the structure tensor. */
public class Structure {
	
	/** Default constructor. */
	public Structure() { }
	
	/** Computes structure tensor eigenimages of images.
		
		@param image the input image for which structure-tensor eigenimages need to be computed. If it is of type {@link FloatImage}, it will be used to store intermediate results. Otherwise it will be left unaltered. If the size of the image in the z-dimension equals {@code 1}, this method will compute, for every image element, the two-dimensional (2D) structure tensor and its two eigenvalues. Otherwise it will compute for every image element the full three-dimensional (3D) structure tensor and its three eigenvalues. These computations are performed on every x-y(-z) subimage in a 5D image.
		
		@param sscale the smoothing scale at which the required image derivatives are computed. The scale is equal to the standard deviation of the Gaussian kernel used for differentiation and must be larger than {@code 0}. In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect-ratio value) in that dimension.
		
		@param iscale the integration scale. This scale is equal to the standard deviation of the Gaussian kernel used for integrating the components of the structure tensor and must be larger than {@code 0}. In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect-ratio value) in that dimension.
		
		@return an array containing the eigenimages. The images are always of type {@link FloatImage}.<br>
		If only the two-dimensional (2D) structure tensor and its two eigenvalues were computed for every image element, the returned array contains two eigenimages:<br>
		Element {@code 0} = the image with, for every element, the largest eigenvalue,<br>
		Element {@code 1} = the image with, for every element, the smallest eigenvalue.<br>
		If the full three-dimensional (3D) structure tensor and its three eigenvalues were computed for every image element, the returned array contains three eigenimages:<br>
		Element {@code 0} = the image with, for every element, the largest eigenvalue,<br>
		Element {@code 1} = the image with, for every element, the middle eigenvalue,<br>
		Element {@code 2} = the image with, for every element, the smallest eigenvalue.
		
		@exception IllegalArgumentException if {@code sscale} or {@code iscale} is less than or equal to {@code 0}.
		
		@exception IllegalStateException if the size of the image elements (aspect-ratio value) is less than or equal to {@code 0} in the x-, y-, or z-dimension.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Vector<Image> run(final Image image, final double sscale, final double iscale) {
		
		messenger.log(ImageScience.prelude()+"Structure");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Initialize:
		messenger.log("Checking arguments");
		if (sscale <= 0) throw new IllegalArgumentException("Smoothing scale less than or equal to 0");
		if (iscale <= 0) throw new IllegalArgumentException("Integration scale less than or equal to 0");
		
		final Dimensions dims = image.dimensions();
		messenger.log("Input image dimensions: (x,y,z,t,c) = ("+dims.x+","+dims.y+","+dims.z+","+dims.t+","+dims.c+")");
		
		final Aspects asps = image.aspects();
		messenger.log("Element aspect-ratios: ("+asps.x+","+asps.y+","+asps.z+","+asps.t+","+asps.c+")");
		if (asps.x <= 0) throw new IllegalStateException("Aspect-ratio value in x-dimension less than or equal to 0");
		if (asps.y <= 0) throw new IllegalStateException("Aspect-ratio value in y-dimension less than or equal to 0");
		if (asps.z <= 0) throw new IllegalStateException("Aspect-ratio value in z-dimension less than or equal to 0");
		
		final Image smoothImage = (image instanceof FloatImage) ? image : new FloatImage(image);
		Vector<Image> eigenimages = null;
		final String name = image.name();
		
		differentiator.messenger.log(messenger.log());
		differentiator.progressor.parent(progressor);
		
		// Compute structure tensor and eigenimages:
		if (dims.z == 1) { // 2D case
			
			final double[] pls = {0, 0.2, 0.4, 0.45, 0.63, 0.80, 0.95, 1}; int pl = 0;
			
			// Compute structure tensor components:
			logstatus("Computing Ix"); progressor.range(pls[pl],pls[++pl]);
			final Image Ix2 = differentiator.run(smoothImage.duplicate(),sscale,1,0,0);
			logstatus("Computing Iy"); progressor.range(pls[pl],pls[++pl]);
			final Image Iy2 = differentiator.run(smoothImage,sscale,0,1,0);
			
			progressor.range(pls[pl],pls[++pl]);
			progressor.steps(3);
			progressor.start();
			final Image IxIy = Ix2.duplicate();
			logstatus("Computing IxIy"); IxIy.multiply(Iy2); progressor.step();
			logstatus("Squaring Ix"); Ix2.square(); progressor.step();
			logstatus("Squaring Iy"); Iy2.square(); progressor.step();
			progressor.stop();
			
			// Integrate tensor components:
			messenger.log("Gaussian integration at scale "+iscale);
			logstatus("Integrating IxIx"); progressor.range(pls[pl],pls[++pl]);
			differentiator.run(Ix2,iscale,0,0,0);
			logstatus("Integrating IxIy"); progressor.range(pls[pl],pls[++pl]);
			differentiator.run(IxIy,iscale,0,0,0);
			logstatus("Integrating IyIy"); progressor.range(pls[pl],pls[++pl]);
			differentiator.run(Iy2,iscale,0,0,0);
			
			// Compute eigenimages (Ix2 and Iy2 are reused to save memory):
			logstatus("Computing eigenimages");
			progressor.steps(dims.c*dims.t*dims.y);
			progressor.range(pls[pl],pls[++pl]);
			Ix2.axes(Axes.X); IxIy.axes(Axes.X); Iy2.axes(Axes.X);
			final double[] axx = new double[dims.x];
			final double[] axy = new double[dims.x];
			final double[] ayy = new double[dims.x];
			final Coordinates coords = new Coordinates();
			messenger.log("Comparing and storing eigenvalues");
			
			progressor.start();
			for (coords.c=0; coords.c<dims.c; ++coords.c)
				for (coords.t=0; coords.t<dims.t; ++coords.t)
					for (coords.y=0; coords.y<dims.y; ++coords.y) {
						Ix2.get(coords,axx);
						IxIy.get(coords,axy);
						Iy2.get(coords,ayy);
						for (int x=0; x<dims.x; ++x) {
							final double b = -(axx[x] + ayy[x]);
							final double c = axx[x]*ayy[x] - axy[x]*axy[x];
							final double q = -0.5*(b + (b < 0 ? -1 : 1)*Math.sqrt(b*b - 4*c));
							double absh1, absh2;
							if (q == 0) {
								absh1 = 0;
								absh2 = 0;
							} else {
								absh1 = Math.abs(q);
								absh2 = Math.abs(c/q);
							}
							if (absh1 > absh2) {
								axx[x] = absh1;
								ayy[x] = absh2;
							} else {
								axx[x] = absh2;
								ayy[x] = absh1;
							}
						}
						Ix2.set(coords,axx);
						Iy2.set(coords,ayy);
						progressor.step();
					}
			progressor.stop();
			
			Ix2.name(name+" largest structure eigenvalues");
			Iy2.name(name+" smallest structure eigenvalues");
			
			Ix2.aspects(asps.duplicate());
			Iy2.aspects(asps.duplicate());
			
			eigenimages = new Vector<Image>(2);
			eigenimages.add(Ix2);
			eigenimages.add(Iy2);
			
		} else { // 3D case
			
			final double[] pls = {0, 0.1, 0.2, 0.3, 0.34, 0.40, 0.46, 0.52, 0.58, 0.64, 0.7, 1}; int pl = 0;
			
			// Compute structure tensor components:
			logstatus("Computing Ix"); progressor.range(pls[pl],pls[++pl]);
			final Image Ix2 = differentiator.run(smoothImage.duplicate(),sscale,1,0,0);
			logstatus("Computing Iy"); progressor.range(pls[pl],pls[++pl]);
			final Image Iy2 = differentiator.run(smoothImage.duplicate(),sscale,0,1,0);
			logstatus("Computing Iz"); progressor.range(pls[pl],pls[++pl]);
			final Image Iz2 = differentiator.run(smoothImage,sscale,0,0,1);
			
			progressor.range(pls[pl],pls[++pl]);
			progressor.steps(6);
			progressor.start();
			logstatus("Computing IxIy"); final Image IxIy = Ix2.duplicate(); IxIy.multiply(Iy2); progressor.step();
			logstatus("Computing IxIz"); final Image IxIz = Ix2.duplicate(); IxIz.multiply(Iz2); progressor.step();
			logstatus("Computing IyIz"); final Image IyIz = Iy2.duplicate(); IyIz.multiply(Iz2); progressor.step();
			logstatus("Squaring Ix"); Ix2.square(); progressor.step();
			logstatus("Squaring Iy"); Iy2.square(); progressor.step();
			logstatus("Squaring Iz"); Iz2.square(); progressor.step();
			progressor.stop();
			
			// Integrate tensor components:
			messenger.log("Gaussian integration at scale "+iscale);
			logstatus("Integrating IxIx"); progressor.range(pls[pl],pls[++pl]);
			differentiator.run(Ix2,iscale,0,0,0);
			logstatus("Integrating IxIy"); progressor.range(pls[pl],pls[++pl]);
			differentiator.run(IxIy,iscale,0,0,0);
			logstatus("Integrating IxIz"); progressor.range(pls[pl],pls[++pl]);
			differentiator.run(IxIz,iscale,0,0,0);
			logstatus("Integrating IyIy"); progressor.range(pls[pl],pls[++pl]);
			differentiator.run(Iy2,iscale,0,0,0);
			logstatus("Integrating IyIz"); progressor.range(pls[pl],pls[++pl]);
			differentiator.run(IyIz,iscale,0,0,0);
			logstatus("Integrating IzIz"); progressor.range(pls[pl],pls[++pl]);
			differentiator.run(Iz2,iscale,0,0,0);
			
			// Compute eigenimages (Ix2, Iy2, Iz2 are reused to save memory):
			logstatus("Computing eigenimages");
			progressor.steps(dims.c*dims.t*dims.z*dims.y);
			progressor.range(pls[pl],pls[++pl]);
			Ix2.axes(Axes.X); IxIy.axes(Axes.X); IxIz.axes(Axes.X);
			Iy2.axes(Axes.X); IyIz.axes(Axes.X); Iz2.axes(Axes.X);
			final double[] axx = new double[dims.x];
			final double[] axy = new double[dims.x];
			final double[] axz = new double[dims.x];
			final double[] ayy = new double[dims.x];
			final double[] ayz = new double[dims.x];
			final double[] azz = new double[dims.x];
			final Coordinates coords = new Coordinates();
			messenger.log("Comparing and storing eigenvalues");
			
			progressor.start();
			for (coords.c=0; coords.c<dims.c; ++coords.c)
				for (coords.t=0; coords.t<dims.t; ++coords.t)
					for (coords.z=0; coords.z<dims.z; ++coords.z)
						for (coords.y=0; coords.y<dims.y; ++coords.y) {
							Ix2.get(coords,axx);
							IxIy.get(coords,axy);
							IxIz.get(coords,axz);
							Iy2.get(coords,ayy);
							IyIz.get(coords,ayz);
							Iz2.get(coords,azz);
							for (int x=0; x<dims.x; ++x) {
								final double fxx = axx[x];
								final double fxy = axy[x];
								final double fxz = axz[x];
								final double fyy = ayy[x];
								final double fyz = ayz[x];
								final double fzz = azz[x];
								final double a = -(fxx + fyy + fzz);
								final double b = fxx*fyy + fxx*fzz + fyy*fzz - fxy*fxy - fxz*fxz - fyz*fyz;
								final double c = fxx*(fyz*fyz - fyy*fzz) + fyy*fxz*fxz + fzz*fxy*fxy - 2*fxy*fxz*fyz;
								final double q = (a*a - 3*b)/9;
								final double r = (a*a*a - 4.5f*a*b + 13.5f*c)/27f;
								final double sqrtq = (q > 0) ? Math.sqrt(q) : 0;
								final double sqrtq3 = sqrtq*sqrtq*sqrtq;
								double absh1, absh2, absh3;
								if (sqrtq3 == 0) {
									absh1 = 0;
									absh2 = 0;
									absh3 = 0;
								} else {
									final double rsqq3 = r/sqrtq3;
									final double angle = (rsqq3*rsqq3 <= 1) ? Math.acos(rsqq3) : Math.acos(rsqq3 < 0 ? -1 : 1);
									absh1 = Math.abs(-2*sqrtq*Math.cos(angle/3) - a/3);
									absh2 = Math.abs(-2*sqrtq*Math.cos((angle + TWOPI)/3) - a/3);
									absh3 = Math.abs(-2*sqrtq*Math.cos((angle - TWOPI)/3) - a/3);
								}
								if (absh2 < absh3) { final double tmp = absh2; absh2 = absh3; absh3 = tmp; }
								if (absh1 < absh2) { final double tmp1 = absh1; absh1 = absh2; absh2 = tmp1;
								if (absh2 < absh3) { final double tmp2 = absh2; absh2 = absh3; absh3 = tmp2; }}
								axx[x] = absh1;
								ayy[x] = absh2;
								azz[x] = absh3;
							}
							Ix2.set(coords,axx);
							Iy2.set(coords,ayy);
							Iz2.set(coords,azz);
							progressor.step();
						}
			progressor.stop();
			
			Ix2.name(name+" largest structure eigenvalues");
			Iy2.name(name+" middle structure eigenvalues");
			Iz2.name(name+" smallest structure eigenvalues");
			
			Ix2.aspects(asps.duplicate());
			Iy2.aspects(asps.duplicate());
			Iz2.aspects(asps.duplicate());
			
			eigenimages = new Vector<Image>(3);
			eigenimages.add(Ix2);
			eigenimages.add(Iy2);
			eigenimages.add(Iz2);
		}
		
		messenger.status("");
		
		timer.stop();
		
		return eigenimages;
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
	
	private static final double TWOPI = 2*Math.PI;
	
}
