package imagescience.transform;

import imagescience.image.Axes;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;

/** Crops images. */
public class Crop {
	
	/** Default constructor. */
	public Crop() { }
	
	/** Creates a cropped version of an image.
		
		@param image the image to be cropped.
		
		@param start {@code stop} - the start and stop (inclusive) coordinates for cropping in every dimension.
		
		@return a new image containing a copy of the input image within the given crop range. The returned image is of the same type as the input image.
		
		@exception IllegalArgumentException if any of the {@code start} or {@code stop} coordinates is out of range, or if the {@code start} coordinate is larger than the {@code stop} coordinate in any dimension.
		
		@exception NullPointerException if any of the parameters is {@code null}.
		
		@exception UnknownError if for any reason the output image could not be created. In most cases this will be due to insufficient free memory.
	*/
	public Image run(final Image image, final Coordinates start, final Coordinates stop) {
		
		messenger.log(ImageScience.prelude()+"Crop");
		
		// Initialize timer:
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Check parameters:
		messenger.log("Checking parameters");
		final Dimensions idims = image.dimensions();
		messenger.log("Input image dimensions: (x,y,z,t,c) = ("+idims.x+","+idims.y+","+idims.z+","+idims.t+","+idims.c+")");
		
		if (start.x < 0 || stop.x >= idims.x || start.x > stop.x)
			throw new IllegalArgumentException("Crop range invalid in x-dimension");
		
		if (start.y < 0 || stop.y >= idims.y || start.y > stop.y)
			throw new IllegalArgumentException("Crop range invalid in y-dimension");
		
		if (start.z < 0 || stop.z >= idims.z || start.z > stop.z)
			throw new IllegalArgumentException("Crop range invalid in z-dimension");
		
		if (start.t < 0 || stop.t >= idims.t || start.t > stop.t)
			throw new IllegalArgumentException("Crop range invalid in t-dimension");
		
		if (start.c < 0 || stop.c >= idims.c || start.c > stop.c)
			throw new IllegalArgumentException("Crop range invalid in c-dimension");
		
		messenger.log("Crop range in x-dimension: [" + start.x + "," + stop.x + "]");
		messenger.log("Crop range in y-dimension: [" + start.y + "," + stop.y + "]");
		messenger.log("Crop range in z-dimension: [" + start.z + "," + stop.z + "]");
		messenger.log("Crop range in t-dimension: [" + start.t + "," + stop.t + "]");
		messenger.log("Crop range in c-dimension: [" + start.c + "," + stop.c + "]");
		
		final Dimensions cdims = new Dimensions(stop.x - start.x + 1,
												stop.y - start.y + 1,
												stop.z - start.z + 1,
												stop.t - start.t + 1,
												stop.c - start.c + 1);
		
		messenger.log("Output image dimensions: (x,y,z,t,c) = ("+cdims.x+","+cdims.y+","+cdims.z+","+cdims.t+","+cdims.c+")");
		
		// Crop:
		messenger.log("Cropping "+image.type());
		final Image cropped = Image.create(cdims,image.type());
		image.axes(Axes.X); cropped.axes(Axes.X);
		final Coordinates ci = new Coordinates(); ci.x = start.x;
		final Coordinates cc = new Coordinates();
		final double[] a = new double[cdims.x];
		messenger.status("Cropping...");
		progressor.steps(cdims.c*cdims.t*cdims.z);
		progressor.start();
		
		for (ci.c=start.c, cc.c=0; ci.c<=stop.c; ++ci.c, ++cc.c) {
			for (ci.t=start.t, cc.t=0; ci.t<=stop.t; ++ci.t, ++cc.t) {
				for (ci.z=start.z, cc.z=0; ci.z<=stop.z; ++ci.z, ++cc.z) {
					for (ci.y=start.y, cc.y=0; ci.y<=stop.y; ++ci.y, ++cc.y) {
						image.get(ci,a);
						cropped.set(cc,a);
					}
					progressor.step();
				}
			}
		}
		
		// Finish up:
		cropped.name(image.name()+" cropped");
		cropped.aspects(image.aspects().duplicate());
		messenger.status("");
		progressor.stop();
		timer.stop();
		
		return cropped;
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
}
