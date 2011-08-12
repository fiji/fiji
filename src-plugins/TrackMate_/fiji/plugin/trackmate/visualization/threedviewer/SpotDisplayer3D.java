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
import java.util.TreeMap;

import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point4f;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.TrackFeature;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class SpotDisplayer3D extends AbstractTrackMateModelView {

	public static final int DEFAULT_RESAMPLING_FACTOR = 4;
	public static final int DEFAULT_THRESHOLD = 50;

	private static final boolean DEBUG = false;
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
		// Add a listener to unregister this instance from the model listener list when closing
		universe.addUniverseListener(unregisterListener);
	}

	/*
	 * OVERRIDDEN METHODS
	 */

	@Override
	public void setModel(TrackMateModel model) {
		super.setModel(model);
		if (model.getSpots() != null) {
			spotContent = makeSpotContent();
			universe.removeContent(SPOT_CONTENT_NAME);
			universe.addContent(spotContent);
		}
		if (model.getNFilteredTracks() > 0) {
			trackContent = makeTrackContent();
			universe.removeContent(TRACK_CONTENT_NAME);
			universe.addContent(trackContent);
		}
	}

	@Override
	public void modelChanged(TrackMateModelChangeEvent event) {
		if (DEBUG) {
			System.out.println("[SpotDisplayer3D: modelChanged() called with event ID: "+event.getEventID());
		}
		switch (event.getEventID()) {
		case TrackMateModelChangeEvent.SPOTS_COMPUTED: 
			spotContent = makeSpotContent();
			universe.removeContent(SPOT_CONTENT_NAME);
			universe.addContent(spotContent);
			break;
		case TrackMateModelChangeEvent.SPOTS_FILTERED:
			for(int key : model.getFilteredSpots().keySet())
				blobs.get(key).setVisible(model.getFilteredSpots().get(key));
					break;
		case TrackMateModelChangeEvent.TRACKS_COMPUTED: 
			trackContent = makeTrackContent();
			universe.removeContent(TRACK_CONTENT_NAME);
			universe.addContent(trackContent);
			break;
		case TrackMateModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
			trackNode.computeTrackColors();
			trackNode.setTrackVisible(model.getVisibleTrackIndices());
			break;

		}
	}

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
		if (null != trackNode)
			trackNode.refresh();
	}

	@Override
	public void render()  {	
		if (DEBUG)
			System.out.println("[SpotDisplayer3D] Call to render().");
		updateRadiuses();
		updateSpotColors();
		spotContent.setVisible((Boolean) displaySettings.get(KEY_SPOTS_VISIBLE));
		if (null != trackContent) {
			trackContent.setVisible((Boolean) displaySettings.get(KEY_TRACKS_VISIBLE));
			trackNode.setTrackDisplayMode((Integer) displaySettings.get(KEY_TRACK_DISPLAY_MODE));
			trackNode.setTrackDisplayDepth((Integer) displaySettings.get(KEY_TRACK_DISPLAY_DEPTH));
			trackNode.refresh();
		}
	}

	@Override
	public void setDisplaySettings(final String key, final Object value) {
		super.setDisplaySettings(key, value);
		// Treat change of radius
		if (key == KEY_SPOT_RADIUS_RATIO) {
			updateRadiuses();
		} else if (key == KEY_SPOT_COLOR_FEATURE) {
			updateSpotColors();
		} else if (key == KEY_TRACK_COLOR_FEATURE) {
			updateTrackColors();
		} else if (key == KEY_DISPLAY_SPOT_NAMES) {
			for(int frame : blobs.keySet()) {
				blobs.get(frame).setShowLabels((Boolean) value);
			}
		} else if (key == KEY_SPOTS_VISIBLE) {
			spotContent.setVisible((Boolean) value);
		} else if (key == KEY_TRACKS_VISIBLE) { 
			trackContent.setVisible((Boolean) value);
		} else if (key == KEY_TRACK_DISPLAY_MODE) {
			trackNode.setTrackDisplayMode((Integer) value);
		} else if (key == KEY_TRACK_DISPLAY_DEPTH) {
			trackNode.setTrackDisplayDepth((Integer) value);
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
		// Prepare tracks instant
		trackNode = new TrackDisplayNode(model, displaySettings);
		universe.addTimelapseListener(trackNode);

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
				radius = spot.getFeature(SpotFeature.RADIUS);
				pos = new float[] {coords[0], coords[1], coords[2], radius*radiusRatio};
				centers.put(spot, new Point4f(pos));
			}
			blobGroup = new SpotGroupNode<Spot>(centers, new Color3f(color));
			contentThisFrame = new ContentInstant("Spots_frame_"+i);
			contentThisFrame.display(blobGroup);

			// Set visibility:
			List<Spot> visibleSpots = model.getFilteredSpots().get(i);
			if (visibleSpots != null) {
				blobGroup.setVisible(visibleSpots);
			}

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
				spotGroup.setRadius(spot, radiusRatio*spot.getFeature(SpotFeature.RADIUS));
		}
	}

	private void updateSpotColors() {
		final Color color = (Color) displaySettings.get(KEY_COLOR);
		final SpotFeature feature = (SpotFeature) displaySettings.get(KEY_SPOT_COLOR_FEATURE);

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

	private void updateTrackColors() {
		final TrackFeature feature = (TrackFeature) displaySettings.get(KEY_TRACK_COLOR_FEATURE);
		if (null == feature) {
			trackNode.computeTrackColors();

		} else {
			InterpolatePaintScale colorMap = (InterpolatePaintScale) displaySettings.get(TrackMateModelView.KEY_COLORMAP);
			// Get min & max
			double min = Float.POSITIVE_INFINITY;
			double max = Float.NEGATIVE_INFINITY;
			for (double val : model.getTrackFeatureValues().get(feature)) {
				if (val > max) max = val;
				if (val < min) min = val;
			}

			for(int i : model.getVisibleTrackIndices()) {
				double val = model.getTrackFeature(i, feature);
				Color color =  colorMap.getPaint((float) (val-min) / (max-min));
				trackNode.setColor(model.getTrackSpots(i), color);
			}

		}
	}
}