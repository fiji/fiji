package mpicbg.spim.fusion;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3f;

import fiji.plugin.Multi_View_Deconvolution;

import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.postprocessing.deconvolution.ExtractPSF;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class PreDeconvolutionFusionSequential extends SPIMImageFusion implements PreDeconvolutionFusionInterface
{
	final Image<FloatType> images[], weights[];
	final int numViews;
	final boolean normalize;
	final ExtractPSF extractPSF;
	
	public PreDeconvolutionFusionSequential( final ViewStructure viewStructure, final ViewStructure referenceViewStructure, 
								  final ArrayList<IsolatedPixelWeightenerFactory<?>> isolatedWeightenerFactories, 
								  final ArrayList<CombinedPixelWeightenerFactory<?>> combinedWeightenerFactories )
	{
		super( viewStructure, referenceViewStructure, isolatedWeightenerFactories, combinedWeightenerFactories );				
		
		// normalize the weights so the the sum for each pixel over all views is 1?
		this.normalize = true;
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Reserving memory for fused image.");
		
		final ImageFactory<FloatType> imageFactory = new ImageFactory<FloatType>( new FloatType(), conf.outputImageFactory );
		numViews = viewStructure.getNumViews();
		
		if ( conf.extractPSF )
			extractPSF = new ExtractPSF( viewStructure );
		else
			extractPSF = ExtractPSF.loadAndTransformPSF( conf.psfFiles, conf.transformPSFs, viewStructure );
		

		images = new Image[ numViews ];
		weights = new Image[ numViews ];

		if ( extractPSF == null )
			return;
		
		for ( int view = 0; view < numViews; view++ )
		{
			weights[ view ] = imageFactory.createImage( new int[]{ imgW, imgH, imgD }, "weights_" + view );
			images[ view ] = imageFactory.createImage( new int[]{ imgW, imgH, imgD }, "view_" + view ); 
			
			if ( images[ view ] == null || weights[ view ] == null )
			{
				if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
					IOFunctions.println("PreDeconvolutionFusion.constructor: Cannot create output image: " + conf.outputImageFactory.getErrorMessage() );

				return;
			}
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
		
		// this is only single channel for noew
		if ( channelIndex > 0 )
			return;
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN && isolatedWeightenerFactories.size() > 0 )
		{
			String methods = "(" + isolatedWeightenerFactories.get(0).getDescriptiveName();			
			for ( int i = 1; i < isolatedWeightenerFactories.size(); ++i )
				methods += ", " + isolatedWeightenerFactories.get(i).getDescriptiveName();			
			methods += ")";
			
			IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Init isolated weighteners for all views " + methods );
		}
			
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
		
		final int[][] imageSizes = new int[numViews][];		
		for ( int i = 0; i < numViews; ++i )
			imageSizes[ i ] = views.get( i ).getImageSize();
		
		//
		// compute only the blending
		//
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println( "Computing blending weights for all input views ... " );
		
		final AtomicInteger ai = new AtomicInteger(0);					
        Thread[] threads = SimpleMultiThreading.newThreads(conf.numberOfThreads);
        final int numThreads = threads.length;

		for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
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
		            			        		
		    			final Point3f[] tmpCoordinates = new Point3f[ numViews ];
		    			final int[][] loc = new int[ numViews ][ 3 ];
		    			final float[][] locf = new float[ numViews ][ 3 ];
		    			final boolean[] use = new boolean[ numViews ];
		    			
		    			for ( int i = 0; i < numViews; ++i )
		    				tmpCoordinates[ i ] = new Point3f();
		    			
		    			final LocalizableCursor<FloatType> outWeights[] = new LocalizableCursor[ numViews ];
		    			
		    			final float[] tmpWeights = new float[ numViews ];
		    			
		    			for ( int i = 0; i < numViews; ++i )
		    				outWeights[ i ] = weights[ i ].createLocalizableCursor();
		    			
		    			final LocalizableCursor<FloatType> firstCursor = outWeights[ 0 ];
		    			
		    			while ( firstCursor.hasNext() )
		    			{
			    			for ( int i = 0; i < numViews; ++i )
			    				outWeights[ i ].fwd();
		    				
		        			if ( firstCursor.getPosition(2) % numThreads == myNumber )
		        			{
		        				// get the coordinates if cropped (all coordinates are the same, so we only use the first cursor)
		        				final int x = firstCursor.getPosition( 0 ) + cropOffsetX;
		        				final int y = firstCursor.getPosition( 1 ) + cropOffsetY;
		        				final int z = firstCursor.getPosition( 2 ) + cropOffsetZ;

		        				// how many view contribute at this position
								int num = 0;
								for ( int i = 0; i < numViews; ++i )
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
		    								use[ i ] = true;
		    								++num;
		    							}	
		    							else
		    							{
		    								use[ i ] = false;
		    							}
									}
								}
		    				
								if ( num > 0 )
								{
		    						// update combined weighteners
									if ( combW.length > 0 )
										for ( final CombinedPixelWeightener<?> w : combW )
											w.updateWeights(locf, use);
		
									float sumWeights = 0;
		
		    						for ( int view = 0; view < numViews; ++view )
		    						{
		    							if ( use[view] )
		    							{
		    								float weight = 1;
		    								
		    								// multiplicate combined weights
		    								if (combW.length > 0)
		    									for (final CombinedPixelWeightener<?> w : combW)
		    										weight *= w.getWeight(view);
		    								
		    								tmp[ 0 ] = tmpCoordinates[view].x;
		    								tmp[ 1 ] = tmpCoordinates[view].y;
		    								tmp[ 2 ] = tmpCoordinates[view].z;
		    								
		    								if ( normalize )
		    								{
			    								sumWeights += weight;
			    								
			    								// set the intensity, remember the weight
			    								tmpWeights[ view ] = weight;
		    								}
		    								else
		    								{
		    									outWeights[ view ].getType().set( weight );
		    								}
		    							}
		    						}
		    						
		    						// set the normalized weights
		    						if ( normalize && sumWeights > 0 )
		    						{
			    						for ( int view = 0; view < numViews; ++view )
			    						{
			    							if ( use[view] )
			    							{
											if ( sumWeights > 1 )
				    								outWeights[ view ].getType().set( tmpWeights[ view ]/sumWeights );
											else
												outWeights[ view ].getType().set( tmpWeights[ view ] );
			    							}
			    						}
		    						}
		    									
	    						}
	    						
	            			} // myThread loop       				
	        			} // iterator loop
	        			
		    			for ( int i = 0; i < numViews; ++i )
		    				outWeights[ i ].close();
		    			
	        			// close combined pixel weighteners
	        			for (int i = 0; i < combW.length; i++)
	        				combW[i].close();     
                	}
                	catch (NoninvertibleModelException e)
                	{
                		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
                			IOFunctions.println( "PreDeconvolutionFusionSequential(): Model not invertible for " + viewStructure );
                	}
                }// Thread.run loop
            });
        
        SimpleMultiThreading.startAndJoin(threads);	
		
		
		//
		// compute the transformed, aligned images
		//
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println( "Computing transformed input views ... " );
		
		// max size of PSF
		final int[] maxSize = new int[]{ 0, 0 ,0 };
		
		for ( int view = 0; view < numViews; ++view )
		{
			if ( !useView[ view ] )
				continue;
			
			final int i = view;
			
			// load image
			views.get( i ).getImage();
			if ( Multi_View_Deconvolution.subtractBackground != 0 )
			{
        		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
        			IOFunctions.println( "PreDeconvolutionFusionSequential(): Subtracting background of " + Multi_View_Deconvolution.subtractBackground + " from " + views.get( i ).getName() );
        		
				PreDeconvolutionFusion.subtractBackground( views.get( i ).getImage( false ), Multi_View_Deconvolution.subtractBackground );
				views.get( i ).getImage( true );
			}
			
	        ai.set( 0 );
	        threads = SimpleMultiThreading.newThreads(conf.numberOfThreads);
	        
			for (int ithread = 0; ithread < threads.length; ++ithread)
	            threads[ithread] = new Thread(new Runnable()
	            {
	                public void run()
	                {
	                	try
	                	{
	                        final int myNumber = ai.getAndIncrement();
	
	                        // temporary float array
			            	final float[] tmp = new float[ 3 ];
			            	
			            	final int imageSizeX = imageSizes[ i ][ 0 ];
			            	final int imageSizeY = imageSizes[ i ][ 1 ];
			            	final int imageSizeZ = imageSizes[ i ][ 2 ];
			            	
			    			final Point3f tmpCoordinates = new Point3f();
			    			
			    			final LocalizableCursor<FloatType> outIntensity = images[ i ].createLocalizableCursor();
			    			
			    			// create Interpolated Iterators for the input images (every thread need own ones!)
			    			final Interpolator<FloatType> interpolator = views.get( i ).getImage().createInterpolator( conf.interpolatorFactorOutput );
			    			
			    			while ( outIntensity.hasNext() )
			    			{
			    				outIntensity.fwd();
			    				
			    				final int zPos = outIntensity.getPosition( 2 ); 
			    				
			        			if ( zPos % numThreads == myNumber )
			        			{
			        				// get the coordinates if cropped (all coordinates are the same, so we only use the first cursor)
			        				final int x = outIntensity.getPosition( 0 ) + cropOffsetX;
			        				final int y = outIntensity.getPosition( 1 ) + cropOffsetY;
			        				final int z = zPos + cropOffsetZ;
	
			        				// how many view contribute at this position
									tmpCoordinates.x = x * scale + min.x;
	    							tmpCoordinates.y = y * scale + min.y;
	    							tmpCoordinates.z = z * scale + min.z;
	
	    							mpicbg.spim.mpicbg.Java3d.applyInverseInPlace( models[i], tmpCoordinates, tmp );
		
	    							final int locX = Util.round( tmpCoordinates.x );
	    							final int locY = Util.round( tmpCoordinates.y );
	    							final int locZ = Util.round( tmpCoordinates.z );	

	    							// do we hit the source image?
									if ( locX >= 0 && locY >= 0 && locZ >= 0 && 
										 locX < imageSizeX && locY < imageSizeY && locZ < imageSizeZ )
	    							{
	    								tmp[ 0 ] = tmpCoordinates.x;
	    								tmp[ 1 ] = tmpCoordinates.y;
	    								tmp[ 2 ] = tmpCoordinates.z;
	    								
	    								interpolator.setPosition( tmp );
	    								
	    								outIntensity.getType().set( interpolator.getType().get() );
	    							}			    						
		            			} // myThread loop       				
		        			} // iterator loop
		        			
		    				outIntensity.close();
	        				interpolator.close();
	                	}
	                	catch (NoninvertibleModelException e)
	                	{
	                		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
	                			IOFunctions.println( "PreDeconvolutionFusionSequential(): Model not invertible for " + viewStructure );
	                	}
	                }// Thread.run loop
	            });
	        
	        SimpleMultiThreading.startAndJoin(threads);
	        
    		
    		if ( conf.extractPSF )
    		{
    	        // extract the PSF for this one	        
        		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
        			IOFunctions.println( "Extracting PSF for " + views.get( i ).getName() );

    			extractPSF.extract( i, maxSize );
    		}
    		
			// unload image
	        views.get( i ).closeImage();
		}
			
		if ( conf.extractPSF )
			extractPSF.computeAveragePSF( maxSize );
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Done computing output image for deconvolution (Channel " + channelIndex +  ").");
	}

	@Override
	public Image<FloatType> getFusedImage() { return null; }
	
	@Override
	public Image<FloatType> getFusedImage( final int index ) { return images[ index ]; }
	
	@Override
	public Image<FloatType> getWeightImage( final int index ) { return weights[ index ]; }

	@Override
	public void closeImages() 
	{
		for ( final ViewDataBeads view : viewStructure.getViews() ) 
			view.closeImage();
	}

	@Override
	public ArrayList<Image<FloatType>> getPointSpreadFunctions() { return extractPSF.getPSFs(); }

	@Override
	public ExtractPSF getExtractPSFInstance() { return this.extractPSF; }
}
