package fiji.plugin.trackmate.visualization;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.vecmath.Color3f;
import javax.vecmath.Point4f;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.test.TrackDisplayNode;

public class SpotDisplayer3D extends SpotDisplayer {
	
	private TreeMap<Integer, SpotGroupNode<Spot>> blobs;	
	private Content spotContent;
	private final Image3DUniverse universe;
	
	
	public SpotDisplayer3D(Image3DUniverse universe, final float radius) {
		this.radius = radius;
		this.universe = universe;
	}
	
	public SpotDisplayer3D(Image3DUniverse universe) {
		this(universe, DEFAULT_DISPLAY_RADIUS);
	}

	
	/*
	 * OVERRIDDEN METHODS
	 */
	
	public void setSpots(java.util.TreeMap<Integer,java.util.List<Spot>> spots) {
		super.setSpots(spots);
		spotContent = makeContent();
	};
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void refresh() { 
		for(int key : spotsToShow.keySet())
			blobs.get(key).setVisible(spotsToShow.get(key)); // NPE if a spot from #spotsToShow does not belong to #spots 
	}
	
	@Override
	public void setTrackGraph(SimpleGraph<Spot, DefaultEdge> trackGraph) {
		super.setTrackGraph(trackGraph);
		makeTrackContent();
	}
	

	@Override
	public void render()  {
		try {
			spotContent = universe.addContentLater(spotContent).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
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

	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private Content makeTrackContent() {
//		TODO
//		TrackDisplayNode trackContent = new TrackDisplayNode(trackGraph, spots, tracks, colors, radius/4);		
//		return new Content("Tracks", trackContent);
		return null;
	}


	
	
	
	private Content makeContent() {
		
		blobs = new TreeMap<Integer, SpotGroupNode<Spot>>();
		List<Spot> spotsThisFrame; 
		SpotGroupNode<Spot> blobGroup;
		ContentInstant contentThisFrame;
		TreeMap<Integer, ContentInstant> contentAllFrames = new TreeMap<Integer, ContentInstant>();
		
		for(Integer i : spots.keySet()) {
			spotsThisFrame = spots.get(i);
			HashMap<Spot, Point4f> centers = new HashMap<Spot, Point4f>(spotsThisFrame.size());
			float[] pos;
			float[] coords = new float[3];
			for(Spot spot : spotsThisFrame) {
				spot.getPosition(coords);
				pos = new float[] {coords[0], coords[1], coords[2], radius};
				centers.put(spot, new Point4f(pos));
			}
			blobGroup = new SpotGroupNode<Spot>(centers, new Color3f(color));
			contentThisFrame = new ContentInstant("Spots_frame_"+i);
			contentThisFrame.display(blobGroup);
			
			contentAllFrames.put(i, contentThisFrame);
			blobs.put(i, blobGroup);
		}
		return new Content("Spots", contentAllFrames);
	}


}
