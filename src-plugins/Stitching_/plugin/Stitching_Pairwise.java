package plugin;
import static stitching.CommonFunctions.addHyperLinkListener;

import java.util.ArrayList;

import process.OverlayFusion;

import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.models.InvertibleBoundable;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.stitching.PairWiseStitchingImgLib;
import mpicbg.stitching.PairWiseStitchingResult;
import mpicbg.stitching.StitchingParameters;
import fiji.stacks.Hyperstack_rearranger;
import ij.CompositeImage;
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
		final boolean simpleFusion;
		
		if ( imp1.getNChannels() != imp2.getNChannels() )
		{
			fusionMethodList = CommonFunctions.fusionMethodListSimple;
			simpleFusion = true;
		}
		else
		{
			fusionMethodList = CommonFunctions.fusionMethodList;
			simpleFusion = false;
		}
		
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
		
		if ( simpleFusion )
			params.fusionMethod = defaultFusionMethod = gd2.getNextChoiceIndex() + 4;
		else
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

		// if there is only one channel we do not need to average
		if ( channels1.length == 2 )
			params.channel1 = 1;

		// if there is only one channel we do not need to average
		if ( channels2.length == 2 )
			params.channel2 = 1;

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
		
		// compute and fuse
		performStitching( imp1, imp2, params );
	}
	
	public static void performStitching( final ImagePlus imp1, final ImagePlus imp2, final StitchingParameters params )
	{
		final ArrayList<InvertibleBoundable> models = new ArrayList< InvertibleBoundable >();
		
		// the simplest case, only one registration necessary
		if ( imp1.getNFrames() == 1 || params.timeSelect == 0 )
		{
			// compute the stitching
			final PairWiseStitchingResult result = PairWiseStitchingImgLib.stitchPairwise( imp1, imp2, 1, params );			
			IJ.log( "shift (second relative to first): " + Util.printCoordinates( result.getOffset() ) + " correlation (R)=" + result.getCrossCorrelation() );

			for ( int f = 1; f <= imp1.getNFrames(); ++f )
			{
				if ( params.dimensionality == 2 )
				{
					TranslationModel2D model1 = new TranslationModel2D();
					TranslationModel2D model2 = new TranslationModel2D();
					model2.set( result.getOffset( 0 ), result.getOffset( 1 ) );
					
					models.add( model1 );			
					models.add( model2 );
				}
				else
				{
					TranslationModel3D model1 = new TranslationModel3D();
					TranslationModel3D model2 = new TranslationModel3D();
					model2.set( result.getOffset( 0 ), result.getOffset( 1 ), result.getOffset( 2 ) );
					
					models.add( model1 );			
					models.add( model2 );
					
				}
			}
			
		}
		else
		{
			
		}
		
		// now fuse
		final CompositeImage ci;
		
		if ( imp1.getType() == ImagePlus.GRAY32 || imp2.getType() == ImagePlus.GRAY32 )
			ci = fuse( new FloatType(), imp1, imp2, models, params );
		else if ( imp1.getType() == ImagePlus.GRAY16 || imp2.getType() == ImagePlus.GRAY16 )
			ci = fuse( new FloatType(), imp1, imp2, models, params );
		else
			ci = fuse( new FloatType(), imp1, imp2, models, params );
		
		if ( ci != null )
			ci.show();
	}
	
	protected static < T extends RealType< T > > CompositeImage fuse( final T targetType, final ImagePlus imp1, final ImagePlus imp2, final ArrayList<InvertibleBoundable> models, final StitchingParameters params )
	{
		if ( params.fusionMethod == 0 )
		{
			//"Linear Blending"
			return null;
		}
		else if ( params.fusionMethod == 1 )
		{
			//"Average"
			return null;
		}
		else if ( params.fusionMethod == 2 )
		{
			//"Max. Intensity"
			return null;
		}
		else if ( params.fusionMethod == 3 )
		{
			//"Min. Intensity"
			return null;
		}
		else if ( params.fusionMethod == 4 )
		{
			// images are always the same, we just trigger different timepoints
			final ArrayList<ImagePlus> images = new ArrayList< ImagePlus >();
			images.add( imp1 );
			images.add( imp2 );
		
			if ( imp1.getNFrames() > 1 )
			{
				//"Overlay into composite image"
				for ( int f = 1; f < imp1.getNFrames(); ++f )
				{
					OverlayFusion.createOverlay( targetType, images, models, params.dimensionality, f );
				}
				return null;
						
			}
			else
			{
				return OverlayFusion.createOverlay( targetType, images, models, params.dimensionality, 1 );				
			}
		}
		else
		{
			//"Do not fuse images"
			return null;
		}				
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
