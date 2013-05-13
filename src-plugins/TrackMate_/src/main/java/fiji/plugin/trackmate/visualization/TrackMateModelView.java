package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.Map;

import org.jfree.chart.renderer.InterpolatePaintScale;

import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public interface TrackMateModelView extends InfoTextable {



	/*
	 * KEY-VALUE CONSTANTS FOR LOOK & FILL CUSTOMIZATION
	 */

	
	/*
	 * KEYS
	 */

	/**
	 * Defines the key for the main color. Accepted values are color.
	 */
	public static final String KEY_COLOR = "Color";

	/**
	 * Defines the key for the highlight color, used to paint selection. Accepted values are color.
	 */
	public static final String KEY_HIGHLIGHT_COLOR = "HighlightColor";
	
	/**
	 * Defines the key for the track display mode. Possible values are
	 * {@link #TRACK_DISPLAY_MODE_WHOLE}, {@link #TRACK_DISPLAY_MODE_LOCAL},
	 * {@link #TRACK_DISPLAY_MODE_LOCAL_BACKWARD}, {@value #TRACK_DISPLAY_MODE_LOCAL_FORWARD}.
	 */
	public static final String KEY_TRACK_DISPLAY_MODE = "TrackDisplaymode";

	/**
	 * Defines the key for the track display depth. Values are integer, and 
	 * they defines how many frames away the track can be seen from the current
	 * time-point.
	 */
	public static final String KEY_TRACK_DISPLAY_DEPTH = "TrackDisplayDepth";

	/**
	 * Defines the key for the track visibility. Values are boolean. If <code>false</code>,
	 * tracks are not visible.
	 */
	public static final String KEY_TRACKS_VISIBLE = "TracksVisible";
	
	/**
	 * Defines the key for the track coloring method. Values are concrete implementations
	 * of {@link TrackColorGenerator}.
	 */
	public static final String KEY_TRACK_COLORING = "TrackColoring";

	/**
	 * Defines the key for the spot visibility. Values are boolean. If <code>false</code>,
	 * spots are not visible.
	 * */
	public static final String KEY_SPOTS_VISIBLE = "SpotsVisible";

	/**
	 * Defines the key for the spot name display. Values are boolean. If <code>false</code>,
	 * spot names are not visible.
	 * */
	public static final String KEY_DISPLAY_SPOT_NAMES = "DisplaySpotNames";

	/**
	 * Defines the key for the spot radius ratio. Value should be a positive float number.
	 * Spots will be rendered with a radius equals to their actual radius multiplied
	 * by this ratio.
	 */
	public static final String KEY_SPOT_RADIUS_RATIO = "SpotRadiusRatio";

	/**
	 * Defines the key for the feature that determines the spot color. Values can
	 * be any {@link SpotFeature} or <code>null</code>. In the later case, the default
	 * color #DEFAULT_COLOR is used for all spots. Otherwise, each spot color 
	 * is set according to the selected feature value.
	 */
	public static final String KEY_SPOT_COLOR_FEATURE = "SpotColorFeature";

	/**
	 * Defines the key for the feature that determines the track color. Values can
	 * be any {@link TrackFeature} or <code>null</code>. In the later case, the default
	 * color #DEFAULT_COLOR is used for all spots. Otherwise, each track color 
	 * is set according to the selected feature value.
	 */
//	public static final String KEY_TRACK_COLOR_FEATURE = "TrackColorFeature";

	/**
	 * Defines the key for the color map to use for painting overlay. Acceptable
	 * values are {@link InterpolatePaintScale}s, the default is {@link InterpolatePaintScale#Jet}.
	 */
	public static final String KEY_COLORMAP = "ColorMap";
	
	
	/*
	 * VALUES
	 */

	/**
	 * Track display mode where the whole tracks are drawn, ignoring
	 * the value of {@link #KEY_TRACK_DISPLAY_DEPTH}.
	 */
	public static final int TRACK_DISPLAY_MODE_WHOLE = 0;

	/**
	 * Track display mode where the only part of the tracks close to the current
	 * time-point are drawn backward and forward. Edges away from current time point
	 * are faded in the background. How much can be seen is 
	 * defined by the value of {@link #KEY_TRACK_DISPLAY_DEPTH}.
	 */
	public static final int TRACK_DISPLAY_MODE_LOCAL = 1;

	/**
	 * Track display mode where the only part of the tracks close to the current
	 * time-point are drawn, backward only. How much can be seen is 
	 * defined by the value of {@link #KEY_TRACK_DISPLAY_DEPTH}.
	 */
	public static final int TRACK_DISPLAY_MODE_LOCAL_BACKWARD = 2;

	/**
	 * Track display mode where the only part of the tracks close to the current
	 * time-point are drawn, forward only. How much can be seen is 
	 * defined by the value of {@link #KEY_TRACK_DISPLAY_DEPTH}.
	 */
	public static final int TRACK_DISPLAY_MODE_LOCAL_FORWARD = 3;

	/**
	 * Track display mode similar to {@link #TRACK_DISPLAY_MODE_LOCAL}, except
	 * that for the sake of speed, edges are not faded.
	 */
	public static final int TRACK_DISPLAY_MODE_LOCAL_QUICK = 4;

	/**
	 * Track display mode similar to {@link #TRACK_DISPLAY_MODE_LOCAL_BACKWARD}, except
	 * that for the sake of speed, edges are not faded.
	 */
	public static final int TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK = 5;

	/**
	 * Track display mode similar to {@link #TRACK_DISPLAY_MODE_LOCAL_FORWARD}, except
	 * that for the sake of speed, edges are not faded.
	 */
	public static final int TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK = 6;

	/*
	 * DESCRIPTIONS
	 */

	/** 
	 * String that describe the corresponding track display mode.
	 */
	public static final String[] TRACK_DISPLAY_MODE_DESCRIPTION = new String[] {
		"Show all entire tracks",
		"Show local tracks",
		"Show local tracks, backward",
		"Show local tracks, forward",
		"Local tracks (fast)",
		"Local tracks, backward (fast)",
		"Local tracks, forward (fast)"
	};
	
	/*
	 * DEFAULTS
	 */

	/** 
	 * The default color.
	 */
	public static final Color DEFAULT_COLOR = new Color(1f, 0, 1f);

	/** 
	 * The default color for highlighting.
	 */
	public static final Color DEFAULT_HIGHLIGHT_COLOR = new Color(0, 1f, 0);

	/**
	 * The default track display mode.
	 */
	public static final int DEFAULT_TRACK_DISPLAY_MODE = TRACK_DISPLAY_MODE_WHOLE;
	
	/**
	 * The default track display mode
	 */
	public static final int DEFAULT_TRACK_DISPLAY_DEPTH = 10;
	
	/** 
	 * The default color map. 
	 */
	public static final InterpolatePaintScale DEFAULT_COLOR_MAP = InterpolatePaintScale.Jet;

	/*
	 * INTERFACE METHODS
	 */

	/**
	 * Initialize this displayer and render it according to its concrete implementation, 
	 * target model.
	 * @see #setModel(TrackMateModel)
	 */
	public void render();

	/**
	 * Refresh the displayer display with current model. If the underlying model was modified,
	 * or the display settings were changed, calling this method should be enough to update 
	 * the display with changes.
	 * @see #setDisplaySettings(String, Object)
	 */
	public void refresh();

	/**
	 * Remove any overlay (for spots or tracks) from this displayer.
	 */
	public void clear();
	
	/**
	 * Center the view on the given spot.
	 */
	public void centerViewOn(final Spot spot);

	/**
	 * @return the current display settings map.
	 */
	public Map<String, Object> getDisplaySettings();

	/** 
	 * Set a display parameter.
	 * @param key  the key of the parameter to change.
	 * @param value  the value for the display parameter
	 */
	public void setDisplaySettings(final String key, final Object value);
	
	/**
	 * @return the value of a specific display parameter. 
	 */
	public Object getDisplaySettings(final String key);
	
	/**
	 * @return the model displayed in this view.
	 */
	public TrackMateModel getModel();
}
