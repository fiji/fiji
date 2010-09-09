package fiji.plugin.spottracker.gui;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.vecmath.Color3f;
import javax.vecmath.Point4f;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.visualization.SpotGroupNode;

public class SpotDisplayer3D extends SpotDisplayer {
	
	private TreeMap<Integer,SpotGroupNode<Spot>> blobs;	
	private Content spotContent;
	
	
	public SpotDisplayer3D(Collection<Spot> spots, final float radius) {
		TreeMap<Integer, Collection<Spot>> spotsOverTime = new TreeMap<Integer, Collection<Spot>>();
		spotsOverTime.put(0, spots);
		this.radius = radius;
		this.spots = spotsOverTime;
		spotContent = makeContent();
	}
	
	public SpotDisplayer3D(TreeMap<Integer, Collection<Spot>> spots, final float radius) {
		this.radius = radius;
		this.spots = spots;
		spotContent = makeContent();
	}
	
	public SpotDisplayer3D(TreeMap<Integer, Collection<Spot>> spots) {
		this(spots, DEFAULT_DISPLAY_RADIUS);
	}
	
	public SpotDisplayer3D(Collection<Spot> spots) {
		this(spots, DEFAULT_DISPLAY_RADIUS);
	}

	/*
	 * PUBLIC METHODS
	 */
	

	public void render(Image3DUniverse universe) throws InterruptedException, ExecutionException {
		spotContent = universe.addContentLater(spotContent).get();
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
			Collection<Spot> spotThisFrame;
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
	public final void refresh(final Feature[] features, double[] thresholds, boolean[] isAboves) {
		TreeMap<Integer, Collection<Spot>> spotToShow = threshold(features, thresholds, isAboves);
		for (int key : spotToShow.keySet())
			blobs.get(key).setVisible(spotToShow.get(key));
	}	

	@Override
	public void resetTresholds() {
		for(int key : blobs.keySet())
			blobs.get(key).setVisible(true);
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private Content makeContent() {
		
		blobs = new TreeMap<Integer, SpotGroupNode<Spot>>();
		Collection<Spot> spotsThisFrame; 
		SpotGroupNode<Spot> spotGroup;
		ContentInstant contentThisFrame;
		TreeMap<Integer, ContentInstant> contentAllFrames = new TreeMap<Integer, ContentInstant>();
		
		for(Integer i : spots.keySet()) {
			spotsThisFrame = spots.get(i);
			HashMap<Spot, Point4f> centers = new HashMap<Spot, Point4f>(spotsThisFrame.size());
			float[] pos;
			float[] coords;
			for(Spot spot : spotsThisFrame) {
				coords = spot.getCoordinates();
				pos = new float[] {coords[0], coords[1], coords[2], radius};
				centers.put(spot, new Point4f(pos));
			}
			spotGroup = new SpotGroupNode<Spot>(centers, new Color3f(color));
			contentThisFrame = new ContentInstant("Spots_frame_"+i);
			contentThisFrame.display(spotGroup);
			
			contentAllFrames.put(i, contentThisFrame);
			blobs.put(i, spotGroup);
		}
		return new Content("Spots", contentAllFrames);
	}


}
