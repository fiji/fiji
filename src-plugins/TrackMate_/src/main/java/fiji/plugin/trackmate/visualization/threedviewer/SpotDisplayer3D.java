package fiji.plugin.trackmate.visualization.threedviewer;

import ij.ImagePlus;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.media.j3d.BadTransformException;
import javax.vecmath.Color3f;
import javax.vecmath.Point4d;

import net.imglib2.exception.ImgLibException;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
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
	private Settings settings;
	/**  the flag specifying whether to render image data or not. By default, it is true. */
	private boolean doRenderImage = true;

	public SpotDisplayer3D(TrackMateModel model) {
		super(model);
		universe = new Image3DUniverse();
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
			for(int key : model.getFilteredSpots().keySet())
				blobs.get(key).setVisible(model.getFilteredSpots().get(key));
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
		highlightEdges(model.getSelectionModel().getEdgeSelection());
		highlightSpots(model.getSelectionModel().getSpotSelection());
		// Center on last spot
		super.selectionChanged(event);
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
		}

		universe.show();
		if (doRenderImage && null != settings.imp) {
			//			if (!settings.imp.isVisible())
			//				settings.imp.show();
			ImagePlus[] images;
			try {
				images = TMUtils.makeImageForViewer(settings);
				final Content imageContent = ContentCreator.createContent(
						settings.imp.getShortTitle(), 
						images, 
						Content.VOLUME, 
						SpotDisplayer3D.DEFAULT_RESAMPLING_FACTOR, 
						0,
						null, 
						SpotDisplayer3D.DEFAULT_THRESHOLD, 
						new boolean[] {true, true, true});
				universe.addContentLater(imageContent);	
			} catch (ImgLibException e) {
				e.printStackTrace();
			}
		} else {
			universe.updateStartAndEndTime(blobs.firstKey(), blobs.lastKey());
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

	/*
	 * PUBLIC SPECIFIC METHODS
	 */

	/**
	 * Set a flag that specifies whether the next call to {@link #render()} will cause
	 * image data to be imported and displayer in this viewer   
	 * @param doRenderImage  the flag specifying whether to render image data or not. By default, it is true.
	 */
	public void setRenderImageData(boolean doRenderImage) {
		this.doRenderImage = doRenderImage;
	}


	/*
	 * PRIVATE METHODS
	 */
	
	private void setModel(TrackMateModel model) {
		this.settings = model.getSettings();
		if (model.getSpots() != null) {
			spotContent = makeSpotContent();
			universe.removeContent(SPOT_CONTENT_NAME);
			universe.addContent(spotContent);
		}
		if (model.getTrackModel().getNFilteredTracks() > 0) {
			trackContent = makeTrackContent();
			universe.removeContent(TRACK_CONTENT_NAME);
			universe.addContent(trackContent);
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

		for (Integer frame : model.getSpots().keySet()) {
			
			List<Spot> spotsThisFrame = model.getSpots().get(frame);
			if (spotsThisFrame.isEmpty()) {
				continue; // Do not create content for empty frames
			}
			
			
			HashMap<Spot, Point4d> centers = new HashMap<Spot, Point4d>(spotsThisFrame.size());
			double[] pos;
			double radius;
			double[] coords = new double[3];
			for(Spot spot : spotsThisFrame) {
				TMUtils.localize(spot, coords);
				radius = spot.getFeature(Spot.RADIUS);
				pos = new double[] {coords[0], coords[1], coords[2], radius*radiusRatio};
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
			List<Spot> visibleSpots = model.getFilteredSpots().get(frame);
			if (visibleSpots != null) {
				blobGroup.setVisible(visibleSpots);
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
		List<Spot> spotsThisFrame; 
		SpotGroupNode<Spot> spotGroup;
		for(int key : blobs.keySet()) {
			spotsThisFrame = model.getSpots().get(key);
			spotGroup = blobs.get(key);
			for(Spot spot : spotsThisFrame)
				spotGroup.setRadius(spot, radiusRatio*spot.getFeature(Spot.RADIUS));
		}
	}

	private void updateSpotColors() {
		final Color color = (Color) displaySettings.get(KEY_COLOR);
		final String feature = (String) displaySettings.get(KEY_SPOT_COLOR_FEATURE);

		if (null == feature) {
			for(int key : blobs.keySet())
				blobs.get(key).setColor(new Color3f(color));
		} else {
			// Get min & max
			double min = Float.POSITIVE_INFINITY;
			double max = Float.NEGATIVE_INFINITY;
			Double val;
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