package register_virtual_stack;

/** 
 * Albert Cardona, Ignacio Arganda-Carreras and Stephan Saalfeld 2009, 2010. 
 * This work released under the terms of the General Public License in its latest edition. 
 * */

import fiji.util.GenericDialogPlus;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.plugin.PlugIn;
import ij.VirtualStack;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.io.FileSaver;
import ij.io.OpenDialog;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;

import mpicbg.ij.FeatureTransform;
import mpicbg.ij.SIFT;
import mpicbg.ij.util.Util;
import mpicbg.imagefeatures.*;


import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;



import mpicbg.trakem2.transform.AffineModel2D;
import mpicbg.trakem2.transform.CoordinateTransform;
import mpicbg.trakem2.transform.CoordinateTransformList;
import mpicbg.trakem2.transform.MovingLeastSquaresTransform;
import mpicbg.trakem2.transform.RigidModel2D;
import mpicbg.trakem2.transform.SimilarityModel2D;
import mpicbg.trakem2.transform.TransformMesh;
import mpicbg.trakem2.transform.TransformMeshMapping;
import mpicbg.trakem2.transform.TranslationModel2D;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

import javax.swing.JFileChooser;

import bunwarpj.Transformation;
import bunwarpj.bUnwarpJ_;
import bunwarpj.trakem2.transform.CubicBSplineTransform;

/** 
 * Fiji plugin to register sequences of images in a concurrent (multi-thread) way.
 * <p>
 * <b>Requires</b>: a directory with images, of any size and type (8, 16, 32-bit gray-scale or RGB color)
 * <p>
 * <b>Performs</b>: registration of a sequence of images, by 6 different registration models:
 * <ul>
 * 				<li> Translation (no deformation)</li>
 * 				<li> Rigid (translation + rotation)</li>
 * 				<li> Similarity (translation + rotation + isotropic scaling)</li>
 * 				<li> Affine (free affine transformation)</li>
 * 				<li> Elastic (consistent elastic deformations by B-splines)</li>
 * 				<li> Moving least squares (maximal warping)</li>
 * </ul>
 * <p>
 * <b>Outputs</b>: the list of new images, one for slice, into a target directory as .tif files.
 * <p>
 * For a detailed documentation, please visit the plugin website at:
 * <p>
 * <A target="_blank" href="http://pacific.mpi-cbg.de/wiki/Register_Virtual_Stack_Slices">http://pacific.mpi-cbg.de/wiki/Register_Virtual_Stack_Slices</A>
 * 
 * @version 03/15/2010
 * @author Ignacio Arganda-Carreras (ignacio.arganda@gmail.com), Stephan Saalfeld and Albert Cardona
 */
public class Register_Virtual_Stack_MT implements PlugIn 
{

	// Registration types
	/** translation registration model id */
	public static final int TRANSLATION 			= 0;
	/** rigid-body registration model id */
	public static final int RIGID 					= 1;
	/** rigid-body + isotropic scaling registration model id */
	public static final int SIMILARITY 				= 2;
	/** affine registration model id */
	public static final int AFFINE 					= 3;
	/** elastic registration model id */
	public static final int ELASTIC 				= 4;
	/** maximal warping registration model id */
	public static final int MOVING_LEAST_SQUARES 	= 5;
	
	/** index of the features model check-box */
	public static int featuresModelIndex = Register_Virtual_Stack_MT.RIGID;
	/** index of the registration model check-box */
	public static int registrationModelIndex = Register_Virtual_Stack_MT.RIGID;
	/** working directory path */
	public static String currentDirectory = (OpenDialog.getLastDirectory() == null) ? 
					 OpenDialog.getDefaultDirectory() : OpenDialog.getLastDirectory();
					 
	/** advance options flag */
	public static boolean advanced = false;
	/** shrinkage constraint flag */
	public static boolean non_shrinkage = false;
	/** save transformation flag */
	public static boolean save_transforms = false;
	
	/** source directory **/
	public static String sourceDirectory="";
	/** output directory **/
	public static String outputDirectory="";
	
	// Regularization 
	/** scaling regularization parameter [0.0-1.0] */
	public static double tweakScale = 0.95;
	/** shear regularization parameter [0.0-1.0] */
	public static double tweakShear = 0.95;
	/** isotropy (aspect ratio) regularization parameter [0.0-1.0] */
	public static double tweakIso = 0.95;
	
	/** display relaxation graph flag */
	public static boolean displayRelaxGraph = false;
	
	// Image centers
	/** array of x- coordinate image centers */ 
	private static double[] centerX = null;
	/** array of y- coordinate image centers */
	private static double[] centerY = null;
	
	/** post-processing flag */
	public static boolean postprocess = true;
	/** debug flat to print out intermediate results and information */
	private static boolean debug = false;

	/** registration model string labels */
	public static final String[] registrationModelStrings =
			       {"Translation          -- no deformation                      ",
	  	            "Rigid                -- translate + rotate                  ",
			        "Similarity           -- translate + rotate + isotropic scale",
			        "Affine               -- free affine transform               ",
			        "Elastic              -- bUnwarpJ splines                    ",
			        "Moving least squares -- maximal warping                     "};
	

	/** feature model string labels */
	public final static String[] featuresModelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };
	/** relaxation threshold (if the difference between last two iterations is below this threshold, the relaxation stops */
	public static final float STOP_THRESHOLD = 0.01f;
	/** maximum number of iterations in the relaxation loop */
	public static final int MAX_ITER = 300;


	//---------------------------------------------------------------------------------
	/**
	 * Plug-in run method
	 * 
	 * @param arg plug-in arguments
	 */
	public void run(String arg) 
	{
		GenericDialogPlus gd = new GenericDialogPlus("Register Virtual Stack");

		gd.addDirectoryField("Source directory", sourceDirectory, 50);
		gd.addDirectoryField("Output directory", outputDirectory, 50);
		gd.addChoice("Feature extraction model: ", featuresModelStrings, featuresModelStrings[featuresModelIndex]);
		gd.addChoice("Registration model: ", registrationModelStrings, registrationModelStrings[registrationModelIndex]);
		gd.addCheckbox("Advanced setup", advanced);	
		gd.addCheckbox("Shrinkage constrain", non_shrinkage);
		gd.addCheckbox("Save transforms", save_transforms);
		
		gd.showDialog();
		
		// Exit when canceled
		if (gd.wasCanceled()) 
			return;
				
		sourceDirectory = gd.getNextString();
		outputDirectory = gd.getNextString();
		featuresModelIndex = gd.getNextChoiceIndex();
		registrationModelIndex = gd.getNextChoiceIndex();
		advanced = gd.getNextBoolean();
		non_shrinkage = gd.getNextBoolean();
		save_transforms = gd.getNextBoolean();

		String source_dir = sourceDirectory;
		if (null == source_dir) 
		{
			IJ.error("Error: No source directory was provided.");
			return;
		}
		source_dir = source_dir.replace('\\', '/');
		if (!source_dir.endsWith("/")) source_dir += "/";
		
		String target_dir = outputDirectory;
		if (null == target_dir) 
		{
			IJ.error("Error: No output directory was provided.");
			return;
		}
		target_dir = target_dir.replace('\\', '/');
		if (!target_dir.endsWith("/")) target_dir += "/";
		
		// Select folder to save the transformation files if
		// the "Save transforms" check-box was checked.
		String save_dir = null;
		if(save_transforms)
		{
			// Choose target folder to save images into
			JFileChooser chooser = new JFileChooser(source_dir); 			
			chooser.setDialogTitle("Choose directory to store Transform files");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(true);
			if (chooser.showOpenDialog(gd) != JFileChooser.APPROVE_OPTION)
		    	return;
			
			save_dir = chooser.getSelectedFile().toString();
			if (null == save_dir) 
				return;
			save_dir = save_dir.replace('\\', '/');
			if (!save_dir.endsWith("/")) save_dir += "/";
		}
		
		// Select reference
		String referenceName = null;						
		if(non_shrinkage == false)
		{		
			JFileChooser chooser = new JFileChooser(source_dir); 
			// Choose reference image
			chooser.setDialogTitle("Choose reference image");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setAcceptAllFileFilterUsed(true);
			if (chooser.showOpenDialog(gd) != JFileChooser.APPROVE_OPTION)
				return;
			referenceName = chooser.getSelectedFile().getName();
		}


		// Execute registration
		exec(source_dir, target_dir, save_dir, referenceName, featuresModelIndex, registrationModelIndex, advanced, non_shrinkage);
	}
	//-----------------------------------------------------------------------------------
	/** 
	 * Execution method. Execute registration after setting parameters. 
	 * 
	 * @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 * @param target_dir Directory to store registered slices into.
	 * @param save_dir Directory to store transform files into.
	 * @param referenceName File name of the reference image.
	 * @param featuresModelIndex Index of the features extraction model (0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE)
	 * @param registrationModelIndex Index of the registration model (0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE, 4=ELASTIC, 5=MOVING_LEAST_SQUARES)
	 * @param advanced Triggers showing parameters setup dialogs
	 * @param non_shrink Triggers showing non-shrinking dialog (if advanced options are selected as well) and execution
	 */
	static public void exec(
			final String source_dir, 
			final String target_dir,
			final String save_dir,
			final String referenceName,
			final int featuresModelIndex, 
			final int registrationModelIndex, 
			final boolean advanced,
			final boolean non_shrink) 
	{
		Param p = new Param();
		Param.featuresModelIndex = featuresModelIndex;
		Param.registrationModelIndex = registrationModelIndex;
		
		if(non_shrink)
		{
			p.elastic_param.divWeight = 0.1;
			p.elastic_param.curlWeight = 0.1;
			p.elastic_param.landmarkWeight = 1.0;
			p.elastic_param.consistencyWeight = 0.0;
			p.elastic_param.imageWeight = 0.0;			
		}
		
		// Show parameter dialogs when advanced option is checked
		if (advanced && !p.showDialog())
			return;
		if (non_shrink && advanced 
				&& Param.registrationModelIndex != Register_Virtual_Stack_MT.TRANSLATION 
				&& !showRegularizationDialog(p))
			return;
		exec(source_dir, target_dir, save_dir, referenceName, p, non_shrink);
	}

	//-----------------------------------------------------------------------------------
	/**
	 * Execute registration when all parameters are set.
	 * 
	 * @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 * @param target_dir Directory to store registered slices into.
	 * @param save_dir Directory to store transform files into.
	 * @param referenceName File name of the reference image (if necessary, for non-shrinkage mode, it can be null)
	 * @param p Registration parameters
	 * @param non_shrink non shrinking mode flag
	 */
	public static void exec(
			final String source_dir, 
			final String target_dir, 
			final String save_dir,
			final String referenceName, 
			final Param p, 
			final boolean non_shrink) 
	{
		// get file listing
		final String exts = ".tif.jpg.png.gif.tiff.jpeg.bmp.pgm";
		final String[] names = new File(source_dir).list(new FilenameFilter() 
		{
			public boolean accept(File dir, String name) 
			{
				int idot = name.lastIndexOf('.');
				if (-1 == idot) return false;
				return exts.contains(name.substring(idot).toLowerCase());
			}
		});
		Arrays.sort(names);
				
		if(non_shrink)
		{
			// Execute registration with shrinkage constrain,
			// so no reference is needed
			exec(source_dir, names, target_dir, save_dir, p);
			return;
		}
		
		int referenceIndex = -1;
		for(int i = 0; i < names.length; i++)
			if(names[i].equals(referenceName))
			{
				referenceIndex = i;
				break;
			}
		
		if(referenceIndex == -1)
		{
			IJ.error("The reference image was not found in the source folder!");
			return;
		}
		
		//IJ.log("Reference index = " + referenceIndex);

		// Execute registration with sorted source file names and reference image index
		exec(source_dir, names, referenceIndex, target_dir, save_dir, p);
	}
	
	//-----------------------------------------------------------------------------------------
	/**
	 * Registration parameters class. It stores SIFT and bUnwarpJ registration parameters. 
	 *
	 */
	public static class Param
	{	
		/** SIFT parameters */
		public final FloatArray2DSIFT.Param sift = new FloatArray2DSIFT.Param();
		
		/**
		 * Closest/next neighbor distance ratio
		 */
		public static float rod = 0.92f;
		
		/**
		 * Maximal allowed alignment error in pixels
		 */
		public static float maxEpsilon = 25.0f;
		
		/**
		 * Inlier/candidates ratio
		 */
		public static float minInlierRatio = 0.05f;
		
		/**
		 * Implemented transformation models for choice
	 	 *  0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE
		 */
		public static int featuresModelIndex = Register_Virtual_Stack_MT.RIGID;

		/**
		 * Implemented transformation models for choice
	 	*  0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE, 4=ELASTIC, 5=MOVING_LEAST_SQUARES
		 */
		public static int registrationModelIndex = Register_Virtual_Stack_MT.RIGID;
                
		/** bUnwarpJ parameters for consistent elastic registration */
        public bunwarpj.Param elastic_param = new bunwarpj.Param();        
        
        //---------------------------------------------------------------------------------
        /**
         * Shows parameter dialog when "advanced options" is checked
         * @return false when dialog is canceled or true when is not
         */
		public boolean showDialog() 
		{
			// Feature extraction parameters
			GenericDialog gd = new GenericDialog("Feature extraction");
			gd.addMessage( "Scale Invariant Interest Point Detector:" );
			gd.addNumericField( "initial_gaussian_blur :", sift.initialSigma, 2, 6, "px" );
			gd.addNumericField( "steps_per_scale_octave :", sift.steps, 0 );
			gd.addNumericField( "minimum_image_size :", sift.minOctaveSize, 0, 6, "px" );
			gd.addNumericField( "maximum_image_size :", sift.maxOctaveSize, 0, 6, "px" );
			
			gd.addMessage( "Feature Descriptor:" );
			gd.addNumericField( "feature_descriptor_size :", 8, 0 );
			gd.addNumericField( "feature_descriptor_orientation_bins :", sift.fdBins, 0 );
			gd.addNumericField( "closest/next_closest_ratio :", rod, 2 );
			
			gd.addMessage( "Geometric Consensus Filter:" );
			gd.addNumericField( "maximal_alignment_error :", maxEpsilon, 2, 6, "px" );
			gd.addNumericField( "inlier_ratio :", minInlierRatio, 2 );
			gd.addChoice( "Feature_extraction_model :", featuresModelStrings, featuresModelStrings[ featuresModelIndex ] ); // rigid

			gd.addMessage("Registration:");
			gd.addChoice( "Registration_model:", registrationModelStrings, registrationModelStrings[ registrationModelIndex ] ); // rigid

			gd.showDialog();

			// Exit when canceled
			if (gd.wasCanceled()) 
				return false;

			sift.initialSigma = (float) gd.getNextNumber();
			sift.steps = (int) gd.getNextNumber();
			sift.minOctaveSize = (int) gd.getNextNumber();
			sift.maxOctaveSize = (int) gd.getNextNumber();

			sift.fdSize = (int) gd.getNextNumber();
			sift.fdBins = (int) gd.getNextNumber();
			rod = (float) gd.getNextNumber();

			maxEpsilon = (float) gd.getNextNumber();
			minInlierRatio = (float) gd.getNextNumber();
			featuresModelIndex = gd.getNextChoiceIndex();

			registrationModelIndex = gd.getNextChoiceIndex();
                      
			// Show bUnwarpJ parameters if elastic registration
			if (registrationModelIndex == Register_Virtual_Stack_MT.ELASTIC)
			{								
				if (!this.elastic_param.showDialog())
					return false;
			}

			return true;
		}
	} // end class Param
	
	//-----------------------------------------------------------------------------------------
	/**
	 * Execute registration with non-shrinking constrain 
	 * 
	 * @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 * @param sorted_file_names Array of sorted source file names.
	 * @param target_dir Directory to store registered slices into.
	 * @param save_dir Directory to store transform files into.
	 * @param p registration parameters
	 */
	public static void exec(
			final String source_dir, 
			final String[] sorted_file_names,
			final String target_dir, 
			final String save_dir,
			final Param p) 
	{		
		// Check if source and output directories are different
		if (source_dir.equals(target_dir)) 
		{
			IJ.error("Source and target directories MUST be different\n or images would get overwritten.\nDid NOT register stack slices.");
			return;
		}
		// Check if the registration model is known
		if (Param.registrationModelIndex < TRANSLATION || Param.registrationModelIndex > MOVING_LEAST_SQUARES) 
		{
			IJ.error("Don't know how to process registration type " + Param.registrationModelIndex);
			return;
		}
		
		// Executor service to run concurrent tasks
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		// Select features model to select the correspondences in every image
		Model< ? > featuresModel;
		switch ( Param.featuresModelIndex )
		{
			case Register_Virtual_Stack_MT.TRANSLATION:
				featuresModel = new TranslationModel2D();
				break;
			case Register_Virtual_Stack_MT.RIGID:
				featuresModel = new RigidModel2D();
				break;
			case Register_Virtual_Stack_MT.SIMILARITY:
				featuresModel = new SimilarityModel2D();
				break;
			case Register_Virtual_Stack_MT.AFFINE:
				featuresModel = new AffineModel2D();
				break;
			default:
				IJ.error("ERROR: unknown featuresModelIndex = " + Param.featuresModelIndex);
				return;
		}		
		
		// Array of inliers for every image with the consecutive one
		List< PointMatch >[] inliers = new ArrayList [sorted_file_names.length-1];
		CoordinateTransform[] transform = new CoordinateTransform [sorted_file_names.length];
		// Initialize arrays of image center coordinates
		centerX = new double[sorted_file_names.length];
		centerY = new double[sorted_file_names.length];
		
		// Set first transform to Identity
		transform[0] = new RigidModel2D();
		
		// FIRST LOOP (calculate correspondences and first RIGID solution)
		ArrayList<Feature>[] fs = new ArrayList[sorted_file_names.length];
		Future<ArrayList<Feature>> fu[] = new Future[sorted_file_names.length];
		try{
			// Extract features for all images
			for (int i=0; i<sorted_file_names.length; i++) 
			{
				IJ.showStatus("Extracting features from slices...");
				fu[i] = exe.submit(extractFeatures(p, source_dir + sorted_file_names[i], i));			
			}

			System.gc();

			// Join threads of feature extraction
			for (int i=0; i<sorted_file_names.length; i++) 	
			{
				IJ.showStatus("Extracting features " + (i+1) + "/" + sorted_file_names.length);
				IJ.showProgress((double) (i+1) / sorted_file_names.length);
				fs[i] = fu[i].get();
				fu[i] = null;
			}

			fu = null;
			
			// Match features				
			final Future<ArrayList<PointMatch>>[] fpm = new Future[sorted_file_names.length-1];
			// Loop over the sequence to select correspondences by pairs			
			for (int i=1; i<sorted_file_names.length; i++) 
			{							
				IJ.showStatus("Matching features...");							
				
				// Match features (create candidates)											
					
				// Filter candidates into inliers (concurrent way)
				try {
					fpm[i-1] = exe.submit(matchFeatures(p, fs[i], fs[i-1], featuresModel));
					
				} 
				catch ( NotEnoughDataPointsException e ) 
				{
					IJ.log("No features model found for file " + i + ": " + sorted_file_names[i]);
					// If the feature extraction does not find correspondences, then
					// only the elastic registration can be performed
					if(Param.registrationModelIndex != Register_Virtual_Stack_MT.ELASTIC)
					{
						IJ.error("No features model found for file " + i + ": " + sorted_file_names[i]);
						return;
					}
				}
			}
			// Join threads of feature matching
			for (int i=1; i<sorted_file_names.length; i++) 			
			{
				IJ.showStatus("Matching features " + (i+1) + "/" + sorted_file_names.length);
				IJ.showProgress((double) (i+1) / sorted_file_names.length);
				inliers[i-1] = fpm[i-1].get();
				fs[i-1].clear();
				if(inliers[i-1].size() < 2)
					IJ.log("Error: not model found for images " + sorted_file_names[i-1] + " and " + sorted_file_names[i] );
			}
			fs[sorted_file_names.length-1].clear();

			fs = null;

			System.gc();
			
			// Rigidly register
			for (int i=1; i<sorted_file_names.length; i++) 			
			{
				IJ.showStatus("Registering slice " + (i+1) + "/" + sorted_file_names.length);
				IJ.showProgress((double) (i+1) / sorted_file_names.length);
				
				// First approach with a RIGID transform (unless it is TRANSLATION)
				CoordinateTransform initialModel = (Param.registrationModelIndex == Register_Virtual_Stack_MT.TRANSLATION ) ? 
								  new TranslationModel2D()			
								: new RigidModel2D();
				// We apply first the previous transform to the points in the previous image
				inliers[i-1] = applyTransformReverse(inliers[i-1], transform[i-1]);
				if (Param.registrationModelIndex == Register_Virtual_Stack_MT.TRANSLATION) 
					((TranslationModel2D) initialModel).fit(inliers[i-1]);
				else 
					((RigidModel2D) initialModel).fit(inliers[i-1]);
				
				// Assign initial model
				transform[i] = initialModel;

			}				
		
			//  we apply transform to the points in the last image
			PointMatch.apply(inliers[inliers.length-1] , transform[transform.length-1]);
			
			if (Param.registrationModelIndex != Register_Virtual_Stack_MT.TRANSLATION )
			{
				// Relax points
				IJ.showStatus("Relaxing inliers...");
				if( !relax(inliers, transform, p) )
				{
					IJ.log("Error when relaxing inliers!");
					return;
				}

				// Clear inliers
				for(int i = 0; i < inliers.length; i++)
					inliers[i].clear();

				inliers = null;

				// Post-processing
				if( postprocess )
				{
					postProcessTransforms(transform);
				}
			}
			// Create final images.
			IJ.showStatus("Calculating final images...");
			if( !createResults(source_dir, sorted_file_names, target_dir, save_dir, exe, transform) )
			{
				IJ.log("Error when creating target images");
				return;
			}
		
		}catch (Exception e) {
			IJ.error("ERROR: " + e);
			e.printStackTrace();
		} finally {
			IJ.showProgress(1);
			IJ.showStatus("Done!");
			exe.shutdownNow();
		}			
		
	} // end method exec (non-shrinking)

	
	//-----------------------------------------------------------------------------------------
	/**
	 * Apply a transformation to the second point (P2) of a list of Point matches
	 * 
	 * @param list list of point matches
	 * @param t transformation to be applied
	 * 
	 * @return new list of point matches (after the transformation)
	 */
	public static List<PointMatch> applyTransformReverse(
			List<PointMatch> list,
			CoordinateTransform t) 
	{
		
		// We need to flip the point matches in order to apply the previous transform 
		List<PointMatch> new_list = (List<PointMatch>) PointMatch.flip(list);
		PointMatch.apply(new_list, t);
		// and flip back
		new_list = (List<PointMatch>) PointMatch.flip(new_list);
		
		return new_list;
	}

	//-----------------------------------------------------------------------------------------
	/**
	 * Relax inliers 
	 * 
	 * @param inliers array of list of inliers in the sequence (one per pair of slices) 
	 * @param transform array of relaxed transforms (output)
	 * @param p registration parameters
	 * @return true or false in case of proper result or error
	 */
	public static boolean relax(
			List<PointMatch>[] inliers,
			CoordinateTransform[] transform, 
			Param p) 			
	{			

		final boolean display = displayRelaxGraph;
		
		// Display mean distance
		int n_iterations = 0;
		float[] mean_distance = new float[MAX_ITER+1];		
		for(int iSlice = 0; iSlice < inliers.length; iSlice++)
			mean_distance[0] += PointMatch.meanDistance(inliers[iSlice]);
		
		mean_distance[0] /= inliers.length;
		
		
		// Array to keep order of relaxation
		int[] index = new int[inliers.length+1];
		for(int i = 0; i < index.length; i++)
			index[i] = i;
						
		for(int n = 0; n < MAX_ITER; n++)				
		{							
			n_iterations++;
			
			//IJ.log("Relax iteration " + n_iterations);
			
			// Randomize order of relaxation
			randomize(index);
			
			/*
			String s = new String("index order = [");
			for(int j = 0; j < index.length; j++)
				s += " " + index[j];
			s+= "]";
			IJ.log(s);
			*/
			
			for(int j = 0; j < index.length; j++)
			{
				final int iSlice = index[j];
												
				CoordinateTransform t = getCoordinateTransform(p);
				if(t instanceof CubicBSplineTransform)
				{
					( (CubicBSplineTransform) t ).set(p.elastic_param, (int)centerX[j]*2, (int)centerY[j]*2,
							(int)centerX[j]*2, (int)centerY[j]*2);
				}
				
				// First slice is treated in a special way
				if(iSlice == 0)
				{
					// First matches (we flip the matches in order to transform the first image too)

					ArrayList<PointMatch> firstMatches = new ArrayList <PointMatch>();
					PointMatch.flip(inliers[0], firstMatches);
					try{
						// Fit inliers given the registration model
						fitInliers(p, t, firstMatches);
						regularize(t, 0);

						// Update inliers (P1 of current slice and P2 in next slice)				
						inliers[0] = applyTransformReverse(inliers[0], t);

						// Update list of transforms
						transform[0] = t;										
					}
					catch(Exception e)
					{
						e.printStackTrace();
						IJ.error("Error when relaxing first matches...");
						return false;
					}
				}
				else
				{
					// Rest of matches				
					// Create list of combined inliers (each image with the previous and the next one). 
					List<PointMatch> combined_inliers = new ArrayList<PointMatch>(inliers[iSlice-1]);
					// if not last slice
					if(iSlice-1 < inliers.length-1)
					{
						// Combine matches with next slice
						ArrayList<PointMatch> flippedMatches = new ArrayList <PointMatch>();
						PointMatch.flip(inliers[iSlice], flippedMatches);
						for(final PointMatch match : flippedMatches )
							combined_inliers.add(match);
					}					

					//t = getCoordinateTransform(p);

					try{
						// Fit inliers given the registration model
						fitInliers(p, t, combined_inliers);
						regularize(t, iSlice);

						// Update inliers (P1 of current slice and P2 in next slice)
						PointMatch.apply(inliers[iSlice-1], t);
						if(iSlice-1 < inliers.length-1)
							inliers[iSlice] = applyTransformReverse(inliers[iSlice], t);

						// Update list of transforms
						transform[iSlice] = t;										
					}
					catch(Exception e)
					{
						e.printStackTrace();
						IJ.error("Error when relaxing...");
						return false;
					}
				
				}
			} // end for random indexes
			
			mean_distance[n+1] = 0;		
			for(int k = 0; k < inliers.length; k++)
				mean_distance[n+1] += PointMatch.meanDistance(inliers[k]);

			mean_distance[n+1] /= inliers.length;						

			if(Math.abs(mean_distance[n+1] - mean_distance[n]) < STOP_THRESHOLD)
				break;

			
		} // end for iterations (n)
		
		if(display)
		{
			// Plot mean distance		
			float[] x_label = new float[n_iterations+1];
			for(int i = 0; i < x_label.length; i++)
				x_label[i] = (float) i;
			float[] distance = new float[n_iterations+1];
			for(int i = 0; i < distance.length; i++)
				distance[i] = mean_distance[i];

			Plot pl = new Plot("Mean distance", "iterations", "MSE", x_label, distance);
			pl.setColor(Color.MAGENTA);
			pl.show();
		}
		
		return true;
	} // end method relax
	
	//-----------------------------------------------------------------------------------------	
	/**
	 * Randomize array of integers
	 * 
	 * @param array array of integers to randomize
	 */
	public static void randomize(final int[] array) 
	{
		Random generator = new Random();
		
		final int n = array.length;
		
		for(int i = 0; i < n; i ++)
		{
			final int randomIndex1 = generator.nextInt( n );
			final int randomIndex2 = generator.nextInt( n );
			// Swap values
			final int aux = array[randomIndex1];
			array[randomIndex1] = array[randomIndex2];
			array[randomIndex2] = aux;
		}			
		
	}// end method randomize
	
	//-----------------------------------------------------------------------------------------	
	/** 
	 * Create final target images  
	 * 
	 * @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 * @param sorted_file_names Array of sorted source file names.
	 * @param target_dir Directory to store registered slices into.
	 * @param save_dir Directory to store transform files into (null if transformations are not saved).
	 * @param exe executor service to save the images.
	 * @param transform array of transforms for every source image (including the first one).
	 * @return true or false in case of proper result or error
	 */
	public static boolean createResults(
			final String source_dir, 
			final String[] sorted_file_names,
			final String target_dir,
			final String save_dir,
			final ExecutorService exe,
			final CoordinateTransform[] transform) 
	{
		
		final ImagePlus first = IJ.openImage(source_dir + sorted_file_names[0]);
		
		// Common bounds to create common frame for all images
		final Rectangle commonBounds = new Rectangle(0, 0, first.getWidth(), first.getHeight());

		flush(first);

		// List of bounds in the forward registration
		final Rectangle bounds[] = new Rectangle[sorted_file_names.length];
			
		// Apply transform	
		ArrayList<Future<Boolean>> save_job = new ArrayList <Future<Boolean>>();
		for (int i=0; i<sorted_file_names.length; i++) 
		{
			save_job.add(exe.submit(applyTransformAndSave(source_dir, sorted_file_names[i], target_dir, transform[i], bounds, i)));
		}


		// Wait for the intermediate output files to be saved
		int ind = 0;
		for (Iterator<Future<Boolean>> it = save_job.iterator(); it.hasNext(); )
		{
			ind++;
			Boolean saved_file = null;
			try{
				IJ.showStatus("Applying transform " + (ind+1) + "/" + sorted_file_names.length);
				saved_file = it.next().get();
				it.remove(); // so list doesn't build up anywhere with Callable-s that have been called already.				
				System.gc();
			} catch (InterruptedException e) {
				IJ.error("Interruption exception!");
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				IJ.error("Execution exception!");
				e.printStackTrace();
				return false;
			}
			
			if( saved_file.booleanValue() == false)
			{
				IJ.log("Error while saving: " +  makeTargetPath(target_dir, sorted_file_names[ind]));
				return false;
			}
			
		}

		save_job = null;
		
		for (int k=0; k<sorted_file_names.length; k++) 
		{
			// Update common bounds
			int min_x = commonBounds.x;
			int min_y = commonBounds.y;
			int max_x = commonBounds.x + commonBounds.width;
			int max_y = commonBounds.y + commonBounds.height;
			
			if(bounds[k].x < commonBounds.x)
				min_x = bounds[k].x;
			if(bounds[k].y < commonBounds.y)
				min_y = bounds[k].y;
			if(bounds[k].x + bounds[k].width > max_x)
				max_x = bounds[k].x + bounds[k].width;
			if(bounds[k].y + bounds[k].height > max_y)
				max_y = bounds[k].y + bounds[k].height;
			
			commonBounds.x = min_x;
			commonBounds.y = min_y;
			commonBounds.width = max_x - min_x;
			commonBounds.height = max_y - min_y;
		}
		
		
		
		//IJ.log("\nFinal common bounding box = [" + commonBounds.x + " " + commonBounds.y + " " + commonBounds.width + " " + commonBounds.height + "]");
		
		// Adjust Forward bounds
		for ( int i = 0; i < bounds.length; ++i )
		{
			final Rectangle b = bounds[i];
			b.x -= commonBounds.x;
			b.y -= commonBounds.y;
		}

		// Reopen all target images and repaint them on an enlarged canvas
		IJ.showStatus("Resizing images...");
		ArrayList<Future<String>> names = new ArrayList<Future<String>>();
		for (int i=0; i<sorted_file_names.length; i++) 
		{
			final Rectangle b = bounds[i];
			names.add(exe.submit(resizeAndSaveImage(makeTargetPath(target_dir, sorted_file_names[i]), b.x, b.y, commonBounds.width, commonBounds.height)));
		}
		

		// Join all and create VirtualStack
		final VirtualStack stack = new VirtualStack(commonBounds.width, commonBounds.height, null, target_dir);
		ind = 0;
		for (Iterator<Future<String>> it1 = names.iterator(); it1.hasNext(); ind++) {
			String filename = null;
			try {
				IJ.showStatus("Resizing image " + (ind+1) + "/" + sorted_file_names.length);
				filename = it1.next().get();
				it1.remove(); // so list doesn't build up anywhere with Callable-s that have been called already.
				System.gc();
			} catch (InterruptedException e) {
				IJ.error("Interruption exception!");
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				IJ.error("Execution exception!");
				e.printStackTrace();
				return false;
			}
			if (null == filename) {
				IJ.log("Image failed: " + filename);
				return false;
			}
			stack.addSlice(filename);
		}

		names.clear();

		// Show registered stack
		new ImagePlus("Registered " + new File(source_dir).getName(), stack).show();
		
		// Save transforms
		if(save_dir != null)
		{
			saveTransforms(transform, save_dir, sorted_file_names, exe);			
		}

		IJ.showStatus("Done!");
		
		return true;
	}
	
	//-----------------------------------------------------------------------------------------
	/**
	 * Save transforms into XML files.
	 * @param transform array of transforms.
	 * @param save_dir directory to save transforms into.
	 * @param sorted_file_names array of sorted file image names.
	 * @param exe executor service to run everything concurrently.
	 * @return true if every file is save correctly, false otherwise.
	 */
	private static boolean saveTransforms(CoordinateTransform[] transform,
			String save_dir, String[] sorted_file_names, ExecutorService exe) 
	{
		
		final Future<String>[] jobs = new Future[transform.length];
		
		for(int i = 0; i < transform.length; i ++)
		{
			jobs[i] = exe.submit(saveTransform(makeTransformPath(save_dir, sorted_file_names[i]), transform[i]) ); 
		}
		// Join
		for (final Future<String> job : jobs) {
			String filename = null;
			try {
				filename = job.get();
			} catch (InterruptedException e) {
				IJ.error("Interruption exception!");
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				IJ.error("Execution exception!");
				e.printStackTrace();
				return false;
			}
			if (null == filename) {
				IJ.log("Not able to save file: " + filename);
				return false;
			}
		}
		return true;
	}
	
	//-----------------------------------------------------------------------------------------	
	/**
	 * Execution method. Execute registration when all parameters are set.
	 * 
	 * @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 * @param sorted_file_names Array of sorted source file names.
	 * @param referenceIndex index of the reference image in the array of sorted source images.
	 * @param target_dir Directory to store registered slices into.
	 * @param save_dir Directory to store transform files into.
	 * @param p registration parameters.
	 */
	static public void exec(
			final String source_dir, 
			final String[] sorted_file_names,
			final int referenceIndex,
			final String target_dir, 
			final String save_dir,
			final Param p) 
	{
		// Check if source and output directories are different
		if (source_dir.equals(target_dir)) 
		{
			IJ.error("Source and target directories MUST be different\n or images would get overwritten.\nDid NOT register stack slices.");
			return;
		}
		// Check if the registration model is known
		if (Param.registrationModelIndex < TRANSLATION || Param.registrationModelIndex > MOVING_LEAST_SQUARES) 
		{
			IJ.error("Don't know how to process registration type " + Param.registrationModelIndex);
			return;
		}
				
		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {			

			ImagePlus imp1 = null;
			ImagePlus imp2 = IJ.openImage(source_dir + sorted_file_names[referenceIndex]);
			imp2.killRoi();
			
			// Masks
			ImagePlus imp1mask = new ImagePlus();
			ImagePlus imp2mask = new ImagePlus();
			
			// Common bounds to create common frame for all images
			final Rectangle commonBounds = new Rectangle(0, 0, imp2.getWidth(), imp2.getHeight());
			
			// List of bounds in the forward registration
			final List<Rectangle> boundsFor = new ArrayList<Rectangle>();
			boundsFor.add(new Rectangle(0, 0, imp2.getWidth(), imp2.getHeight()));
			
			// Save the reference image, untouched:
			exe.submit(saveImage(imp2, makeTargetPath(target_dir, sorted_file_names[referenceIndex])));

			// Array of resulting coordinate transforms
			CoordinateTransform[] transform = new CoordinateTransform[sorted_file_names.length];
			
			// Forward registration (from reference image to the end of the sequence)			
			for (int i=referenceIndex+1; i<sorted_file_names.length; i++) 
			{												
				// Shift images
				imp1 = imp2;
				imp1mask = imp2mask;
				// Create empty mask for second image
				imp2mask = new ImagePlus();
				imp2 = IJ.openImage(source_dir + sorted_file_names[i]);
				imp2.killRoi();
				
				// Select coordinate transform based on the registration model
				final CoordinateTransform t = getCoordinateTransform(p);	
				// Register (the transformed image is stored in imp2 and the transformed mask
				// on imp2mask)
				if(!register( imp1, imp2, imp1mask, imp2mask, i, sorted_file_names,
						  source_dir, target_dir, exe, p, t, commonBounds, boundsFor, referenceIndex))
					return;		
				// Store transform
				transform[i] = t;
			}
			
			imp2 = null;
			imp2mask = new ImagePlus();
			
			// Reference
			transform[referenceIndex] = new AffineModel2D();
			
			// Backward registration (from reference image to the beginning of the sequence)
			imp2 = IJ.openImage(source_dir + sorted_file_names[referenceIndex]);
			// Backward bounds
			final List<Rectangle> boundsBack = new ArrayList<Rectangle>();
			boundsBack.add(new Rectangle(0, 0, imp2.getWidth(), imp2.getHeight()));
			
			for (int i = referenceIndex-1; i >= 0; i--) 
			{				
				// Shift images
				imp1 = imp2;
				imp1mask = imp2mask;
				// Create empty mask for second image
				imp2mask = new ImagePlus();
				imp2 = IJ.openImage(source_dir + sorted_file_names[i]);
				imp2.killRoi();
				
				// Select coordinate transform based on the registration model
				final CoordinateTransform t = getCoordinateTransform(p);	
				// Register (the transformed image is stored in imp2 and the transformed mask
				// on imp2mask)
				if(!register( imp1, imp2, imp1mask, imp2mask, i, sorted_file_names,
						  source_dir, target_dir, exe, p, t, commonBounds, boundsBack, referenceIndex))
					return;		
				// Store transform
				transform[i] = t;
			}
			
			// Adjust transforms to the right position.
			// Since the transforms are relative to the previous image, we have 
			// the images to translate them to the origin of the previous image. 
			for(int i = 1, j = referenceIndex+1 ; i < boundsFor.size(); i++, j++)
			{
				final Rectangle b = boundsFor.get(i-1);
				// Copy coordinate transform with corresponding translation
				final CoordinateTransformList<CoordinateTransform> ctl = new CoordinateTransformList<CoordinateTransform>();
				ctl.add(transform[j]);
				
				
				final TranslationModel2D tr = new TranslationModel2D();
				tr.set(b.x, b.y);
				ctl.add(tr);
				
				transform[j] = ctl;				
			}
			for(int i = 1, j = referenceIndex-1 ; i < boundsBack.size(); i++, j--)
			{
				final Rectangle b = boundsBack.get(i-1);
				// Copy coordinate transform with corresponding translation
				final CoordinateTransformList<CoordinateTransform> ctl = new CoordinateTransformList<CoordinateTransform>();
				ctl.add(transform[j]);
				
				
				final TranslationModel2D tr = new TranslationModel2D();
				tr.set(b.x, b.y);
				ctl.add(tr);
				
				transform[j] = ctl;	
			}
			
			
			// Adjust Forward bounds
			for ( int i = 0; i < boundsFor.size(); ++i )
			{
				final Rectangle b = boundsFor.get(i);
				b.x -= commonBounds.x;
				b.y -= commonBounds.y;
			}
			
			// Adjust Backward bounds
			for ( int i = 0; i < boundsBack.size(); ++i )
			{
				final Rectangle b = boundsBack.get(i);
				b.x -= commonBounds.x;
				b.y -= commonBounds.y;
			}
			
			//IJ.log("Common bounds = " + commonBounds.x + " " + commonBounds.y + " " + commonBounds.width + " " + commonBounds.height);
			
			// Reopen all target images and repaint them on an enlarged canvas
			final Future<String>[] jobs = new Future[sorted_file_names.length];						
			
			for (int j = 0, i=referenceIndex; i<sorted_file_names.length; i++, j++) 
			{
				final Rectangle b = boundsFor.get(j);								
				
				//IJ.log(i+": " + b.x + " " + b.y);
				
				// Resize and save;
				jobs[i] = exe.submit(resizeAndSaveImage(makeTargetPath(target_dir, sorted_file_names[i]), b.x, b.y, commonBounds.width, commonBounds.height));				
			}
			
			
			
			for (int j=1, i=referenceIndex-1; i>=0; i--, j++) 
			{
				final Rectangle b = boundsBack.get(j);
				
				//IJ.log(i+": " + b.x + " " + b.y);												
				// Resize and save
				jobs[i] = exe.submit(resizeAndSaveImage(makeTargetPath(target_dir, sorted_file_names[i]), b.x, b.y, commonBounds.width, commonBounds.height));								
			}
			

			// Join all and create VirtualStack
			final VirtualStack stack = new VirtualStack(commonBounds.width, commonBounds.height, null, target_dir);
			for (final Future<String> job : jobs) 
			{
				String filename = job.get();
				if (null == filename) 
				{
					IJ.log("Image failed: " + filename);
					return;
				}
				stack.addSlice(filename);
			}
			
			// Save transforms
			if(save_dir != null)
			{
				saveTransforms(transform, save_dir, sorted_file_names, exe);			
			}

			// Show registered stack
			new ImagePlus("Registered " + new File(source_dir).getName(), stack).show();

			IJ.showStatus("Done!");

		} catch (Exception e) {
			IJ.error("ERROR: " + e);
			e.printStackTrace();
		} finally {
			IJ.showProgress(1);
			exe.shutdownNow();
		}
	} // end method exec

	//-----------------------------------------------------------------------------------------
	/**
	 * Resize an image to a new size and save it 
	 * 
	 * @param path saving path
	 * @param x x- image origin (offset)
	 * @param y y- image origin (offset)
	 * @param width final image width
	 * @param height final image height
	 * @return file name of the saved image, or null if there was an error
	 */
	static private Callable<String> resizeAndSaveImage(final String path, final int x, final int y, final int width, final int height) 
	{
		return new Callable<String>() {
			public String call() {
				try {					
					ImagePlus imp = IJ.openImage(path);
					if (null == imp) {
						IJ.log("Could not open target image at " + path);
						return null;
					}
					ImageProcessor ip = imp.getProcessor().createProcessor(width, height);
					// Color images are white by default: fill with black
					if (imp.getType() == ImagePlus.COLOR_RGB) 
					{
						ip.setRoi(0, 0, width, height);
						ip.setValue(0);
						ip.fill();
					}
					ip.insert(imp.getProcessor(), x, y);					
					ImagePlus big = new ImagePlus(imp.getTitle(), ip);
					big.setCalibration(imp.getCalibration());
					flush(imp);
					imp = null;
					ip = null;
					if (! new FileSaver(big).saveAsTiff(path)) {
						return null;
					}
					flush(big);
					big = null;
					
					System.gc();
					
					return new File(path).getName();
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		};
	} // end resizeAndSaveImage method
	
	//-----------------------------------------------------------------------------------------
	/**
	 * Save transform into a file
	 * 
	 * @param path saving path and file name
	 * @param t coordinate transform to save
	 * @return file name of the saved file, or null if there was an error
	 */
	static private Callable<String> saveTransform(final String path, final CoordinateTransform t) 
	{
		return new Callable<String>() {
			public String call() {
				try {
					final FileWriter fw = new FileWriter(path);
					fw.write(t.toXML(""));
					fw.close();
					return new File(path).getName();
				} catch (Exception e) {
					e.printStackTrace();					
					return null;
				}
			}
		};
	} // end saveTransform method
	//-----------------------------------------------------------------------------------------
	/**
	 * Make target (output) path by adding the output directory and the file name.
	 * File names are forced to have ".tif" extension.
	 * 
	 * @param dir output directory.
	 * @param name output file name.
	 * @return complete path for the target image.
	 */
	static private String makeTargetPath(final String dir, final String name) 
	{
		String filepath = dir + name;
		if (! name.toLowerCase().matches("^.*ti[f]{1,2}$")) 
			filepath += ".tif";
		return filepath;
	}	
	
	//-----------------------------------------------------------------------------------------
	/**
	 * Make transform file path.
	 * File names are forced to have ".xml" extension.
	 * 
	 * @param dir output directory.
	 * @param name output file name.
	 * @return complete path for the transform file.
	 */
	static private String makeTransformPath(final String dir, final String name) 
	{
		final int i = name.lastIndexOf(".");
		final String no_ext = name.substring(0, i+1);
		return dir + no_ext + "xml";		
	}	

	//-----------------------------------------------------------------------------------------
	/**
	 * Generate object to concurrently extract features
	 * 
	 * @param p feature extraction parameters
	 * @param ip input image
	 * @return list of extracted features
	 */
	private static  Callable<ArrayList<Feature>> extractFeatures(final Param p, final ImageProcessor ip) {
		return new Callable<ArrayList<Feature>>() {
			public ArrayList<Feature> call() {
				final ArrayList<Feature> fs = new ArrayList<Feature>();
				new SIFT( new FloatArray2DSIFT( p.sift ) ).extractFeatures(ip, fs);
				return fs;
			}
		};
	}


	//-----------------------------------------------------------------------------------------
	/**
	 * Generate object to concurrently extract features
	 * 
	 * @param p feature extraction parameters
	 * @param ip input image
	 * @return list of extracted features
	 */
	private static  Callable<ArrayList<Feature>> extractFeatures(final Param p, final String path, final int index) {
		return new Callable<ArrayList<Feature>>() {
			public ArrayList<Feature> call() 
			{
				
				ImagePlus imp = IJ.openImage(path);
				centerX[index] = imp.getWidth() / 2;
				centerY[index] = imp.getHeight() / 2;
				ArrayList<Feature> fs = new ArrayList<Feature>();
				new SIFT( new FloatArray2DSIFT( p.sift ) ).extractFeatures(imp.getProcessor(), fs);
				flush(imp);
				imp = null;

				System.gc();
				
				return fs;
			}
		};
	}
	
	
	//-----------------------------------------------------------------------------------------
	/**
	 * Generate object to concurrently save an image
	 * 
	 * @param imp image to save
	 * @param path output path
	 * @return true if the image was saved correctly
	 */
	private static Callable<Boolean> saveImage(final ImagePlus imp, final String path) 
	{
		return new Callable<Boolean>() {
			public Boolean call() {
				try {
					return new FileSaver(imp).saveAsTiff(path);
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
		};
	}
	
	//-----------------------------------------------------------------------------------------
	/**
	 * Concurrently apply a transform and save the resulting image
	 * 
	 */
	private static Callable<Boolean> applyTransformAndSave(
			final String source_dir, 
			final String file_name, 
			final String target_dir, 
			final CoordinateTransform transform,
			final Rectangle[] bounds,
			final int i) 
	{
		return new Callable<Boolean>() {
			public Boolean call() {
				// Open next image
				final ImagePlus imp2 = IJ.openImage(source_dir + file_name);
				// Calculate transform mesh
				TransformMesh mesh = new TransformMesh(transform, 32, imp2.getWidth(), imp2.getHeight());
				TransformMeshMapping mapping = new TransformMeshMapping(mesh);
							
				// Create interpolated deformed image with black background
				imp2.getProcessor().setValue(0);
				imp2.setProcessor(imp2.getTitle(), mapping.createMappedImageInterpolated(imp2.getProcessor()));						
				
				//imp2.show();

				// Accumulate bounding boxes, so in the end they can be reopened and re-saved with an enlarged canvas.
				final Rectangle currentBounds = mesh.getBoundingBox();			
				bounds[i] = currentBounds;									
				
				// Save target image
				return new FileSaver(imp2).saveAsTiff(makeTargetPath(target_dir, file_name));
			}
		};
	}
	
	//-----------------------------------------------------------------------------------------	
	/**
	 * Match features into inliers in a concurrent way
	 * 
	 * @param p registration parameters
	 * @param fs2 collection of features to match
	 * @param fs1 collection of features to match
	 * @param featuresModel features model 
	 * @return list of matched features
	 * @throws Exception if not enough points
	 */
	private static Callable<ArrayList<PointMatch>> matchFeatures(
			final Param p, 
			final Collection<Feature> fs2, 
			final Collection<Feature> fs1, 
			final Model<?> featuresModel ) throws Exception
	{
		return new Callable<ArrayList<PointMatch>>(){
			public ArrayList<PointMatch> call() throws Exception{
				// Match features (create candidates)
				final List< PointMatch > candidates = new ArrayList< PointMatch >();
				FeatureTransform.matchFeatures( fs2, fs1, candidates, Param.rod );

				final ArrayList<PointMatch> inliers = new ArrayList< PointMatch >();								

				// Filter candidates into inliers

				featuresModel.filterRansac(
						candidates,
						inliers,
						1000,
						Param.maxEpsilon,
						Param.minInlierRatio );
				return inliers;
			}
		};
		
	}// end method matchFeatures
	
	//-----------------------------------------------------------------------------------------
	/**
	 * Register two images with corresponding masks and 
	 * following the features and registration models.
	 * 
	 * @param imp1 target image
	 * @param imp2 source image (input and output)
	 * @param imp1mask target mask 
	 * @param imp2mask source mask (input and output)
	 * @param i index in the loop of images (just to show information)
	 * @param sorted_file_names array of sorted source file names
	 * @param source_dir source directory
	 * @param target_dir target (output) directory
	 * @param exe executor service to save the images
	 * @param p registration parameters
	 * @param t coordinate transform
	 * @param commonBounds current common bounds of the registration space
	 * @param bounds list of bounds for the already registered images
	 * @param referenceIndex index of the reference image
	 * @return false if there is an error, true otherwise
	 * @throws Exception if something fails
	 */
	public static boolean register(
			ImagePlus imp1, 
			ImagePlus imp2,
			ImagePlus imp1mask,
			ImagePlus imp2mask,
			final int i,
			final String[] sorted_file_names,
			final String source_dir,
			final String target_dir,
			final ExecutorService exe,
			final Param p,
			CoordinateTransform t,
			Rectangle commonBounds,
			List<Rectangle> bounds,
			final int referenceIndex) throws Exception
	{
		// Update progress bar
		IJ.showStatus("Registering slice " + (i+1) + "/" + sorted_file_names.length);		
		
		// Extract SIFT features				
		Future<ArrayList<Feature>> fu1 = exe.submit(extractFeatures(p, imp1.getProcessor()));
		Future<ArrayList<Feature>> fu2 = exe.submit(extractFeatures(p, imp2.getProcessor()));
		ArrayList<Feature> fs1 = fu1.get();
		ArrayList<Feature> fs2 = fu2.get();
		
		final List< PointMatch > candidates = new ArrayList< PointMatch >();
		FeatureTransform.matchFeatures( fs2, fs1, candidates, Param.rod );

		final List< PointMatch > inliers = new ArrayList< PointMatch >();

		// Select features model
		Model< ? > featuresModel;
		switch ( Param.featuresModelIndex )
		{
			case Register_Virtual_Stack_MT.TRANSLATION:
				featuresModel = new TranslationModel2D();
				break;
			case Register_Virtual_Stack_MT.RIGID:
				featuresModel = new RigidModel2D();
				break;
			case Register_Virtual_Stack_MT.SIMILARITY:
				featuresModel = new SimilarityModel2D();
				break;
			case Register_Virtual_Stack_MT.AFFINE:
				featuresModel = new AffineModel2D();
				break;
			default:
				IJ.error("ERROR: unknown featuresModelIndex = " + Param.featuresModelIndex);
				return false;
		}
			
		// Filter candidates into inliers
		try {
			featuresModel.filterRansac(
					candidates,
					inliers,
					1000,
					Param.maxEpsilon,
					Param.minInlierRatio );
		} 
		catch ( NotEnoughDataPointsException e ) 
		{
			IJ.log("No features model found for file " + i + ": " + sorted_file_names[i]);
			// If the feature extraction does not find correspondences, then
			// only the elastic registration can be performed
			if(Param.registrationModelIndex != Register_Virtual_Stack_MT.ELASTIC)
			{
				IJ.error("No features model found for file " + i + ": " + sorted_file_names[i]);
				return false;
			}
		}
	
		// Generate registered image, put it into imp2 and save it
		switch (Param.registrationModelIndex) 
		{
			case Register_Virtual_Stack_MT.TRANSLATION:
			case Register_Virtual_Stack_MT.SIMILARITY:
			case Register_Virtual_Stack_MT.RIGID:
			case Register_Virtual_Stack_MT.AFFINE:
				((Model<?>)t).fit(inliers);
				break;
			case Register_Virtual_Stack_MT.ELASTIC:
				// set inliers as a PointRoi, and set masks
				//call_bUnwarpJ(...);
				//imp1.show();
				//imp2.show();
				final List< Point > sourcePoints = new ArrayList<Point>();
				final List< Point > targetPoints = new ArrayList<Point>();
				if(inliers.size() != 0)
				{
					PointMatch.sourcePoints( inliers, sourcePoints );
					PointMatch.targetPoints( inliers, targetPoints );
					
					imp2.setRoi( Util.pointsToPointRoi(sourcePoints) );
					imp1.setRoi( Util.pointsToPointRoi(targetPoints) );
				}
				
				//imp1.show();
				//ImagePlus aux = new ImagePlus("source", imp2.getProcessor().duplicate());
				//aux.setRoi( Util.pointsToPointRoi(targetPoints) );
				//aux.show();
				//if(imp1mask != null) new ImagePlus("mask", imp1mask).show();
				
				// Tweak initial affine transform based on the chosen model
				if(Param.featuresModelIndex < Register_Virtual_Stack_MT.AFFINE)
				{
					// Remove shearing 
					p.elastic_param.setShearCorrection(1.0);
					// Remove anisotropy
					p.elastic_param.setAnisotropyCorrection(1.0);
					if(Param.featuresModelIndex < Register_Virtual_Stack_MT.SIMILARITY)
					{
						// Remove scaling
						p.elastic_param.setScaleCorrection(1.0);
					}
				}
				
				// Perform registration
				ImageProcessor mask1 = imp1mask.getProcessor() == null ? null : imp1mask.getProcessor();
				ImageProcessor mask2 = imp2mask.getProcessor() == null ? null : imp2mask.getProcessor();
			
				if(debug )
				{
					IJ.log("\nsource  "+ i + ": " + imp1.getOriginalFileInfo().directory + imp1.getOriginalFileInfo().fileName);
					IJ.log("target "+ i + ": " + imp2.getOriginalFileInfo().directory + imp2.getOriginalFileInfo().fileName);
				}
				Transformation warp = bUnwarpJ_.computeTransformationBatch(imp2, imp1, mask2, mask1, p.elastic_param);
				
				// take the mask from the results
				//final ImagePlus output_ip = warp.getDirectResults();
				//output_ip.setTitle("result " + i);
				
				//imp2mask.setProcessor(imp2mask.getTitle(), output_ip.getStack().getProcessor(3));
				//output_ip.show();
				
				
				// Store result in a Cubic B-Spline transform
				((CubicBSplineTransform) t).set(warp.getIntervals(), warp.getDirectDeformationCoefficientsX(), warp.getDirectDeformationCoefficientsY(),
                		imp2.getWidth(), imp2.getHeight());
				break;
			case Register_Virtual_Stack_MT.MOVING_LEAST_SQUARES:
				((MovingLeastSquaresTransform)t).setModel(AffineModel2D.class);
				((MovingLeastSquaresTransform)t).setAlpha(1); // smoothness
				((MovingLeastSquaresTransform)t).setMatches(inliers);
				break;
		}

		// Calculate transform mesh
		TransformMesh mesh = new TransformMesh(t, 32, imp2.getWidth(), imp2.getHeight());
		TransformMeshMapping mapping = new TransformMeshMapping(mesh);
		
		// Create interpolated mask (only for elastic registration)
		if(Param.registrationModelIndex == Register_Virtual_Stack_MT.ELASTIC)
		{
			imp2mask.setProcessor(imp2mask.getTitle(), new ByteProcessor(imp2.getWidth(), imp2.getHeight()));
			imp2mask.getProcessor().setValue(255);
			imp2mask.getProcessor().fill();
			imp2mask.setProcessor(imp2mask.getTitle(), mapping.createMappedImageInterpolated(imp2mask.getProcessor() ) );
		}
		// Create interpolated deformed image with black background
		imp2.getProcessor().setValue(0);
		imp2.setProcessor(imp2.getTitle(), mapping.createMappedImageInterpolated(imp2.getProcessor()));						
		
		// Accumulate bounding boxes, so in the end they can be reopened and re-saved with an enlarged canvas.
		final Rectangle currentBounds = mesh.getBoundingBox();
		final Rectangle previousBounds = bounds.get( bounds.size() - 1 );
		currentBounds.x += previousBounds.x;
		currentBounds.y += previousBounds.y;
		bounds.add(currentBounds);
		
		//IJ.log(i + ": current bounding box = [" + currentBounds.x + " " + currentBounds.y + " " + currentBounds.width + " " + currentBounds.height + "]");
		
		// Update common bounds
		int min_x = commonBounds.x;
		int min_y = commonBounds.y;
		int max_x = commonBounds.x + commonBounds.width;
		int max_y = commonBounds.y + commonBounds.height;
		
		if(currentBounds.x < commonBounds.x)
			min_x = currentBounds.x;
		if(currentBounds.y < commonBounds.y)
			min_y = currentBounds.y;
		if(currentBounds.x + currentBounds.width > max_x)
			max_x = currentBounds.x + currentBounds.width;
		if(currentBounds.y + currentBounds.height > max_y)
			max_y = currentBounds.y + currentBounds.height;
		
		commonBounds.x = min_x;
		commonBounds.y = min_y;
		commonBounds.width = max_x - min_x;
		commonBounds.height = max_y - min_y;
		
		// Save target image
		exe.submit(saveImage(imp2, makeTargetPath(target_dir, sorted_file_names[i])));		
		return true;
	} //end method register
	
	// -------------------------------------------------------------------
	/**
	 * Get a new coordinate transform given a registration model
	 * 
	 * @param p registration parameters
	 * @return new coordinate transform
	 */
	public static CoordinateTransform getCoordinateTransform(Param p)
	{
		CoordinateTransform t;
		switch (Param.registrationModelIndex) 
		{
			case Register_Virtual_Stack_MT.TRANSLATION: t = new TranslationModel2D(); break;
			case Register_Virtual_Stack_MT.RIGID: t = new RigidModel2D(); break;
			case Register_Virtual_Stack_MT.SIMILARITY: t = new SimilarityModel2D(); break;
			case Register_Virtual_Stack_MT.AFFINE: t = new AffineModel2D(); break;
			case Register_Virtual_Stack_MT.ELASTIC: t = new CubicBSplineTransform(); break;
			case Register_Virtual_Stack_MT.MOVING_LEAST_SQUARES: t = new MovingLeastSquaresTransform(); break;
			default:
				IJ.log("ERROR: unknown registrationModelIndex = " + Param.registrationModelIndex);
				return null;
		}
		return t;
	}
	// -------------------------------------------------------------------
	/**
	 * Fit inliers given a registration model
	 * 
	 * @param p registration parameters
	 * @param t coordinate transform
	 * @param inliers point matches
	 * @throws Exception if something fails
	 */
	public static void fitInliers(Param p, CoordinateTransform t, List< PointMatch > inliers) throws Exception
	{
		switch (Param.registrationModelIndex) 
		{
			case Register_Virtual_Stack_MT.TRANSLATION:
			case Register_Virtual_Stack_MT.SIMILARITY:
			case Register_Virtual_Stack_MT.RIGID:
			case Register_Virtual_Stack_MT.AFFINE:
			case Register_Virtual_Stack_MT.ELASTIC:
				((Model<?>)t).fit(inliers);							
				break;													
			case Register_Virtual_Stack_MT.MOVING_LEAST_SQUARES:
				((MovingLeastSquaresTransform)t).setModel(AffineModel2D.class);
				((MovingLeastSquaresTransform)t).setAlpha(1); // smoothness
				((MovingLeastSquaresTransform)t).setMatches(inliers);
				break;
		}
		return;
	} // end method fitInliers
	
	// -------------------------------------------------------------------
	/**
	 * Regularize coordinate transform
	 * 
	 * @param t coordinate transform
	 * @param index slice index
	 */
	public static void regularize(CoordinateTransform t, int index)
	{
		if( t instanceof AffineModel2D || t instanceof SimilarityModel2D )
		{
			final AffineTransform a = (t instanceof AffineModel2D) ? ((AffineModel2D)t).createAffine() : ((SimilarityModel2D)t).createAffine();
			
			// Move to the center of the image
			a.translate(centerX[index], centerY[index]);
			
			/*
			IJ.log(" A: " + a.getScaleX() + " " + a.getShearY() + " " + a.getShearX()
					+ " " + a.getScaleY() + " " + a.getTranslateX() + " " + 
					+ a.getTranslateY() );
					*/
			
			// retrieves scaling, shearing, rotation and translation from an affine
			// transformation matrix A (which has translation values in the right column)
			// by Daniel Berger for MIT-BCS Seung, April 19 2009

			// We assume that sheary=0
			// scalex=sqrt(A(1,1)*A(1,1)+A(2,1)*A(2,1));
			final double a11 = a.getScaleX();
			final double a21 = a.getShearY();
			final double scaleX = Math.sqrt( a11 * a11 + a21 * a21 );
			// rotang=atan2(A(2,1)/scalex,A(1,1)/scalex);
			final double rotang = Math.atan2( a21/scaleX, a11/scaleX);

			// R=[[cos(-rotang) -sin(-rotang)];[sin(-rotang) cos(-rotang)]];
			
			// rotate back shearx and scaley
			//v=R*[A(1,2) A(2,2)]';
			final double a12 = a.getShearX();
			final double a22 = a.getScaleY();
			final double shearX = Math.cos(-rotang) * a12 - Math.sin(-rotang) * a22;
			final double scaleY = Math.sin(-rotang) * a12 + Math.cos(-rotang) * a22;

			// rotate back translation
			// v=R*[A(1,3) A(2,3)]';
			final double transX = Math.cos(-rotang) * a.getTranslateX() - Math.sin(-rotang) * a.getTranslateY();
			final double transY = Math.sin(-rotang) * a.getTranslateX() + Math.cos(-rotang) * a.getTranslateY();
			
			// TWEAK		
			
			final double new_shearX = shearX * (1.0 - tweakShear); 
			//final double new_shearY = 0; // shearY * (1.0 - tweakShear);
			
			final double avgScale = (scaleX + scaleY)/2;
		    final double aspectRatio = scaleX / scaleY;
		    final double regAvgScale = avgScale * (1.0 - tweakScale) + 1.0  * tweakScale;
		    final double regAspectRatio = aspectRatio * (1.0 - tweakIso) + 1.0 * tweakIso;
		    
		    //IJ.log("avgScale = " + avgScale + " aspectRatio = " + aspectRatio + " regAvgScale = " + regAvgScale + " regAspectRatio = " + regAspectRatio);
		    
		    final double new_scaleY = (2.0 * regAvgScale) / (regAspectRatio + 1.0);
		    final double new_scaleX = regAspectRatio * new_scaleY;
			
			final AffineTransform b = makeAffineMatrix(new_scaleX, new_scaleY, new_shearX, 0, rotang, transX, transY);
									
		    //IJ.log("new_scaleX = " + new_scaleX + " new_scaleY = " + new_scaleY + " new_shearX = " + new_shearX + " new_shearY = " + new_shearY);		    		    		    
			
			// Move back the center
			b.translate(-centerX[index], -centerY[index]);
			
			if(t instanceof AffineModel2D)
				((AffineModel2D)t).set( b );
			else
				((SimilarityModel2D)t).set( (float) b.getScaleX(), (float) b.getShearY(), (float) b.getTranslateX(), (float) b.getTranslateY() );			
		}		
		
										
	}// end method regularize
	
	//---------------------------------------------------------------------------------
	/**
	 * Makes an affine transformation matrix from the given scale, shear,
	 * rotation and translation values
     * if you want a uniquely retrievable matrix, give sheary=0
     * 
	 * @param scalex scaling in x
	 * @param scaley scaling in y
	 * @param shearx shearing in x
	 * @param sheary shearing in y
	 * @param rotang angle of rotation (in radians)
	 * @param transx translation in x
	 * @param transy translation in y
	 * @return affine transformation matrix
	 */
	public static AffineTransform makeAffineMatrix(
			final double scalex, 
			final double scaley, 
			final double shearx, 
			final double sheary, 
			final double rotang, 
			final double transx, 
			final double transy)
	{
		/*
		%makes an affine transformation matrix from the given scale, shear,
		%rotation and translation values
		%if you want a uniquely retrievable matrix, give sheary=0
		%by Daniel Berger for MIT-BCS Seung, April 19 2009

		A=[[scalex shearx transx];[sheary scaley transy];[0 0 1]];
		A=[[cos(rotang) -sin(rotang) 0];[sin(rotang) cos(rotang) 0];[0 0 1]] * A;
		*/
		
		final double m00 = Math.cos(rotang) * scalex - Math.sin(rotang) * sheary;
		final double m01 = Math.cos(rotang) * shearx - Math.sin(rotang) * scaley;
		final double m02 = Math.cos(rotang) * transx - Math.sin(rotang) * transy;
		
		final double m10 = Math.sin(rotang) * scalex + Math.cos(rotang) * sheary;
		final double m11 = Math.sin(rotang) * shearx + Math.cos(rotang) * scaley;
		final double m12 = Math.sin(rotang) * transx + Math.cos(rotang) * transy;
		
		return new AffineTransform( m00,  m10,  m01,  m11,  m02,  m12);		
	} // end method makeAffineMatrix
	
	//---------------------------------------------------------------------------------
    /**
     * Shows regularization dialog when "Shrinkage constrain" is checked.
     * 
     * @param p registration parameters
     * @return false when dialog is canceled or true when it is not
     */
	public static boolean showRegularizationDialog(Param p) 
	{
		// Feature extraction parameters
		GenericDialog gd = new GenericDialog("Shrinkage regularization");
		
		// If the registration model is SIMILARITY, then display isostropy weight = 1.0
		if(Param.registrationModelIndex == Register_Virtual_Stack_MT.SIMILARITY)
			tweakIso = 1.0;
		
		gd.addNumericField( "shear :", tweakShear, 2 );
		final TextField shearTextField = (TextField) gd.getNumericFields().lastElement();
		
		gd.addNumericField( "scale :", tweakScale, 2);
		final TextField scaleTextField = (TextField) gd.getNumericFields().lastElement();
		
		gd.addNumericField( "isotropy :", tweakIso, 2 );		
		final TextField isotropyTextField = (TextField) gd.getNumericFields().lastElement();
		
		// If the registration model is SIMILARITY, then disable the isotropy text field
		if( Param.registrationModelIndex == Register_Virtual_Stack_MT.SIMILARITY )
			isotropyTextField.setEnabled(false);
		else if (Param.registrationModelIndex == Register_Virtual_Stack_MT.ELASTIC)
		{
			shearTextField.setEnabled(false);
			scaleTextField.setEnabled(false);
			isotropyTextField.setEnabled(false);			
			
		}
		else
			isotropyTextField.setEnabled(true);						
		
		gd.addMessage( "Values between 0 and 1 are expected" );
		gd.addMessage( "(the closest to 1, the closest to rigid)" );

		gd.addCheckbox("Display_relaxation_graph", displayRelaxGraph);

		gd.showDialog();

		// Exit when canceled
		if (gd.wasCanceled()) 
			return false;
		
		tweakShear = (double) gd.getNextNumber();
		tweakScale = (double) gd.getNextNumber();		
		tweakIso = (double) gd.getNextNumber();
		displayRelaxGraph = gd.getNextBoolean();


		return true;
	} // end method showRegularizationDialog
	

	//-----------------------------------------------------------------------------------------
	/**
	 * Correct transforms from global scaling or rotation
	 * 
	 * @param transform array of transforms
	 */
	public static void postProcessTransforms(CoordinateTransform[] transform) 
	{
		double avgAngle = 0;
		double avgScale = 0;
		
		// Calculate average scaling and angle.
		for(int i = 0; i < transform.length; i++)
		{
			final CoordinateTransform t = transform[i];
			
			if( t instanceof AffineModel2D || t instanceof SimilarityModel2D )
			{
				final AffineTransform a = (t instanceof AffineModel2D) ? ((AffineModel2D)t).createAffine() : ((SimilarityModel2D)t).createAffine();
				
				// Move to the center of the image
				//a.translate(centerX[i], centerY[i]);
								
				// We assume that sheary=0
				// scalex=sqrt(A(1,1)*A(1,1)+A(2,1)*A(2,1));
				final double a11 = a.getScaleX();
				final double a21 = a.getShearY();
				final double scaleX = Math.sqrt( a11 * a11 + a21 * a21 );
				// rotang=atan2(A(2,1)/scalex,A(1,1)/scalex);
				final double rotang = Math.atan2( a21/scaleX, a11/scaleX);

				// R=[[cos(-rotang) -sin(-rotang)];[sin(-rotang) cos(-rotang)]];
				
				// rotate back shearx and scaley
				//v=R*[A(1,2) A(2,2)]';
				final double a12 = a.getShearX();
				final double a22 = a.getScaleY();
				final double scaleY = Math.sin(-rotang) * a12 + Math.cos(-rotang) * a22;
				
				avgScale += (scaleX + scaleY) / 2.0;
				avgAngle += rotang;							
			}					
		}
		
		avgScale /= transform.length;
		avgAngle /= transform.length;
		
		//IJ.log("average scaling = " + avgScale + " average rotation = " + avgAngle);
		
		AffineTransform correctionMatrix = new AffineTransform( Math.cos(-avgAngle) / avgScale,
																Math.sin(-avgAngle) / avgScale,
															   -Math.sin(-avgAngle) / avgScale,
															    Math.cos(-avgAngle) / avgScale,
															    0,0							    );
		
		// Correct from average scaling and rotation
		for(int i = 0; i < transform.length; i++)
		{
			if( transform[i] instanceof AffineModel2D || transform[i] instanceof SimilarityModel2D )
			{
				final AffineTransform a = (transform[i] instanceof AffineModel2D) ? 
										((AffineModel2D)transform[i]).createAffine() : 
										((SimilarityModel2D)transform[i]).createAffine();
				
				final AffineTransform b = new AffineTransform(a);
				b.concatenate(correctionMatrix);
						
				
			    // Move back the center
				//b.translate(-centerX[i], -centerY[i]);

				if(transform[i] instanceof AffineModel2D)
					((AffineModel2D)transform[i]).set( b );
				else
					((SimilarityModel2D)transform[i]).set( (float) b.getScaleX(), (float) b.getShearY(), (float) b.getTranslateX(), (float) b.getTranslateY() );
				
			}
		}
		
	} // end method postProcessTransform
	


		  
	public static void flush(ImagePlus imp) 
	{
		if (null == imp) return;
		imp.flush();
		if (null != imp.getProcessor() && null != imp.getProcessor().getPixels()) 
		{
			imp.getProcessor().setPixels(null);
		}
	}
	
}// end Register_Virtual_Stack_MT class
