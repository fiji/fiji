package mpicbg.spim.fusion;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3f;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class MappingFusionParalellMaxWeight extends SPIMImageFusion
{
	final Image<FloatType> fusedImage;

	public MappingFusionParalellMaxWeight( final ViewStructure viewStructure, final ViewStructure referenceViewStructure,
								  final ArrayList<IsolatedPixelWeightenerFactory<?>> isolatedWeightenerFactories,
								  final ArrayList<CombinedPixelWeightenerFactory<?>> combinedWeightenerFactories )
	{
		super( viewStructure, referenceViewStructure, isolatedWeightenerFactories, combinedWeightenerFactories );


		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Reserving memory for fused image.");

		final ImageFactory<FloatType> fusedImageFactory = new ImageFactory<FloatType>( new FloatType(), conf.outputImageFactory );
		fusedImage = fusedImageFactory.createImage( new int[]{ imgW, imgH, imgD }, "Fused image");

		if (fusedImage == null)
		{
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("MappingFusionParalell.constructor: Cannot create output image: " + conf.outputImageFactory.getErrorMessage());

			return;
		}
	}

	@Override
	public void fuseSPIMImages( final int channelIndex )
	{
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("Loading source images (Channel " + channelIndex +  ").");

		//
		// update views so that only the current channel is being fused
		//
		final ArrayList<ViewDataBeads> views = new ArrayList<ViewDataBeads>();

		for ( final ViewDataBeads view : viewStructure.getViews() )
			if ( view.getChannelIndex() == channelIndex )
				views.add( view );

		final int numViews = views.size();

		// clear the previous output image
		if ( channelIndex > 0 )
			for ( final FloatType type : fusedImage )
				type.set( 0 );

		// load images
		for ( final ViewDataBeads view : views )
			view.getImage( false );

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN && isolatedWeightenerFactories.size() > 0 )
		{
			String methods = "(" + isolatedWeightenerFactories.get(0).getDescriptiveName();
			for ( int i = 1; i < isolatedWeightenerFactories.size(); ++i )
				methods += ", " + isolatedWeightenerFactories.get(i).getDescriptiveName();
			methods += ")";

			IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Init isolated weighteners for all views " + methods );
		}

		// init isolated pixel weighteners
		final AtomicInteger ai = new AtomicInteger(0);
        Thread[] threads = SimpleMultiThreading.newThreads(conf.numberOfThreads);
        final int numThreads = threads.length;

        // compute them all in paralell ( computation done while opening )
		IsolatedPixelWeightener<?>[][] isoWinit = new IsolatedPixelWeightener<?>[ isolatedWeightenerFactories.size() ][ numViews ];
		for (int j = 0; j < isoWinit.length; j++)
		{
			final int i = j;

			final IsolatedPixelWeightener<?>[][] isoW = isoWinit;

			for (int ithread = 0; ithread < threads.length; ++ithread)
	            threads[ithread] = new Thread(new Runnable()
	            {
	                @Override
					public void run()
	                {
	                	final int myNumber = ai.getAndIncrement();

						for (int view = 0; view < numViews; view++)
							if ( view % numThreads == myNumber)
							{
								IOFunctions.println( "Computing " + isolatedWeightenerFactories.get( i ).getDescriptiveName() + " for " + views.get( view ) );
								isoW[i][view] = isolatedWeightenerFactories.get(i).createInstance( views.get(view) );
							}
	                }
	            });

			SimpleMultiThreading.startAndJoin( threads );
		}

		// test if the isolated weighteners were successfull...
		try
		{
			boolean successful = true;
			for ( final IsolatedPixelWeightener[] iso : isoWinit )
				for ( final IsolatedPixelWeightener i : iso )
					if ( i == null )
						successful = false;

			if ( !successful )
			{
				IOFunctions.println( "Not enough memory for running the content-based fusion, running without it" );
				isoWinit = new IsolatedPixelWeightener[ 0 ][ 0 ];
			}
		}
		catch (final Exception e)
		{
			IOFunctions.println( "Not enough memory for running the content-based fusion, running without it" );
			isoWinit = new IsolatedPixelWeightener[ 0 ][ 0 ];
		}

		final IsolatedPixelWeightener<?>[][] isoW = isoWinit;

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Computing output image (Channel " + channelIndex +  ").");

		// cache the views, imageSizes and models that we use
		final boolean useView[] = new boolean[ numViews ];
		final AbstractAffineModel3D<?> models[] = new AbstractAffineModel3D[ numViews ];

		for ( int i = 0; i < numViews; ++i )
		{
			useView[ i ] = Math.max( views.get( i ).getViewErrorStatistics().getNumConnectedViews(), views.get( i ).getTile().getConnectedTiles().size() ) > 0 || views.get( i ).getViewStructure().getNumViews() == 1;

			// if a corresponding view that was used for registration is valid, this one is too
			if ( views.get( i ).getUseForRegistration() == false )
			{
				final int angle = views.get( i ).getAcqusitionAngle();
				final int timepoint = views.get( i ).getViewStructure().getTimePoint();

				for ( final ViewDataBeads view2 : viewStructure.getViews() )
					if ( view2.getAcqusitionAngle() == angle && timepoint == view2.getViewStructure().getTimePoint() && view2.getUseForRegistration() == true )
						useView[ i ] = true;
			}

			models[ i ] = (AbstractAffineModel3D<?>)views.get( i ).getTile().getModel();
		}

		// we want the image to be not normalized to [0...1]
		for (int view = 0; view < numViews ; view++)
			views.get( view ).unnormalizeImage();

		final int[][] imageSizes = new int[numViews][];
		for ( int i = 0; i < numViews; ++i )
			imageSizes[ i ] = views.get( i ).getImageSize();

		ai.set( 0 );
        threads = SimpleMultiThreading.newThreads( numThreads );

		for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                @Override
				public void run()
                {
                	try
                	{
                        final int myNumber = ai.getAndIncrement();

                        // temporary float array
		            	final float[] tmp = new float[ 3 ];

		        		// init combined pixel weighteners
		        		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN && combinedWeightenerFactories.size() > 0 )
		        		{
		        			String methods = "(" + combinedWeightenerFactories.get(0).getDescriptiveName();
		        			for ( int i = 1; i < combinedWeightenerFactories.size(); ++i )
		        				methods += ", " + combinedWeightenerFactories.get(i).getDescriptiveName();
		        			methods += ")";

		        			if ( myNumber == 0 )
		        				IOFunctions.println("Initialize combined weighteners for all views " + methods );
		        		}

		        		final CombinedPixelWeightener<?>[] combW = new CombinedPixelWeightener<?>[combinedWeightenerFactories.size()];
		        		for (int i = 0; i < combW.length; i++)
		        			combW[i] = combinedWeightenerFactories.get(i).createInstance( views );

						// get iterators for isolated weights
		        		final LocalizableByDimCursor<FloatType> isoIterators[][] = new LocalizableByDimCursor[ isoW.length ][ numViews ];
						for (int i = 0; i < isoW.length; i++)
							for (int view = 0; view < isoW[i].length; view++)
								isoIterators[i][view] = isoW[i][view].getResultIterator();

		    			final Point3f[] tmpCoordinates = new Point3f[ numViews ];
		    			final int[][] loc = new int[ numViews ][ 3 ];
		    			final float[][] locf = new float[ numViews ][ 3 ];
		    			final boolean[] use = new boolean[ numViews ];

		    			for ( int i = 0; i < numViews; ++i )
		    				tmpCoordinates[ i ] = new Point3f();

		    			final LocalizableCursor<FloatType> it = fusedImage.createLocalizableCursor();

		    			// create Interpolated Iterators for the input images (every thread need own ones!)
		    			final Interpolator<FloatType>[] imageInterpolators = new Interpolator[ numViews ];
		    			for (int view = 0; view < numViews ; view++)
		    				imageInterpolators[ view ] = views.get( view ).getImage( false ).createInterpolator( conf.interpolatorFactorOutput );

		    			// create Interpolators for weights
		    			final Interpolator<FloatType>[][] weightInterpolators = new Interpolator[ isoW.length ][ numViews ];
		    			for (int i = 0; i < isoW.length; i++)
		    				for (int view = 0; view < numViews ; view++)
		    					weightInterpolators[i][ view ] = isoW[i][view].getResultImage().createInterpolator( new LinearInterpolatorFactory<FloatType>( new OutOfBoundsStrategyValueFactory<FloatType>()) );

		    			while (it.hasNext())
		    			{
		    				it.fwd();

		        			if (it.getPosition(2) % numThreads == myNumber)
		        			{
		        				// get the coordinates if cropped
		        				final int x = it.getPosition(0) + cropOffsetX;
		        				final int y = it.getPosition(1) + cropOffsetY;
		        				final int z = it.getPosition(2) + cropOffsetZ;

								int num = 0;
								for (int i = 0; i < numViews; ++i)
								{
									if ( useView[ i ] )
									{
		    							tmpCoordinates[ i ].x = x * scale + min.x;
		    							tmpCoordinates[ i ].y = y * scale + min.y;
		    							tmpCoordinates[ i ].z = z * scale + min.z;

		    							mpicbg.spim.mpicbg.Java3d.applyInverseInPlace( models[i], tmpCoordinates[i], tmp );

		    							loc[i][0] = Util.round( tmpCoordinates[i].x );
		    							loc[i][1] = Util.round( tmpCoordinates[i].y );
		    							loc[i][2] = Util.round( tmpCoordinates[i].z );

		    							locf[i][0] = tmpCoordinates[i].x;
		    							locf[i][1] = tmpCoordinates[i].y;
		    							locf[i][2] = tmpCoordinates[i].z;

		    							// do we hit the source image?
										if ( loc[ i ][ 0 ] >= 0 && loc[ i ][ 1 ] >= 0 && loc[ i ][ 2 ] >= 0 &&
											 loc[ i ][ 0 ] < imageSizes[ i ][ 0 ] &&
											 loc[ i ][ 1 ] < imageSizes[ i ][ 1 ] &&
											 loc[ i ][ 2 ] < imageSizes[ i ][ 2 ] )
		    							{
		    								use[i] = true;
		    								++num;
		    							}
		    							else
		    							{
		    								use[i] = false;
		    							}
									}
								}

								if (num > 0)
								{
		    						// update combined weighteners
									if (combW.length > 0)
										for (final CombinedPixelWeightener<?> w : combW)
											w.updateWeights(locf, use);

									//float sumWeights = 0;
		    						//float value = 0;

    								int maxView = -1;
    								float maxWeight = -1;

		    						for (int view = 0; view < numViews; ++view)
		    							if (use[view])
		    							{
		    								float weight = 1;

		    								// multiplicate combined weights
		    								if (combW.length > 0)
		    									for (final CombinedPixelWeightener<?> w : combW)
		    										weight *= w.getWeight(view);

		    								tmp[ 0 ] = tmpCoordinates[view].x;
		    								tmp[ 1 ] = tmpCoordinates[view].y;
		    								tmp[ 2 ] = tmpCoordinates[view].z;

		    								for (int i = 0; i < isoW.length; i++)
		    								{
		    									weightInterpolators[i][view].setPosition( tmp );
		    									weight = weightInterpolators[i][view].getType().get();
		    								}

		    								if ( weight > maxWeight )
		    								{
		    									maxWeight = weight;
		    									maxView = view;
		    								}

		    								/*
		    								tmp[ 0 ] = tmpCoordinates[view].x;
		    								tmp[ 1 ] = tmpCoordinates[view].y;
		    								tmp[ 2 ] = tmpCoordinates[view].z;

		    								interpolators[view].moveTo( tmp );

		    								value += weight * interpolators[view].getType().get();
		    								sumWeights += weight;
		    								*/
		    							}

		    						if ( maxView != -1 )
		    						{
		    							/*
		    							// set label
		    							it.getType().set( views.get( maxView ).getAcqusitionAngle() + 10 );
		    							*/

		    							// set intensity of the maxview
		    							tmp[ 0 ] = tmpCoordinates[maxView].x;
	    								tmp[ 1 ] = tmpCoordinates[maxView].y;
	    								tmp[ 2 ] = tmpCoordinates[maxView].z;

	    								imageInterpolators[maxView].setPosition( tmp );
		    							it.getType().set( imageInterpolators[maxView].getType().get() );
		    						}

	    						}

	            			} // myThread loop
	        			} // iterator loop

	        			it.close();
	        			for (int i = 0; i < isoW.length; i++)
							for (int view = 0; view < numViews; view++)
								weightInterpolators[i][view].close();

						for (int view = 0; view < numViews; view++)
							imageInterpolators[view].close();

	        			// close combined pixel weighteners
	        			for (int i = 0; i < combW.length; i++)
	        				combW[i].close();

	        			// close isolated iterators
						for (int i = 0; i < isoW.length; i++)
							for (int view = 0; view < isoW[i].length; view++)
								isoIterators[i][view].close();

                	}
                	catch (final NoninvertibleModelException e)
                	{
                		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
                			IOFunctions.println( "MappingFusionParalell(): Model not invertible for " + viewStructure );
                	}
                }// Thread.run loop
            });

        SimpleMultiThreading.startAndJoin(threads);

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Closing all input images (Channel " + channelIndex +  ").");

		// unload images
		for ( final ViewDataBeads view : views )
			view.closeImage();

		// close weighteners
		// close isolated pixel weighteners
		try
		{
			for (int i = 0; i < isoW.length; i++)
				for (int view = 0; view < numViews; view++)
					isoW[i][view].close();
		}
		catch (final Exception e )
		{
			// this will fail if there was not enough memory...
		}

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Done computing output image (Channel " + channelIndex +  ").");
	}

	@Override
	public Image<FloatType> getFusedImage() { return fusedImage; }
}
