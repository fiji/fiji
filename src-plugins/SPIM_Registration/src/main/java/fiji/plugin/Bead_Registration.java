package fiji.plugin;

import fiji.Debug;
import fiji.plugin.timelapsedisplay.GraphFrame;
import fiji.plugin.timelapsedisplay.TimeLapseDisplay;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.TextField;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.TextEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.io.LOCI;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.Reconstruction;
import mpicbg.spim.io.ConfigurationParserException;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.segmentation.InteractiveDoG;
import spimopener.SPIMExperiment;

public class Bead_Registration implements PlugIn 
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	final private String paperURL = "http://www.nature.com/nmeth/journal/v7/n6/full/nmeth0610-418.html";

	final String beadRegistration[] = new String[] { "Single-channel", "Multi-channel (same beads visible in different channels)" };
	static int defaultBeadRegistration = 0;
	
	@Override
	public void run(String arg0) 
	{
		// output to IJ.log
		IOFunctions.printIJLog = true;
		
		final GenericDialog gd = new GenericDialog( "Bead based registration" );
		
		gd.addChoice( "Select type of registration", beadRegistration, beadRegistration[ defaultBeadRegistration ] );		
		gd.addMessage( "Please note that the SPIM Registration is based on a publication.\n" +
						"If you use it successfully for your research please be so kind to cite our work:\n" +
						"Preibisch et al., Nature Methods (2010), 7(6):418-419\n" );

		MultiLineLabel text =  (MultiLineLabel) gd.getMessage();
		addHyperLinkListener( text, paperURL );

		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		final int choice = gd.getNextChoiceIndex();
		defaultBeadRegistration = choice;
		
		final SPIMConfiguration conf;

		if ( choice == 0 )
			conf = singleChannel();
		else
			conf = multiChannel();
		
		// cancelled
		if ( conf == null )
			return;

		// get filenames and so on...
		if ( !init( conf ) )
			return;

		// this is only registration
		conf.registerOnly = true;

		// if we do not do timelapseregistration we can just go ahead and
		// display the result if wanted
		if ( !timeLapseRegistration )
		{
			conf.timeLapseRegistration = false;
			conf.collectRegistrationStatistics = true;

			final Reconstruction reconstruction = new Reconstruction( conf );

			if ( reconstruction.getSPIMConfiguration().file.length > 1 && defaultTimeLapseRegistration == 0 )
				TimeLapseDisplay.plotData( reconstruction.getRegistrationStatistics(), -1, false );
		}
		else
		{
			// now compute or load the inter-timepoint registration
			conf.timeLapseRegistration = false;
			conf.collectRegistrationStatistics = true;

			// compute per-timepoint registration
			Reconstruction reconstruction = new Reconstruction( conf );
			
			if ( defaultTimeLapseRegistration == 0 )
			{
				// manually select timepoint
				final GraphFrame gf = TimeLapseDisplay.plotData( reconstruction.getRegistrationStatistics(), -1, true );
				
				int lastRefTP = -1;
				do
				{
					lastRefTP = gf.getReferenceTimePoint();
					SimpleMultiThreading.threadWait( 1000 );
				}
				while ( gf.getReferenceTimePoint() == lastRefTP );
				
				conf.referenceTimePoint =  gf.getReferenceTimePoint();
			}
			else
			{
				// automatically select, but still show the display
				conf.referenceTimePoint = TimeLapseDisplay.getOptimalTimePoint( reconstruction.getRegistrationStatistics() );
				TimeLapseDisplay.plotData( reconstruction.getRegistrationStatistics(), conf.referenceTimePoint, false );
			}
			
			// and now the timelapse-registration
			conf.timeLapseRegistration = true;
			conf.readRegistration = false;
			reconstruction = new Reconstruction( conf );			
		}
	}

	public static String spimDataDirectory = "";
	public static String timepoints = "18";
	public static String fileNamePattern = "spim_TL{t}_Angle{a}.lsm";
	public static String angles = "0-270:45";
	public static boolean processAnglesSimultaneously = false;
	
	public static boolean loadSegmentation = false;
	public static String[] beadBrightness = { "Very weak", "Weak", "Comparable to Sample", "Strong", "Advanced ...", "Interactive ..." };	
	public static int defaultBeadBrightness = 1;
	public static boolean overrideResolution = false;
	public static double xyRes = 0.73;
	public static double zRes = 2;

	final String model[] = new String[] { "Translation", "Rigid", "Affine" };
	public static int defaultModel = 2;
	public static boolean loadRegistration = false;
	public static boolean timeLapseRegistration = false;
	final String timeLapseRegistrationTypes[] = new String[] { "Manually", "Automatically" };
	static int defaultTimeLapseRegistration = 0;
	
	private SPIMConfiguration conf;

	public SPIMConfiguration singleChannel()
	{
		conf = null;
		final GenericDialogPlus gd = new GenericDialogPlus( "Single Channel Bead-based Registration" );
		
		gd.addDirectoryOrFileField( "SPIM_data_directory", spimDataDirectory );
		final TextField tfSpimDataDirectory = (TextField) gd.getStringFields().lastElement();
		gd.addStringField( "Pattern_of_SPIM files", fileNamePattern, 25 );
		final TextField tfFilePattern = (TextField) gd.getStringFields().lastElement();
		gd.addStringField( "Timepoints_to_process", timepoints );
		final TextField tfTimepoints = (TextField) gd.getStringFields().lastElement();
		gd.addStringField( "Angles_to_process", angles );
		final TextField tfAngles = (TextField) gd.getStringFields().lastElement();
		//gd.addCheckbox( "Process_angles_simultaneously (needs lots of RAM)", processAnglesSimultaneously );
		
		gd.addMessage( "" );		
		
		gd.addCheckbox( "Re-use_segmented_beads", loadSegmentation );
		gd.addChoice( "Bead_brightness", beadBrightness, beadBrightness[ defaultBeadBrightness ] );
		gd.addCheckbox( "Override_file_dimensions", overrideResolution );
		final Checkbox dimensionsBox = (Checkbox)gd.getCheckboxes().lastElement();
		gd.addNumericField( "xy_resolution (um/px)", xyRes, 3 );
		final TextField tfXyRes = (TextField) gd.getNumericFields().lastElement();
		gd.addNumericField( "z_resolution (um/px)", zRes, 3 );
		final TextField tfZRes = (TextField) gd.getNumericFields().lastElement();
		
		gd.addMessage( "" );		
		
		gd.addChoice( "Transformation_model", model, model[ defaultModel ] );
		gd.addCheckbox( "Re-use_per_timepoint_registration", loadRegistration );

		gd.addMessage( "" );		

		gd.addCheckbox( "Timelapse_registration", timeLapseRegistration );
		gd.addChoice( "Select_reference timepoint", timeLapseRegistrationTypes, timeLapseRegistrationTypes[ defaultTimeLapseRegistration ] );

		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener(text, myURL);

		gd.addDialogListener( new DialogListener()
		{
			@Override
			public boolean dialogItemChanged( GenericDialog dialog, AWTEvent e )
			{
				if ( e == null )
				{
					if ( !gd.wasCanceled() )
					{
						conf = getConfigurationSingleChannel( dialog );
					}
					return true;
				}
				if ( e instanceof TextEvent && e.getID() == TextEvent.TEXT_VALUE_CHANGED && e.getSource() == tfSpimDataDirectory )
				{
					TextField tf = ( TextField ) e.getSource();
					spimDataDirectory = tf.getText();
					File f = new File( spimDataDirectory );
					if ( f.exists() && f.isFile() && f.getName().endsWith( ".xml" ) )
					{
						SPIMExperiment exp = new SPIMExperiment( f.getAbsolutePath() );

						// disable file pattern field
						tfFilePattern.setEnabled( false );

						// set timepoint string
						String expTimepoints = "";
						if ( exp.timepointStart == exp.timepointEnd )
							expTimepoints = "" + exp.timepointStart;
						else
							expTimepoints = "" + exp.timepointStart + "-" + exp.timepointEnd;
						tfTimepoints.setText( expTimepoints );

						// set angles string
						String expAngles = "";
						for ( String angle : exp.angles )
						{
							int a = Integer.parseInt( angle.substring( 1, angle.length() ) );
							if ( !expAngles.equals( "" ) )
								expAngles += ",";
							expAngles += a;
						}
						tfAngles.setText( expAngles );

						// set dimension fields
						if ( exp.pw != exp.ph )
							IJ.log( "Warning: pixel width != pixel height in " + spimDataDirectory );
						dimensionsBox.setState( true );
						tfXyRes.setText( String.format( "%.3f", exp.pw ) );
						tfZRes.setText( String.format( "%.3f", exp.pd ) );
					}
					else
					{
						// enable file pattern field
						tfFilePattern.setEnabled( true );
					}
				}
				return true;
			}
		} );
		System.out.println( "init Bead_Registration dialog: tfSpimDataDirectory.getText() = " + tfSpimDataDirectory.getText() );
		File f = new File( tfSpimDataDirectory.getText() );
		if ( f.exists() && f.isFile() && f.getName().endsWith( ".xml" ) )
		{
			// disable file pattern field
			tfFilePattern.setEnabled( false );
		}
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return null;

		return conf;
	}

	private static SPIMConfiguration getConfigurationSingleChannel( final GenericDialog gd )
	{
		spimDataDirectory = gd.getNextString();
		fileNamePattern = gd.getNextString();
		timepoints = gd.getNextString();
		angles = gd.getNextString();
		//processAnglesSimultaneously = gd.getNextBoolean();
		
		loadSegmentation = gd.getNextBoolean();
		defaultBeadBrightness = gd.getNextChoiceIndex();
		overrideResolution = gd.getNextBoolean();
		xyRes = gd.getNextNumber();
		zRes = gd.getNextNumber();
		
		defaultModel = gd.getNextChoiceIndex();
		loadRegistration = gd.getNextBoolean();
		
		timeLapseRegistration = gd.getNextBoolean();
		defaultTimeLapseRegistration = gd.getNextChoiceIndex();
		
		SPIMConfiguration conf = new SPIMConfiguration();
		
		if ( conf.initialSigma == null || conf.initialSigma.length != 1 )
			conf.initialSigma = new float[]{ 1.8f };

		if ( conf.minPeakValue == null || conf.minPeakValue.length != 1 )
			conf.minPeakValue = new float[]{ 0.01f };

		if ( !loadSegmentation )
		{
			if ( defaultBeadBrightness == 0 )
				conf.minPeakValue[ 0 ] = 0.001f;
			else if ( defaultBeadBrightness == 1 )
				conf.minPeakValue[ 0 ] = 0.008f;
			else if ( defaultBeadBrightness == 2 )
				conf.minPeakValue[ 0 ] = 0.03f;
			else if ( defaultBeadBrightness == 3 )
				conf.minPeakValue[ 0 ] = 0.1f;
			else
			{
				// open advanced bead brightness detection
				final double[] values;

				if ( defaultBeadBrightness == 4 )
					values = getAdvancedDoGParameters( new int[ 1 ] )[ 0 ];
				else
				{
					values = new double[]{ conf.initialSigma[ 0 ], conf.minPeakValue[ 0 ] };
					getInteractiveDoGParameters( "Select view to analyze", values );
				}

				// cancelled
				if ( values == null )
					return null;

				conf.initialSigma[ 0 ] = (float)values[ 0 ];
				conf.minPeakValue[ 0 ] = (float)values[ 1 ];
			}
		}
		conf.minInitialPeakValue = new float[]{ conf.minPeakValue[ 0 ]/4 };

		conf.timepointPattern = timepoints;
		conf.channelPattern = "";
		conf.channelsToRegister = "";
		conf.channelsToFuse = "";
		conf.anglePattern = angles;
		conf.multiThreadedOpening = processAnglesSimultaneously;
		conf.inputFilePattern = fileNamePattern;

		File f = new File( spimDataDirectory );
		if ( f.exists() && f.isFile() && f.getName().endsWith( ".xml" ) )
		{
			conf.spimExperiment = new SPIMExperiment( f.getAbsolutePath() );
			conf.inputdirectory = f.getAbsolutePath().substring( 0, f.getAbsolutePath().length() - 4 );
			System.out.println( "inputdirectory : " + conf.inputdirectory );
		}
		else
		{		
			conf.inputdirectory = spimDataDirectory;
		}

		conf.overrideImageZStretching = overrideResolution;

		if ( overrideResolution )
			conf.zStretching = zRes / xyRes;

		conf.readSegmentation = loadSegmentation;
		conf.readRegistration = loadRegistration;

		if ( defaultModel == 0 )
		{
			conf.transformationModel = "Translation";
			conf.max_epsilon = 10;
			conf.numIterations = 10000;
		}
		else if ( defaultModel == 1 )
		{
			conf.transformationModel = "Rigid";
			conf.max_epsilon = 7;
			conf.numIterations = 10000;
		}
		else
		{
			conf.transformationModel = "Affine";
		}
		
		conf.registerOnly = true;
		conf.timeLapseRegistration = timeLapseRegistration;
		
		return conf;
	}

	public static String fileNamePatternMC = "spim_TL{t}_Channel{c}_Angle{a}.lsm";
	public static String channelsBeadsMC = "0, 1";
	public static int[] defaultBeadBrightnessMC = null;

	public SPIMConfiguration multiChannel()
	{
		conf = null;
		// The first main dialog
		final GenericDialogPlus gd = new GenericDialogPlus( "Multi Channel Bead-based Registration" );

		gd.addDirectoryOrFileField( "SPIM_data_directory", spimDataDirectory );
		final TextField tfSpimDataDirectory = (TextField) gd.getStringFields().lastElement();
		gd.addStringField( "Pattern_of_SPIM files", fileNamePatternMC, 25 );
		final TextField tfFilePattern = (TextField) gd.getStringFields().lastElement();
		gd.addStringField( "Timepoints_to_process", timepoints );
		final TextField tfTimepoints = (TextField) gd.getStringFields().lastElement();
		gd.addStringField( "Channels_containing_beads", channelsBeadsMC );
		final TextField tfBeadChannels = (TextField) gd.getStringFields().lastElement();
		gd.addStringField( "Angles to process", angles );
		final TextField tfAngles = (TextField) gd.getStringFields().lastElement();
		//gd.addCheckbox( "Process_angles_simultaneously (needs lots of RAM)", processAnglesSimultaneously );
		
		gd.addMessage( "" );

		gd.addCheckbox( "Re-use_segmented_beads", loadSegmentation );
		gd.addCheckbox( "Override_file_dimensions", overrideResolution );
		final Checkbox dimensionsBox = (Checkbox)gd.getCheckboxes().lastElement();
		gd.addNumericField( "xy_resolution (um/px)", xyRes, 3 );
		final TextField tfXyRes = (TextField) gd.getNumericFields().lastElement();
		gd.addNumericField( "z_resolution (um/px)", zRes, 3 );
		final TextField tfZRes = (TextField) gd.getNumericFields().lastElement();

		gd.addMessage( "" );

		gd.addChoice( "Transformation_model", model, model[ defaultBeadBrightness ] );
		gd.addCheckbox( "Re-use_per_timepoint_registration", loadRegistration );

		gd.addMessage( "" );

		gd.addCheckbox( "Timelapse_registration", timeLapseRegistration );
		gd.addChoice( "Select_reference timepoint", timeLapseRegistrationTypes, timeLapseRegistrationTypes[ defaultTimeLapseRegistration ] );

		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		final MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		addHyperLinkListener(text, myURL);

		gd.addDialogListener( new DialogListener()
		{
			@Override
			public boolean dialogItemChanged( final GenericDialog dialog, final AWTEvent e )
			{
				if ( e == null )
				{
					if ( !gd.wasCanceled() )
					{
						conf = getConfigurationMultiChannel( dialog );
					}
					return true;
				}
				if ( e instanceof TextEvent && e.getID() == TextEvent.TEXT_VALUE_CHANGED && e.getSource() == tfSpimDataDirectory )
				{
					final TextField tf = ( TextField ) e.getSource();
					spimDataDirectory = tf.getText();
					final File f = new File( spimDataDirectory );
					if ( f.exists() && f.isFile() && f.getName().endsWith( ".xml" ) )
					{
						final SPIMExperiment exp = new SPIMExperiment( f.getAbsolutePath() );

						// disable file pattern field
						tfFilePattern.setEnabled( false );

						// set timepoint string
						String expTimepoints = "";
						if ( exp.timepointStart == exp.timepointEnd )
							expTimepoints = "" + exp.timepointStart;
						else
							expTimepoints = "" + exp.timepointStart + "-" + exp.timepointEnd;
						tfTimepoints.setText( expTimepoints );

						// set channels string
						String expChannels = "";
						for ( final String channel : exp.channels )
						{
							final int c = Integer.parseInt( channel.substring( 1, channel.length() ) );
							if ( !expChannels.equals( "" ) )
								expChannels += ",";
							expChannels += c;
						}
						tfBeadChannels.setText( expChannels );

						// set angles string
						String expAngles = "";
						for ( final String angle : exp.angles )
						{
							final int a = Integer.parseInt( angle.substring( 1, angle.length() ) );
							if ( !expAngles.equals( "" ) )
								expAngles += ",";
							expAngles += a;
						}
						tfAngles.setText( expAngles );

						// set dimension fields
						if ( exp.pw != exp.ph )
							IJ.log( "Warning: pixel width != pixel height in " + spimDataDirectory );
						dimensionsBox.setState( true );
						tfXyRes.setText( String.format( "%.3f", exp.pw ) );
						tfZRes.setText( String.format( "%.3f", exp.pd ) );
					}
					else
					{
						// enable file pattern field
						tfFilePattern.setEnabled( true );
					}
				}
				return true;
			}
		} );
		System.out.println( "init Bead_Registration dialog: tfSpimDataDirectory.getText() = " + tfSpimDataDirectory.getText() );
		final File f = new File( tfSpimDataDirectory.getText() );
		if ( f.exists() && f.isFile() && f.getName().endsWith( ".xml" ) )
		{
			// disable file pattern field
			tfFilePattern.setEnabled( false );
		}
		gd.showDialog();

		if ( gd.wasCanceled() )
			return null;

		return conf;
	}

	private static SPIMConfiguration getConfigurationMultiChannel( final GenericDialog gd )
	{
		spimDataDirectory = gd.getNextString();
		fileNamePatternMC = gd.getNextString();
		timepoints = gd.getNextString();
		channelsBeadsMC = gd.getNextString();
		angles = gd.getNextString();
		//processAnglesSimultaneously = gd.getNextBoolean();
		
		loadSegmentation = gd.getNextBoolean();
		overrideResolution = gd.getNextBoolean();
		xyRes = gd.getNextNumber();
		zRes = gd.getNextNumber();

		defaultModel = gd.getNextChoiceIndex();
		loadRegistration = gd.getNextBoolean();

		timeLapseRegistration = gd.getNextBoolean();
		defaultTimeLapseRegistration = gd.getNextChoiceIndex();


		// check if channels are more or less ok
		int numChannels = 0;
		ArrayList<Integer> channels;
		try
		{
			channels = SPIMConfiguration.parseIntegerString( channelsBeadsMC );
			numChannels = channels.size();
		}
		catch (ConfigurationParserException e)
		{
			IOFunctions.printErr( "Cannot understand/parse the channels: " + channelsBeadsMC );
			return null;
		}

		if ( numChannels < 1 )
		{
			IOFunctions.printErr( "There are no channels given: " + channelsBeadsMC );
			return null;
		}

		// create the configuration object
		final SPIMConfiguration conf = new SPIMConfiguration();

		if ( conf.initialSigma == null || conf.initialSigma.length != numChannels )
		{
			conf.initialSigma = new float[ numChannels ];
			for ( int c = 0; c < numChannels; ++c )
				conf.initialSigma[ c ] = 1.8f;
		}

		if ( conf.minPeakValue == null || conf.minPeakValue.length != numChannels )
		{
			conf.minPeakValue = new float[ numChannels ];
			for ( int c = 0; c < numChannels; ++c )
				conf.minPeakValue[ c ] = 0.01f;
		}

		if ( conf.minInitialPeakValue == null || conf.minInitialPeakValue.length != numChannels )
		{
			conf.minInitialPeakValue = new float[ numChannels ];
			for ( int c = 0; c < numChannels; ++c )
				conf.minInitialPeakValue[ c ] = conf.minPeakValue[ c ] / 4;
		}

		// if not segmentation and registration are loaded ask the parameters
		// individually for each channel
		if ( !loadSegmentation && !loadRegistration )
		{
			if ( defaultBeadBrightnessMC == null || defaultBeadBrightness != numChannels )
			{
				defaultBeadBrightnessMC = new int[ numChannels ];
				for ( int c = 0; c < numChannels; ++c )
					defaultBeadBrightnessMC[ c ] = 1;
			}

			final GenericDialogPlus gd2 = new GenericDialogPlus( "Bead Brightness for Multi Channel Registration" );

			for ( int c = 0; c < numChannels; ++c )
				gd2.addChoice( "Bead_brightness_channel_" + channels.get( c ), beadBrightness, beadBrightness[ defaultBeadBrightnessMC[ c ] ] );

			gd2.showDialog();

			if ( gd2.wasCanceled() )
				return null;

			int advanced = 0;
			int interactive = 0;

			for ( int c = 0; c < numChannels; ++c )
			{
				defaultBeadBrightnessMC[ c ] = gd2.getNextChoiceIndex();

				if ( defaultBeadBrightnessMC[ c ] == 0 )
					conf.minPeakValue[ c ] = 0.001f;
				else if ( defaultBeadBrightnessMC[ c ] == 1 )
					conf.minPeakValue[ c ] = 0.008f;
				else if ( defaultBeadBrightnessMC[ c ] == 2 )
					conf.minPeakValue[ c ] = 0.03f;
				else if ( defaultBeadBrightnessMC[ c ] == 3 )
					conf.minPeakValue[ c ] = 0.1f;
				else if ( defaultBeadBrightnessMC[ c ] == 4 )
					advanced++;
				else
					interactive++;
			}

			// get the interactive values for all channels
			if ( interactive > 0 )
				for ( int c = 0; c < numChannels; ++c )
					if ( defaultBeadBrightnessMC[ c ] == 5 )
					{
						final double[] values = new double[] { conf.initialSigma[ c ], conf.minPeakValue[ c ] };

						getInteractiveDoGParameters( "Select view to analyze for channel " + channels.get( c ), values );

						conf.initialSigma[ c ] = (float)values[ 0 ];
						conf.minPeakValue[ c ] = (float)values[ 1 ];
					}

			// get the advanced values for all channels
			if ( advanced > 0 )
			{
				final int channelIndices[] = new int[ advanced ];
				int count = 0;

				// do all advanced parameters in one dialog
				for ( int c = 0; c < numChannels; ++c )
					if ( defaultBeadBrightnessMC[ c ] == 4 )
						channelIndices[ count++ ] = channels.get( c );

				final double[][] values = getAdvancedDoGParameters( channelIndices );

				// write them to the configuration
				count = 0;
				for ( int c = 0; c < numChannels; ++c )
					if ( defaultBeadBrightnessMC[ c ] == 4 )
					{
						conf.initialSigma[ c ] = (float)values[ count ][ 0 ];
						conf.minPeakValue[ c ] = (float)values[ count++ ][ 1 ];
					}
			}
		}

		for ( int c = 0; c < numChannels; ++c )
			conf.minInitialPeakValue[ c ] = conf.minPeakValue[ c ] / 4;

		conf.timepointPattern = timepoints;
		conf.anglePattern = angles;
		conf.multiThreadedOpening = processAnglesSimultaneously;
		conf.channelPattern = channelsBeadsMC;
		conf.channelsToRegister = channelsBeadsMC;
		conf.channelsToFuse = "";
		conf.inputFilePattern = fileNamePatternMC;

		final File f = new File( spimDataDirectory );
		if ( f.exists() && f.isFile() && f.getName().endsWith( ".xml" ) )
		{
			conf.spimExperiment = new SPIMExperiment( f.getAbsolutePath() );
			conf.inputdirectory = f.getAbsolutePath().substring( 0, f.getAbsolutePath().length() - 4 );
			System.out.println( "inputdirectory : " + conf.inputdirectory );
		}
		else
		{
			conf.inputdirectory = spimDataDirectory;
		}

		conf.overrideImageZStretching = overrideResolution;

		if ( overrideResolution )
			conf.zStretching = zRes / xyRes;

		conf.readSegmentation = loadSegmentation;
		conf.readRegistration = loadRegistration;
		
		if ( defaultModel == 0 )
		{
			conf.transformationModel = "Translation";
			conf.max_epsilon = 10;
			conf.numIterations = 10000;
		}
		else if ( defaultModel == 1 )
		{
			conf.transformationModel = "Rigid";
			conf.max_epsilon = 7;
			conf.numIterations = 10000;
		}
		else
		{
			conf.transformationModel = "Affine";
		}

		conf.registerOnly = true;
		conf.timeLapseRegistration = timeLapseRegistration;

		return conf;
	}
	
	static double[][] dogParameters = null;
	
	public static double[][] getAdvancedDoGParameters( final int[] channelIndices )
	{
		if ( channelIndices == null || channelIndices.length == 0 )
			return null;
		
		if ( dogParameters == null || dogParameters.length != channelIndices.length )
		{
			dogParameters = new double[ channelIndices.length ][ 2 ];
			
			for ( final double dog[] : dogParameters )
			{
				dog[ 0 ] = 1.8;
				dog[ 1 ] = 0.008;
			}
		}

		final GenericDialog gd = new GenericDialog( "Select Difference-of-Gaussian Parameters" );
		
		for ( int i = 0; i < channelIndices.length; ++i )
		{
			final int channel = channelIndices[ i ];

			gd.addNumericField( "Channel_" + channel + "_Initial_sigma", dogParameters[ i ][ 0 ], 4 );
			gd.addNumericField( "Channel_" + channel + "_Threshold", dogParameters[ i ][ 1 ], 4 );
		}
		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return null;
		
		for ( int i = 0; i < channelIndices.length; ++i )
		{
			dogParameters[ i ][ 0 ] = gd.getNextNumber();
			dogParameters[ i ][ 1 ] = gd.getNextNumber();
		}
		
		return dogParameters.clone();
	}
	
	/**
	 * Can be called with values[ 3 ], i.e. [initialsigma, sigma2, threshold] or
	 * values[ 2 ], i.e. [initialsigma, threshold]
	 * 
	 * The results are stored in the same array.
	 * If called with values[ 2 ], sigma2 changing will be disabled
	 * 
	 * @param text - the text which is shown when asking for the file
	 * @param values - the intial values and also contains the result
	 */
	public static void getInteractiveDoGParameters( final String text, final double values[] )
	{
		final GenericDialogPlus gd = new GenericDialogPlus( text );		
		gd.addFileField( "", spimDataDirectory, 50 );		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		final String file = gd.getNextString();
		
		IOFunctions.println( "Loading " + file );
		final Image<FloatType> img = LOCI.openLOCIFloatType( file, new ArrayContainerFactory() );
		
		if ( img == null )
		{
			IOFunctions.println( "File not found: " + file );
			return;
		}
		
		img.getDisplay().setMinMax();
		final ImagePlus imp = ImageJFunctions.copyToImagePlus( img );
		img.close();
		
		imp.show();		
		imp.setSlice( imp.getStackSize() / 2 );	
		imp.setRoi( 0, 0, imp.getWidth()/3, imp.getHeight()/3 );		
		
		final InteractiveDoG idog = new InteractiveDoG();
		
		if ( values.length == 2 )
		{
			idog.setSigma2isAdjustable( false );
			idog.setInitialSigma( (float)values[ 0 ] );
			idog.setThreshold( (float)values[ 1 ] );
		}
		else
		{
			idog.setInitialSigma( (float)values[ 0 ] );
			idog.setThreshold( (float)values[ 2 ] );			
		}
		
		idog.run( null );
		
		while ( !idog.isFinished() )
			SimpleMultiThreading.threadWait( 100 );
		
		imp.close();
		
		if ( values.length == 2)
		{
			values[ 0 ] = idog.getInitialSigma();
			values[ 1 ] = idog.getThreshold();
		}
		else
		{
			values[ 0 ] = idog.getInitialSigma();
			values[ 1 ] = idog.getSigma2();						
			values[ 2 ] = idog.getThreshold();			
		}
	}

	protected static boolean init( final SPIMConfiguration conf )
	{
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
			if ( conf.isHuiskenFormat() )
				conf.getFilenamesHuisken();
			else
				conf.getFileNames();
		}
		catch ( ConfigurationParserException e )
		{
			IJ.error( "Cannot parse input: " + e );
			return false;
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
					return false;
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
					return false;
				}
			}
		}
		
		return true;
	}

	public static final void addHyperLinkListener(final MultiLineLabel text, final String myURL)
	{
		if ( text != null && myURL != null )
		{
			text.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					try
					{
						BrowserLauncher.openURL(myURL);
					}
					catch (Exception ex)
					{
						IJ.error("" + ex);
					}
				}
	
				@Override
				public void mouseEntered(MouseEvent e)
				{
					text.setForeground(Color.BLUE);
					text.setCursor(new Cursor(Cursor.HAND_CURSOR));
				}
	
				@Override
				public void mouseExited(MouseEvent e)
				{
					text.setForeground(Color.BLACK);
					text.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			});
		}
	}

	public static void main(String[] args) {
		Debug.run("Record...", null);
		Debug.run("Find Commands...", null);
		Debug.run("Bead-based registration", null);
	}
}
