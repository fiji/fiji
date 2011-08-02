package plugin;
import static stitching.CommonFunctions.addHyperLinkListener;

import java.awt.Choice;
import java.awt.TextField;

import stitching.CommonFunctions;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;


public class Stitching_Pairwise implements PlugIn 
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	final private String paperURL = "http://bioinformatics.oxfordjournals.org/cgi/content/abstract/btp184";

	public static int defaultImg1 = 0;
	public static int defaultImg2 = 1;
	public static int defaultChannelSelect = 0;
	public static int defaultTimeSelect = 1;
	public static boolean defaultFuseImages = true;
	public static int defaultFusionMethod = 1;
	public static boolean defaultComputeOverlap = true;
	public static int defaultCheckPeaks = 5;
	public static double defaultAlpha = 1.5;
	public static double defaultxOffset = 0, defaultyOffset = 0, defaultzOffset = 0;

	public static boolean[] defaultHandleChannel1 = null;
	public static boolean[] defaultHandleChannel2 = null;

	@Override
	public void run( final String arg0 ) 
	{
		// get list of image stacks
		final int[] idList = WindowManager.getIDList();		

		if ( idList == null || idList.length < 2 )
		{
			IJ.error( "You need at least two open images." );
			return;
		}

		final String[] imgList = new String[ idList.length ];
		for ( int i = 0; i < idList.length; ++i )
			imgList[ i ] = WindowManager.getImage(idList[i]).getTitle();

		/**
		 * The first dialog for choosing the images
		 */
		final GenericDialog gd1 = new GenericDialog( "Paiwise Stitching of Images" );
		
		gd1.addChoice("First_image (reference)", imgList, imgList[ defaultImg1 ] );
		gd1.addChoice("Second_image (to register)", imgList, imgList[ defaultImg2 ] );
		
		gd1.addMessage( "Please note that the Stitching is based on a publication.\n" +
						"If you use it successfully for your research please be so kind to cite our work:\n" +
						"Preibisch et al., Bioinformatics (2009), 25(11):1463-1465\n" );

		MultiLineLabel text = (MultiLineLabel) gd1.getMessage();
		addHyperLinkListener( text, paperURL );
		
		gd1.showDialog();
		
		if ( gd1.wasCanceled() )
			return;
		
		final ImagePlus imp1 = WindowManager.getImage( idList[ defaultImg1 = gd1.getNextChoiceIndex() ] );		
		final ImagePlus imp2 = WindowManager.getImage( idList[ defaultImg2 = gd1.getNextChoiceIndex() ] );		

		// test if the images are compatible
		String error = testRegistrationCompatibility( imp1, imp2 );
		
		if ( error != null )
		{
			IJ.error( error );
			return;
		}
		
		/**
		 * Show the next dialog
		 */
		final GenericDialog gd2 = new GenericDialog( "Paiwise Stitching of Images Parameters" );
		
		gd2.addChoice("Fusion_method", CommonFunctions.fusionMethodList, CommonFunctions.fusionMethodList[ defaultFusionMethod ] );
		gd2.addNumericField("Fusion_alpha", defaultAlpha, 2);		
		gd2.addStringField("Fused_image name: ", "Fused_" + imp1.getTitle() + "_" + imp2.getTitle() );
		gd2.addSlider("Check_peaks", 1, 100, defaultCheckPeaks );
		gd2.addCheckbox("Compute_overlap", defaultComputeOverlap );
		gd2.addNumericField("x", defaultxOffset, 0 );
		gd2.addNumericField("y", defaultyOffset, 0 );
		if ( !is2d( imp1 ) )		
			gd2.addNumericField("z", defaultzOffset, 0 );		
		
		if ( isMultiChannel( imp1 ) ) 
			gd2.addChoice( "Multi-channel_registration", CommonFunctions.channelSelect, CommonFunctions.channelSelect[ defaultChannelSelect ] );
		
		if ( imp1.getNFrames() > 1 ) 
			gd2.addChoice( "Time-lapse_registration", CommonFunctions.timeSelect, CommonFunctions.timeSelect[ defaultTimeSelect ] );
			
		gd2.addMessage( "" );
		gd2.addMessage( "This Plugin is developed by Stephan Preibisch\n" + myURL );

		text = (MultiLineLabel) gd2.getMessage();
		addHyperLinkListener( text, myURL );
		
		gd2.showDialog();

		if ( gd2.wasCanceled() )
			return;

		final int fusionMethod = defaultFusionMethod = gd2.getNextChoiceIndex();
		final double fusionAlpha = defaultAlpha = gd2.getNextNumber();
		final String fusedName = gd2.getNextText();
		final int checkPeaks = defaultCheckPeaks = (int)Math.round( gd2.getNextNumber() );
		final boolean computeOverlap = defaultComputeOverlap = gd2.getNextBoolean();
		final double xOffset = defaultxOffset = gd2.getNextNumber();
		final double yOffset = defaultyOffset = gd2.getNextNumber();
		
		final double zOffset;
		if ( is2d( imp1 ))
			zOffset = 0;
		else
			zOffset = defaultzOffset = gd2.getNextNumber();
		
		final int channelSelectMethod;
		if ( isMultiChannel( imp1 ) )
			channelSelectMethod = defaultChannelSelect = gd2.getNextChoiceIndex();
		else
			channelSelectMethod = 0;
		
		final int timeSelect;
		if ( imp1.getNFrames() > 1 ) 
			timeSelect = defaultTimeSelect = gd2.getNextChoiceIndex();
		else
			timeSelect = 0;
		
		// test if we can fuse like that
		error = testFusionCompatibility( imp1, imp2, fusionMethod );

		if ( error != null )
		{
			IJ.error( error );
			return;
		}
		
		/**
		 * Third GenericDialog if special channels for registration are selected
		 */		
		final int numChannels1 = getNumChannels( imp1 );
		final int numChannels2 = getNumChannels( imp2 );
		final boolean[] handleChannel1 = new boolean[ numChannels1 ];
		final boolean[] handleChannel2 = new boolean[ numChannels2 ];
		
		if ( defaultHandleChannel1 == null || defaultHandleChannel1.length != numChannels1 )
		{
			for ( int i = 0; i < numChannels1; ++i )
				handleChannel1[ i ] = true;
			
			defaultHandleChannel1 = handleChannel1.clone();
		}
		else
		{
			for ( int i = 0; i < numChannels1; ++i )
				handleChannel1[ i ] = defaultHandleChannel1[ i ];
		}

		if ( defaultHandleChannel2 == null || defaultHandleChannel2.length != numChannels2 )
		{
			for ( int i = 0; i < numChannels2; ++i )
				handleChannel2[ i ] = true;
			
			defaultHandleChannel2 = handleChannel2.clone();
		}
		else
		{
			for ( int i = 0; i < numChannels1; ++i )
				handleChannel2[ i ] = defaultHandleChannel2[ i ];
		}

		// channel choice if wanted
		if ( defaultChannelSelect == 1 )
		{
			final GenericDialog gd3 = new GenericDialog( "Select channels used for registration" );
			
			final String[] channels1 = new String[ numChannels1 ];
			final String[] channels2 = new String[ numChannels2 ];
			
			for ( int i = 0; i < numChannels1; ++i )
				channels1[ i ] = "Channel_" + (i+1);

			for ( int i = 0; i < numChannels2; ++i )
				channels2[ i ] = "Channel_" + (i+1);

			// add the checkbox group
			gd3.addCheckboxGroup( channels1.length, 1, channels1, handleChannel1, new String[] { "Channels for image 1" } );

			// add the checkbox group
			gd3.addCheckboxGroup( channels2.length, 1, channels2, handleChannel2, new String[] { "Channels for image 2" } );
			
			gd3.showDialog();
			
			if ( gd3.wasCanceled() )
				return;
			
			for ( int i = 0; i < numChannels1; ++i )
				handleChannel1[ i ] = defaultHandleChannel1[ i ] = gd3.getNextBoolean();
			
			for ( int i = 0; i < numChannels2; ++i )
				handleChannel2[ i ] = defaultHandleChannel2[ i ] = gd3.getNextBoolean();
		}		
	}
	
	public static String testFusionCompatibility( final ImagePlus imp1, final ImagePlus imp2, final int fusionMethod ) 
	{
		final int type1 = imp1.getType();
		final int type2 = imp2.getType();

		if ( getNumChannels( imp1 ) != getNumChannels( imp2 ) && fusionMethod < 4 )
			return "Images have different number of channels, so you can only choose to not fuse or combine as different channels.";
		
		if ( imp1.getType() != imp2.getType() )
		{
			// RGB is compatible with Composite 8-bit
			if ( ! ( imp1.isComposite() && type1 == ImagePlus.GRAY8 && (imp2.getType() == ImagePlus.COLOR_RGB || imp2.getType() == ImagePlus.COLOR_256 ) || 
				     imp2.isComposite() && type2 == ImagePlus.GRAY8 && (imp1.getType() == ImagePlus.COLOR_RGB || imp1.getType() == ImagePlus.COLOR_256 ) ) )
				return "Images are of different type, so you can only choose to not fuse."; 
		}

		return null;
	}

	public static boolean isMultiChannel( final ImagePlus imp )
	{
		if ( imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.COLOR_256 || imp.isComposite() )
			return true;
		else
			return false;
	}

	public static int getNumChannels( final ImagePlus imp )
	{
		if ( imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.COLOR_256 )
			return 3;
		else if ( imp.isComposite() )
			return imp.getNChannels();
		else
			return 1;
	}

	public static boolean is2d( final ImagePlus imp )
	{
		if ( imp.getNSlices() == 1 )
			return true;
		else
			return false;
	}

	public static String testRegistrationCompatibility( final ImagePlus imp1, final ImagePlus imp2 ) 
	{
		// test time points
		final int numFrames1 = imp1.getNFrames();
		final int numFrames2 = imp2.getNFrames();
		
		if ( numFrames1 != numFrames2 )
			return "Images have a different number of time points, cannot proceed...";
		
		// test if both have 2d or 3d image contents
		final int numSlices1 = imp1.getNSlices();
		final int numSlices2 = imp2.getNSlices();
		
		if ( numSlices1 == 1 && numSlices2 != 1 || numSlices1 != 1 && numSlices2 == 1 )
			return "One image is 2d and the other one is 3d, cannot proceed...";
		
		// test image type
		final int type1 = imp1.getType();
		final int type2 = imp2.getType();
		
		if ( type1 != type2 )
		{
			// RGB is compatible with Composite 8-bit
			if ( ! ( imp1.isComposite() && type1 == ImagePlus.GRAY8 && (imp2.getType() == ImagePlus.COLOR_RGB || imp2.getType() == ImagePlus.COLOR_256 ) || 
				     imp2.isComposite() && type2 == ImagePlus.GRAY8 && (imp1.getType() == ImagePlus.COLOR_RGB || imp1.getType() == ImagePlus.COLOR_256 ) ) )
				return "Images are of a differnt type, cannot proceed...";
		}
		
		return null;
	}

}
