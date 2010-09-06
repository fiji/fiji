package fiji.plugin.nperry.gui;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.vecmath.Color3f;
import javax.vecmath.Point4f;

import org.jfree.chart.renderer.InterpolatePaintScale;


import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;
import fiji.plugin.nperry.visualization.SpotGroupNode;

public class SpotDisplayer {
	
	private TreeMap<Integer,SpotGroupNode<Spot>> blobs;
	private TreeMap<Integer,Collection<Spot>> spots;
	
	private Color3f color = new Color3f(new Color(1f, 0, 1f));
	private float radius = 5;
	private Content spotContent;
	private final static InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;

	
	public SpotDisplayer(Collection<Spot> spots) {
		TreeMap<Integer, Collection<Spot>> spotsOverTime = new TreeMap<Integer, Collection<Spot>>();
		spotsOverTime.put(0, spots);
		spotContent = makeContent(spotsOverTime);
	}

	public SpotDisplayer(TreeMap<Integer, Collection<Spot>> spots) {
		spotContent = makeContent(spots);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void render(Image3DUniverse universe) throws InterruptedException, ExecutionException {
		spotContent = universe.addContentLater(spotContent).get();
	}
	
	public void setColorByFeature(final Feature feature) {
		if (null == feature) {
			for(int key : blobs.keySet())
				blobs.get(key).setColor(color);
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
						spotGroup.setColor(spot, color);
					else
						spotGroup.setColor(spot, new Color3f(colorMap.getPaint((val-min)/(max-min))));
				}
			}
		}
	}


	
	public final void threshold(final Feature[] features, double[] thresholds, boolean[] isAboves) {
		double threshold;
		boolean isAbove;
		Feature feature;
		Float val;
		Collection<Spot> spotThisFrame;

		for (int key : spots.keySet()) {
			
			spotThisFrame = spots.get(key);
			ArrayList<Spot> blobToShow = new ArrayList<Spot>(spotThisFrame);
			ArrayList<Spot> blobToHide = new ArrayList<Spot>(spotThisFrame.size());

			Spot blob;

			for (int i = 0; i < features.length; i++) {

				threshold = thresholds[i];
				feature = features[i];
				isAbove = isAboves[i];

				blobToHide.clear();
				if (isAbove) {
					for (int j = 0; j < blobToShow.size(); j++) {
						blob = blobToShow.get(j);
						val = blob.getFeature(feature);
						if (null == val)
							continue;
						if ( val < threshold) {
							blobToHide.add(blob);
						}
					}

				} else {
					for (int j = 0; j < blobToShow.size(); j++) {
						blob = blobToShow.get(j);
						val = blob.getFeature(feature);
						if (null == val)
							continue;
						if ( val > threshold) {
							blobToHide.add(blob); 
						}
					}

				}
				blobToShow.removeAll(blobToHide); // no need to treat them multiple times
			}
			blobs.get(key).setVisible(blobToShow);
		}
	}	

	public void resetTresholds() {
		for(int key : blobs.keySet())
			blobs.get(key).setVisible(true);
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private Content makeContent(final TreeMap<Integer, Collection<Spot>> spots) {
		this.spots = spots;
		
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
			spotGroup = new SpotGroupNode<Spot>(centers, color);
			contentThisFrame = new ContentInstant("Spots_frame_"+i);
			contentThisFrame.display(spotGroup);
			
			contentAllFrames.put(i, contentThisFrame);
			blobs.put(i, spotGroup);
		}
		
		return new Content("Spots", contentAllFrames);
	}


}
