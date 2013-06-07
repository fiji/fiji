package mpicbg.spim.fusion;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3f;

import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class MappingFusionSequentialDifferentOutput extends SPIMImageFusion
{
	final Image<FloatType> fusedImages[];
	final int numViews;
	final int numParalellStacks;
	
	//int angleIndicies[] = new int[]{ 0, 6, 7 };
	public static int angleIndicies[] = null;

	public MappingFusionSequentialDifferentOutput( final ViewStructure viewStructure, final ViewStructure referenceViewStructure,
			  									   final ArrayList<IsolatedPixelWeightenerFactory<?>> isolatedWeightenerFactories,
			  									   final ArrayList<CombinedPixelWeightenerFactory<?>> combinedWeightenerFactories,
			  									   final int numParalellStacks )
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

		this.numParalellStacks = numParalellStacks;
		
		fusedImages = new Image[ angleIndicies.length ];
		final ImageFactory<FloatType> fusedImageFactory = new ImageFactory<FloatType>( new FloatType(), conf.outputImageFactory );

		final long size = (4l * imgW * imgH * imgD)/(1000l*1000l);

		for (int i = 0; i < angleIndicies.length; i++)
		{
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Reserving " + size + " MiB for '" + viewStructure.getViews().get( angleIndicies[i] ).getName() + "'" );

			fusedImages[ i ] = fusedImageFactory.createImage( new int[]{ imgW, imgH, imgD }, "Fused image" );

			if ( fusedImages[i] == null && viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.printErr("MappingFusionSequentialDifferentOutput.constructor: Cannot create output image: " + conf.outputImageFactory.getErrorMessage());
		}
	}

	@Override
	public void fuseSPIMImages( final int channelIndex )
	{
		// here we do all at once
		if ( channelIndex > 0 )
			return;

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Unloading source images.");

		//
		// get all views of all channels
		//
		final ArrayList<ViewDataBeads> views = viewStructure.getViews();
		final int numViews = views.size();

		// unload images
		for ( final ViewDataBeads view : views )
			view.closeImage();

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Computing output image.");

		// iterate over input images in steps of numParalellStacks
		for ( int v = 0; v < angleIndicies.length; v += numParalellStacks )
		{
			final int viewIndexStart = v;
			final int viewIndexEnd = Math.min( v + numParalellStacks, numViews );
		
			// open input images
			//for ( int viewIndex = 0; viewIndex < angleIndicies.length; viewIndex++ )
			for ( int viewIndex = viewIndexStart; viewIndex < viewIndexEnd; viewIndex++ )
				views.get( angleIndicies[ viewIndex ] ).getImage( false );
	
			// compute output images in paralell
			final AtomicInteger ai = new AtomicInteger(0);
	        final Thread[] threads = SimpleMultiThreading.newThreads(conf.numberOfThreads);
	        final int numThreads = threads.length;
	
			for (int ithread = 0; ithread < threads.length; ++ithread)
	            threads[ithread] = new Thread(new Runnable()
	            {
	                @Override
					public void run()
	                {
	                	final int myNumber = ai.getAndIncrement();
	
						//for ( int viewIndex = 0; viewIndex < angleIndicies.length; viewIndex++ )
						for ( int viewIndex = viewIndexStart; viewIndex < viewIndexEnd; viewIndex++ )
							if ( viewIndex % numThreads == myNumber)
							{
								final ViewDataBeads view = views.get( angleIndicies[ viewIndex ] );
	
								IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Computing individual registered image for '" + view.getName() + "'" );
	
								if ( Math.max( view.getTile().getConnectedTiles().size(), view.getViewErrorStatistics().getNumConnectedViews() ) <= 0 && view.getViewStructure().getNumViews() > 1 )
								{
									if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
										IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Cannot use view '" + view.getName() + ", view is not connected to any other view.");
	
									continue;
								}
	
								// load the current image
								if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
									IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Loading view: " + view.getName());
	
								final Image<FloatType> img = view.getImage( conf.imageFactoryFusion, false );
								final Interpolator<FloatType> interpolator = img.createInterpolator( conf.interpolatorFactorOutput );
	
								final Point3f tmpCoordinates = new Point3f();
	
								final int[] imageSize = view.getImageSize();
								final int w = imageSize[ 0 ];
								final int h = imageSize[ 1 ];
								final int d = imageSize[ 2 ];
	
								final AbstractAffineModel3D<?> model = (AbstractAffineModel3D<?>)view.getTile().getModel();
	
								// temporary float array
					        	final float[] tmp = new float[ 3 ];
	
	
					    		final CombinedPixelWeightener<?>[] combW = new CombinedPixelWeightener<?>[combinedWeightenerFactories.size()];
					    		for (int i = 0; i < combW.length; i++)
					    			combW[i] = combinedWeightenerFactories.get(i).createInstance( views );
	
								final float[][] loc = new float[ numViews ][ 3 ];
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
	
										final int locX = Util.round( tmpCoordinates.x );
										final int locY = Util.round( tmpCoordinates.y );
										final int locZ = Util.round( tmpCoordinates.z );
	
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
	
												interpolator.setPosition( tmp );
	
												final float intensity = interpolator.getType().get();
												iteratorFused.getType().set( intensity * weight );
											}
									}
								}
					        	catch (final NoninvertibleModelException e)
					        	{
					        		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
					        			IOFunctions.println( "MappingFusionSequentialDifferentOutput(): Model not invertible for " + viewStructure );
					        	}
	
					        	iteratorFused.close();
								interpolator.close();
	
								// unload input image
								view.closeImage();
							}
	                }
	            });
	
			SimpleMultiThreading.startAndJoin( threads );
		}

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Done computing output image.");
	}

	@Override
	public Image<FloatType> getFusedImage() { return fusedImages[ 0 ]; }

	public Image<FloatType> getFusedImage( final int index ) { return fusedImages[ index ]; }

	@Override
	public boolean saveAsTiffs( final String dir, final String name, final int channelIndex )
	{
		if ( channelIndex > 0 )
			return true;

		boolean success = true;

		for ( int i = 0; i < fusedImages.length; i++ )
		{
			final ViewDataBeads view = viewStructure.getViews().get( angleIndicies[ i ] );

			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Saving '" + name + "_ch" + view.getChannel() + "_angle" + view.getAcqusitionAngle() + "'" );

			success &= ImageJFunctions.saveAsTiffs( fusedImages[ i ], dir, name + "_ch" + view.getChannel() + "_angle" + view.getAcqusitionAngle(), ImageJFunctions.GRAY32 );
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
