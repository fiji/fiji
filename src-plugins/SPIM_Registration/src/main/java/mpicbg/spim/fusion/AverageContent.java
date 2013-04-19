package mpicbg.spim.fusion;

import java.util.Date;

import ij.IJ;
import ij.ImagePlus;
import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.algorithm.integral.IntegralImageLong;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.integer.LongType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.segmentation.DOM;
import mpicbg.spim.segmentation.IntegralImage3d;

public class AverageContent extends IsolatedPixelWeightener<AverageContent> 
{
	Image<FloatType> gaussContent;
	
	protected AverageContent( final ViewDataBeads view, final ContainerFactory entropyContainer ) 
	{
		super( view );
		
		try
		{			
			final int numThreads = Math.max( 1, Runtime.getRuntime().availableProcessors() / view.getNumViews() );
			
			final SPIMConfiguration conf = view.getViewStructure().getSPIMConfiguration();
			
			// compute the radii
			final int rxy1 = Math.round( conf.fusionSigma1 );
			final int rxy2 = Math.round( conf.fusionSigma2 );

			final int rz1 = (int)Math.round( conf.fusionSigma1 / view.getZStretching() );
			final int rz2 = (int)Math.round( conf.fusionSigma2 / view.getZStretching() );

			// compute the integral image
			final Image< FloatType > img = view.getImage( false ); 
			
			if ( view.getViewStructure().getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Computing Integral Image");					
			/*
			final IntegralImageLong< FloatType > intImg = new IntegralImageLong<FloatType>( img, new Converter< FloatType, LongType >()
			{
				@Override
				public void convert( final FloatType input, final LongType output ) { output.set( Util.round( input.get() ) ); } 
			} 
			);
			
			intImg.process();
			
			final Image< LongType > integralImg = intImg.getResult();
			*/
			
			final Image< LongType > integralImg = IntegralImage3d.computeArray( img );
			
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
		catch ( OutOfMemoryError e )
		{
			IJ.log( "OutOfMemory: Cannot compute Gauss approximated Entropy for " + view.getName() + ": " + e );
			e.printStackTrace();
			gaussContent = null;
		}
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

	@Override
	public Image<FloatType> getResultImage() {
		return gaussContent;
	}
}
