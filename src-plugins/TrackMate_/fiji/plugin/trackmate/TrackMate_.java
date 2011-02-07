package fiji.plugin.trackmate;

import fiji.plugin.trackmate.features.FeatureFacade;
import fiji.plugin.trackmate.gui.TrackMateFrameController;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.SpotTracker;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;


/**
 * <p>The TrackMate_ class runs on the currently active time lapse image (2D or 3D) 
 * and both identifies and tracks bright blobs over time.</p>
 * 
 * <p><b>Required input:</b> A 2D or 3D time-lapse image with bright blobs.</p>
 *
 * @author Nicholas Perry, Jean-Yves Tinevez - Institut Pasteur - July 2010 - January 2011
 *
 */
public class TrackMate_ <T extends RealType<T>> implements PlugIn, TrackMateModelInterface {
	
	public static final String PLUGIN_NAME_STR = "Track Mate";
	public static final String PLUGIN_NAME_VERSION = ".alpha";
	
	
	/** Contain the segmentation result, un-filtered.*/
	private SpotCollection spots;
	/** Contain the Spot retained for tracking, after thresholding by features. */
	private SpotCollection selectedSpots;
	/** The tracks as a graph. */
	private SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph;

	private Logger logger = Logger.DEFAULT_LOGGER;
	private Settings settings;
	private List<FeatureThreshold> thresholds = new ArrayList<FeatureThreshold>();
	private SpotSegmenter<T> segmenter;	
	private Float initialThreshold;

	/*
	 * CONSTRUCTORS
	 */
	
	public TrackMate_() {
		this.settings = new Settings();
	}
	
	public TrackMate_(Settings settings) {
		this.settings = settings;
	}
	
	
	/*
	 * RUN METHOD
	 */
	
	/** 
	 * Launch the GUI.
	 */
	public void run(String arg) {
		settings.imp = WindowManager.getCurrentImage();
		new TrackMateFrameController(this);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public void execTracking() {
		SpotTracker tracker = settings.getSpotTracker(selectedSpots);
		tracker.setLogger(logger);
		if (tracker.checkInput() && tracker.process())
			trackGraph = tracker.getTrackGraph();
		else
			logger.error("Problem occured in tracking:\n"+tracker.getErrorMessage()+'\n');
	}
	
	@Override
	public void execSegmentation() {
		final ImagePlus imp = settings.imp;
		if (null == imp) {
			logger.error("No image to operate on.\n");
			return;
		}

		int numFrames = settings.tend - settings.tstart + 1;

		/* 0 -- Initialize local variables */
		final float[] calibration = new float[] {(float) imp.getCalibration().pixelWidth, (float) imp.getCalibration().pixelHeight, (float) imp.getCalibration().pixelDepth};

		segmenter = settings.getSpotSegmenter();
		segmenter.setCalibration(calibration);
		
		spots = new SpotCollection();
		List<Spot> spotsThisFrame;
		
		// For each frame...
		int spotFound = 0;
		for (int i = settings.tstart-1; i < settings.tend; i++) {
			
			/* 1 - Prepare stack for use with Imglib. */
			Image<T> img = TMUtils.getSingleFrameAsImage(imp, i, settings); // will be cropped according to settings
			
			/* 2 Segment it */

			logger.setStatus("Frame "+(i+1)+": Segmenting...");
			logger.setProgress((i-settings.tstart) / (float)numFrames );
			segmenter.setImage(img);
			if (segmenter.checkInput() && segmenter.process()) {
				spotsThisFrame = segmenter.getResult(settings);
				for (Spot spot : spotsThisFrame)
					spot.putFeature(Feature.POSITION_T, i);
				spots.put(i, spotsThisFrame);
				spotFound += spotsThisFrame.size();
			} else {
				logger.error(segmenter.getErrorMessage()+'\n');
				return;
			}
						
		} // Finished looping over frames
		logger.log("Found "+spotFound+" spots.\n");
		logger.setProgress(1);
		logger.setStatus("");
		return;
	}
	
	@Override
	public void execInitialThresholding() {
		FeatureThreshold featureThreshold = new FeatureThreshold(Feature.QUALITY, initialThreshold, true);
		this.spots = spots.threshold(featureThreshold);
	}
		
	@Override
	public void computeFeatures() {
		int numFrames = settings.tend - settings.tstart + 1;
		List<Spot> spotsThisFrame;
		FeatureFacade<T> featureCalculator;
		final float[] calibration = new float[] { settings.dx, settings.dy, settings.dz };
		
		for (int i = settings.tstart-1; i < settings.tend; i++) {
			
			/* 1 - Prepare stack for use with Imglib. */
			Image<T> img = TMUtils.getSingleFrameAsImage(settings.imp, i, settings); // will be cropped according to settings
			
			/* 2 - Compute features. */
			logger.setProgress((2*(i-settings.tstart)) / (2f * numFrames + 1));
			logger.setStatus("Frame "+(i+1)+": Calculating features...");
			featureCalculator = new FeatureFacade<T>(img, settings.segmenterSettings.expectedRadius, calibration);
			spotsThisFrame = spots.get(i);
			featureCalculator.processAllFeatures(spotsThisFrame);
						
		} // Finished looping over frames
		logger.setProgress(1);
		logger.setStatus("");
		return;
	}


	@Override
	public void execThresholding() {
		this.selectedSpots = spots.threshold(thresholds);
	}
	
	/*
	 * GETTERS / SETTERS
	 */
	
	@Override
	public SimpleWeightedGraph<Spot,DefaultWeightedEdge> getTrackGraph() {
		return trackGraph;
	}
	
	@Override
	public SpotCollection getSpots() {
		return spots;
	}

	@Override
	public SpotCollection getSelectedSpots() {
		return selectedSpots;
	}
	
	@Override
	public void addThreshold(final FeatureThreshold threshold) { thresholds .add(threshold); }
	
	@Override
	public void removeThreshold(final FeatureThreshold threshold) { thresholds .remove(threshold); }
	
	@Override
	public void clearTresholds() { thresholds.clear(); }
	
	@Override
	public List<FeatureThreshold> getFeatureThresholds() { return thresholds; }
	
	@Override
	public void setFeatureThresholds(List<FeatureThreshold> thresholds) { this.thresholds = thresholds; }
	
	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	public Settings getSettings() {
		return settings;
	}

	@Override
	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	@Override
	public void setSpots(SpotCollection spots) {
		this.spots = spots;
	}

	@Override
	public void setSpotSelection(SpotCollection selectedSpots) {
		this.selectedSpots = selectedSpots;
	}
	
	@Override
	public void setTrackGraph(SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph) {
		this.trackGraph = trackGraph;
	}

	@Override
	public EnumMap<Feature, double[]> getFeatureValues() {
		return TMUtils.getFeatureValues(spots.values());
	}

	@Override
	public Float getInitialThreshold() {
		return initialThreshold;
	}

	@Override
	public void setInitialThreshold(Float initialThreshold) {
		this.initialThreshold = initialThreshold;
	}


	
	
}
