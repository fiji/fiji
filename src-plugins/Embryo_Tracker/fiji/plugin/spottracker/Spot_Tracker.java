package fiji.plugin.spottracker;

import fiji.plugin.spottracker.features.FeatureFacade;
import fiji.plugin.spottracker.gui.SpotTrackerFrame;
import fiji.plugin.spottracker.segmentation.SpotSegmenter;
import fiji.util.SplitString;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import mpicbg.imglib.algorithm.roi.MedianFilter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * <p>The Embryo_Tracker class runs on the currently active time lapse image (2D or 3D) of embryo development
 * and both identifies and tracks fluorescently stained nuclei over time.</p>
 * 
 * <p><b>Required input:</b> A 2D or 3D time-lapse image of embryo development.</p>
 * <p><b>Output:</b> Detailed information regarding embryo development. TODO add more</p>
 * 
 * <p>There are two landmark steps for the Embryo_Tracker class:</p>
 * 
 * <ol>
 * <li>Segmentation (nuclei/object detection).</li>
 * <li>Object tracking.</li>
 * </ol>
 * 
 *
 * @author Nicholas Perry, Jean-Yves Tinevez - Institut Pasteur - July-Aug-Sep 2010
 *
 * @param <T>
 */
public class Spot_Tracker implements PlugIn {
	
	
	/*
	 * INNER CLASSES
	 */
	
	private static final String DIAMETER_KEY = "diameter";
	private static final String MEDIAN_FILTER_KEY = "median";
	private static final String ALLOW_EDGE_KEY = "edge";
	private static final Map<String, String> optionStrings = new HashMap<String, String>();
	static {
		optionStrings.put(DIAMETER_KEY, 		""+Settings.DEFAULT.expectedDiameter);
		optionStrings.put(MEDIAN_FILTER_KEY, 	""+Settings.DEFAULT.useMedianFilter);
		optionStrings.put(ALLOW_EDGE_KEY, 		""+Settings.DEFAULT.allowEdgeMaxima);	
	}	
	
	/** Contain the segmentation result, un-filtered. See {@link #execSegmentation(ImagePlus, Settings)}*/
	private List<Collection<Spot>> spots;
	/** Contain the Spot retained for tracking, after thresholding by features. */
	private List<Collection<Spot>> selectedSpots;
	
	private ArrayList<Feature> thresholdFeatures = new ArrayList<Feature>();
	private ArrayList<Float> thresholdValues = new ArrayList<Float>();
	private ArrayList<Boolean> thresholdAbove = new ArrayList<Boolean>();
	/**
	 * Logger used to append log message to. By default, they are appended 
	 * to the console output, with no color. Progress values are ignored.
	 */
	private Logger logger = new Logger() {
		@Override
		public void log(String message, Color color) {
			System.out.print(message);
		}
		@Override
		public void error(String message) {
			System.err.println(message);
		}
		@Override
		public void setProgress(float val) {}
	};

	/*
	 * RUN METHOD
	 */
	
	/** 
	 * If the <code>arg</code> is empty or <code>null</code>, simply launch the GUI.
	 * Otherwise, parse it and execture the plugin without the GUI. 
	 */
	public void run(String arg) {
		final Spot_Tracker instance = this;
		if (null == arg || arg.isEmpty()) {
			// Launch the GUI 
			SwingUtilities.invokeLater(new Runnable() {			
				@Override
				public void run() {
					SpotTrackerFrame mainGui = new SpotTrackerFrame(instance);
					mainGui.setLocationRelativeTo(null);
					mainGui.setVisible(true);
				}
			});
			return;
		}

		// If ImageJ is running, we use its toolbar to echo log messages
		if (IJ.getInstance() != null)
			logger = new Logger() {
				public void log(String message, Color color) {IJ.showStatus(message);}
				public void error(String message) { IJ.error("Spot_Tracker", message);}
				public void setProgress(float val) {IJ.showProgress(val); }
		};
		
		// Parse arg string for settings
		try {
			Map<String, String> options = SplitString.splitMacroOptions(arg);
			for(String key : options.keySet())
				for(String setting : optionStrings.keySet())
					if (key.equals(setting))
						optionStrings.put(key, options.get(key));
		} catch (ParseException e) {
			logger.error(e.toString());
		}
		
		Settings  settings = new Settings();
		settings.expectedDiameter 	= Float.parseFloat(optionStrings.get(DIAMETER_KEY));
		settings.useMedianFilter 	= Boolean.parseBoolean(optionStrings.get(MEDIAN_FILTER_KEY));
		settings.allowEdgeMaxima 	= Boolean.parseBoolean(optionStrings.get(ALLOW_EDGE_KEY));
		
		// Run plugin on current image
		ImagePlus imp = WindowManager.getCurrentImage();
		settings.imp = imp;
		
		// Segment
		execSegmentation(settings);
		// Threshold
		execThresholding(spots, thresholdFeatures, thresholdValues, thresholdAbove);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void execThresholding(final List<Collection<Spot>> spots, final ArrayList<Feature> thresholdFeatures, final ArrayList<Float> thresholdValues, final ArrayList<Boolean> thesholdAbove) {
		selectedSpots = new ArrayList<Collection<Spot>>(spots.size());
		Collection<Spot> spotThisFrame, spotToKeep, spotToRemove;
		
		float threshold;
		Float val;
		Feature feature;
		boolean isAbove;
		
		for (int timepoint = 0; timepoint < spots.size(); timepoint++) {
			
			spotThisFrame = spots.get(timepoint);
			spotToKeep = new ArrayList<Spot>(spotThisFrame);
			
			if (null == thresholdFeatures || null == thresholdValues || null == thesholdAbove) {
				selectedSpots.add(timepoint, spotToKeep);
				continue;
			}
			
			spotToRemove = new ArrayList<Spot>(spotThisFrame.size());

			for (int i = 0; i < thresholdFeatures.size(); i++) {

				threshold = thresholdValues.get(i);
				feature = thresholdFeatures.get(i);
				isAbove = thesholdAbove.get(i);
				spotToRemove.clear();

				if (isAbove) {
					for (Spot spot : spotToKeep) {
						val = spot.getFeature(feature);
						if (null == val)
							continue;
						if ( val < threshold)
							spotToRemove.add(spot);
					}

				} else {
					for (Spot spot : spotToKeep) {
						val = spot.getFeature(feature);
						if (null == val)
							continue;
						if ( val > threshold)
							spotToRemove.add(spot);
					}
				}
				spotToKeep.removeAll(spotToRemove); // no need to treat them multiple times
			}
			selectedSpots.add(timepoint, spotToKeep);
		}
	}
	
	/** 
	 * Execute the segmentation part.
	 * <p>
	 * This method looks for bright blobs: bright object of approximately spherical shape, whose expected 
	 * diameter is given in argument. The following steps are applied:
	 * 
	 * <ol>
	 * 
	 * <li>An optional median filter is applied to reduce salt-and-pepper noise in hopes
	 * of false-positive reduction (the user specifies whether to run a median filter). See {@link MedianFilter}.</li>
	 *
	 * <li>The image is down-sampled in order to: [a] reduce the processing time of the other pre-processing
	 * techniques; and [b] reduce the scale of the bright objects to be a uniform diameter in
	 * all dimensions. Thus, each dimension is scaled separately to reduce the diameter in that dimension
	 * to the uniform diameter, if possible, since it is possible the original diameter
	 * is less than the desired diameter to start, in which case no down-sampling is performed in that dimension.</li>
	 * 
	 * <li>The newly down-sized, and optionally median filtered, image is then convolved with a Gaussian kernel,
	 * with <code>sigma = expected diameter / sqrt(ndim)</code>. Using this <code>sigma</code>, the Gaussian kernel is about the size of our objects (nuclei)
	 * in volume, and therefore will (hopefully) make the center of the nuclei the brightest pixel in the convolved 
	 * image, while at the same time further eliminating noise. The convolution itself is performed using the Fourier Transform
	 * approach to convolution, which resulted in a faster computation.</li>
	 * 
	 * <li>The image is then convoluted with a modified Laplacian kernel (again, via a Fourier Transform). The kernel is modified
	 * such that the center matrix cell is actually positive, and the surrounding cells are negative. The result of this
	 * modification is an image where the brightest regions in the input image are also the brightest regions in the output image.
	 * The purpose of this convolution is to accentuate the areas on the image where the bright pixels are "flowing," which should
	 * represent object centers following the Gaussian convolution.</li>
	 * 
	 * <li>Finally, the entire, newly processed image is searched for regional maxima, which are defined as pixels (or groups of equally-valued
	 * intensity pixels) that are strictly brighter than their adjacent neighbors. Following the pre-processing steps above,
	 * these regional maxima likely represent the center of the objects we would like to find and track.
	 * See {@link SpotSegmenter}.
	 * </li>
	 * 
	 * <li> This gives us a collection of spots, which at this stage simply wrap a physical center location. Features are then
	 * calculated for each spot, using their location, the raw image, and the filtered image. See the {@link FeatureFacade} class
	 * for details. 
	 * </ol>
	 * 
	 * Because of the presence of noise, it is possible that some of the regional maxima found in the previous step have
	 * identified noise, rather than objects of interest. A thresholding operation based on the calculated features in this 
	 * step should allow to rule them out.
	 * 
	 * @param imp  the {@link ImagePlus} to segment. Its physical calibration fields must be set correctly.
	 * @param settings  the {@link Settings} for segmentation. Only the fields <code>expectedDiameter</code>, <code>useMedianFilter</code>
	 * and <code>allowEdgeMaxima</code> are used here.
	 * @return  a list ({@link ArrayList}) of {@link Spot} collections. There is one collection in the list per time-point. Collections in the list are ordered 
	 * by frame number (time-point 0 is item 0 in the list, etc...).   
	 */
	public void execSegmentation(final Settings settings) {
		final ImagePlus imp = settings.imp;
		if (null == imp) {
			logger.error("No image to operate on.");
			return;
		}

		/* 0 -- Initialize local variables */
		final int numFrames = imp.getNFrames();
		final float[] calibration = new float[] {(float) imp.getCalibration().pixelWidth, (float) imp.getCalibration().pixelHeight, (float) imp.getCalibration().pixelDepth};
		final float diam = settings.expectedDiameter;
		final boolean useMedFilt = settings.useMedianFilter;
		final boolean allowEdgeMax = settings.allowEdgeMaxima;
		
		// Since we can't get the NumericType out of imp, we assume it is a FloatType.
		final SpotSegmenter<FloatType> segmenter = new SpotSegmenter<FloatType>(null, diam, calibration, useMedFilt, allowEdgeMax);				
		spots = new ArrayList<Collection<Spot>>(numFrames);
		Collection<Spot> spotsThisFrame;
		Image<FloatType> filteredImage;
		
		// For each frame...
		for (int i = 0; i < numFrames; i++) {
			
			/* 1 - Prepare stack for use with Imglib. */
			
			logger.log("Frame "+(i+1)+": Converting to ImgLib...\n");
			Image<FloatType> img = Utils.getSingleFrameAsImage(imp, i);
			
			/* 2 Segment it */

			logger.log("Frame "+(i+1)+": Segmenting...\n");
			logger.setProgress((i+1) / (2f * numFrames + 1));
			segmenter.setImage(img);
			if (segmenter.checkInput() && segmenter.process()) {
				filteredImage = segmenter.getFilteredImage();
				spotsThisFrame = segmenter.getResult();
				for (Spot spot : spotsThisFrame)
					spot.setFrame(i);
				spots.add(i, spotsThisFrame);
				logger.log("Frame "+(i+1)+": found "+spotsThisFrame.size()+" spots.\n");
			} else {
				logger.error(segmenter.getErrorMessage());
				return;
			}
			
			/* 3 - Extract features for the spot collection */
			logger.log("Frame "+(i+1)+": Calculating features...\n");
			logger.setProgress((i+2) / (2f * numFrames + 1));
			final FeatureFacade<FloatType> featureCalculator = new FeatureFacade<FloatType>(img, filteredImage, diam, calibration);
			featureCalculator.processFeature(Feature.MEAN_INTENSITY, spotsThisFrame);
			
		} // Finished looping over frames
				
		return;
	}
	
	/*
	 * GETTERS / SETTERS
	 */

	/**
	 * Return the spots generated by the segmentation part of this plugin. The collection are un-filtered and contain
	 * all spots. They are returned as a List of Collection, one item in the list per time-point, in order.
	 * @see #execSegmentation(ImagePlus, Settings)
	 */
	public List<Collection<Spot>> getSpots() {
		return spots;
	}

	/**
	 * Return the spots filtered by feature, after the execution of {@link #execThresholding(List, ArrayList, ArrayList, ArrayList)}.
	 * @see #execSegmentation(ImagePlus, Settings)
	 * @see #addThreshold(Feature, float, boolean)
	 */
	public List<Collection<Spot>> getSelectedSpots() {
		return selectedSpots;
	}
	
	/**
	 * Add a threshold to the list of thresholds to deal with when executing {@link #execThresholding(List, ArrayList, ArrayList, ArrayList)}.
	 * @param feature  the spot feature to threshold on
	 * @param value  the value of the threshold
	 * @param isAbove  should we threshold above the value?
	 */
	public void addThreshold(final Feature feature, final float value, final boolean isAbove) {
		if (null == feature) 
			return;
		thresholdFeatures.add(feature);
		thresholdValues.add(value);
		thresholdAbove.add(isAbove);
	}
	
	public void clearTresholds() {
		thresholdFeatures.clear();
		thresholdValues.clear();
		thresholdAbove.clear();
	}
	
	public List<Feature> getThresholdFeatures() { return thresholdFeatures; }
	public List<Float> getThresholdValues() { return thresholdValues; }
	public List<Boolean> getThresholdAbove() { return thresholdAbove; }

	/**
	 * Set the logger that will receive the messages from the processes occuring within this plugin.
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
}
