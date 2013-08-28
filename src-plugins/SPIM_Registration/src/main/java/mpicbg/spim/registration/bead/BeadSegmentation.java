package mpicbg.spim.registration.bead;

import ij.IJ;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

import mpicbg.imglib.algorithm.integral.IntegralImageLong;
import mpicbg.imglib.algorithm.math.LocalizablePoint;
import mpicbg.imglib.algorithm.peak.GaussianPeakFitterND;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussian.SpecialPoint;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianReal1;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.cursor.LocalizableByDimCursor3D;
import mpicbg.imglib.cursor.special.HyperSphereIterator;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.integer.IntType;
import mpicbg.imglib.type.numeric.integer.LongType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.io.SPIMConfiguration.SegmentationTypes;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.laplace.LaPlaceFunctions;
import mpicbg.spim.registration.threshold.ComponentProperties;
import mpicbg.spim.registration.threshold.ConnectedComponent;
import mpicbg.spim.segmentation.DOM;
import mpicbg.spim.segmentation.IntegralImage3d;
import mpicbg.spim.segmentation.InteractiveIntegral;
import mpicbg.spim.segmentation.SimplePeak;

public class BeadSegmentation
{	
	public static final boolean debugBeads = false;
	public ViewStructure viewStructure;
	public SegmentationBenchmark benchmark = new SegmentationBenchmark();
	
	public BeadSegmentation( final ViewStructure viewStructure ) 
	{
		this.viewStructure = viewStructure;
	}	
	
	public void segment( )
	{
		segment( viewStructure.getSPIMConfiguration(), viewStructure.getViews() );
	}
	
	public void segment( final SPIMConfiguration conf, final ArrayList<ViewDataBeads> views )
	{
		final float threshold = conf.threshold;
				
		//
		// Extract the beads
		// 		
		for ( final ViewDataBeads view : views )
		{
			if ( conf.segmentation == SegmentationTypes.DOG )					
			{
	    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Scale Space Bead Extraction for " + view.getName() );
				
	    		view.setBeadStructure( extractBeadsLaPlaceImgLib( view, conf ) );
	    		
	    		if ( debugBeads )
	    		{
					Image<FloatType> img = getFoundBeads( view );				
					img.setName( "imglib" );
					img.getDisplay().setMinMax();
					ImageJFunctions.copyToImagePlus( img ).show();				
					SimpleMultiThreading.threadHaltUnClean();		    			
	    		}
			}
			else if ( conf.segmentation == SegmentationTypes.THRESHOLD )
			{
	    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Threshold Bead Extraction for " + view.getName() );				
				
				view.setBeadStructure( extractBeadsThresholdSegmentation( view, threshold, conf.minSize, conf.maxSize, conf.minBlackBorder) );
			}
			else if ( conf.segmentation == SegmentationTypes.DOM )
			{
	    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Integral Image based DOM Bead Extraction for " + view.getName() );
	    		
				view.setBeadStructure( extractBeadsDOM( view, conf ) );
			}
			else
			{
				throw new RuntimeException( "Unknown segmentation: " + conf.segmentation );
			}

			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println( "Found peaks (possible beads): " + view.getBeadStructure().getBeadList().size() + " in view " + view.getName() );

			//
			// do we want to re-localize all detections with a gauss fit?
			//
			if ( conf.doFit == 3 )
			{
				if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Gaussian fit for all detections (this will take a little)");
				
				double sxy = 2;//10;
				double sz = 2;//(1.5 * sxy) / conf.zStretching;
				
				IJ.log( "Assumed sigma: [" + sxy + ", " + sxy + ", " + sz + "]" );
					
				final double[] typicalSigma = new double[]{ sxy, sxy, sz };
				
				gaussFit( view.getImage(), view.getBeadStructure().getBeadList(), typicalSigma );
			}
				
			// close the image if no re-localization is required and/or the memory is low
			if ( !( conf.doFit == 2 && conf.doGaussKeepImagesOpen ) )
				view.closeImage();
			
			//
			// Store segmentation in a file
			//
			if ( conf.writeSegmentation )
				IOFunctions.writeSegmentation( view, conf.registrationFiledirectory );										
		}
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
		{
			int p1 = (int)Math.round( (double)benchmark.openFiles/((double)benchmark.computation+(double)benchmark.openFiles) * 100 );
			int p2 = (int)Math.round( (double)benchmark.computation/((double)benchmark.computation+(double)benchmark.openFiles) * 100 );
			IJ.log( "Opening files took: " + benchmark.openFiles/1000 + " sec (" + p1 + " %)" );
			IJ.log( "Computation took: " + benchmark.computation/1000 + " sec (" + p2 + " %)" );
		}
	}
	
	public int reLocalizeTrueCorrespondences( final boolean run )
	{
		// how many beads are (potentially) relocalized
		int count = 0;
		
		for ( final ViewDataBeads view : viewStructure.getViews() )
		{
			final ArrayList< Bead > beadList = new ArrayList<Bead>();
			
			// list all beads that have RANSAC correspondences
			for ( final Bead bead : view.getBeadStructure().getBeadList() )
				if ( bead.getRANSACCorrespondence().size() > 0 && !bead.relocalized )
					beadList.add( bead );
			
			count += beadList.size();
			
			if ( run )
			{
				if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Re-localizing " + beadList.size() + " beads using gaussian fit for view " + view.getName() );
				
				gaussFit( view.getImage( false ), beadList, new double[]{ 1, 1, 2 } );
				
				if ( !viewStructure.getSPIMConfiguration().doGaussKeepImagesOpen )
					view.closeImage();
			}
		}
		
		return count;
	}
	
	public static void gaussFit( final Image< FloatType > image, final ArrayList< Bead > beadList, final double[] typicalSigma )
	{
		final AtomicInteger ai = new AtomicInteger( 0 );					
        final Thread[] threads = SimpleMultiThreading.newThreads();
        
		final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( beadList.size(), threads.length );
		
		final int[] count = new int[ threads.length ];
		final double[][] sigmas = new double[ threads.length ][ 3 ];
		
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                 	// Thread ID
                	final int myNumber = ai.getAndIncrement();
                	
                	// get chunk of beads to process
                	final Chunk myChunk = threadChunks.get( myNumber );
                	final int loopSize = (int)myChunk.getLoopSize();
                	final int start = (int)myChunk.getStartPosition();
                	final int end = start + loopSize;
                	
                	final GaussianPeakFitterND<FloatType> fitter = new GaussianPeakFitterND<FloatType>( image );
                	
            		// do as many pixels as wanted by this thread
                    for ( int j = start; j < end; ++j )
                    {
                    	final Bead bead = beadList.get( j );
                    	
            			final double[] results = fitter.process( new LocalizablePoint( bead.getL() ), typicalSigma );
            			
            			//double a = results[ 0 ];
            			//double x = results[ 1 ];
            			//double y = results[ 2 ];
            			//double z = results[ 3 ];
            			double sx = 1/Math.sqrt( results[ 4 ] );
            			double sy = 1/Math.sqrt( results[ 5 ] );
            			double sz = 1/Math.sqrt( results[ 6 ] );
            			
            			bead.getL()[ 0 ] = bead.getW()[ 0 ] = (float)results[ 1 ];
            			bead.getL()[ 1 ] = bead.getW()[ 1 ] = (float)results[ 2 ];
            			bead.getL()[ 2 ] = bead.getW()[ 2 ] = (float)results[ 3 ];
            			
            			bead.relocalized = true;
            			
            			if ( ! (Double.isNaN( sx ) || Double.isNaN( sy ) || Double.isNaN( sz ) ) )
            			{
            				sigmas[ myNumber ][ 0 ] += sx;
            				sigmas[ myNumber ][ 1 ] += sy;
            				sigmas[ myNumber ][ 2 ] += sz;
            				count[ myNumber ]++;
            			}

                    }
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );

        // compute final average sigma's
		for ( int i = 1; i < sigmas.length; ++i )
		{
			sigmas[ 0 ][ 0 ] += sigmas[ i ][ 0 ];
			sigmas[ 0 ][ 1 ] += sigmas[ i ][ 1 ];
			sigmas[ 0 ][ 2 ] += sigmas[ i ][ 2 ];
			count[ 0 ] += count[ i ];
		}
		
		IJ.log( "avg sigma: [" + ( sigmas[ 0 ][ 0 ] / count[ 0 ] ) + " px, " + ( sigmas[ 0 ][ 1 ] / count[ 0 ] ) + " px, " + ( sigmas[ 0 ][ 2 ] / count[ 0 ] ) + " px]" );
	}
		
	public Image<FloatType> getFoundBeads( final ViewDataBeads view )
	{
		// display found beads
		ImageFactory<FloatType> factory = new ImageFactory<FloatType>( new FloatType(), view.getViewStructure().getSPIMConfiguration().imageFactory );
		Image<FloatType> img = factory.createImage( view.getImageSize() );
		
		LocalizableByDimCursor3D<FloatType> cursor = (LocalizableByDimCursor3D<FloatType>) img.createLocalizableByDimCursor();		
		
		for ( Bead bead : view.getBeadStructure().getBeadList())
		{
			final float[] pos = bead.getL();
			
			final LocalizablePoint p = new LocalizablePoint( pos );
			
			HyperSphereIterator<FloatType> it = new HyperSphereIterator<FloatType>( img, p, 1, new OutOfBoundsStrategyValueFactory<FloatType>() );
			
			for ( final FloatType f : it )
				f.setOne();			
		}
		
		cursor.close();

		return img;
	}

	public static boolean subpixel = true;
	
	/**
	 * Computes potential beads based on the difference-of-mean (DOM). The computation of the DOM is based on
	 * integral images for high performance
	 *  
	 * @param view
	 * @param conf
	 * @return
	 */
	protected BeadStructure extractBeadsDOM( final ViewDataBeads view, final SPIMConfiguration conf )
	{
		long time1 = System.currentTimeMillis();
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Opening Image");					

		final Image< FloatType > img = view.getImage( false ); 

		long time2 = System.currentTimeMillis();
		benchmark.openFiles += time2 - time1;

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
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
		
		final FloatType min = new FloatType();
		final FloatType max = new FloatType();
		
		if ( ViewDataBeads.minmaxset == null )
		{
			DOM.computeMinMax( img, min, max );
		}
		else
		{
			min.set( ViewDataBeads.minmaxset[ 0 ] );
			max.set( ViewDataBeads.minmaxset[ 1 ] );
		}

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): min intensity = " + min.get() + ", max intensity = " + max.get() );
		
		// in-place
		final int r1 = view.getIntegralRadius1();
		final int r2 = view.getIntegralRadius2();
		final float t = view.getIntegralThreshold();
		
		final int s1 = r1*2 + 1;
		final int s2 = r2*2 + 1;

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Computing Difference-of-Mean");					

		// in-place overwriting img if no adjacent Gauss fit is required
		final Image< FloatType > domImg;
		
		if ( conf.doFit == 3 || ( conf.doFit == 2 && conf.doGaussKeepImagesOpen ) )
		{
			domImg = img.createNewImage();
		}
		else
		{
			domImg = img;
			for ( final FloatType tt : img )
				tt.setZero();
		}
		
		DOM.computeDifferencOfMean3d( integralImg, domImg, s1, s1, s1, s2, s2, s2, min.get(), max.get() );
		
		// close integral img
		integralImg.close();
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Extracting peaks");					

		// compute the maxima/minima
		final ArrayList< SimplePeak > peaks = InteractiveIntegral.findPeaks( domImg, t );

		final BeadStructure beads = new BeadStructure();
		int id = 0;

		// do quadratic fit??
		if ( conf.doFit == 1 )
		{
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Subpixel localization using quadratic n-dimensional fit");					

	        final ArrayList< DifferenceOfGaussianPeak<FloatType> > peakList = new ArrayList<DifferenceOfGaussianPeak<FloatType>>();

	        for ( final SimplePeak peak : peaks )
	        	if ( peak.isMax ) 
	        		peakList.add( new DifferenceOfGaussianPeak<FloatType>( peak.location, new FloatType( peak.intensity ), SpecialPoint.MAX ) );

			
	        final SubpixelLocalization<FloatType> spl = new SubpixelLocalization<FloatType>( domImg, peakList );
			spl.setAllowMaximaTolerance( true );
			spl.setMaxNumMoves( 10 );
			
			if ( !spl.checkInput() || !spl.process() )
			{
	    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Warning! Failed to compute subpixel localization " + spl.getErrorMessage() );
			}
			
			final float[] pos = new float[ img.getNumDimensions() ];
	                
	        for ( DifferenceOfGaussianPeak<FloatType> maximum : peakList )
	        {
	        	if ( maximum.isMax() ) 
	        	{
	        		maximum.getSubPixelPosition( pos );
		        	final Bead bead = new Bead( id, new Point3d( pos[ 0 ], pos[ 1 ], pos[ 2 ] ), view );
		        	beads.addDetection( bead );
		        	id++;
	        	}
	        }
		}
		else
		{
	        for ( final SimplePeak peak : peaks )
	        {
	        	if ( peak.isMax ) 
	        	{
		        	final Bead bead = new Bead( id++, new Point3d( peak.location[ 0 ], peak.location[ 1 ], peak.location[ 2 ] ), view );
		        	beads.addDetection( bead );
	        	}
	        }
		}       

		// if we made a copy we close it now
        if ( domImg != img )
        	domImg.close();
        
        benchmark.computation += System.currentTimeMillis() - time2;
        
		return beads;
	}
	
	protected BeadStructure extractBeadsLaPlaceImgLib( final ViewDataBeads view, final SPIMConfiguration conf )
	{
		long time1 = System.currentTimeMillis();
		
		// load the image
		final Image<FloatType> img = view.getImage();

		long time2 = System.currentTimeMillis();
		benchmark.openFiles += time2 - time1;
		
        float imageSigma = conf.imageSigma;
        float initialSigma = view.getInitialSigma();

        final float minPeakValue;
        final float minInitialPeakValue;

        // adjust for 12bit images
        // we stop doing that for now...
        if ( view.getMaxValueUnnormed() > 256 )
        {
        	minPeakValue = view.getMinPeakValue();///3;
        	minInitialPeakValue = view.getMinInitialPeakValue();///3;
        }
        else
        {
            minPeakValue = view.getMinPeakValue();
            minInitialPeakValue = view.getMinInitialPeakValue();
        }        

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
		{
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): min intensity = " + view.getMinValue() + ", max intensity = " + view.getMaxValue() );
			IOFunctions.println( view.getName() + " sigma: " + initialSigma + " minPeakValue: " + minPeakValue );
		}
        final float k = LaPlaceFunctions.computeK(conf.stepsPerOctave);
        final float K_MIN1_INV = LaPlaceFunctions.computeKWeight(k);
        final int steps = conf.steps;

        //
        // Compute the Sigmas for the gaussian folding
        //
        final float[] sigma = LaPlaceFunctions.computeSigma(steps, k, initialSigma);
        final float[] sigmaDiff = LaPlaceFunctions.computeSigmaDiff(sigma, imageSigma);
         
		// compute difference of gaussian
		final DifferenceOfGaussianReal1<FloatType> dog = new DifferenceOfGaussianReal1<FloatType>( img, conf.strategyFactoryGauss, sigmaDiff[0], sigmaDiff[1], minInitialPeakValue, K_MIN1_INV );
		
		// do quadratic fit??
		if ( conf.doFit == 1 )
			dog.setKeepDoGImage( true );
		else
			dog.setKeepDoGImage( false );
		
		if ( !dog.checkInput() || !dog.process() )
		{
    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Cannot compute difference of gaussian for " + dog.getErrorMessage() );
			
			return new BeadStructure();
		}
		
		// remove all minima
        final ArrayList< DifferenceOfGaussianPeak<FloatType> > peakListOld = dog.getPeaks();
        final ArrayList< DifferenceOfGaussianPeak<FloatType> > peakList = new ArrayList<DifferenceOfGaussianPeak<FloatType>>();
        
        for ( int i = peakListOld.size() - 1; i >= 0; --i )
        	if ( peakListOld.get( i ).isMax() )
        		peakList.add( peakListOld.get( i ) );

		// do quadratic fit??
		if ( conf.doFit == 1 )
		{
	        final SubpixelLocalization<FloatType> spl = new SubpixelLocalization<FloatType>( dog.getDoGImage(), dog.getPeaks() );
			spl.setAllowMaximaTolerance( true );
			spl.setMaxNumMoves( 10 );
			
			if ( !spl.checkInput() || !spl.process() )
			{
	    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Warning! Failed to compute subpixel localization " + spl.getErrorMessage() );
			}
	
			dog.getDoGImage().close();
		}
		
        final BeadStructure beads = new BeadStructure();
        int id = 0;
        final float[] pos = new float[ img.getNumDimensions() ];
        
        int peakTooLow = 0;
        int invalid = 0;
        int max = 0;
                
        for ( DifferenceOfGaussianPeak<FloatType> maximum : dog.getPeaks() )
        {
        	if ( !maximum.isValid() )
        		invalid++;
        	if ( maximum.isMax() )
        		max++;
        	
        	if ( maximum.isMax() ) 
        	{
        		if ( Math.abs( maximum.getValue().get() ) >= minPeakValue )
        		{
	        		maximum.getSubPixelPosition( pos );
		        	final Bead bead = new Bead( id, new Point3d( pos[ 0 ], pos[ 1 ], pos[ 2 ] ), view );
		        	beads.addDetection( bead );
		        	id++;
        		}
        		else
        		{
        			peakTooLow++;
        		}
        	}
        }
        
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
		{
	        IOFunctions.println( "number of peaks: " + dog.getPeaks().size() );        
	        IOFunctions.println( "invalid: " + invalid );
	        IOFunctions.println( "max: " + max );
	        IOFunctions.println( "peak to low: " + peakTooLow );
		}
		
		benchmark.computation += System.currentTimeMillis() - time2;
		
		return beads;
		
	}
	
	protected BeadStructure extractBeadsThresholdSegmentation( final ViewDataBeads view, final float thresholdI, final int minSize, final int maxSize, final int minBlackBorder)
	{
		final SPIMConfiguration conf = view.getViewStructure().getSPIMConfiguration();
		final Image<FloatType> img  = view.getImage();
		
		//
		// Extract connected components
		//
		
		final ImageFactory<IntType> imageFactory = new ImageFactory<IntType>( new IntType(), img.getContainerFactory() );
		final Image<IntType> connectedComponents = imageFactory.createImage( img.getDimensions() );

		int label = 0;
		
		img.getDisplay().setMinMax();
		
		final int maxValue = (int) Util.round( img.getDisplay().getMax() );

		final float thresholdImg;

		if ( !conf.useFixedThreshold )
		{
    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
    			IOFunctions.println( view.getName() + " maximum value: " + maxValue + " means a threshold of " + thresholdI*maxValue );
    		
			thresholdImg = thresholdI*maxValue;
		}
		else
		{
    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
    			IOFunctions.println( view.getName() + " maximum value: " + maxValue + " using a threshold of " + conf.fixedThreshold );
    		
			thresholdImg = conf.fixedThreshold;
		}
		
		final ArrayList<Point3i> neighbors = getVisitedNeighbors();
		final ConnectedComponent components = new ConnectedComponent();
		
		final int w = connectedComponents.getDimension( 0 );
		final int h = connectedComponents.getDimension( 1 );
		final int d = connectedComponents.getDimension( 2 );

		final LocalizableByDimCursor3D<FloatType> cursorImg = (LocalizableByDimCursor3D<FloatType>) img.createLocalizableByDimCursor();
		final LocalizableByDimCursor3D<IntType> cursorComponents = (LocalizableByDimCursor3D<IntType>) connectedComponents.createLocalizableByDimCursor();
		
		for (int z = 0; z < d; z++)
		{
			//IOFunctions.println("Processing z: " + z);

			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
				{
					cursorImg.setPosition( x, y, z );
					
					// is above threshold?
					if ( cursorImg.getType().get() > thresholdImg)
					{
						// check previously visited neighboring pixels if they
						// have a label assigned
						ArrayList<Integer> neighboringLabels = getNeighboringLabels(connectedComponents, neighbors, x, y, z);

						// if there are no neighors, introduce new label
						if (neighboringLabels == null || neighboringLabels.size() == 0)
						{
							label++;
							cursorComponents.setPosition( x, y, z );
							cursorComponents.getType().set( label );
							components.addLabel(label);
						}
						else if (neighboringLabels.size() == 1) 
						// if there is only one neighboring label, set this one
						{
							cursorComponents.getType().set( neighboringLabels.get(0) );
						}
						else
						// remember all the labels are actually the same
						// because they touch
						{
							int label1 = neighboringLabels.get(0);

							try
							{
								for (int i = 1; i < neighboringLabels.size(); i++)
									components.addEqualLabels(label1, neighboringLabels.get(i));
							}
							catch (Exception e)
							{
								e.printStackTrace();
								IOFunctions.printErr("\n" + x + " " + y);
								System.exit(0);
							}

							cursorComponents.getType().set( label1 );
						}
					}
				}
		}
		
		cursorImg.close();
		cursorComponents.close();
		
		// merge components
		components.equalizeLabels(connectedComponents);
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
			IOFunctions.println("Found " + components.distinctLabels.size() + " distinct labels out of " + label + " labels");
		
		// remove invalid components
		ArrayList<ComponentProperties> segmentedBeads = components.getBeads(connectedComponents, img, minSize, maxSize, minBlackBorder, conf.useCenterOfMass, conf.circularityFactor);
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("Fount Beads: " + segmentedBeads.size());

		final BeadStructure beads = new BeadStructure();
		int id = 0;
		
		for ( final ComponentProperties comp : segmentedBeads )
		{
			Bead bead = new Bead( id, comp.center, view );
			beads.addDetection( bead );
			id++;
		}
		
		return beads;
	}
	
	protected ArrayList<Integer> getNeighboringLabels( final Image<IntType> connectedComponents, final ArrayList<Point3i> neighbors, final int x, final int y, final int z )
	{
		final ArrayList<Integer> labels = new ArrayList<Integer>();
		final Iterator<Point3i> iterateNeighbors = neighbors.iterator();
		
		final int w = connectedComponents.getDimension( 0 );
		final int h = connectedComponents.getDimension( 1 );
		final int d = connectedComponents.getDimension( 2 );
		
		final LocalizableByDimCursor3D<IntType> cursor = (LocalizableByDimCursor3D<IntType>) connectedComponents.createLocalizableByDimCursor();

		while (iterateNeighbors.hasNext())
		{
			Point3i neighbor = iterateNeighbors.next();

			int xp = x + neighbor.x;
			int yp = y + neighbor.y;
			int zp = z + neighbor.z;

			if (xp >= 0 && yp >= 0 && zp >= 0 && xp < w && yp < h && zp < d )
			{
				cursor.setPosition( xp, yp, zp );
				int label = cursor.getType().get();

				if (label != 0 && !labels.contains(neighbor)) 
					labels.add(label);
			}
		}
		
		cursor.close();

		return labels;
	}

	protected ArrayList<Point3i> getVisitedNeighbors()
	{
		ArrayList<Point3i> visitedNeighbors = new ArrayList<Point3i>();

		int z = -1;

		for (int y = -1; y <= 1; y++)
			for (int x = -1; x <= 1; x++)
				visitedNeighbors.add(new Point3i(x, y, z));

		visitedNeighbors.add(new Point3i(-1, 0, 0));
		visitedNeighbors.add(new Point3i(-1, -1, 0));
		visitedNeighbors.add(new Point3i(0, -1, 0));
		visitedNeighbors.add(new Point3i(1, -1, 0));

		return visitedNeighbors;
	}	
	
	
}
