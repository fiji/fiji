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
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.FileSaver;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.Graphics2D;
import java.awt.Color;
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

import mpicbg.trakem2.transform.CoordinateTransform;
import mpicbg.trakem2.transform.TransformMesh;
import mpicbg.trakem2.transform.CoordinateTransformList;
import mpicbg.trakem2.transform.TransformMeshMapping;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;

import bunwarpj.bUnwarpJTransformation;
import bunwarpj.trakem2.transform.CubicBSplineTransform;

/** 
 * Requires: a directory with images, all of the same dimensions
 * Performs: registration of one image to the next, by 6 different registration models:
 * 				- Translation (no deformation)
 * 				- Rigid (transation + rotation)
 * 				- Similarity (translation + rotation + isotropic scaling)
 * 				- Affine (free affine transformation)
 * 				- Elsatic (consistent elastic deformations by B-splines)
 * 				- Moving least squares (maximal warping)
 * Outputs: the list of new images, one for slice, into a target directory as .tif files.
 */
public class Register_Virtual_Stack_MT implements PlugIn {

	// Registration types
	static public final int TRANSLATION = 0;
	static public final int RIGID = 1;
	static public final int SIMILARITY = 2;
	static public final int AFFINE = 3;
	static public final int ELASTIC = 4;
	static public final int MOVING_LEAST_SQUARES = 5;

	static final public String[] registrationModelStrings =
			       {"Translation           -- no deformation",
	  	            "Rigid                 -- translate + rotate",
			        "Similarity            -- translate + rotate + isotropic scale",
			        "Affine                -- free affine transform",
			        "Elastic               -- bUnwarpJ splines",
			        "Moving least squares  -- maximal warping"};

	final static public String[] featuresModelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };


	/**
	 * Plug-in run method
	 */
	public void run(String arg) {

		GenericDialog gd = new GenericDialog("Options");

		gd.addChoice("Feature extraction model: ", featuresModelStrings, featuresModelStrings[1]);
		gd.addChoice("Registration model: ", registrationModelStrings, registrationModelStrings[1]);
		gd.addCheckbox("Advanced setup", false);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		final int featuresModelIndex = gd.getNextChoiceIndex();
		final int registrationModelIndex = gd.getNextChoiceIndex();
		final boolean advanced = gd.getNextBoolean();

		// Choose source image folder
		DirectoryChooser dc = new DirectoryChooser("Source images");
		String source_dir = dc.getDirectory();
		if (null == source_dir) return;
		source_dir = source_dir.replace('\\', '/');
		if (!source_dir.endsWith("/")) source_dir += "/";

		// Choose target folder to save images into
		dc = new DirectoryChooser("Target folder");
		String target_dir = dc.getDirectory();
		if (null == target_dir) return;
		target_dir = target_dir.replace('\\', '/');
		if (!target_dir.endsWith("/")) target_dir += "/";

		exec(source_dir, target_dir, featuresModelIndex, registrationModelIndex, advanced);
	}

	/** 
	 * Execution method. Execute registration after setting parameters. 
	 * 
	 * @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 * @param target_dir Directory to store registered slices into.
	 * @param registration_type 0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE, 4=ELASTIC, 5=MOVING_LEAST_SQUARES
	 * @param advanced Triggers showing parameters setup dialogs.
	 */
	static public void exec(final String source_dir, final String target_dir, final int featuresModelIndex, final int registrationModelIndex, final boolean advanced) {
		Param p = new Param();
		p.featuresModelIndex = featuresModelIndex;
		p.registrationModelIndex = registrationModelIndex;
		if (advanced) p.showDialog();
		exec(source_dir, target_dir, p);
	}

	/**
	 * Execution method. Execute registration when all parameters are set.
	 * 
	 * @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 * @param target_dir Directory to store registered slices into.
	 * @param p Registration parameters
	 */
	static public void exec(final String source_dir, final String target_dir, final Param p) {
		// get file listing
		final String exts = ".tif.jpg.png.gif.tiff.jpeg.bmp.pgm";
		final String[] names = new File(source_dir).list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				int idot = name.lastIndexOf('.');
				if (-1 == idot) return false;
				return exts.contains(name.substring(idot).toLowerCase());
			}
		});
		Arrays.sort(names);

		exec(source_dir, names, target_dir, p);
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
		 * Closest/next closest neighbour distance ratio
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
		public int featuresModelIndex = 1;

		/**
		 * Implemented transformation models for choice
	 	*  0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE, 4=ELASTIC, 5=MOVING_LEAST_SQUARES
		 */
		public int registrationModelIndex = 1;
                
		/** bUnwarpJ parameters for consistent elastic registration */
        public bunwarpj.Param elastic_param = new bunwarpj.Param();

        /**
         * Shows parameter dialog when "advanced options" is checked
         * @return false when dialog is canceled or 
         */
		public boolean showDialog() {
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

			if (gd.wasCanceled()) return false;

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
                        
			if (registrationModelIndex == 4)
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
	 * @param target_dir Directory to store registered slices into.
	 * @param p registration parameters
	 */
	static public void exec(final String source_dir, final String[] sorted_file_names, final String target_dir, final Param p) {
		if (source_dir.equals(target_dir)) {
			IJ.log("Source and target directories MUST be different\n or images would get overwritten.\nDid NOT register stack slices.");
			return;
		}
		if (p.registrationModelIndex < TRANSLATION || p.registrationModelIndex > MOVING_LEAST_SQUARES) {
			IJ.log("Don't know how to process registration type " + p.registrationModelIndex);
			return;
		}

		
		// Select registration model
		mpicbg.models.CoordinateTransform t;
		switch (p.registrationModelIndex) {
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
			ImagePlus imp2 = IJ.openImage(source_dir + sorted_file_names[0]);
			
			// Masks
			ImageProcessor imp1mask = null;
			ImageProcessor imp2mask = null;
			
			
			final Rectangle commonBounds = new Rectangle(0, 0, imp2.getWidth(), imp2.getHeight());
			final List<Rectangle> bounds = new ArrayList<Rectangle>();
			bounds.add(new Rectangle(0, 0, imp2.getWidth(), imp2.getHeight()));
			
			// Save the first image, untouched:
			exe.submit(saveImage(imp2, makeTargetPath(target_dir, sorted_file_names[0])));

			// For each image, extract features, register with previous one and save the transformed image.
			for (int i=1; i<sorted_file_names.length; i++) 
			{
				IJ.showStatus("Registering slice " + (i+1) + "/" + sorted_file_names.length);
				imp1 = imp2;
				imp1mask = imp2mask;
				imp2 = IJ.openImage(source_dir + sorted_file_names[i]);
				Future<ArrayList<Feature>> fu1 = exe.submit(extractFeatures(p, imp1.getProcessor()));
				Future<ArrayList<Feature>> fu2 = exe.submit(extractFeatures(p, imp2.getProcessor()));
				ArrayList<Feature> fs1 = fu1.get();
				ArrayList<Feature> fs2 = fu2.get();

				/*
				IJ.log("features for imp1: " + fs1.size());
				IJ.log("features for imp2: " + fs2.size());
				*/
				
				final List< PointMatch > candidates = new ArrayList< PointMatch >();
				FeatureTransform.matchFeatures( fs1, fs2, candidates, p.rod );

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
						IJ.log("ERROR: unknown featuresModelIndex = " + p.featuresModelIndex);
						return;
				}
				
				boolean modelfound = false;
				try {
					modelfound = featuresModel.filterRansac(
							candidates,
							inliers,
							1000,
							p.maxEpsilon,
							p.minInlierRatio );
				} catch ( NotEnoughDataPointsException e ) {
					IJ.log("No features model found for file " + i + ": " + sorted_file_names[i]);
					return;
				}
				
				/*
				IJ.log("inliers = " +inliers.size() + " candidates = " + candidates.size());
				IJ.log("imp1: " + imp1);
				IJ.log("imp2: " + imp2);
				 */
				// Generate registered image, put it into imp2 and save it
				switch (p.registrationModelIndex) {
					case 0:
					case 1:
					case 2:
					case 3:
						((Model<?>)t).fit(inliers);
						break;
					case 4:
						// set inliers as a PointRoi, and set masks
						//call_bUnwarpJ(...);
						//imp1.show();
						//imp2.show();
						final List< Point > sourcePoints = new ArrayList<Point>();
						final List< Point > targetPoints = new ArrayList<Point>();
						PointMatch.sourcePoints( inliers, sourcePoints );
						PointMatch.targetPoints( inliers, targetPoints );
						
						imp1.setRoi( Util.pointsToPointRoi(sourcePoints) );
						imp2.setRoi( Util.pointsToPointRoi(targetPoints) );
						
						/*
						imp1.show();
						ImagePlus aux = new ImagePlus("source", imp2.getProcessor().duplicate());
						aux.setRoi( Util.pointsToPointRoi(targetPoints) );
						aux.show();
						if(imp1mask != null) new ImagePlus("mask", imp1mask).show();
						*/
						
						// Perform registration
						bUnwarpJTransformation warp 
						  = (bunwarpj.bUnwarpJTransformation) Class.forName("bUnwarpJ_").getDeclaredMethod("computeTransformationBatch", 
								  ImagePlus.class, ImagePlus.class, ImageProcessor.class, ImageProcessor.class, 
								  bunwarpj.Param.class).invoke(null, new Object[]{imp2, imp1, imp2mask, imp1mask, p.elastic_param});
						
						// take the mask from the results
						final ImagePlus output_ip = warp.getDirectResults();
						imp2mask = output_ip.getStack().getProcessor(3);
						//output_ip.show();
						
						
						// Store result in a Cubic B-Spline transform
						t = new CubicBSplineTransform(warp.getIntervals(), warp.getDirectDeformationCoefficientsX(), warp.getDirectDeformationCoefficientsY(),
                        		imp2.getWidth(), imp2.getHeight());
						break;
					case 5:
						((MovingLeastSquaresTransform)t).setModel(AffineModel2D.class);
						((MovingLeastSquaresTransform)t).setAlpha(1); // smoothness
						((MovingLeastSquaresTransform)t).setMatches(inliers);
						break;
				}

				TransformMesh mesh = new TransformMesh(t, 32, imp2.getWidth(), imp2.getHeight());
				TransformMeshMapping mapping = new TransformMeshMapping(mesh);
				
				// Create interpolated mask
				imp2mask = new ByteProcessor(imp2.getWidth(), imp2.getHeight());
				imp2mask.setValue(255);
				imp2mask.fill();
				imp2mask = mapping.createMappedImageInterpolated(imp2mask);
				
				// Create interpolated image with black background
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
				
				exe.submit(saveImage(imp2, makeTargetPath(target_dir, sorted_file_names[i])));

				IJ.showProgress(i / (float)sorted_file_names.length);
			}
			
			for ( int i = 0; i < bounds.size(); ++i )
			{
				final Rectangle b = bounds.get(i);
				b.x = -commonBounds.x + b.x;
				b.y = -commonBounds.y + b.y;
			}

			// Reopen all target images and repaint them on an enlarged canvas
			final ArrayList<Future<String>> jobs = new ArrayList<Future<String>>();
			for (int i=0; i<sorted_file_names.length; i++) 
			{
				final Rectangle b = bounds.get(i);
				jobs.add(exe.submit(resizeAndSaveImage(makeTargetPath(target_dir, sorted_file_names[i]), b.x, b.y, commonBounds.width, commonBounds.height)));
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

			// Show
			new ImagePlus("Registered " + new File(source_dir).getName(), stack).show();

			IJ.showStatus("Done!");

		} catch (Exception e) {
			IJ.log("ERROR: " + e);
			e.printStackTrace();
		} finally {
			IJ.showProgress(1);
			exe.shutdownNow();
		}
	}

	/** Returns the file name of the saved image, or null if there was an error. */
	static private Callable<String> resizeAndSaveImage(final String path, final int x, final int y, final int width, final int height) {
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
					if (imp.getType() == ImagePlus.COLOR_RGB) {
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
	}

	static private String makeTargetPath(final String dir, final String name) {
		String filepath = dir + name;
		if (! name.toLowerCase().matches("^.*ti[f]{1,2}$")) filepath += ".tif";
		return filepath;
	}

	static private Callable<ImagePlus> openImage(final String path) {
		return new Callable<ImagePlus>() {
			public ImagePlus call() {
				return IJ.openImage(path);
			}
		};
	}

	static private Callable<ArrayList<Feature>> extractFeatures(final Param p, final ImageProcessor ip) {
		return new Callable<ArrayList<Feature>>() {
			public ArrayList<Feature> call() {
				final ArrayList<Feature> fs = new ArrayList<Feature>();
				new SIFT( new FloatArray2DSIFT( p.sift ) ).extractFeatures(ip, fs);
				return fs;
			}
		};
	}

	static private Callable<Boolean> saveImage(final ImagePlus imp, final String path) {
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
}
