package fiji.plugin;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import spimopener.SPIMRegularStack;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.cell.CellContainerFactory;
import mpicbg.imglib.container.planar.PlanarContainerFactory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.spim.Reconstruction;
import mpicbg.spim.fusion.FusionControl;
import mpicbg.spim.fusion.PreDeconvolutionFusion;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.postprocessing.deconvolution.ExtractPSF;
import mpicbg.spim.postprocessing.deconvolution2.BayesMVDeconvolution;
import mpicbg.spim.postprocessing.deconvolution2.CUDAConvolution;
import mpicbg.spim.postprocessing.deconvolution2.LRFFT;
import mpicbg.spim.postprocessing.deconvolution2.LRFFT.PSFTYPE;
import mpicbg.spim.postprocessing.deconvolution2.LRInput;
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
	public static int psfSize = 17;
	
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
		
		// set the instance to be called
		conf.instance = this;
		
		// run the first part of fusion
		final Reconstruction reconstruction = new Reconstruction( conf );
		
		// reconstruction calls deconvolve for each timepoint
	}
	
	public void deconvolve( final ViewStructure viewStructure, final SPIMConfiguration conf, final int timePoint )
	{
		// get the input images for the deconvolution
		final FusionControl fusionControl = viewStructure.getFusionControl();
		final PreDeconvolutionFusion fusion = (PreDeconvolutionFusion)fusionControl.getFusion();
		
		final int numViews = viewStructure.getNumViews();
		
		// extract the beads
		IJ.log( new Date( System.currentTimeMillis() ) +": Extracting Point spread functions." );
		final ExtractPSF extractPSF = new ExtractPSF( viewStructure, showAveragePSF );
		extractPSF.setPSFSize( psfSize, false );
		extractPSF.extract();
		
		final ArrayList< Image< FloatType > > pointSpreadFunctions = extractPSF.getPSFs();
		
		if ( showAveragePSF )
			ImageJFunctions.show( extractPSF.getMaxProjectionAveragePSF() );
		
		//for ( final Image<FloatType> k : pointSpreadFunctions )
		//mageJFunctions.show( pointSpreadFunctions.get( 0 ) );
		
		// now we close all the input images
		for ( final ViewDataBeads view : viewStructure.getViews() )
			view.closeImage();
		//
		// run the deconvolution
		//
		//final ArrayList<LucyRichardsonFFT> deconvolutionData = new ArrayList<LucyRichardsonFFT>();
		final LRInput deconvolutionData = new LRInput();
		
		IJ.log( "Type of iteration: " + iterationType );
		IJ.log( "Number iterations: " + numIterations );
		IJ.log( "Using blocks: " + useBlocks );
		if ( useBlocks )
			IJ.log( "Block size: " + Util.printCoordinates( blockSize ) );
		IJ.log( "Using CUDA: " + useCUDA );
		
		if ( debugMode )
			IJ.log( "Debugging every " + debugInterval + " iterations." );
		
		IJ.log( "ImgLib container (input): " + conf.outputImageFactory.getClass().getSimpleName() );
		IJ.log( "ImgLib container (output): " + conf.imageFactory.getClass().getSimpleName() );
		
		if ( useTikhonovRegularization )
			IJ.log( "Using Tikhonov regularization (lambda = " + lambda + ")" );
		else
			IJ.log( "Not using Tikhonov regularization" );

		// set debug mode
		BayesMVDeconvolution.debug = debugMode;
		BayesMVDeconvolution.debugInterval = debugInterval;
		
		for ( int view = 0; view < numViews; ++view )
		{
			//ImageJFunctions.show( fusion.getFusedImage( view ) );
			//ImageJFunctions.show( fusion.getWeightImage( view ) );
			//ImageJFunctions.copyToImagePlus( pointSpreadFunctions.get( view ) ).show();

			//deconvolutionData.add( new LucyRichardsonFFT( fusion.getFusedImage( view ), fusion.getWeightImage( view ), pointSpreadFunctions.get( view ), cpusPerView ) );
			final int[] devList = new int[ deviceList.size() ];
			for ( int i = 0; i < devList.length; ++i )
				devList[ i ] = deviceList.get( i );
			
			deconvolutionData.add( new LRFFT( fusion.getFusedImage( view ), fusion.getWeightImage( view ), pointSpreadFunctions.get( view ), devList, useBlocks, blockSize ) );
		}
		
		final Image<FloatType> deconvolved;
		
		// this is influenced a lot by whether normalization is required or not!
		/*
		if ( useTikhonovRegularization )
			deconvolved = LucyRichardsonMultiViewDeconvolution.lucyRichardsonMultiView( deconvolutionData, minNumIterations, maxNumIterations, multiplicative, lambda, paralellViews );
		else
			deconvolved = LucyRichardsonMultiViewDeconvolution.lucyRichardsonMultiView( deconvolutionData, minNumIterations, maxNumIterations, multiplicative, 0, paralellViews );
		*/
		
		if ( useTikhonovRegularization )
			deconvolved = new BayesMVDeconvolution( deconvolutionData, iterationType, numIterations, lambda, "deconvolved" ).getPsi();
		else
			deconvolved = new BayesMVDeconvolution( deconvolutionData, iterationType, numIterations, 0, "deconvolved" ).getPsi();
		
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
				ImageJFunctions.saveAsTiffs( deconvolved, conf.outputdirectory, "DC(l=" + lambda + ")_t" + timePoint + "_ch" + viewStructure.getChannelNum( 0 ), ImageJFunctions.GRAY32 );
		}		
	}

	public static boolean displayFusedImageStatic = true;
	public static boolean saveFusedImageStatic = true;
	public static int defaultNumIterations = 10;
	public static boolean defaultUseTikhonovRegularization = true;
	public static double defaultLambda = 0.006;
	public static boolean showAveragePSF = true;
	public static boolean defaultDebugMode = false;
	public static int defaultDebugInterval = 1;
	public static int defaultIterationType = 1;
	public static int defaultContainer = 0;
	public static int defaultComputationIndex = 0;
	public static int defaultBlockSizeIndex = 0, defaultBlockSizeX = 256, defaultBlockSizeY = 256, defaultBlockSizeZ = 256;
	
	public static String[] iterationTypeString = new String[]{ "Ad-hoc (very fast, imprecise)", "Conditional Probability (fast, precise)", "Independent (slow, precise)" };
	public static String[] imglibContainer = new String[]{ "Array container", "Planar container", "Cell container" };
	public static String[] computationOn = new String[]{ "CPU (Java)", "GPU (Nvidia CUDA via JNA)" };
	public static String[] blocks = new String[]{ "Entire image at once", "in 64x64x64 blocks", "in 128x128x128 blocks", "in 256x256x256 blocks", "in 512x512x512 blocks", "specify maximal blocksize manually" };
	
	PSFTYPE iterationType;
	int numIterations, container, computationType, blockSizeIndex, debugInterval = 1;
	int[] blockSize = null;
	boolean useTikhonovRegularization = true, useBlocks = false, useCUDA = false, debugMode = false;
	
	/**
	 * -1 == CPU
	 * 0 ... n == CUDA device i
	 */
	ArrayList< Integer > deviceList = null;
	
	/**
	 * 0 ... n == index for i'th CUDA device
	 * n + 1 == CPU
	 */
	public static ArrayList< Boolean > deviceChoice = null;
	public static int standardDevice = 10000;
	
	double lambda = 0.006;
	
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
		gd2.addNumericField( "Crop_output_image_offset_x", Multi_View_Fusion.cropOffsetXStatic, 0 );
		gd2.addNumericField( "Crop_output_image_offset_y", Multi_View_Fusion.cropOffsetYStatic, 0 );
		gd2.addNumericField( "Crop_output_image_offset_z", Multi_View_Fusion.cropOffsetZStatic, 0 );
		gd2.addNumericField( "Crop_output_image_size_x", Multi_View_Fusion.cropSizeXStatic, 0 );
		gd2.addNumericField( "Crop_output_image_size_y", Multi_View_Fusion.cropSizeYStatic, 0 );
		gd2.addNumericField( "Crop_output_image_size_z", Multi_View_Fusion.cropSizeZStatic, 0 );
		gd2.addMessage( "" );	
		gd2.addChoice( "Type_of_iteration", iterationTypeString, iterationTypeString[ defaultIterationType ] );
		gd2.addNumericField( "Number_of_iterations", defaultNumIterations, 0 );
		gd2.addCheckbox( "Use_Tikhonov_regularization", defaultUseTikhonovRegularization );
		gd2.addNumericField( "Tikhonov_parameter", defaultLambda, 4 );
		gd2.addChoice( "ImgLib_container", imglibContainer, imglibContainer[ defaultContainer ] );
		gd2.addChoice( "Compute", blocks, blocks[ defaultBlockSizeIndex ] );
		gd2.addChoice( "Compute_on", computationOn, computationOn[ defaultComputationIndex ] );
		gd2.addCheckbox( "Show_averaged_PSF", showAveragePSF );
		gd2.addCheckbox( "Debug_mode", defaultDebugMode );
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
			
			// if the reference is not part of the time series, add it but do not fuse it
			ArrayList< Integer > tpList = null;
			try 
			{
				tpList = SPIMConfiguration.parseIntegerString( conf.timepointPattern );
			} 
			catch (ConfigurationParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				IJ.log( "Cannot parse time-point pattern: " + conf.timepointPattern );
				return null;
			}
			
			if ( !tpList.contains( tp ) )
			{
				conf.timepointPattern += ", " + tp;
				conf.fuseReferenceTimepoint = false;
				
				//System.out.println( "new tp: '" + conf.timepointPattern + "'" );
				
				if ( !Bead_Registration.init( conf ) )
					return null;
			}
			else
			{
				//System.out.println( "old tp: '" + conf.timepointPattern + "'" );
			}
		}
		
		//IOFunctions.println( "tp " + tp );
		
		Multi_View_Fusion.cropOffsetXStatic = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropOffsetYStatic = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropOffsetZStatic = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropSizeXStatic  = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropSizeYStatic = (int)Math.round( gd2.getNextNumber() );
		Multi_View_Fusion.cropSizeZStatic = (int)Math.round( gd2.getNextNumber() );
		
		defaultIterationType = gd2.getNextChoiceIndex();
		
		if ( defaultIterationType == 0 )
			iterationType = PSFTYPE.EXPONENT;
		else if ( defaultIterationType == 1 )
			iterationType = PSFTYPE.CONDITIONAL;
		else
			iterationType = PSFTYPE.INDEPENDENT;
		
		numIterations = defaultNumIterations = (int)Math.round( gd2.getNextNumber() );
		useTikhonovRegularization = defaultUseTikhonovRegularization = gd2.getNextBoolean();
		lambda = defaultLambda = gd2.getNextNumber();
		container = defaultContainer = gd2.getNextChoiceIndex();
		blockSizeIndex = defaultBlockSizeIndex = gd2.getNextChoiceIndex();
		computationType = defaultComputationIndex = gd2.getNextChoiceIndex();
		showAveragePSF = gd2.getNextBoolean();
		defaultDebugMode = debugMode = gd2.getNextBoolean();
		displayFusedImageStatic = gd2.getNextBoolean(); 
		saveFusedImageStatic = gd2.getNextBoolean();
		
		if ( blockSizeIndex == 0 )
		{
			this.useBlocks = false;
			this.blockSize = null;
		}
		else if ( blockSizeIndex == 1 )
		{
			this.useBlocks = true;
			this.blockSize = new int[]{ 64, 64, 64 };
		}
		else if ( blockSizeIndex == 2 )
		{
			this.useBlocks = true;
			this.blockSize = new int[]{ 128, 128, 128 };
		}
		else if ( blockSizeIndex == 3 )
		{
			this.useBlocks = true;
			this.blockSize = new int[]{ 256, 256, 256 };
		}
		else if ( blockSizeIndex == 4 )
		{
			this.useBlocks = true;
			blockSize = new int[]{ 512, 512, 512 };
		}
		if ( blockSizeIndex == 5 )
		{
			GenericDialog gd3 = new GenericDialog( "Define block sizes" );
			
			gd3.addNumericField( "blocksize_x", defaultBlockSizeX, 0 );
			gd3.addNumericField( "blocksize_y", defaultBlockSizeY, 0 );
			gd3.addNumericField( "blocksize_z", defaultBlockSizeZ, 0 );
			
			gd3.showDialog();
			
			if ( gd2.wasCanceled() )
				return null;
			
			defaultBlockSizeX = Math.max( 1, (int)Math.round( gd3.getNextNumber() ) );
			defaultBlockSizeY = Math.max( 1, (int)Math.round( gd3.getNextNumber() ) );
			defaultBlockSizeZ = Math.max( 1, (int)Math.round( gd3.getNextNumber() ) );

			this.useBlocks = true;
			this.blockSize = new int[]{ defaultBlockSizeX, defaultBlockSizeY, defaultBlockSizeZ };
		}
		
		// we need to popluate the deviceList in any case
		deviceList = new ArrayList<Integer>();
		
		if ( computationType == 0 )
		{
			useCUDA = false;
			deviceList.add( -1 );
		}
		else
		{
			// well, do some testing first
			try
			{
		        //String fijiDir = new File( "names.txt" ).getAbsoluteFile().getParentFile().getAbsolutePath();
		        //IJ.log( "Fiji directory: " + fijiDir );
				//LRFFT.cuda = (CUDAConvolution) Native.loadLibrary( fijiDir  + File.separator + "libConvolution3D_fftCUDAlib.so", CUDAConvolution.class );
				
				// under linux automatically checks lib/linux64
		        LRFFT.cuda = (CUDAConvolution) Native.loadLibrary( "Convolution3D_fftCUDAlib", CUDAConvolution.class );
			}
			catch (UnsatisfiedLinkError e )
			{
				IJ.log( "Cannot find CUDA JNA library: " + e );
				return null;
			}
			
			final int numDevices = LRFFT.cuda.getNumDevicesCUDA();
			
			if ( numDevices == 0 )
			{
				IJ.log( "No CUDA devices detected, only CPU will be available." );
			}
			else
			{
				IJ.log( "numdevices = " + numDevices );
				
				// yes, CUDA is possible
				useCUDA = true;
			}
			
			//
			// get the ID's and functionality of the CUDA GPU's
			//
			final String[] devices = new String[ numDevices ];
			final byte[] name = new byte[ 256 ];
			
			for ( int i = 0; i < numDevices; ++i )
			{		
				LRFFT.cuda.getNameDeviceCUDA( i, name );
				
				devices[ i ] = "GPU " + (i+1) + "/" + numDevices  + ": ";
				for ( final byte b : name )
					if ( b != 0 )
						devices[ i ] = devices[ i ] + (char)b;
				
				devices[ i ].trim();
				
				final long mem = LRFFT.cuda.getMemDeviceCUDA( i );	
				devices[ i ] = devices[ i ] + " (" + mem/(1024*1024) + " MB, CUDA capability " + LRFFT.cuda.getCUDAcomputeCapabilityMajorVersion( i )  + "." + LRFFT.cuda.getCUDAcomputeCapabilityMinorVersion( i ) + ")";
				devices[ i ] = devices[ i ].replaceAll( " ", "_" );
			}
			
			// get the CPU specs
			final String cpuSpecs = "CPU (" + Runtime.getRuntime().availableProcessors() + " cores, " + Runtime.getRuntime().maxMemory()/(1024*1024) + " MB RAM available)";
			
			// if we use blocks, it makes sense to run more than one device
			if ( useBlocks )
			{
				// make a list where all are checked if there is no previous selection
				if ( deviceChoice == null || deviceChoice.size() != devices.length + 1 )
				{
					deviceChoice = new ArrayList<Boolean>( devices.length + 1 );
					for ( int i = 0; i < devices.length; ++i )
						deviceChoice.add( true );
					
					// CPU is by default not checked
					deviceChoice.add( false );
				}
				
				final GenericDialog gdCUDA = new GenericDialog( "Choose CUDA/CPUs devices to use" );
				
				for ( int i = 0; i < devices.length; ++i )
					gdCUDA.addCheckbox( devices[ i ], deviceChoice.get( i ) );
	
				gdCUDA.addCheckbox( cpuSpecs, deviceChoice.get( devices.length ) );			
				gdCUDA.showDialog();
				
				if ( gdCUDA.wasCanceled() )
					return null;
	
				// check all CUDA devices
				for ( int i = 0; i < devices.length; ++i )
				{
					if( gdCUDA.getNextBoolean() )
					{
						deviceList.add( i );
						deviceChoice.set( i , true );
					}
					else
					{
						deviceChoice.set( i , false );
					}
				}
				
				// check the CPUs
				if ( gdCUDA.getNextBoolean() )
				{
					deviceList.add( -1 );
					deviceChoice.set( devices.length , true );
				}
				else
				{
					deviceChoice.set( devices.length , false );				
				}
				
				for ( final int i : deviceList )
				{
					if ( i >= 0 )
						IJ.log( "Using device " + devices[ i ] );
					else if ( i == -1 )
						IJ.log( "Using device " + cpuSpecs );
				}
				
				if ( deviceList.size() == 0 )
				{
					IJ.log( "You selected no device, quitting." );
					return null;
				}
			}
			else
			{
				// only choose one device to run everything at once				
				final GenericDialog gdCUDA = new GenericDialog( "Choose CUDA device" );

				if ( standardDevice >= devices.length )
					standardDevice = devices.length - 1;
				
				gdCUDA.addChoice( "Device", devices, devices[ standardDevice ] );
				
				gdCUDA.showDialog();
			
				if ( gdCUDA.wasCanceled() )
					return null;
				
				deviceList.add( standardDevice = gdCUDA.getNextChoiceIndex() );
				IJ.log( "Using device " + devices[ deviceList.get( 0 ) ] );
			}
		}
		
		if ( debugMode )
		{
			GenericDialog gdDebug = new GenericDialog( "Debug options" );
			gdDebug.addNumericField( "Show debug output every n'th frame, n = ", defaultDebugInterval, 0 );
			gdDebug.showDialog();
			
			if ( gdDebug.wasCanceled() )
				return null;
			
			defaultDebugInterval = debugInterval = (int)Math.round( gdDebug.getNextNumber() );
		}
		
		conf.paralellFusion = false;
		conf.sequentialFusion = false;

		// we need different output and weight images
		conf.multipleImageFusion = false;
		
		if ( displayFusedImageStatic  )
			conf.showOutputImage = true;
		else
			conf.showOutputImage = false;
		
		if ( saveFusedImageStatic )
			conf.writeOutputImage = true;
		else
			conf.writeOutputImage = false;
		
		conf.useLinearBlening = true;
		conf.useGauss = false;
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
