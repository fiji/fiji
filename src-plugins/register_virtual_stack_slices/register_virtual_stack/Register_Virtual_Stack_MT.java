package register_virtual_stack;

/** Albert Cardona 2008. This work released under the terms of the General Public License in its latest edition. */

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.VirtualStack;
import ij.ImagePlus;
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
import mpicbg.imagefeatures.*;
import mpicbg.models.AffineModel2D;
import mpicbg.models.Model;
import mpicbg.models.MovingLeastSquaresTransform;
import mpicbg.models.NotEnoughDataPointsException;
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

/** Requires: a directory with images, all of the same dimensions
 *  Performs: registration of one image to the next, by phase- and cross-correlation or by SIFT
 *  Outputs: the list of new images, one for slice, into a target directory as .tif files.
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

	/** @param source_dir Directory to read all images from, where each image is a slice in a sequence. Their names must be bit-sortable, i.e. if numbered, they must be padded with zeros.
	 *  @param target_dir Directory to store registered slices into.
	 *  @param registration_type 0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE, 4=ELASTIC, 5=MOVING_LEAST_SQUARES
	 *  @param advanced Triggers showing parameters setup dialogs.
	 */
	static public void exec(final String source_dir, final String target_dir, final int featuresModelIndex, final int registrationModelIndex, final boolean advanced) {
		Param p = new Param();
		p.featuresModelIndex = featuresModelIndex;
		p.registrationModelIndex = registrationModelIndex;
		if (advanced) p.showDialog();
		exec(source_dir, target_dir, p);
	}

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
		 * Implemeted transformation models for choice
	 	*  0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE
		 */
		public int featuresModelIndex = 1;

		/**
		 * Implemeted transformation models for choice
	 	*  0=TRANSLATION, 1=RIGID, 2=SIMILARITY, 3=AFFINE, 4=ELASTIC, 5=MOVING_LEAST_SQUARES
		 */
		public int registrationModelIndex = 1;

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

			return true;
		}
	}

	static public void exec(final String source_dir, final String[] sorted_file_names, final String target_dir, final Param p) {
		if (source_dir.equals(target_dir)) {
			IJ.log("Source and target directories MUST be different\n or images would get overwritten.\nDid NOT register stack slices.");
			return;
		}
		if (p.registrationModelIndex < TRANSLATION || p.registrationModelIndex > MOVING_LEAST_SQUARES) {
			IJ.log("Don't know how to process registration type " + p.registrationModelIndex);
			return;
		}

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

		final mpicbg.models.CoordinateTransform t;
		switch (p.registrationModelIndex) {
			case 0: t = new TranslationModel2D(); break;
			case 1: t = new RigidModel2D(); break;
			case 2: t = new SimilarityModel2D(); break;
			case 3: t = new AffineModel2D(); break;
//			case 4: t = new CubicBSplineTransform(); break;
			case 5: t = new MovingLeastSquaresTransform(); break;
			default:
				IJ.log("ERROR: unknown registrationModelIndex = " + p.registrationModelIndex);
				return;
		}

		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {

			final Rectangle[] bounds = new Rectangle[sorted_file_names.length];
			final AffineTransform[] affines = new AffineTransform[sorted_file_names.length];

			ImagePlus imp1 = null;
			ImagePlus imp2 = IJ.openImage(source_dir + sorted_file_names[0]);
			bounds[0] = new Rectangle(0, 0, imp2.getWidth(), imp2.getHeight());
			affines[0] = new AffineTransform();

			// Save the first image, untouched:
			exe.submit(saveImage(imp2, makeTargetPath(target_dir, sorted_file_names[0])));

			// For each image, extract features, register with previous one and save the transformed image.
			for (int i=1; i<sorted_file_names.length; i++) {
				IJ.showStatus("Registering slice " + (i+1) + "/" + sorted_file_names.length);
				imp1 = imp2;
				imp2 = IJ.openImage(source_dir + sorted_file_names[i]);
				Future<ArrayList<Feature>> fu1 = exe.submit(extractFeatures(p, imp1.getProcessor()));
				Future<ArrayList<Feature>> fu2 = exe.submit(extractFeatures(p, imp2.getProcessor()));
				ArrayList<Feature> fs1 = fu1.get();
				ArrayList<Feature> fs2 = fu2.get();

				final List< PointMatch > candidates = new ArrayList< PointMatch >();
				FeatureTransform.matchFeatures( fs1, fs2, candidates, p.rod );

				final List< PointMatch > inliers = new ArrayList< PointMatch >();

				boolean modelFound;
				try {
					modelFound = featuresModel.filterRansac(
							candidates,
							inliers,
							1000,
							p.maxEpsilon,
							p.minInlierRatio );
				} catch ( NotEnoughDataPointsException e ) {
					IJ.log("No features model found for file " + i + ": " + sorted_file_names[i]);
					return;
				}

				// Generate registered image, put it into imp2 and save it
				switch (p.registrationModelIndex) {
					case 0:
					case 1:
					case 2:
					case 3:
						((Model<?>)t).fit(inliers);
						break;
//					case 4:
//						// set inliers as a PointRoi, and set masks
//						call_bUnwarpJ(...);
//						break;
					case 5:
						((MovingLeastSquaresTransform)t).setModel(AffineModel2D.class);
						((MovingLeastSquaresTransform)t).setAlpha(1); // smoothness
						((MovingLeastSquaresTransform)t).setMatches(inliers);
						break;
				}

				TransformMesh mesh = new TransformMesh(t, 32, imp2.getWidth(), imp2.getHeight());
				TransformMeshMapping mapping = new TransformMeshMapping(mesh);
				imp2.setProcessor(imp2.getTitle(), mapping.createMappedImageInterpolated(imp2.getProcessor()));

				// Accumulate bounding boxes, so in the end they can be reopened and re-saved with an enlarged canvas.
				bounds[i] = mesh.getBoundingBox();
				affines[i] = new AffineTransform();
				affines[i].translate(-bounds[i].x, -bounds[i].y);

				//IJ.log("bounds[" + i + "] = " + bounds[i]);

				exe.submit(saveImage(imp2, makeTargetPath(target_dir, sorted_file_names[i])));

				IJ.showProgress(i / (float)sorted_file_names.length);
			}

			// Determine maximum canvas, make affines global by concatenation
			final Rectangle box = (Rectangle) bounds[0].clone();
			final Rectangle one = (Rectangle) box.clone();

			for (int i=1; i<affines.length; i++) {
				affines[i].concatenate(affines[i-1]);
				box.add(affines[i].createTransformedShape(one).getBounds());
				// reset
				one.setRect(0, 0, bounds[i].width, bounds[i].height);
			}

			box.width = box.width - box.x;
			box.height = box.height - box.y;
			final AffineTransform trans = new AffineTransform();
			trans.translate(-box.x, -box.y);
			box.x = box.y = 0;

			// Reopen all target images and repaint them on an enlarged canvas
			final ArrayList<Future<String>> jobs = new ArrayList<Future<String>>();
			for (int i=0; i<sorted_file_names.length; i++) {
				Point2D.Double po = new Point2D.Double(0, 0);
				affines[i].concatenate(trans);
				affines[i].transform(po, po);
				IJ.log("po: " + po.x + "," + po.y + "\nxy: " + bounds[i].x + "," + bounds[i].y + "\n------");
				jobs.add(exe.submit(resizeAndSaveImage(makeTargetPath(target_dir, sorted_file_names[i]), (int)po.x, (int)po.y, box.width, box.height)));
			}

			// Join all and create VirtualStack
			final VirtualStack stack = new VirtualStack(box.width, box.height, null, target_dir);
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
