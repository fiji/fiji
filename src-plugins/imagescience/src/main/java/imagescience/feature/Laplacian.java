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

/** Computes the Laplacian of images. */
public class Laplacian {
	
	/** Default constructor. */
	public Laplacian() { }
	
	/** Computes the Laplacian of images.
		
		@param image the input image for which the Laplacian needs to be computed. If it is of type {@link FloatImage}, it will be used to store intermediate results. Otherwise it will be left unaltered. If the size of the image in the z-dimension equals {@code 1}, this method will compute, for every image element, the two-dimensional (2D) Laplacian. Otherwise it will compute for every image element the full three-dimensional (3D) Laplacian. These computations are performed on every x-y(-z) subimage in a 5D image.
		
		@param scale the smoothing scale at which the required image derivatives are computed. The scale is equal to the standard deviation of the Gaussian kernel used for differentiation and must be larger than {@code 0}. In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect-ratio value) in that dimension.
		
		@return an image showing the Laplacian of the input image. The returned image is always of type {@link FloatImage}.
		
		@exception IllegalArgumentException if {@code scale} is less than or equal to {@code 0}.
		
		@exception IllegalStateException if the size of the image elements (aspect-ratio value) is less than or equal to {@code 0} in the x-, y-, or z-dimension.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Image run(final Image image, final double scale) {
		
		messenger.log(ImageScience.prelude()+"Laplacian");
		
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
		
		final Image lapImage = (image instanceof FloatImage) ? image : new FloatImage(image);
		final String name = image.name();
		
		differentiator.messenger.log(messenger.log());
		differentiator.progressor.parent(progressor);
		
		if (dims.z == 1) { // 2D case
			
			final double[] pls = {0, 0.55, 0.99, 1}; int pl = 0;
			
			// Compute Laplacian components:
			logstatus("Computing Ixx");
			progressor.range(pls[pl],pls[++pl]);
			final Image Ixx = differentiator.run(lapImage.duplicate(),scale,2,0,0);
			logstatus("Computing Iyy");
			progressor.range(pls[pl],pls[++pl]);
			final Image Iyy = differentiator.run(lapImage,scale,0,2,0);
			
			// Compute Laplacian:
			logstatus("Computing Laplacian");
			progressor.range(pls[pl],pls[++pl]);
			progressor.steps(1);
			progressor.start();
			lapImage.add(Ixx); // lapImage = Iyy
			progressor.step();
			
		} else { // 3D case
			
			final double[] pls = {0, 0.35, 0.7, 0.98, 1}; int pl = 0;
			
			// Compute Laplacian components:
			logstatus("Computing Ixx");
			progressor.range(pls[pl],pls[++pl]);
			final Image Ixx = differentiator.run(lapImage.duplicate(),scale,2,0,0);
			logstatus("Computing Iyy");
			progressor.range(pls[pl],pls[++pl]);
			final Image Iyy = differentiator.run(lapImage.duplicate(),scale,0,2,0);
			logstatus("Computing Izz");
			progressor.range(pls[pl],pls[++pl]);
			final Image Izz = differentiator.run(lapImage,scale,0,0,2);
			
			// Compute Laplacian:
			logstatus("Computing Laplacian");
			progressor.range(pls[pl],pls[++pl]);
			progressor.steps(2);
			progressor.start();
			lapImage.add(Iyy); // lapImage = Izz
			progressor.step();
			lapImage.add(Ixx);
			progressor.step();
		}
		
		messenger.status("");
		progressor.stop();
		timer.stop();
		
		lapImage.name(name+" Laplacian");
		
		return lapImage;
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
