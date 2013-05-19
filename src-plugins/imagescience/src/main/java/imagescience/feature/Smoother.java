package imagescience.feature;

import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;

/** Smoothes images. */
public class Smoother {
	
	/** Default constructor. */
	public Smoother() { }
	
	/** Applies Gaussian smoothing to images.
		
		@param image the input image to be smoothed. If it is of type {@link FloatImage}, it will be overwritten with the smoothing results. Otherwise it will be left unaltered. The algorithm is applied to every x-y(-z) subimage in a 5D image.
		
		@param scale the smoothing scale at which derivatives are computed. The scale is equal to the standard deviation of the Gaussian smoothing kernel and must be larger than {@code 0}. In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect-ratio value) in that dimension.
		
		@return the smoothed image. The returned image is always of type {@link FloatImage}. If the input image is also of that type, the returned image is the same object, overwritten with the smoothing results.
		
		@exception IllegalArgumentException if {@code scale} is less than or equal to {@code 0}.
		
		@exception IllegalStateException if the size of the image elements (aspect-ratio value) is less than or equal to {@code 0} in the x-, y-, or z-dimension.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Image gauss(final Image image, final double scale) {
		
		messenger.log(ImageScience.prelude()+"Smoother");
		messenger.log("Gaussian smoothing using Differentiator");
		final Differentiator d = new Differentiator();
		d.messenger.log(messenger.log());
		d.progressor.parent(progressor);
		return d.run(image,scale,0,0,0);
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
}
