package fiji.plugin.trackmate.io;

import static fiji.plugin.trackmate.io.IOUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntAttribute;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_NAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.DETECTOR_SETTINGS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.FILTERED_SPOT_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.FILTERED_SPOT_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.FILTER_ABOVE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.FILTER_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.FILTER_FEATURE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.FILTER_VALUE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.FRAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_FILENAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_FOLDER_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_HEIGHT_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_NFRAMES_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_NSLICES_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_TIME_UNITS_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.IMAGE_WIDTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.INITIAL_SPOT_FILTER_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.LOG_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.PLUGIN_VERSION_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SETTINGS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SETTINGS_TEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SETTINGS_TSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SETTINGS_XEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SETTINGS_XSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SETTINGS_YEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SETTINGS_YSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SETTINGS_ZEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SETTINGS_ZSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SPOT_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SPOT_COLLECTION_NSPOTS_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SPOT_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SPOT_FILTER_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SPOT_FRAME_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SPOT_ID_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SPOT_ID_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.SPOT_NAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.TRACKER_SETTINGS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v20.TRACK_FILTER_COLLECTION_ELEMENT_KEY;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.imglib2.img.ImgPlus;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.features.track.TrackLocationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;
import fiji.plugin.trackmate.gui.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class TmXmlReader_v20 extends TmXmlReader {

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Initialize this reader to read the file given in argument.
	 */
	public TmXmlReader_v20(final File file) {
		super(file);
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * @return the log text saved in the specified file, or <code>null</code> if
	 *         log text was not saved. Must be called after {@link #process()}.
	 */
	public String getLogText() {
		final Element logElement = root.getChild(LOG_ELEMENT_KEY);
		if (null != logElement) {
			return logElement.getTextTrim();
		} else {
			return "";
		}
	}

	/**
	 * We default to the main hyperstack view.
	 */
	@Override
	public Collection<TrackMateModelView> getViews(final ViewProvider provider) {
		final Collection<TrackMateModelView> views = new ArrayList<TrackMateModelView>(1);
		final TrackMateModelView view = provider.getView(HyperStackDisplayer.NAME);
		views.add(view);
		return views;
	}

	/**
	 * We default to the configure view panel.
	 */
	@Override
	public String getGUIState() {
		return ConfigureViewsDescriptor.KEY;
	}

	/**
	 * @return the version string stored in the file.
	 */
	@Override
	public String getVersion() {
		return root.getAttribute(PLUGIN_VERSION_ATTRIBUTE_NAME).getValue();
	}

	@Override
	public String getErrorMessage() {
		return logger.toString();
	}

	@Override
	public Model getModel() {
		final Model model = new Model();

		// Physical units - fetch them from the settings element
		final Element infoEl = root.getChild(IMAGE_ELEMENT_KEY);
		String spaceUnits;
		String timeUnits;
		if (null != infoEl) {
			spaceUnits = infoEl.getAttributeValue(IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME);
			timeUnits = infoEl.getAttributeValue(IMAGE_TIME_UNITS_ATTRIBUTE_NAME);
		} else {
			spaceUnits = "pixel";
			timeUnits = "frame";
		}
		model.setPhysicalUnits(spaceUnits, timeUnits);

		// Feature declaration - has to be manual declaration
		final FeatureModel fm = model.getFeatureModel();

		fm.declareEdgeFeatures(Spot.FEATURES, Spot.FEATURE_NAMES, Spot.FEATURE_SHORT_NAMES, Spot.FEATURE_DIMENSIONS);
		fm.declareSpotFeatures(SpotIntensityAnalyzerFactory.FEATURES, SpotIntensityAnalyzerFactory.FEATURE_NAMES, SpotIntensityAnalyzerFactory.FEATURE_SHORT_NAMES, SpotIntensityAnalyzerFactory.FEATURE_DIMENSIONS);
		fm.declareSpotFeatures(SpotContrastAndSNRAnalyzerFactory.FEATURES, SpotContrastAndSNRAnalyzerFactory.FEATURE_NAMES, SpotContrastAndSNRAnalyzerFactory.FEATURE_SHORT_NAMES, SpotContrastAndSNRAnalyzerFactory.FEATURE_DIMENSIONS);
		fm.declareSpotFeatures(SpotRadiusEstimatorFactory.FEATURES, SpotRadiusEstimatorFactory.FEATURE_NAMES, SpotRadiusEstimatorFactory.FEATURE_SHORT_NAMES, SpotRadiusEstimatorFactory.FEATURE_DIMENSIONS);

		fm.declareEdgeFeatures(EdgeTargetAnalyzer.FEATURES, EdgeTargetAnalyzer.FEATURE_NAMES, EdgeTargetAnalyzer.FEATURE_SHORT_NAMES, EdgeTargetAnalyzer.FEATURE_DIMENSIONS);
		fm.declareEdgeFeatures(EdgeVelocityAnalyzer.FEATURES, EdgeVelocityAnalyzer.FEATURE_NAMES, EdgeVelocityAnalyzer.FEATURE_SHORT_NAMES, EdgeVelocityAnalyzer.FEATURE_DIMENSIONS);
		fm.declareEdgeFeatures(EdgeTimeLocationAnalyzer.FEATURES, EdgeTimeLocationAnalyzer.FEATURE_NAMES, EdgeTimeLocationAnalyzer.FEATURE_SHORT_NAMES, EdgeTimeLocationAnalyzer.FEATURE_DIMENSIONS);

		fm.declareTrackFeatures(TrackIndexAnalyzer.FEATURES, TrackIndexAnalyzer.FEATURE_NAMES, TrackIndexAnalyzer.FEATURE_SHORT_NAMES, TrackIndexAnalyzer.FEATURE_DIMENSIONS);
		fm.declareTrackFeatures(TrackDurationAnalyzer.FEATURES, TrackDurationAnalyzer.FEATURE_NAMES, TrackDurationAnalyzer.FEATURE_SHORT_NAMES, TrackDurationAnalyzer.FEATURE_DIMENSIONS);
		fm.declareTrackFeatures(TrackBranchingAnalyzer.FEATURES, TrackBranchingAnalyzer.FEATURE_NAMES, TrackBranchingAnalyzer.FEATURE_SHORT_NAMES, TrackBranchingAnalyzer.FEATURE_DIMENSIONS);
		fm.declareTrackFeatures(TrackLocationAnalyzer.FEATURES, TrackLocationAnalyzer.FEATURE_NAMES, TrackLocationAnalyzer.FEATURE_SHORT_NAMES, TrackLocationAnalyzer.FEATURE_DIMENSIONS);
		fm.declareTrackFeatures(TrackSpeedStatisticsAnalyzer.FEATURES, TrackSpeedStatisticsAnalyzer.FEATURE_NAMES, TrackSpeedStatisticsAnalyzer.FEATURE_SHORT_NAMES, TrackSpeedStatisticsAnalyzer.FEATURE_DIMENSIONS);

		// Spots - we can find them under the root element
		final SpotCollection spots = getAllSpots();
		setSpotsVisibility();
		model.setSpots(spots, false);

		// Tracks - we can find them under the root element
		if (!readTracks(root, model)) {
			ok = false;
		}

		// Track features
		try {
			final Map<Integer, Map<String, Double>> savedFeatureMap = readTrackFeatures(root);
			for (final Integer savedKey : savedFeatureMap.keySet()) {

				final Map<String, Double> savedFeatures = savedFeatureMap.get(savedKey);
				for (final String feature : savedFeatures.keySet()) {
					model.getFeatureModel().putTrackFeature(savedKey, feature, savedFeatures.get(feature));
				}
			}
		} catch (final RuntimeException re) {
			logger.error("Problem populating track features:\n");
			logger.error(re.getMessage());
			ok = false;
		}

		// Return
		return model;
	}

	@Override
	public void readSettings(final Settings settings, final DetectorProvider detectorProvider, final TrackerProvider trackerProvider, final SpotAnalyzerProvider spotAnalyzerProvider, final EdgeAnalyzerProvider edgeAnalyzerProvider, final TrackAnalyzerProvider trackAnalyzerProvider) {
		settings.imp = getImage();
		getBaseSettings(settings);
		getDetectorSettings(settings, detectorProvider);
		getTrackerSettings(settings, trackerProvider);
		settings.initialSpotFilterValue = getInitialFilter().value;
		settings.setSpotFilters(getSpotFeatureFilters());
		settings.setTrackFilters(getTrackFeatureFilters());

		// Analyzers - we add them all
		final ImgPlus<?> img = TMUtils.rawWraps(settings.imp);
		settings.clearSpotAnalyzerFactories();
		final List<String> spotAnalyzerKeys = spotAnalyzerProvider.getAvailableSpotFeatureAnalyzers();
		for (final String key : spotAnalyzerKeys) {
			final SpotAnalyzerFactory<?> spotFeatureAnalyzer = spotAnalyzerProvider.getSpotFeatureAnalyzer(key, img);
			settings.addSpotAnalyzerFactory(spotFeatureAnalyzer);
		}

		settings.clearEdgeAnalyzers();
		final List<String> edgeAnalyzerKeys = edgeAnalyzerProvider.getAvailableEdgeFeatureAnalyzers();
		for (final String key : edgeAnalyzerKeys) {
			final EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getEdgeFeatureAnalyzer(key);
			settings.addEdgeAnalyzer(edgeAnalyzer);
		}

		settings.clearTrackAnalyzers();
		final List<String> trackAnalyzerKeys = trackAnalyzerProvider.getAvailableTrackFeatureAnalyzers();
		for (final String key : trackAnalyzerKeys) {
			final TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getTrackFeatureAnalyzer(key);
			settings.addTrackAnalyzer(trackAnalyzer);
		}

	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Returns a map of the saved track features, as they appear in the file
	 */
	private Map<Integer, Map<String, Double>> readTrackFeatures(final Element modelElement) {

		final HashMap<Integer, Map<String, Double>> featureMap = new HashMap<Integer, Map<String, Double>>();

		final Element allTracksElement = modelElement.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement) {
			logger.error("Cannot find the track collection in file.\n");
			ok = false;
			return null;
		}

		// Load tracks
		final List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
		for (final Element trackElement : trackElements) {

			int trackID = -1;
			try {
				trackID = trackElement.getAttribute(TrackIndexAnalyzer.TRACK_ID).getIntValue();
			} catch (final DataConversionException e1) {
				logger.error("Found a track with invalid trackID for " + trackElement + ". Skipping.\n");
				ok = false;
				continue;
			}

			final HashMap<String, Double> trackMap = new HashMap<String, Double>();

			final List<Attribute> attributes = trackElement.getAttributes();
			for (final Attribute attribute : attributes) {

				String attName = attribute.getName();
				if (attName.equals(TRACK_NAME_ATTRIBUTE_NAME)) { // Skip trackID attribute
					continue;
				} else if (attName.equals("X_LOCATION")) { // convert old names on the fly
					attName = TrackLocationAnalyzer.X_LOCATION;
				} else if (attName.equals("Y_LOCATION")) {
					attName = TrackLocationAnalyzer.Y_LOCATION;
				} else if (attName.equals("Z_LOCATION")) {
					attName = TrackLocationAnalyzer.Z_LOCATION;
				}

				Double attVal = Double.NaN;
				try {
					attVal = attribute.getDoubleValue();
				} catch (final DataConversionException e) {
					logger.error("Track " + trackID + ": Cannot read the feature " + attName + " value. Skipping.\n");
					ok = false;
					continue;
				}

				trackMap.put(attName, attVal);

			}

			featureMap.put(trackID, trackMap);
		}

		return featureMap;

	}

	private ImagePlus getImage() {
		final Element imageInfoElement = root.getChild(IMAGE_ELEMENT_KEY);
		if (null == imageInfoElement)
			return null; // value will still be null
		final String filename = imageInfoElement.getAttributeValue(IMAGE_FILENAME_ATTRIBUTE_NAME);
		String folder = imageInfoElement.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME);
		if (null == filename || filename.isEmpty())
			return null;
		if (null == folder || folder.isEmpty())
			folder = file.getParent(); // it is a relative path, then
		File imageFile = new File(folder, filename);
		if (!imageFile.exists() || !imageFile.canRead()) {
			// Could not find it to the absolute path. Then we look for the same path of the xml file
			folder = file.getParent();
			imageFile = new File(folder, filename);
			if (!imageFile.exists() || !imageFile.canRead()) {
				return null;
			}
		}
		return IJ.openImage(imageFile.getAbsolutePath());
	}

	/**
	 * Return the initial threshold on quality stored in this file. Return
	 * <code>null</code> if the initial threshold data cannot be found in the
	 * file.
	 */
	private FeatureFilter getInitialFilter() {

		final Element itEl = root.getChild(INITIAL_SPOT_FILTER_ELEMENT_KEY);
		if (null == itEl)
			return null;
		final String feature = itEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
		final Double value = readDoubleAttribute(itEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
		final boolean isAbove = readBooleanAttribute(itEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
		final FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
		return ft;
	}

	/**
	 * Return the list of {@link FeatureFilter} for spots stored in this file.
	 * Return <code>null</code> if the spot feature filters data cannot be found
	 * in the file.
	 */
	private List<FeatureFilter> getSpotFeatureFilters() {

		final List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		final Element ftCollectionEl = root.getChild(SPOT_FILTER_COLLECTION_ELEMENT_KEY);
		if (null == ftCollectionEl)
			return null;
		final List<Element> ftEls = ftCollectionEl.getChildren(FILTER_ELEMENT_KEY);
		for (final Element ftEl : ftEls) {
			final String feature = ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
			final Double value = readDoubleAttribute(ftEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
			final boolean isAbove = readBooleanAttribute(ftEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
			final FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
			featureThresholds.add(ft);
		}
		return featureThresholds;
	}

	/**
	 * @return the list of {@link FeatureFilter} for tracks stored in this file.
	 *         Return <code>null</code> if the track feature filters data cannot
	 *         be found in the file.
	 */
	private List<FeatureFilter> getTrackFeatureFilters() {
		final List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		final Element ftCollectionEl = root.getChild(TRACK_FILTER_COLLECTION_ELEMENT_KEY);
		if (null == ftCollectionEl)
			return null;
		final List<Element> ftEls = ftCollectionEl.getChildren(FILTER_ELEMENT_KEY);
		for (final Element ftEl : ftEls) {
			final String feature = ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
			final Double value = readDoubleAttribute(ftEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
			final boolean isAbove = readBooleanAttribute(ftEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
			final FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
			featureThresholds.add(ft);
		}
		return featureThresholds;
	}

	private void getBaseSettings(final Settings settings) {
		// Basic settings
		final Element settingsEl = root.getChild(SETTINGS_ELEMENT_KEY);
		if (null != settingsEl) {
			settings.xstart = readIntAttribute(settingsEl, SETTINGS_XSTART_ATTRIBUTE_NAME, logger, 1);
			settings.xend = readIntAttribute(settingsEl, SETTINGS_XEND_ATTRIBUTE_NAME, logger, 512);
			settings.ystart = readIntAttribute(settingsEl, SETTINGS_YSTART_ATTRIBUTE_NAME, logger, 1);
			settings.yend = readIntAttribute(settingsEl, SETTINGS_YEND_ATTRIBUTE_NAME, logger, 512);
			settings.zstart = readIntAttribute(settingsEl, SETTINGS_ZSTART_ATTRIBUTE_NAME, logger, 1);
			settings.zend = readIntAttribute(settingsEl, SETTINGS_ZEND_ATTRIBUTE_NAME, logger, 10);
			settings.tstart = readIntAttribute(settingsEl, SETTINGS_TSTART_ATTRIBUTE_NAME, logger, 1);
			settings.tend = readIntAttribute(settingsEl, SETTINGS_TEND_ATTRIBUTE_NAME, logger, 10);
			//			settings.detectionChannel = readIntAttribute(settingsEl, SETTINGS_DETECTION_CHANNEL_ATTRIBUTE_NAME, logger, 1);
		}
		// Image info settings
		final Element infoEl = root.getChild(IMAGE_ELEMENT_KEY);
		if (null != infoEl) {
			settings.dx = readDoubleAttribute(infoEl, IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME, logger);
			settings.dy = readDoubleAttribute(infoEl, IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME, logger);
			settings.dz = readDoubleAttribute(infoEl, IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME, logger);
			settings.dt = readDoubleAttribute(infoEl, IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME, logger);
			settings.width = readIntAttribute(infoEl, IMAGE_WIDTH_ATTRIBUTE_NAME, logger, 512);
			settings.height = readIntAttribute(infoEl, IMAGE_HEIGHT_ATTRIBUTE_NAME, logger, 512);
			settings.nslices = readIntAttribute(infoEl, IMAGE_NSLICES_ATTRIBUTE_NAME, logger, 1);
			settings.nframes = readIntAttribute(infoEl, IMAGE_NFRAMES_ATTRIBUTE_NAME, logger, 1);
			settings.imageFileName = infoEl.getAttributeValue(IMAGE_FILENAME_ATTRIBUTE_NAME);
			settings.imageFolder = infoEl.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME);
		}
	}

	/**
	 * Update the given {@link Settings} object with the
	 * {@link SpotDetectorFactory} and settings map fields named
	 * {@link Settings#detectorFactory} and {@link Settings#detectorSettings}
	 * read within the XML file this reader is initialized with.
	 * <p>
	 * As a side effect, this method also configure the {@link DetectorProvider}
	 * stored in the passed {@link TrackMate_} plugin for the found target
	 * detector factory.
	 * <p>
	 * If the detector settings XML element is not present in the file, the
	 * {@link Settings} object is not updated.
	 * 
	 * @param settings
	 *            the base {@link Settings} object to update.
	 * @param provider
	 *            the {@link DetectorProvider} that can unmarshal detector and
	 *            detector settings.
	 */
	private void getDetectorSettings(final Settings settings, final DetectorProvider provider) {
		final Element element = root.getChild(DETECTOR_SETTINGS_ELEMENT_KEY);
		if (null == element) {
			return;
		}

		final Map<String, Object> ds = new HashMap<String, Object>();
		// All the hard work is delegated to the provider.
		final boolean ok = provider.unmarshall(element, ds);

		if (!ok) {
			logger.error(provider.getErrorMessage());
			return;
		}

		settings.detectorSettings = ds;
		settings.detectorFactory = provider.getDetectorFactory();
	}

	/**
	 * Update the given {@link Settings} object with {@link SpotTracker} proper
	 * settings map fields named {@link Settings#trackerSettings} and
	 * {@link Settings#tracker} read within the XML file this reader is
	 * initialized with.
	 * <p>
	 * If the tracker settings XML element is not present in the file, the
	 * {@link Settings} object is not updated. If the tracker settings or the
	 * tracker info can be read, but cannot be understood (most likely because
	 * the class the XML refers to is unknown) then a default object is
	 * substituted.
	 * 
	 * @param settings
	 *            the base {@link Settings} object to update.
	 * @param provider
	 *            the {@link TrackerProvider} that can unmarshal tracker and
	 *            tracker settings.
	 */
	private void getTrackerSettings(final Settings settings, final TrackerProvider provider) {
		final Element element = root.getChild(TRACKER_SETTINGS_ELEMENT_KEY);
		if (null == element) {
			return;
		}

		final Map<String, Object> ds = new HashMap<String, Object>();
		// All the hard work is delegated to the provider.
		final boolean ok = provider.unmarshall(element, ds);

		if (!ok) {
			logger.error(provider.getErrorMessage());
			return;
		}

		settings.trackerSettings = ds;
		settings.tracker = provider.getTracker();
	}

	/**
	 * Read the list of all spots stored in this file.
	 * <p>
	 * Internally, this methods also builds the cache field, which will be
	 * required by the following methods:
	 * <ul>
	 * <li> {@link #getFilteredSpots()}
	 * <li> {@link #readTracks()}
	 * <li> {@link #readTrackEdges(SimpleDirectedWeightedGraph)}
	 * <li> {@link #readTrackSpots(SimpleDirectedWeightedGraph)}
	 * </ul>
	 * It is therefore sensible to call this method first, just afther
	 * {@link #parse()}ing the file. If not called, this method will be called
	 * anyway by the other methods to build the cache.
	 * 
	 * @return a {@link SpotCollection}. Return <code>null</code> if the spot
	 *         section is not present in the file.
	 */
	private SpotCollection getAllSpots() {
		// Root element for collection
		final Element spotCollection = root.getChild(SPOT_COLLECTION_ELEMENT_KEY);
		if (null == spotCollection)
			return null;

		// Retrieve children elements for each frame
		final List<Element> frameContent = spotCollection.getChildren(SPOT_FRAME_COLLECTION_ELEMENT_KEY);

		// Determine total number of spots
		int nspots = readIntAttribute(spotCollection, SPOT_COLLECTION_NSPOTS_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		if (nspots == 0) {
			// Could not find it or read it. Determine it by quick sweeping through children element
			for (final Element currentFrameContent : frameContent) {
				nspots += currentFrameContent.getChildren(SPOT_ELEMENT_KEY).size();
			}
		}

		// Instantiate cache
		cache = new ConcurrentHashMap<Integer, Spot>(nspots);

		// Load collection and build cache
		int currentFrame = 0;
		ArrayList<Spot> spotList;
		final SpotCollection allSpots = new SpotCollection();

		for (final Element currentFrameContent : frameContent) {

			currentFrame = readIntAttribute(currentFrameContent, FRAME_ATTRIBUTE_NAME, logger);
			final List<Element> spotContent = currentFrameContent.getChildren(SPOT_ELEMENT_KEY);
			spotList = new ArrayList<Spot>(spotContent.size());
			for (final Element spotElement : spotContent) {
				final Spot spot = createSpotFrom(spotElement);
				spotList.add(spot);
				cache.put(spot.ID(), spot);
			}

			allSpots.put(currentFrame, spotList);
		}
		return allSpots;
	}

	/**
	 * Sets the spot visibility as stored in this file.
	 */
	private void setSpotsVisibility() {
		final Element selectedSpotCollection = root.getChild(FILTERED_SPOT_ELEMENT_KEY);
		if (null == selectedSpotCollection)
			return;

		if (null == cache)
			getAllSpots(); // build it if it's not here

		final List<Element> frameContent = selectedSpotCollection.getChildren(FILTERED_SPOT_COLLECTION_ELEMENT_KEY);

		for (final Element currentFrameContent : frameContent) {
			final List<Element> spotContent = currentFrameContent.getChildren(SPOT_ID_ELEMENT_KEY);
			// Loop over all spot element
			for (final Element spotEl : spotContent) {
				// Find corresponding spot in cache
				final int ID = readIntAttribute(spotEl, SPOT_ID_ATTRIBUTE_NAME, logger);
				final Spot spot = cache.get(ID);
				spot.putFeature(SpotCollection.VISIBLITY, SpotCollection.ONE);
			}
		}
	}

	private Spot createSpotFrom(final Element spotEl) {
		final int ID = readIntAttribute(spotEl, SPOT_ID_ATTRIBUTE_NAME, logger);
		final Spot spot = new Spot(ID);

		final List<Attribute> atts = spotEl.getAttributes();
		atts.remove(SPOT_ID_ATTRIBUTE_NAME);

		String name = spotEl.getAttributeValue(SPOT_NAME_ATTRIBUTE_NAME);
		if (null == name || name.equals(""))
			name = "ID" + ID;
		spot.setName(name);
		atts.remove(SPOT_NAME_ATTRIBUTE_NAME);

		for (final Attribute att : atts) {
			if (att.getName().equals(SPOT_NAME_ATTRIBUTE_NAME) || att.getName().equals(SPOT_ID_ATTRIBUTE_NAME)) {
				continue;
			}
			try {
				spot.putFeature(att.getName(), att.getDoubleValue());
			} catch (final DataConversionException e) {
				logger.error("Cannot read the feature " + att.getName() + " value. Skipping.\n");
			}
		}
		return spot;
	}

}