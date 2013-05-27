package fiji.plugin.trackmate.visualization.threedviewer;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import javax.media.j3d.BadTransformException;
import javax.vecmath.Color3f;
import javax.vecmath.Point4d;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;

public class SpotDisplayer3D extends AbstractTrackMateModelView {

	public static final String NAME = "3D Viewer";
	public static final String INFO_TEXT = "<html>" +
			"This invokes a new 3D viewer (over time) window, which receive a <br> " +
			"8-bit copy of the image data. Spots and tracks are rendered in 3D. <br>" +
			"All the spots 3D shapes are calculated during the rendering step, which <br>" +
			"can take long." +
			"<p>" +
			"This displayer does not allow manual editing of spots. Use it only for <br>" +
			"for very specific cases where you need to have a good 3D image to judge <br>" +
			"the quality of detection and tracking. If you don't, use the hyperstack <br>" +
			"displayer; you can generate a 3D viewer at the last step of tracking that will <br>" +
			"be in sync with the hyperstack displayer. " +
			"</html>";
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

	public SpotDisplayer3D(TrackMateModel model, final SelectionModel selectionModel, Image3DUniverse universe) {
		super(model, selectionModel);
		this.universe = universe;
		setModel(model);
	}

	/*
	 * OVERRIDDEN METHODS
	 */


	@Override
	public void modelChanged(ModelChangeEvent event) {
		if (DEBUG) {
			System.out.println("[SpotDisplayer3D: modelChanged() called with event ID: "+event.getEventID());
		}
		switch (event.getEventID()) {
		case ModelChangeEvent.SPOTS_COMPUTED: 
			spotContent = makeSpotContent();
			universe.removeContent(SPOT_CONTENT_NAME);
			universe.addContent(spotContent);
			break;
		case ModelChangeEvent.SPOTS_FILTERED:
			for (int frame : blobs.keySet()) {
				SpotGroupNode<Spot> frameBlobs = blobs.get(frame);
				for (Iterator<Spot> it = model.getSpots().iterator(frame, false); it.hasNext();) {
					Spot spot = it.next();
					boolean visible = spot.getFeature(SpotCollection.VISIBLITY).compareTo(SpotCollection.ZERO) > 0;
					frameBlobs.setVisible(spot, visible);
				}
			}
			break;
		case ModelChangeEvent.TRACKS_COMPUTED: 
			trackContent = makeTrackContent();
			universe.removeContent(TRACK_CONTENT_NAME);
			universe.addContent(trackContent);
			break;
		case ModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
			updateTrackColors();
			trackNode.setTrackVisible(model.getTrackModel().getFilteredTrackIDs());
			break;

		}
	}

	@Override
	public void selectionChanged(SelectionChangeEvent event) {
		// Highlight
		highlightEdges(selectionModel.getEdgeSelection());
		highlightSpots(selectionModel.getSpotSelection());
		// Center on last spot
		super.selectionChanged(event);
	}



	@Override
	public void centerViewOn(Spot spot) {
		int frame = spot.getFeature(Spot.FRAME).intValue();
		universe.showTimepoint(frame);
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
			updateTrackColors();
			trackNode.refresh();
			universe.updateStartAndEndTime(blobs.firstKey(), blobs.lastKey());
			universe.updateTimelineGUI();
		}
	}

	@Override
	public void setDisplaySettings(final String key, final Object value) {
		super.setDisplaySettings(key, value);
		// Treat change of radius
		if (key == KEY_SPOT_RADIUS_RATIO) {
			updateRadiuses();
		} else if (key == KEY_SPOT_COLORING) {
			updateSpotColors();
		} else if (key == KEY_TRACK_COLORING) {
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

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

	@Override
	public String toString() {
		return NAME;
	}

	@Override
	public String getKey() {
		return NAME;
	}

	/*
	 * PRIVATE METHODS
	 */

	private void setModel(TrackMateModel model) {
		if (model.getSpots() != null) {
			spotContent = makeSpotContent();
			universe.removeContent(SPOT_CONTENT_NAME);
			universe.addContentLater(spotContent);
		}
		if (model.getTrackModel().getNFilteredTracks() > 0) {
			trackContent = makeTrackContent();
			universe.removeContent(TRACK_CONTENT_NAME);
			universe.addContentLater(trackContent);
		}
	}

	private Content makeTrackContent() {
		// Prepare tracks instant
		trackNode = new TrackDisplayNode(model);
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
		TreeMap<Integer, ContentInstant> contentAllFrames = new TreeMap<Integer, ContentInstant>();
		final float radiusRatio = (Float) displaySettings.get(KEY_SPOT_RADIUS_RATIO);
		final Color color = (Color) displaySettings.get(KEY_COLOR);
		SpotCollection spots = model.getSpots();

		for (int frame : spots.keySet()) {

			if (spots.getNSpots(frame, false) == 0) {
				continue; // Do not create content for empty frames
			}


			HashMap<Spot, Point4d> centers = new HashMap<Spot, Point4d>(spots.getNSpots(frame, false));
			double[] coords = new double[3];

			for (Iterator<Spot> it = spots.iterator(frame, false); it.hasNext();) {
				Spot spot = it.next();
				TMUtils.localize(spot, coords);
				Double radius = spot.getFeature(Spot.RADIUS);
				double[] pos = new double[] {coords[0], coords[1], coords[2], radius*radiusRatio};
				centers.put(spot, new Point4d(pos));
			}
			SpotGroupNode<Spot> blobGroup = new SpotGroupNode<Spot>(centers, new Color3f(color));
			ContentInstant contentThisFrame = new ContentInstant("Spots_frame_"+frame);

			try {
				contentThisFrame.display(blobGroup);
			} catch (BadTransformException bte) {
				System.err.println("Bad content for frame " + frame + ". Generated an exception:\n" 
						+ bte.getLocalizedMessage() 
						+ "\nContent was:\n" 
						+ blobGroup.toString());
			}

			// Set visibility:
			if (spots.getNSpots(frame, true) > 0) {
				blobGroup.setVisible(spots.iterable(frame, true));
			}

			contentAllFrames.put(frame, contentThisFrame);
			blobs.put(frame, blobGroup);
		}

		Content blobContent = new Content(SPOT_CONTENT_NAME, contentAllFrames);
		blobContent.showCoordinateSystem(false);
		return blobContent;
	}


	private void updateRadiuses() {
		final float radiusRatio = (Float) displaySettings.get(KEY_SPOT_RADIUS_RATIO);

		for (int frame : blobs.keySet()) {
			SpotGroupNode<Spot> spotGroup = blobs.get(frame);

			for (Iterator<Spot> iterator = model.getSpots().iterator(frame, false); iterator.hasNext();) {
				Spot spot = iterator.next();
				spotGroup.setRadius(spot, radiusRatio*spot.getFeature(Spot.RADIUS));
			}
		}
	}

	private void updateSpotColors() {
		@SuppressWarnings("unchecked")
		final FeatureColorGenerator<Spot> spotColorGenerator = (FeatureColorGenerator<Spot>) displaySettings.get(KEY_SPOT_COLORING);

		for (int frame : blobs.keySet()) {
			SpotGroupNode<Spot> spotGroup = blobs.get(frame);
			for (Iterator<Spot> iterator = model.getSpots().iterator(frame, false); iterator.hasNext();) {
				Spot spot = iterator.next();
				spotGroup.setColor(spot, new Color3f(spotColorGenerator.color(spot)));
			}
		}
	}

	private void updateTrackColors() {
		final TrackColorGenerator colorGenerator = (TrackColorGenerator) displaySettings.get(KEY_TRACK_COLORING);

		for(Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {
			colorGenerator.setCurrentTrackID(trackID);
			for (DefaultWeightedEdge edge : model.getTrackModel().getTrackEdges(trackID)) {
				Color color =  colorGenerator.color(edge);
				trackNode.setColor(edge, color);
			}
		}
	}

	private void highlightSpots(Collection<Spot> spots) {
		// Restore previous display settings for previously highlighted spot
		if (null != previousSpotHighlight)
			for (Spot spot : previousSpotHighlight)
				blobs.get(previousFrameHighlight.get(spot)).setColor(spot, previousColorHighlight.get(spot));
		previousSpotHighlight = new ArrayList<Spot>(spots.size());
		previousColorHighlight = new HashMap<Spot, Color3f>(spots.size());
		previousFrameHighlight = new HashMap<Spot, Integer>(spots.size());

		Color3f highlightColor = new Color3f((Color) displaySettings.get(KEY_HIGHLIGHT_COLOR));
		for (Spot spot : spots) {
			int frame = spot.getFeature(Spot.FRAME).intValue();
			// Store current settings
			previousSpotHighlight.add(spot);
			previousColorHighlight.put(spot, blobs.get(frame).getColor3f(spot));
			previousFrameHighlight.put(spot, frame);

			// Update target spot display
			blobs.get(frame).setColor(spot, highlightColor);
		}
	}

	private void highlightEdges(Collection<DefaultWeightedEdge> edges) {
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
}