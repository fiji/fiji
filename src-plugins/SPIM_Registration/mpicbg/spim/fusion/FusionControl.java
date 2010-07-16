package mpicbg.spim.fusion;

import java.util.ArrayList;
import java.util.Date;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class FusionControl
{	
	public void fuse( final ViewStructure viewStructure, final int timePoint )
	{
		fuse( viewStructure, viewStructure, timePoint );
	}
	
	public void fuse( final ViewStructure viewStructure, final ViewStructure referenceViewStructure, final int timePoint)
	{
		final SPIMConfiguration conf = viewStructure.getSPIMConfiguration();
		
		final ArrayList<IsolatedPixelWeightenerFactory<?>> isolatedWeightenerFactories = new ArrayList<IsolatedPixelWeightenerFactory<?>>();
		final ArrayList<CombinedPixelWeightenerFactory<?>> combinedWeightenerFactories = new ArrayList<CombinedPixelWeightenerFactory<?>>();
		
		if (conf.useEntropy)
			isolatedWeightenerFactories.add( new EntropyFastFactory( conf.entropyFactory ) );

		if (conf.useGauss)
			isolatedWeightenerFactories.add( new GaussContentFactory( conf.entropyFactory ) );

		if (conf.useLinearBlening)
			combinedWeightenerFactories.add( new BlendingFactory() );
		
		final SPIMImageFusion fusion;
		
		if (conf.multipleImageFusion)
			fusion = new MappingFusionSequentialDifferentOutput( viewStructure, referenceViewStructure, isolatedWeightenerFactories, combinedWeightenerFactories);
		else if (conf.paralellFusion)
			fusion = new MappingFusionParalell( viewStructure, referenceViewStructure, isolatedWeightenerFactories, combinedWeightenerFactories );
		else
		{
			if ( conf.numParalellViews >= viewStructure.getNumViews() )
				fusion = new MappingFusionParalell( viewStructure, referenceViewStructure, isolatedWeightenerFactories, combinedWeightenerFactories );
			else
				fusion = new MappingFusionSequential( viewStructure, referenceViewStructure, isolatedWeightenerFactories, combinedWeightenerFactories, conf.numParalellViews );
		}
		
		fusion.fuseSPIMImages();
		
		if (conf.showOutputImage)
		{
			if ( !conf.multipleImageFusion )
			{
				if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
					IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Displaying image.");
				
				fusion.getFusedImage().getDisplay().setMinMax();
				
				String name = viewStructure.getSPIMConfiguration().inputFilePattern;			
				String replaceTP = SPIMConfiguration.getReplaceStringTimePoints( name );
				
				if ( replaceTP != null )
					name = name.replace( replaceTP, "" + timePoint );
				
				fusion.getFusedImage().setName( "Fused_" + name );
				
				ImageJFunctions.copyToImagePlus( fusion.getFusedImage() ).show();
				//fusion.getFusedImageVirtual().show();
			}
			else
			{
				MappingFusionSequentialDifferentOutput multipleFusion = (MappingFusionSequentialDifferentOutput)fusion;
				
				int i = 0;
				
				for ( final ViewDataBeads view : viewStructure.getViews() )
				{
					final Image<FloatType> fused = multipleFusion.getFusedImage( i++ );

					String name = viewStructure.getSPIMConfiguration().inputFilePattern;			
					String replaceTP = SPIMConfiguration.getReplaceStringTimePoints( name );
					String replaceAngle = SPIMConfiguration.getReplaceStringAngle( name );
					name = name.replace( replaceTP, "" + timePoint );
					name = name.replace( replaceAngle, "" + view.getAcqusitionAngle() );

					fused.setName( name );
					fused.getDisplay().setMinMax();
					
					ImageJFunctions.copyToImagePlus( fused ).show();
					
					fused.close();
				}
			}
		}
		
		if (conf.writeOutputImage)
		{			
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Writing output file.");
			
			fusion.saveAsTiffs( conf.outputdirectory, "img_tl" + timePoint );
		}
				
		//if ( !conf.showOutputImage )
		fusion.closeImages();
	}
	
}
