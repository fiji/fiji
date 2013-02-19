package plugin;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import fiji.plugin.Apply_External_Transformation;
import fiji.plugin.Bead_Registration;
import fiji.stacks.Hyperstack_rearranger;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.models.AffineModel2D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.HomographyModel2D;
import mpicbg.models.InterpolatedAffineModel2D;
import mpicbg.models.InterpolatedAffineModel3D;
import mpicbg.models.InvertibleBoundable;
import mpicbg.models.RigidModel2D;
import mpicbg.models.RigidModel3D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.spim.segmentation.InteractiveDoG;
import mpicbg.util.TransformUtils;
import process.Matching;

public class Descriptor_based_registration implements PlugIn 
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	final private String paperURL = "http://www.nature.com/nmeth/journal/v7/n6/full/nmeth0610-418.html";

	public static int defaultImg1 = 0;
	public static int defaultImg2 = 1;
	public static boolean defaultReApply = false;

	// if both are not null, the option of re-apply previous models will show up
	public static InvertibleBoundable lastModel1 = null;
	public static InvertibleBoundable lastModel2 = null;
	// define if it is was a 2d or 3d model
	public static int lastDimensionality = Integer.MAX_VALUE;
	
	//@Override
	public void run(String arg0) 
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

		if ( defaultImg1 >= imgList.length || defaultImg2 >= imgList.length )
		{
			defaultImg1 = 0;
			defaultImg2 = 1;
		}

		/**
		 * The first dialog for choosing the images
		 */
		final GenericDialog gd = new GenericDialog( "Descriptor based registration" );
	
		gd.addChoice("First_image (to register)", imgList, imgList[ defaultImg1 ] );
		gd.addChoice("Second_image (reference)", imgList, imgList[ defaultImg2 ] );
		
		if ( lastModel1 != null )
			gd.addCheckbox( "Reapply last model", defaultReApply );
		
		gd.addMessage( "Warning: if images are of RGB or 8-bit color they will be converted to hyperstacks.");
		gd.addMessage( "Please note that the SPIM Registration is based on a publication.\n" +
					   "If you use it successfully for your research please be so kind to cite our work:\n" +
					   "Preibisch et al., Nature Methods (2010), 7(6):418-419\n" );

		MultiLineLabel text =  (MultiLineLabel) gd.getMessage();
		Bead_Registration.addHyperLinkListener( text, paperURL );

		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		ImagePlus imp1 = WindowManager.getImage( idList[ defaultImg1 = gd.getNextChoiceIndex() ] );		
		ImagePlus imp2 = WindowManager.getImage( idList[ defaultImg2 = gd.getNextChoiceIndex() ] );		
		boolean reApply = false;
		
		if ( lastModel1 != null )
		{
			reApply = gd.getNextBoolean();
			defaultReApply = reApply;			
		}
		
		// if one of the images is rgb or 8-bit color convert them to hyperstack
		imp1 = Hyperstack_rearranger.convertToHyperStack( imp1 );
		imp2 = Hyperstack_rearranger.convertToHyperStack( imp2 );
		
		// test if the images are compatible
		String error = testRegistrationCompatibility( imp1, imp2 );

		if ( error != null )
		{
			IJ.log( error );
			return;
		}
				
		// get the parameters
		final int dimensionality;
		
		if ( imp1.getNSlices() > 1 )
			dimensionality = 3;
		else
			dimensionality = 2;
		
		if ( imp1.getNFrames() > 1 || imp2.getNFrames() > 2 )
			IJ.log( "WARNING: Images have more than one timepoint, ignoring all but the first one." );
		
		// reapply?
		if ( reApply )
		{
			if ( dimensionality < lastDimensionality )
			{
				IJ.log( "Cannot reapply, cannot apply a " + lastModel1.getClass().getSimpleName() + " to " + dimensionality + " data." );
				defaultReApply = false;
				return;
			}
			else if ( dimensionality > lastDimensionality )
			{
				IJ.log( "WARNING: applying a " + lastModel1.getClass().getSimpleName() + " to " + dimensionality + " data." );
			}
			
			// just fuse
			final DescriptorParameters params = new DescriptorParameters();
			params.dimensionality = dimensionality;
			params.reApply = true;
			params.fuse = 0;
			params.setPointsRois = false;
			Matching.descriptorBasedRegistration( imp1, imp2, params );
			return;			
		}

		// open a second dialog and query the other parameters
		final DescriptorParameters params = getParameters( imp1, imp2, dimensionality );
		
		if ( params == null )
			return;
				
		// compute the actual matching
		Matching.descriptorBasedRegistration( imp1, imp2, params );
	}
	
	public String[] transformationModels2d = new String[] { "Translation (2d)", "Rigid (2d)", "Similarity (2d)", "Affine (2d)", "Homography (2d)" };
	public String[] transformationModels3d = new String[] { "Translation (3d)", "Rigid (3d)", "Affine (3d)" };
	public static int defaultTransformationModel = 1;
	public static int defaultRegularizationTransformationModel = 1;
	public static double defaultLambda = 0.1;
	public static boolean defaultFixFirstTile = false;
	public static boolean defaultRegularize = false;
	
	public static String[] detectionBrightness = { "Very low", "Low", "Medium", "Strong", "Advanced ...", "Interactive ..." };
	public static int defaultDetectionBrightness = detectionBrightness.length - 1;
	public static double defaultSigma = 2;
	public static double defaultThreshold = 0.03;
	
	public static String[] detectionSize = { "2 px", "3 px", "4 px", "5 px", "6 px", "7 px", "8 px", "9 px", "10 px", "Advanced ...", "Interactive ..." };
	public static int defaultDetectionSize = detectionSize.length - 1;
	
	public static String[] detectionTypes = { "Maxima only", "Minima only", "Minima & Maxima", "Interactive ..." };
	public static int defaultDetectionType = detectionTypes.length - 1;
	public static boolean defaultInteractiveMaxima = true;
	public static boolean defaultInteractiveMinima = false;
	
	public static String[] orientation = { "Not prealigned", "Approxmiately aligned", "I will provide the approximate alignment" };
	public static int defaultSimilarOrientation = 0;
	public static int defaultNumNeighbors = 3;
	public static int defaultRedundancy = 1;
	public static double defaultSignificance = 3;
	public static double defaultRansacThreshold = 5;
	public static int defaultChannel1 = 1;
	public static int defaultChannel2 = 1;
	
	public static boolean defaultCreateOverlay = true;
	public static boolean defaultAddPointRoi = true;
	
	public String[] axes = new String[] { "x-axis", "y-axis", "z-axis" };
	public static int defaultAxis1 = 0;
	public static int defaultAxis2 = 1;
	public static int defaultAxis3 = 2;
	public static double defaultDegrees1 = 90;
	public static double defaultDegrees2 = 0;
	public static double defaultDegrees3 = 0;

	public static double defaultScale = 1;
	public static double m00 = 1, m01 = 0, m02 = 0, m03 = 0;
	public static double m10 = 0, m11 = 1, m12 = 0, m13 = 0;
	public static double m20 = 0, m21 = 0, m22 = 1, m23 = 0;

	/**
	 * Ask for all other required parameters ..
	 * 
	 * @param dimensionality
	 */
	protected DescriptorParameters getParameters( final ImagePlus imp1, final ImagePlus imp2, final int dimensionality )
	{
		final String[] transformationModel = dimensionality == 2 ? transformationModels2d : transformationModels3d;
	
		// check if default selection of transformation model holds
		if ( defaultTransformationModel >= transformationModel.length )
			defaultTransformationModel = 1;
		
		if ( defaultRegularizationTransformationModel >= transformationModel.length  )
			defaultRegularizationTransformationModel = 1;

		// one of them is by default interactive, then all are interactive
		if ( defaultDetectionBrightness == detectionBrightness.length - 1 || 
			 defaultDetectionSize == detectionSize.length - 1 ||
			 defaultDetectionType == detectionTypes.length - 1 )
		{
			defaultDetectionBrightness = detectionBrightness.length - 1; 
			defaultDetectionSize = detectionSize.length - 1;
			defaultDetectionType = detectionTypes.length - 1;
		}
		
		final GenericDialog gd = new GenericDialog( dimensionality + "-dimensional descriptor based registration" );			
		
		gd.addChoice( "Brightness_of detections", detectionBrightness, detectionBrightness[ defaultDetectionBrightness ] );
		gd.addChoice( "Approximate_size of detections", detectionSize, detectionSize[ defaultDetectionSize ] );
		gd.addChoice( "Type_of_detections", detectionTypes, detectionTypes[ defaultDetectionType ] );
		
		gd.addChoice( "Transformation_model", transformationModel, transformationModel[ defaultTransformationModel ] );
		gd.addCheckbox( "Regularize_model", defaultRegularize );
		gd.addChoice( "Images_pre-alignemnt", orientation, orientation[ defaultSimilarOrientation ] );
		
		if ( dimensionality == 2 )
		{
			if ( defaultNumNeighbors < 2 )
				defaultNumNeighbors = 2;
			
			gd.addSlider( "Number_of_neighbors for the descriptors", 2, 10, defaultNumNeighbors );
		}
		else
		{
			if ( defaultNumNeighbors < 3 )
				defaultNumNeighbors = 3;
			
			gd.addSlider( "Number_of_neighbors for the descriptors", 3, 10, defaultNumNeighbors );
		}
		gd.addSlider( "Redundancy for descriptor matching", 0, 10, defaultRedundancy );		
		gd.addSlider( "Significance required for a descriptor match", 1.0, 10.0, defaultSignificance );
		gd.addSlider( "Allowed_error_for_RANSAC (px)", 0.5, 20.0, defaultRansacThreshold );

		final int numChannels1 = imp1.getNChannels();
		final int numChannels2 = imp2.getNChannels();
		
		if ( defaultChannel1 > numChannels1 )
			defaultChannel1 = 1;
		if ( defaultChannel2 > numChannels2 )
			defaultChannel2 = 1;
		
		gd.addSlider( "Choose_registration_channel_for_image_1" , 1, numChannels1, defaultChannel1 );
		gd.addSlider( "Choose_registration_channel_for_image_2" , 1, numChannels2, defaultChannel2 );
		gd.addMessage( "Image fusion" );
		gd.addCheckbox( "Create_overlayed images", defaultCreateOverlay );
		gd.addCheckbox( "Add_point_rois for corresponding features to images", defaultAddPointRoi );

		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		Bead_Registration.addHyperLinkListener(text, myURL);

		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return null;
		
		final DescriptorParameters params = new DescriptorParameters();
		params.dimensionality = dimensionality;
		params.roi1 = imp1.getRoi();
		params.roi2 = imp2.getRoi();
		
		final int detectionBrightnessIndex = gd.getNextChoiceIndex();
		final int detectionSizeIndex = gd.getNextChoiceIndex();
		final int detectionTypeIndex = gd.getNextChoiceIndex();
		final int transformationModelIndex = gd.getNextChoiceIndex();
		final boolean regularize = gd.getNextBoolean();
		final int similarOrientation = gd.getNextChoiceIndex();
		final int numNeighbors = (int)Math.round( gd.getNextNumber() );
		final int redundancy = (int)Math.round( gd.getNextNumber() );
		final double significance = gd.getNextNumber();
		final double ransacThreshold = gd.getNextNumber();
		// zero-offset channel
		final int channel1 = (int)Math.round( gd.getNextNumber() ) - 1;
		final int channel2 = (int)Math.round( gd.getNextNumber() ) - 1;
		final boolean createOverlay = gd.getNextBoolean();
		final boolean addPointRoi = gd.getNextBoolean();
		
		
		// update static values for next call
		defaultDetectionBrightness = detectionBrightnessIndex;
		defaultDetectionSize = detectionSizeIndex;
		defaultDetectionType = detectionTypeIndex;
		defaultTransformationModel = transformationModelIndex;
		defaultRegularize = regularize;
		defaultSimilarOrientation = similarOrientation;
		defaultNumNeighbors = numNeighbors;
		defaultRedundancy = redundancy;
		defaultSignificance = significance;
		defaultRansacThreshold = ransacThreshold;
		defaultChannel1 = channel1 + 1;
		defaultChannel2 = channel2 + 1;
		defaultCreateOverlay = createOverlay;
		defaultAddPointRoi = addPointRoi;
		
		// instantiate model
		if ( dimensionality == 2 )
		{
			switch ( transformationModelIndex ) 
			{
				case 0:
					params.model = new TranslationModel2D();
					break;
				case 1:
					params.model = new RigidModel2D();
					break;
				case 2:
					params.model = new SimilarityModel2D();
					break;
				case 3:
					params.model = new AffineModel2D();
					break;
				case 4:
					if ( regularize )
					{
						IJ.log( "HomographyModel2D cannot be regularized yet" );
						return null;
					}
					params.model = new HomographyModel2D();
					break;
				default:
					params.model = new RigidModel2D();
					break;
			}
		}
		else
		{
			switch ( transformationModelIndex ) 
			{
				case 0:
					params.model = new TranslationModel3D();
					break;
				case 1:
					params.model = new RigidModel3D();
					break;
				case 2:
					params.model = new AffineModel3D();
					break;
				default:
					params.model = new RigidModel3D();
					break;
			}			
		}
		
		// regularization
		if ( regularize )
		{
			final GenericDialog gdRegularize = new GenericDialog( "Choose Regularization Model" );
			gdRegularize.addChoice( "Transformation_model", transformationModel, transformationModel[ defaultRegularizationTransformationModel ] );
			gdRegularize.addNumericField( "Lambda", defaultLambda, 2 );
			
			gdRegularize.showDialog();
			
			if ( gdRegularize.wasCanceled() )
				return null;
			
			params.regularize = true;
			final int modelIndex = defaultRegularizationTransformationModel = gdRegularize.getNextChoiceIndex();
			params.fixFirstTile = true;
			params.lambda = defaultLambda = gdRegularize.getNextNumber();

			// update model to a regularized one using the previous model
			if ( dimensionality == 2 )
			{
				switch ( modelIndex ) 
				{
					case 0:
						params.model = new InterpolatedAffineModel2D( params.model, new TranslationModel2D(), (float)params.lambda );
						break;
					case 1:
						params.model = new InterpolatedAffineModel2D( params.model, new RigidModel2D(), (float)params.lambda );
						break;
					case 2:
						params.model = new InterpolatedAffineModel2D( params.model, new SimilarityModel2D(), (float)params.lambda );
						break;
					case 3:
						params.model = new InterpolatedAffineModel2D( params.model, new AffineModel2D(), (float)params.lambda );
						break;
					case 4:
						IJ.log( "HomographyModel2D cannot be used for regularization yet" );
						return null;
					default:
						params.model = new InterpolatedAffineModel2D( params.model, new RigidModel2D(), (float)params.lambda );
						break;
				}
			}
			else
			{
				switch ( modelIndex ) 
				{
					case 0:
						params.model = new InterpolatedAffineModel3D( params.model, new TranslationModel3D(), (float)params.lambda );
						break;
					case 1:
						params.model = new InterpolatedAffineModel3D( params.model, new RigidModel3D(), (float)params.lambda );
						break;
					case 2:
						params.model = new InterpolatedAffineModel3D( params.model, new AffineModel3D(), (float)params.lambda );
						break;
					default:
						params.model = new InterpolatedAffineModel3D( params.model, new RigidModel3D(), (float)params.lambda );
						break;
				}			
			}
		}
		else
		{
			params.regularize = false;
			params.fixFirstTile = true;
		}
		
		// one of them is by default interactive, then all are interactive
		if ( detectionBrightnessIndex == detectionBrightness.length - 1 || 
			 detectionSizeIndex == detectionSize.length - 1 ||
			 detectionTypeIndex == detectionTypes.length - 1 )
		{
			// query parameters interactively
			final double[] values = new double[]{ defaultSigma, defaultThreshold };
			final InteractiveDoG idog = getInteractiveDoGParameters( imp1, channel1, values, 20 );
			
			params.sigma1 = values[ 0 ];
			params.threshold = values[ 1 ];
			params.lookForMaxima = idog.getLookForMaxima();
			params.lookForMinima = idog.getLookForMinima();
		}
		else 
		{
			if ( detectionBrightnessIndex == detectionBrightness.length - 2 || detectionSizeIndex == detectionSize.length - 2 )
			{
				// ask for the dog parameters
				final double[] values = getAdvancedDoGParameters( defaultSigma, defaultThreshold );
				
				// cancelled
				if ( values == null )
					return null;
				
				params.sigma1 = values[ 0 ];
				params.threshold = values[ 1 ];				
			}
			else
			{
				if ( detectionBrightnessIndex == 0 )
					params.threshold = 0.001;			
				else if ( detectionBrightnessIndex == 1 )
					params.threshold = 0.008;			
				else if ( detectionBrightnessIndex == 2 )
					params.threshold = 0.03;			
				else if ( detectionBrightnessIndex == 3 )
					params.threshold = 0.1;
	
				params.sigma1 = (detectionSizeIndex + 2.0) / 2.0;
			}
			
			if ( detectionTypeIndex == 2 )
			{
				params.lookForMaxima = true;
				params.lookForMinima = true;
			}
			else if ( detectionTypeIndex == 1 )
			{
				params.lookForMinima = true;
				params.lookForMaxima = false;
			}
			else
			{
				params.lookForMinima = false;
				params.lookForMaxima = true;				
			}			
		}
		// set the new default values
		defaultSigma = params.sigma1;
		defaultThreshold = params.threshold;
		
		if ( params.lookForMaxima && params.lookForMinima )
			defaultDetectionType = 2;
		else if ( params.lookForMinima )
			defaultDetectionType = 1;
		else
			defaultDetectionType = 0;
		
		// other parameters
		params.sigma2 = InteractiveDoG.computeSigma2( (float)params.sigma1, InteractiveDoG.standardSenstivity );
		if ( similarOrientation == 0 )
			params.similarOrientation = false;
		else
			params.similarOrientation = true;
		params.numNeighbors = numNeighbors;
		params.redundancy = redundancy;
		params.significance = significance;
		params.ransacThreshold = ransacThreshold;
		params.channel1 = channel1; 
		params.channel2 = channel2;
		if ( createOverlay )
			params.fuse = 0;
		else
			params.fuse = 2;
		params.setPointsRois = addPointRoi;
		
		// ask for the approximate transformation
		if ( similarOrientation == 2 )
		{
			if ( TranslationModel3D.class.isInstance( params.model ) || TranslationModel2D.class.isInstance( params.model ) )
			{
				IJ.log( "No parameters necessary ... the matching is anyways translation invariant." );
			}
			else if ( RigidModel3D.class.isInstance( params.model ) )
			{
				final GenericDialog gd2 = new GenericDialog( "Model parameters for rigid model 3d" );
				
				gd2.addChoice( "1st_Rotation_axis", axes, axes[ defaultAxis1 ] );
				gd2.addSlider( "1st_Rotation_angle", 0, 359, defaultDegrees1 );

				gd2.addChoice( "2nd_Rotation_axis", axes, axes[ defaultAxis2 ] );
				gd2.addSlider( "2nd_Rotation_angle", 0, 359, defaultDegrees2 );

				gd2.addChoice( "3rd_Rotation_axis", axes, axes[ defaultAxis3 ] );
				gd2.addSlider( "3rd_Rotation_angle", 0, 359, defaultDegrees3 );
				
				gd2.showDialog();
				
				if ( gd2.wasCanceled() )
					return null;
				
				defaultAxis1 = gd2.getNextChoiceIndex();
				defaultDegrees1 = gd2.getNextNumber();
				defaultAxis2 = gd2.getNextChoiceIndex();				
				defaultDegrees2 = gd2.getNextNumber();
				defaultAxis3 = gd2.getNextChoiceIndex();
				defaultDegrees3 = gd2.getNextNumber();
				
				final Transform3D t3 = new Transform3D();
				final Transform3D t2 = new Transform3D();
				final Transform3D t1 = new Transform3D();
				
				if ( defaultAxis1 == 0 )
					t1.rotX( Math.toRadians( defaultDegrees1 ) );
				else if ( defaultAxis1 == 1 )
					t1.rotY( Math.toRadians( defaultDegrees1 ) );
				else
					t1.rotZ( Math.toRadians( defaultDegrees1 ) );
				
				if ( defaultAxis2 == 0 )
					t2.rotX( Math.toRadians( defaultDegrees2 ) );
				else if ( defaultAxis2 == 1 )
					t2.rotY( Math.toRadians( defaultDegrees2 ) );
				else
					t2.rotZ( Math.toRadians( defaultDegrees2 ) );
				
				if ( defaultAxis3 == 0 )
					t3.rotX( Math.toRadians( defaultDegrees3 ) );
				else if ( defaultAxis3 == 1 )
					t3.rotY( Math.toRadians( defaultDegrees3 ) );
				else
					t3.rotZ( Math.toRadians( defaultDegrees3 ) );
				
				final AffineModel3D m1 = TransformUtils.getAffineModel3D( t1 );
				final AffineModel3D m2 = TransformUtils.getAffineModel3D( t2 );
				final AffineModel3D m3 = TransformUtils.getAffineModel3D( t3 );

				m1.preConcatenate( m2 );
				m1.preConcatenate( m3 );
				
				// set the model as initial guess
				params.initialModel = m1;
			}
			else if ( AffineModel3D.class.isInstance( params.model ) )
			{
				final GenericDialog gd2 = new GenericDialog( "Model parameters for affine model 3d" );
				
				gd2.addMessage( "" );
				gd2.addMessage( "m00 m01 m02 m03" );
				gd2.addMessage( "m10 m11 m12 m13" );
				gd2.addMessage( "m20 m21 m22 m23" );
				gd2.addMessage( "" );
				gd2.addMessage( "Please provide 3d affine in this form (any brackets will be ignored):" );
				gd2.addMessage( "m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23" );
				gd2.addStringField( "Affine_matrix", m00 + ", " + m01 + ", " + m02 + ", " + m03 + ", " + m10 + ", " + m11 + ", " + m12 + ", " + m13 + ", " + m20 + ", " + m21 + ", " + m22 + ", " + m23, 80 );

				gd2.showDialog();
				
				if ( gd2.wasCanceled() )
					return null;

				String entry = Apply_External_Transformation.removeSequences( gd2.getNextString().trim(), new String[] { "(", ")", "{", "}", "[", "]", "<", ">", ":", "m00", "m01", "m02", "m03", "m10", "m11", "m12", "m13", "m20", "m21", "m22", "m23", " " } );			
				final String[] numbers = entry.split( "," );
				
				if  ( numbers.length != 12 )
				{
					IJ.log( "Affine matrix has to have 12 entries: m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23" );
					IJ.log( "This one has only " + numbers.length + " after trimming: " + entry );
					
					return null;				
				}
				
				m00 = Double.parseDouble( numbers[ 0 ] );
				m01 = Double.parseDouble( numbers[ 1 ] );
				m02 = Double.parseDouble( numbers[ 2 ] );
				m03 = Double.parseDouble( numbers[ 3 ] );
				m10 = Double.parseDouble( numbers[ 4 ] );
				m11 = Double.parseDouble( numbers[ 5 ] );
				m12 = Double.parseDouble( numbers[ 6 ] );
				m13 = Double.parseDouble( numbers[ 7 ] );
				m20 = Double.parseDouble( numbers[ 8 ] );
				m21 = Double.parseDouble( numbers[ 9 ] );
				m22 = Double.parseDouble( numbers[ 10 ] );
				m23 = Double.parseDouble( numbers[ 11 ] );
				
				// set the model as initial guess
				final AffineModel3D model = new AffineModel3D();
				model.set( (float)m00, (float)m01, (float)m02, (float)m03, (float)m10, (float)m11, (float)m12, (float)m13, (float)m20, (float)m21, (float)m22, (float)m23 );
				params.initialModel = model;
			}
			else if ( AffineModel2D.class.isInstance( params.model ) )
			{
				final GenericDialog gd2 = new GenericDialog( "Model parameters for affine model 2d" );
				gd2.addMessage( "" );
				gd2.addMessage( "m00 m01 m02" );
				gd2.addMessage( "m10 m11 m12" );
				gd2.addMessage( "" );

				gd2.addMessage( "Please provide 2d affine in this form (any brackets will be ignored):" );
				gd2.addMessage( "m00, m01, m02, m10, m11, m12" );
				gd2.addStringField( "Affine_matrix", m00 + ", " + m01 + ", " + m02 + ", " + m10 + ", " + m11 + ", " + m12, 60 );
				
				gd2.showDialog();
				
				if ( gd2.wasCanceled() )
					return null;

				String entry = Apply_External_Transformation.removeSequences( gd2.getNextString().trim(), new String[] { "(", ")", "{", "}", "[", "]", "<", ">", ":", "m00", "m01", "m02", "m03", "m10", "m11", "m12", "m13", "m20", "m21", "m22", "m23", " " } );			
				final String[] numbers = entry.split( "," );
				
				if  ( numbers.length != 6 )
				{
					IJ.log( "Affine matrix has to have 6 entries: m00, m01, m02, m10, m11, m12" );
					IJ.log( "This one has only " + numbers.length + " after trimming: " + entry );
					
					return null;				
				}
				
				m00 = Double.parseDouble( numbers[ 0 ] );
				m01 = Double.parseDouble( numbers[ 1 ] );
				m02 = Double.parseDouble( numbers[ 2 ] );
				m10 = Double.parseDouble( numbers[ 3 ] );
				m11 = Double.parseDouble( numbers[ 4 ] );
				m12 = Double.parseDouble( numbers[ 5 ] );
				
				// set the model as initial guess
				final AffineModel2D model = new AffineModel2D();
				model.set( (float)m00, (float)m01, (float)m02, (float)m10, (float)m11, (float)m12 );
				params.initialModel = model;
			}
			else if ( HomographyModel2D.class.isInstance( params.model ) )
			{
				final GenericDialog gd2 = new GenericDialog( "Model parameters for homography model 2d" );
				
				gd2.addMessage( "" );
				gd2.addMessage( "m00 m01 m02" );
				gd2.addMessage( "m10 m11 m12" );
				gd2.addMessage( "m20 m21 m22" );
				gd2.addMessage( "" );
				gd2.addMessage( "Please provide 2d homography in this form (any brackets will be ignored):" );
				gd2.addMessage( "m00, m01, m02, m10, m11, m12, m20, m21, m22" );
				gd2.addStringField( "Homography_matrix", m00 + ", " + m01 + ", " + m02 + ", " + m10 + ", " + m11 + ", " + m12, 70 );

				gd2.showDialog();
				
				if ( gd2.wasCanceled() )
					return null;

				String entry = Apply_External_Transformation.removeSequences( gd2.getNextString().trim(), new String[] { "(", ")", "{", "}", "[", "]", "<", ">", ":", "m00", "m01", "m02", "m03", "m10", "m11", "m12", "m13", "m20", "m21", "m22", "m23", " " } );			
				final String[] numbers = entry.split( "," );
				
				if  ( numbers.length != 9 )
				{
					IJ.log( "Homography matrix has to have 9 entries: m00, m01, m02, m10, m11, m12, m20, m21, m22" );
					IJ.log( "This one has only " + numbers.length + " after trimming: " + entry );
					
					return null;				
				}
				
				m00 = Double.parseDouble( numbers[ 0 ] );
				m01 = Double.parseDouble( numbers[ 1 ] );
				m02 = Double.parseDouble( numbers[ 2 ] );
				m10 = Double.parseDouble( numbers[ 3 ] );
				m11 = Double.parseDouble( numbers[ 4 ] );
				m12 = Double.parseDouble( numbers[ 5 ] );
				m20 = Double.parseDouble( numbers[ 6 ] );
				m21 = Double.parseDouble( numbers[ 7 ] );
				m22 = Double.parseDouble( numbers[ 8 ] );
				
				// set the model as initial guess
				final HomographyModel2D model = new HomographyModel2D();
				model.set( (float)m00, (float)m01, (float)m02, (float)m10, (float)m11, (float)m12, (float)m20, (float)m21, (float)m22 );
				params.initialModel = model;
			}
			else if ( RigidModel2D.class.isInstance( params.model ) )
			{
				final GenericDialog gd2 = new GenericDialog( "Model parameters for rigid model 2d" );
				gd2.addSlider( "Rotation_angle", 0, 359, defaultDegrees1 );
				
				gd2.showDialog();
				
				if ( gd2.wasCanceled() )
					return null;

				defaultDegrees1 = gd2.getNextNumber();
				
				// set the model as initial guess
				final RigidModel2D model = new RigidModel2D();
				model.set( (float)Math.toRadians( defaultDegrees1 ), 0, 0 );
				params.initialModel = model;
			}			
			else if ( SimilarityModel2D.class.isInstance( params.model ) )
			{
				final GenericDialog gd2 = new GenericDialog( "Model parameters for rigid model 2d" );
				gd2.addSlider( "Rotation_angle", 0, 359, defaultDegrees1 );
				gd2.addNumericField( "Scaling", defaultScale, 2 );
				gd2.showDialog();
				
				if ( gd2.wasCanceled() )
					return null;

				defaultDegrees1 = gd2.getNextNumber();
				defaultScale = gd2.getNextNumber();
				
				// set the model as initial guess
				final SimilarityModel2D model = new SimilarityModel2D();
				model.set( (float)( defaultScale*Math.cos( Math.toRadians( defaultDegrees1 ) ) ), (float)( defaultScale * Math.sin( Math.toRadians( defaultDegrees1 ) ) ), 0, 0 );
				params.initialModel = model;
			}			
			else
			{
				IJ.log( "Unfortunately this is not supported this model yet ... " );
				return null;
			}					
		}
		
		return params;
	}
	
	protected static double[] getAdvancedDoGParameters( final double defaultSigma, final double defaultThreshold )
	{
		final GenericDialog gd = new GenericDialog( "Select Difference-of-Gaussian parameters" );
		
		gd.addNumericField( "Detection_sigma (approx. radius)", defaultSigma, 4 );
		gd.addNumericField( "Threshold", defaultThreshold, 4 );
		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return null;
		else
			return new double[]{ gd.getNextNumber(), gd.getNextNumber() };
	}
	
	/**
	 * Can be called with values[ 3 ], i.e. [initialsigma, sigma2, threshold] or
	 * values[ 2 ], i.e. [initialsigma, threshold]
	 * 
	 * The results are stored in the same array.
	 * If called with values[ 2 ], sigma2 changing will be disabled
	 * 
	 * @param text - the text which is shown when asking for the file
	 * @param values - the initial values and also contains the result
	 * @param sigmaMax - the maximal sigma allowed by the interactive app
	 * @return {@link InteractiveDoG} - the instance for querying additional parameters
	 */
	public static InteractiveDoG getInteractiveDoGParameters( final ImagePlus imp, final int channel, final double values[], final float sigmaMax )
	{
		 if ( imp.isDisplayedHyperStack() )
             imp.setPosition( imp.getStackIndex( channel + 1, imp.getNSlices() / 2 + 1 , 1 ) );
         else
             imp.setSlice( imp.getStackIndex( channel + 1, imp.getNSlices() / 2 + 1 , 1 ) );
		 
		imp.setRoi( 0, 0, imp.getWidth()/3, imp.getHeight()/3 );		
		
		final InteractiveDoG idog = new InteractiveDoG( imp, channel );
		idog.setSigmaMax( sigmaMax );
		idog.setLookForMaxima( defaultInteractiveMaxima );
		idog.setLookForMinima( defaultInteractiveMinima );
		
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
		
		// remove the roi
		imp.killRoi();
		
		defaultInteractiveMaxima = idog.getLookForMaxima();
		defaultInteractiveMinima = idog.getLookForMinima();
		
		return idog;
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
