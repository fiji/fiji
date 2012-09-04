package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom.Element;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.NearestNeighborTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.TrackerConfigurationPanel;
import fiji.plugin.trackmate.tracking.FastLAPTracker;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.SimpleFastLAPTracker;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTrackerSettings;

public class TrackerProvider <T extends RealType<T> & NativeType<T>> {


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
		
		if (currentKey.equals(SimpleFastLAPTracker.TRACKER_KEY)) {
			return new SimpleFastLAPTracker<T>(spots, settings);
			
		} else if (currentKey.equals(FastLAPTracker.TRACKER_KEY)) {
			return new FastLAPTracker<T>(spots, settings);
			
		} else if (currentKey.equals(NearestNeighborTracker.TRACKER_KEY)) {
			return new NearestNeighborTracker<T>(spots, settings);
			
		} else {
			return null;
		}
	}

	/**
	 * @return the html String containing a descriptive information about the target tracker,
	 * or <code>null</code> if it is unknown to this factory.
	 */
	public String getInfoText(String key) {
		int index = keys.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return SimpleFastLAPTracker.INFO_TEXT;
		case 1:
			return FastLAPTracker.INFO_TEXT;
		case 2:
			return NearestNeighborTracker.INFO_TEXT;
		default:
			return null;
		}
	}

	/**
	 * @return a new GUI panel able to configure the settings suitable for the target tracker 
	 * identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */

	public TrackerConfigurationPanel<T> getTrackerConfigurationPanel(String key) 	{
		int index = keys.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return new SimpleLAPTrackerSettingsPanel<T>(SimpleFastLAPTracker.INFO_TEXT);
		case 1:
			return new LAPTrackerSettingsPanel<T>();
		case 2:
			return new NearestNeighborTrackerSettingsPanel<T>(NearestNeighborTracker.INFO_TEXT);
		default:
			return null;
		}
	}

	/**
	 * @return a list of the tracker names available through this factory.
	 */
	public List<String> getAvailableTrackers() {
		return keys;
	}

}
