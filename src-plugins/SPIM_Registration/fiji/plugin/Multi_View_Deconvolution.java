package fiji.plugin;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.cell.CellContainerFactory;
import mpicbg.imglib.container.planar.PlanarContainerFactory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.Reconstruction;
import mpicbg.spim.fusion.FusionControl;
import mpicbg.spim.fusion.PreDeconvolutionFusion;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.postprocessing.deconvolution.ExtractPSF;
import mpicbg.spim.postprocessing.deconvolution.LucyRichardsonFFT;
import mpicbg.spim.postprocessing.deconvolution.LucyRichardsonMultiViewDeconvolution;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

/**
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 *
 */
public class Multi_View_Deconvolution implements PlugIn
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";

	@Override
	public void run(String arg0) 
	{
		// output to IJ.log
		IOFunctions.printIJLog = true;
		
		final SPIMConfiguration conf = getParameters();
		
		// cancelled
		if ( conf == null )
			return;
		
		conf.readSegmentation = true;
		conf.readRegistration = true;
		
		// we need the individual images back by reference
		conf.isDeconvolution = true;
		
		// run the first part of fusion
		final Reconstruction reconstruction = new Reconstruction( conf );
		
		// get the input images for the deconvolution
		final ViewStructure viewStructure = reconstruction.getCurrentViewStructure();
		final FusionControl fusionControl = viewStructure.getFusionControl();
		final PreDeconvolutionFusion fusion = (PreDeconvolutionFusion)fusionControl.getFusion();
		
		final int numViews = viewStructure.getNumViews();
		
		final ArrayList< Image < FloatType > > images = new ArrayList< Image < FloatType > >();
		final ArrayList< Image < FloatType > > weights = new ArrayList< Image < FloatType > >();
		
		for ( int view = 0; view < numViews; ++view )
		{
			images.add( fusion.getFusedImage( view ) );
			weights.add( fusion.getWeightImage( view ) );
		}
		
		// extract the beads
		IJ.log( new Date( System.currentTimeMillis() ) +": Extracting Point spread functions." );
		final ExtractPSF extractPSF = new ExtractPSF( viewStructure, showAveragePSF );
		extractPSF.setPSFSize( 21, false );
		extractPSF.extract();
		
		final ArrayList< Image< FloatType > > pointSpreadFunctions = extractPSF.getPSFs();
		
		if ( showAveragePSF )
			ImageJFunctions.show( extractPSF.getMaxProjectionAveragePSF() );
		
		// now we close all the input images
		for ( final ViewDataBeads view : viewStructure.getViews() )
			view.closeImage();
		//
		// run the deconvolution
		//
		final ArrayList<LucyRichardsonFFT> deconvolutionData = new ArrayList<LucyRichardsonFFT>();
		final int cpusPerView = Math.min( Runtime.getRuntime().availableProcessors(), Math.round( Runtime.getRuntime().availableProcessors() / (float)paralellViews ) );
		
		IJ.log( "Compute views in paralell: " + paralellViews );
		IJ.log( "CPUs per view: " + cpusPerView );
		IJ.log( "Minimal number iterations: " + minNumIterations );
		IJ.log( "Maximal number iterations: " + maxNumIterations );
		
		IJ.log( "ImgLib container (input): " + conf.outputImageFactory.getClass().getSimpleName() );
		IJ.log( "ImgLib container (output): " + conf.imageFactory.getClass().getSimpleName() );
		
		if ( multiplicative )
			IJ.log( "Using a multiplicative multiview combination scheme." );
		else
			IJ.log( "Using an additive multiview combination scheme." );
		
		if ( useTikhonovRegularization )
			IJ.log( "Using Tikhonov regularization (lambda = " + lambda + ")" );
		else
			IJ.log( "Not using Tikhonov regularization" );
		
		for ( int view = 0; view < numViews; ++view )
		{
			//ImageJFunctions.copyToImagePlus( fusion.getFusedImage( view ) ).show();
			//ImageJFunctions.copyToImagePlus( fusion.getWeightImage( view ) ).show();
			//ImageJFunctions.copyToImagePlus( pointSpreadFunctions.get( view ) ).show();

			deconvolutionData.add( new LucyRichardsonFFT( fusion.getFusedImage( view ), fusion.getWeightImage( view ), pointSpreadFunctions.get( view ), cpusPerView ) );
		}
		
		final Image<FloatType> deconvolved;
		
		if ( useTikhonovRegularization )
			deconvolved = LucyRichardsonMultiViewDeconvolution.lucyRichardsonMultiView( deconvolutionData, minNumIterations, maxNumIterations, multiplicative, lambda, paralellViews );
		else
			deconvolved = LucyRichardsonMultiViewDeconvolution.lucyRichardsonMultiView( deconvolutionData, minNumIterations, maxNumIterations, multiplicative, 0, paralellViews );
				
		if ( conf.writeOutputImage || conf.showOutputImage )
		{
			String name = viewStructure.getSPIMConfiguration().inputFilePattern;			
			String replaceTP = SPIMConfiguration.getReplaceStringTimePoints( name );
			String replaceChannel = SPIMConfiguration.getReplaceStringChannels( name );
			
			if ( replaceTP != null )
				name = name.replace( replaceTP, "" + conf.timepoints[ 0 ] );

			if ( replaceChannel != null )
				name = name.replace( replaceChannel, "" + conf.channelsFuse[ 0 ] );

			deconvolved.setName( "DC(l=" + lambda + ")_" + name );
			
			if ( conf.showOutputImage )
			{
				deconvolved.getDisplay().setMinMax( 0 , 1 );
				ImageJFunctions.copyToImagePlus( deconvolved ).show();
			}

			if ( conf.writeOutputImage )
				ImageJFunctions.saveAsTiffs( deconvolved, conf.outputdirectory, "DC(l=" + lambda + ")_" + name + "_ch" + viewStructure.getChannelNum( 0 ), ImageJFunctions.GRAY32 );
		}
	}

	public static boolean fusionUseContentBasedStatic = false;
	public static boolean displayFusedImageStatic = true;
	public static boolean saveFusedImageStatic = true;
	public static int defaultMinNumIterations = 20;
	public static int defaultMaxNumIterations = 50;
	public static boolean defaultUseTikhonovRegularization = true;
	public static double defaultLambda = 0.006;
	public static int defaultParalellViews = 0;
	public static boolean showAveragePSF = true;
	public static int defaultDeconvolutionScheme = 0;
	public static int defaultContainer = 0;
	
	public static String[] deconvolutionScheme = new String[]{ "Multiplicative", "Additive" };
	public static String[] imglibContainer = new String[]{ "Array container", "Planar container", "Cell container" };
	
	int minNumIterations, maxNumIterations, paralellViews, container;
	boolean useTikhonovRegularization = true;
	double lambda = 0.006;
	boolean multiplicative = true;
	
	protected SPIMConfiguration getParameters() 
	{
		final GenericDialogPlus gd = new GenericDialogPlus( "Multi-View Deconvolution" );
		
		gd.addDirectoryField( "SPIM_data_directory", Bead_Registration.spimDataDirectory );
		gd.addStringField( "Pattern_of_SPIM files", Bead_Registration.fileNamePattern, 25 );
		gd.addStringField( "Timepoints_to_process", Bead_Registration.timepoints );
		gd.addStringField( "Angles to process", Bead_Registration.angles );
		
		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		Bead_Registration.addHyperLinkListener(text, myURL);
		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return null;

		Bead_Registration.spimDataDirectory = gd.getNextString();
		Bead_Registration.fileNamePattern = gd.getNextString();
		Bead_Registration.timepoints = gd.getNextString();
		Bead_Registration.angles = gd.getNextString();

		int numViews = 0;
		
		try
		{
			numViews = SPIMConfiguration.parseIntegerString( Bead_Registration.angles ).size();
		}
		catch (ConfigurationParserException e)
		{
			IOFunctions.printErr( "Cannot understand/parse the channels: " + Bead_Registration.angles );
			return null;
		}
		
		ArrayList<Integer> channels;
		channels = new ArrayList<Integer>();
		channels.add( 0 );
		
		// create the configuration object
		final SPIMConfiguration conf = new SPIMConfiguration();

		conf.timepointPattern = Bead_Registration.timepoints;
		conf.anglePattern = Bead_Registration.angles;
		conf.channelPattern = "";
		conf.channelsToRegister = "";
		conf.channelsToFuse = "";			
		conf.inputFilePattern = Bead_Registration.fileNamePattern;
		conf.inputdirectory = Bead_Registration.spimDataDirectory;
		
		// get filenames and so on...
		if ( !Bead_Registration.init( conf ) )
			return null;
		
		// test which registration files are there for each channel
		// file = new File[ timepoints.length ][ channels.length ][ angles.length ];
		final ArrayList<ArrayList<Integer>> timepoints = new ArrayList<ArrayList<Integer>>();
		int numChoices = 0;
		conf.zStretching = -1;
		
		for ( int c = 0; c < channels.size(); ++c )
		{
			timepoints.add( new ArrayList<Integer>() );
		
			final String name = conf.file[ 0 ][ c ][ 0 ][ 0 ].getName();			
			final File regDir = new File( conf.registrationFiledirectory );
			
			if ( !regDir.isDirectory() )
			{
				IOFunctions.println( conf.registrationFiledirectory + " is not a directory. " );
				return null;
			}
			
			final String entries[] = regDir.list( new FilenameFilter() 
			{
				@Override
				public boolean accept(File directory, String filename) 
				{
					if ( filename.contains( name ) && filename.contains( ".registration" ) )
						return true;
					else 
						return false;
				}
			});
			
			final String entriesBeads[] = regDir.list( new FilenameFilter() 
			{
				@Override
				public boolean accept(File directory, String filename) 
				{
					if ( filename.contains( name ) && filename.contains( ".beads.txt" ) )
						return true;
					else 
						return false;
				}
			});

			for ( final String s : entries )
			{
				if ( s.endsWith( ".registration" ) )
				{
					// does the same file exist ending with .beads.txt?
					String query = s.substring( 0, s.length() - new String( "registration" ).length() );
					boolean isPresent = false;
					for ( final String beadFile : entriesBeads )
						if ( beadFile.contains( query ) )
							isPresent = true;
					
					if ( !timepoints.get( c ).contains( -1 ) && isPresent )
					{
						timepoints.get( c ).add( -1 );
						numChoices++;
					}
				}
				else
				{
					final int timepoint = Integer.parseInt( s.substring( s.indexOf( ".registration.to_" ) + 17, s.length() ) );

					// does the same file exist ending with .beads.txt?
					String query = s.substring( 0, s.length() - new String( "registration.to_" + timepoint ).length() );
					boolean isPresent = false;
					for ( final String beadFile : entriesBeads )
						if ( beadFile.contains( query ) )
							isPresent = true;

					if ( !timepoints.get( c ).contains( timepoint ) && isPresent )
					{
						timepoints.get( c ).add( timepoint );
						numChoices++;
					}
				}
				
				if ( conf.zStretching < 0 )
				{
					conf.zStretching = Multi_View_Fusion.loadZStretching( conf.registrationFiledirectory + s );
					IOFunctions.println( "Z-stretching = " + conf.zStretching );
				}
			}
		}
		
		if ( numChoices == 0 )
		{
			IOFunctions.println( "No bead-based segmentation (*.beads.txt) and/or registration (*.registration) files available." );
			return null;
		}

		//for ( int c = 0; c < channels.size(); ++c )
		//	for ( final int i : timepoints.get( c ) )
		//		IOFunctions.println( c + ": " + i );

		final GenericDialog gd2 = new GenericDialog( "Multi-View Deconvolution" );
		
		// build up choices
		final String[] choices = new String[ numChoices ];
		final int[] suggest = new int[ channels.size() ];

		int firstSuggestion = -1;
		int index = 0;
		for ( int c = 0; c < channels.size(); ++c )
		{
			final ArrayList<Integer> tps = timepoints.get( c );
			
			// no suggestion yet
			suggest[ c ] = -1;
			for ( int i = 0; i < tps.size(); ++i )
			{
				if ( tps.get( i ) == -1 )
					choices[ index ] = "Individual registration of channel " + channels.get( c );
				else
					choices[ index ] = "Time-point registration (reference=" + tps.get( i ) + ") of channel " + channels.get( c );

				if ( suggest[ c ] == -1 )
				{
					suggest[ c ] = index;
					if ( firstSuggestion == -1 )
						firstSuggestion = index;
				}

				index++;
			}
		}
			
		for ( int c = 0; c < channels.size(); ++c )
			if ( suggest[ c ] == -1 )
				suggest[ c ] = firstSuggestion;
		
		for ( int c = 0; c < channels.size(); ++c )
			gd2.addChoice( "Registration for channel " + channels.get( c ), choices, choices[ suggest[ c ] ]);

		gd2.addMessage( "" );
		gd2.addCheckbox( "Apply_content_based_weightening", fusionUseContentBasedStatic );
		gd2.addMessage( "" );
		gd2.addNumericField( "Crop_output_image_offset_x", Multi_View_Fusion.cropOffsetXStatic, 0 );
		gd2.addNumericField( "Crop_output_image_offset_y", Multi_View_Fusion.cropOffsetYStatic, 0 );
		gd2.addNumericField( "Crop_output_image_offset_z", Multi_View_Fusion.cropOffsetZStatic, 0 );
		gd2.addNumericField( "Crop_output_image_size_x", Multi_View_Fusion.cropSizeXStatic, 0 );
		gd2.addNumericField( "Crop_output_image_size_y", Multi_View_Fusion.cropSizeYStatic, 0 );
		gd2.addNumericField( "Crop_output_image_size_z", Multi_View_Fusion.cropSizeZStatic, 0 );
		gd2.addMessage( "" );		
		gd2.addNumericField( "Minimal_number_of_iterations", defaultMinNumIterations, 0 );
		gd2.addNumericField( "Maximal_number_of_iterations", defaultMaxNumIterations, 0 );
		gd2.addChoice( "Type_of_multiview_combination", deconvolutionScheme, deconvolutionScheme[ defaultDeconvolutionScheme ] );
		gd2.addCheckbox( "Use_Tikhonov_regularization", defaultUseTikhonovRegularization );
		gd2.addNumericField( "Tikhonov_parameter", defaultLambda, 4 );
		
		final String[] views = new String[ numViews ];
		views[ 0 ] = "All";
		for ( int v = 1; v < numViews; v++ )
			views[ v ] = "" + v;
		
		if ( defaultParalellViews >= views.length )
			defaultParalellViews = views.length - 1;
		
		gd2.addChoice( "Process_views_in_paralell", views, views[ defaultParalellViews ] );
		gd2.addChoice( "ImgLib_container", imglibContainer, imglibContainer[ defaultContainer ] );
		gd2.addCheckbox( "Show_averaged_PSF", showAveragePSF );
		gd2.addMessage( "" );
		gd2.addCheckbox( "Display_fused_image", displayFusedImageStatic );
		gd2.addCheckbox( "Save_fused_image", saveFusedImageStatic );

		gd2.addMessage("");
		gd2.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		text = (MultiLineLabel) gd2.getMessage();
		Bead_Registration.addHyperLinkListener(text, myURL);

		gd2.showDialog();
		
		if ( gd2.wasCanceled() )
			return null;

		// which channel uses which registration file from which channel to be fused
		final int[][] registrationAssignment = new int[ channels.size() ][ 2 ];

		for ( int c = 0; c < channels.size(); ++c )
		{
			final int choice = gd2.getNextChoiceIndex();
			
			index = 0;
			for ( int c2 = 0; c2 < channels.size(); ++c2 )
			{
				final ArrayList<Integer> tps = timepoints.get( c2 );
				for ( int i = 0; i < tps.size(); ++i )
				{
					if ( index == choice )
					{
						registrationAssignment[ c ][ 0 ] = tps.get( i );
						registrationAssignment[ c ][ 1 ] = c2;
					}
					index++;
				}
			}
		}

		// test consistency
		final int tp = registrationAssignment[ 0 ][ 0 ];
		
		for ( int c = 1; c < channels.size(); ++c )
		{
			if ( tp != registrationAssignment[ c ][ 0 ] )
			{
				IOFunctions.println( "Inconsistent choice of reference timeseries, only same reference timepoints or individual registration are allowed.");
				return null;
			}
		}
		
		// save from which channel to load registration
		conf.registrationAssignmentForFusion = new int[ channels.size() ];
		for ( int c = 0; c < channels.size(); ++c )
		{
			IOFunctions.println( "channel " + c + " takes it from channel " + registrationAssignment[ c ][ 1 ] );
			conf.registrationAssignmentForFusion[ c ] = registrationAssignment[ c ][ 1 ];
		}
		
		if ( tp >= 0 )
		{
			conf.timeLapseRegistration = true;
			conf.referenceTimePoint = tp;
		}
		
		//IOFunctions.println( "tp " + tp );
		
		fusionUseContentBasedStatic = gd2.getNextBoolean();
		Multi_View_Fusion.cropOffsetXStatic = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropOffsetYStatic = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropOffsetZStatic = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropSizeXStatic  = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropSizeYStatic = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropSizeZStatic = (int)Math.round( gd2.getNextNumber() );
		
		minNumIterations = defaultMinNumIterations = (int)Math.round( gd2.getNextNumber() );
		maxNumIterations = defaultMaxNumIterations = (int)Math.round( gd2.getNextNumber() );
		defaultDeconvolutionScheme = gd2.getNextChoiceIndex();
		if ( defaultDeconvolutionScheme == 0 )
			multiplicative = true;
		else
			multiplicative = false;
		useTikhonovRegularization = defaultUseTikhonovRegularization = gd2.getNextBoolean();
		lambda = defaultLambda = gd2.getNextNumber();
		paralellViews = defaultParalellViews = gd2.getNextChoiceIndex(); // 0 = all
		if ( paralellViews == 0 )
			paralellViews = numViews;
		container = defaultContainer = gd2.getNextChoiceIndex();
		showAveragePSF = gd2.getNextBoolean();
		displayFusedImageStatic = gd2.getNextBoolean(); 
		saveFusedImageStatic = gd2.getNextBoolean(); 		

		conf.paralellFusion = false;
		conf.sequentialFusion = false;
		conf.multipleImageFusion = false;

		// we need different output and weight images
		conf.multipleImageFusion = true;
		
		if ( displayFusedImageStatic  )
			conf.showOutputImage = true;
		else
			conf.showOutputImage = false;
		
		if ( saveFusedImageStatic )
			conf.writeOutputImage = true;
		else
			conf.writeOutputImage = false;
		
		conf.useLinearBlening = true;
		conf.useGauss = fusionUseContentBasedStatic;
		conf.scale = 1;
		conf.cropOffsetX = Multi_View_Fusion.cropOffsetXStatic;
		conf.cropOffsetY = Multi_View_Fusion.cropOffsetYStatic;
		conf.cropOffsetZ = Multi_View_Fusion.cropOffsetZStatic;
		conf.cropSizeX = Multi_View_Fusion.cropSizeXStatic;
		conf.cropSizeY = Multi_View_Fusion.cropSizeYStatic;
		conf.cropSizeZ = Multi_View_Fusion.cropSizeZStatic;
		
		if ( container == 1 )
		{
			conf.outputImageFactory = new PlanarContainerFactory();
			conf.imageFactory = new PlanarContainerFactory();
		}
		else if ( container == 2 )
		{
			conf.outputImageFactory = new CellContainerFactory( 256 );
			conf.imageFactory = new CellContainerFactory( 256 );
		}
		else
		{
			conf.outputImageFactory = new ArrayContainerFactory();
			conf.imageFactory = new ArrayContainerFactory();
		}
		
		conf.overrideImageZStretching = true;

		return conf;
	}

}
