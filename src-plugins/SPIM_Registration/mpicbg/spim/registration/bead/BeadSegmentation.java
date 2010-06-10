package mpicbg.spim.registration.bead;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

import mpicbg.imglib.algorithm.gauss.GaussianConvolution;
import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.cursor.LocalizableByDimCursor3D;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.integer.IntType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.laplace.DoGMaximum;
import mpicbg.spim.registration.bead.laplace.LaPlaceFunctions;
import mpicbg.spim.registration.bead.laplace.RejectStatistics;
import mpicbg.spim.registration.threshold.ComponentProperties;
import mpicbg.spim.registration.threshold.ConnectedComponent;

public class BeadSegmentation
{	
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
		for ( final ViewDataBeads view : views )
		{
			if (conf.useScaleSpace)					
			{
	    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Scale Space Bead Extraction for " + view.getName() );
				
				view.setBeadStructure( extractBeadsLaPlace( view, conf ) );
				
				/*
				final Image<FloatType> img = getFoundBeads( view );
				
				img.getDisplay().setMinMax();
				ImageJFunctions.copyToImagePlus( img ).show();
				
				SimpleMultiThreading.threadHaltUnClean();
				*/
				
				view.closeImage();
			}
			else
			{
	    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
	    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Threshold Bead Extraction");					
				
				view.setBeadStructure( extractBeadsThresholdSegmentation( view, threshold, conf.minSize, conf.maxSize, conf.minBlackBorder) );
				
				view.closeImage();				
			}
										
			//
			// Store segmentation in a file
			//
			if ( conf.writeSegmentation )
				IOFunctions.writeSegmentation( view, conf.registrationFiledirectory );										
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
			float[] pos = bead.getL();
			cursor.setPosition((int)(pos[0] + 0.5), (int)(pos[1] + 0.5), (int)(pos[2] + 0.5));
			cursor.getType().setOne();
		}
		
		cursor.close();

		return img;
	}
	
	/*
	protected BeadStructure extractBeadsScaleSpace(final Float3D img, final SPIMConfiguration conf, final ViewDataBeads view)
	{
    	final RejectStatistics rsFinal = new RejectStatistics();
        ArrayList<DoGMaximum> localMaximaFinal = new ArrayList<DoGMaximum>();
    	
    	final int width = img.getWidth();
        final int height = img.getHeight();
        final int depth = img.getDepth();
        
        // found local maxima
       
        // start with sigma = 1,6
        // assume it to be 0,5 in the original image
        // d(sigma) = sqrt(sigmaB^2 - sigmaA^2)
        
        final float imageSigma = conf.imageSigma;
        final float initialSigma = conf.initialSigma;

        final float k = ScaleSpace3D.computeK(conf.stepsPerOctave);
        final float K_MIN1_INV = ScaleSpace3D.computeKWeight(k);
        final int steps = conf.steps; 

        //
        // Compute the Sigmas for the gaussian folding
        //
        final float[] sigma = ScaleSpace3D.computeSigma(steps, k, initialSigma);
        final float[] sigmaDiff = ScaleSpace3D.computeSigmaDiff(sigma, imageSigma);
              
        //
        // Now initially fold with gaussian kernel to get to sigma = 1.6
        //
        final Float3D[] octaveSteps = new Float3D[2];
        final Float3D[] laPlace = new Float3D[3];

        octaveSteps[0] = ImageFilter.computeGaussianFast(img, sigmaDiff[0], conf.scaleSpaceNumberOfThreads, conf.strategyFactoryGauss);
        octaveSteps[1] = ImageFilter.computeGaussianFast(img, sigmaDiff[1], conf.scaleSpaceNumberOfThreads, conf.strategyFactoryGauss);        
        laPlace[0] = conf.scaleSpaceFactory.createInstanceNoException(1, 1, 1); 
        laPlace[1] = ScaleSpace3D.subtractArrays(octaveSteps[1], octaveSteps[0], K_MIN1_INV);
        
        laPlace[1].convertToImagePlus("test1").show();
        
        octaveSteps[0].close();
        octaveSteps[0] = octaveSteps[1];
        octaveSteps[1] = ImageFilter.computeGaussianFast(img, sigmaDiff[2], conf.scaleSpaceNumberOfThreads, conf.strategyFactoryGauss);
        laPlace[2] = ScaleSpace3D.subtractArrays(octaveSteps[1], octaveSteps[0], K_MIN1_INV);
        
        laPlace[2].convertToImagePlus("test2").show();

        //
        // Now compute the rest of the steps
        //
        for ( int step = 3; step <= steps; step++ )
        {
            IOFunctions.println(step + ": " + sigma[step]);

        	// update and compute gauss
        	octaveSteps[0].close();
            octaveSteps[0] = null;
            octaveSteps[0] = octaveSteps[1];
            IOFunctions.println(step + ": Compute gauss " + new Date(System.currentTimeMillis()));
            octaveSteps[1] = ImageFilter.computeGaussianFast(img, sigmaDiff[step], conf.scaleSpaceNumberOfThreads, conf.strategyFactoryGauss);
            IOFunctions.println(step + ": Done Compute gauss" + new Date(System.currentTimeMillis()));
                        
            // Now compute and update LaPlace
            laPlace[0].close();
            laPlace[0] = null;
            laPlace[0] = laPlace[1];
            laPlace[1] = laPlace[2];
            IOFunctions.println(step + ": Subtract arrays " + new Date(System.currentTimeMillis()));
            laPlace[2] = ScaleSpace3D.subtractArrays(octaveSteps[1], octaveSteps[0], K_MIN1_INV);
            IOFunctions.println(step + ": Done Subtract arrays" + new Date(System.currentTimeMillis()));

            laPlace[2].convertToImagePlus("test3").show();

            //
            // Fit quadratic function and reject points which do not fit
            // And also compute the principle curvatures which are
            //
            IOFunctions.println( new Date(System.currentTimeMillis()) + ": " + step + ": Analyze Maxima " + new Date(System.currentTimeMillis()));

            final AtomicInteger ai = new AtomicInteger(0);					
            Thread[] threads = MultiThreading.newThreads(conf.numberOfThreads);
            final int numThreads = threads.length;
            
            final int stepLocal = step;
            final RejectStatistics rsList[] = new RejectStatistics[ numThreads ];

            @SuppressWarnings("unchecked")
            final ArrayList<DoGMaximum> localMaximaList[] = new ArrayList[ numThreads ];


			for (int ithread = 0; ithread < threads.length; ++ithread)
	            threads[ithread] = new Thread(new Runnable()
	            {
	                public void run()
	                {
	                	final int myNumber = ai.getAndIncrement();
	                	
	                	rsList[ myNumber ] = new RejectStatistics();
	                	localMaximaList[ myNumber ] = new ArrayList<DoGMaximum>();
	                	
	                	final RejectStatistics rs = rsList[ myNumber ];
	                	final ArrayList<DoGMaximum> localMaxima = (ArrayList<DoGMaximum>) localMaximaList[ myNumber ];

	                    FloatLocalizableIteratorByDim it0 = laPlace[0].createLocalizableIteratorByDim( new FloatOutOfBoundsStrategyValueFactory(0) );
	                    FloatLocalizableIteratorByDim it1 = laPlace[1].createLocalizableIteratorByDim( new FloatOutOfBoundsStrategyValueFactory(0) );
	                    FloatLocalizableIteratorByDim it2 = laPlace[2].createLocalizableIteratorByDim( new FloatOutOfBoundsStrategyValueFactory(0) );
	                    
	                    while (it1.hasNext())
	                    {
	                    	it0.next();
	                    	it1.next();
	                    	it2.next();
	                    	
	                    	final int x = it1.getX();
	                    	final int y = it1.getY();
	                    	final int z = it1.getZ();
	                    	
	                    	if ( z % numThreads == myNumber )
	                    	{
		                    	if (x > 0 && y > 0 && z > 0 && x < width - 1 && y < height - 1 && z < depth - 1)
		                    	{
		                    		if ( ScaleSpace3D.isSpecialPoint(it1, it0, it2, conf.minInitialPeakValue) )
		                    		{
		                            	ScaleSpace3D.analyzeMaximum(it0, conf.minPeakValue, width, height, depth, sigma[stepLocal - 2 - (1 - 0)], conf.identityRadius, conf.maximaTolerance, rs, localMaxima);
		                            	ScaleSpace3D.analyzeMaximum(it1, conf.minPeakValue, width, height, depth, sigma[stepLocal - 2 - (1 - 1)], conf.identityRadius, conf.maximaTolerance, rs, localMaxima);
		                            	ScaleSpace3D.analyzeMaximum(it2, conf.minPeakValue, width, height, depth, sigma[stepLocal - 2 - (1 - 2)], conf.identityRadius, conf.maximaTolerance, rs, localMaxima);            			            		
		                    		}
	
		                    		if ( conf.detectSmallestStructures && ScaleSpace3D.isSpecialPoint(it0, it1, conf.minInitialPeakValue) )
		                    		{
		                            	ScaleSpace3D.analyzeMaximum(it0, conf.minPeakValue, width, height, depth, sigma[stepLocal - 2 - (1 - 0)], conf.identityRadius, conf.maximaTolerance, rs, localMaxima );
		                            	ScaleSpace3D.analyzeMaximum(it1, conf.minPeakValue, width, height, depth, sigma[stepLocal - 2 - (1 - 1)], conf.identityRadius, conf.maximaTolerance, rs, localMaxima );            			
		                    		}
		                    	}
	                    	}
	                    }
	                    
	                    it0.close();
	                    it1.close();
	                    it2.close();
	                }
	            });
			
				MultiThreading.startAndJoin( threads );
				
				// summarize RejectStatistics and LocalMaxima
				for (int ithread = 0; ithread < threads.length; ++ithread)
				{
					rsFinal.imaginaryEigenValues += rsList[ ithread ].imaginaryEigenValues;
					rsFinal.noInverseOfHessianMatrix += rsList[ ithread ].noInverseOfHessianMatrix;
					rsFinal.noStableMaxima += rsList[ ithread ].noStableMaxima;
					rsFinal.notHighestValueInIdentityRadius += rsList[ ithread ].notHighestValueInIdentityRadius;
					rsFinal.peakTooLow += rsList[ ithread ].peakTooLow;
					rsFinal.tooHighEigenValueRatio += rsList[ ithread ].tooHighEigenValueRatio;
					
					IOFunctions.println( localMaximaList[ ithread ].size() );
					
					for ( DoGMaximum dogM : localMaximaList[ ithread ] )
						localMaximaFinal.add( dogM );
					
					IOFunctions.println( localMaximaFinal.size() );
				}
				

				// now we check that this maxima is unique in its identity radius.
				// if there are maxima which are smaller they get removed

	            IOFunctions.println( step + ": Check Maxima " + new Date(System.currentTimeMillis()));
				
				localMaximaFinal = ScaleSpace3D.checkMaximaXTree( localMaximaFinal, conf.identityRadius );
            
				IOFunctions.println(step + ": Finished Analyze Maxima " + new Date(System.currentTimeMillis()));
        }
        
        // close the used image datastructures
        for (int i = 0; i < octaveSteps.length; i++)
        {
        	octaveSteps[i].close();
        	octaveSteps[i] = null;        	
        }

        for (int i = 0; i < laPlace.length; i++)
        {
        	laPlace[i].close();
        	laPlace[i] = null;
        }

        IOFunctions.println("noStableMaxima: " + rsFinal.noStableMaxima);
        IOFunctions.println("tooHighEigenValueRatio: " + rsFinal.tooHighEigenValueRatio);
        IOFunctions.println("noInverseOfHessianMatrix: " + rsFinal.noInverseOfHessianMatrix); 
        IOFunctions.println("peakTooLow: " + rsFinal.peakTooLow);
        IOFunctions.println("imaginaryEigenValues: " + rsFinal.imaginaryEigenValues);
        IOFunctions.println("\nfound peaks: " + localMaximaFinal.size());
		
        BeadStructure beads = new BeadStructure();
        //ArrayList<ComponentProperties> beads = new ArrayList<ComponentProperties>();        
        int id = 0;
        
        for (Iterator <DoGMaximum>i = localMaximaFinal.iterator(); i.hasNext(); )
        {
        	DoGMaximum maximum = i.next();
        	Bead bead = new Bead( id, new Point3d(maximum.x + maximum.xd, maximum.y + maximum.yd, maximum.z + maximum.zd), view );
        	beads.addBead( bead );
        	id++;
        	
        	//ComponentProperties prop = new ComponentProperties();
                
        	//prop.center = new Point3d(maximum.x + maximum.xd, maximum.y + maximum.yd, maximum.z + maximum.zd);
        	//prop.label = label++;        	
        	//beads.add(prop);
        }
        
		return beads;
	}
	*/
	
	protected BeadStructure extractBeadsLaPlace( final ViewDataBeads view, final SPIMConfiguration conf )
	{
    	final RejectStatistics rsFinal = new RejectStatistics();
        ArrayList<DoGMaximum> localMaximaFinal = new ArrayList<DoGMaximum>();
        
        final Image<FloatType> img = view.getImage();
        
		final int width = img.getDimension( 0 );
		final int height = img.getDimension( 1 );
		final int depth = img.getDimension( 2 );
        
        // found local maxima
       
        // start with sigma = 1,6
        // assume it to be 0,5 in the original image
        // d(sigma) = sqrt(sigmaB^2 - sigmaA^2)
        		
        float imageSigma = conf.imageSigma;
        float initialSigma = conf.initialSigma;

        final float minPeakValue;
        final float minInitialPeakValue;

        // adjust for 12bit images
        if ( view.getMaxValueUnnormed() > 256 )
        {
        	minPeakValue = conf.minPeakValue/3;
        	minInitialPeakValue = conf.minInitialPeakValue/3;
        }
        else
        {
            minPeakValue = conf.minPeakValue;
            minInitialPeakValue = conf.minInitialPeakValue;        	
        }        

        final float k = LaPlaceFunctions.computeK(conf.stepsPerOctave);
        final float K_MIN1_INV = LaPlaceFunctions.computeKWeight(k);
        final int steps = conf.steps;

        //
        // Compute the Sigmas for the gaussian folding
        //
        final float[] sigma = LaPlaceFunctions.computeSigma(steps, k, initialSigma);
        final float[] sigmaDiff = LaPlaceFunctions.computeSigmaDiff(sigma, imageSigma);
              
        //
        // Now initially fold with gaussian kernel to get to sigma = 1.6
        //
        final GaussianConvolution<FloatType> conv1 = new GaussianConvolution<FloatType>( img, conf.strategyFactoryGauss, sigmaDiff[0] );
        final GaussianConvolution<FloatType> conv2 = new GaussianConvolution<FloatType>( img, conf.strategyFactoryGauss, sigmaDiff[1] );
        
        final Image<FloatType> gauss1, gauss2;
        
        if ( conv1.checkInput() && conv2.checkInput() )
        {
        	int numThreads = conf.numberOfThreads / 2;
        	if ( numThreads < 1 )
        		numThreads = 1;
        	
        	conv1.setNumThreads( numThreads );
        	conv2.setNumThreads( numThreads );
        	
            final AtomicInteger ai = new AtomicInteger(0);					
            Thread[] threads = SimpleMultiThreading.newThreads( 2 );

        	for (int ithread = 0; ithread < threads.length; ++ithread)
                threads[ithread] = new Thread(new Runnable()
                {
                    public void run()
                    {
                    	final int myNumber = ai.getAndIncrement();
                    	if ( myNumber == 0 )
                    	{
                    		if ( !conv1.process() )
                    		{
                            	if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
                            		IOFunctions.println( "Cannot compute gaussian convolution 1: " + conv1.getErrorMessage() );
                    		}
                    		
                    	}
                    	else
                    	{
                    		if ( !conv2.process() )
                    		{
                            	if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
                            		IOFunctions.println( "Cannot compute gaussian convolution 2: " + conv2.getErrorMessage() );
                    		}
                    		
                    	}                    	
                    }
                });
        	
    		SimpleMultiThreading.startAndJoin( threads );       	
        }
        else
        {
        	if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
        		IOFunctions.println( "Cannot compute gaussian convolutions: " + conv1.getErrorMessage() + " & " + conv2.getErrorMessage() );
        	
        	gauss1 = gauss2 = null;
        	return null;
        }
        
        if ( conv1.getErrorMessage().length() == 0 && conv2.getErrorMessage().length() == 0 )
        {
	        gauss1 = conv1.getResult();
	        gauss2 = conv2.getResult();
        }
        else
        {
        	gauss1 = gauss2 = null;
        	return null;        	
        }
        
        LaPlaceFunctions.subtractImagesInPlace( gauss2, gauss1, K_MIN1_INV );
        gauss1.close();
        
	    // Fit quadratic function and reject points which do not fit
	    // And also compute the principle curvatures which are
	    //
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
			IOFunctions.println( new Date(System.currentTimeMillis()) + ": Analyze Maxima " + new Date(System.currentTimeMillis()));

	    final AtomicInteger ai = new AtomicInteger(0);					
	    Thread[] threads = SimpleMultiThreading.newThreads( conf.numberOfThreads );
	    final int numThreads = threads.length;
	    
	    final RejectStatistics rsList[] = new RejectStatistics[ numThreads ];
		
	    @SuppressWarnings("unchecked")
	    final ArrayList<DoGMaximum> localMaximaList[] = new ArrayList[ numThreads ];


		for (int ithread = 0; ithread < threads.length; ++ithread)
	        threads[ithread] = new Thread(new Runnable()
	        {
	            public void run()
	            {
	            	final int myNumber = ai.getAndIncrement();
            	
	            	rsList[ myNumber ] = new RejectStatistics();
	            	localMaximaList[ myNumber ] = new ArrayList<DoGMaximum>();
	            	
	            	final RejectStatistics rs = rsList[ myNumber ];
	            	final ArrayList<DoGMaximum> localMaxima = (ArrayList<DoGMaximum>) localMaximaList[ myNumber ];
	
	            	final LocalizableByDimCursor3D<FloatType> cursor = (LocalizableByDimCursor3D<FloatType>) gauss2.createLocalizableByDimCursor();
	                
	                while ( cursor.hasNext() )
	                {
	                	cursor.fwd();
	                	
	                	final int x = cursor.getX();
	                	final int y = cursor.getY();
	                	final int z = cursor.getZ();
	                	
	                	if ( z % numThreads == myNumber )
	                	{
	                    	if ( x > 0 && y > 0 && z > 0 && 
	                    		 x < width - 1 && y < height - 1 && z < depth - 1 )
	                    	{
	                    		if ( LaPlaceFunctions.isSpecialPointMin( cursor , minInitialPeakValue) )
	                    		{
	                    			LaPlaceFunctions.analyzeMaximum( cursor, minPeakValue, width, height, depth, conf.initialSigma, conf.identityRadius, conf.maximaTolerance, rs, localMaxima );
	                    		}
	                    	}
	                	}
	                }
                
	                cursor.close();
            }
        });
	
		SimpleMultiThreading.startAndJoin( threads );				

		gauss2.close();
		
		// now we check that this maxima is unique in its identity radius.
		// if there are maxima which are smaller they get removed
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
			IOFunctions.println( "Check Maxima " + new Date(System.currentTimeMillis()));		
		
		localMaximaFinal = LaPlaceFunctions.checkMaximaXTree( localMaximaFinal, conf.identityRadius );    
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
			IOFunctions.println("Finished Analyze Maxima " + new Date(System.currentTimeMillis()));

		
		// summarize RejectStatistics and LocalMaxima
		for (int ithread = 0; ithread < threads.length; ++ithread)
		{
			rsFinal.imaginaryEigenValues += rsList[ ithread ].imaginaryEigenValues;
			rsFinal.noInverseOfHessianMatrix += rsList[ ithread ].noInverseOfHessianMatrix;
			rsFinal.noStableMaxima += rsList[ ithread ].noStableMaxima;
			rsFinal.notHighestValueInIdentityRadius += rsList[ ithread ].notHighestValueInIdentityRadius;
			rsFinal.peakTooLow += rsList[ ithread ].peakTooLow;
			rsFinal.tooHighEigenValueRatio += rsList[ ithread ].tooHighEigenValueRatio;
						
			for ( DoGMaximum dogM : localMaximaList[ ithread ] )
				localMaximaFinal.add( dogM );
		}
		
		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
		{
	        IOFunctions.println("noStableMaxima: " + rsFinal.noStableMaxima);
	        IOFunctions.println("tooHighEigenValueRatio: " + rsFinal.tooHighEigenValueRatio);
	        IOFunctions.println("noInverseOfHessianMatrix: " + rsFinal.noInverseOfHessianMatrix); 
	        IOFunctions.println("peakTooLow: " + rsFinal.peakTooLow);
	        IOFunctions.println("imaginaryEigenValues: " + rsFinal.imaginaryEigenValues);
		}

		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("Found peaks (possible beads): " + localMaximaFinal.size());

				
        final BeadStructure beads = new BeadStructure();
        int id = 0;
        
        for (Iterator <DoGMaximum>i = localMaximaFinal.iterator(); i.hasNext(); )
        {
        	DoGMaximum maximum = i.next();
        	Bead bead = new Bead( id, new Point3d(maximum.x + maximum.xd, maximum.y + maximum.yd, maximum.z + maximum.zd), view );
        	beads.addBead( bead );
        	id++;        	
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
		
		final int maxValue = (int) MathLib.round( img.getDisplay().getMax() );

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
			beads.addBead( bead );
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
