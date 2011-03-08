package mpicbg.spim.registration.bead;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.pointdescriptor.model.RigidModel3D;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.mpicbg.PointMatchGeneric;
import mpicbg.spim.mpicbg.TileConfigurationSPIM;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.error.GlobalErrorStatistics;
import mpicbg.spim.registration.detection.DetectionRegistration;
import mpicbg.spim.registration.detection.descriptor.CoordSystem3d;
import mpicbg.spim.registration.detection.descriptor.CorrespondenceExtraction;
import mpicbg.util.TransformUtils;

public class BeadRegistration
{
	final ViewStructure viewStructure;
	final static NumberFormat nf = NumberFormat.getPercentInstance();
	
	/**
	 * Instantiates the object with a certain {@link ViewStructure} it is working on when using non-static method calls
	 * @param viewStructure - the {@link ViewStructure} to work on
	 */
	public BeadRegistration( final ViewStructure viewStructure ) { this.viewStructure = viewStructure; }
	
	/**
	 * The {@link ViewStructure} this {@link BeadRegistration} is working with
	 * @return - the associated {@link ViewStructure}
	 */
	public ViewStructure getViewStructure() { return viewStructure; }

	/**
	 * Registers the current {@link ViewStructure}, see {@link BeadRegistration#registerViews(ViewStructure)} 
	 */
	public TileConfigurationSPIM registerViews() { return registerViews( viewStructure ); }

	/**
	 * Registers the current {@link ViewStructure}, see {@link BeadRegistration#registerViews(ViewStructure)} 
	 */
	public TileConfigurationSPIM registerViews( final boolean resetW ) { return registerViews( viewStructure, resetW ); }

	/**
	 * Registers the template {@link ViewStructure} to the {@link ViewStructure} of this {@link BeadRegistration} object, see {@link BeadRegistration#registerViewStructure(ViewStructure, ViewStructure)} 
	 */
	public void registerViewStructure( final ViewStructure template ) { registerViewStructure( viewStructure, template ); }

	/**
	 * Registers the {@link ViewDataBeads} of the template {@link ViewStructure} to the {@link ViewDataBeads} of the reference {@link ViewStructure}.
	 * All tiles of the reference {@link ViewStructure} are therefore fixed during the registration.
	 * The {@link SPIMConfiguration} and {@link GlobalErrorStatistics} are taken from the template {@link ViewStructure}, 
	 * the debuglevel is the highest of template and reference.
	 * 
	 * @param reference - the reference {@link ViewStructure}
	 * @param template - the template {@link ViewStructure}
	 */
	public static void registerViewStructure( final ViewStructure reference, final ViewStructure template )
	{	
		final int debugLevel = Math.min( reference.getDebugLevel(), template.getDebugLevel() );
		final GlobalErrorStatistics errorStatistics = template.getGlobalErrorStatistics();
		final SPIMConfiguration conf = template.getSPIMConfiguration();
		
		// we do not need to register if the reference time point is the same as the template time point 
		if ( reference.getTimePoint() == template.getTimePoint() )
		{
			for ( int view = 0; view < reference.getNumViews(); ++view )
			{
				final ViewDataBeads templateView = template.getViews().get( view );
				final ViewDataBeads referenceView = reference.getViews().get( view );
				
				templateView.getTile().getModel().set( referenceView.getTile().getModel() );
				for ( int i = 0; i < reference.getNumViews(); ++i )
					if ( i != view )
					templateView.getViewErrorStatistics().setViewConnected( template.getViews().get( i ), true );
			}
			
			if ( conf.writeRegistration )
				for ( ViewDataBeads view : template.getViews() )
					view.writeRegistrationTimeLapse( template.getTimePoint() );
			
			return;
		}
		
		// reset the affine models of the template (only if template does not equal reference)
		AffineModel3D m = new AffineModel3D();
		for ( final ViewDataBeads views : template.getViews() )
			views.getTile().getModel().set( m );
		
		//
		// compute the proper alignment
		//
		
		// Apply stretching and clear correspondences			 
		for ( final ViewDataBeads view : reference.getViews() )
		{
			if ( debugLevel <= ViewStructure.DEBUG_ALL )
				IOFunctions.println( view + ": " + (float)view.getZStretching() );

			for ( final Bead bead : view.getBeadStructure().getBeadList() )
			{
				bead.getL()[2] *= (float)view.getZStretching();					
				bead.getDescriptorCorrespondence().clear();
				bead.getRANSACCorrespondence().clear();
			}
		}
		
		// Apply stretching and clear correspondences			 
		for ( final ViewDataBeads view : template.getViews() )
		{
			if ( debugLevel <= ViewStructure.DEBUG_ALL )
				IOFunctions.println( view + ": " + (float)view.getZStretching() );

			for ( final Bead bead : view.getBeadStructure().getBeadList() )
			{
				bead.getL()[2] *= (float)view.getZStretching();					
				bead.getDescriptorCorrespondence().clear();
				bead.getRANSACCorrespondence().clear();
			}
		}
	
		//
		// Now we extract correspondences
		//
		final Vector <ViewDataBeads[]> comparePairs = new Vector<ViewDataBeads[]>();

		// for now all template views with each other
		for ( int viewIndexA = 0; viewIndexA < template.getNumViews() - 1; viewIndexA++ )
    		for ( int viewIndexB = viewIndexA + 1; viewIndexB < template.getNumViews(); viewIndexB++ )
    				comparePairs.add( new ViewDataBeads[]{ template.getViews().get( viewIndexA ), template.getViews().get( viewIndexB )} );
		
		// and all reference against all template views
		for ( int viewIndexA = 0; viewIndexA < reference.getNumViews(); viewIndexA++ )
    		for ( int viewIndexB = 0; viewIndexB < template.getNumViews(); viewIndexB++ )
    				comparePairs.add( new ViewDataBeads[]{ reference.getViews().get( viewIndexA ), template.getViews().get( viewIndexB )} );

		final AtomicInteger ai = new AtomicInteger(0);					
        Thread[] threads = SimpleMultiThreading.newThreads();
        final int numThreads = threads.length;

        for ( int ithread = 0; ithread < threads.length; ++ithread )
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                    final int myNumber = ai.getAndIncrement();
                    final CorrespondenceExtraction<Bead> ce = new CoordSystem3d<Bead>();
                    //final CorrespondenceExtraction<Bead> ce = new Quaternion3d<Bead>();
                    //final CorrespondenceExtraction<Bead> ce = new CoordSystemSecure3d<Bead>( 800 );
                    
                    for ( int i = 0; i < comparePairs.size(); i++ )
                    	if ( i%numThreads == myNumber )
                    	{
                    		final ViewDataBeads[] pair = comparePairs.get( i );
                    		final ViewDataBeads viewA = pair[ 0 ];
                    		final ViewDataBeads viewB = pair[ 1 ];
                    		
                    		// extract candidates
                    		if ( debugLevel <= ViewStructure.DEBUG_ALL )
                    			IOFunctions.println( viewA.getName() + "<->" + viewB.getName() +  ": Starting Correspondence Extraction, " +  viewA.getBeadStructure().getBeadList().size() + " <-> " + viewB.getBeadStructure().getBeadList().size() + " detection comparisons.");

                    		final ArrayList< PointMatchGeneric<Bead> > candidates = 
                    			ce.extractCorrespondenceCandidates( viewA.getBeadStructure().getBeadList(), viewB.getBeadStructure().getBeadList(), conf.differenceThreshold, conf.ratioOfDistance, conf.useAssociatedBeads );
                    		
                    		// compute ransac and remove inconsistent candidates
                    		final ArrayList< PointMatchGeneric<Bead> > correspondences = new ArrayList<PointMatchGeneric<Bead>>();
                    		final Model<?> model = viewA.getTile().getModel().copy();                    		
                    		
                    		String result = DetectionRegistration.computeRANSAC( candidates, correspondences, model, conf.max_epsilon, conf.min_inlier_ratio, 3, conf.numIterations );
                    		if ( debugLevel <= ViewStructure.DEBUG_MAIN )
                    			IOFunctions.println( viewA.getName() + "<->" + viewB.getName() +  ": " + result );                    		

                    		// update the error statistics
                    		synchronized ( errorStatistics ) { errorStatistics.setNumCandidates( errorStatistics.getNumCandidates() + candidates.size() ); }
                    		if ( correspondences.size() == 0 )
                    		{
                    			viewA.getViewErrorStatistics().resetViewSpecificError( viewB );
                    		}
                    		else
                    		{
                    			synchronized ( errorStatistics )
                    			{
                    				errorStatistics.setNumCorrespondences( errorStatistics.getNumCorrespondences() + correspondences.size() );
                    				errorStatistics.setAbsoluteLocalAlignmentError( errorStatistics.getAbsoluteLocalAlignmentError() + model.getCost() );
                    				errorStatistics.setAlignmentErrorCount( errorStatistics.getAlignmentErrorCount() + 1 );								
                    			}
                    			
                    			// this does not has to be synchronized as there is always one pair only which is unique
                    			if ( viewA.getViewStructure() == viewB.getViewStructure() )
                    			{
                    				viewA.getViewErrorStatistics().setViewSpecificError( viewB, model.getCost() );
                    				viewB.getViewErrorStatistics().setViewSpecificError( viewA, model.getCost() );
                    			}                    			
                    		}
                    		
                    		// add them to the tiles
                    		DetectionRegistration.addPointMatches( correspondences, viewA.getTile(), viewB.getTile() );	                    			                    		
                    	}
                }
            });
        SimpleMultiThreading.startAndJoin(threads);
        
        if ( debugLevel <= ViewStructure.DEBUG_MAIN )
        {
	        for ( ViewDataBeads view : reference.getViews() )
	        	IOFunctions.println( view + " has " + view.getViewErrorStatistics().getNumTrueCorrespondencePairs() + " correspondences in " + view.getViewErrorStatistics().getNumConnectedViews() + " other views.");

	        for ( ViewDataBeads view : template.getViews() )
	        	IOFunctions.println( view + " has " + view.getViewErrorStatistics().getNumTrueCorrespondencePairs() + " correspondences in " + view.getViewErrorStatistics().getNumConnectedViews() + " other views.");

	        IOFunctions.println( "The total number of detections was: " + errorStatistics.getNumDetections() );
	        IOFunctions.println( "The total number of true correspondences is: " + errorStatistics.getNumCorrespondences() );
	        IOFunctions.println( "The total number of correspondence candidates was: " + errorStatistics.getNumCandidates() );
	        
	        final float ratio = ((float)errorStatistics.getNumCorrespondences() / (float)errorStatistics.getNumCandidates());
			
	        IOFunctions.println( "The ratio is: " + nf.format( ratio ) );
        }
        
		// 
		// Now we optimize everything
		//
        final ArrayList<ViewDataBeads> views = new ArrayList<ViewDataBeads>();
        views.addAll( reference.getViews() );
        views.addAll( template.getViews() );
        
        // optimize the tiles but fix all the reference tiles
		optimizeTiles( views, reference.getNumViews(), errorStatistics, debugLevel );	
					
		// unapply stretching
		for ( final ViewDataBeads view : reference.getViews() )
		{
			for ( final Bead bead : view.getBeadStructure().getBeadList() )
			{
				bead.getL()[2] /= (float)view.getZStretching();
				bead.resetW();
			}
		}

		for ( final ViewDataBeads view : template.getViews() )
		{
			for ( final Bead bead : view.getBeadStructure().getBeadList() )
			{
				bead.getL()[2] /= (float)view.getZStretching();
				bead.resetW();
			}
		}
		
		// write the registration data
		if ( conf.writeRegistration )
			for ( ViewDataBeads view : template.getViews() )
				view.writeRegistrationTimeLapse( reference.getTimePoint() );
	}
		
	public static TileConfigurationSPIM registerViews( final ViewStructure viewStructure ) { return registerViews( viewStructure, true ); }
	
	/**
	 * Registers all {@link ViewDataBeads} within a given {@link ViewStructure}, taking the first view as fixed Tile
	 * Registration works without z-scaling, it has to be concatenated afterwards if wanted
	 * 
	 * @param viewStructure - The {@link ViewStructure} to register
	 */
	public static TileConfigurationSPIM registerViews( final ViewStructure viewStructure, final boolean resetW )
	{	
		final ArrayList<ViewDataBeads> views = viewStructure.getViews();
		final GlobalErrorStatistics errorStatistics = viewStructure.getGlobalErrorStatistics();
		final SPIMConfiguration conf = viewStructure.getSPIMConfiguration();
		
		// Apply stretching and clear correspondences			 
		for ( final ViewDataBeads view : views )
		{
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
				IOFunctions.println( view + ": " + (float)view.getZStretching() );

			for ( final Bead bead : view.getBeadStructure().getBeadList() )
			{
				bead.getL()[2] *= (float)view.getZStretching();					
				bead.getDescriptorCorrespondence().clear();
				bead.getRANSACCorrespondence().clear();
			}
		}

		//
		// Now we extract correspondences
		//
		final Vector <int[]> comparePairs = new Vector<int[]>();

		// count the number of detections ( possible candidates )
		for ( final ViewDataBeads view : views )
			errorStatistics.setNumDetections( errorStatistics.getNumDetections() + view.getViewErrorStatistics().getNumDetections() ); 

		// determine which view pairs to compare
		for ( int viewIndexA = 0; viewIndexA < views.size() - 1; viewIndexA++ )
    		for ( int viewIndexB = viewIndexA + 1; viewIndexB < views.size(); viewIndexB++ )
    				comparePairs.add( new int[]{viewIndexA, viewIndexB} );
		
		final AtomicInteger ai = new AtomicInteger(0);					
        Thread[] threads = SimpleMultiThreading.newThreads();
        final int numThreads = threads.length;

        for ( int ithread = 0; ithread < threads.length; ++ithread )
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	final int myNumber = ai.getAndIncrement();
                    final CorrespondenceExtraction<Bead> ce = new CoordSystem3d<Bead>();
                    //final CorrespondenceExtraction<Bead> ce = new ModelBased3d<Bead>( new SubsetMatcher( 3, 4 ) );
                    //final CorrespondenceExtraction<Bead> ce = new CoordSystemSecure3d<Bead>( 2 );
                    
                    for ( int i = 0; i < comparePairs.size(); i++ )
                    	if ( i%numThreads == myNumber )
                    	{
                    		final int[] pair = comparePairs.get( i );
                    		final ViewDataBeads viewA = views.get( pair[0] );
                    		final ViewDataBeads viewB = views.get( pair[1] );
                    		
                    		// extract candidates
                    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ALL )
                    			IOFunctions.println( viewA.getName() + "<->" + viewB.getName() +  ": Starting Correspondence Extraction, " +  viewA.getBeadStructure().getBeadList().size() + " <-> " + viewB.getBeadStructure().getBeadList().size() + " detection comparisons.");
                    		
                    		final ArrayList< PointMatchGeneric<Bead> > candidates = 
                    			ce.extractCorrespondenceCandidates( viewA.getBeadStructure().getBeadList(), viewB.getBeadStructure().getBeadList(), conf.differenceThreshold, conf.ratioOfDistance, conf.useAssociatedBeads );
                    		
                    		// compute ransac and remove inconsistent candidates
                    		final ArrayList< PointMatchGeneric<Bead> > correspondences = new ArrayList<PointMatchGeneric<Bead>>();
                    		final Model<?> model = viewA.getTile().getModel().copy();                    		
                    		
                    		String result = DetectionRegistration.computeRANSAC( candidates, correspondences, model, conf.max_epsilon, conf.min_inlier_ratio, 3, conf.numIterations );
                    		if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
                    			IOFunctions.println( viewA.getName() + "<->" + viewB.getName() +  ": " + result );                    		

                    		// update the error statistics
                    		synchronized ( errorStatistics ) { errorStatistics.setNumCandidates( errorStatistics.getNumCandidates() + candidates.size() ); }
                    		if ( correspondences.size() == 0 )
                    		{
                    			viewA.getViewErrorStatistics().resetViewSpecificError( viewB );
                    		}
                    		else
                    		{
                    			synchronized ( errorStatistics )
                    			{
                    				errorStatistics.setNumCorrespondences( errorStatistics.getNumCorrespondences() + correspondences.size() );
                    				errorStatistics.setAbsoluteLocalAlignmentError( errorStatistics.getAbsoluteLocalAlignmentError() + model.getCost() );
                    				errorStatistics.setAlignmentErrorCount( errorStatistics.getAlignmentErrorCount() + 1 );								
                    			}
                    			
                    			// this does not has to be synchronized as there is always one pair only which is unique
                    			if ( viewA.getViewStructure() == viewB.getViewStructure() )
                    			{
                    				viewA.getViewErrorStatistics().setViewSpecificError( viewB, model.getCost() );
                    				viewB.getViewErrorStatistics().setViewSpecificError( viewA, model.getCost() );
                    			}                    			
                    		}
                    		
                    		// add them to the tiles
                    		DetectionRegistration.addPointMatches( correspondences, viewA.getTile(), viewB.getTile() );	    
                    	}
                }
            });
        SimpleMultiThreading.startAndJoin(threads);
        
        if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
        {
	        for ( ViewDataBeads view : views )
	        	IOFunctions.println( view + " has " + view.getViewErrorStatistics().getNumTrueCorrespondencePairs() + " correspondences in " + view.getViewErrorStatistics().getNumConnectedViews() + " other views.");
	        
	        IOFunctions.println( "The total number of detections was: " + errorStatistics.getNumDetections() );
	        IOFunctions.println( "The total number of correspondence candidates was: " + errorStatistics.getNumCandidates() );
	        IOFunctions.println( "The total number of true correspondences is: " + errorStatistics.getNumCorrespondences() );
        }
        
        //Rigid-body
        //NucleiRegistration.optimizeTiles(rigidTileList);
        //System.exit( 0 );
        
		// 
		// Now we optimize everything
		//
        TileConfigurationSPIM tc = optimizeTiles( views, errorStatistics, viewStructure.getDebugLevel() );	
					
		// unapply stretching
		for ( ViewDataBeads view : views )
		{
			for ( final Bead bead : view.getBeadStructure().getBeadList() )
			{
				bead.getL()[2] /= (float)view.getZStretching();
				if ( resetW )
					bead.resetW();
			}
		}
		
		if ( conf.writeRegistration )
		{
			for ( ViewDataBeads view : views )
			{
				// write bead data including correspondences
				view.writeSegmentation();
				
				// write the registration data
				view.writeRegistration();
			}
		}
		
		return tc;
	}
	
	public static void computeErrors( final ArrayList<ViewDataBeads> views, final TileConfigurationSPIM tc )
	{
		tc.computeError();		
	}
	
	public static TileConfigurationSPIM optimizeTiles( final ArrayList<ViewDataBeads> views, final GlobalErrorStatistics errorStatistics, final int debugLevel )
	{
		return optimizeTiles( views, 1, errorStatistics, debugLevel );
	}

	public static TileConfigurationSPIM optimizeTiles( final ArrayList<ViewDataBeads> views, final int numFixed, final GlobalErrorStatistics errorStatistics, final int debugLevel )
	{		
		final TileConfigurationSPIM tc = new TileConfigurationSPIM( debugLevel );
		
		int fixedTiles = 0;
		
		for ( final ViewDataBeads view : views )
		{
			if (view.getTile().getConnectedTiles().size() > 0)
			{
				tc.addTile(view.getTile());
				if ( fixedTiles < numFixed )
				{					
					if ( debugLevel <= ViewStructure.DEBUG_MAIN )
						IOFunctions.println( "Fixing tile " + view );
					
					tc.fixTile(view.getTile());
					fixedTiles++;
				}
			}
		}			
		
		try
		{
			if ( views.get( 0 ).getViewStructure().getSPIMConfiguration().displayRegistration )
				tc.optimizeWith3DViewer( 10, 10000, 200, views, debugLevel );
			else
				tc.optimize( 10, 10000, 200, debugLevel );
			
			//tc.optimizeWithSketchTikZ( 10, 10000, 200, debugLevel );
			//tc.optimizeWithSketchTikZNuclei( 10, 10000, 200, debugLevel );
			
			/*tc.optimizeWithErrorAnalysis( 10, 10000, 200, conf, showDetails );
			ArrayList<Double> distances = tc.getDistances();
			ArrayList<Double> xd = tc.getXDistances();
			ArrayList<Double> yd = tc.getYDistances();
			ArrayList<Double> zd = tc.getZDistances();
			
			PrintWriter out = fileAccess.openFileWrite( conf.inputdirectory + "distances.txt");			
			for ( Double distance : distances )
				out.println( distance );
			out.close();

			out = fileAccess.openFileWrite( conf.inputdirectory + "xyzdistances.txt");
			out.println( "x" + "\t" + "y" + "\t" + "z" );
			
			for ( int i = 0; i < xd.size(); i++ )
				out.println( xd.get(i) + "\t" + yd.get(i) + "\t" + zd.get(i) );
			out.close();
			*/
			
			/*
			for ( final ViewDataBeads view : views )
			{
				final double maxDistanceCorrespondence = PointMatch.maxDistance( view.getTile().getMatches() );
				final double avgDistanceCorrespondence = view.getTile().getDistance();
				
				IOFunctions.println( view.getName() + " " + avgDistanceCorrespondence + " (" + maxDistanceCorrespondence + ")" );
			}
			*/
			
			errorStatistics.setAverageAlignmentError( tc.getError() );
			errorStatistics.setMinAlignmentError( tc.getMinError() );
			errorStatistics.setMaxAlignmentError( tc.getMaxError() );
		}
		catch ( NotEnoughDataPointsException e )
		{ 
			e.printStackTrace(); 
			errorStatistics.setAverageAlignmentError( Double.MAX_VALUE );
			errorStatistics.setMinAlignmentError( Double.MAX_VALUE );
			errorStatistics.setMaxAlignmentError( Double.MAX_VALUE );
		}
		catch ( IllDefinedDataPointsException e )
		{
			
			e.printStackTrace(); 
			errorStatistics.setAverageAlignmentError( Double.MAX_VALUE );
			errorStatistics.setMinAlignmentError( Double.MAX_VALUE );
			errorStatistics.setMaxAlignmentError( Double.MAX_VALUE );
		}

		if ( debugLevel <= ViewStructure.DEBUG_MAIN )
			IOFunctions.println("Optimizer Matrices");
		
		for ( final ViewDataBeads view : views )
		{
			if ( view.getTile().getConnectedTiles().size() > 0 )
			{				
				if ( debugLevel <= ViewStructure.DEBUG_MAIN )
				{
					IOFunctions.println( view + ":");
					IOFunctions.println( "Transformation:\n"+ view.getTile().getModel() );	
					
					Transform3D t = TransformUtils.getTransform3D1( (AbstractAffineModel3D<?>)view.getTile().getModel() );
					Vector3d s = new Vector3d();
					t.getScale( s );
					System.out.println( "Scaling: " + s );
					
					// TODO: Rigidmodel
					/*
					if ( view.getTile().getModel() instanceof RigidModel3D )
					{
						
					}
					*/
				}
			}
			else
			{
				for ( final ViewDataBeads otherView : views )
					view.getViewErrorStatistics().resetViewSpecificError( otherView );
				
				view.getBeadStructure().clearAllRANSACCorrespondences();
				
				if ( debugLevel <= ViewStructure.DEBUG_MAIN )
					IOFunctions.println( view + ": is not connected to any other tile!" );
			}
		}		
		
		return tc;
	}

	public static void concatenateAxialScaling( final AbstractAffineModel3D model, final double zStretching )
	{
		if ( model != null )
		{
			if ( model instanceof AffineModel3D )
			{
				final AffineModel3D tmpModel = new AffineModel3D();				
				final float z = (float)zStretching;
				
				tmpModel.set( 1f, 0f, 0f, 0f, 
				              0f, 1f, 0f, 0f,
				              0f, 0f, z,  0f );
				
				((AffineModel3D)model).concatenate( tmpModel );
			}
			else if ( model instanceof RigidModel3D )
			{
				final RigidModel3D tmpModel = new RigidModel3D();				
				final float z = (float)zStretching;
				
				final Transform3D t = new Transform3D();
				t.setScale( new Vector3d( 1, 1, z ) );				
				tmpModel.set( t );
				
				((RigidModel3D)model).concatenate( tmpModel );				
			}
			else
			{
				System.out.println( "Cannot concatenate Axial Scaling, unknown model!" );
			}
		}		
	}

	public static void concatenateAxialScaling( final ViewDataBeads view, final int debugLevel )
	{
		final AbstractAffineModel3D<?> m = (AbstractAffineModel3D<?>)view.getTile().getModel();
		
		if ( m != null )
		{
			concatenateAxialScaling( m, view.getZStretching() );
			
			/*
			final AffineModel3D tmpModel = new AffineModel3D();				
			final float z = (float)view.getZStretching();
			
			tmpModel.set( 1f, 0f, 0f, 0f, 
			              0f, 1f, 0f, 0f,
			              0f, 0f, z,  0f );
			
			m.concatenate( tmpModel );
			*/
			
			if ( debugLevel <= ViewStructure.DEBUG_ALL )
				IOFunctions.println( view + "(" + view.getZStretching() + "): " + m );
		}		
	}
    
	public static void concatenateAxialScaling( final ArrayList<ViewDataBeads> views, final int debugLevel )
	{
		if ( debugLevel <= ViewStructure.DEBUG_ALL )
			IOFunctions.println("----------------------Scaling corrected-------------------");
		
		//
		// update target registration
		//
		for ( final ViewDataBeads view : views)
			concatenateAxialScaling( view, debugLevel );		
	}	
}
