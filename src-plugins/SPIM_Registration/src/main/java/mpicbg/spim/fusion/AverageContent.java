package mpicbg.spim.fusion;

import ij.IJ;

import java.util.Date;

import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.integer.LongType;
import mpicbg.imglib.type.numeric.real.FloatType;
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
						
			// compute I*sigma1, store in imgConv
			final Image< LongType > integralImg = IntegralImage3d.computeArray( img );
			final Image< FloatType > imgConv = img.createNewImage();			
			DOM.meanMirror( integralImg, imgConv, rxy1*2 + 1, rxy1*2 + 1, rz1*2 + 1 );
			
			// compute ( I - I*sigma1 )^2, store in imgConv
			final Cursor<FloatType> cursorImg = view.getImage().createCursor();
			final Cursor<FloatType> cursorConv = imgConv.createCursor();
			
			while ( cursorImg.hasNext() )
			{
				cursorImg.fwd();
				cursorConv.fwd();
				
				final float diff = cursorImg.getType().get() - cursorConv.getType().get();
				
				cursorConv.getType().set( diff*diff );
			}
			
			// compute ( ( I - I*sigma1 )^2 ) * sigma2, store in imgConv
			IntegralImage3d.computeArray( integralImg, imgConv );
			
			DOM.meanMirror( integralImg, imgConv, rxy2*2 + 1, rxy2*2 + 1, rz2*2 + 1 );
			
			integralImg.close();
			
			gaussContent = imgConv;
			
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
