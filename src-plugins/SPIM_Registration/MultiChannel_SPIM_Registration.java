import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import ij.IJ;

import mpicbg.imglib.container.cell.CellContainerFactory;
import mpicbg.spim.Reconstruction;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;

import fiji.util.gui.GenericDialogPlus;

public class MultiChannel_SPIM_Registration extends SPIMRegistrationAbstract
{	
	public static String spimDataDirectoryStatic = "";
	public static String timepointsStatic = "1";
	public static String fileNamePatternStatic = "spim_TL{t}_Angle{a}_Track{c}.lsm";
	public static String anglesStatic = "0-315:45";
	public static String channelsStatic = "0,1";
	public static String beadBrightnessStatic = SPIM_Registration.beadBrightnessList[ 1 ];
	public int beadBrightness;
	public static String fusionMethodStatic = SPIM_Registration.fusionMethodList[ 0 ];
	public int fusionMethod = 0;
	
	public static boolean overrideResStatic = false;
	public static boolean timeLapseRegistrationStatic = false;
	public static boolean loadSegmentationStatic = false;
	public static boolean loadRegistrationStatic = false;
	public static boolean registrationOnlyStatic = false;
	public static boolean displayRegistrationStatic = false;
	public static boolean fusionUseBlendingStatic = true;
	public static boolean fusionUseContentBasedStatic = false;
	public static boolean displayFusedImageStatic = true;

	public static int referenceTimePointStatic = 1;
	public static int outputImageScalingStatic = 1;
	public static int cropOffsetXStatic = 0;
	public static int cropOffsetYStatic = 0;
	public static int cropOffsetZStatic = 0;
	public static int cropSizeXStatic = 0;
	public static int cropSizeYStatic = 0;
	public static int cropSizeZStatic = 0;
	
	public static double xyResStatic = 0.73;
	public static double zResStatic = 2.00;
	
	@Override
	protected GenericDialogPlus createGenericDialogPlus()
	{
		final GenericDialogPlus gd = new GenericDialogPlus( "MultiChannel SPIM Registration" );
		
		gd.addDirectoryField( "SPIM_Data_Directory", spimDataDirectoryStatic );
		gd.addStringField( "Timepoints_to_process", timepointsStatic );
		gd.addStringField( "Pattern_of_SPIM_files", fileNamePatternStatic, 25 );
		gd.addStringField( "Angles_to_process", anglesStatic );
		gd.addStringField( "Channels_to_process", channelsStatic );
		gd.addCheckbox( "Timelapse_registration", timeLapseRegistrationStatic );
		gd.addNumericField( "Reference_Timepoint", referenceTimePointStatic, 0 );
		gd.addCheckbox( "Override_file_dimensions", overrideResStatic );
		gd.addNumericField( "xy_resolution (um/px)", xyResStatic, 3 );
		gd.addNumericField( "z_resolution (um/px)", zResStatic, 3 );		
		gd.addCheckbox( "Load_segmented_beads", loadSegmentationStatic );
		gd.addChoice( "Bead_brightness", SPIM_Registration.beadBrightnessList, beadBrightnessStatic );
		gd.addCheckbox( "Load_registration", loadRegistrationStatic );
		gd.addCheckbox( "Register_only (no fusion)", registrationOnlyStatic );
		gd.addCheckbox( "Display_registration", displayRegistrationStatic );
		gd.addChoice( "Fusion_Method", SPIM_Registration.fusionMethodList, fusionMethodStatic );
		gd.addCheckbox( "Fusion_Use_Blending", fusionUseBlendingStatic );
		gd.addCheckbox( "Fusion_Use_Content_Based_Weightening", fusionUseContentBasedStatic );
		gd.addNumericField( "Output_Image_Scaling", outputImageScalingStatic, 0 );
		gd.addNumericField( "Crop_Offset_Output_Image_X", cropOffsetXStatic, 0 );
		gd.addNumericField( "Crop_Offset_Output_Image_Y", cropOffsetYStatic, 0 );
		gd.addNumericField( "Crop_Offset_Output_Image_Z", cropOffsetZStatic, 0 );
		gd.addNumericField( "Crop_Size_Output_Image_X", cropSizeXStatic, 0 );
		gd.addNumericField( "Crop_Size_Output_Image_Y", cropSizeYStatic, 0 );
		gd.addNumericField( "Crop_Size_Output_Image_Z", cropSizeZStatic, 0 );
		gd.addCheckbox( "Display_fused_image", displayFusedImageStatic );
		return gd;
	}

	@Override
	protected Reconstruction execute()
	{	
		SPIMConfiguration conf = new SPIMConfiguration( );
		
		conf.timepointPattern = timepointsStatic;	
		conf.anglePattern = anglesStatic;
		conf.channelPattern = channelsStatic;
		conf.inputFilePattern = fileNamePatternStatic;
		conf.inputdirectory = spimDataDirectoryStatic;
		
		conf.overrideImageZStretching = overrideResStatic;
		
		if ( conf.overrideImageZStretching )
			conf.zStretching = zResStatic / xyResStatic;
		
		conf.timeLapseRegistration = timeLapseRegistrationStatic;
		conf.referenceTimePoint = referenceTimePointStatic;
		
		conf.readSegmentation = loadSegmentationStatic;
		
		if ( beadBrightness == 0 )
			conf.minPeakValue = 0.001f;
		else if ( beadBrightness == 1 )
			conf.minPeakValue = 0.008f;
		else if ( beadBrightness == 1 )
			conf.minPeakValue = 0.03f;
		else
			conf.minPeakValue = 0.1f;
		
		conf.minInitialPeakValue = conf.minPeakValue/4;
		
		conf.readRegistration = loadRegistrationStatic;
		conf.registerOnly = registrationOnlyStatic;
		conf.displayRegistration = displayRegistrationStatic;
		
		conf.paralellFusion = false;
		conf.sequentialFusion = false;
		conf.multipleImageFusion = false;

		if ( fusionMethod == 0 )
			conf.paralellFusion = true;
		else if ( fusionMethod == 1 )
			conf.sequentialFusion = true;
		else
			conf.multipleImageFusion = true;
		
		if ( conf.timeLapseRegistration || conf.multipleImageFusion || !displayFusedImageStatic )
			conf.showOutputImage = false;
		else
			conf.showOutputImage = true;
		
		conf.useLinearBlening = fusionUseBlendingStatic;
		conf.useGauss = fusionUseContentBasedStatic;
		conf.scale = outputImageScalingStatic;
		conf.cropOffsetX = cropOffsetXStatic;
		conf.cropOffsetY = cropOffsetYStatic;
		conf.cropOffsetZ = cropOffsetZStatic;
		conf.cropSizeX = cropSizeXStatic;
		conf.cropSizeY = cropSizeYStatic;
		conf.cropSizeZ = cropSizeZStatic;
		conf.outputImageFactory = new CellContainerFactory( 256 );		
		
		// check the directory string
		conf.inputdirectory = conf.inputdirectory.replace('\\', '/');
		conf.inputdirectory = conf.inputdirectory.replaceAll( "//", "/" );

		conf.inputdirectory = conf.inputdirectory.trim();
		if (conf.inputdirectory.length() > 0 && !conf.inputdirectory.endsWith("/"))
			conf.inputdirectory = conf.inputdirectory + "/";

		conf.outputdirectory = conf.inputdirectory + "output/";
		conf.registrationFiledirectory = conf.inputdirectory + "registration/";
		
		// variable specific verification
		if (conf.numberOfThreads < 1)
			conf.numberOfThreads = Runtime.getRuntime().availableProcessors();				
		
		if (conf.scaleSpaceNumberOfThreads < 1)
			conf.scaleSpaceNumberOfThreads = Runtime.getRuntime().availableProcessors();				

		try
		{
			conf.getFileNames();
		}
		catch ( ConfigurationParserException e )
		{
			IJ.error( "Cannot parse input: " + e );
			return null;
		}
		
		// set interpolator stuff
		conf.interpolatorFactorOutput.setOutOfBoundsStrategyFactory( conf.strategyFactoryOutput );
		
		// check if directories exist
		File dir = new File(conf.outputdirectory, "");
		if (!dir.exists())
		{
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Creating directory '" + conf.outputdirectory + "'.");
			boolean success = dir.mkdirs();
			if (!success)
			{
				if (!dir.exists())
				{
					IOFunctions.printErr("(" + new Date(System.currentTimeMillis()) + "): Cannot create directory '" + conf.outputdirectory + "', quitting.");
					System.exit(0);
				}				
			}
		}
		
		dir = new File(conf.registrationFiledirectory, "");
		if (!dir.exists())
		{
			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Creating directory '" + conf.registrationFiledirectory + "'.");
			boolean success = dir.mkdirs();
			if (!success)
			{
				if (!dir.exists())
				{
					IOFunctions.printErr("(" + new Date(System.currentTimeMillis()) + "): Cannot create directory '" + conf.registrationFiledirectory + "', quitting.");
					System.exit(0);
				}
			}
		}			

		return new Reconstruction( conf );
	}

	@Override
	protected boolean getParameters( final GenericDialogPlus gd )
	{
		spimDataDirectoryStatic = gd.getNextString();
		timepointsStatic = gd.getNextString();
		fileNamePatternStatic = gd.getNextString();
		anglesStatic = gd.getNextString();
		channelsStatic = gd.getNextString();
		timeLapseRegistrationStatic = gd.getNextBoolean();
		referenceTimePointStatic = (int)Math.round( gd.getNextNumber() );
		
		overrideResStatic = gd.getNextBoolean();
		xyResStatic = gd.getNextNumber();
		zResStatic = gd.getNextNumber();
		
		loadSegmentationStatic = gd.getNextBoolean();
		beadBrightness = gd.getNextChoiceIndex();
		beadBrightnessStatic = SPIM_Registration.beadBrightnessList[ beadBrightness ];
		
		loadRegistrationStatic = gd.getNextBoolean();
		registrationOnlyStatic = gd.getNextBoolean();
		displayRegistrationStatic = gd.getNextBoolean();
		
		fusionMethod = gd.getNextChoiceIndex();
		fusionMethodStatic = SPIM_Registration.fusionMethodList[ fusionMethod ];
		fusionUseBlendingStatic = gd.getNextBoolean();
		fusionUseContentBasedStatic = gd.getNextBoolean();
		outputImageScalingStatic = (int)Math.round( gd.getNextNumber() );
		cropOffsetXStatic = (int)Math.round( gd.getNextNumber() );
		cropOffsetYStatic = (int)Math.round( gd.getNextNumber() );
		cropOffsetZStatic = (int)Math.round( gd.getNextNumber() );
		cropSizeXStatic  = (int)Math.round( gd.getNextNumber() );
		cropSizeYStatic = (int)Math.round( gd.getNextNumber() );
		cropSizeZStatic = (int)Math.round( gd.getNextNumber() );		
		displayFusedImageStatic = gd.getNextBoolean(); 
		
		ArrayList<Integer> channels = null;
		
		try
		{
			channels = SPIMConfiguration.parseIntegerString( channelsStatic );
		}
		catch (ConfigurationParserException e)
		{
			e.printStackTrace();
			channels = null;
		}
		
		if ( channels == null )
		{
			IJ.error( "Error parsing the channel information: '" + channelsStatic + "'" );
			return false;
		}
		
		if ( channels.size() == 0 )
		{
			IJ.error( "At least one channel is required, but is only: '" + channelsStatic + "'" );
			return false;
		}
		
		return true;
	}

}
