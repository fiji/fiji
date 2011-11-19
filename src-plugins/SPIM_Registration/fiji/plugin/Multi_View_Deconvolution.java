package fiji.plugin;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import fiji.util.gui.GenericDialogPlus;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.cell.CellContainerFactory;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;

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
		final SPIMConfiguration conf = getParameters();
		
		// cancelled
		if ( conf == null )
			return;
		
		conf.readSegmentation = true;
		conf.readRegistration = true;
	}

	public static String allChannels = "0, 1";
	public static String[] fusionMethodList = { "Fuse all views at once", "Fuse views sequentially", "Create independent registered images" };	
	public static int defaultFusionMethod = 0;
	public static boolean fusionUseBlendingStatic = true;
	public static boolean fusionUseContentBasedStatic = false;
	public static boolean displayFusedImageStatic = true;
	public static boolean saveFusedImageStatic = true;
	public static int outputImageScalingStatic = 1;
	public static int cropOffsetXStatic = 0;
	public static int cropOffsetYStatic = 0;
	public static int cropOffsetZStatic = 0;
	public static int cropSizeXStatic = 0;
	public static int cropSizeYStatic = 0;
	public static int cropSizeZStatic = 0;
	
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
		Bead_Registration.fileNamePatternMC = gd.getNextString();
		Bead_Registration.timepoints = gd.getNextString();
		Bead_Registration.angles = gd.getNextString();

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
		
			final String name = conf.file[ 0 ][ c ][ 0 ].getName();			
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
					String query = s.substring( 0, s.length() - 12 );
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
					String query = s.substring( 0, s.length() - 12 );
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

		for ( int c = 0; c < channels.size(); ++c )
			for ( final int i : timepoints.get( c ) )
				IOFunctions.println( c + ": " + i );

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
		gd2.addNumericField( "Downsample_output image n-times", outputImageScalingStatic, 0 );
		gd2.addNumericField( "Crop_output_image_offset_x", cropOffsetXStatic, 0 );
		gd2.addNumericField( "Crop_output_image_offset_y", cropOffsetYStatic, 0 );
		gd2.addNumericField( "Crop_output_image_offset_x", cropOffsetZStatic, 0 );
		gd2.addNumericField( "Crop_output_image_size_x", cropSizeXStatic, 0 );
		gd2.addNumericField( "Crop_output_image_size_y", cropSizeYStatic, 0 );
		gd2.addNumericField( "Crop_output_image_size_z", cropSizeZStatic, 0 );
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
		
		IOFunctions.println( "tp " + tp );
		
		fusionUseContentBasedStatic = gd2.getNextBoolean();
		cropOffsetXStatic = (int)Math.round( gd2.getNextNumber() );
		cropOffsetYStatic = (int)Math.round( gd2.getNextNumber() );
		cropOffsetZStatic = (int)Math.round( gd2.getNextNumber() );
		cropSizeXStatic  = (int)Math.round( gd2.getNextNumber() );
		cropSizeYStatic = (int)Math.round( gd2.getNextNumber() );
		cropSizeZStatic = (int)Math.round( gd2.getNextNumber() );
		displayFusedImageStatic = gd2.getNextBoolean(); 
		saveFusedImageStatic = gd2.getNextBoolean(); 		

		conf.paralellFusion = false;
		conf.sequentialFusion = false;
		conf.multipleImageFusion = false;

		if ( defaultFusionMethod == 0 )
			conf.paralellFusion = true;
		else if ( defaultFusionMethod == 1 )
			conf.sequentialFusion = true;
		else
			conf.multipleImageFusion = true;
		
		if ( conf.timeLapseRegistration || !displayFusedImageStatic  )
			conf.showOutputImage = false;
		else
			conf.showOutputImage = true;
		
		if ( saveFusedImageStatic )
			conf.writeOutputImage = true;
		else
			conf.writeOutputImage = false;
		
		conf.useLinearBlening = true;
		conf.useGauss = fusionUseContentBasedStatic;
		conf.scale = 1;
		conf.cropOffsetX = cropOffsetXStatic;
		conf.cropOffsetY = cropOffsetYStatic;
		conf.cropOffsetZ = cropOffsetZStatic;
		conf.cropSizeX = cropSizeXStatic;
		conf.cropSizeY = cropSizeYStatic;
		conf.cropSizeZ = cropSizeZStatic;
		conf.outputImageFactory = new ArrayContainerFactory();
		
		conf.overrideImageZStretching = true;

		return conf;
	}

}
