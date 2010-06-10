package mpicbg.spim;

import java.util.ArrayList;
import java.util.Date;

import mpicbg.models.AffineModel3D;
import mpicbg.models.Point;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.ConfigurationParserGeneral;
import mpicbg.spim.io.ConfigurationParserSPIM;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.ProgramConfiguration;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;
import mpicbg.spim.registration.bead.Bead;
import mpicbg.spim.registration.bead.BeadIdentification;
import mpicbg.spim.registration.bead.BeadRegistration;

public class Reconstruction
{
	final protected SPIMConfiguration conf;
	public SPIMConfiguration getSPIMConfiguration() { return conf; }
	
	public Reconstruction( final SPIMConfiguration conf )
	{
		this.conf = conf; 
		
		if ( conf.timeLapseRegistration )
			processTimeLapse( conf );
		else
			processIndividualViewStructure( conf );
	}
	
	protected void findAllRansacCorrespondencesRecurively( final Bead bead, ArrayList<Bead> correspondingBeadList )
	{
		if ( bead.isUsed() )
			return;
		
		correspondingBeadList.add( bead );
		bead.setUsed( true );
		
		final ArrayList<BeadIdentification> ransacList = bead.getRANSACCorrespondence();
		
		for ( final BeadIdentification correspondence : ransacList )
		{
			final Bead correspondingBead = correspondence.getBead();
			
			if ( !correspondingBead.isUsed() )
				findAllRansacCorrespondencesRecurively( correspondingBead, correspondingBeadList );
		}
	}
	
	protected ArrayList<Bead> getAveragedBeadList( final ArrayList<ViewDataBeads> views, final ViewDataBeads newView )
	{
		final ArrayList<Bead> averagedBeadList = new ArrayList<Bead>();
		BeadRegistration.concatenateAxialScaling( views, ViewStructure.DEBUG_ERRORONLY );
		
		for( ViewDataBeads view : views )
		{
			//IOFunctions.println( "Analyzing view " + view.shortName );
			
			final ArrayList<Bead> beadList = view.getBeadStructure().getBeadList();
			
			for ( final Bead bead : beadList )
			{
				final ArrayList<Bead> correspondingBeadList = new ArrayList<Bead>();								
				findAllRansacCorrespondencesRecurively( bead, correspondingBeadList );
				
				// if we find more than one bead we average their positions
				if ( correspondingBeadList.size() > 1 )
				{
					Point location = new Point( new float[]{0,0,0} );
					
					for ( final Bead correspondingBead : correspondingBeadList )
					{
						correspondingBead.apply( correspondingBead.getView().getTile().getModel() );

						location.getW()[ 0 ] += correspondingBead.getW()[ 0 ]; 
						location.getW()[ 1 ] += correspondingBead.getW()[ 1 ]; 
						location.getW()[ 2 ] += correspondingBead.getW()[ 2 ]; 
						//IOFunctions.println( correspondingBead );
					}	
					
					location.getW()[ 0 ] /= (float)correspondingBeadList.size();
					location.getW()[ 1 ] /= (float)correspondingBeadList.size();
					location.getW()[ 2 ] /= (float)correspondingBeadList.size();
					
					averagedBeadList.add( new Bead( averagedBeadList.size(), location.getW().clone(), newView ) );
					
					//IOFunctions.println( location );
					//IOFunctions.println();
				}
			}							
		}

		// clean up
		for( ViewDataBeads view : views )
			for ( final Bead bead : view.getBeadStructure().getBeadList() )
				bead.setUsed( false );

		return averagedBeadList;
	}

	protected void processTimeLapse( SPIMConfiguration conf )
	{
		for (int timePointIndex = 0; timePointIndex < conf.file.length; timePointIndex++)
		{
			final ViewStructure reference = ViewStructure.initViewStructure( conf, conf.referenceTimePoint, conf.getFileName( conf.referenceTimePoint ), new AffineModel3D(), "Reference ViewStructure Timepoint " + conf.referenceTimePoint, conf.debugLevelInt );
			final ViewStructure template = ViewStructure.initViewStructure( conf, timePointIndex, new AffineModel3D(), "Template ViewStructure Timepoint " + conf.timepoints[timePointIndex], conf.debugLevelInt );
			
			//
			// get timepoint information
			//
			if ( reference.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Loading reference timepoint information " + reference);
			
			reference.loadDimensions();
			reference.loadSegmentations();
			reference.loadRegistrations();

			if ( reference.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
				IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Loading timepoint " + template + " information");
			
			template.loadDimensions();
			template.loadSegmentations();
			
			// update the ids of the template timepoint so that there are no identical ids
			int id = reference.getViews().get( 0 ).getID();
			for ( ViewDataBeads view : reference.getViews() )
				if ( view.getID() >= id )
					id = view.getID() + 1;
			
			for ( ViewDataBeads view : template.getViews() )
			{
				view.setID( id++ );
				if ( reference.getDebugLevel() <= ViewStructure.DEBUG_ALL )
					IOFunctions.println( view + ", updated id" );
			}
			
			boolean readReg = conf.readRegistration;
			
			if ( readReg )
				for ( ViewDataBeads view : template.getViews() )
				{
					readReg &= view.loadRegistrationTimePoint( reference.getTimePoint() );
					if ( !readReg )
						break;
				}
				
			if ( !readReg )
				reference.getBeadRegistration().registerViewStructure( template );			

			BeadRegistration.concatenateAxialScaling( reference.getViews(), reference.getDebugLevel() );
			BeadRegistration.concatenateAxialScaling( template.getViews(), template.getDebugLevel() );
			
	        //
	        // remove the beads
	        //        
	       // for ( final ViewDataBeads view : template.getViews() )
	        //	RemoveBeads.removeBeads( view );
			
	        //
	        // add an external transformation
	        //
	        addExternalTransformation( template );
        	addExternalTransformation( reference );
			
			if (!conf.registerOnly)
			{
				IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Fusion for timepoint " + conf.timepoints[timePointIndex]);		
				
				template.getFusionControl().fuse( template, reference, template.getTimePoint() );
				
				IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Finished Fusion for timepoint " + conf.timepoints[timePointIndex]);
			}
			
			//
			// close all images
			//
			for ( ViewDataBeads view : template.getViews() )
				view.closeImage();
		}		
	}  	
	
	protected void processIndividualViewStructure( SPIMConfiguration conf )
	{
		for (int timePointIndex = 0; timePointIndex < conf.file.length; timePointIndex++)
		{
			final ViewStructure viewStructure = ViewStructure.initViewStructure( conf, timePointIndex, new AffineModel3D(), "ViewStructure Timepoint " + timePointIndex, conf.debugLevelInt );						
			
			//
			// Segmentation
			//
	        if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
	        	IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Bead Extraction");
			
			boolean dimSuccess = false, segSuccess = false;
			
			if ( viewStructure.getSPIMConfiguration().readSegmentation )
			{
				dimSuccess = viewStructure.loadDimensions();
				
				if ( !dimSuccess )
				{
			        if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
			        	IOFunctions.println("Cannot find files for " + viewStructure );
			        
					System.exit(0);
				}
				
				segSuccess = viewStructure.loadSegmentations();
			}
			
			if ( !segSuccess )
				viewStructure.getBeadSegmentation().segment();
			
			/*
			Image<FloatType> img = viewStructure.getBeadSegmentation().getFoundBeads( viewStructure.getViews().get( 0) );
			img.getDisplay().setMinMax();
			ImageJFunctions.copyToImagePlus(img).show();
			*/
			
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
	        	IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Finished Bead Extraction");					
	        
			//
			// Registration
			//			
	        if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
	        	IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Registration");

	        boolean regSuccess = false;
	        
			if ( viewStructure.getSPIMConfiguration().readRegistration )
				regSuccess = viewStructure.loadRegistrations();

			if ( !regSuccess )
				viewStructure.getBeadRegistration().registerViews();

			BeadRegistration.concatenateAxialScaling( viewStructure.getViews(), viewStructure.getDebugLevel() );
	        
	        if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_MAIN )
	        	IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Finished Registration");
					        

	        //
	        // remove the beads
	        //        
	        //for ( final ViewDataBeads view : viewStructure.getViews() )
	        	//RemoveBeads.removeBeads( view );
	        
			//
			// Predisplay result
			//
			
	        //if ( conf.showOutputImage )
	        	//new PreviewRegistration( viewStructure );
	        
	        //
	        // add an external transformation
	        //
	        addExternalTransformation( viewStructure );
	        
			//
			// Fusion
			//			
			if (!conf.registerOnly)
			{
				IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Starting Fusion");
				
				viewStructure.getFusionControl().fuse( viewStructure, conf.timepoints[timePointIndex] );
				
				IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Finished Fusion");
			}	

			//
			// close all images
			//
			for ( ViewDataBeads view : viewStructure.getViews() )
				view.closeImage();
			
			IOFunctions.println( "Finished processing." );
		}
	}
	
	protected void addExternalTransformation( final ViewStructure viewStructure )
	{
		/*
		final AffineModel3D model = new AffineModel3D();
		// 5 angle
		//model.set( 0.87249345f, -0.48363087f, -0.06975689f, 0.0f, 0.48480982f, 0.87462026f, 0.0f, 0.0f, 0.06101087f, -0.033818856f, 0.9975669f, 0.0f );
		
		// 6 angle red beads
		model.set( 0.78610414f, 0.38340846f, 0.4848098f, 0.0f, -0.43837133f, 0.89879465f, 0.0f, 0.0f, -0.43574473f, -0.2125268f, 0.87462026f, 0.0f );
		
		for ( final ViewDataBeads view : viewStructure.getViews() )
		{
			final AffineModel3D viewModel = view.getTile().getModel();
			viewModel.preConcatenate( model );
		}
		*/
	}
	
    public static void main( String[] args )
    {    	
        SPIMConfiguration config = null;
        
        try 
        {
        	String temp;
        	
        	if ( args == null || args.length == 0 || args[0].trim().length() < 1 )
        		temp = "spimconfig/configuration.txt";
        	else
        		temp = args[0].trim();
        	
			config = ConfigurationParserSPIM.parseFile( temp );
			
			if ( config.debugLevelInt <= ViewStructure.DEBUG_ALL )
				config.printProperties();
		} 
        catch ( ConfigurationParserException e ) 
        {
        	IOFunctions.println( "Cannot open SPIM configuration file: \n" + e );
        	e.printStackTrace();
        	return;
		}
    	
    	if ( config.showImageJWindow )
    	{
			// read&parse configuration file
			ProgramConfiguration conf = null;
			
			try
			{
				conf = ConfigurationParserGeneral.parseFile( "config/configuration.txt" );
			} 
			catch (final Exception e)
			{
				IOFunctions.println( "Cannot open configuration file: \n" + e );
			}
	
			// open imageJ window
			if ( conf != null )
			{
				System.getProperties().setProperty( "plugins.dir", conf.pluginsDir );
				final String params[] = { "-ijpath " + conf.pluginsDir };
				
				// call the imageJ main class
				ij.ImageJ.main( params );
			}
			else
			{
				final String params[] = { "-ijpath ." };
				
				// call the imageJ main class
				ij.ImageJ.main( params );
			}
    	}
    	
        new Reconstruction( config );
    }        	
}
