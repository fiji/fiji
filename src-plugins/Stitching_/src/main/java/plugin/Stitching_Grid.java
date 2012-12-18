package plugin;

import static stitching.CommonFunctions.addHyperLinkListener;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import loci.common.services.ServiceFactory;
import loci.formats.ChannelSeparator;
import loci.formats.IFormatReader;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.InvertibleBoundable;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.stitching.CollectionStitchingImgLib;
import mpicbg.stitching.ImageCollectionElement;
import mpicbg.stitching.ImagePlusTimePoint;
import mpicbg.stitching.StitchingParameters;
import mpicbg.stitching.TextFileAccess;
import mpicbg.stitching.fusion.Fusion;
import ome.xml.model.primitives.PositiveFloat;
import stitching.CommonFunctions;

/**
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 *
 */
public class Stitching_Grid implements PlugIn
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	
	public static boolean seperateOverlapY = false;
	
	public static int defaultGridChoice1 = 0;
	public static int defaultGridChoice2 = 0;

	public static int defaultGridSizeX = 2, defaultGridSizeY = 3;
	public static double defaultOverlapX = 20;
	public static double defaultOverlapY = 20;
	
	public static String defaultDirectory = "";
	public static String defaultSeriesFile = "";
	public static boolean defaultConfirmFiles = true;
	
	public static String defaultFileNames = "tile_{ii}.tif";
	public static String defaultTileConfiguration = "TileConfiguration.txt";
	public static boolean defaultComputeOverlap = true;
	public static boolean defaultInvertX = false;
	public static boolean defaultInvertY = false;
	public static boolean defaultSubpixelAccuracy = true;
	public static boolean writeOnlyTileConfStatic = false;
	
	public static boolean defaultIgnoreCalibration = false;
	public static double defaultIncreaseOverlap = 10;
	public static boolean defaultVirtualInput = false;
	
	public static int defaultStartI = 1;
	public static int defaultStartX = 1;
	public static int defaultStartY = 1;
	public static int defaultFusionMethod = 0;
	public static double defaultR = 0.3;
	public static double defaultRegressionThreshold = 0.3;
	public static double defaultDisplacementThresholdRelative = 2.5;		
	public static double defaultDisplacementThresholdAbsolute = 3.5;		
	public static boolean defaultOnlyPreview = false;
	public static int defaultMemorySpeedChoice = 0;
	
	//Added by John Lapage: user sets this parameter to define how many adjacent files each image will be compared to
	public static double defaultSeqRange = 1;	
	
	public static boolean defaultQuickFusion = true;
	
	public static String[] resultChoices = { "Fuse and display", "Write to disk" };
	public static int defaultResult = 0;
	public static String defaultOutputDirectory = "";
	
	@Override
	public void run( String arg0 ) 
	{
		final GridType grid = new GridType();
		
		final int gridType = grid.getType();
		final int gridOrder = grid.getOrder();
				
		if ( gridType == -1 || gridOrder == -1 )
			return;

		final GenericDialogPlus gd = new GenericDialogPlus( "Grid stitching: " + GridType.choose1[ gridType ] + ", " + GridType.choose2[ gridType ][ gridOrder ] );

		if ( gridType < 5 )
		{
			gd.addNumericField( "Grid_size_x", defaultGridSizeX, 0 );
			gd.addNumericField( "Grid_size_y", defaultGridSizeY, 0 );
			
			if ( seperateOverlapY )
			{
				gd.addSlider( "Tile_overlap_x [%]", 0, 100, defaultOverlapX );
				gd.addSlider( "Tile_overlap_y [%]", 0, 100, defaultOverlapY );				
			}
			else
			{
				gd.addSlider( "Tile_overlap [%]", 0, 100, defaultOverlapX );
			}
			
			// row-by-row, column-by-column or snake
			// needs the same questions
			if ( grid.getType() < 4 )
			{
				gd.addNumericField( "First_file_index_i", defaultStartI, 0 );
			}
			else
			{
				gd.addNumericField( "First_file_index_x", defaultStartX, 0 );
				gd.addNumericField( "First_file_index_y", defaultStartY, 0 );
			}
		}
		
		if ( gridType == 6 && gridOrder == 1 )
		{
			gd.addFileField( "Multi_series_file", defaultSeriesFile, 50 );
		}
		else
		{
			gd.addDirectoryField( "Directory", defaultDirectory, 50 );
		
			// Modified by John Lapage: copying the general setup for Unknown Positions option 
			if ( gridType == 5 || gridType == 7)
				gd.addCheckbox( "Confirm_files", defaultConfirmFiles );
			
			if ( gridType < 5 )			
				gd.addStringField( "File_names for tiles", defaultFileNames, 50 );
			
			if ( gridType == 6 )
				gd.addStringField( "Layout_file", defaultTileConfiguration, 50 );
			else
				gd.addStringField( "Output_textfile_name", defaultTileConfiguration, 50 );
		}
		
		gd.addChoice( "Fusion_method", CommonFunctions.fusionMethodListGrid, CommonFunctions.fusionMethodListGrid[ defaultFusionMethod ] );
		gd.addNumericField( "Regression_threshold", defaultRegressionThreshold, 2 );
		gd.addNumericField( "Max/avg_displacement_threshold", defaultDisplacementThresholdRelative, 2 );		
		gd.addNumericField( "Absolute_displacement_threshold", defaultDisplacementThresholdAbsolute, 2 );
		// added by John Lapage: creates text box in which the user can set which range to compare within. Would be nicer as an Integer.
		if (gridType == 7) 
			gd.addNumericField( "Frame range to compare", defaultSeqRange, 0 );
		
		if ( gridType < 5 )
			gd.addCheckbox( "Compute_overlap (otherwise use approximate grid coordinates)", defaultComputeOverlap );
		else if ( gridType == 6 && gridOrder == 0 )
			gd.addCheckbox( "Compute_overlap (otherwise apply coordinates from layout file)", defaultComputeOverlap );
		else if ( gridType == 6 && gridOrder == 1 )
		{
			gd.addCheckbox( "Compute_overlap (otherwise trust coordinates in the file)", defaultComputeOverlap );
			gd.addCheckbox( "Ignore_Calibration", defaultIgnoreCalibration );
			gd.addSlider( "Increase_overlap [%]", 0, 100, defaultIncreaseOverlap );
		}
		
		gd.addCheckbox( "Invert_X coordinates", defaultInvertX );
		gd.addCheckbox( "Invert_Y coordinates", defaultInvertY );
		gd.addCheckbox( "Subpixel_accuracy", defaultSubpixelAccuracy );
		gd.addCheckbox( "Use_virtual_input_images (Slow! Even slower when combined with subpixel accuracy during fusion!)", defaultVirtualInput );
		gd.addChoice( "Computation_parameters", CommonFunctions.cpuMemSelect, CommonFunctions.cpuMemSelect[ defaultMemorySpeedChoice ] );
		gd.addChoice( "Image_output", resultChoices, resultChoices[ defaultResult ] );
		gd.addMessage("");
		gd.addMessage( "This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener(text, myURL);

		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		// the general stitching parameters
		final StitchingParameters params = new StitchingParameters();
		
		final int gridSizeX, gridSizeY;
		double overlapX, overlapY;
		int startI = 0, startX = 0, startY = 0;
		
		if ( gridType < 5 )
		{
			gridSizeX = defaultGridSizeX = (int)Math.round(gd.getNextNumber());
			gridSizeY = defaultGridSizeY = (int)Math.round(gd.getNextNumber());
			
			if ( seperateOverlapY )
			{
				overlapX = defaultOverlapX = gd.getNextNumber();
				overlapX /= 100.0;				
				overlapY = defaultOverlapY = gd.getNextNumber();
				overlapY /= 100.0;				
				
			}
			else
			{
				overlapX = overlapY = defaultOverlapY = defaultOverlapX = gd.getNextNumber();
				overlapX /= 100.0;
				overlapY = overlapX;
			}
	
			// row-by-row, column-by-column or snake
			// needs the same questions
			if ( grid.getType() < 4 )
			{
				startI = defaultStartI = (int)Math.round(gd.getNextNumber());
			}
			else // position
			{
				startX = defaultStartI = (int)Math.round(gd.getNextNumber());
				startY = defaultStartI = (int)Math.round(gd.getNextNumber());			
			}
		}
		else
		{
			gridSizeX = gridSizeY = 0;
			overlapX = overlapY = 0;
		}
		
		String directory, outputFile, seriesFile;
		final boolean confirmFiles;
		final String filenames;
		
		if ( gridType == 6 && gridOrder == 1 )
		{
			seriesFile = defaultSeriesFile = gd.getNextString();
			outputFile = null;
			directory = null;
			filenames = null;
			confirmFiles = false;
		}
		else
		{
			directory = defaultDirectory = gd.getNextString();
			seriesFile = null;
				
			// Modified by John Lapage: copying the general setup for Unknown Positions option 
			if ( gridType == 5 || gridType == 7)
				confirmFiles = defaultConfirmFiles = gd.getNextBoolean();
			else
				confirmFiles = false;
			
			if ( gridType < 5 )
				filenames = defaultFileNames = gd.getNextString();
			else
				filenames = "";
	
			outputFile = defaultTileConfiguration = gd.getNextString();
		}
		
		params.fusionMethod = defaultFusionMethod = gd.getNextChoiceIndex();
		params.regThreshold = defaultRegressionThreshold = gd.getNextNumber();
		params.relativeThreshold = defaultDisplacementThresholdRelative = gd.getNextNumber();		
		params.absoluteThreshold = defaultDisplacementThresholdAbsolute = gd.getNextNumber();
		// Added by John Lapage: sends user specified range to the parameters object
		if ( gridType == 7) 
			params.seqRange = (int)(defaultSeqRange = Math.round( gd.getNextNumber() ) );
		
		// Modified by John Lapage (rearranged). Copies the setup for Unknown Positions. User specifies this with all other options.
		if ( gridType == 5 || gridType == 7)
			params.computeOverlap = true;
		else 
			params.computeOverlap = defaultComputeOverlap = gd.getNextBoolean();

		final double increaseOverlap;
		final boolean ignoreCalibration;
		if ( gridType == 6 && gridOrder == 1 )
		{
			ignoreCalibration = defaultIgnoreCalibration = gd.getNextBoolean();
			increaseOverlap = defaultIncreaseOverlap = gd.getNextNumber();
		}
		else
		{
			ignoreCalibration = false;
			increaseOverlap = 0;
		}
		
		final boolean invertX = params.invertX = defaultInvertX = gd.getNextBoolean();
		final boolean invertY = params.invertY = defaultInvertY = gd.getNextBoolean();

		params.subpixelAccuracy = defaultSubpixelAccuracy = gd.getNextBoolean();
		params.virtual = defaultVirtualInput = gd.getNextBoolean();
		params.cpuMemChoice = defaultMemorySpeedChoice = gd.getNextChoiceIndex();
		params.outputVariant = defaultResult = gd.getNextChoiceIndex();
		
		if ( params.virtual )
		{
			IJ.log( "WARNING: Using virtual input images. This will save a lot of RAM, but will also be slower ... \n" );
			
			if ( params.subpixelAccuracy && params.fusionMethod != CommonFunctions.fusionMethodListGrid.length - 1 )
			{
				IJ.log( "WARNING: You combine subpixel-accuracy with virtual input images, fusion will take 2-times longer ... \n" );
			}
		}
		
		if ( params.fusionMethod != CommonFunctions.fusionMethodListGrid.length - 1 && params.outputVariant == 1 )
		{
			if ( defaultOutputDirectory == null || defaultOutputDirectory.length() == 0 )
				defaultOutputDirectory = defaultDirectory;
			
			final GenericDialogPlus gd2 = new GenericDialogPlus( "Select output directory" );
			gd2.addDirectoryField( "Output_directory", defaultOutputDirectory, 60 );
			gd2.showDialog();
			
			if ( gd2.wasCanceled() )
				return;
			
			params.outputDirectory = defaultOutputDirectory = gd2.getNextString();
		}
		else
		{
			params.outputDirectory = null;
		}

		final long startTime = System.currentTimeMillis();
		
		// we need to set this
		params.channel1 = 0;
		params.channel2 = 0;
		params.timeSelect = 0;
		params.checkPeaks = 5;
				
		//added by John Lapage: sets the parameters object to recognise that Sequential File pairing should be performed
		if ( gridType == 7) params.sequential=true;
				
		// for reading in writing the tileconfiguration file
		if ( ! (gridType == 6 && gridOrder == 1 ) )
		{		
			directory = directory.replace('\\', '/');
			directory = directory.trim();
			if (directory.length() > 0 && !directory.endsWith("/"))
				directory = directory + "/";
		}
		
		// get all imagecollectionelements
		final ArrayList< ImageCollectionElement > elements;
		
		if ( gridType < 5 )
			elements = getGridLayout( grid, gridSizeX, gridSizeY, overlapX, overlapY, directory, filenames, startI, startX, startY, params.virtual );
		//John Lapage modified this: copying setup for Unknown Positions
		else if ( gridType == 5 || gridType == 7)
			elements = getAllFilesInDirectory( directory, confirmFiles );
		else if ( gridType == 6 && gridOrder == 1 )
			elements = getLayoutFromMultiSeriesFile( seriesFile, increaseOverlap, ignoreCalibration, invertX, invertY );
		else if ( gridType == 6 )
			elements = getLayoutFromFile( directory, outputFile );
		else
			elements = null;
		
		if ( elements == null || elements.size() < 2 )
		{
			IJ.log( "Could not initialise stitching." );
			return;
		}
		
		// open all images (if not done already by grid parsing) and test them, collect information
		int numChannels = -1;
		int numTimePoints = -1;
		
		boolean is2d = false;
		boolean is3d = false;
		
		for ( final ImageCollectionElement element : elements )
		{
			if ( gridType >=5 )
			{
				if ( params.virtual )
					IJ.log( "Opening VIRTUAL: " + element.getFile().getAbsolutePath() + " ... " );
				else
					IJ.log( "Loading: " + element.getFile().getAbsolutePath() + " ... " );
			}
				
			
			long time = System.currentTimeMillis();
			final ImagePlus imp = element.open( params.virtual );
			
			time = System.currentTimeMillis() - time;
			
			if ( imp == null )
				return;
			
			int lastNumChannels = numChannels;
			int lastNumTimePoints = numTimePoints;
			numChannels = imp.getNChannels();
			numTimePoints = imp.getNFrames();
			
			if ( imp.getNSlices() > 1 )
			{
				if ( gridType >=5 )
					IJ.log( "" + imp.getWidth() + "x" + imp.getHeight() + "x" + imp.getNSlices() + "px, channels=" + numChannels + ", timepoints=" + numTimePoints + " (" + time + " ms)" );
				is3d = true;					
			}
			else
			{
				if ( gridType >=5 )
					IJ.log( "" + imp.getWidth() + "x" + imp.getHeight() + "px, channels=" + numChannels + ", timepoints=" + numTimePoints + " (" + time + " ms)" );
				is2d = true;
			}
			
			// test validity of images
			if ( is2d && is3d )
			{
				IJ.log( "Some images are 2d, some are 3d ... cannot proceed" );
				return;
			}
			
			if ( ( lastNumChannels != numChannels ) && lastNumChannels != -1 )
			{
				IJ.log( "Number of channels per image changes ... cannot proceed" );
				return;					
			}

			if ( ( lastNumTimePoints != numTimePoints ) && lastNumTimePoints != -1 )
			{
				IJ.log( "Number of timepoints per image changes ... cannot proceed" );
				return;					
			}
			
		// John Lapage changed this: copying setup for Unknown Positions
		if ( gridType == 5 || gridType == 7)
			{
				if ( is2d )
				{
					element.setDimensionality( 2 );
            		element.setModel( new TranslationModel2D() );
            		element.setOffset( new float[]{ 0, 0 } );
				}
				else
				{
					element.setDimensionality( 3 );
            		element.setModel( new TranslationModel3D() );
            		element.setOffset( new float[]{ 0, 0, 0 } );
				}
				
			}
		}
		
		// the dimensionality of each image that will be correlated (might still have more channels or timepoints)
		final int dimensionality;
		
		if ( is2d )
			dimensionality = 2;
		else
			dimensionality = 3;
		
		params.dimensionality = dimensionality;
    	
    	// write the initial tileconfiguration
    	if ( gridType != 6 )
    		writeTileConfiguration( new File( directory, outputFile ), elements );
    	    	
    	// call the final stitiching
    	final ArrayList<ImagePlusTimePoint> optimized = CollectionStitchingImgLib.stitchCollection( elements, params );
    	
    	if ( optimized == null )
    		return;
    	
    	// output the result
		for ( final ImagePlusTimePoint imt : optimized )
			IJ.log( imt.getImagePlus().getTitle() + ": " + imt.getModel() );
		
    	// write the file tileconfiguration
		if ( params.computeOverlap && outputFile != null )
		{
			if ( outputFile.endsWith( ".txt" ) )
				outputFile = outputFile.substring( 0, outputFile.length() - 4 ) + ".registered.txt";
			else
				outputFile = outputFile + ".registered.txt";
				
			writeRegisteredTileConfiguration( new File( directory, outputFile ), elements );
		}
		
		// fuse		
		if ( params.fusionMethod != CommonFunctions.fusionMethodListGrid.length - 1 )
		{
			long time = System.currentTimeMillis();
			
			if ( params.outputDirectory == null )
				IJ.log( "Fuse & Display ..." );
			else
				IJ.log( "Fuse & Write to disk (into directory '" + new File( params.outputDirectory, "" ).getAbsolutePath() + "') ..." );
			
			// first prepare the models and get the targettype
			final ArrayList<InvertibleBoundable> models = new ArrayList< InvertibleBoundable >();
			final ArrayList<ImagePlus> images = new ArrayList<ImagePlus>();
			
			boolean is32bit = false;
			boolean is16bit = false;
			boolean is8bit = false;
			
			for ( final ImagePlusTimePoint imt : optimized )
			{
				final ImagePlus imp = imt.getImagePlus();
				
				if ( imp.getType() == ImagePlus.GRAY32 )
					is32bit = true;
				else if ( imp.getType() == ImagePlus.GRAY16 )
					is16bit = true;
				else if ( imp.getType() == ImagePlus.GRAY8 )
					is8bit = true;
				
				images.add( imp );
			}
			
			for ( int f = 1; f <= numTimePoints; ++f )
				for ( final ImagePlusTimePoint imt : optimized )
					models.add( (InvertibleBoundable)imt.getModel() );
	
			ImagePlus imp = null;
			
			// test if there is no overlap between any of the tiles
			// if so fusion can be much faster
			boolean noOverlap = false;
			if ( overlapX == 0 && overlapY == 0 && params.computeOverlap == false && params.subpixelAccuracy == false && grid.getType() < 4 )
			{
				final GenericDialogPlus gd3 = new GenericDialogPlus( "Use fast fusion algorithm" );
				gd3.addMessage( "There seems to be no overlap between any of the tiles." );
				gd3.addCheckbox( "Use fast fusion?", defaultQuickFusion );
				
				gd3.showDialog();
				
				if ( gd3.wasCanceled() )
					return;
				
				noOverlap = defaultQuickFusion = gd3.getNextBoolean();
				
				if ( noOverlap )
					IJ.log( "There is no overlap between any of the tiles, using faster fusion algorithm." );
			}
			
			if ( is32bit )
				imp = Fusion.fuse( new FloatType(), images, models, params.dimensionality, params.subpixelAccuracy, params.fusionMethod, params.outputDirectory, noOverlap );
			else if ( is16bit )
				imp = Fusion.fuse( new UnsignedShortType(), images, models, params.dimensionality, params.subpixelAccuracy, params.fusionMethod, params.outputDirectory, noOverlap );
			else if ( is8bit )
				imp = Fusion.fuse( new UnsignedByteType(), images, models, params.dimensionality, params.subpixelAccuracy, params.fusionMethod, params.outputDirectory, noOverlap );
			else
				IJ.log( "Unknown image type for fusion." );
			
			IJ.log( "Finished fusion (" + (System.currentTimeMillis() - time) + " ms)");
			IJ.log( "Finished ... (" + (System.currentTimeMillis() - startTime) + " ms)");
			
			if ( imp != null )
			{
				imp.setTitle( "Fused" );
				imp.show();
			}
		}
		
    	// close all images
    	for ( final ImageCollectionElement element : elements )
    		element.close();
	}
	
	protected ArrayList< ImageCollectionElement > getLayoutFromMultiSeriesFile( final String multiSeriesFile, final double increaseOverlap, final boolean ignoreCalibration, final boolean invertX, final boolean invertY )
	{
		if ( multiSeriesFile == null || multiSeriesFile.length() == 0 )
		{
			IJ.log( "Filename is empty!" );
			return null;
		}

		final ArrayList< ImageCollectionElement > elements = new ArrayList< ImageCollectionElement >();		
		
		final IFormatReader r = new ChannelSeparator();
		
		final boolean timeHack;
		try 
		{
			final ServiceFactory factory = new ServiceFactory();
			final OMEXMLService service = factory.getInstance( OMEXMLService.class );
			final IMetadata meta = service.createOMEXMLMetadata();
			r.setMetadataStore( meta );

			r.setId( multiSeriesFile );

			final int numSeries = r.getSeriesCount();
			
			if ( IJ.debugMode )
				IJ.log( "numSeries:  " + numSeries );
			
			// get maxZ
			int dim = 2;
			for ( int series = 0; series < numSeries; ++series )
				if ( r.getSizeZ() > 1 )
					dim = 3;

			if ( IJ.debugMode )
				IJ.log( "dim:  " + dim );

			final MetadataRetrieve retrieve = service.asRetrieve(r.getMetadataStore());
			if ( IJ.debugMode )
				IJ.log( "retrieve:  " + retrieve );

			// CTR HACK: In the case of a single series, we treat each time point
			// as a separate series for the purpose of stitching tiles.
			timeHack = numSeries == 1;

			for ( int series = 0; series < numSeries; ++series )
			{
				if ( IJ.debugMode )
					IJ.log( "fetching data for series:  " + series );
				r.setSeries( series );

				final int sizeT = r.getSizeT();
				if ( IJ.debugMode )
					IJ.log( "sizeT:  " + sizeT );

				final int maxT = timeHack ? sizeT : 1;

				for ( int t = 0; t < maxT; ++t )
				{
					double[] location =
						CommonFunctions.getPlanePosition( r, retrieve, series, t, invertX, invertY );
					double locationX = location[0];
					double locationY = location[1];
					double locationZ = location[2];

					if ( !ignoreCalibration )
					{
						// calibration
						double calX = 1, calY = 1, calZ = 1;
						PositiveFloat cal;
						final String dimOrder = r.getDimensionOrder().toUpperCase();

						final int posX = dimOrder.indexOf( 'X' );
						cal = retrieve.getPixelsPhysicalSizeX( series );
						if ( posX >= 0 && cal != null && cal.getValue().floatValue() != 0 )
							calX = cal.getValue().floatValue();

						if ( IJ.debugMode )
							IJ.log( "calibrationX:  " + calX );

						final int posY = dimOrder.indexOf( 'Y' );
						cal = retrieve.getPixelsPhysicalSizeY( series );
						if ( posY >= 0 && cal != null && cal.getValue().floatValue() != 0 )
							calY = cal.getValue().floatValue();

						if ( IJ.debugMode )
							IJ.log( "calibrationY:  " + calY );

						final int posZ = dimOrder.indexOf( 'Z' );
						cal = retrieve.getPixelsPhysicalSizeZ( series );
						if ( posZ >= 0 && cal != null && cal.getValue().floatValue() != 0 )
							calZ = cal.getValue().floatValue();

						if ( IJ.debugMode )
							IJ.log( "calibrationZ:  " + calZ );

						// location in pixel values;
						locationX /= calX;
						locationY /= calY;
						locationZ /= calZ;
					}

					// increase overlap if desired
					locationX *= (100.0-increaseOverlap)/100.0;
					locationY *= (100.0-increaseOverlap)/100.0;
					locationZ *= (100.0-increaseOverlap)/100.0;

					// create ImageInformationList

					final ImageCollectionElement element;

					if ( dim == 2 )
					{
						element = new ImageCollectionElement( new File( multiSeriesFile ), elements.size() );
						element.setModel( new TranslationModel2D() );
						element.setOffset( new float[]{ (float)locationX, (float)locationY } );
						element.setDimensionality( 2 );
					}
					else
					{
						element = new ImageCollectionElement( new File( multiSeriesFile ), elements.size() );
						element.setModel( new TranslationModel3D() );
						element.setOffset( new float[]{ (float)locationX, (float)locationY, (float)locationZ } );
						element.setDimensionality( 3 );
					}

					elements.add( element );
				}
			}
		}
		catch ( Exception ex ) 
		{ 
			IJ.handleException(ex);
			ex.printStackTrace();
			return null; 
		}

		// open all images
		ImporterOptions options;
		try 
		{
			options = new ImporterOptions();
			options.setId( new File( multiSeriesFile ).getAbsolutePath() );
			options.setSplitChannels( false );
			options.setSplitTimepoints( timeHack );
			options.setSplitFocalPlanes( false );
			options.setAutoscale( false );
			
			options.setOpenAllSeries( true );		
			
			final ImagePlus[] imps = BF.openImagePlus( options );
			
			if ( imps.length != elements.size() )
			{
				IJ.log( "Inconsistent series layout. Metadata says " + elements.size() + " tiles, but contains only " + imps.length + " images/tiles." );
				
				for ( ImagePlus imp : imps )
					if ( imp != null )
						imp.close();
				
				return null;
			}
			
			for ( int series = 0; series < elements.size(); ++series )
			{
				final ImageCollectionElement element = elements.get( series );
				element.setImagePlus( imps[ series ] );
				
				if ( element.getDimensionality() == 2 )
					IJ.log( "series " + series + ": position = (" + element.getOffset( 0 ) + "," + element.getOffset( 1 ) + ") [px], " +
							"size = (" + element.getDimension( 0 ) + "," + element.getDimension( 1 ) + ")" );
				else
					IJ.log( "series " + series + ": position = (" + element.getOffset( 0 ) + "," + element.getOffset( 1 ) + "," + element.getOffset( 2 ) + ") [px], " +
							"size = (" + element.getDimension( 0 ) + "," + element.getDimension( 1 ) + "," + element.getDimension( 2 ) + ")" );
			}
			
		} 
		catch (Exception e) 
		{
			IJ.log( "Cannot open multiseries file: " + e );
			e.printStackTrace();
			return null;
		}

		return elements;
	}	
	
	protected ArrayList< ImageCollectionElement > getLayoutFromFile( final String directory, final String layoutFile )
	{
		final ArrayList< ImageCollectionElement > elements = new ArrayList< ImageCollectionElement >();
		int dim = -1;
		int index = 0;
		
		try
		{
			final BufferedReader in = TextFileAccess.openFileRead( new File( directory, layoutFile ) );
			
			if ( in == null )
			{
				IJ.log( "Cannot find tileconfiguration file '" + new File( directory, layoutFile ).getAbsolutePath() + "'" );
				return null;
			}
			
			int lineNo = 0;
			
			while ( in.ready() )
			{
				String line = in.readLine().trim();
				lineNo++;
				if ( !line.startsWith( "#" ) && line.length() > 3 )
				{
					if ( line.startsWith( "dim" ) )
					{
						String entries[] = line.split( "=" );
						if ( entries.length != 2 )
						{
							System.out.println( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + " does not look like [ dim = n ]: " + line);
							IJ.log( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + " does not look like [ dim = n ]: " + line);
							return null;						
						}
						
						try
						{
							dim = Integer.parseInt( entries[1].trim() );
						}
						catch ( NumberFormatException e )
						{
							System.out.println( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": Cannot parse dimensionality: " + entries[1].trim());
							IJ.log( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": Cannot parse dimensionality: " + entries[1].trim());
							return null;														
						}
					}
					else
					{
						if ( dim < 0 )
						{
							System.out.println( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": Header missing, should look like [dim = n], but first line is: " + line);
							IJ.log( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": Header missing, should look like [dim = n], but first line is: " + line);
							return null;							
						}
						
						if ( dim < 2 || dim > 3 )
						{
							System.out.println( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": only dimensions of 2 and 3 are supported: " + line);
							IJ.log( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": only dimensions of 2 and 3 are supported: " + line);
							return null;							
						}
						
						// read image tiles
						String entries[] = line.split(";");
						if (entries.length != 3)
						{
							System.out.println( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + " does not have 3 entries! [fileName; ImagePlus; (x,y,...)]");
							IJ.log( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + " does not have 3 entries! [fileName; ImagePlus; (x,y,...)]");
							return null;						
						}
						String imageName = entries[0].trim();
						String imp = entries[1].trim();
						
						if (imageName.length() == 0 && imp.length() == 0)
						{
							System.out.println( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": You have to give a filename or a ImagePlus [fileName; ImagePlus; (x,y,...)]: " + line);
							IJ.log( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": You have to give a filename or a ImagePlus [fileName; ImagePlus; (x,y,...)]: " + line);
							return null;						
						}
						
						String point = entries[2].trim();
						if (!point.startsWith("(") || !point.endsWith(")"))
						{
							System.out.println( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": Wrong format of coordinates: (x,y,...): " + point);
							IJ.log( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": Wrong format of coordinates: (x,y,...): " + point);
							return null;
						}
						
						point = point.substring(1, point.length() - 1);
						String points[] = point.split(",");
						if (points.length != dim)
						{
							System.out.println( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": Wrong format of coordinates: (x,y,z,..), dim = " + dim + ": " + point);
							IJ.log( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": Wrong format of coordinates: (x,y,z,...), dim = " + dim + ": " + point);
							return null;
						}
						
						ImageCollectionElement element = new ImageCollectionElement( new File( directory, imageName ), index++ );
						element.setDimensionality( dim );
								
						if ( dim == 3 )
							element.setModel( new TranslationModel3D() );
						else
							element.setModel( new TranslationModel2D() );

						final float[] offset = new float[ dim ];
						for ( int i = 0; i < dim; i++ )
						{
							try
							{
								offset[ i ] = Float.parseFloat( points[i].trim() ); 
							}
							catch (NumberFormatException e)
							{
								System.out.println( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": Cannot parse number: " + points[i].trim());
								IJ.log( "Stitching_Grid.getLayoutFromFile: Line " + lineNo + ": Cannot parse number: " + points[i].trim());
								return null;							
							}
						}
						
						element.setOffset( offset );
						elements.add( element );
					}
				}
			}
		}
		catch ( IOException e )
		{
			System.out.println( "Stitch_Grid.getLayoutFromFile: " + e );
			IJ.log( "Stitching_Grid.getLayoutFromFile: " + e );
			return null;
		};
		
		return elements;
	}
	
	protected ArrayList< ImageCollectionElement > getAllFilesInDirectory( final String directory, final boolean confirmFiles )
	{
		// get all files from the directory
		final File dir = new File( directory );
		if ( !dir.isDirectory() )
		{
			IJ.log( "'" + directory + "' is not a directory. stop.");
			return null;
		}
		
		final String[] imageFiles = dir.list();
		final ArrayList<String> files = new ArrayList<String>();
		for ( final String fileName : imageFiles )
		{
			File file = new File( dir, fileName );
			
			if ( file.isFile() && !file.isHidden() && !fileName.endsWith( ".txt" ) && !fileName.endsWith( ".TXT" ) )
			{
				IJ.log( file.getPath() );
				files.add( fileName );
			}
		}
		
		IJ.log( "Found " + files.size() + " files (we ignore hidden and .txt files)." );
		
		if ( files.size() < 2 )
		{
			IJ.log( "Only " + files.size() + " files found in '" + dir.getPath() + "', you need at least 2 - stop." );
			return null ;
		}
		
		final boolean[] useFile = new boolean[ files.size() ];
		for ( int i = 0; i < files.size(); ++i )
			useFile[ i ] = true;
		
		if ( confirmFiles )
		{
			final GenericDialogPlus gd = new GenericDialogPlus( "Confirm files" );
			
			for ( final String name : files )
				gd.addCheckbox( name, true );
			
			gd.showDialog();
			
			if ( gd.wasCanceled() )
				return null;
			
			for ( int i = 0; i < files.size(); ++i )
				useFile[ i ] = gd.getNextBoolean();
		}
	
		final ArrayList< ImageCollectionElement > elements = new ArrayList< ImageCollectionElement >();

		for ( int i = 0; i < files.size(); ++i )
			if ( useFile [ i ] )
				elements.add( new ImageCollectionElement( new File( directory, files.get( i ) ), i ) );
		
		if ( elements.size() < 2 )
		{
			IJ.log( "Only " + elements.size() + " files selected, you need at least 2 - stop." );
			return null ;			
		}
		
		return elements;
	}
	
	protected ArrayList< ImageCollectionElement > getGridLayout( final GridType grid, final int gridSizeX, final int gridSizeY, final double overlapX, final double overlapY, final String directory, final String filenames, 
			final int startI, final int startX, final int startY, final boolean virtual )
	{
		final int gridType = grid.getType();
		final int gridOrder = grid.getOrder();

		// define the parsing of filenames
		// find how to parse
		String replaceX = "{", replaceY = "{", replaceI = "{";
		int numXValues = 0, numYValues = 0, numIValues = 0;

		if ( grid.getType() < 4 )
		{
			int i1 = filenames.indexOf("{i");
			int i2 = filenames.indexOf("i}");
			if (i1 >= 0 && i2 > 0)
			{
				numIValues = i2 - i1;
				for (int i = 0; i < numIValues; i++)
					replaceI += "i";
				replaceI += "}";
			}
			else
			{
				replaceI = "\\\\\\\\";
			}			
		}
		else
		{
			int x1 = filenames.indexOf("{x");
			int x2 = filenames.indexOf("x}");
			if (x1 >= 0 && x2 > 0)
			{
				numXValues = x2 - x1;
				for (int i = 0; i < numXValues; i++)
					replaceX += "x";
				replaceX += "}";
			}
			else
			{
				replaceX = "\\\\\\\\";
			}

			int y1 = filenames.indexOf("{y");
			int y2 = filenames.indexOf("y}");
			if (y1 >= 0 && y2 > 0)
			{
				numYValues = y2 - y1;
				for (int i = 0; i < numYValues; i++)
					replaceY += "y";
				replaceY += "}";
			}
			else
			{
				replaceY = "\\\\\\\\";
			}			
		}
		
		// determine the layout
		final ImageCollectionElement[][] gridLayout = new ImageCollectionElement[ gridSizeX ][ gridSizeY ];
		
		// all snakes, row, columns, whatever
		if ( grid.getType() < 4 )
		{
			// the current position[x, y] 
			final int[] position = new int[ 2 ];
			
			// we have gridSizeX * gridSizeY tiles
			for ( int i = 0; i < gridSizeX * gridSizeY; ++i )
			{
				// get the vector where to move
				getPosition( position, i, gridType, gridOrder, gridSizeX, gridSizeY );

				// get the filename
            	final String file = filenames.replace( replaceI, getLeadingZeros( numIValues, i + startI ) );
            	gridLayout[ position[ 0 ] ][ position [ 1 ] ] = new ImageCollectionElement( new File( directory, file ), i ); 
			}
		}
		else // fixed positions
		{
			// an index for the element
			int i = 0;
			
			for ( int y = 0; y < gridSizeY; ++y )
				for ( int x = 0; x < gridSizeX; ++x )
				{
					final String file = filenames.replace( replaceX, getLeadingZeros( numXValues, x + startX ) ).replace( replaceY, getLeadingZeros( numYValues, y + startY ) );
	            	gridLayout[ x ][ y ] = new ImageCollectionElement( new File( directory, file ), i++ );
				}
		}
		
		// based on the minimum size we will compute the initial arrangement
		int minWidth = Integer.MAX_VALUE;
		int minHeight = Integer.MAX_VALUE;
		int minDepth = Integer.MAX_VALUE;
		
		boolean is2d = false;
		boolean is3d = false;
		
		// open all images and test them, collect information
		for ( int y = 0; y < gridSizeY; ++y )
			for ( int x = 0; x < gridSizeX; ++x )
			{
				if ( virtual )
					IJ.log( "Opening VIRTUAL (" + x + ", " + y + "): " + gridLayout[ x ][ y ].getFile().getAbsolutePath() + " ... " );
				else
					IJ.log( "Loading (" + x + ", " + y + "): " + gridLayout[ x ][ y ].getFile().getAbsolutePath() + " ... " );			
				
				long time = System.currentTimeMillis();
				final ImagePlus imp = gridLayout[ x ][ y ].open( virtual );
				time = System.currentTimeMillis() - time;
				
				if ( imp == null )
					return null;
				
				if ( imp.getNSlices() > 1 )
				{
					IJ.log( "" + imp.getWidth() + "x" + imp.getHeight() + "x" + imp.getNSlices() + "px, channels=" + imp.getNChannels() + ", timepoints=" + imp.getNFrames() + " (" + time + " ms)" );
					is3d = true;					
				}
				else
				{
					IJ.log( "" + imp.getWidth() + "x" + imp.getHeight() + "px, channels=" + imp.getNChannels() + ", timepoints=" + imp.getNFrames() + " (" + time + " ms)" );
					is2d = true;
				}
				
				// test validity of images
				if ( is2d && is3d )
				{
					IJ.log( "Some images are 2d, some are 3d ... cannot proceed" );
					return null;
				}

				if ( imp.getWidth() < minWidth )
					minWidth = imp.getWidth();

				if ( imp.getHeight() < minHeight )
					minHeight = imp.getHeight();
				
				if ( imp.getNSlices() < minDepth )
					minDepth = imp.getNSlices();
			}
		
		final int dimensionality;
		
		if ( is3d )
			dimensionality = 3;
		else
			dimensionality = 2;
			
		// now get the approximate coordinates for each element
		// that is easiest done incrementally
		int xoffset = 0, yoffset = 0, zoffset = 0;
    	
		// an ArrayList containing all the ImageCollectionElements
		final ArrayList< ImageCollectionElement > elements = new ArrayList< ImageCollectionElement >();
		
    	for ( int y = 0; y < gridSizeY; y++ )
    	{
        	if ( y == 0 )
        		yoffset = 0;
        	else 
        		yoffset += (int)( minHeight * ( 1 - overlapY ) );

        	for ( int x = 0; x < gridSizeX; x++ )
            {
        		final ImageCollectionElement element = gridLayout[ x ][ y ];
        		
            	if ( x == 0 && y == 0 )
            		xoffset = yoffset = zoffset = 0;
            	
            	if ( x == 0 )
            		xoffset = 0;
            	else 
            		xoffset += (int)( minWidth * ( 1 - overlapX ) );
            	            	
            	element.setDimensionality( dimensionality );
            	
            	if ( dimensionality == 3 )
            	{
            		element.setModel( new TranslationModel3D() );
            		element.setOffset( new float[]{ xoffset, yoffset, zoffset } );
            	}
            	else
            	{
            		element.setModel( new TranslationModel2D() );
            		element.setOffset( new float[]{ xoffset, yoffset } );
            	}
            	
            	elements.add( element );
            }
    	}
    	
    	return elements;
	}
	
	// current snake directions ( if necessary )
	// they need a global state
	int snakeDirectionX = 0; 
	int snakeDirectionY = 0; 
	
	protected void writeTileConfiguration( final File file, final ArrayList< ImageCollectionElement > elements )
	{
    	// write the initial tileconfiguration
		final PrintWriter out = TextFileAccess.openFileWrite( file );
		final int dimensionality = elements.get( 0 ).getDimensionality();
		
		out.println( "# Define the number of dimensions we are working on" );
        out.println( "dim = " + dimensionality );
        out.println( "" );
        out.println( "# Define the image coordinates" );
        
        for ( final ImageCollectionElement element : elements )
        {
    		if ( dimensionality == 3 )
    			out.println( element.getFile().getName() + "; ; (" + element.getOffset( 0 ) + ", " + element.getOffset( 1 ) + ", " + element.getOffset( 2 ) + ")");
    		else
    			out.println( element.getFile().getName() + "; ; (" + element.getOffset( 0 ) + ", " + element.getOffset( 1 ) + ")");        	
        }

    	out.close();		
	}

	protected void writeRegisteredTileConfiguration( final File file, final ArrayList< ImageCollectionElement > elements )
	{
    	// write the initial tileconfiguration
		final PrintWriter out = TextFileAccess.openFileWrite( file );
		final int dimensionality = elements.get( 0 ).getDimensionality();
		
		out.println( "# Define the number of dimensions we are working on" );
        out.println( "dim = " + dimensionality );
        out.println( "" );
        out.println( "# Define the image coordinates" );
        
        for ( final ImageCollectionElement element : elements )
        {
    		if ( dimensionality == 3 )
    		{
    			final TranslationModel3D m = (TranslationModel3D)element.getModel();
    			out.println( element.getFile().getName() + "; ; (" + m.getTranslation()[ 0 ] + ", " + m.getTranslation()[ 1 ] + ", " + m.getTranslation()[ 2 ] + ")");
    		}
    		else
    		{
    			final TranslationModel2D m = (TranslationModel2D)element.getModel();
    			final float[] tmp = new float[ 2 ];
    			m.applyInPlace( tmp );
    			
    			out.println( element.getFile().getName() + "; ; (" + tmp[ 0 ] + ", " + tmp[ 1 ] + ")");
    		}
        }

    	out.close();		
	}

	protected void getPosition( final int[] currentPosition, final int i, final int gridType, final int gridOrder, final int sizeX, final int sizeY )
	{
		// gridType: "Row-by-row", "Column-by-column", "Snake by rows", "Snake by columns", "Fixed position"
		// gridOrder:
		//		choose2[ 0 ] = new String[]{ "Right & Down", "Left & Down", "Right & Up", "Left & Up" };
		//		choose2[ 1 ] = new String[]{ "Down & Right", "Down & Left", "Up & Right", "Up & Left" };
		//		choose2[ 2 ] = new String[]{ "Right & Down", "Left & Down", "Right & Up", "Left & Up" };
		//		choose2[ 3 ] = new String[]{ "Down & Right", "Down & Left", "Up & Right", "Up & Left" };
			
		// init the position
		if ( i == 0 )
		{
			if ( gridOrder == 0 || gridOrder == 2 )
				currentPosition[ 0 ] = 0;
			else
				currentPosition[ 0 ] = sizeX - 1;
			
			if ( gridOrder == 0 || gridOrder == 1 )
				currentPosition[ 1 ] = 0;
			else
				currentPosition[ 1 ] = sizeY - 1;
			
			// it is a snake
			if ( gridType == 2 || gridType == 3 )
			{
				// starting with right
				if ( gridOrder == 0 || gridOrder == 2 )
					snakeDirectionX = 1;
				else // starting with left
					snakeDirectionX = -1;
				
				// starting with down
				if ( gridOrder == 0 || gridOrder == 1 )
					snakeDirectionY = 1;
				else // starting with up
					snakeDirectionY = -1;
			}
		}
		else // a move is required
		{
			// row-by-row, "Right & Down", "Left & Down", "Right & Up", "Left & Up"
			if ( gridType == 0 )
			{
				// 0="Right & Down", 2="Right & Up"
				if ( gridOrder == 0 || gridOrder == 2 )
				{
					if ( currentPosition[ 0 ] < sizeX - 1 )
					{
						// just move one more right
						++currentPosition[ 0 ];
					}
					else
					{
						// we have to change rows
						if ( gridOrder == 0 )
							++currentPosition[ 1 ];
						else
							--currentPosition[ 1 ];
						
						// row-by-row going right, so only set position to 0
						currentPosition[ 0 ] = 0;
					}
				}
				else // 1="Left & Down", 3="Left & Up"
				{
					if ( currentPosition[ 0 ] > 0 )
					{
						// just move one more left
						--currentPosition[ 0 ];
					}
					else
					{
						// we have to change rows
						if ( gridOrder == 1 )
							++currentPosition[ 1 ];
						else
							--currentPosition[ 1 ];
						
						// row-by-row going left, so only set position to 0
						currentPosition[ 0 ] = sizeX - 1;
					}					
				}
			}
			else if ( gridType == 1 ) // col-by-col, "Down & Right", "Down & Left", "Up & Right", "Up & Left"
			{
				// 0="Down & Right", 1="Down & Left"
				if ( gridOrder == 0 || gridOrder == 1 )
				{
					if ( currentPosition[ 1 ] < sizeY - 1 )
					{
						// just move one down
						++currentPosition[ 1 ];
					}
					else
					{
						// we have to change columns
						if ( gridOrder == 0 )
							++currentPosition[ 0 ];
						else
							--currentPosition[ 0 ];
						
						// column-by-column going down, so position = 0
						currentPosition[ 1 ] = 0;
					}
				}
				else // 2="Up & Right", 3="Up & Left"
				{
					if ( currentPosition[ 1 ] > 0 )
					{
						// just move one up
						--currentPosition[ 1 ];
					}
					else
					{
						// we have to change columns
						if ( gridOrder == 2 )
							++currentPosition[ 0 ];
						else
							--currentPosition[ 0 ];
						
						// column-by-column going up, so position = sizeY - 1
						currentPosition[ 1 ] = sizeY - 1;
					}
				}
			}
			else if ( gridType == 2 ) // "Snake by rows"
			{
				// currently going right
				if ( snakeDirectionX > 0 )
				{
					if ( currentPosition[ 0 ] < sizeX - 1 )
					{
						// just move one more right
						++currentPosition[ 0 ];
					}
					else
					{
						// just we have to change rows
						currentPosition[ 1 ] += snakeDirectionY;
						
						// and change the direction of the snake in x
						snakeDirectionX *= -1;
					}
				}
				else
				{
					// currently going left
					if ( currentPosition[ 0 ] > 0 )
					{
						// just move one more left
						--currentPosition[ 0 ];
						return;
					}
					else
					{
						// just we have to change rows
						currentPosition[ 1 ] += snakeDirectionY;
						
						// and change the direction of the snake in x
						snakeDirectionX *= -1;
					}
				}
			}
			else if ( gridType == 3 ) // "Snake by columns" 
			{
				// currently going down
				if ( snakeDirectionY > 0 )
				{
					if ( currentPosition[ 1 ] < sizeY - 1 )
					{
						// just move one more down
						++currentPosition[ 1 ];
					}
					else
					{
						// we have to change columns
						currentPosition[ 0 ] += snakeDirectionX;
						
						// and change the direction of the snake in y
						snakeDirectionY *= -1;
					}
				}
				else
				{
					// currently going up
					if ( currentPosition[ 1 ] > 0 )
					{
						// just move one more up
						--currentPosition[ 1 ];
					}
					else
					{
						// we have to change columns
						currentPosition[ 0 ] += snakeDirectionX;
						
						// and change the direction of the snake in y
						snakeDirectionY *= -1;
					}
				}
			}
		}
	}
	
	public static String getLeadingZeros( final int zeros, final int number )
	{
		String output = "" + number;
		
		while (output.length() < zeros)
			output = "0" + output;
		
		return output;
	}
}
 
