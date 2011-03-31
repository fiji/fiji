package fiji.plugin.trackmate.tracking;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.Listable;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.TrackerSettingsPanel;

public class TrackerFactory implements Listable<SpotTracker> {

	/*
	 * FIELDS
	 */
	
	protected ArrayList<SpotTracker> trackers = null;
	protected SpotTracker simpleLAPTracker = createSimpleLAPTracker();
	protected SpotTracker lapTracker = new LAPTracker();
	protected SpotTracker simpleFastLAPTracker = createSimpleFastLAPTracker();
	protected SpotTracker fastLAPTracker = new FastLAPTracker();
	
	/*
	 * CONSTRUCTOR
	 */
	
	public TrackerFactory() {
		trackers = createTrackerList();
	}
	
	/*
	 * METHODS
	 */
	
	@Override
	public List<SpotTracker> getList() {
		return trackers;
	}
	
	/**
	 * Generate a suitable configuration panel for the tracker set in the 
	 * given settings. Return <code>null</code> if the tracker is unknown.
	 */
	public TrackerSettingsPanel createPanel(Settings settings) {
		SpotTracker tracker = settings.tracker;
		TrackerSettings trackerSettings = settings.trackerSettings;
		if (null == trackerSettings) {
			trackerSettings = tracker.createSettings();
			trackerSettings.spaceUnits = settings.spaceUnits;
			trackerSettings.timeUnits = settings.timeUnits;
		}
		
		if (tracker instanceof LAPTracker || tracker instanceof FastLAPTracker)
			return new LAPTrackerSettingsPanel(trackerSettings);
		
		if (tracker.getClass() == simpleLAPTracker.getClass() || tracker.getClass() == simpleFastLAPTracker.getClass())
			return new SimpleLAPTrackerSettingsPanel(trackerSettings);
		
		return null;
	}
	
	/**
	 * Hook for subclassers.
	 * <p>
	 * Generate a list of concrete instances of {@link SpotTracker}
	 */
	protected ArrayList<SpotTracker> createTrackerList() {
		ArrayList<SpotTracker> list = new ArrayList<SpotTracker>();
		list.add(simpleLAPTracker);
		list.add(lapTracker);
		list.add(simpleFastLAPTracker);
		list.add(fastLAPTracker);
		return list;
	}
	
	
	protected SpotTracker createSimpleLAPTracker() {
		SpotTracker simpleLapTracker = new LAPTracker() {
			
			@Override
			public TrackerSettings createSettings() {
				TrackerSettings ts = new TrackerSettings();
				ts.allowMerging = false;
				ts.allowSplitting = false;
				return ts;
			}
			
			@Override
			public String toString() {
				return "Simple LAP tracker";
			}
			
			@Override
			public String getInfoText() {
				return "<html>" +
				"This tracker is identical to the LAP tracker present in this plugin, except that it <br>" +
				"proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>" +
				"a distance and time condition. Track splitting and merging are not allowed, resulting <br>" +
				"in having non-branching tracks." +
				" </html>";
			}
		};
		
		return simpleLapTracker;
	}
	
	protected SpotTracker createSimpleFastLAPTracker() {
		SpotTracker simpleLapTracker = new FastLAPTracker() {
			
			@Override
			public TrackerSettings createSettings() {
				TrackerSettings ts = new TrackerSettings();
				ts.allowMerging = false;
				ts.allowSplitting = false;
				return ts;
			}
			
			@Override
			public String toString() {
				return "Fast Simple LAP tracker";
			}
			
			@Override
			public String getInfoText() {
				return "<html>" +
				"This tracker is a simple version of the Fast LAP tracker. Only gap closing <br> " +
				"is allowed, based solely on a distance and time condition. Track splitting <br> " +
				"and merging are not allowed, resulting in having non-branching tracks.<br> " +
				" </html>";
			}
		};
		
		return simpleLapTracker;
	}

	public SpotTracker getFromString(String trackerTypeStr) {
		for(SpotTracker tracker : trackers) 
			if (tracker.toString().equals(trackerTypeStr))
				return tracker;
		return null;
	}
	
	
	
}
