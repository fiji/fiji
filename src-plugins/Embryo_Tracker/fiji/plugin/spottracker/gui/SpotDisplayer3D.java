package fiji.plugin.spottracker.gui;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.vecmath.Color3f;
import javax.vecmath.Point4f;

import fiji.plugin.spottracker.Featurable;
import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.TrackNode;
import fiji.plugin.spottracker.visualization.SpotGroupNode;

public class SpotDisplayer3D <K extends Featurable> extends SpotDisplayer<K> {
	
	private TreeMap<Integer, SpotGroupNode<K>> blobs;	
	private Content spotContent;
	private final Image3DUniverse universe;
	
	
	public SpotDisplayer3D(Collection<TrackNode<K>> spots, Image3DUniverse universe, final float radius) {
		TreeMap<Integer, Collection<TrackNode<K>>> spotsOverTime = new TreeMap<Integer, Collection<TrackNode<K>>>();
		spotsOverTime.put(0, spots);
		this.radius = radius;
		this.tracks = spotsOverTime;
		this.universe = universe;
		spotContent = makeContent();
	}
	
	public SpotDisplayer3D(TreeMap<Integer, Collection<TrackNode<K>>> spots, Image3DUniverse universe, final float radius) {
		this.radius = radius;
		this.tracks = spots;
		this.universe = universe;
		spotContent = makeContent();
	}
	
	public SpotDisplayer3D(TreeMap<Integer, Collection<TrackNode<K>>> spots, Image3DUniverse universe) {
		this(spots, universe, DEFAULT_DISPLAY_RADIUS);
	}
	
	public SpotDisplayer3D(Collection<TrackNode<K>> spots, Image3DUniverse universe) {
		this(spots, universe, DEFAULT_DISPLAY_RADIUS);
	}

	/*
	 * PUBLIC METHODS
	 */
	

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
			for (int key : tracks.keySet()) {
				for (TrackNode<K> spot : tracks.get(key)) {
					val = spot.getObject().getFeature(feature);
					if (null == val)
						continue;
					if (val > max) max = val;
					if (val < min) min = val;
				}
			}
			// Color using LUT
			Collection<TrackNode<K>> spotThisFrame;
			SpotGroupNode<K> spotGroup;
			for (int key : blobs.keySet()) {
				spotThisFrame = tracks.get(key);
				spotGroup = blobs.get(key);
				for ( TrackNode<K> node : spotThisFrame) {
					val = node.getObject().getFeature(feature);
					if (null == val) 
						spotGroup.setColor(node.getObject(), new Color3f(color));
					else
						spotGroup.setColor(node.getObject(), new Color3f(colorMap.getPaint((val-min)/(max-min))));
				}
			}
		}
	}


	@Override
	public final void refresh(final Feature[] features, double[] thresholds, boolean[] isAboves) {
		TreeMap<Integer, Collection<TrackNode<K>>> allNodesToShow = threshold(features, thresholds, isAboves);
		Collection<K> spotToShow;
		Collection<TrackNode<K>> nodesToShow;
		for(int key : allNodesToShow.keySet()){
			nodesToShow = allNodesToShow.get(key);
			spotToShow = new ArrayList<K>(nodesToShow.size());
			for(TrackNode<K> node : nodesToShow)
				spotToShow.add(node.getObject());
			blobs.get(key).setVisible(spotToShow);
		}
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
		
		blobs = new TreeMap<Integer, SpotGroupNode<K>>();
		Collection<TrackNode<K>> spotsThisFrame; 
		SpotGroupNode<K> spotGroup;
		ContentInstant contentThisFrame;
		TreeMap<Integer, ContentInstant> contentAllFrames = new TreeMap<Integer, ContentInstant>();
		
		for(Integer i : tracks.keySet()) {
			spotsThisFrame = tracks.get(i);
			HashMap<K, Point4f> centers = new HashMap<K, Point4f>(spotsThisFrame.size());
			float[] pos;
			float[] coords = new float[3];
			for(TrackNode<K> spot : spotsThisFrame) {
				spot.getObject().getPosition(coords);
				pos = new float[] {coords[0], coords[1], coords[2], radius};
				centers.put(spot.getObject(), new Point4f(pos));
			}
			spotGroup = new SpotGroupNode<K>(centers, new Color3f(color));
			contentThisFrame = new ContentInstant("Spots_frame_"+i);
			contentThisFrame.display(spotGroup);
			
			contentAllFrames.put(i, contentThisFrame);
			blobs.put(i, spotGroup);
		}
		return new Content("Spots", contentAllFrames);
	}


}
