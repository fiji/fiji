package plugin;

import fiji.plugin.Bead_Registration;
import fiji.stacks.Hyperstack_rearranger;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.MultiLineLabel;
import ij.plugin.PlugIn;

import java.util.ArrayList;

import mpicbg.models.AffineModel2D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.HomographyModel2D;
import mpicbg.models.InterpolatedAffineModel2D;
import mpicbg.models.InterpolatedAffineModel3D;
import mpicbg.models.InterpolatedModel;
import mpicbg.models.InvertibleBoundable;
import mpicbg.models.RigidModel2D;
import mpicbg.models.RigidModel3D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.spim.segmentation.InteractiveDoG;
import process.Matching;
import process.OverlayFusion;

public class Descriptor_based_series_registration implements PlugIn
{
	final private String myURL = "http://fly.mpi-cbg.de/preibisch";
	final private String paperURL = "http://www.nature.com/nmeth/journal/v7/n6/full/nmeth0610-418.html";

	public static int defaultImg = 0;
	public static boolean defaultReApply = false;
	// if both are not null, the option of re-apply previous models will show up
	public static ArrayList<InvertibleBoundable> lastModels = null;
	// define if it is was a 2d or 3d model
	public static int lastDimensionality = Integer.MAX_VALUE;
	
	@Override
	public void run( final String arg0 ) 
	{
		// get list of image stacks
		final int[] idList = WindowManager.getIDList();		

		if ( idList == null || idList.length < 1 )
		{
			IJ.error( "You need at least one open image." );
			return;
		}
		
		final String[] imgList = new String[ idList.length ];
		for ( int i = 0; i < idList.length; ++i )
			imgList[ i ] = WindowManager.getImage(idList[i]).getTitle();

		if ( defaultImg >= imgList.length )
			defaultImg = 0;
		
		/**
		 * The first dialog for choosing the images
		 */
		final GenericDialog gd = new GenericDialog( "Descriptor based registration" );
	
		gd.addChoice("Series_of_images", imgList, imgList[ defaultImg ] );
		
		if ( lastModels != null )
			gd.addCheckbox( "Reapply last models", defaultReApply );
		
		gd.addMessage( "Warning: if images are of RGB or 8-bit color they will be converted to hyperstacks.");
		gd.addMessage( "Please note that the SPIM Registration is based on a publication.\n" +
					   "If you use it successfully for your research please be so kind to cite our work:\n" +
					   "Preibisch et al., Nature Methods (2010), 7(6):418-419\n" );

		MultiLineLabel text =  (MultiLineLabel) gd.getMessage();
		Bead_Registration.addHyperLinkListener( text, paperURL );

		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return;
		
		ImagePlus imp = WindowManager.getImage( idList[ defaultImg = gd.getNextChoiceIndex() ] );		
		boolean reApply = false;
		
		if ( lastModels != null )
		{
			reApply = gd.getNextBoolean();
			defaultReApply = reApply;			
		}

		// if one of the images is rgb or 8-bit color convert them to hyperstack
		imp = Hyperstack_rearranger.convertToHyperStack( imp );
		
		if ( imp.getNSlices() == 1 && imp.getNFrames() == 1)
		{
			IJ.log( "The image has only one slice/frame, cannot perform stack registration" );
			return;
		}

		final int dimensionality;
		
		// is it 2d over time or 3d over time
		if ( imp.getNSlices() > 1 && imp.getNFrames() > 1 )
			dimensionality = 3;
		else
			dimensionality = 2;
		
		// if it is a stack, we convert it into a movie
		if ( dimensionality == 2 && imp.getNSlices() > 1 )
			imp = OverlayFusion.switchZTinXYCZT( imp );

		// reapply?
		if ( reApply )
		{
			if ( lastModels.size() < imp.getNFrames() )
			{
				IJ.log( "Cannot reapply, you have only " + lastModels.size() + " models, but the series size is" + imp.getNFrames() + "." );
				defaultReApply = false;
				return;
			}
			else if ( lastModels.size() > imp.getNFrames() )
			{
				IJ.log( "WARNING: you have " + lastModels.size() + " models, but the series size is only" + imp.getNFrames() + "." );
			}
			else if ( dimensionality < lastDimensionality )
			{
				IJ.log( "Cannot reapply, cannot apply " + lastModels.get( 0 ).getClass().getSimpleName() + " to " + dimensionality + " data." );
				defaultReApply = false;
				return;
			}
			else if ( dimensionality > lastDimensionality )
			{
				IJ.log( "WARNING: applying " + lastModels.get( 0 ).getClass().getSimpleName() + " to " + dimensionality + " data." );
			}
			
			// just fuse
			final DescriptorParameters params = new DescriptorParameters();
			params.reApply = true;
			params.dimensionality = dimensionality;
			Matching.descriptorBasedStackRegistration( imp, params );
			return;
		}

		// open a second dialog and query the other parameters
		final DescriptorParameters params = getParameters( imp, dimensionality );
		
		if ( params == null )
			return;
		
		// compute the matching
		Matching.descriptorBasedStackRegistration( imp, params );
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
	
	public static boolean defaultSimilarOrientation = false;
	public static int defaultNumNeighbors = 3;
	public static int defaultRedundancy = 1;
	public static double defaultSignificance = 3;
	public static double defaultRansacThreshold = 5;
	
	public static String[] globalOptTypes = { "All-to-all matching (global optimization)", "All-to-all matching with range ('reasonable' global optimization)", "All against first image (no global optimization)", "Consecutive matching of images (no global optimization)" };
	public static int defaultGlobalOpt = 1;
	public static int defaultRange = 5;
	
	public static int defaultChannel = 1;
	
	public static String[] resultChoices = { "Fuse and display", "Write to disk", "Do not fuse" };
	public static int defaultResult = 0;

	public static String defaultDirectory = "";
	
	/**
	 * Ask for all other required parameters ..
	 * 
	 * @param dimensionality
	 */
	protected DescriptorParameters getParameters( final ImagePlus imp, final int dimensionality )
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
		
		final GenericDialog gd = new GenericDialog( "Descriptor based stack registration" );			
		
		gd.addChoice( "Brightness_of detections", detectionBrightness, detectionBrightness[ defaultDetectionBrightness ] );
		gd.addChoice( "Approximate_size of detections", detectionSize, detectionSize[ defaultDetectionSize ] );
		gd.addChoice( "Type_of_detections", detectionTypes, detectionTypes[ defaultDetectionType ] );
		
		gd.addChoice( "Transformation_model", transformationModel, transformationModel[ defaultTransformationModel ] );
		gd.addCheckbox( "Regularize_model", defaultRegularize );
		gd.addCheckbox( "Images_are_roughly_aligned", defaultSimilarOrientation );
		
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
		gd.addChoice( "Global_optimization", globalOptTypes, globalOptTypes[ defaultGlobalOpt ] );
		gd.addSlider( "Range for all-to-all matching", 2, 10, defaultRange );
		final int numChannels = imp.getNChannels();
		
		if ( defaultChannel > numChannels )
			defaultChannel = 1;
		
		gd.addSlider( "Choose_registration_channel" , 1, numChannels, defaultChannel );
		gd.addMessage( "" );
		gd.addChoice( "Image fusion", resultChoices, resultChoices[ defaultResult ] );
		
		gd.addMessage("");
		gd.addMessage("This Plugin is developed by Stephan Preibisch\n" + myURL);

		MultiLineLabel text = (MultiLineLabel) gd.getMessage();
		Bead_Registration.addHyperLinkListener(text, myURL);

		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return null;
		
		final DescriptorParameters params = new DescriptorParameters();
		params.roi1 = imp.getRoi();
		
		final int detectionBrightnessIndex = gd.getNextChoiceIndex();
		final int detectionSizeIndex = gd.getNextChoiceIndex();
		final int detectionTypeIndex = gd.getNextChoiceIndex();
		final int transformationModelIndex = gd.getNextChoiceIndex();
		final boolean regularize = gd.getNextBoolean();
		final boolean similarOrientation = gd.getNextBoolean();
		final int numNeighbors = (int)Math.round( gd.getNextNumber() );
		final int redundancy = (int)Math.round( gd.getNextNumber() );
		final double significance = gd.getNextNumber();
		final double ransacThreshold = gd.getNextNumber();
		final int globalOptIndex = gd.getNextChoiceIndex();
		final int range = (int)Math.round( gd.getNextNumber() );
		// zero-offset channel
		final int channel = (int)Math.round( gd.getNextNumber() ) - 1;
		final int result = gd.getNextChoiceIndex();
		
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
		defaultGlobalOpt = globalOptIndex;
		defaultRange = range;
		defaultChannel = channel + 1;
		defaultResult = result;

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
			gdRegularize.addCheckbox( "Fix_first_tile", defaultFixFirstTile );
			gdRegularize.addNumericField( "Lambda", defaultLambda, 2 );
			
			gdRegularize.showDialog();
			
			if ( gdRegularize.wasCanceled() )
				return null;
			
			params.regularize = true;
			final int modelIndex = defaultRegularizationTransformationModel = gdRegularize.getNextChoiceIndex();
			params.fixFirstTile = defaultFixFirstTile = gdRegularize.getNextBoolean();
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

		if ( defaultResult == 1 )
		{
			final GenericDialogPlus gd2 = new GenericDialogPlus( "Select output directory" );
			gd2.addDirectoryField( "Output_directory", defaultDirectory, 60 );
			gd2.showDialog();
			
			if ( gd2.wasCanceled() )
				return null;
			
			defaultDirectory = gd2.getNextString();
		}
			
		// one of them is by default interactive, then all are interactive
		if ( detectionBrightnessIndex == detectionBrightness.length - 1 || 
			 detectionSizeIndex == detectionSize.length - 1 ||
			 detectionTypeIndex == detectionTypes.length - 1 )
		{
			// query parameters interactively
			final double[] values = new double[]{ defaultSigma, defaultThreshold };
			
			final ImagePlus interactiveTmp;
			
			if ( dimensionality == 2 )
			{
				interactiveTmp = new ImagePlus( "First slice of " + imp.getTitle(), imp.getStack().getProcessor( imp.getStackIndex( channel + 1, 1, 1 ) ) );
			}
			else
			{
				ImageStack stack = new ImageStack( imp.getWidth(), imp.getHeight() );
				for ( int s = 1; s <= imp.getNSlices(); ++s )
					stack.addSlice( "", imp.getStack().getProcessor( imp.getStackIndex( channel + 1, s, 1 ) ) );
				interactiveTmp = new ImagePlus( "First series of " + imp.getTitle(), stack );
			}
			interactiveTmp.show();
			final InteractiveDoG idog = Descriptor_based_registration.getInteractiveDoGParameters( interactiveTmp, 1, values, 20 );
			interactiveTmp.close();
			
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
				final double[] values = Descriptor_based_registration.getAdvancedDoGParameters( defaultSigma, defaultThreshold );
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
		params.similarOrientation = similarOrientation;
		params.numNeighbors = numNeighbors;
		params.redundancy = redundancy;
		params.significance = significance;
		params.ransacThreshold = ransacThreshold;
		params.channel1 = channel; 
		params.channel2 = -1;
		params.fuse = result;
		params.directory = defaultDirectory;
		params.setPointsRois = false;
		params.globalOpt = globalOptIndex;
		params.range = range;
		params.dimensionality = dimensionality;
		
		return params;
	}
	
}
