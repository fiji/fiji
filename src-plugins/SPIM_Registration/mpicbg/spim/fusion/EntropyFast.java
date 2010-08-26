package mpicbg.spim.fusion;

import java.util.Date;

import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.fusion.entropy.Entropy;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class EntropyFast extends IsolatedPixelWeightener<EntropyFast>
{
	final Image<FloatType> entropy;

	public EntropyFast( ViewDataBeads view, ContainerFactory entropyContainerFactory )
	{
		super( view );

		// compute window size in z direction
		final int windowSizeZ = Math.round( conf.windowSizeX/(float)view.getZStretching() );
		
		// precompute the entropy for the whole image
		if ( debugLevel <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Computing entropy for " + view + " using a "+conf.windowSizeX+"x"+conf.windowSizeY+"x"+windowSizeZ+" window");
		
		entropy = Entropy.computeEntropy( view.getImage(), entropyContainerFactory, conf.histogramBins, conf.windowSizeX, conf.windowSizeY, windowSizeZ );		
        
		// compute entropy = 100^entropy
		final Cursor<FloatType> i = entropy.createCursor();
		
		while ( i.hasNext() )
		{
			i.fwd();
			i.getType().set( (float)(Math.pow(100, i.getType().get())) );
		}
		
		i.close();
	}

	@Override
	public LocalizableByDimCursor<FloatType> getResultIterator()
	{
        // the iterator we need to get values from the weightening image
		return entropy.createLocalizableByDimCursor();
	}
	
	@Override
	public LocalizableByDimCursor<FloatType> getResultIterator( OutOfBoundsStrategyFactory<FloatType> factory )
	{
        // the iterator we need to get values from the weightening image
		return entropy.createLocalizableByDimCursor( factory );
	}
	
	@Override
	public void close()
	{
		entropy.close();
	}
}