package mpicbg.spim.registration;

import java.util.ArrayList;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.io.LOCI;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.spim.fusion.FusionControl;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.bead.BeadRegistration;
import mpicbg.spim.registration.bead.BeadSegmentation;
import mpicbg.spim.registration.bead.error.GlobalErrorStatistics;
import mpicbg.spim.registration.bead.error.GlobalErrorStatisticsImpl;

/**
 * This class stores the links to all Views and initializes the View-dependent error statistics
 * 
 * @author Stephan Preibisch
 *
 */
public class ViewStructure 
{
	final public static int DEBUG_ALL = 0;
	final public static int DEBUG_MAIN = 1;
	final public static int DEBUG_ERRORONLY = 2;
	
	public static int firstID = 0;

	/**
	 * The link to the Views
	 */
	final protected ArrayList<ViewDataBeads> views;
	
	/**
	 * The time point of the current view constellation
	 */
	final protected int timePoint;
	
	/**
	 * The number of channels in this view constellation
	 */
	final protected int numChannels;
	
	/**
	 * Arbitrary String for identification
	 */
	final protected String identification;
	
	/**
	 * The debug level of this view constellation
	 */
	protected int debugLevel;
	
	/**
	 * The configuration of the current SPIM registration
	 */
	final protected SPIMConfiguration conf;
	
	/**
	 * The global error statistics of this view constellation
	 */
	protected GlobalErrorStatistics errorStatistics;
	
	/**
	 * The object that handles segmentation
	 */
	protected BeadSegmentation beadSegment;

	/**
	 * The object that handles registration
	 */
	protected BeadRegistration beadRegister;

	/**
	 * The object that handles fusion
	 */
	protected FusionControl fusionControl;
	
	public ViewStructure( final ArrayList<ViewDataBeads> views, final SPIMConfiguration conf, final String id, final int timePoint, final int numChannels )
	{
		this( views, conf, id, timePoint, numChannels, ViewStructure.DEBUG_MAIN );
	}
	
	public ViewStructure( final ArrayList<ViewDataBeads> views, final SPIMConfiguration conf, final String id, final int timePoint, final int numChannels, final int debugLevel )
	{
		this.views = views;
		this.identification = id;
		this.conf = conf;
		this.timePoint = timePoint;
		this.numChannels = numChannels;
		setDebugLevel( debugLevel );
				
		this.errorStatistics = new GlobalErrorStatisticsImpl();
		
		this.beadSegment = new BeadSegmentation( this );
		
		for ( final ViewDataBeads view : views )
			view.setViewStructure( this );

		for ( final ViewDataBeads view : views )
			view.initErrorStatistics();
		
		beadSegment = new BeadSegmentation( this );
		beadRegister = new BeadRegistration( this );
		fusionControl = new FusionControl( );				
	}
	
	public BeadSegmentation getBeadSegmentation() { return beadSegment; }
	public BeadRegistration getBeadRegistration() { return beadRegister; }
	public FusionControl getFusionControl() { return fusionControl; }

	/**
	 * The number of views in this view collection
	 * @return the number of views
	 */
	public int getNumViews() { return getViews().size(); }

	/**
	 * The number of channels in this view collection
	 * @return the number of channels
	 */
	public int getNumChannels() { return numChannels; }

	/**
	 * Returns the channel id for a certain channelIndex
	 * @param channelIndex
	 * @return the naming of the channel
	 */
	public int getChannelNum( final int channelIndex )
	{
		for ( final ViewDataBeads view : getViews() )
			if ( view.getChannelIndex() == channelIndex )
				return view.getChannel();
		
		return -1;
	}
	
	/**
	 * The {@link GlobalErrorStatistics} collects error details of the registration prodedure 
	 * @return The {@link GlobalErrorStatistics} object of this {@link ViewStructure} 
	 */
	public GlobalErrorStatistics getGlobalErrorStatistics() { return errorStatistics; }
	
	/**
	 * Sets a new object that collects error details of the registration prodedure
	 * @param errorStatistics - The {@link GlobalErrorStatistics} object of this {@link ViewStructure}
	 */
	public void setGlobalErrorStatistics( final GlobalErrorStatistics errorStatistics ) { this.errorStatistics = errorStatistics; }
	
	/**
	 * DebugLevel of this view constellation
	 * @return the current debug level
	 */
	public int getDebugLevel () { return debugLevel; }
	
	/**
	 * DebugLevel of this view constellation
	 * @param debugLevel - the new debug level of this view constellation
	 */
	public void setDebugLevel ( final int debugLevel ) { this.debugLevel = debugLevel; }
	
	/**
	 * The time point index of the current {@link ViewStructure}
	 * @return the id
	 */
	public int getTimePoint() { return timePoint; }
	
	/**
	 * Return the current SPIMConfiguration
	 * @return current {@link SPIMConfiguration} object
	 */
	public SPIMConfiguration getSPIMConfiguration() { return conf; }
	
	@Override
	public String toString() { return identification; }
	
	/**
	 * get the identification String of the ViewStructure
	 * @return String ID
	 */
	public String getID() { return identification; }
	
	/**
	 * Get the views
	 * 
	 * @return ArrayList containing all Views
	 */
	public ArrayList<ViewDataBeads> getViews() { return views; }
	
	/**
	 * For getting a View with a certain ID
	 * 
	 * @param viewID - the ID of the wanted View
	 * @return the View with the respective id or null if it does not exist
	 */
	public ViewDataBeads getViewFromID( final int viewID )
	{
		if ( viewID < views.size() )
			if ( views.get( viewID ).getID() == viewID )
				return views.get( viewID );
		
		for ( final ViewDataBeads view : views )
			if ( view.getID() == viewID )
				return view;
		
		//IOFunctions.println( "ViewStructure.getView( " + viewID + " ): View not part of this ViewStructure" );
		return null;
	}
	
	/**
	 * Gets the number of dimensions from view 1
	 * @return - the numnber of dimensions
	 */
	public int getNumDimensions() { return views.get( 0 ).getImageSize().length; }

	/**
	 * Loads the bead detections and its correspondences candidates as well as true correspondences from the *.beads.txt files in the registration file directory
	 * 
	 * @return true if successful, false otherwise
	 */
	public boolean loadSegmentations()
	{
		for ( final ViewDataBeads view : getViews() )
		{
			// only load those who are needed for registration
			if ( view.getUseForRegistration() )
			{
				boolean readSeg = IOFunctions.readSegmentation( view, conf.registrationFiledirectory, conf );
							
				if (!readSeg)
				{
					if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
						IOFunctions.printErr("Cannot read segmentation for " + view );
					
					return false;
				}		
			}
		}
		
		return true;
	}
	
	/**
	 * Loads the registration matrix and the errors from the *.registration files in the registration file directory
	 * 
	 * @return true if successful, false otherwise
	 */
	public boolean loadRegistrations()
	{
		boolean success = true;
		
		for ( final ViewDataBeads view : getViews() )
		{
			boolean readReg = view.loadRegistration();

			if (!readReg)
			{
				if ( getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
					IOFunctions.printErr( "Cannot read registration for view " + view + " in " + this );
				success = false;
			}
		}
		
		return success;		
	}
	
	/**
	 * Loads the dimensions from the *.dim files in the registration file directory
	 * 
	 * @return true if successful, false otherwise
	 */
	public boolean loadDimensions()
	{
		boolean success = true;
		
		for ( final ViewDataBeads view : getViews() )
		{
			boolean readDim = view.loadDimensions();

			if (!readDim)
			{
				if ( getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
					IOFunctions.printErr( "Cannot read dimensions for view " + view + " in " + this );
				success = false;
			}			
		}
		
		return success;
	}	
	
	/**
	 * This static methods creates an instance of {@link ViewStructure} with all {@link ViewDataBeads} completely instantiated.
	 * Afterwards processing can start or already processed data can be loaded.
	 * 
	 * @param conf - the {@link SPIMConfiguration} object containing all information about the current registration
	 * @param timePointIndex - the id of the current time point as the {@link SPIMConfiguration} stores the information for all time points 
	 * @param model - the model to be used for registration
	 * @param id - arbitrary id, will be printed with the toString method
	 * @param debugLevel - the debug level of the program ViewStructure.DEBUG_ALL, ViewStructure.DEBUG_MAIN or ViewStructure.DEBUG_ERRORONLY
	 * @return an instance of the ViewStructure, completely intialized
	 */
	public static <M extends AbstractAffineModel3D<M>> ViewStructure initViewStructure( final SPIMConfiguration conf, final int timePointIndex, final M model, final String id, final int debugLevel )
	{
		final ArrayList<ViewDataBeads> views = new ArrayList<ViewDataBeads>();
		
		double zStretching;
		
		if ( conf.overrideImageZStretching )
		{
			zStretching = conf.zStretching;
		}
		else if ( conf.isHuiskenFormat() )
		{
			IOFunctions.println( "Reading z-stretching from xml." );
			zStretching = conf.getZStretchingHuisken();
			IOFunctions.println( "z-stretching = " + zStretching );
		}
		else
		{
			IOFunctions.println( "Opening first image to determine z-stretching." );
			final Image<FloatType> image = LOCI.openLOCIFloatType( conf.file[ timePointIndex ][ 0 ][ 0 ][ 0 ].getPath(), conf.inputImageFactory );

			if ( image == null )
			{
				IOFunctions.println( "Cannot open fie: '" + conf.file[ timePointIndex ][ 0 ][ 0 ][ 0 ].getPath() + "'" );
				return null;
			}
			
			zStretching = image.getCalibration( 2 ) / image.getCalibration( 0 );
			IOFunctions.println( "z-stretching = " + zStretching );
			image.close();
		}

		int idNr = firstID;
		final int numChannels = conf.file[ timePointIndex ].length;

		IOFunctions.println( "Using model: " + model.getClass().getCanonicalName() );
		
		for (int c = 0; c < conf.file[ timePointIndex ].length; c++)
			for (int a = 0; a < conf.file[ timePointIndex ][ c ].length; a++)
				for ( int i = 0; i < conf.file[ timePointIndex ][ c ][ a ].length; ++i )
				{
					final ViewDataBeads view = new ViewDataBeads( idNr++, model.copy(), conf.file[ timePointIndex ][ c ][ a ][ i ].getPath(), zStretching );
					int channelRegister = 0;

					view.setAcqusitionAngle( conf.angles[ a ] );
					view.setChannel( conf.channels[ c ] );
					view.setChannelIndex( c );
					view.setTimePoint( conf.timepoints[timePointIndex] );
					view.setIllumination( conf.illuminations[ i ] );

					if ( numChannels == 1 )
					{
						view.setUseForFusion( true );
						view.setUseForRegistration( true );
						view.setInitialSigma( conf.initialSigma[ channelRegister ] );
						view.setMinPeakValue( conf.minPeakValue[ channelRegister ] );
						view.setIntegralRadius1( conf.integralImgRadius1[ channelRegister ] );
						view.setIntegralRadius2( conf.integralImgRadius2[ channelRegister ] );
						view.setIntegralThreshold( conf.integralImgThreshold[ channelRegister ] );
					}
					else
					{
						boolean contains = false;
						for ( final int cR : conf.channelsRegister )
							if ( cR == view.getChannel() )
								contains = true;
						view.setUseForRegistration( contains );

						if ( contains )
						{
							view.setInitialSigma( conf.initialSigma[ channelRegister ] );
							view.setMinPeakValue( conf.minPeakValue[ channelRegister ] );
							view.setIntegralRadius1( conf.integralImgRadius1[ channelRegister ] );
							view.setIntegralRadius2( conf.integralImgRadius2[ channelRegister ] );
							view.setIntegralThreshold( conf.integralImgThreshold[ channelRegister ] );
							channelRegister++;
						}

						contains = false;
						for ( final int cF : conf.channelsFuse )
							if ( cF == view.getChannel() )
								contains = true;
						view.setUseForFusion( contains );
					}

					if ( conf.channelsMirror != null )
					{
						for ( final int[] mirror : conf.channelsMirror )
						{
							if ( conf.channels[ c ] == mirror[ 0 ] )
							{
								if ( mirror[ 1 ] == 0 )
									view.setMirrorHorizontally( true );
								if ( mirror[ 1 ] == 1 )
									view.setMirrorVertically( true );
							}
						}
					}
					views.add( view );
				}

		final ViewStructure viewStructure = new ViewStructure( views, conf, id, conf.timepoints[ timePointIndex ], numChannels, debugLevel );

		return viewStructure;
	}
}
