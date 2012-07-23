package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.gui.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.NearestNeighborTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.TrackerConfigurationPanel;
import fiji.plugin.trackmate.tracking.FastLAPTracker;
import fiji.plugin.trackmate.tracking.LAPTrackerSettings;
import fiji.plugin.trackmate.tracking.SimpleFastLAPTracker;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTrackerSettings;

public class TrackerFactory <T extends RealType<T> & NativeType<T>> {

	
	/** The tracker names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant tracker classes.  */
	protected List<String> names;
	
	/*
	 * BLANK CONSTRUCTOR
	 */
	
	/**
	 * This factory provides the GUI with the spot trackers currently available in the 
	 * TrackMate plugin. Each tracker is identified by a key String, which can be used 
	 * to retrieve new instance of the tracker, settings for the target tracker and a 
	 * GUI panel able to configure these settings.
	 * <p>
	 * If you want to add custom trackers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom trackers and provide this 
	 * extended factory to the {@link TrackMate_} plugin.
	 */
	public TrackerFactory() {
		registerTrackers();
	}
	
	
	/*
	 * METHODS
	 */
	
	/**
	 * Register the standard trackers shipped with TrackMate.
	 */
	protected void registerTrackers() {
		// Names
		names = new ArrayList<String>(3);
		names.add(SimpleFastLAPTracker.NAME);
		names.add(FastLAPTracker.NAME);
		names.add(NearestNeighborTracker.NAME);
	}
	
	/**
	 * @return a new instance of the target tracker identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public SpotTracker<T> getTracker(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return new SimpleFastLAPTracker<T>();
		case 1:
			return new FastLAPTracker<T>();
		case 2:
			return new NearestNeighborTracker<T>();
		default:
			return null;
		}
	}
	
	/**
	 * @return a new instance of settings suitable for the target tracker identified by 
	 * the key parameter. Settings are instantiated with default values.  
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public TrackerSettings<T> getDefaultSettings(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0: {
			LAPTrackerSettings<T> ts = new LAPTrackerSettings<T>();
			ts.allowMerging = false;
			ts.allowSplitting = false;
			return ts;
		}
		case 1:
			return new LAPTrackerSettings<T>();
		case 2:
			return new NearestNeighborTrackerSettings<T>();
		default:
			return null;
		}
	}
	
	/**
	 * @return the html String containing a descriptive information about the target tracker,
	 * or <code>null</code> if it is unknown to this factory.
	 */
	public String getInfoText(String key) {
		int index = names.indexOf(key);
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
		int index = names.indexOf(key);
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
		return names;
	}

}
