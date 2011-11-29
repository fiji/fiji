package mpicbg.spim.postprocessing.deconvolution;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;

public class LucyRichardsonFFT 
{
	final Image<FloatType> image, kernel, weight;
	final FourierConvolution<FloatType, FloatType> fftConvolution;
	
	Image<FloatType> viewContribution = null;
	
	public LucyRichardsonFFT( final Image<FloatType> image, final Image<FloatType> weight, final Image<FloatType> kernel, final int cpusPerView )
	{
		this.image = image;
		this.kernel = kernel;
		this.weight = weight;
		
		fftConvolution = new FourierConvolution<FloatType, FloatType>( image, kernel );	
		fftConvolution.setNumThreads( Math.max( 1, cpusPerView ) );
	}

	public Image<FloatType> getImage() { return image; }
	public Image<FloatType> getWeight() { return weight; }
	public Image<FloatType> getKernel() { return kernel; }
	public Image<FloatType> getViewContribution() { return viewContribution; }
	
	public FourierConvolution<FloatType, FloatType> getFFTConvolution() { return fftConvolution; }
	
	public void setViewContribution( final Image<FloatType> viewContribution )
	{
		if ( this.viewContribution != null )
			this.viewContribution.close();
		
		this.viewContribution = viewContribution;
	}
}
