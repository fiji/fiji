package mpicbg.spim.registration.bead;

import ij.IJ;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

import mpicbg.imglib.algorithm.math.LocalizablePoint;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianReal1;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.cursor.LocalizableByDimCursor3D;
import mpicbg.imglib.cursor.special.HyperSphereIterator;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.integer.IntType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.laplace.LaPlaceFunctions;
import mpicbg.spim.registration.threshold.ComponentProperties;
import mpicbg.spim.registration.threshold.ConnectedComponent;

public class BeadSegmentation
{	
	public static final boolean debugBeads = false;
	public ViewStructure viewStructure;
	
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
		if ( conf.multiThreadedOpening )
		{
			final int numThreads = views.size();
			
			for ( final ViewDataBeads view : views )
				if ( view.getUseForRegistration() )
					view.getImage();

			final AtomicInteger ai = new AtomicInteger(0);					
	        Thread[] threads = SimpleMultiThreading.newThreads( numThreads );

			for (int ithread = 0; ithread < threads.length; ++ithread)
	            threads[ithread] = new Thread(new Runnable()
	            {
	                public void run()
	                {
	                    final int myNumber = ai.getAndIncrement();

	                    final ViewDataBeads view = views.get( myNumber );
	                    
	                    if ( view.getUseForRegistration() )
	                    {	                    
		        			if (conf.useScaleSpace)					
		        			{
		        	    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
		        	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Scale Space Bead Extraction for " + view.getName() );
		        				
		        	    		view.setBeadStructure( extractBeadsLaPlaceImgLib( view, conf ) );

		        				view.closeImage();
		        			}
		        			else
		        			{
		        	    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
		        	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Threshold Bead Extraction");					
		        				
		        				view.setBeadStructure( extractBeadsThresholdSegmentation( view, threshold, conf.minSize, conf.maxSize, conf.minBlackBorder) );
		        				
		        				view.closeImage();				
		        			}
		        				
		        			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
		        				IOFunctions.println( "Found peaks (possible beads): " + view.getBeadStructure().getBeadList().size() );
		        			
		        			//
		        			// Store segmentation in a file
		        			//
		        			if ( conf.writeSegmentation )
		        				IOFunctions.writeSegmentation( view, conf.registrationFiledirectory );
	                    }
	                }
	            });
			
			SimpleMultiThreading.startAndJoin( threads );
		}
		else
		{		
			for ( final ViewDataBeads view : views )
			{
				if (conf.useScaleSpace)					
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
		    		
					view.closeImage();
				}
				else
				{
		    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
		    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Threshold Bead Extraction");					
					
					view.setBeadStructure( extractBeadsThresholdSegmentation( view, threshold, conf.minSize, conf.maxSize, conf.minBlackBorder) );
					
					view.closeImage();				
				}
					
				if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
					IOFunctions.println( "Found peaks (possible beads): " + view.getBeadStructure().getBeadList().size() );
				
				//
				// Store segmentation in a file
				//
				if ( conf.writeSegmentation )
					IOFunctions.writeSegmentation( view, conf.registrationFiledirectory );										
			}
		}
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
			
	protected BeadStructure extractBeadsLaPlaceImgLib( final ViewDataBeads view, final SPIMConfiguration conf )
	{
		// load the image
		final Image<FloatType> img = view.getImage();

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

        IOFunctions.println( view.getName() + " sigma: " + initialSigma + " minPeakValue: " + minPeakValue );

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
		dog.setKeepDoGImage( true );
		
		if ( !dog.checkInput() || !dog.process() )
		{
    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Cannot compute difference of gaussian for " + dog.getErrorMessage() );
			
			return new BeadStructure();
		}

		// remove all minima
        final ArrayList< DifferenceOfGaussianPeak<FloatType> > peakList = dog.getPeaks();
        for ( int i = peakList.size() - 1; i >= 0; --i )
        	if ( peakList.get( i ).isMin() )
        		peakList.remove( i );
		
        final SubpixelLocalization<FloatType> spl = new SubpixelLocalization<FloatType>( dog.getDoGImage(), dog.getPeaks() );
		spl.setAllowMaximaTolerance( true );
		spl.setMaxNumMoves( 10 );
		
		if ( !spl.checkInput() || !spl.process() )
		{
    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Warning! Failed to compute subpixel localization " + spl.getErrorMessage() );
		}

		dog.getDoGImage().close();
    	
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
