import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.io.LOCI;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.segmentation.InteractiveDoG;

import fiji.util.gui.GenericDialogPlus;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;

public class Bead_Registration implements PlugIn 
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	final private String paperURL = "http://www.nature.com/nmeth/journal/v7/n6/full/nmeth0610-418.html";

	final String beadRegistration[] = new String[] { "Single-channel", "Multi-channel" };
	static int defaultBeadRegistration = 0;
	
	@Override
	public void run(String arg0) 
	{
		// output to IJ.log
		IOFunctions.printIJLog = true;
		
		final GenericDialog gd = new GenericDialog( "Bead based Registration" );
		
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
		
		if ( choice == 0 )
			singleChannel();
		else
			multiChannel();
	}

	public static String spimDataDirectoryStatic = "";
	public static String timepointsStatic = "1";
	public static String fileNamePatternStatic = "spim_TL{t}_Angle{a}.lsm";
	public static String anglesStatic = "0-315:45";
	
	public static boolean loadSegmentationStatic = false;
	public static String[] beadBrightnessList = { "Very weak", "Weak", "Comparable to Sample", "Strong", "Advanced ...", "Interactive ..." };	
	public static int defaultBeadBrightness = 1;
	public static boolean overrideResStatic = false;
	public static double xyResStatic = 0.73;
	public static double zResStatic = 2.00;

	public static boolean loadRegistrationStatic = false;
	public static boolean timeLapseRegistrationStatic = false;
	final String timeLapseRegistration[] = new String[] { "manually", "automatically" };
	static int defaultTimeLapseRegistration = 0;
	
	
	public void singleChannel()
	{
		final GenericDialogPlus gd = new GenericDialogPlus( "Single Channel Bead Registration" );
		
		gd.addDirectoryField( "SPIM_data_directory", spimDataDirectoryStatic );
		gd.addStringField( "Timepoints_to_process", timepointsStatic );
		gd.addStringField( "Pattern_of_SPIM files", fileNamePatternStatic, 25 );
		gd.addStringField( "Angles to process", anglesStatic );

		gd.addMessage( "" );		
		
		gd.addCheckbox( "Re-use_segmented_beads", loadSegmentationStatic );
		gd.addChoice( "Bead_brightness", beadBrightnessList, beadBrightnessList[ defaultBeadBrightness ] );
		gd.addCheckbox( "Override_file_dimensions", overrideResStatic );
		gd.addNumericField( "xy_resolution (um/px)", xyResStatic, 3 );
		gd.addNumericField( "z_resolution (um/px)", zResStatic, 3 );
		
		gd.addMessage( "" );		

		gd.addCheckbox( "Re-use_per_timepoint_registration", loadRegistrationStatic );

		gd.addMessage( "" );		

		gd.addCheckbox( "Timelapse_registration", timeLapseRegistrationStatic );
		gd.addChoice( "Select_reference timepoint", timeLapseRegistration, timeLapseRegistration[ defaultTimeLapseRegistration ] );

		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		spimDataDirectoryStatic = gd.getNextString();
		timepointsStatic = gd.getNextString();
		fileNamePatternStatic = gd.getNextString();
		anglesStatic = gd.getNextString();
		
		loadSegmentationStatic = gd.getNextBoolean();
		defaultBeadBrightness = gd.getNextChoiceIndex();
		overrideResStatic = gd.getNextBoolean();
		xyResStatic = gd.getNextNumber();
		zResStatic = gd.getNextNumber();
		
		loadRegistrationStatic = gd.getNextBoolean();
		
		timeLapseRegistrationStatic = gd.getNextBoolean();
		defaultTimeLapseRegistration = gd.getNextChoiceIndex();
		
		SPIMConfiguration conf = new SPIMConfiguration();
		
		if ( !loadSegmentationStatic && ( defaultBeadBrightness == 4 || defaultBeadBrightness == 5 ) )
		{
			// open advanced bead brightness detection
			final double[] values;
			
			if ( defaultBeadBrightness == 4 )
				values = getAdvancedDoGParameters( new int[ 1 ] )[ 0 ];
			else
				values = getInteractiveDoGParameters( "Select view to analyze" );
			
			// cancelled
			if ( values == null )
				return;
			
			conf.initialSigma = (float)values[ 0 ];
			conf.minPeakValue = (float)values[ 1 ];
			
			IOFunctions.println( "Selected initial sigma " + conf.initialSigma + ", threshold "+ conf.minPeakValue );
		}
	}
	
	public void multiChannel()
	{
		
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
		
		if ( channelIndices.length == 1 )
		{
			// single channel
			gd.addNumericField( "Initial_sigma", dogParameters[ 0 ][ 0 ], 4 );
			gd.addNumericField( "Threshold", dogParameters[ 0 ][ 1 ], 4 );
		}
		else
		{
			// multi channel
			for ( int i = 0; i < channelIndices.length; ++i )
			{
				final int channel = channelIndices[ i ];
				
				gd.addNumericField( "Channel_" + channel + "_Initial_sigma", dogParameters[ i ][ 0 ], 4 );
				gd.addNumericField( "Channel_" + channel + "_Threshold", dogParameters[ i ][ 1 ], 4 );				
			}
		}
		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return null;
		
		if ( channelIndices.length == 1 )
		{
			dogParameters[ 0 ][ 0 ] = gd.getNextNumber();
			dogParameters[ 0 ][ 1 ] = gd.getNextNumber();
		}
		else
		{
			// multi channel
			for ( int i = 0; i < channelIndices.length; ++i )
			{
				dogParameters[ i ][ 0 ] = gd.getNextNumber();
				dogParameters[ i ][ 1 ] = gd.getNextNumber();
			}			
		}
		
		return dogParameters.clone();
	}
	
	public static double[] getInteractiveDoGParameters( final String text )
	{
		final GenericDialogPlus gd = new GenericDialogPlus( text );		
		gd.addFileField( "", spimDataDirectoryStatic, 50 );		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return null;
		
		final String file = gd.getNextString();
		
		IOFunctions.println( "Loading " + file );
		final Image<FloatType> img = LOCI.openLOCIFloatType( file, new ArrayContainerFactory() );
		
		if ( img == null )
		{
			IOFunctions.println( "File not found: " + file );
			return null;
		}
		
		img.getDisplay().setMinMax();
		final ImagePlus imp = ImageJFunctions.copyToImagePlus( img );
		img.close();
		
		imp.show();		
		imp.setSlice( imp.getStackSize() / 2 );	
		imp.setRoi( 0, 0, imp.getWidth()/3, imp.getHeight()/3 );		
		
		final InteractiveDoG idog = new InteractiveDoG();
		idog.setSigma2isAdjustable( false );
		idog.setThreshold( 0.008f );
		idog.setInitialSigma( 1.8f );
		idog.run( null );
		
		while ( !idog.isFinished() )
			SimpleMultiThreading.threadWait( 100 );
		
		imp.close();
		
		return new double[]{ idog.getInitialSigma(), idog.getThreshold() };
	}

	public static final void addHyperLinkListener(final MultiLineLabel text, final String myURL)
	{
		text.addMouseListener(new MouseAdapter()
		{
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

			public void mouseEntered(MouseEvent e)
			{
				text.setForeground(Color.BLUE);
				text.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			public void mouseExited(MouseEvent e)
			{
				text.setForeground(Color.BLACK);
				text.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});
	}	
}
