package fiji.plugin.trackmate.visualization.threedviewer;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.vecmath.Color3f;
import javax.vecmath.Point4f;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class SpotDisplayer3D extends TrackMateModelView {
	
	public static final int DEFAULT_RESAMPLING_FACTOR = 4;
	public static final int DEFAULT_THRESHOLD = 50;

	private static final Color3f HIGHLIGHT_COLOR3F = new Color3f(HIGHLIGHT_COLOR);
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
	
	public SpotDisplayer3D(Image3DUniverse universe) {
		this.universe = universe;
	}
	
	/*
	 * OVERRIDDEN METHODS
	 */


	@Override
	public void setSpotNameVisible(boolean spotNameVisible) {
		// TODO 
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
		
		SpotCollection sc = spotsToShow.subset(spots);
		List<Spot> st;
		for(int frame : sc.keySet()) {
			st = sc.get(frame);
			for(Spot spot : st) {
				// Store current settings
				previousSpotHighlight.add(spot);
				previousColorHighlight.put(spot, blobs.get(frame).getColor3f(spot));
				previousFrameHighlight.put(spot, frame);

				// Update target spot display
				blobs.get(frame).setColor(spot,HIGHLIGHT_COLOR3F);
			}
		}
	}
	
	@Override
	public void centerViewOn(Spot spot) {
		int frame = - 1;
		for(int i : spotsToShow.keySet()) {
			List<Spot> spotThisFrame = spotsToShow.get(i);
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
	public void highlightEdges(Set<DefaultWeightedEdge> edges) {
		// Restore previous display settings for previously highlighted edges
		if (null != previousEdgeHighlight)
			for(DefaultWeightedEdge edge : previousEdgeHighlight.keySet())
					trackNode.setColor(edge, previousEdgeHighlight.get(edge));
		
		// Store current color settings
		previousEdgeHighlight = new HashMap<DefaultWeightedEdge, Color>();
		for(DefaultWeightedEdge edge :edges)
			previousEdgeHighlight.put(edge, trackNode.getColor(edge));
		
		// Change edge color
		for(DefaultWeightedEdge edge :edges)
			trackNode.setColor(edge, HIGHLIGHT_COLOR);
	}
	
	@Override
	public void setTrackVisible(boolean displayTrackSelected) {
		trackContent.setVisible(displayTrackSelected);
	}
	
	@Override
	public void setSpotVisible(boolean displaySpotSelected) {
		spotContent.setVisible(displaySpotSelected);
	}
	
	@Override
	public void setRadiusDisplayRatio(float ratio) {
		super.setRadiusDisplayRatio(ratio);
		List<Spot> spotsThisFrame; 
		SpotGroupNode<Spot> spotGroup;
		for(int key : blobs.keySet()) {
			spotsThisFrame = spots.get(key);
			spotGroup = blobs.get(key);
			for(Spot spot : spotsThisFrame)
				spotGroup.setRadius(spot, radiusRatio*spot.getFeature(Feature.RADIUS));
		}
	}
	
	@Override
	public void setDisplayTrackMode(TrackDisplayMode mode, int displayDepth) {
		super.setDisplayTrackMode(mode, displayDepth);
		if (null == trackContent) 
			return;
		trackNode.setDisplayTrackMode(mode, displayDepth);	
	}
	
	public void refresh() { 
		for(int key : spotsToShow.keySet())
			blobs.get(key).setVisible(spotsToShow.get(key)); // NPE if a spot from #spotsToShow does not belong to #spots 
	}
	
	@Override
	public void setTrackGraph(SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph) {
		super.setTrackGraph(trackGraph);
		if (universe.contains(TRACK_CONTENT_NAME)) {
			universe.removeContent(TRACK_CONTENT_NAME);
			universe.removeTimelapseListener(trackNode);
		}
		trackContent = makeTrackContent();
		universe.addTimelapseListener(trackNode);
		try {
			trackContent = universe.addContentLater(trackContent).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void setSpots(SpotCollection spots) {
		super.setSpots(spots);
		if (universe.contains(SPOT_CONTENT_NAME))
			universe.removeContent(SPOT_CONTENT_NAME);
		spotContent = makeSpotContent();
		try {
			spotContent = universe.addContentLater(spotContent).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	};
	

	@Override
	public void render()  {
		// do nothing, since this implementation is given a universe which must
		// be correctly instantiated with the image content.
	}
	
	@Override
	public void setColorByFeature(final Feature feature) {
		if (null == feature) {
			for(int key : blobs.keySet())
				blobs.get(key).setColor(new Color3f(color));
		} else {
			// Get min & max
			float min = Float.POSITIVE_INFINITY;
			float max = Float.NEGATIVE_INFINITY;
			Float val;
			for (int key : spots.keySet()) {
				for (Spot spot : spots.get(key)) {
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
			for (int key : blobs.keySet()) {
				spotThisFrame = spots.get(key);
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
		for(Set<Spot> track : tracks) {
			value = (float) index / tracks.size();
			colors.put(track, colorMap.getPaint(value));
			index++;
		}
		
		// Prepare tracks instant
		trackNode = new TrackDisplayNode(trackGraph, spots, tracks, colors);
		
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
		
		for(Integer i : spots.keySet()) {
			spotsThisFrame = spots.get(i);
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


}
