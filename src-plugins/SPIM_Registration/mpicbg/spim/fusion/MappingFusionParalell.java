package mpicbg.spim.fusion;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3f;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.AffineModel3D;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class MappingFusionParalell extends SPIMImageFusion
{
	final Image<FloatType> fusedImage;
	
	public MappingFusionParalell( final ViewStructure viewStructure, final ViewStructure referenceViewStructure, 
								  final ArrayList<IsolatedPixelWeightenerFactory<?>> isolatedWeightenerFactories, 
								  final ArrayList<CombinedPixelWeightenerFactory<?>> combinedWeightenerFactories )
	{
		super( viewStructure, referenceViewStructure, isolatedWeightenerFactories, combinedWeightenerFactories );				
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Reserving memory for fused image.");
		
		ImageFactory<FloatType> fusedImageFactory = new ImageFactory<FloatType>( new FloatType(), conf.outputImageFactory );
		fusedImage = fusedImageFactory.createImage( new int[]{ imgW, imgH, imgD }, "Fused image"); 

		if (fusedImage == null)
		{
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("MappingFusionParalell.constructor: Cannot create output image: " + conf.outputImageFactory.getErrorMessage());

			return;
		}
	}

	@Override
	public void fuseSPIMImages()
	{
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("Loading source images.");

		// load images 
		for ( final ViewDataBeads view : views )
			view.getImage();
			
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
		final IsolatedPixelWeightener<?>[][] isoW = new IsolatedPixelWeightener<?>[ isolatedWeightenerFactories.size() ][ numViews ];
		for (int j = 0; j < isoW.length; j++)		
		{
			final int i = j;
			
			for (int ithread = 0; ithread < threads.length; ++ithread)
	            threads[ithread] = new Thread(new Runnable()
	            {
	                public void run()
	                {
	                	final int myNumber = ai.getAndIncrement();
	                	
						for (int view = 0; view < numViews; view++)
							if ( view % numThreads == myNumber)
								isoW[i][view] = isolatedWeightenerFactories.get(i).createInstance( views.get(view) );
	                }
	            });
			
			SimpleMultiThreading.startAndJoin( threads );
		}
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Computing output image.");

		// cache the views, imageSizes and models that we use
		final boolean useView[] = new boolean[ numViews ];
		final AffineModel3D models[] = new AffineModel3D[ numViews ];
		
		for ( int i = 0; i < numViews; ++i )
		{
			useView[ i ] = views.get( i ).getViewErrorStatistics().getNumConnectedViews() > 0 || views.get( i ).getViewStructure().getNumViews() == 1;
			models[ i ] = views.get( i ).getTile().getModel(); 
		}
		
		final int[][] imageSizes = new int[numViews][];		
		for ( int i = 0; i < numViews; ++i )
			imageSizes[ i ] = views.get( i ).getImageSize();
		
		ai.set( 0 );					
        threads = SimpleMultiThreading.newThreads( numThreads );

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
		        			combW[i] = combinedWeightenerFactories.get(i).createInstance( viewStructure );
		            	
						// get iterators for isolated weights
		        		final LocalizableByDimCursor<FloatType> isoIterators[][] = new LocalizableByDimCursor[ isoW.length ][ numViews ];            		
						for (int i = 0; i < isoW.length; i++)
							for (int view = 0; view < isoW[i].length; view++)
								isoIterators[i][view] = isoW[i][view].getResultIterator();
		        		
		    			final Point3f[] tmpCoordinates = new Point3f[ numViews ];
		    			final int[][] loc = new int[ numViews ][ 3 ];
		    			final boolean[] use = new boolean[ numViews ];
		    			
		    			for ( int i = 0; i < numViews; ++i )
		    				tmpCoordinates[ i ] = new Point3f();
		    			
		    			final LocalizableCursor<FloatType> it = fusedImage.createLocalizableCursor();
		    			
		    			// create Interpolated Iterators for the input images (every thread need own ones!)
		    			final Interpolator<FloatType>[] interpolators = new Interpolator[ numViews ];
		    			for (int view = 0; view < numViews ; view++)
		    				interpolators[ view ] = views.get( view ).getImage().createInterpolator( conf.interpolatorFactorOutput );
		    			
		    			while (it.hasNext())
		    			{
		    				it.fwd();
		    				
		        			if (it.getPosition(2) % numThreads == myNumber)
		        			{
		        				int countUse = 0;
		        				
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
			
		    							loc[i][0] = MathLib.round( tmpCoordinates[i].x );
		    							loc[i][1] = MathLib.round( tmpCoordinates[i].y );
		    							loc[i][2] = MathLib.round( tmpCoordinates[i].z );	
		    							
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
											w.updateWeights(loc, use);
		
									countUse++;
		
		    						float sumWeights = 0;
		    						float value = 0;
		
		    						for (int view = 0; view < numViews; ++view)
		    							if (use[view])
		    							{
		    								float weight = 1;
		    								
		    								// multiplicate combined weights
		    								if (combW.length > 0)
		    									for (final CombinedPixelWeightener<?> w : combW)
		    										weight *= w.getWeight(view);
		    								
		    								// multiplicate isolated weights
		    								for (int i = 0; i < isoW.length; i++)
		    								{
		    									isoIterators[ i ][ view ].setPosition( loc[ view ] );
												weight *= isoIterators[ i ][ view ].getType().get();
		    								}        									      
		    								
		    								tmp[ 0 ] = tmpCoordinates[view].x;
		    								tmp[ 1 ] = tmpCoordinates[view].y;
		    								tmp[ 2 ] = tmpCoordinates[view].z;
		    								
		    								interpolators[view].moveTo( tmp );
		    								
		    								value += weight * interpolators[view].getType().get(); 
		    								sumWeights += weight;
		    							}
		    							
		    						if (sumWeights > 0)
		    							it.getType().set(value/sumWeights);
		
	    						}
	    						
	            			} // myThread loop       				
	        			} // iterator loop
	        			
	        			it.close();
	        			for (int view = 0; view < numViews; view++)
	        				interpolators[view].close();
	        			
	        			// close combined pixel weighteners
	        			for (int i = 0; i < combW.length; i++)
	        				combW[i].close();     
	        			
	        			// close isolated iterators
						for (int i = 0; i < isoW.length; i++)
							for (int view = 0; view < isoW[i].length; view++)
								isoIterators[i][view].close();
         			
                	}
                	catch (NoninvertibleModelException e)
                	{
                		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
                			IOFunctions.println( "MappingFusionParalell(): Model not invertible for " + viewStructure );
                	}
                }// Thread.run loop
            });
        
        SimpleMultiThreading.startAndJoin(threads);	

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Closing all input images.");

		// unload images
		for ( final ViewDataBeads view : views ) 
			view.closeImage();
			
		// close weighteners		
		// close isolated pixel weighteners
		for (int i = 0; i < isoW.length; i++)
			for (int view = 0; view < numViews; view++)
				isoW[i][view].close();
				
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Done computing output image.");
	}

	@Override
	public Image<FloatType> getFusedImage() { return fusedImage; } 
}
