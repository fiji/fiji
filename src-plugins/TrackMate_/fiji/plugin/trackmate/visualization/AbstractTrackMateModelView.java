package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeListener;
import fiji.plugin.trackmate.TrackMateSelectionChangeEvent;
import fiji.plugin.trackmate.TrackMateSelectionChangeListener;

/**
 * An abstract class for spot displayers, that can overlay segmented spots and tracks on top
 * of the image data.
 * <p>
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Jan 2011
 */
public abstract class AbstractTrackMateModelView implements TrackMateSelectionChangeListener, TrackMateSelectionView, TrackMateModelView, TrackMateModelChangeListener {

	/*
	 * FIELDS
	 */

	/**
	 * A map of String/Object that configures the look and feel of the display.
	 */
	protected Map<String, Object> displaySettings = new HashMap<String, Object>();

	/** The model displayed by this class. */
	protected TrackMateModel model;
	
	/** The track colors. */
	protected Map<Set<Spot>, Color> trackColors;
	
	/** The list of listener to warn for spot selection change. */
	protected ArrayList<TrackMateSelectionChangeListener> selectionChangeListeners = new ArrayList<TrackMateSelectionChangeListener>();


	/*
	 * PRIVATE CONSTRUCTOR
	 */

	protected AbstractTrackMateModelView() {
		initDisplaySettings();
	}
	
	/*
	 * PUBLIC METHODS
	 */

	@Override
	public TrackMateModel getModel() {
		return model;
	}

	@Override
	public void setModel(TrackMateModel model) {
		if (null != this.model) {
			this.model.removeTrackMateModelChangeListener(this);
			this.model.removeTrackMateSelectionChangeListener(this);
		}
		this.model = model;
		this.model.addTrackMateModelChangeListener(this);
		this.model.addTrackMateSelectionChangeListener(this);
	}

	
	@Override
	public void setDisplaySettings(final String key, final Object value) {
		displaySettings.put(key, value);
	}
	
	@Override 
	public Object getDisplaySettings(final String key) {
		return displaySettings.get(key);
	}

	@Override 
	public Map<String, Object> getDisplaySettings() {
		return displaySettings;
	}

	/*
	 * PROTECTED METHODS
	 */

	protected void initDisplaySettings() {
		displaySettings.put(KEY_COLOR, DEFAULT_COLOR);
		displaySettings.put(KEY_HIGHLIGHT_COLOR, DEFAULT_HIGHLIGHT_COLOR);
		displaySettings.put(KEY_SPOTS_VISIBLE, true);
		displaySettings.put(KEY_DISPLAY_SPOT_NAMES, false);
		displaySettings.put(KEY_SPOT_COLOR_FEATURE, null);
		displaySettings.put(KEY_SPOT_RADIUS_RATIO, 1.0f);
		displaySettings.put(KEY_TRACKS_VISIBLE, true);
		displaySettings.put(KEY_TRACK_DISPLAY_MODE, DEFAULT_TRACK_DISPLAY_MODE);
		displaySettings.put(KEY_TRACK_DISPLAY_DEPTH, DEFAULT_TRACK_DISPLAY_DEPTH);
		displaySettings.put(KEY_COLORMAP, DEFAULT_COLOR_MAP);
	}
	
	/*
	 * TMSelectionChangeListener
	 */

	@Override
	public void selectionChanged(TrackMateSelectionChangeEvent event) {
		highlightSpots(model.getSpotSelection());
		highlightEdges(model.getEdgeSelection());
		// Center on selection if we added one spot exactly
		Map<Spot, Boolean> spotsAdded = event.getSpots();
		if (spotsAdded != null && spotsAdded.size() == 1) {
			boolean added = spotsAdded.values().iterator().next();
			if (added) {
				Spot spot = spotsAdded.keySet().iterator().next();
				centerViewOn(spot);
			}
		}
	}
}