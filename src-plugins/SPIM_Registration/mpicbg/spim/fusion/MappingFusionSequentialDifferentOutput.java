package mpicbg.spim.fusion;

import java.util.ArrayList;
import java.util.Date;

import javax.vecmath.Point3f;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.AffineModel3D;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class MappingFusionSequentialDifferentOutput extends SPIMImageFusion
{	
	final Image<FloatType> fusedImages[];
	final int numViews;
	
	//int angleIndicies[] = new int[]{ 0, 6, 7 };
	int angleIndicies[] = null;
	
	public MappingFusionSequentialDifferentOutput( final ViewStructure viewStructure, final ViewStructure referenceViewStructure, 
			  									   final ArrayList<IsolatedPixelWeightenerFactory<?>> isolatedWeightenerFactories, 
			  									   final ArrayList<CombinedPixelWeightenerFactory<?>> combinedWeightenerFactories )
	{
		super( viewStructure, referenceViewStructure, isolatedWeightenerFactories, combinedWeightenerFactories );
		
		numViews = viewStructure.getNumViews();
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Reserving memory for fused images.");

		if ( angleIndicies == null )
		{
			angleIndicies = new int[ numViews ];
			
			for ( int view = 0; view < numViews; view++ )
				angleIndicies[ view ] = view;
		}
		
		fusedImages = new Image[ angleIndicies.length ];
		final ImageFactory<FloatType> fusedImageFactory = new ImageFactory<FloatType>( new FloatType(), conf.outputImageFactory );
		
		final long size = (4l * imgW * imgH * imgD)/(1000l*1000l);
		
		for (int i = 0; i < angleIndicies.length; i++)
		{
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Reserving " + size + " MiB for '" + views.get( angleIndicies[i] ).getName() + "'" );
			
			fusedImages[ i ] = fusedImageFactory.createImage( new int[]{ imgW, imgH, imgD }, "Fused image" );
			
			if ( fusedImages[i] == null && viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.printErr("MappingFusionSequentialDifferentOutput.constructor: Cannot create output image: " + conf.outputImageFactory.getErrorMessage());
		}		
	}
	
	@Override
	public void fuseSPIMImages()
	{
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Unloading source images.");

		// unload images 
		for ( ViewDataBeads view : views )
			view.closeImage();

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Computing output image.");

		// iterate over input images
		for (int viewIndex = 0; viewIndex < angleIndicies.length; viewIndex++)
		{
			final ViewDataBeads view = views.get( angleIndicies[ viewIndex ] );

			IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Computing individual registered image for '" + view.getName() + "'" );

			if ( view.getViewErrorStatistics().getNumConnectedViews() <= 0 && view.getViewStructure().getNumViews() > 1 )
			{
				if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
					IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Cannot use view '" + view.getName() + ", view is not connected to any other view.");
				
				continue;
			}
			
			// load the current image
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Loading view: " + view.getName());
			
			final Image<FloatType> img = view.getImage( conf.imageFactoryFusion );
			final Interpolator<FloatType> interpolator = img.createInterpolator( conf.interpolatorFactorOutput );
						
			final Point3f tmpCoordinates = new Point3f();
			
			final int[] imageSize = view.getImageSize();			
			final int w = imageSize[ 0 ];
			final int h = imageSize[ 1 ];
			final int d = imageSize[ 2 ];
			
			final AffineModel3D model = view.getTile().getModel();

			// temporary float array
        	final float[] tmp = new float[ 3 ];

        	
    		final CombinedPixelWeightener<?>[] combW = new CombinedPixelWeightener<?>[combinedWeightenerFactories.size()];
    		for (int i = 0; i < combW.length; i++)
    		{
    			System.out.println( "init " + combinedWeightenerFactories.get(i).getDescriptiveName() );
    			combW[i] = combinedWeightenerFactories.get(i).createInstance( viewStructure );
    		}

			final float[][] loc = new float[ numViews ][3];
			final boolean[] use = new boolean[ numViews ];
			
			for ( int v = 0; v < numViews; ++v )
			{
				use[ v ] = true;
				for ( int i = 0; i < 3; ++i )
					loc[ v ][ i ] = viewStructure.getViews().get( v ).getImageSize()[ i ] / 2;
			}
		
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting fusion for: " + view.getName());
			
			final LocalizableCursor<FloatType> iteratorFused = fusedImages[ viewIndex ].createLocalizableCursor();

			try
			{
				while (iteratorFused.hasNext())
				{
					iteratorFused.next();
	
					// get the coordinates if cropped
					final int x = iteratorFused.getPosition(0) + cropOffsetX;
					final int y = iteratorFused.getPosition(1) + cropOffsetY;
					final int z = iteratorFused.getPosition(2) + cropOffsetZ;
	
					tmpCoordinates.x = x * scale + min.x;
					tmpCoordinates.y = y * scale + min.y;
					tmpCoordinates.z = z * scale + min.z;
	
					mpicbg.spim.mpicbg.Java3d.applyInverseInPlace( model, tmpCoordinates, tmp );
						
					final int locX = MathLib.round( tmpCoordinates.x );
					final int locY = MathLib.round( tmpCoordinates.y );
					final int locZ = MathLib.round( tmpCoordinates.z );
					
					// do we hit the source image?
					if (locX >= 0 && locY >= 0 && locZ >= 0 && 
						locX < w  && locY < h  && locZ < d )
						{
							float weight = 1;
							
							// update combined weighteners
							if (combW.length > 0)
							{
								loc[ viewIndex ][ 0 ] = tmpCoordinates.x;
								loc[ viewIndex ][ 1 ] = tmpCoordinates.y;
								loc[ viewIndex ][ 2 ] = tmpCoordinates.z;
								
								for (final CombinedPixelWeightener<?> we : combW)
									we.updateWeights( loc, use );
								
								for (final CombinedPixelWeightener<?> we : combW)
									weight *= we.getWeight( viewIndex );
							}
						
							tmp[ 0 ] = tmpCoordinates.x;
							tmp[ 1 ] = tmpCoordinates.y;
							tmp[ 2 ] = tmpCoordinates.z;
							
							interpolator.moveTo( tmp );
						
							final float intensity = interpolator.getType().get();					
							iteratorFused.getType().set( intensity * weight );
						}				
				}
			}
        	catch (NoninvertibleModelException e)
        	{
        		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
        			IOFunctions.println( "MappingFusionSequentialDifferentOutput(): Model not invertible for " + viewStructure );
        	}

        	iteratorFused.close();
			interpolator.close();
			
			// unload input image
			view.closeImage();
						
		}// input images
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Done computing output image.");
	}

	@Override
	public Image<FloatType> getFusedImage() { return fusedImages[ 0 ]; }

	public Image<FloatType> getFusedImage( final int index ) { return fusedImages[ index ]; }

	@Override
	public boolean saveAsTiffs(String dir, String name) 
	{ 
		boolean success = true;
		
		for ( int i = 0; i < fusedImages.length; i++ )
		{
			final ViewDataBeads view = views.get( angleIndicies[ i ] );
			
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Saving '" + view.getName() + "_" + name + "'" );
			
			success &= ImageJFunctions.saveAsTiffs( fusedImages[ i ], dir, name + "_angle_" + view.getAcqusitionAngle(), ImageJFunctions.GRAY32 );
		}
		
		return success;
	}

	@Override
	public void closeImages() 
	{
		for (int i = 0; i < fusedImages.length; i++)
			fusedImages[i].close();
	}

}
