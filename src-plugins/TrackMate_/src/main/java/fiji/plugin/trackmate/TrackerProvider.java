package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.Element;

import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.NearestNeighborTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.TrackerConfigurationPanel;
import fiji.plugin.trackmate.tracking.FastLAPTracker;
import fiji.plugin.trackmate.tracking.SimpleFastLAPTracker;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;

public class TrackerProvider <T extends RealType<T> & NativeType<T>> implements TrackerKeys {


	/** The tracker names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant tracker classes.  */
	protected List<String> keys;
	protected String currentKey = SimpleFastLAPTracker.TRACKER_KEY;
	protected String errorMessage;
	private ArrayList<String> names;
	private ArrayList<String> infoTexts;
	protected final TrackMateModel<T> model;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the spot trackers currently available in the 
	 * TrackMate plugin. Each tracker is identified by a key String, which can be used 
	 * to retrieve new instance of the tracker, settings for the target tracker and a 
	 * GUI panel able to configure these settings.
	 * <p>
	 * To proper instantiate the target {@link SpotTracker}s, this provider has a reference
	 * to the target model. It is this provider's responsibility to pass the required 
	 * info to the concrete {@link SpotTracker}, extracted from the stored model.
	 * <p>
	 * If you want to add custom trackers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom trackers and pass this 
	 * extended provider to the {@link TrackMate_} plugin.
	 */
	public TrackerProvider(TrackMateModel<T> model) {
		this.model = model;
		registerTrackers();
		
	}


	/*
	 * METHODS
	 */

	/**
	 * Register the standard trackers shipped with TrackMate.
	 */
	protected void registerTrackers() {
		// keys
		keys = new ArrayList<String>();
		keys.add(SimpleFastLAPTracker.TRACKER_KEY);
		keys.add(SimpleFastLAPTracker.TRACKER_KEY);
		keys.add(NearestNeighborTracker.TRACKER_KEY);
		// infoTexts
		infoTexts = new ArrayList<String>();
		infoTexts.add(SimpleFastLAPTracker.INFO_TEXT);
		infoTexts.add(FastLAPTracker.INFO_TEXT);
		infoTexts.add(NearestNeighborTracker.INFO_TEXT);
		// Names
		names = new ArrayList<String>();
		names.add(SimpleFastLAPTracker.NAME);
		names.add(FastLAPTracker.NAME);
		names.add(NearestNeighborTracker.NAME);
	}


	/**
	 * @return an error message for the last unsuccessful methods call
	 * amongst {@link #select(String)}, {@link #marshall(Map, Element)},
	 * {@link #unmarshall(Element, Map)}, {@link #checkSettingsValidity(Map)}.
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Configure this provider for the target {@link SpotDetectorFactory} identified by 
	 * the given key. If the key is not found in this provider's list, the 
	 * provider state is not changed.
	 * @return true if the given key was found and the target detector was changed.
	 */
	public boolean select(final String key) {
		if (keys.contains(key)) {
			currentKey = key;
			errorMessage = null;
			return true;
		} else {
			errorMessage = "Unknown tracker key: "+key+".\n";
			return false;
		}
	}

	/**
	 * @return the currently selected key.
	 */
	public String getCurrentKey() {
		return currentKey;
	}
	
	/**
	 * @return a new instance of the target tracker identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public SpotTracker getTracker() {
		
		final Map<String, Object> settings = model.getSettings().trackerSettings;
		final SpotCollection spots = model.getFilteredSpots();
		final Logger logger = model.getLogger();
		
		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY)) {
			return new SimpleFastLAPTracker(spots, settings, logger);
			
		} else if (currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			return new FastLAPTracker(spots, settings, logger);
			
		} else if (currentKey.equals(NearestNeighborTracker.TRACKER_KEY)) {
			final double maxDist = (Double) settings.get(KEY_LINKING_MAX_DISTANCE);
			return new NearestNeighborTracker(spots, maxDist, logger);
			
		} else {
			return null;
		}
	}

	/**
	 * @return the html String containing a descriptive information about the target tracker,
	 * or <code>null</code> if it is unknown to this factory.
	 */
	public String getInfoText() {
		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY)) {
			return SimpleFastLAPTracker.INFO_TEXT;
			
		} else if (currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			return FastLAPTracker.INFO_TEXT;
			
		} else if (currentKey.equals(NearestNeighborTracker.TRACKER_KEY)) {
			return NearestNeighborTracker.INFO_TEXT;
			
		} else {
			return null;
		}
	}

	/**
	 * @return a new GUI panel able to configure the settings suitable for the target tracker 
	 * identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */

	public TrackerConfigurationPanel<T> getTrackerConfigurationPanel() 	{
		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY)) {
			return new SimpleLAPTrackerSettingsPanel<T>(SimpleFastLAPTracker.INFO_TEXT);
			
		} else if (currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			return new LAPTrackerSettingsPanel<T>();
			
		} else if (currentKey.equals(NearestNeighborTracker.TRACKER_KEY)) {
			return new NearestNeighborTrackerSettingsPanel<T>(NearestNeighborTracker.INFO_TEXT);
			
		} else {
			return null;
		}
	}
	
	/**
	 * @return a new default settings map suitable for the target tracker identified by 
	 * the {@link #currentKey}. Settings are instantiated with default values.  
	 * If the key is unknown to this provider, <code>null</code> is returned. 
	 */
	public Map<String, Object> getDefaultSettings() {
		Map<String, Object> settings = new HashMap<String, Object>();

		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY) || currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			// Linking
			settings.put(KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE);
			settings.put(KEY_LINKING_FEATURE_PENALTIES, DEFAULT_LINKING_FEATURE_PENALTIES);
			// Gap closing
			settings.put(KEY_ALLOW_GAP_CLOSING, DEFAULT_ALLOW_GAP_CLOSING);
			settings.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, DEFAULT_GAP_CLOSING_MAX_FRAME_GAP);
			settings.put(KEY_GAP_CLOSING_MAX_DISTANCE, DEFAULT_GAP_CLOSING_MAX_DISTANCE);
			settings.put(KEY_GAP_CLOSING_FEATURE_PENALTIES, DEFAULT_GAP_CLOSING_FEATURE_PENALTIES);
			// Track splitting
			settings.put(KEY_ALLOW_TRACK_SPLITTING, DEFAULT_ALLOW_TRACK_SPLITTING);
			settings.put(KEY_SPLITTING_MAX_DISTANCE, DEFAULT_SPLITTING_MAX_DISTANCE);
			settings.put(KEY_SPLITTING_FEATURE_PENALTIES, DEFAULT_SPLITTING_FEATURE_PENALTIES);
			// Track merging
			settings.put(KEY_ALLOW_TRACK_MERGING, DEFAULT_ALLOW_TRACK_MERGING);
			settings.put(KEY_MERGING_MAX_DISTANCE, DEFAULT_MERGING_MAX_DISTANCE);
			settings.put(KEY_MERGING_FEATURE_PENALTIES, DEFAULT_MERGING_FEATURE_PENALTIES);
			// Others
			settings.put(KEY_BLOCKING_VALUE, DEFAULT_BLOCKING_VALUE);
			settings.put(KEY_ALTERNATIVE_LINKING_COST_FACTOR, DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR);
			settings.put(KEY_CUTOFF_PERCENTILE, DEFAULT_CUTOFF_PERCENTILE);
			
		} else if (currentKey.equals(NearestNeighborTracker.TRACKER_KEY)) {
			settings.put(KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE);
			
		} else {
			return null;
		}
		return settings;

	}


	/**  @return a list of the tracker keys available through this provider.  */
	public List<String> getTrackerKeys() {
		return keys;
	}

	/**  @return a list of the tracker info texts available through this provider.  */
	public List<String> getTrackerInfoTexts() {
		return infoTexts;
	}
	
	/**  @return a list of the tracker names available through this provider.  */
	public List<String> getTrackerNames() {
		return names;
	}
	
}
