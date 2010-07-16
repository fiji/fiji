package mpicbg.spim.fusion;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;

public class GaussContent extends IsolatedPixelWeightener<GaussContent> 
{
	final Image<FloatType> gaussContent;
	
	protected GaussContent( final ViewDataBeads view, final ContainerFactory entropyContainer ) 
	{
		super( view );
		
		final int numThreads = Math.max( 1, Runtime.getRuntime().availableProcessors() / view.getNumViews() );
		
		final SPIMConfiguration conf = view.getViewStructure().getSPIMConfiguration();
		
		// get the kernels
		
		final double[] k1 = new double[ view.getNumDimensions() ];
		final double[] k2 = new double[ view.getNumDimensions() ];
		
		for ( int d = 0; d < view.getNumDimensions() - 1; ++d )
		{
			k1[ d ] = conf.fusionSigma1;
			k2[ d ] = conf.fusionSigma2;
		}
		
		k1[ view.getNumDimensions() - 1 ] = conf.fusionSigma1 / view.getZStretching();
		k2[ view.getNumDimensions() - 1 ] = conf.fusionSigma2 / view.getZStretching();		
		
		final Image<FloatType> kernel1 = FourierConvolution.createGaussianKernel( new ArrayContainerFactory(), k1 );
		final Image<FloatType> kernel2 = FourierConvolution.createGaussianKernel( new ArrayContainerFactory(), k2 );

		// compute I*sigma1
		FourierConvolution<FloatType, FloatType> fftConv1 = new FourierConvolution<FloatType, FloatType>( view.getImage(), kernel1 );
		fftConv1.setNumThreads( numThreads );
		fftConv1.process();		
		final Image<FloatType> conv1 = fftConv1.getResult();
		
		fftConv1.close();
		fftConv1 = null;
				
		// compute ( I - I*sigma1 )^2
		final Cursor<FloatType> cursorImg = view.getImage().createCursor();
		final Cursor<FloatType> cursorConv = conv1.createCursor();
		
		while ( cursorImg.hasNext() )
		{
			cursorImg.fwd();
			cursorConv.fwd();
			
			final float diff = cursorImg.getType().get() - cursorConv.getType().get();
			
			cursorConv.getType().set( diff*diff );
		}

		// compute ( ( I - I*sigma1 )^2 ) * sigma2
		FourierConvolution<FloatType, FloatType> fftConv2 = new FourierConvolution<FloatType, FloatType>( conv1, kernel2 );
		fftConv2.setNumThreads( numThreads );
		fftConv2.process();	
		
		gaussContent = fftConv2.getResult();

		fftConv2.close();
		fftConv2 = null;
		
		// close the unnecessary image
		kernel1.close();
		kernel2.close();
		conv1.close();
		
		ViewDataBeads.normalizeImage( gaussContent );		
	}

	@Override
	public LocalizableByDimCursor<FloatType> getResultIterator()
	{
        // the iterator we need to get values from the weightening image
		return gaussContent.createLocalizableByDimCursor();
	}
	
	@Override
	public LocalizableByDimCursor<FloatType> getResultIterator( OutOfBoundsStrategyFactory<FloatType> factory )
	{
        // the iterator we need to get values from the weightening image
		return gaussContent.createLocalizableByDimCursor( factory );
	}
	
	@Override
	public void close()
	{
		gaussContent.close();
	}
}
