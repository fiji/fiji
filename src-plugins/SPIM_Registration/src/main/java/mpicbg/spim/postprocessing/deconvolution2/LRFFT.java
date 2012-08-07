package mpicbg.spim.postprocessing.deconvolution2;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.algorithm.mirror.MirrorImage;
import mpicbg.imglib.container.constant.ConstantContainer;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;

public class LRFFT 
{
	private Image<FloatType> image, weight, kernel1, kernel2;
	Image<FloatType> viewContribution = null;
	FourierConvolution<FloatType, FloatType> fftConvolution1, fftConvolution2;
	protected int numViews = 0;
	boolean useExponentialKernel = false;
	
	/**
	 * Used to determine if the Convolutions already have been computed for the current iteration
	 */
	int i = -1;
	
	public LRFFT( final Image<FloatType> image, final Image<FloatType> weight, final Image<FloatType> kernel )
	{
		this.image = image;
		this.kernel1 = kernel;
		this.weight = weight;
	}

	public LRFFT( final Image<FloatType> image, final Image<FloatType> kernel )
	{
		this( image, new Image< FloatType > ( new ConstantContainer< FloatType >( image.getDimensions(), new FloatType( 1 ) ), new FloatType() ), kernel );
	}

	/**
	 * @param numViews - the number of views in the acquisition, determines the exponential of the kernel
	 */
	protected void setNumViews( final int numViews ) { this.numViews = numViews; }
	
	/**
	 * This method is called once all views are added to the {@link LRInput}
	 */
	protected void init( final boolean useExponentialKernel )
	{
		this.useExponentialKernel = useExponentialKernel;
		
		// normalize kernel so that sum of all pixels == 1
		AdjustInput.normImage( kernel1 );

		if ( useExponentialKernel )
		{
			if ( numViews == 0 )
			{
				System.out.println( "Warning, numViews was not set." );
				numViews = 1;
			}
			
			// compute the squared kernel and its inverse
			final Image< FloatType > exponentialKernel = computeExponentialKernel( this.kernel1, numViews );
			
			// norm the squared kernel
			AdjustInput.normImage( exponentialKernel );
			
			// compute the inverted squared kernel
			this.kernel2 = computeInvertedKernel( exponentialKernel );	
		}
		else
		{
			// compute the inverted kernel (switch dimensions)
			this.kernel2 = computeInvertedKernel( this.kernel1 );
		}
		
		// the first kernel is to compute the ratio between original image and current guess,
		// so we need no exponent here
		this.fftConvolution1 = new FourierConvolution<FloatType, FloatType>( this.image, this.kernel1 );	
		this.fftConvolution1.setNumThreads();
		this.fftConvolution1.setKeepImgFFT( false );
		
		this.fftConvolution2 = new FourierConvolution<FloatType, FloatType>( this.image, this.kernel2 );	
		this.fftConvolution2.setNumThreads();
		this.fftConvolution2.setKeepImgFFT( false );
	}
	
	public static Image<FloatType> computeExponentialKernel( final Image<FloatType> kernel, final int numViews )
	{
		final Image<FloatType> exponentialKernel = kernel.clone();
		
		for ( final FloatType f : exponentialKernel )
			f.set( pow( f.get(), numViews ) );
		
		return exponentialKernel;
	}

	public static Image< FloatType > computeInvertedKernel( final Image< FloatType > kernel )
	{
		final Image< FloatType > invKernel = kernel.clone();
		
		for ( int d = 0; d < invKernel.getNumDimensions(); ++d )
			new MirrorImage< FloatType >( invKernel, d ).process();
		
		return invKernel;
	}

	final private static float pow( final float value, final int power )
	{
		float result = value;
		
		for ( int i = 1; i < power; ++i )
			result *= value;
		
		return result;
	}


	public void setImage( final Image<FloatType> image ) 
	{
		this.image = image;
		setCurrentIteration( -1 );
	}
	public void setWeight( final Image<FloatType> weight ) { this.weight = weight; }
	public void setKernel( final Image<FloatType> kernel ) 
	{
		this.kernel1 = kernel;
		
		init( useExponentialKernel );

		setCurrentIteration( -1 );
	}

	public Image<FloatType> getImage() { return image; }
	public Image<FloatType> getWeight() { return weight; }
	public Image<FloatType> getKernel1() { return kernel1; }
	public Image<FloatType> getKernel2() { return kernel2; }
	public Image<FloatType> getViewContribution() { return viewContribution; }
	
	public void setCurrentIteration( final int i ) { this.i = i; }
	public int getCurrentIteration() { return i; }
	
	public FourierConvolution<FloatType, FloatType> getFFTConvolution1() { return fftConvolution1; }
	public FourierConvolution<FloatType, FloatType> getFFTConvolution2() { return fftConvolution2; }
	
	public void setViewContribution( final Image<FloatType> viewContribution )
	{
		if ( this.viewContribution != null )
			this.viewContribution.close();
		
		this.viewContribution = viewContribution;
	}
	
	@Override
	public LRFFT clone()
	{
		final LRFFT viewClone = new LRFFT( this.image.clone(), this.weight.clone(), this.kernel1.clone() );
	
		viewClone.numViews = numViews;
		viewClone.useExponentialKernel = useExponentialKernel;
		viewClone.i = i;
		
		if ( this.kernel2 != null )
			viewClone.kernel2 = kernel2.clone();
		
		if ( this.viewContribution != null )
			viewClone.viewContribution = this.viewContribution.clone();
		
		if ( this.fftConvolution1 != null )
		{
			viewClone.fftConvolution1 = new FourierConvolution<FloatType, FloatType>( fftConvolution1.getImage(), fftConvolution1.getKernel() );
			viewClone.fftConvolution1.process();
		}

		if ( this.fftConvolution2 != null )
		{
			viewClone.fftConvolution2 = new FourierConvolution<FloatType, FloatType>( fftConvolution2.getImage(), fftConvolution2.getKernel() );
			viewClone.fftConvolution2.process();
		}

		return viewClone;
	}	
}
