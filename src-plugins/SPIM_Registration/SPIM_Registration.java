import java.io.File;
import java.util.Date;

import ij.IJ;

import mpicbg.imglib.container.cell.CellContainerFactory;
import mpicbg.spim.Reconstruction;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;

import fiji.util.gui.GenericDialogPlus;

public class SPIM_Registration extends SPIMRegistrationAbstract
{
	public static String[] beadBrightnessList = { "Very weak", "Weak", "Comparable to Sample", "Strong" };	
	public static String[] fusionMethodList = { "Fuse all images at once", "Fuse images sequentially", "Create independent registered images" };	
	
	public static String spimDataDirectoryStatic = "";
	public static String timepointsStatic = "18";
	public static String fileNamePatternStatic = "spim_TL{t}_Angle{a}.lsm";
	public static String anglesStatic = "0-270:45";
	public static String beadBrightnessStatic = beadBrightnessList[ 1 ];
	public int beadBrightness = 0;
	public static String fusionMethodStatic = fusionMethodList[ 0 ];
	public int fusionMethod = 0;
	
	public static boolean timeLapseRegistrationStatic = false;
	public static boolean loadSegmentationStatic = false;
	public static boolean loadRegistrationStatic = false;
	public static boolean registrationOnlyStatic = false;
	public static boolean displayRegistrationStatic = false;
	public static boolean fusionUseBlendingStatic = true;
	public static boolean fusionUseContentBasedStatic = false;

	public static int referenceTimePointStatic = 1;
	public static int outputImageScalingStatic = 1;
	public static int cropOffsetXStatic = 285;
	public static int cropOffsetYStatic = 353;
	public static int cropOffsetZStatic = 375;
	public static int cropSizeXStatic = 727;
	public static int cropSizeYStatic = 395;
	public static int cropSizeZStatic = 325;
	
	@Override
	protected GenericDialogPlus createGenericDialogPlus()
	{
		final GenericDialogPlus gd = new GenericDialogPlus( "SPIM Registration" );
		
		gd.addDirectoryField( "SPIM_Data_Directory", spimDataDirectoryStatic );
		gd.addStringField( "Timepoints_to_process", timepointsStatic );
		gd.addStringField( "Pattern_of_SPIM_files", fileNamePatternStatic, 25 );
		gd.addStringField( "Angles_to_process", anglesStatic );
		gd.addCheckbox( "Timelapse_registration", timeLapseRegistrationStatic );
		gd.addNumericField( "Reference_Timepoint", referenceTimePointStatic, 0 );
		gd.addMessage( "" );
		gd.addCheckbox( "Load_segmented_beads", loadSegmentationStatic );
		gd.addChoice( "Bead_brightness", beadBrightnessList, beadBrightnessStatic );
		gd.addMessage( "" );		
		gd.addCheckbox( "Load_registration", loadRegistrationStatic );
		gd.addCheckbox( "Register_only (no fusion)", registrationOnlyStatic );
		gd.addCheckbox( "Display_registration", displayRegistrationStatic );
		gd.addMessage( "" );		
		gd.addChoice( "Fusion_Method", fusionMethodList, fusionMethodStatic );
		gd.addCheckbox( "Fusion_Use_Blending", fusionUseBlendingStatic );
		gd.addCheckbox( "Fusion_Use_Content_Based_Weightening", fusionUseContentBasedStatic );
		gd.addNumericField( "Output_Image_Scaling", outputImageScalingStatic, 0 );
		gd.addNumericField( "Crop_Offset_Output_Image_X", cropOffsetXStatic, 0 );
		gd.addNumericField( "Crop_Offset_Output_Image_Y", cropOffsetYStatic, 0 );
		gd.addNumericField( "Crop_Offset_Output_Image_Z", cropOffsetZStatic, 0 );
		gd.addNumericField( "Crop_Size_Output_Image_X", cropSizeXStatic, 0 );
		gd.addNumericField( "Crop_Size_Output_Image_Y", cropSizeYStatic, 0 );
		gd.addNumericField( "Crop_Size_Output_Image_Z", cropSizeZStatic, 0 );

		/*
		//
		// disable timelapse registration if not selected
		//
		final Checkbox timeLapseController = (Checkbox)gd.getCheckboxes().get(0);
		final ArrayList<Component> timeLapseComponents = new ArrayList<Component>();

		timeLapseComponents.add( (Component)gd.getNumericFields().get(0) ); //reference timepoint
		
		enableChannelChoice( timeLapseController, timeLapseComponents );
		
		//
		// disable bead options if beads are loaded
		//
		final Checkbox segmentationController = (Checkbox)gd.getCheckboxes().get(1);
		final ArrayList<Component> segmentationComponents = new ArrayList<Component>();

		segmentationComponents.add( (Component)gd.getChoices().get(0) ); //bead brightness
		
		enableInverseChannelChoice( segmentationController, segmentationComponents );
		
		//
		// disable crop and scaling of output image is not created
		//
		final Checkbox fusionController = (Checkbox)gd.getCheckboxes().get(3);
		final ArrayList<Component> fusionComponents = new ArrayList<Component>();
		
		fusionComponents.add( (Component)gd.getChoices().get(1) ); //fusion method
		fusionComponents.add( (Component)gd.getCheckboxes().get(5) ); //use blending
		fusionComponents.add( (Component)gd.getCheckboxes().get(6) ); //use content based
		fusionComponents.add( (Component)gd.getNumericFields().get(1) ); //scaling
		fusionComponents.add( (Component)gd.getNumericFields().get(2) ); //cropOffsetX
		fusionComponents.add( (Component)gd.getNumericFields().get(3) ); //cropOffsetY
		fusionComponents.add( (Component)gd.getNumericFields().get(4) ); //cropOffsetZ
		fusionComponents.add( (Component)gd.getNumericFields().get(5) ); //cropSizeX
		fusionComponents.add( (Component)gd.getNumericFields().get(6) ); //cropSizeY
		fusionComponents.add( (Component)gd.getNumericFields().get(7) ); //cropSizeZ
		
		enableInverseChannelChoice( fusionController, fusionComponents );
		*/
		
		return gd;
	}
	/*
	private final void enableInverseChannelChoice( final Checkbox controller, final ArrayList<Component> target )
	{
		enableChannelChoice( controller, target, true );
	}

	private final void enableChannelChoice( final Checkbox controller, final ArrayList<Component> target )
	{
		enableChannelChoice( controller, target, false );
	}

	private final void enableChannelChoice( final Checkbox controller, final ArrayList<Component> target, final boolean inverse )
	{		
		changeState( controller, target, inverse );

		controller.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent ie)
			{
				changeState( controller, target, inverse );
			}
		});
	}
	
	private void changeState( final Checkbox controller, final ArrayList<Component> target, final boolean inverse )
	{
		if ( controller.getState() )
		{
			if ( inverse )
				setComponentState( target, false );
			else
				setComponentState( target, true );
		}
		else
		{
			if ( inverse )
				setComponentState( target, true );
			else
				setComponentState( target, false );					
		}		
	}

	private final void setComponentState( final ArrayList<Component> target, final boolean state )
	{
		for ( final Component comp : target )
			comp.setEnabled( state );
	}
	*/

	@Override
	protected Reconstruction execute()
	{
		SPIMConfiguration conf = new SPIMConfiguration( );
		
		conf.timepointPattern = timepointsStatic;	
		conf.anglePattern = anglesStatic;
		conf.inputFilePattern = fileNamePatternStatic;
		conf.inputdirectory = spimDataDirectoryStatic;
		
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
		
		if ( conf.timeLapseRegistration || conf.multipleImageFusion )
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
	protected void getParameters(GenericDialogPlus gd)
	{
		spimDataDirectoryStatic = gd.getNextString();
		timepointsStatic = gd.getNextString();
		fileNamePatternStatic = gd.getNextString();
		anglesStatic = gd.getNextString();
		timeLapseRegistrationStatic = gd.getNextBoolean();
		referenceTimePointStatic = (int)Math.round( gd.getNextNumber() );
		
		loadSegmentationStatic = gd.getNextBoolean();
		beadBrightness = gd.getNextChoiceIndex();
		beadBrightnessStatic = beadBrightnessList[ beadBrightness ];
		
		loadRegistrationStatic = gd.getNextBoolean();
		registrationOnlyStatic = gd.getNextBoolean();
		displayRegistrationStatic = gd.getNextBoolean();
		
		fusionMethod = gd.getNextChoiceIndex();
		fusionMethodStatic = fusionMethodList[ fusionMethod ];
		fusionUseBlendingStatic = gd.getNextBoolean();
		fusionUseContentBasedStatic = gd.getNextBoolean();
		outputImageScalingStatic = (int)Math.round( gd.getNextNumber() );
		cropOffsetXStatic = (int)Math.round( gd.getNextNumber() );
		cropOffsetYStatic = (int)Math.round( gd.getNextNumber() );
		cropOffsetZStatic = (int)Math.round( gd.getNextNumber() );
		cropSizeXStatic  = (int)Math.round( gd.getNextNumber() );
		cropSizeYStatic = (int)Math.round( gd.getNextNumber() );
		cropSizeZStatic = (int)Math.round( gd.getNextNumber() );		
	}

}
