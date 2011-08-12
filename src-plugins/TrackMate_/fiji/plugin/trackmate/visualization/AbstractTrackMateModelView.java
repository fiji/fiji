package fiji.plugin.trackmate.visualization;

import ij.ImagePlus;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeListener;
import fiji.plugin.trackmate.TrackMateSelectionChangeEvent;
import fiji.plugin.trackmate.TrackMateSelectionChangeListener;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;

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
	 * PRICATE CONSTRUCTOR
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

	/*
	 * ENUMS
	 */

	/**
	 * This enum stores the list of {@link AbstractTrackMateModelView} currently available.
	 */
	public static enum ViewType implements InfoTextable {
		THREEDVIEWER_DISPLAYER,
		HYPERSTACK_DISPLAYER;

		@Override
		public String toString() {
			switch(this) {
			case HYPERSTACK_DISPLAYER:
				return "HyperStack Displayer";
			case THREEDVIEWER_DISPLAYER:
				return "3D Viewer";
			}
			return null;
		}

		@Override
		public String getInfoText() {
			switch(this) {
			case HYPERSTACK_DISPLAYER:
				return "<html>" +
				"This displayer overlays the spots and tracks on the current<br>" +
				"ImageJ hyperstack window. <br>" +
				"<p> " +
				"This displayer allows manual editing of spots, thanks to the spot <br> " +
				"edit tool that appear in ImageJ toolbar." +
				"<p>" +
				"Double-clicking in a spot toggles the editing mode: The spot can <br> " +
				"be moved around in a XY plane by mouse dragging. To move it in Z <br>" +
				"or in time, simply change the current plane and time-point by <br>" +
				"using the hyperstack sliders. To change its radius, hold the " +
				"<tt>alt</tt> key down and rotate the mouse-wheel. Holding the " +
				"<tt>shift</tt> key on top changes it faster."+
				"</html>";
			case THREEDVIEWER_DISPLAYER:
				return "<html>" +
				"This invokes a new 3D viewer (over time) window, which receive a<br> " +
				"8-bit copy of the image data. Spots and tracks are rendered in 3D. " +
				"All the spots 3D shapes are calculated during the rendering step, which" +
				"can take long." +
				"<p>" +
				"This displayer does not allow manual editing of spots. Use it only for <br>" +
				"for very specific cases where you need to have a good 3D image to jusdge <br>" +
				"the quality of segmentation and tracking. If you don't, use the hyperstack <br>" +
				"displayer; you can generate a 3D viewer at the last step of tracking that will <br>" +
				"be in sync with the hyperstack displayer. " +
				"</html>"; 
			}
			return null;
		}

	}

	/*
	 * STATIC METHOD
	 */

	/**
	 * Instantiate and render the displayer specified by the given {@link ViewType}, using the data from
	 * the model given. This will render the chosen {@link AbstractTrackMateModelView} only with image data.
	 */
	public static TrackMateModelView instantiateView(final ViewType displayerType, final TrackMateModel model) {
		final AbstractTrackMateModelView disp;
		Settings settings = model.getSettings();
		switch (displayerType) {
		case THREEDVIEWER_DISPLAYER:
		{ 
			final Image3DUniverse universe = new Image3DUniverse();
			universe.show();
			if (null != settings.imp) {
				if (!settings.imp.isVisible())
					settings.imp.show();
				ImagePlus[] images = TMUtils.makeImageForViewer(settings);
				final Content imageContent = ContentCreator.createContent(
						settings.imp.getTitle(), 
						images, 
						Content.VOLUME, 
						SpotDisplayer3D.DEFAULT_RESAMPLING_FACTOR, 
						0,
						null, 
						SpotDisplayer3D.DEFAULT_THRESHOLD, 
						new boolean[] {true, true, true});
				universe.addContentLater(imageContent);	
			}
			disp = new SpotDisplayer3D(universe, model);
			disp.render();
			break;

		} 
		case HYPERSTACK_DISPLAYER:
		default:
		{
			disp = new HyperStackDisplayer(model);
			disp.render();
			break;
		}
		}
		return disp;
	}


}