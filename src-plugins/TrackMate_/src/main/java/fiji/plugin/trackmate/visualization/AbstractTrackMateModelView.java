package fiji.plugin.trackmate.visualization;

import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;

/**
 * An abstract class for spot displayers, that can overlay detected spots and tracks on top
 * of the image data.
 * <p>
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Jan 2011
 */
public abstract class AbstractTrackMateModelView implements SelectionChangeListener, TrackMateModelView, ModelChangeListener {

	/*
	 * FIELDS
	 */

	/**
	 * A map of String/Object that configures the look and feel of the display.
	 */
	protected Map<String, Object> displaySettings;

	/** The model displayed by this class. */
	protected Model model;

	protected final  SelectionModel selectionModel;
	

	/*
	 * PROTECTED CONSTRUCTOR
	 */

	protected AbstractTrackMateModelView(Model model, SelectionModel selectionModel) {
		this.selectionModel = selectionModel;
		this.model = model;
		this.displaySettings = initDisplaySettings(model);
		model.addModelChangeListener(this);
		selectionModel.addSelectionChangeListener(this);
	}
	
	/*
	 * PUBLIC METHODS
	 */

	
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

	/**
	 * This needs to be overriden for concrete implementation to display selection.
	 */
	@Override
	public void selectionChanged(SelectionChangeEvent event) {
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
	
	@Override
	public Model getModel() {
		return model;
	}
	
	/**
	 * Provides default display settings.
	 * @param model  the model this view operate on. Needed for some display settings.
	 * @return 
	 */
	protected Map<String, Object> initDisplaySettings(Model model) {
		Map<String, Object> displaySettings = new HashMap<String, Object>(11);
		displaySettings.put(KEY_COLOR, DEFAULT_SPOT_COLOR);
		displaySettings.put(KEY_HIGHLIGHT_COLOR, DEFAULT_HIGHLIGHT_COLOR);
		displaySettings.put(KEY_SPOTS_VISIBLE, true);
		displaySettings.put(KEY_DISPLAY_SPOT_NAMES, false);
		displaySettings.put(KEY_SPOT_COLORING, new DummySpotColorGenerator());
		displaySettings.put(KEY_SPOT_RADIUS_RATIO, 1.0f);
		displaySettings.put(KEY_TRACKS_VISIBLE, true);
		displaySettings.put(KEY_TRACK_DISPLAY_MODE, DEFAULT_TRACK_DISPLAY_MODE);
		displaySettings.put(KEY_TRACK_DISPLAY_DEPTH, DEFAULT_TRACK_DISPLAY_DEPTH);
		displaySettings.put(KEY_TRACK_COLORING, new DummyTrackColorGenerator());
		displaySettings.put(KEY_COLORMAP, DEFAULT_COLOR_MAP);
		return displaySettings;
	}
	
}