package fiji.plugin.trackmate;

import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.features.FeatureAnalyzer;
import fiji.plugin.trackmate.features.FeatureFacade;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * 
 */
public class TrackMateModel {		

	private static final boolean DEBUG = true;
	/** Contain the segmentation result, un-filtered.*/
	protected SpotCollection spots;
	/** Contain the spots retained for tracking, after filtering by features. */
	protected SpotCollection filteredSpots;
	/** The tracks as a graph. */
	protected SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph;

	protected Logger logger = Logger.DEFAULT_LOGGER;
	protected Settings settings;
	protected List<FeatureFilter> featureFilters = new ArrayList<FeatureFilter>();
	@SuppressWarnings("rawtypes")
	protected SpotSegmenter<? extends RealType> segmenter;	
	protected Float initialFilterValue;
	private List<TrackMateModelChangeListener> modelChangeListeners = new ArrayList<TrackMateModelChangeListener>();


	/*
	 * DEAL WITH CHANGE LISTENER
	 */


	public void addTrackMateModelChangeListener(TrackMateModelChangeListener listener) {
		modelChangeListeners.add(listener);
	}

	public boolean removeTrackMateModelChangeListener(TrackMateModelChangeListener listener) {
		return modelChangeListeners.remove(listener);
	} 

	public List<TrackMateModelChangeListener> getTrackMateModelChangeListener(TrackMateModelChangeListener listener) {
		return modelChangeListeners;
	}

	/*
	 * PROCESSES
	 */

	/**
	 * Execute the tracking part.
	 * <p>
	 * This method links all the selected spots from the thresholding part using the selected tracking algorithm.
	 * This tracking process will generate a graph (more precisely a {@link SimpleWeightedGraph}) made of the spot 
	 * election for its vertices, and edges representing the links.
	 * @see #getTrackGraph()
	 */ 
	public void execTracking() {
		SpotTracker tracker = settings.getSpotTracker(filteredSpots);
		tracker.setLogger(logger);
		if (tracker.checkInput() && tracker.process())
			trackGraph = tracker.getTrackGraph();
		else
			logger.error("Problem occured in tracking:\n"+tracker.getErrorMessage()+'\n');
	}

	/** 
	 * Execute the segmentation part.
	 * <p>
	 * This method looks for bright blobs: bright object of approximately spherical shape, whose expected 
	 * diameter is given in argument. The method used for segmentation depends on the {@link SpotSegmenter} 
	 * chosen, and set in {@link #settings};
	 * <p>
	 * This gives us a collection of spots, which at this stage simply wrap a physical center location. 
	 * @see #getSpots()
	 */
	@SuppressWarnings("unchecked")
	public void execSegmentation() {
		final ImagePlus imp = settings.imp;
		if (null == imp) {
			logger.error("No image to operate on.\n");
			return;
		}

		Roi roi = imp.getRoi();
		Polygon polygon = null;
		if (roi != null)
			polygon = roi.getPolygon();

		int numFrames = settings.tend - settings.tstart + 1;

		/* 0 -- Initialize local variables */
		final float[] calibration = new float[] {(float) imp.getCalibration().pixelWidth, (float) imp.getCalibration().pixelHeight, (float) imp.getCalibration().pixelDepth};

		segmenter = settings.getSpotSegmenter();
		segmenter.setCalibration(calibration);

		spots = new SpotCollection();

		// For each frame...
		int spotFound = 0;
		for (int i = settings.tstart-1; i < settings.tend; i++) {

			/* 1 - Prepare stack for use with Imglib. */
			@SuppressWarnings("rawtypes")
			Image img = TMUtils.getSingleFrameAsImage(imp, i, settings); // will be cropped according to settings

			/* 2 Segment it */
			logger.setStatus("Frame "+(i+1)+": Segmenting...");
			logger.setProgress((i-settings.tstart) / (float)numFrames );
			segmenter.setImage(img);
			if (segmenter.checkInput() && segmenter.process()) {
				List<Spot> spotsThisFrame = segmenter.getResult(settings);
				List<Spot> prunedSpots;
				// Prune if outside of ROI
				if (null != polygon) {
					prunedSpots = new ArrayList<Spot>();
					for (Spot spot : spotsThisFrame) {
						if (polygon.contains(spot.getFeature(Feature.POSITION_X)/calibration[0], spot.getFeature(Feature.POSITION_Y)/calibration[1])) 
							prunedSpots.add(spot);
					}
				} else {
					prunedSpots = spotsThisFrame;
				}
				// Add segmentation feature other than position
				for (Spot spot : prunedSpots) {
					spot.putFeature(Feature.POSITION_T, i * settings.dt);
					spot.putFeature(Feature.RADIUS, settings.segmenterSettings.expectedRadius);
				}
				spots.put(i, prunedSpots);
				spotFound += prunedSpots.size();
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

	/**
	 * Execute the initial filtering part.
	 *<p>
	 * Because of the presence of noise, it is possible that some of the regional maxima found in the segmenting step have
	 * identified noise, rather than objects of interest. This can generates a very high number of spots, which is
	 * inconvenient to deal with when it comes to  computing their features, or displaying them.
	 * <p>
	 * Any {@link SpotSegmenter} is expected to at least compute the {@link Feature#QUALITY} value for each spot
	 * it creates, so it is possible to set up an initial filtering on this Feature, prior to any other operation. 
	 * <p>
	 * This method simply takes all the segmented spots, and discard those whose quality value is below the threshold set 
	 * by {@link #setInitialFilter(Float)}. The spot field is overwritten, and discarded spots can't be recalled.
	 * 
	 * @see #getSpots()
	 * @see #setInitialFilter(Float)
	 */
	public void execInitialFiltering() {
		FeatureFilter featureFilter = new FeatureFilter(Feature.QUALITY, initialFilterValue, true);
		this.spots = spots.threshold(featureFilter);
	}

	/**
	 * Calculate given features for the given spots, according to the {@link Settings} set in this model.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. See the {@link FeatureFacade} class
	 * for details. Since a {@link FeatureAnalyzer} can compute more than a {@link Feature} at once, spots might
	 * received more data than required.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void computeFeatures(final SpotCollection toCompute, final List<Feature> features) {

		int numFrames = settings.tend - settings.tstart + 1;
		List<Spot> spotsThisFrame;
		FeatureFacade<?> featureCalculator;
		final float[] calibration = new float[] { settings.dx, settings.dy, settings.dz };

		for (int i = settings.tstart-1; i < settings.tend; i++) {
			logger.setProgress((2*(i-settings.tstart)) / (2f * numFrames + 1));
			logger.setStatus("Frame "+(i+1)+": Calculating features...");

			/* 1 - Prepare stack for use with Imglib.
			 * This time, since the spot coordinates are with respect to the top-left corner of the image, 
			 * we must not generate a cropped version of the image, but a full snapshot. 	 */
			Settings uncroppedSettings = new Settings();
			uncroppedSettings.xstart = 1;
			uncroppedSettings.xend   = settings.imp.getWidth();
			uncroppedSettings.ystart = 1;
			uncroppedSettings.yend   = settings.imp.getHeight();
			uncroppedSettings.zstart = 1;
			uncroppedSettings.zend   = settings.imp.getNSlices();
			Image<? extends RealType> img = TMUtils.getSingleFrameAsImage(settings.imp, i, uncroppedSettings); 

			/* 1.5 Determine what analyzers are needed */
			featureCalculator = new FeatureFacade(img, calibration);
			HashSet<FeatureAnalyzer> analyzers = new HashSet<FeatureAnalyzer>();
			for (Feature feature : features)
				analyzers.add(featureCalculator.getAnalyzerForFeature(feature));

			/* 2 - Compute features. */
			spotsThisFrame = toCompute.get(i);
			for (FeatureAnalyzer analyzer : analyzers)
				analyzer.process(spotsThisFrame);

		} // Finished looping over frames
		logger.setProgress(1);
		logger.setStatus("");
		return;
	}
	
	/**
	 * Calculate given features for the all segmented spots of this model, 
	 * according to the {@link Settings} set in this model.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. See the {@link FeatureFacade} class
	 * for details. Since a {@link FeatureAnalyzer} can compute more than a {@link Feature} at once, spots might
	 * received more data than required.
	 */
	public void computeFeatures(final List<Feature> features) {
		computeFeatures(spots, features);
	}

	/**
	 * Calculate given featuresfor the all filtered spots of this model, 
	 * according to the {@link Settings} set in this model.
	 */
	public void computeFeatures(final Feature feature) {
		ArrayList<Feature> features = new ArrayList<Feature>(1);
		features.add(feature);
		computeFeatures(features);
	}

	/**
	 * Calculate all features for all segmented spots.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. See the {@link FeatureFacade} class
	 * for details. 
	 */
	public void computeFeatures() {
		computeFeatures(spots);
	}
	
	/**
	 * Calculate all features for all segmented spots.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. See the {@link FeatureFacade} class
	 * for details. 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void computeFeatures(final SpotCollection toCompute) {
		int numFrames = toCompute.keySet().size();
		List<Spot> spotsThisFrame;
		FeatureFacade<?> featureCalculator;
		final float[] calibration = settings.getCalibration();

		for (int i : toCompute.keySet()) {
			logger.setProgress((2*(i-settings.tstart)) / (2f * numFrames + 1));
			logger.setStatus("Frame "+(i+1)+": Calculating features...");

			/* 1 - Prepare stack for use with Imglib.
			 * This time, since the spot coordinates are with respect to the top-left corner of the image, 
			 * we must not generate a cropped version of the image, but a full snapshot. 	 */
			Settings uncroppedSettings = new Settings();
			uncroppedSettings.xstart = 1;
			uncroppedSettings.xend   = settings.imp.getWidth();
			uncroppedSettings.ystart = 1;
			uncroppedSettings.yend   = settings.imp.getHeight();
			uncroppedSettings.zstart = 1;
			uncroppedSettings.zend   = settings.imp.getNSlices();
			Image<? extends RealType> img = TMUtils.getSingleFrameAsImage(settings.imp, i, uncroppedSettings); 

			/* 1.5 Determine what analyzers are needed */
			featureCalculator = new FeatureFacade(img, calibration);
			spotsThisFrame = toCompute.get(i);
			featureCalculator.processAllFeatures(spotsThisFrame);

		} // Finished looping over frames
		logger.setProgress(1);
		logger.setStatus("");
		return;
	}


	/**
	 * Execute the feature filtering part.
	 *<p>
	 * Because of the presence of noise, it is possible that some of the regional maxima found in the segmenting step have
	 * identified noise, rather than objects of interest. A filtering operation based on the calculated features in this
	 * step should allow to rule them out.
	 * <p>
	 * This method simply takes all the segmented spots, and store in the field {@link #filteredSpots}
	 * the spots whose features satisfy all of the thresholds entered with the method {@link #addFilter(FeatureFilter)}
	 * @see #getFilteredSpots()
	 */
	public void execFiltering() {
		this.filteredSpots = spots.threshold(featureFilters);
	}


	/*
	 * GETTERS / SETTERS
	 */


	public SimpleWeightedGraph<Spot,DefaultWeightedEdge> getTrackGraph() {
		return trackGraph;
	}

	/**
	 * Return the spots generated by the segmentation part of this plugin. The collection are un-filtered and contain
	 * all spots. They are returned as a {@link SpotCollection}.
	 */
	public SpotCollection getSpots() {
		return spots;
	}

	/**
	 * Return the spots filtered by feature threshold. 
	 * These spots will be used for subsequent tracking and display.
	 * <p>
	 * Feature thresholds can be set / added / cleared by 
	 * {@link #setFeatureThresholds(List)}, {@link #addThreshold(FeatureThreshold)} and {@link #clearTresholds()}.
	 */
	public SpotCollection getFilteredSpots() {
		return filteredSpots;
	}

	/**
	 * Overwrite the raw {@link #spots} field, resulting normally from the {@link #execSegmentation()} process.
	 * @param spots
	 */
	public void setSpots(SpotCollection spots) {
		this.spots = spots;
	}

	/**
	 * Overwrite the {@link #filteredSpots} field, resulting normally from the {@link #execFiltering()} process.
	 */
	public void setFilteredSpots(SpotCollection filteredSpots) {
		this.filteredSpots = filteredSpots;
	}

	/**
	 * Overwrite the {@link #trackGraph} field, resulting from the tracking step.
	 */
	public void setTrackGraph(SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph) {
		this.trackGraph = trackGraph;
	}



	/*
	 * FEATURE FILTERS
	 */

	/**
	 * Add a threshold to the list of filters to deal with when executing {@link #execFiltering(List, ArrayList, ArrayList, ArrayList)}.
	 */
	public void addFeatureFilter(final FeatureFilter filter) { featureFilters.add(filter); }


	public void removeFeatureFilter(final FeatureFilter filter) { featureFilters.remove(filter); }

	/**
	 * Remove all thresholds stored in this model.
	 */
	public void clearFeatureFilters() { featureFilters.clear(); }

	public List<FeatureFilter> getFeatureFilters() { return featureFilters; }

	public void setFeatureFilters(List<FeatureFilter> featureFilters) { this.featureFilters = featureFilters; }

	/**
	 * Return the initial filter value on {@link Feature#QUALITY} stored in this model.
	 */
	public Float getInitialFilterValue() {
		return initialFilterValue;
	}

	/**
	 * Set the initial filter value on {@link Feature#QUALITY} stored in this model.
	 */
	public void setInitialFilterValue(Float initialFilterValue) {
		this.initialFilterValue = initialFilterValue;
	}

	/*
	 * LOGGER
	 */

	/**
	 * Set the logger that will receive the messages from the processes occurring within this plugin.
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Return the logger currently set for this model.
	 */
	public Logger getLogger() {
		return logger;
	}



	/*
	 * SETTINGS
	 */

	/**
	 * Return the {@link Settings} object that determines the behavior of this plugin.
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * Set the {@link Settings} object that determines the behavior of this model's processes.
	 * @see #execSegmentation()
	 * @see #execTracking()
	 */

	public void setSettings(Settings settings) {
		this.settings = settings;
	}


	/*
	 * FEATURES
	 */

	/**
	 * Return a map of {@link Feature} values for the spot collection held by this instance.
	 * Each feature maps a double array, with 1 element per {@link Spot}, all pooled
	 * together.
	 */
	public EnumMap<Feature, double[]> getFeatureValues() {
		return TMUtils.getFeatureValues(spots.values());
	}

	/*
	 * SPOT UPDATING METHODS
	 */

	/**
	 * Move some spots from a frame to another, then update their features.
	 * @param spotsToMove  the list of spots to move
	 * @param fromFrame  the frame each spot originated from
	 * @param toFrame  the destination frame of each spot
	 * @param doNotify  if false, {@link TrackMateModelChangeListener}s will not be notified of this change
	 */
	public void moveSpotsFrom(List<Spot> spotsToMove, List<Integer> fromFrame, List<Integer> toFrame, boolean doNotify) {
		if (null != spots) 
			for (int i = 0; i < spotsToMove.size(); i++) {
				spots.add(spotsToMove.get(i), toFrame.get(i));
				spots.remove(spotsToMove.get(i), fromFrame.get(i));
				if (DEBUG)
					System.out.println("[TrackMateModel] Moving "+spotsToMove.get(i)+" from frame "+fromFrame.get(i)+" to frame "+toFrame.get(i));
			}
		if (null != filteredSpots) 
			for (int i = 0; i < spotsToMove.size(); i++) {
				filteredSpots.add(spotsToMove.get(i), toFrame.get(i));
				filteredSpots.remove(spotsToMove.get(i), fromFrame.get(i));
			}
		updateFeatures(spotsToMove, false);
		
		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(spotsToMove.size());
			for (int i = 0; i < spotFlags.size(); i++)
				spotFlags.add(TrackMateModelChangeEvent.SPOT_FRAME_CHANGED);
			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, spotsToMove, spotFlags, fromFrame, toFrame);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}
	
	/**
	 * Move a single spot from a frame to another, then update its features.
	 * @param spotToMove  the spot to move
	 * @param fromFrame  the frame the spot originated from
	 * @param toFrame  the destination frame
	 * @param doNotify  if false, {@link TrackMateModelChangeListener}s will not be notified of this change
	 */
	public void moveSpotsFrom(Spot spotToMove, Integer fromFrame, Integer toFrame, boolean doNotify) {
		if (null != spots) {
				spots.add(spotToMove, toFrame);
				spots.remove(spotToMove, fromFrame);
				if (DEBUG)
					System.out.println("[TrackMateModel] Moving "+spotToMove+" from frame "+fromFrame+" to frame "+toFrame);
			}
		if (null != filteredSpots) {
				filteredSpots.add(spotToMove, toFrame);
				filteredSpots.remove(spotToMove, fromFrame);
			}
		updateFeatures(spotToMove, false);
		
		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(1);
			List<Integer> toFrames = new ArrayList<Integer>(1);
			List<Integer> fromFrames = new ArrayList<Integer>(1);
			List<Spot> spotsToMove = new ArrayList<Spot>(1);
			spotsToMove .add(spotToMove);
			spotFlags.add(TrackMateModelChangeEvent.SPOT_FRAME_CHANGED);
			toFrames.add(toFrame);
			fromFrames.add(fromFrame);
			
			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, spotsToMove, spotFlags, fromFrames, toFrames);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}


	/**
	 * Add spots to the collections managed by this model, then update heir features.
	 * @param doNotify  if false, listeners of this class will not be notified of this change. 
	 */
	public void addSpotTo(List<Spot> spotsToAdd, List<Integer> toFrame, boolean doNotify) {
		if (null != spots) 
			for (int i = 0; i < spotsToAdd.size(); i++) {
				spots.add(spotsToAdd.get(i), toFrame.get(i));
				if (DEBUG)
					System.out.println("[TrackMateModel] Adding spot "+spotsToAdd.get(i)+" to frame "+ toFrame.get(i));
			}
		if (null != filteredSpots) 
			for (int i = 0; i < spotsToAdd.size(); i++) 
				filteredSpots.add(spotsToAdd.get(i), toFrame.get(i));
		if (null != trackGraph)
			for (Spot spot : spotsToAdd) 
				trackGraph.addVertex(spot);
		updateFeatures(spotsToAdd, false);

		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(spotsToAdd.size());
			for (int i = 0; i < spotFlags.size(); i++)
				spotFlags.add(TrackMateModelChangeEvent.SPOT_ADDED);
			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, spotsToAdd, spotFlags, null, toFrame);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	/**
	 * Add a single spot to the collections managed by this model, then update its features.
	 * @param doNotify  if false, listeners of this class will not be notified of this change. 
	 */
	public void addSpotTo(Spot spotToAdd, Integer toFrame, boolean doNotify) {
		if (null != spots)  {
			spots.add(spotToAdd, toFrame);
			if (DEBUG)
				System.out.println("[TrackMateModel] Adding spot "+spotToAdd+" to frame "+ toFrame);
		}
		if (null != filteredSpots) 

			filteredSpots.add(spotToAdd, toFrame);
		if (null != trackGraph)
			trackGraph.addVertex(spotToAdd);
		updateFeatures(spotToAdd, false);

		if (doNotify) {
			List<Spot> spotsToAdd = new ArrayList<Spot>(1);
			List<Integer> spotFlags = new ArrayList<Integer>(1);
			List<Integer> toFrames = new ArrayList<Integer>(1);
			spotFlags.add(TrackMateModelChangeEvent.SPOT_ADDED);
			spotsToAdd.add(spotToAdd);
			toFrames.add(toFrame);
			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, spotsToAdd, spotFlags, null, toFrames);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}


	/**
	 * Remove given spots from the collections managed by this model.
 	 * @param doNotify  if false, listeners of this class will not be notified of this change. 
	 */
	public void removeSpotFrom(List<Spot> spotsToRemove, List<Integer> fromFrame, boolean doNotify) {
		if (null != spots) 
			for (int i = 0; i < spotsToRemove.size(); i++) {
				spots.remove(spotsToRemove.get(i), fromFrame.get(i));
				if (DEBUG)
					System.out.println("[TrackMateModel] Removing spot "+spotsToRemove.get(i)+" from frame "+ fromFrame.get(i));
			}
		if (null != filteredSpots) 
			for (int i = 0; i < spotsToRemove.size(); i++) 
				filteredSpots.remove(spotsToRemove.get(i), fromFrame.get(i));
		if (null != trackGraph)
			for (Spot spot : spotsToRemove) 
				trackGraph.removeVertex(spot);
		
		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(spotsToRemove.size());
			for (int i = 0; i < spotFlags.size(); i++)
				spotFlags.add(TrackMateModelChangeEvent.SPOT_REMOVED);
			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, spotsToRemove, spotFlags, fromFrame, null);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	/**
	 * Remove a single spot from the collections managed by this model.
	 * @param doNotify  if false, listeners of this class will not be notified of this change. 
	 */
	public void removeSpotFrom(Spot spotToRemove, Integer fromFrame, boolean doNotify) {
		if (null != spots) {
			spots.remove(spotToRemove, fromFrame);
			if (DEBUG)
				System.out.println("[TrackMateModel] Removing spot "+spotToRemove+" from frame "+ fromFrame);
		}
		if (null != filteredSpots) 
			filteredSpots.remove(spotToRemove, fromFrame);
		if (null != trackGraph)
			trackGraph.removeVertex(spotToRemove);

		if (doNotify) {
			List<Spot> spotsToRemove = new ArrayList<Spot>(1);
			List<Integer> spotFlags = new ArrayList<Integer>(1);
			List<Integer> fromFrames = new ArrayList<Integer>(1);
			spotFlags.add(TrackMateModelChangeEvent.SPOT_REMOVED);
			spotsToRemove.add(spotToRemove);
			fromFrames.add(fromFrame);
			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, spotsToRemove, spotFlags, fromFrames, null);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	public void updateFeatures(List<Spot> spotsToUpdate, boolean doNotify) {
		if (DEBUG)
			System.out.println("[TrackMateModel] Updating the features of spot "+spotsToUpdate.size());
		if (null == spots)
			return;

		// Find common frames
		SpotCollection toCompute = filteredSpots.subset(spotsToUpdate);
		computeFeatures(toCompute);


		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(spotsToUpdate.size());
			for (int i = 0; i < spotFlags.size(); i++)
				spotFlags.add(TrackMateModelChangeEvent.SPOT_MODIFIED);
			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, spotsToUpdate, spotFlags, null, null);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	public void updateFeatures(Spot spotToUpdate, boolean doNotify) {
		if (DEBUG)
			System.out.println("[TrackMateModel] Updating the features of spot "+spotToUpdate);
		if (null == spots)
			return;

		// Find frame
		SpotCollection toCompute = new SpotCollection();
		int frame = spots.getFrame(spotToUpdate);
		toCompute.add(spotToUpdate, frame);

		// Calculate features
		computeFeatures(toCompute);
		
		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(1);
			List<Spot> spotsToUpdate = new ArrayList<Spot>(1);
			spotsToUpdate.add(spotToUpdate);
			spotFlags.add(TrackMateModelChangeEvent.SPOT_MODIFIED);
			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, spotsToUpdate, spotFlags, null, null);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}



}
