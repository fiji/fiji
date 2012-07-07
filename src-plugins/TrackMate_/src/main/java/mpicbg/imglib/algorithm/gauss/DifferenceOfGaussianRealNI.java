package mpicbg.imglib.algorithm.gauss;

import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianReal;
import mpicbg.imglib.function.RealTypeConverter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

public class DifferenceOfGaussianRealNI<A extends RealType<A>, B extends RealType<B>> extends DifferenceOfGaussianReal<A, B> {

	/*
	 * FIELDS
	 */
	
	private float[] calibration;
	
	/*
	 * CONSTRUCTOR
	 */

	public DifferenceOfGaussianRealNI( 
			final Image<A> img, 
			final ImageFactory<B> factory,  
			final OutOfBoundsStrategyFactory<B> outOfBoundsFactory, 
			final double sigma1, 
			final double sigma2, 
			final double minPeakValue, 
			final double normalizationFactor,
			final float[] calibration) {
		super(img, factory, outOfBoundsFactory, sigma1, sigma2, minPeakValue, normalizationFactor);
		this.calibration = calibration;
	}
	
	/*
	 * METHODS
	 */
	
	/**
	 * This method returns the {@link OutputAlgorithm} that will compute the Gaussian Convolutions, more efficient versions can override this method
	 * 
	 * @param sigma - the sigma of the convolution, <b>in physical units</b>: for this concrete method, the {@link #calibration}
	 * field will be used to scale the convolver, possibly non-isotropically.  
	 * @param numThreads - the number of threads for this convolution
	 * @return
	 */
	protected OutputAlgorithm<B> getGaussianConvolution( final double sigma, final int numThreads )
	{
		final double[] sigmas = new double[calibration.length];
		for (int i = 0; i < sigmas.length; i++) 
			sigmas[i] = sigma * calibration[i];
		final GaussianConvolution2<A,B> gauss = new GaussianConvolution2<A,B>( image, factory, outOfBoundsFactory, new RealTypeConverter<A, B>(), sigmas );
		return gauss;
	}

}
