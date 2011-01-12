package fiji.plugin.trackmate.visualization;

import ij.ImagePlus;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TMUtils;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

/**
 * The mother abstract class for spot displayers, that can overlay segmented spots and tracks on top
 * of the image data. 
 * <p>
 * Displayers must implements this abastract class. It offers on top some facilities to store common
 * fields, and can instantiate concrete implementation based on factory design.
 * <p>
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Jan 2011
 *
 */
public abstract class SpotDisplayer {

	
	/*
	 * ENUMS
	 */
	
	/**
	 * This enum stores the list of {@link SpotDisplayer} currently available.
	 */
	public static enum DisplayerType {
		STACK_DISPLAYER,		
		THREEDVIEWER_DISPLAYER,
		HYPERSTACK_DISPLAYER;
		
		public static DisplayerType[] get2DDisplayers() {
			return new DisplayerType[] { STACK_DISPLAYER };
		}

		public static DisplayerType[] get3DDisplayers() {
			return new DisplayerType[] { HYPERSTACK_DISPLAYER, THREEDVIEWER_DISPLAYER };
		}

	}

	/**
	 * This enum stores the different display omde for tracks. Note that it might be ignored 
	 * by some displayers.
	 */
	public enum TrackDisplayMode {
		ALL_WHOLE_TRACKS,
		LOCAL_WHOLE_TRACKS,
		LOCAL_BACKWARD_TRACKS,
		LOCAL_FORWARD_TRACKS;
		
		@Override
		public String toString() {
			switch(this) {
			case ALL_WHOLE_TRACKS:
				return "Show all entire tracks";
			case LOCAL_WHOLE_TRACKS:
				return "Show current tracks";
			case LOCAL_BACKWARD_TRACKS:
				return "Show current tracks, only backward";
			case LOCAL_FORWARD_TRACKS:
				return "Show current tracks, only forward";
			}
			return "Not implemented";
		}
		
	}
	
	/*
	 * FIELDS
	 */
	
	public static final TrackDisplayMode DEFAULT_TRACK_DISPLAY_MODE = TrackDisplayMode.ALL_WHOLE_TRACKS;
	public static final int DEFAULT_TRACK_DISPLAY_DEPTH 			= 10;
	
	/** The default display radius. */
	protected static final float DEFAULT_DISPLAY_RADIUS = 5;
	/** The default color. */
	protected static final Color DEFAULT_COLOR = new Color(1f, 0, 1f);
	/** The display radius. */
	protected float radius = DEFAULT_DISPLAY_RADIUS;
	/** The ratio setting the actual display size of the spots, with respect to the physical radius. */
	protected float radiusRatio = 1.0f;
	
	/** The colorMap. */
	protected InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;
	/** The default color to paint the spots and tracks. */ 
	protected Color color = DEFAULT_COLOR;
	
	/** The track collections emanating from tracking. They are represented as a graph of Spot,
	 * which must be the same objects as for {@link #spots}. */
	protected SimpleGraph<Spot, DefaultEdge> trackGraph;
	/** The individual tracks contained in the {@link #trackGraph}. */ 
	protected List<Set<Spot>> tracks;
	/** The track colors. */
	protected Map<Set<Spot>, Color> trackColors;
	/** The Spot lists emanating from segmentation, indexed by the frame index as Integer. */
	protected TreeMap<Integer, List<Spot>> spots = new TreeMap<Integer, List<Spot>>();
	/** The subset of Spots retained from {@link #spots} for displaying. */
	protected TreeMap<Integer, List<Spot>> spotsToShow = new TreeMap<Integer, List<Spot>>();

	/** The display track mode. */
	protected TrackDisplayMode trackDisplayMode = DEFAULT_TRACK_DISPLAY_MODE;
	/** The display depth: how many track segments will be shown on the track display. */
	protected int trackDisplayDepth = DEFAULT_TRACK_DISPLAY_DEPTH;

	

	/*
	 * STATIC METHOD
	 */
	
	/**
	 * Instantiate and render the displayer specified by the given {@link DisplayerType}, using the data from
	 * the model given. This will render the chosen {@link SpotDisplayer} only with image data.
	 */
	public static SpotDisplayer instantiateDisplayer(final DisplayerType displayerType, final TrackMateModelInterface model) {
		final SpotDisplayer disp;
		Settings settings = model.getSettings();
		switch (displayerType) {
		case THREEDVIEWER_DISPLAYER:
		{ 
			if (!settings.imp.isVisible())
				settings.imp.show();
			final Image3DUniverse universe = new Image3DUniverse();
			universe.show();
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
			// Render spots
			disp = new SpotDisplayer3D(universe, settings.segmenterSettings.expectedRadius);
			disp.setSpots(model.getSpots());
			new Thread("Displayer rendering thread") {
				public void run() {
					disp.render();
					universe.addContentLater(imageContent);					
				};
			}.start();
			break;

		} 
		case STACK_DISPLAYER:
		{
			disp = new SpotDisplayer2D(settings);
			disp.render();
			break;
		}
		case HYPERSTACK_DISPLAYER:
		default:
			{
				disp = new HyperStackDisplayer(settings);
				disp.render();
				break;
			}
		}
		return disp;
	}


	/*
	 * PUBLIC METHODS
	 */
	

	/**
	 * Set the display mode for tracks. The {@link #refresh()} method must be called to refresh
	 * the display.
	 */
	public void setDisplayTrackMode(TrackDisplayMode mode, int displayDepth) {
		this.trackDisplayMode = mode;
		this.trackDisplayDepth = displayDepth;
	}	
	
	/**
	 * Set the track to be displayed in this displayer.
	 */
	public void setTrackGraph(SimpleGraph<Spot, DefaultEdge> trackGraph) {
		this.trackGraph = trackGraph;
		this.tracks = new ConnectivityInspector<Spot, DefaultEdge>(trackGraph).connectedSets();
		this.trackColors = new HashMap<Set<Spot>, Color>(tracks.size());
		int counter = 0;
		int ntracks = tracks.size();
		for(Set<Spot> track : tracks) {
			trackColors.put(track, colorMap.getPaint((float) counter / (ntracks-1)));
			counter++;
		}
	}
	
	/**
	 * Set the spots that can be displayed by this displayer. Note that calling this method this 
	 * does not actually draw the spots. The spots to be displayed has to be specified 
	 * using {@link #setSpotsToShow(TreeMap)}, and must be a subset from the field passed to this method.
	 * @see #setSpotsToShow(TreeMap)  
	 */
	public void setSpots(TreeMap<Integer, List<Spot>> spots) {
		this.spots = spots;
	}
	
	/**
	 * Set what spots are to be displayed in this displayer. The list of spot given here must be a subset
	 * of the list passed to the {@link #setSpots(TreeMap)} method.
	 * @see #setSpots(TreeMap) 
	 */
	public void setSpotsToShow(TreeMap<Integer, List<Spot>> spotsToShow) {
		this.spotsToShow = spotsToShow;
	}

	/**
	 * Set up the ratio used to determine the actual display radius of spots. The spots on the image
	 * will have a radius given by <code> {@link SegmenterSettings#expectedRadius} * ratio </code>.
	 */
	public void setRadiusDisplayRatio(float ratio) {
		this.radiusRatio = ratio;
	}
	
	
	/*
	 * ABSTRACT METHODS
	 */
	
	/**
	 * Prepare this displayer and render it according to its concrete implementation. Must be called before
	 * adding spots or tracks for displaying. 
	 */
	public abstract void render();
	
	/**
	 * Color all displayed spots according to the feature given. 
	 * If feature is <code>null</code>, then the default color is 
	 * used. The {@link #refresh()} method must be called for display.
	 */
	public abstract void setColorByFeature(final Feature feature);
	
	
	public abstract void refresh();

	public abstract void setTrackVisible(boolean displayTrackSelected);

	public abstract void setSpotVisible(boolean displaySpotSelected);

	/**
	 * Remove any overlay (for spots or tracks) from this displayer.
	 */
	public abstract void clear();
	
}
