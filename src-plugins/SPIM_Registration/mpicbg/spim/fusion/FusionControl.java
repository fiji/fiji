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
			combinedWeightenerFactories.add( new BlendingSimpleFactory() );
		
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
				
		for ( int channelIndex = 0; channelIndex < viewStructure.getNumChannels(); ++channelIndex )
		{
			fusion.fuseSPIMImages( channelIndex );
		
			final int channelID = viewStructure.getChannelNum( channelIndex );
			
			if (conf.showOutputImage)
			{
				if ( !conf.multipleImageFusion )
				{
					if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
						IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Displaying image (Channel " + channelIndex +  ").");
					
					fusion.getFusedImage().getDisplay().setMinMax();
					
					String name = viewStructure.getSPIMConfiguration().inputFilePattern;			
					String replaceTP = SPIMConfiguration.getReplaceStringTimePoints( name );
					String replaceChannel = SPIMConfiguration.getReplaceStringChannels( name );
					
					if ( replaceTP != null )
						name = name.replace( replaceTP, "" + timePoint );

					if ( replaceChannel != null )
						name = name.replace( replaceChannel, "" + channelID );

					fusion.getFusedImage().setName( "Fused_" + name );
					
					ImageJFunctions.copyToImagePlus( fusion.getFusedImage() ).show();
					//fusion.getFusedImageVirtual().show();
				}
				else
				{	
					if ( channelIndex == 0 )
					{
						MappingFusionSequentialDifferentOutput multipleFusion = (MappingFusionSequentialDifferentOutput)fusion;
						
						int i = 0;
						
						for ( final ViewDataBeads view : viewStructure.getViews() )
						{
							final Image<FloatType> fused = multipleFusion.getFusedImage( i++ );
		
							String name = viewStructure.getSPIMConfiguration().inputFilePattern;			
							String replaceTP = SPIMConfiguration.getReplaceStringTimePoints( name );
							String replaceAngle = SPIMConfiguration.getReplaceStringAngle( name );
							String replaceChannel = SPIMConfiguration.getReplaceStringChannels( name );
							
							try
							{
								name = name.replace( replaceAngle, "" + view.getAcqusitionAngle() );
							}
							catch (Exception e ){};
		
							try
							{
								name = name.replace( replaceTP, "" + timePoint );
								name = name.replace( replaceChannel, "" + channelID );
							}
							catch (Exception e ){};
							
							fused.setName( name );
							fused.getDisplay().setMinMax();
							
							ImageJFunctions.copyToImagePlus( fused ).show();
						}
					}
				}
			}
			
			if ( conf.writeOutputImage )
			{			
				if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
					IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Writing output file (Channel " + channelIndex +  ").");
				
				fusion.saveAsTiffs( conf.outputdirectory, "img_tl" + timePoint, channelIndex );
			}					
		}
		
		fusion.closeImages();
	}
	
}
