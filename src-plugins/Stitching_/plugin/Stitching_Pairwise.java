package plugin;
import static stitching.CommonFunctions.addHyperLinkListener;
import mpicbg.stitching.StitchingImgLib;
import mpicbg.stitching.StitchingParameters;
import fiji.stacks.Hyperstack_rearranger;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;
import stitching.CommonFunctions;


public class Stitching_Pairwise implements PlugIn 
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	final private String paperURL = "http://bioinformatics.oxfordjournals.org/cgi/content/abstract/btp184";

	public static int defaultImg1 = 0;
	public static int defaultImg2 = 1;
	public static int defaultChannel1 = 0;
	public static int defaultChannel2 = 0;
	public static int defaultTimeSelect = 1;
	public static boolean defaultFuseImages = true;
	public static int defaultFusionMethod = 0;
	public static boolean defaultComputeOverlap = true;
	public static boolean defaultSubpixelAccuracy = true;
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
		
		ImagePlus imp1 = WindowManager.getImage( idList[ defaultImg1 = gd1.getNextChoiceIndex() ] );		
		ImagePlus imp2 = WindowManager.getImage( idList[ defaultImg2 = gd1.getNextChoiceIndex() ] );		

		// if one of the images is rgb or 8-bit color convert them to hyperstack
		imp1 = Hyperstack_rearranger.convertToHyperStack( imp1 );
		imp2 = Hyperstack_rearranger.convertToHyperStack( imp2 );
		
		// test if the images are compatible
		String error = testRegistrationCompatibility( imp1, imp2 );
		
		if ( error != null )
		{
			IJ.error( error );
			return;
		}
		
		// the dimensionality
		final int dimensionality;
		
		if ( imp1.getNSlices() > 1 || imp2.getNSlices() > 1 )
			dimensionality = 3;
		else
			dimensionality = 2;

		// create channel selector
		final int numChannels1 = imp1.getNChannels();
		final int numChannels2 = imp2.getNChannels();

		final String[] channels1 = new String[ numChannels1 + 1 ];
		final String[] channels2 = new String[ numChannels2 + 1 ];
		
		channels1[ 0 ] = "Average all channels";
		for ( int c = 1; c < channels1.length; ++c )
			channels1[ c ] = "Only channel " + c;

		channels2[ 0 ] = "Average all channels";
		for ( int c = 1; c < channels2.length; ++c )
			channels2[ c ] = "Only channel " + c;

		if ( defaultChannel1 >= channels1.length )
			defaultChannel1 = 0;
		
		if ( defaultChannel2 >= channels2.length )
			defaultChannel2 = 0;
		
		// which fusion methods are available
		String[] fusionMethodList;
		
		if ( imp1.getNChannels() != imp2.getNChannels() )
			fusionMethodList = CommonFunctions.fusionMethodListSimple;
		else
			fusionMethodList = CommonFunctions.fusionMethodList;		
		
		if ( defaultFusionMethod >= fusionMethodList.length )
			defaultFusionMethod = 0;
		
		IJ.log( "dim: " + dimensionality );
		/**
		 * Show the next dialog
		 */
		final GenericDialog gd2 = new GenericDialog( "Paiwise Stitching" );
				
		gd2.addChoice("Fusion_method", fusionMethodList, fusionMethodList[ defaultFusionMethod ] );
		gd2.addNumericField("Fusion_alpha", defaultAlpha, 2 );		
		gd2.addStringField("Fused_image name: ", "Stitched_" + imp1.getTitle() + "_" + imp2.getTitle(), 30 );
		gd2.addSlider("Check_peaks", 1, 100, defaultCheckPeaks );
		gd2.addCheckbox("Compute_overlap", defaultComputeOverlap );
		gd2.addCheckbox("Subpixel_accuracy", defaultSubpixelAccuracy );
		gd2.addNumericField("x", defaultxOffset, 0 );
		gd2.addNumericField("y", defaultyOffset, 0 );
		if ( dimensionality == 3 )		
			gd2.addNumericField("z", defaultzOffset, 0 );		

		gd2.addChoice( "Registration_channel_image_1 ", channels1, channels1[ defaultChannel1 ] );
		gd2.addChoice( "Registration_channel_image_2 ", channels2, channels2[ defaultChannel2 ] );
		
		if ( imp1.getNFrames() > 1 ) 
			gd2.addChoice( "Time-lapse_registration", CommonFunctions.timeSelect, CommonFunctions.timeSelect[ defaultTimeSelect ] );
			
		gd2.addMessage( "" );
		gd2.addMessage( "This Plugin is developed by Stephan Preibisch\n" + myURL );

		text = (MultiLineLabel) gd2.getMessage();
		addHyperLinkListener( text, myURL );
		
		gd2.showDialog();

		if ( gd2.wasCanceled() )
			return;
		
		final StitchingParameters params = new StitchingParameters();
		
		params.dimensionality = dimensionality;
		params.fusionMethod = defaultFusionMethod = gd2.getNextChoiceIndex();
		params.fusionAlpha = defaultAlpha = gd2.getNextNumber();
		params.fusedName = gd2.getNextText();
		params.checkPeaks = defaultCheckPeaks = (int)Math.round( gd2.getNextNumber() );
		params.computeOverlap = defaultComputeOverlap = gd2.getNextBoolean();
		params.subpixelAccuracy = defaultSubpixelAccuracy = gd2.getNextBoolean();
		params.xOffset = defaultxOffset = gd2.getNextNumber();
		params.yOffset = defaultyOffset = gd2.getNextNumber();

		if ( dimensionality == 3 )		
			params.zOffset = defaultzOffset = gd2.getNextNumber();
		else
			params.zOffset = 0;

		params.channel1 = defaultChannel1 = gd2.getNextChoiceIndex();
		params.channel2 = defaultChannel2 = gd2.getNextChoiceIndex();

		if ( imp1.getNFrames() > 1 ) 
			params.timeSelect = defaultTimeSelect = gd2.getNextChoiceIndex();
		else
			params.timeSelect = 0;
		
		if ( !params.computeOverlap && params.timeSelect > 0 )
		{
			IJ.log( "WARNING: You chose to not compute overlap, ignoring the option '" + CommonFunctions.timeSelect[ params.timeSelect ] + "'!" );
			params.timeSelect = defaultTimeSelect = 0;
			IJ.log( "WARNING: Instead we will '" + CommonFunctions.timeSelect[ params.timeSelect ] + "'" );
		}
		
		// compute the stitching
		StitchingImgLib.stitchPairwise( imp1, imp2, params );
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
				
		return null;
	}

}
