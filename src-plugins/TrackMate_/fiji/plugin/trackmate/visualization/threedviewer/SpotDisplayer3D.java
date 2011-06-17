package fiji.plugin.trackmate.visualization.threedviewer;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;
import ij3d.UniverseListener;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point4f;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;

public class SpotDisplayer3D extends AbstractTrackMateModelView {
	
	public static final int DEFAULT_RESAMPLING_FACTOR = 4;
	public static final int DEFAULT_THRESHOLD = 50;

	private static final String TRACK_CONTENT_NAME = "Tracks";
	private static final String SPOT_CONTENT_NAME = "Spots";
	
	private TreeMap<Integer, SpotGroupNode<Spot>> blobs;	
	private TrackDisplayNode trackNode;
	private Content spotContent;
	private Content trackContent;
	private final Image3DUniverse universe;
	// For highlighting
	private ArrayList<Spot> previousSpotHighlight;
	private HashMap<Spot, Color3f> previousColorHighlight;
	private HashMap<Spot, Integer> previousFrameHighlight;
	private HashMap<DefaultWeightedEdge, Color> previousEdgeHighlight;
	private UniverseListener unregisterListener = new UniverseListener() {			
		@Override
		public void universeClosed() {
			SpotDisplayer3D.this.model.removeTrackMateModelChangeListener(SpotDisplayer3D.this);
			SpotDisplayer3D.this.model.removeTrackMateSelectionChangeListener(SpotDisplayer3D.this);
		}
		@Override
		public void transformationUpdated(View view) {}
		@Override
		public void transformationStarted(View view) {}
		@Override
		public void transformationFinished(View view) {}
		@Override
		public void contentSelected(Content c) {}
		@Override
		public void contentRemoved(Content c) {}
		@Override
		public void contentChanged(Content c) {}
		@Override
		public void contentAdded(Content c) {}
		@Override
		public void canvasResized() {}
	};
	
	public SpotDisplayer3D(Image3DUniverse universe, TrackMateModel model) {
		this.universe = universe;
		setModel(model);
		spotContent = makeSpotContent();
		trackContent = makeTrackContent();
		// Add a listener to unregister this instance from the model listener list when closing
		universe.addUniverseListener(unregisterListener);
	}
	
	/*
	 * OVERRIDDEN METHODS
	 */

	
	@Override
	public void highlightSpots(Collection<Spot> spots) {
		// Restore previous display settings for previously highlighted spot
		if (null != previousSpotHighlight)
			for (Spot spot : previousSpotHighlight)
				blobs.get(previousFrameHighlight.get(spot)).setColor(spot, previousColorHighlight.get(spot));
		previousSpotHighlight = new ArrayList<Spot>(spots.size());
		previousColorHighlight = new HashMap<Spot, Color3f>(spots.size());
		previousFrameHighlight = new HashMap<Spot, Integer>(spots.size());
		
		SpotCollection sc = model.getFilteredSpots().subset(spots);
		Color3f highlightColor = new Color3f((Color) displaySettings.get(KEY_HIGHLIGHT_COLOR));
		List<Spot> st;
		for(int frame : sc.keySet()) {
			st = sc.get(frame);
			for(Spot spot : st) {
				// Store current settings
				previousSpotHighlight.add(spot);
				previousColorHighlight.put(spot, blobs.get(frame).getColor3f(spot));
				previousFrameHighlight.put(spot, frame);

				// Update target spot display
				blobs.get(frame).setColor(spot, highlightColor);
			}
		}
	}
	
	@Override
	public void centerViewOn(Spot spot) {
		int frame = - 1;
		for(int i : model.getFilteredSpots().keySet()) {
			List<Spot> spotThisFrame = model.getFilteredSpots().get(i);
			if (spotThisFrame.contains(spot)) {
				frame = i;
				break;
			}
		}
		if (frame == -1)
			return;
		universe.showTimepoint(frame);
	}

	@Override
	public void highlightEdges(Collection<DefaultWeightedEdge> edges) {
		// Restore previous display settings for previously highlighted edges
		if (null != previousEdgeHighlight)
			for(DefaultWeightedEdge edge : previousEdgeHighlight.keySet())
					trackNode.setColor(edge, previousEdgeHighlight.get(edge));
		
		// Store current color settings
		previousEdgeHighlight = new HashMap<DefaultWeightedEdge, Color>();
		for(DefaultWeightedEdge edge :edges)
			previousEdgeHighlight.put(edge, trackNode.getColor(edge));
		
		// Change edge color
		Color highlightColor = (Color) displaySettings.get(KEY_HIGHLIGHT_COLOR);
		for(DefaultWeightedEdge edge :edges)
			trackNode.setColor(edge, highlightColor);
	}
	
	
	@Override
	public void refresh() { 
//		for(int key : model.getFilteredSpots().keySet())
//			blobs.get(key).setVisible(model.getFilteredSpots().get(key)); // NPE if a spot from #spotsToShow does not belong to #spots 
	}
	
	@Override
	public void render()  {
		universe.addContent(spotContent);
		universe.addContent(trackContent);
	}

	@Override
	public void setDisplaySettings(final String key, final Object value) {
		super.setDisplaySettings(key, value);
		// Treat change of radius
		if (key == KEY_SPOT_RADIUS_RATIO) {
			updateRadiuses();
		} else if (key == KEY_SPOT_COLOR_FEATURE) {
			updateColors();
		}
	}
	
	@Override
	public void clear() {
		universe.removeContent(SPOT_CONTENT_NAME);
		universe.removeContent(TRACK_CONTENT_NAME);
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private Content makeTrackContent() {
		// Prepare track color
		HashMap<Set<Spot>, Color> colors = new HashMap<Set<Spot>, Color>();
		float value;
		int index = 0;
		List<Set<Spot>> tracks = new ConnectivityInspector<Spot, DefaultWeightedEdge>(model.getTrackGraph()).connectedSets();
		final InterpolatePaintScale colorMap = (InterpolatePaintScale) displaySettings.get(KEY_COLORMAP);
		for(Set<Spot> track : tracks ) {
			value = (float) index / tracks.size();
			colors.put(track, colorMap.getPaint(value));
			index++;
		}
		
		// Prepare tracks instant
		trackNode = new TrackDisplayNode(model.getTrackGraph(), model.getFilteredSpots(), tracks, colors);
		
		// Pass tracks instant to all instants
		TreeMap<Integer, ContentInstant> instants = new TreeMap<Integer,ContentInstant>();
		ContentInstant trackCI = new ContentInstant("Tracks_all_frames");
		trackCI.display(trackNode);
		instants.put(0, trackCI);
		Content tc = new Content(TRACK_CONTENT_NAME, instants);
		tc.setShowAllTimepoints(true);
		tc.showCoordinateSystem(false);
		return tc;
	}

	
	private Content makeSpotContent() {
		
		blobs = new TreeMap<Integer, SpotGroupNode<Spot>>();
		List<Spot> spotsThisFrame; 
		SpotGroupNode<Spot> blobGroup;
		ContentInstant contentThisFrame;
		TreeMap<Integer, ContentInstant> contentAllFrames = new TreeMap<Integer, ContentInstant>();
		final float radiusRatio = (Float) displaySettings.get(KEY_SPOT_RADIUS_RATIO);
		final Color color = (Color) displaySettings.get(KEY_COLOR);
		
		for(Integer i : model.getSpots().keySet()) {
			spotsThisFrame = model.getSpots().get(i);
			HashMap<Spot, Point4f> centers = new HashMap<Spot, Point4f>(spotsThisFrame.size());
			float[] pos;
			float radius;
			float[] coords = new float[3];
			for(Spot spot : spotsThisFrame) {
				spot.getPosition(coords);
				radius = spot.getFeature(Feature.RADIUS);
				pos = new float[] {coords[0], coords[1], coords[2], radius*radiusRatio};
				centers.put(spot, new Point4f(pos));
			}
			blobGroup = new SpotGroupNode<Spot>(centers, new Color3f(color));
			contentThisFrame = new ContentInstant("Spots_frame_"+i);
			contentThisFrame.display(blobGroup);
			
			contentAllFrames.put(i, contentThisFrame);
			blobs.put(i, blobGroup);
		}
		Content blobContent = new Content(SPOT_CONTENT_NAME, contentAllFrames);
		blobContent.showCoordinateSystem(false);
		return blobContent;
	}


	private void updateRadiuses() {
		final float radiusRatio = (Float) displaySettings.get(KEY_SPOT_RADIUS_RATIO);
		List<Spot> spotsThisFrame; 
		SpotGroupNode<Spot> spotGroup;
		for(int key : blobs.keySet()) {
			spotsThisFrame = model.getSpots().get(key);
			spotGroup = blobs.get(key);
			for(Spot spot : spotsThisFrame)
				spotGroup.setRadius(spot, radiusRatio*spot.getFeature(Feature.RADIUS));
		}
	}

	private void updateColors() {
		final Color color = (Color) displaySettings.get(KEY_COLOR);
		final Feature feature = (Feature) displaySettings.get(KEY_SPOT_COLOR_FEATURE);
		
		if (null == feature) {
			for(int key : blobs.keySet())
				blobs.get(key).setColor(new Color3f(color));
		} else {
			// Get min & max
			float min = Float.POSITIVE_INFINITY;
			float max = Float.NEGATIVE_INFINITY;
			Float val;
			for (int key : model.getSpots().keySet()) {
				for (Spot spot : model.getSpots().get(key)) {
					val = spot.getFeature(feature);
					if (null == val)
						continue;
					if (val > max) max = val;
					if (val < min) min = val;
				}
			}
			// Color using LUT
			List<Spot> spotThisFrame;
			SpotGroupNode<Spot> spotGroup;
			final InterpolatePaintScale colorMap = (InterpolatePaintScale) displaySettings.get(KEY_COLORMAP);
			for (int key : blobs.keySet()) {
				spotThisFrame = model.getSpots().get(key);
				spotGroup = blobs.get(key);
				for ( Spot spot : spotThisFrame) {
					val = spot.getFeature(feature);
					if (null == val) 
						spotGroup.setColor(spot, new Color3f(color));
					else
						spotGroup.setColor(spot, new Color3f(colorMap.getPaint((val-min)/(max-min))));
				}
			}
		}
	}
}
