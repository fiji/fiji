package plugin;
import static stitching.CommonFunctions.addHyperLinkListener;
import fiji.stacks.Hyperstack_rearranger;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.interpolation.nearestneighbor.NearestNeighborInterpolatorFactory;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.models.InvertibleBoundable;
import mpicbg.models.Model;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.stitching.ComparePair;
import mpicbg.stitching.GlobalOptimization;
import mpicbg.stitching.ImagePlusTimePoint;
import mpicbg.stitching.PairWiseStitchingImgLib;
import mpicbg.stitching.PairWiseStitchingResult;
import mpicbg.stitching.StitchingParameters;
import mpicbg.stitching.fusion.Fusion;
import mpicbg.stitching.fusion.OverlayFusion;
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
	public static double defaultxOffset = 0, defaultyOffset = 0, defaultzOffset = 0;

	public static boolean[] defaultHandleChannel1 = null;
	public static boolean[] defaultHandleChannel2 = null;

	public static int defaultMemorySpeedChoice = 0;
	public static double defaultDisplacementThresholdRelative = 2.5;		
	public static double defaultDisplacementThresholdAbsolute = 3.5;		

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
		
		if ( defaultImg1 >= imgList.length || defaultImg2 >= imgList.length )
		{
			defaultImg1 = 0;
			defaultImg2 = 1;
		}
		
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
		
		/**
		 * Show the next dialog
		 */
		final GenericDialog gd2 = new GenericDialog( "Paiwise Stitching" );
				
		gd2.addChoice("Fusion_method", fusionMethodList, fusionMethodList[ defaultFusionMethod ] );
		gd2.addStringField("Fused_image name: ", imp1.getTitle() + "<->" + imp2.getTitle(), 20 );
		gd2.addSlider("Check_peaks", 1, 100, defaultCheckPeaks );
		gd2.addCheckbox("Compute_overlap", defaultComputeOverlap );
		gd2.addCheckbox("Subpixel_accuracy", defaultSubpixelAccuracy );
		gd2.addNumericField("x", defaultxOffset, 4 );
		gd2.addNumericField("y", defaultyOffset, 4 );
		if ( dimensionality == 3 )		
			gd2.addNumericField("z", defaultzOffset, 4 );		

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
		
		if ( simpleFusion ) // 
			params.fusionMethod = defaultFusionMethod = gd2.getNextChoiceIndex() + ( CommonFunctions.fusionMethodList.length - CommonFunctions.fusionMethodListSimple.length );
		else
			params.fusionMethod = defaultFusionMethod = gd2.getNextChoiceIndex();
		
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
		
		if ( params.timeSelect > 0 )
		{
			GenericDialog gd3 = new GenericDialog( "Details for timelapse stitching" );
			
			gd3.addChoice( "Computation parameters", CommonFunctions.cpuMemSelect, CommonFunctions.cpuMemSelect[ defaultMemorySpeedChoice ] );
			//gd3.addNumericField( "Regression_Threshold", defaultRegressionThreshold, 2 );
			gd3.addNumericField( "Max/Avg Displacement Threshold", defaultDisplacementThresholdRelative, 2 );		
			gd3.addNumericField( "Absolute Avg Displacement Threshold", defaultDisplacementThresholdAbsolute, 2 );
			
			gd3.showDialog();

			if ( gd3.wasCanceled() )
				return;
			
			params.cpuMemChoice = defaultMemorySpeedChoice = gd3.getNextChoiceIndex();
			//params.regThreshold = defaultRegressionThreshold = gd3.getNextNumber();
			params.relativeThreshold = defaultDisplacementThresholdRelative = gd3.getNextNumber();
			params.absoluteThreshold = defaultDisplacementThresholdAbsolute = gd3.getNextNumber();
		}
		
		// compute and fuse
		performPairWiseStitching( imp1, imp2, params );
	}
	
	public static void performPairWiseStitching( final ImagePlus imp1, final ImagePlus imp2, final StitchingParameters params )
	{
		final ArrayList<InvertibleBoundable> models = new ArrayList< InvertibleBoundable >();
		
		// the simplest case, only one registration necessary
		if ( imp1.getNFrames() == 1 || params.timeSelect == 0 )
		{
			// compute the stitching
			long start = System.currentTimeMillis();
			
			final PairWiseStitchingResult result;
			
			if ( params.computeOverlap )
			{
				result = PairWiseStitchingImgLib.stitchPairwise( imp1, imp2, imp1.getRoi(), imp2.getRoi(), 1, 1, params );
				IJ.log( "shift (second relative to first): " + Util.printCoordinates( result.getOffset() ) + " correlation (R)=" + result.getCrossCorrelation() + " (" + (System.currentTimeMillis() - start) + " ms)");
				
				// update the dialog to show the numbers next time
				defaultxOffset = result.getOffset( 0 );
				defaultyOffset = result.getOffset( 1 );
				if ( params.dimensionality == 3 )
					defaultzOffset = result.getOffset( 2 );
			}
			else
			{
				final float[] offset;
				
				if ( params.dimensionality == 2 )
				{
					if ( params.subpixelAccuracy )
						offset = new float[] { (float)params.xOffset, (float)params.yOffset };
					else
						offset = new float[] { Math.round( (float)params.xOffset ), Math.round( (float)params.yOffset ) };
				}
				else
				{
					if ( params.subpixelAccuracy )
						offset = new float[] { (float)params.xOffset, (float)params.yOffset, (float)params.zOffset };
					else
						offset = new float[] { Math.round( (float)params.xOffset ), Math.round( (float)params.yOffset ), Math.round( (float)params.zOffset ) };					
				}
				
				result = new PairWiseStitchingResult( offset, 0.0f, 0.0f );
				IJ.log( "shift (second relative to first): " + Util.printCoordinates( result.getOffset() ) + " (from dialog)");
			}
						
			
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
			// get all that we have to compare
			final Vector< ComparePair > pairs = getComparePairs( imp1, imp2, params.dimensionality, params.timeSelect );
			
			// compute all compare pairs
			// compute all matchings
			final AtomicInteger ai = new AtomicInteger(0);
			
			final int numThreads;
			
			if ( params.cpuMemChoice == 0 )
				numThreads = 1;
			else
				numThreads = Runtime.getRuntime().availableProcessors();
			
	        final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
	    	
	        for ( int ithread = 0; ithread < threads.length; ++ithread )
	            threads[ ithread ] = new Thread(new Runnable()
	            {
	                public void run()
	                {		
	                   	final int myNumber = ai.getAndIncrement();
	                    
	                    for ( int i = 0; i < pairs.size(); i++ )
	                    {
	                    	if ( i % numThreads == myNumber )
	                    	{
	                    		final ComparePair pair = pairs.get( i );
	                    		
	                    		long start = System.currentTimeMillis();			

	            				final PairWiseStitchingResult result = PairWiseStitchingImgLib.stitchPairwise( pair.getImagePlus1(), pair.getImagePlus2(), 
	            						pair.getImagePlus1().getRoi(), pair.getImagePlus2().getRoi(), pair.getTimePoint1(), pair.getTimePoint2(), params );			

	            				if ( params.dimensionality == 2 )
	            					pair.setRelativeShift( new float[]{ result.getOffset( 0 ), result.getOffset( 1 ) } );
	            				else
	            					pair.setRelativeShift( new float[]{ result.getOffset( 0 ), result.getOffset( 1 ), result.getOffset( 2 ) } );
	            				
	            				pair.setCrossCorrelation( result.getCrossCorrelation() );

	            				IJ.log( pair.getImagePlus1().getTitle() + "[" + pair.getTimePoint1() + "]" + " <- " + pair.getImagePlus2().getTitle() + "[" + pair.getTimePoint2() + "]" + ": " + 
	            						Util.printCoordinates( result.getOffset() ) + " correlation (R)=" + result.getCrossCorrelation() + " (" + (System.currentTimeMillis() - start) + " ms)");
	                    	}
	                    }
	                }
	            });
	        
	        SimpleMultiThreading.startAndJoin( threads );
			
	        // get the final positions of all tiles
			final ArrayList< ImagePlusTimePoint > optimized = GlobalOptimization.optimize( pairs, pairs.get( 0 ).getTile1(), params );
			
			for ( int f = 0; f < imp1.getNFrames(); ++f )
			{
				IJ.log ( optimized.get( f*2 ).getImagePlus().getTitle() + "["+ optimized.get( f*2 ).getImpId() + "," + optimized.get( f*2 ).getTimePoint() + "]: " + optimized.get( f*2 ).getModel() );
				IJ.log ( optimized.get( f*2 + 1 ).getImagePlus().getTitle() + "["+ optimized.get( f*2 + 1 ).getImpId() + "," + optimized.get( f*2 + 1 ).getTimePoint() + "]: " + optimized.get( f*2 + 1 ).getModel() );
				models.add( (InvertibleBoundable)optimized.get( f*2 ).getModel() );
				models.add( (InvertibleBoundable)optimized.get( f*2 + 1 ).getModel() );
			}
			
		}
		
		// now fuse
		IJ.log( "Fusing ..." );
		
		final ImagePlus ci;
		final long start = System.currentTimeMillis();			
			
		if ( imp1.getType() == ImagePlus.GRAY32 || imp2.getType() == ImagePlus.GRAY32 )
			ci = fuse( new FloatType(), imp1, imp2, models, params );
		else if ( imp1.getType() == ImagePlus.GRAY16 || imp2.getType() == ImagePlus.GRAY16 )
			ci = fuse( new UnsignedShortType(), imp1, imp2, models, params );
		else
			ci = fuse( new UnsignedByteType(), imp1, imp2, models, params );
		
		if ( ci != null )
		{
			ci.setTitle( params.fusedName );
			ci.show();
		}

		IJ.log( "Finished ... (" + (System.currentTimeMillis() - start) + " ms)");
	}
	
	protected static < T extends RealType< T > > ImagePlus fuse( final T targetType, final ImagePlus imp1, final ImagePlus imp2, final ArrayList<InvertibleBoundable> models, final StitchingParameters params )
	{
		final ArrayList<ImagePlus> images = new ArrayList< ImagePlus >();
		images.add( imp1 );
		images.add( imp2 );
		
		if ( params.fusionMethod < 5 )
		{
			ImagePlus imp = Fusion.fuse( targetType, images, models, params.dimensionality, params.subpixelAccuracy, params.fusionMethod, null, false );
			return imp;
		}
		else if ( params.fusionMethod == 5 ) // overlay
		{
			// images are always the same, we just trigger different timepoints
			final InterpolatorFactory< FloatType > factory;
			
			if ( params.subpixelAccuracy )
				factory  = new LinearInterpolatorFactory<FloatType>( new OutOfBoundsStrategyValueFactory<FloatType>() );
			else
				factory  = new NearestNeighborInterpolatorFactory<FloatType>( new OutOfBoundsStrategyValueFactory<FloatType>() );
		
			// fuses the first timepoint but estimates the boundaries for all timepoints as it gets all models
			final CompositeImage timepoint0 = OverlayFusion.createOverlay( targetType, images, models, params.dimensionality, 1, factory );
			
			if ( imp1.getNFrames() > 1 )
			{
				final ImageStack stack = new ImageStack( timepoint0.getWidth(), timepoint0.getHeight() );
				
				// add all slices of the first timepoint
				for ( int c = 1; c <= timepoint0.getStackSize(); ++c )
					stack.addSlice( "", timepoint0.getStack().getProcessor( c ) );
				
				//"Overlay into composite image"
				for ( int f = 2; f <= imp1.getNFrames(); ++f )
				{
					final CompositeImage tmp = OverlayFusion.createOverlay( targetType, images, models, params.dimensionality, f, factory );
					
					// add all slices of the first timepoint
					for ( int c = 1; c <= tmp.getStackSize(); ++c )
						stack.addSlice( "", tmp.getStack().getProcessor( c ) );					
				}
				
				//convertXYZCT ...
				ImagePlus result = new ImagePlus( params.fusedName, stack );
				
				// numchannels, z-slices, timepoints (but right now the order is still XYZCT)
				result.setDimensions( timepoint0.getNChannels(), timepoint0.getNSlices(), imp1.getNFrames() );
				return new CompositeImage( result, CompositeImage.COMPOSITE );
			}
			else
			{
				timepoint0.setTitle( params.fusedName );
				return timepoint0;
			}
		}
		else
		{
			//"Do not fuse images"
			return null;
		}
	}
	
	protected static Vector< ComparePair > getComparePairs( final ImagePlus imp1, final ImagePlus imp2, final int dimensionality, final int timeSelect )
	{
		final Model< ? > model;
		
		if ( dimensionality == 2 )
			model = new TranslationModel2D();
		else
			model = new TranslationModel3D();

		final ArrayList< ImagePlusTimePoint > listImp1 = new ArrayList< ImagePlusTimePoint >();
		final ArrayList< ImagePlusTimePoint > listImp2 = new ArrayList< ImagePlusTimePoint >();
		
		for ( int timePoint1 = 1; timePoint1 <= imp1.getNFrames(); timePoint1++ )
			listImp1.add( new ImagePlusTimePoint( imp1, 1, timePoint1, model.copy(), null ) );

		for ( int timePoint2 = 1; timePoint2 <= imp2.getNFrames(); timePoint2++ )
			listImp2.add( new ImagePlusTimePoint( imp2, 2, timePoint2, model.copy(), null ) );
		
		final Vector< ComparePair > pairs = new Vector< ComparePair >();		
				
		// imp1 vs imp2 at all timepoints
		for ( int timePointA = 1; timePointA <= Math.min( imp1.getNFrames(), imp2.getNFrames() ); timePointA++ )
		{
			ImagePlusTimePoint a = listImp1.get( timePointA - 1 );
			ImagePlusTimePoint b = listImp2.get( timePointA - 1 );
			pairs.add( new ComparePair( a, b ) );
		}

		if ( timeSelect == 1 )
		{
			// consequtively all timepoints of imp1
			for ( int timePointA = 1; timePointA <= imp1.getNFrames() - 1; timePointA++ )
				pairs.add( new ComparePair( listImp1.get( timePointA - 1 ), listImp1.get( timePointA + 1 - 1 ) ) );

			// consequtively all timepoints of imp2
			for ( int timePointB = 1; timePointB <= imp2.getNFrames() - 1; timePointB++ )
				pairs.add( new ComparePair( listImp2.get( timePointB - 1 ), listImp2.get( timePointB + 1 - 1 ) ) );
			
		}
		else
		{
			// all against all for imp1
			for ( int timePointA = 1; timePointA <= imp1.getNFrames() - 1; timePointA++ )
				for ( int timePointB = timePointA + 1; timePointB <= imp1.getNFrames(); timePointB++ )
					pairs.add( new ComparePair( listImp1.get( timePointA - 1 ), listImp1.get( timePointB - 1 ) ) );
			
			// all against all for imp2
			for ( int timePointA = 1; timePointA <= imp2.getNFrames() - 1; timePointA++ )
				for ( int timePointB = timePointA + 1; timePointB <= imp2.getNFrames(); timePointB++ )
					pairs.add( new ComparePair( listImp2.get( timePointA - 1 ), listImp2.get( timePointB - 1 ) ) );
		}
		
		return pairs;
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
