package imagescience.random;

import imagescience.image.Axes;
import imagescience.image.ByteImage;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;
import java.lang.Math;

/** Randomizes images. */
public class Randomizer {
	
	/** Additive insertion of random numbers. For each image element, a random number is generated from the selected distribution and is added to the original value. */
	public final static int ADDITIVE = 0;
	
	/** Multiplicative insertion of random numbers. For each image element, a random number is generated from the selected distribution which multiplies the original value. */
	public final static int MULTIPLICATIVE = 1;
	
	/** Modulatory insertion of random numbers. For each image element, a random number is generated from the selected distribution (currently this works only for Poisson) with the original value being a parameter value, which replaces the original value. */
	public final static int MODULATORY = 2;
	
	/** Default constructor. */
	public Randomizer() { }
	
	/** Randomizes images using a Gaussian random variable.
		
		@param image the input image to be randomized.
		
		@param mean the mean of the Gaussian distribution.
		
		@param stdev the standard deviation of the Gaussian distribution. Must be larger than or equal to {@code 0}.
		
		@param insertion determines how the random numbers generated from the distribution are inserted into the image. Must be one of {@link #ADDITIVE} or {@link #MULTIPLICATIVE}.
		
		@param newimage indicates whether the randomized image should be returned as a new image. If this parameter is {@code true}, the result is returned as a new {@code Image} object (of the same type as the input image) and the input image is left unaltered; if it is {@code false}, the input image itself is randomized and returned, thereby saving memory.
		
		@return a randomized version of the input image. Be aware of the value conversion and insertion rules for the different types of images: random numbers may have been clipped and rounded.
		
		@exception IllegalArgumentException if {@code stdev} is less than {@code 0}, or if {@code insertion} is invalid.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Image gaussian(
		final Image image,
		final double mean,
		final double stdev,
		final int insertion,
		final boolean newimage
	) {
		
		messenger.log(ImageScience.prelude()+"Randomizer");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		messenger.log("Checking arguments for Gaussian randomization");
		if (stdev < 0) throw new IllegalArgumentException("Standard deviation less than 0");
		messenger.log("Initializing Gaussian random number generator");
		messenger.log("Mean = " + mean);
		messenger.log("Standard deviation = " + stdev);
		messenger.log(">> Variance = " + stdev*stdev);
		final RandomGenerator rng = new GaussianGenerator(mean,stdev);
		final Image ran = newimage ? image.duplicate() : image;
		ran.name(image.name()+" with "+insertion(insertion)+" Gaussian noise");
		insert(ran,rng,insertion);
		
		timer.stop();
		
		return ran;
	}
	
	/** Randomizes images using a binomial random variable.
		
		@param image the input image to be randomized.
		
		@param trials the number of trials of the binomial distribution. Must be larger than or equal to {@code 0}.
		
		@param probability the probability for each trial of the binomial distribution. Must be in the range {@code [0,1]}.
		
		@param insertion determines how the random numbers generated from the distribution are inserted into the image. Must be one of {@link #ADDITIVE} or {@link #MULTIPLICATIVE}.
		
		@param newimage indicates whether the randomized image should be returned as a new image. If this parameter is {@code true}, the result is returned as a new {@code Image} object (of the same type as the input image) and the input image is left unaltered; if it is {@code false}, the input image itself is randomized and returned, thereby saving memory.
		
		@return a randomized version of the input image. Be aware of the value conversion and insertion rules for the different types of images: random numbers may have been clipped and rounded.
		
		@exception IllegalArgumentException if {@code trials} is less than {@code 0}, if {@code probability} is outside the range {@code [0,1]}, or if {@code insertion} is invalid.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Image binomial(
		final Image image,
		final int trials,
		final double probability,
		final int insertion,
		final boolean newimage
	) {
		
		messenger.log(ImageScience.prelude()+"Randomizer");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		messenger.log("Checking arguments for binomial randomization");
		if (trials < 0) throw new IllegalArgumentException("Number of trials less than 0");
		if (probability < 0 || probability > 1) throw new IllegalArgumentException("Probability outside range [0,1]");
		messenger.log("Initializing binomial random number generator");
		messenger.log("Trials = " + trials);
		messenger.log("Probability = " + probability);
		final double mean = trials*probability;
		final double variance = mean*(1 - probability);
		messenger.log(">> Mean = " + mean);
		messenger.log(">> Variance = " + variance);
		messenger.log(">> Standard deviation = " + Math.sqrt(variance));
		final RandomGenerator rng = new BinomialGenerator(trials,probability);
		final Image ran = newimage ? image.duplicate() : image;
		ran.name(image.name()+" with "+insertion(insertion)+" binomial noise");
		insert(ran,rng,insertion);
		
		timer.stop();
		
		return ran;
	}
	
	/** Randomizes images using a gamma random variable.
		
		@param image the input image to be randomized.
		
		@param order the integer order of the gamma distribution. Must be larger than {@code 0}.
		
		@param insertion determines how the random numbers generated from the distribution are inserted into the image. Must be one of {@link #ADDITIVE} or {@link #MULTIPLICATIVE}.
		
		@param newimage indicates whether the randomized image should be returned as a new image. If this parameter is {@code true}, the result is returned as a new {@code Image} object (of the same type as the input image) and the input image is left unaltered; if it is {@code false}, the input image itself is randomized and returned, thereby saving memory.
		
		@return a randomized version of the input image. Be aware of the value conversion and insertion rules for the different types of images: random numbers may have been clipped and rounded.
		
		@exception IllegalArgumentException if {@code order} is less than or equal to {@code 0}, or if {@code insertion} is invalid.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Image gamma(
		final Image image,
		final int order,
		final int insertion,
		final boolean newimage
	) {
		
		messenger.log(ImageScience.prelude()+"Randomizer");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		messenger.log("Checking arguments for gamma randomization");
		if (order <= 0) throw new IllegalArgumentException("Order less than or equal to 0");
		messenger.log("Initializing gamma random number generator");
		messenger.log("Order = " + order);
		messenger.log(">> Mean = " + order);
		messenger.log(">> Variance = " + order);
		messenger.log(">> Standard deviation = " + Math.sqrt(order));
		final RandomGenerator rng = new GammaGenerator(order);
		final Image ran = newimage ? image.duplicate() : image;
		ran.name(image.name()+" with "+insertion(insertion)+" gamma noise");
		insert(ran,rng,insertion);
		
		timer.stop();
		
		return ran;
	}
	
	/** Randomizes images using a uniform random variable.
		
		@param image the input image to be randomized.
		
		@param min {@code max} - the interval parameters of the uniform distribution. Random numbers are generated in the open interval ({@code min},{@code max}).
		
		@param insertion determines how the random numbers generated from the distribution are inserted into the image. Must be one of {@link #ADDITIVE} or {@link #MULTIPLICATIVE}.
		
		@param newimage indicates whether the randomized image should be returned as a new image. If this parameter is {@code true}, the result is returned as a new {@code Image} object (of the same type as the input image) and the input image is left unaltered; if it is {@code false}, the input image itself is randomized and returned, thereby saving memory.
		
		@return a randomized version of the input image. Be aware of the value conversion and insertion rules for the different types of images: random numbers may have been clipped and rounded.
		
		@exception IllegalArgumentException if {@code min} is larger than or equal to {@code max}, or if {@code insertion} is invalid.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Image uniform(
		final Image image,
		final double min,
		final double max,
		final int insertion,
		final boolean newimage
	) {
		
		messenger.log(ImageScience.prelude()+"Randomizer");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		messenger.log("Checking arguments for uniform randomization");
		if (min >= max) throw new IllegalArgumentException("Maximum must be larger than minimum");
		messenger.log("Initializing uniform random number generator");
		messenger.log("Minimum = " + min);
		messenger.log("Maximum = " + max);
		final double mean = 0.5*(min + max);
		final double stdev = (max - min)/Math.sqrt(12);
		messenger.log(">> Mean = " + mean);
		messenger.log(">> Variance = " + stdev*stdev);
		messenger.log(">> Standard deviation = " + stdev);
		final RandomGenerator rng = new UniformGenerator(min,max);
		final Image ran = newimage ? image.duplicate() : image;
		ran.name(image.name()+" with "+insertion(insertion)+" uniform noise");
		insert(ran,rng,insertion);
		
		timer.stop();
		
		return ran;
	}
	
	/** Randomizes images using an exponential random variable.
		
		@param image the input image to be randomized.
		
		@param lambda the lambda parameter of the exponential distribution. Must be larger than {@code 0}.
		
		@param insertion determines how the random numbers generated from the distribution are inserted into the image. Must be one of {@link #ADDITIVE} or {@link #MULTIPLICATIVE}.
		
		@param newimage indicates whether the randomized image should be returned as a new image. If this parameter is {@code true}, the result is returned as a new {@code Image} object (of the same type as the input image) and the input image is left unaltered; if it is {@code false}, the input image itself is randomized and returned, thereby saving memory.
		
		@return a randomized version of the input image. Be aware of the value conversion and insertion rules for the different types of images: random numbers may have been clipped and rounded.
		
		@exception IllegalArgumentException if {@code lambda} is less than or equal to {@code 0}, or if {@code insertion} is invalid.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Image exponential(
		final Image image,
		final double lambda,
		final int insertion,
		final boolean newimage
	) {
		
		messenger.log(ImageScience.prelude()+"Randomizer");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		messenger.log("Checking arguments for exponential randomization");
		if (lambda <= 0) throw new IllegalArgumentException("Lambda less than or equal to 0");
		messenger.log("Initializing exponential random number generator");
		messenger.log("Lambda = " + lambda);
		final double mean = 1/lambda;
		final double stdev = mean;
		messenger.log(">> Mean = " + mean);
		messenger.log(">> Variance = " + stdev*stdev);
		messenger.log(">> Standard deviation = " + stdev);
		final RandomGenerator rng = new ExponentialGenerator(lambda);
		final Image ran = newimage ? image.duplicate() : image;
		ran.name(image.name()+" with "+insertion(insertion)+" exponential noise");
		insert(ran,rng,insertion);
		
		timer.stop();
		
		return ran;
	}
	
	/** Randomizes images using a Poisson random variable.
		
		@param image the input image to be randomized.
		
		@param mean the mean of the Poisson distribution. Must be larger than or equal to {@code 0}.
		
		@param insertion determines how the random numbers generated from the distribution are inserted into the image. Must be one of {@link #ADDITIVE}, {@link #MULTIPLICATIVE}, or {@link #MODULATORY}.
		
		@param newimage indicates whether the randomized image should be returned as a new image. If this parameter is {@code true}, the result is returned as a new {@code Image} object (of the same type as the input image) and the input image is left unaltered; if it is {@code false}, the input image itself is randomized and returned, thereby saving memory.
		
		@return a randomized version of the input image. Be aware of the value conversion and insertion rules for the different types of images: random numbers may have been clipped and rounded.
		
		@exception IllegalArgumentException if {@code mean} is less than {@code 0}, or if {@code insertion} is invalid.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public Image poisson(
		final Image image,
		final double mean,
		final int insertion,
		final boolean newimage
	) {
		
		messenger.log(ImageScience.prelude()+"Randomizer");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		messenger.log("Checking arguments for Poisson randomization");
		if (mean < 0) throw new IllegalArgumentException("Mean less than 0");
		messenger.log("Initializing Poisson random number generator");
		if (insertion == ADDITIVE || insertion == MULTIPLICATIVE) {
			messenger.log("Mean = " + mean);
			messenger.log(">> Variance = " + mean);
			messenger.log(">> Standard deviation = " + Math.sqrt(mean));
		}
		final RandomGenerator rng = new PoissonGenerator(mean);
		final Image ran = newimage ? image.duplicate() : image;
		ran.name(image.name()+" with "+insertion(insertion)+" Poisson noise");
		insert(ran,rng,insertion);
		
		timer.stop();
		
		return ran;
	}
	
	private void insert(final Image img, final RandomGenerator rng, final int insertion) {
		
		messenger.log("Randomizing "+img.type());
		final Dimensions dims = img.dimensions();
		final Coordinates coords = new Coordinates();
		messenger.status("Randomizing...");
		progressor.steps(dims.c*dims.t*dims.z*dims.y);
		final double[] array = new double[dims.x];
		img.axes(Axes.X);
		
		progressor.start();
		switch (insertion) {
			case ADDITIVE: {
				messenger.log("Inserting random numbers by addition");
				for (coords.c=0; coords.c<dims.c; ++coords.c)
					for (coords.t=0; coords.t<dims.t; ++coords.t)
						for (coords.z=0; coords.z<dims.z; ++coords.z)
							for (coords.y=0; coords.y<dims.y; ++coords.y) {
								img.get(coords,array);
								for (int x=0; x<dims.x; ++x)
									array[x] += rng.next();
								img.set(coords,array);
								progressor.step();
							}
				break;
			}
			case MULTIPLICATIVE: {
				messenger.log("Inserting random numbers by multiplication");
				for (coords.c=0; coords.c<dims.c; ++coords.c)
					for (coords.t=0; coords.t<dims.t; ++coords.t)
						for (coords.z=0; coords.z<dims.z; ++coords.z)
							for (coords.y=0; coords.y<dims.y; ++coords.y) {
								img.get(coords,array);
								for (int x=0; x<dims.x; ++x)
									array[x] *= rng.next();
								img.set(coords,array);
								progressor.step();
							}
				break;
			}
			case MODULATORY: {
				if (!(rng instanceof PoissonGenerator))
					throw new IllegalArgumentException("Invalid type of insertion");
				final PoissonGenerator prng = (PoissonGenerator)rng;
				messenger.log("Inserting random numbers by modulation");
				for (coords.c=0; coords.c<dims.c; ++coords.c)
					for (coords.t=0; coords.t<dims.t; ++coords.t)
						for (coords.z=0; coords.z<dims.z; ++coords.z)
							for (coords.y=0; coords.y<dims.y; ++coords.y) {
								img.get(coords,array);
								for (int x=0; x<dims.x; ++x)
									array[x] = prng.next(array[x]);
								img.set(coords,array);
								progressor.step();
							}
				break;
			}
			default: throw new IllegalArgumentException("Invalid type of insertion");
		}
		progressor.stop();
		messenger.status("");
	}
	
	private String insertion(final int insertion) {
		
		switch (insertion) {
			case ADDITIVE: return "additive";
			case MULTIPLICATIVE: return "multiplicative";
			case MODULATORY: return "modulatory";
		}
		
		return null;
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
}
