package register_virtual_stack;

/** 
 * Albert Cardona, Ignacio Arganda-Carreras and Stephan Saalfeld 2009. 
 * This work released under the terms of the General Public License in its latest edition. 
 * */

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.VirtualStack;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.io.FileSaver;
import ij.io.OpenDialog;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.Rectangle;
import java.io.File;
import java.io.FilenameFilter;

import mpicbg.ij.FeatureTransform;
import mpicbg.ij.SIFT;
import mpicbg.ij.util.Util;
import mpicbg.imagefeatures.*;
import mpicbg.models.AffineModel2D;
import mpicbg.models.Model;
import mpicbg.models.MovingLeastSquaresTransform;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.RigidModel2D;

import mpicbg.trakem2.transform.TransformMesh;
import mpicbg.trakem2.transform.TransformMeshMapping;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

import javax.swing.JFileChooser;

import bunwarpj.Transformation;
import bunwarpj.bUnwarpJ_;
import bunwarpj.trakem2.transform.CubicBSplineTransform;

/** 
 * Requires: a directory with images, all of the same dimensions
 * Performs: registration of one image to the next, by 6 different registration models:
 * 				- Translation (no deformation)
 * 				- Rigid (translation + rotation)
 * 				- Similarity (translation + rotation + isotropic scaling)
 * 				- Affine (free affine transformation)
 * 				- Elastic (consistent elastic deformations by B-splines)
 * 				- Moving least squares (maximal warping)
 * Outputs: the list of new images, one for slice, into a target directory as .tif files.
 */
public class Register_Virtual_Stack_MT implements PlugIn 
{

	// Registration types
	static public final int TRANSLATION 			= 0;
	static public final int RIGID 					= 1;
	static public final int SIMILARITY 				= 2;
	static public final int AFFINE 					= 3;
	static public final int ELASTIC 				= 4;
	static public final int MOVING_LEAST_SQUARES 	= 5;
	
	public static int featuresModelIndex = 1;
	public static int registrationModelIndex = 1;
	public static String currentDirectory = (OpenDialog.getLastDirectory() == null) ? 
					 OpenDialog.getDefaultDirectory() : OpenDialog.getLastDirectory();
	public static boolean advanced = false;
					 

	static final public String[] registrationModelStrings =
			       {"Translation          -- no deformation                      ",
	  	            "Rigid                -- translate + rotate                  ",
			        "Similarity           -- translate + rotate + isotropic scale",
			        "Affine               -- free affine transform               ",
			        "Elastic              -- bUnwarpJ splines                    ",
			        "Moving least squares -- maximal warping                     "};

	final static public String[] featuresModelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };


	/**
	 * Plug-in run method
	 */
	public void run(String arg) 
	{
		GenericDialog gd = new GenericDialog("Register Virtual Stack");

		gd.addChoice("Feature extraction model: ", featuresModelStrings, featuresModelStrings[featuresModelIndex]);
		gd.addChoice("Registration model: ", registrationModelStrings, registrationModelStrings[registrationModelIndex]);
		gd.addCheckbox("Advanced setup", advanced);	
		
		gd.showDialog();
		
		// Exit when canceled
		if (gd.wasCanceled()) 
			return;
				
		featuresModelIndex = gd.getNextChoiceIndex();
		registrationModelIndex = gd.getNextChoiceIndex();
		advanced = gd.getNextBoolean();

		// Choose source image folder
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File(currentDirectory));
	    chooser.setDialogTitle("Choose directory with Source images");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    chooser.setAcceptAllFileFilterUsed(false);
	    if (chooser.showOpenDialog(gd) != JFileChooser.APPROVE_OPTION)
	    	return;
	     		
		String source_dir = chooser.getSelectedFile().toString();
		if (null == source_dir) 
			return;
		source_dir = source_dir.replace('\\', '/');
		if (!source_dir.endsWith("/")) source_dir += "/";

		// Choose target folder to save images into
		chooser.setDialogTitle("Choose Output folder");
		if (chooser.showOpenDialog(gd) != JFileChooser.APPROVE_OPTION)
	    	return;
		
		String target_dir = chooser.getSelectedFile().toString();
		if (null == target_dir) 
			return;
		target_dir = target_dir.replace('\\', '/');
		if (!target_dir.endsWith("/")) target_dir += "/";
		
		// Choose reference image
		chooser.setDialogTitle("Choose reference image");
		chooser.setCurrentDirectory(new java.io.File(source_dir));
	    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    chooser.setAcceptAllFileFilterUsed(true);
	    if (chooser.showOpenDialog(gd) != JFileChooser.APPROVE_OPTION)
	    	return;
	    String referenceName = chooser.getSelectedFile().getName();

		// Execute registration
		exec(source_dir, target_dir, referenceName, featuresModelIndex, registrationModelIndex, advanced);
	}

	/** 
	 * Execution method. Execute registration after setting parameters. 
	 * 
	 * @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 * @param target_dir Directory to store registered slices into.
	 * @param refrenceName File name of the reference image
	 * @param featuresModelIndex Index of the features extraction model (0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE)
	 * @param registrationModelIndex Index of the registration model (0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE, 4=ELASTIC, 5=MOVING_LEAST_SQUARES)
	 * @param advanced Triggers showing parameters setup dialogs.
	 */
	static public void exec(
			final String source_dir, 
			final String target_dir,
			final String referenceName,
			final int featuresModelIndex, 
			final int registrationModelIndex, 
			final boolean advanced) 
	{
		Param p = new Param();
		p.featuresModelIndex = featuresModelIndex;
		p.registrationModelIndex = registrationModelIndex;
		// Show parameter dialogs when advanced option is checked
		if (advanced && !p.showDialog())
			return;
		exec(source_dir, target_dir, referenceName, p);
	}

	/**
	 * Execution method. Execute registration when all parameters are set.
	 * 
	 * @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 * @param target_dir Directory to store registered slices into.
	 * @param referenceName File name of the reference image
	 * @param p Registration parameters
	 */
	static public void exec(final String source_dir, final String target_dir, final String referenceName, final Param p) 
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
		
		IJ.log("Reference index = " + referenceIndex);

		// Execute registration with sorted source file names and reference image index
		exec(source_dir, names, referenceIndex, target_dir, p);
	}

	//-----------------------------------------------------------------------------------------
	/**
	 * Registration parameters class 
	 *
	 */
	static public class Param
	{	
		final public FloatArray2DSIFT.Param sift = new FloatArray2DSIFT.Param();
		
		/**
		 * Closest/next closest neighbor distance ratio
		 */
		public float rod = 0.92f;
		
		/**
		 * Maximal allowed alignment error in px
		 */
		public float maxEpsilon = 25.0f;
		
		/**
		 * Inlier/candidates ratio
		 */
		public float minInlierRatio = 0.05f;
		
		/**
		 * Implemented transformation models for choice
	 	 *  0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE
		 */
		public int featuresModelIndex = Register_Virtual_Stack_MT.RIGID;

		/**
		 * Implemented transformation models for choice
	 	*  0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE, 4=ELASTIC, 5=MOVING_LEAST_SQUARES
		 */
		public int registrationModelIndex = Register_Virtual_Stack_MT.RIGID;
                
		/** bUnwarpJ parameters for consistent elastic registration */
        public bunwarpj.Param elastic_param = new bunwarpj.Param();

        /**
         * Shows parameter dialog when "advanced options" is checked
         * @return false when dialog is canceled or 
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
			gd.addNumericField( "feature_descriptor_size :", sift.fdSize, 0 );
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
	 * Execution method. Execute registration when all parameters are set.
	 * 
	 * @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 * @param sorted_file_names Array of sorted source file names.
	 * @param referenceIndex index of the reference image in the array of sorted source images
	 * @param target_dir Directory to store registered slices into.
	 * @param p registration parameters
	 */
	static public void exec(
			final String source_dir, 
			final String[] sorted_file_names,
			final int referenceIndex,
			final String target_dir, 
			final Param p) 
	{
		// Check if source and output directories are different
		if (source_dir.equals(target_dir)) 
		{
			IJ.error("Source and target directories MUST be different\n or images would get overwritten.\nDid NOT register stack slices.");
			return;
		}
		// Check if the registration model is known
		if (p.registrationModelIndex < TRANSLATION || p.registrationModelIndex > MOVING_LEAST_SQUARES) 
		{
			IJ.error("Don't know how to process registration type " + p.registrationModelIndex);
			return;
		}
		
		// Select coordinate transform based on the registration model
		mpicbg.models.CoordinateTransform t;
		switch (p.registrationModelIndex) 
		{
			case 0: t = new TranslationModel2D(); break;
			case 1: t = new RigidModel2D(); break;
			case 2: t = new SimilarityModel2D(); break;
			case 3: t = new AffineModel2D(); break;
			case 4: t = new CubicBSplineTransform(); break;
			case 5: t = new MovingLeastSquaresTransform(); break;
			default:
				IJ.log("ERROR: unknown registrationModelIndex = " + p.registrationModelIndex);
				return;
		}

		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {			

			ImagePlus imp1 = null;
			ImagePlus imp2 = IJ.openImage(source_dir + sorted_file_names[referenceIndex]);
			
			// Masks
			ImagePlus imp1mask = new ImagePlus();
			ImagePlus imp2mask = new ImagePlus();
			
			// Common bounds to create common frame for all images
			final Rectangle commonBounds = new Rectangle(0, 0, imp2.getWidth(), imp2.getHeight());
			// List of bounds in the forward registration
			final List<Rectangle> boundsFor = new ArrayList<Rectangle>();
			boundsFor.add(new Rectangle(0, 0, imp2.getWidth(), imp2.getHeight()));
			
			// Save the first image, untouched:
			exe.submit(saveImage(imp2, makeTargetPath(target_dir, sorted_file_names[referenceIndex])));

			// For each image, extract features, register with previous one and save the transformed image.
			
			// Forward registration (from reference image to the end of the sequence)			
			for (int i=referenceIndex+1; i<sorted_file_names.length; i++) 
			{
				// Shift images
				imp1 = imp2;
				imp1mask = imp2mask;
				// Create empty mask for second image
				imp2mask = new ImagePlus();
				imp2 = IJ.openImage(source_dir + sorted_file_names[i]);
				// Register
				if(!register( imp1, imp2, imp1mask, imp2mask, i, sorted_file_names,
						  source_dir, target_dir, exe, p, t, commonBounds, boundsFor, referenceIndex))
					return;
			}
			
			// Backward registration (from reference image to the beginning of the sequence)
			imp2 = IJ.openImage(source_dir + sorted_file_names[referenceIndex]);
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
				// Register
				if(!register( imp1, imp2, imp1mask, imp2mask, i, sorted_file_names,
						  source_dir, target_dir, exe, p, t, commonBounds, boundsBack, referenceIndex))
					return;
			}
			
			// Adjust Forward bounds
			for ( int i = 0; i < boundsFor.size(); ++i )
			{
				final Rectangle b = boundsFor.get(i);
				b.x = -commonBounds.x + b.x;
				b.y = -commonBounds.y + b.y;
			}
			
			// Adjust Backward bounds
			for ( int i = 0; i < boundsBack.size(); ++i )
			{
				final Rectangle b = boundsBack.get(i);
				b.x = -commonBounds.x + b.x;
				b.y = -commonBounds.y + b.y;
			}

			// Reopen all target images and repaint them on an enlarged canvas
			final Future[] jobs = new Future[sorted_file_names.length];
			for (int j = 0, i=referenceIndex; i<sorted_file_names.length; i++, j++) 
			{
				final Rectangle b = boundsFor.get(j);
				jobs[i] = exe.submit(resizeAndSaveImage(makeTargetPath(target_dir, sorted_file_names[i]), b.x, b.y, commonBounds.width, commonBounds.height));
			}
			
			for (int j=1, i=referenceIndex-1; i>=0; i--, j++) 
			{
				final Rectangle b = boundsBack.get(j);
				jobs[i] = exe.submit(resizeAndSaveImage(makeTargetPath(target_dir, sorted_file_names[i]), b.x, b.y, commonBounds.width, commonBounds.height));
			}
			

			// Join all and create VirtualStack
			final VirtualStack stack = new VirtualStack(commonBounds.width, commonBounds.height, null, target_dir);
			for (final Future<String> job : jobs) {
				String filename = job.get();
				if (null == filename) {
					IJ.log("Image failed: " + filename);
					return;
				}
				stack.addSlice(filename);
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
					final ImagePlus imp = IJ.openImage(path);
					if (null == imp) {
						IJ.log("Could not open target image at " + path);
						return null;
					}
					final ImageProcessor ip = imp.getProcessor().createProcessor(width, height);
					// Color images are white by default: fill with black
					if (imp.getType() == ImagePlus.COLOR_RGB) 
					{
						ip.setRoi(0, 0, width, height);
						ip.setValue(0);
						ip.fill();
					}
					ip.insert(imp.getProcessor(), x, y);
					imp.flush();
					final ImagePlus big = new ImagePlus(imp.getTitle(), ip);
					big.setCalibration(imp.getCalibration());
					if (! new FileSaver(big).saveAsTiff(path)) {
						return null;
					}
					return new File(path).getName();
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		};
	} // end resizeAndSaveImage method

	/**
	 * Make target (output) path
	 * 
	 * @param dir output directory
	 * @param name output name
	 * @return complete path for the target image
	 */
	static private String makeTargetPath(final String dir, final String name) 
	{
		String filepath = dir + name;
		if (! name.toLowerCase().matches("^.*ti[f]{1,2}$")) 
			filepath += ".tif";
		return filepath;
	}


	/**
	 * Generate object to concurrently extract features
	 * 
	 * @param p feature extraction parameters
	 * @param ip input image
	 * @return callable object to execute feature extraction
	 */
	static private Callable<ArrayList<Feature>> extractFeatures(final Param p, final ImageProcessor ip) {
		return new Callable<ArrayList<Feature>>() {
			public ArrayList<Feature> call() {
				final ArrayList<Feature> fs = new ArrayList<Feature>();
				new SIFT( new FloatArray2DSIFT( p.sift ) ).extractFeatures(ip, fs);
				return fs;
			}
		};
	}

	/**
	 * Generate object to concurrently save an image
	 * 
	 * @param imp image to save
	 * @param path output path
	 * @return callable object to execute the saving
	 */
	static private Callable<Boolean> saveImage(final ImagePlus imp, final String path) 
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
	
	/**
	 * Register two images with corresponding masks and 
	 * following the features and registration models.
	 * 
	 * @param imp1 target image
	 * @param imp2 source image
	 * @param imp1mask target mask
	 * @param imp2mask source mask
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
	 * @throws Exception
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
			mpicbg.models.CoordinateTransform t,
			Rectangle commonBounds,
			List<Rectangle> bounds,
			final int referenceIndex) throws Exception
	{
		// Extract SIFT features
		IJ.showStatus("Registering slice " + (i+1) + "/" + sorted_file_names.length);		
		Future<ArrayList<Feature>> fu1 = exe.submit(extractFeatures(p, imp1.getProcessor()));
		Future<ArrayList<Feature>> fu2 = exe.submit(extractFeatures(p, imp2.getProcessor()));
		ArrayList<Feature> fs1 = fu1.get();
		ArrayList<Feature> fs2 = fu2.get();
		
		final List< PointMatch > candidates = new ArrayList< PointMatch >();
		FeatureTransform.matchFeatures( fs2, fs1, candidates, p.rod );

		final List< PointMatch > inliers = new ArrayList< PointMatch >();

		// Select features model
		Model< ? > featuresModel;
		switch ( p.featuresModelIndex )
		{
			case 0:
				featuresModel = new TranslationModel2D();
				break;
			case 1:
				featuresModel = new RigidModel2D();
				break;
			case 2:
				featuresModel = new SimilarityModel2D();
				break;
			case 3:
				featuresModel = new AffineModel2D();
				break;
			default:
				IJ.error("ERROR: unknown featuresModelIndex = " + p.featuresModelIndex);
				return false;
		}
			
		// Filter candidates into inliers
		try {
			featuresModel.filterRansac(
					candidates,
					inliers,
					1000,
					p.maxEpsilon,
					p.minInlierRatio );
		} 
		catch ( NotEnoughDataPointsException e ) 
		{
			IJ.error("No features model found for file " + i + ": " + sorted_file_names[i]);
			return false;
		}
	
		// Generate registered image, put it into imp2 and save it
		switch (p.registrationModelIndex) 
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
				PointMatch.sourcePoints( inliers, sourcePoints );
				PointMatch.targetPoints( inliers, targetPoints );
				
				imp2.setRoi( Util.pointsToPointRoi(sourcePoints) );
				imp1.setRoi( Util.pointsToPointRoi(targetPoints) );
				
				
				//imp1.show();
				//ImagePlus aux = new ImagePlus("source", imp2.getProcessor().duplicate());
				//aux.setRoi( Util.pointsToPointRoi(targetPoints) );
				//aux.show();
				//if(imp1mask != null) new ImagePlus("mask", imp1mask).show();
				
				
				// Perform registration
				ImageProcessor mask1 = imp1mask.getProcessor() == null ? null : imp1mask.getProcessor();
				ImageProcessor mask2 = imp2mask.getProcessor() == null ? null : imp2mask.getProcessor();
				
				Transformation warp = bUnwarpJ_.computeTransformationBatch(imp2, imp1, mask2, mask1, p.elastic_param);
				
				// take the mask from the results
				//final ImagePlus output_ip = warp.getDirectResults();
				//imp2mask.setProcessor(imp2mask.getTitle(), output_ip.getStack().getProcessor(3));
				//output_ip.show();
				
				
				// Store result in a Cubic B-Spline transform
				t = new CubicBSplineTransform(warp.getIntervals(), warp.getDirectDeformationCoefficientsX(), warp.getDirectDeformationCoefficientsY(),
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
		if(p.registrationModelIndex == Register_Virtual_Stack_MT.ELASTIC)
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
		
		// Update common bounds
		if(currentBounds.x < commonBounds.x)
			commonBounds.x = currentBounds.x;
		if(currentBounds.y < commonBounds.y)
			commonBounds.y = currentBounds.y;
		if(currentBounds.x + currentBounds.width > commonBounds.x + commonBounds.width)
			commonBounds.width = currentBounds.x + currentBounds.width - commonBounds.x;
		if(currentBounds.y + currentBounds.height > commonBounds.y + commonBounds.height)
			commonBounds.height = currentBounds.y + currentBounds.height - commonBounds.y;
		
		// Save target image
		exe.submit(saveImage(imp2, makeTargetPath(target_dir, sorted_file_names[i])));		
		return true;
	} //end method register
	
}// end Register_Virtual_Stack_MT class