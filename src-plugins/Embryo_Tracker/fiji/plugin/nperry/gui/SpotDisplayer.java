package fiji.plugin.nperry.gui;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import javax.vecmath.Color3f;
import javax.vecmath.Point4f;


import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;
import fiji.plugin.nperry.visualization.SpotGroupNode;

public class SpotDisplayer {
	
	private SpotGroupNode<Spot> blobs;
	private Collection<Spot> spots;
	
	private Color3f color = new Color3f(Color.YELLOW);
	private float radius = 5;

	
	public SpotDisplayer(Collection<Spot> spots) {
		setSpots(spots);
	}
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void render(Image3DUniverse universe) {
		ContentInstant ci = new ContentInstant("t0"); // TODO other instants, frames?
		ci.display(blobs);
		TreeMap<Integer, ContentInstant> instants = new TreeMap<Integer, ContentInstant>();
		instants.put(0, ci);
		Content c = new Content("instants", instants);
		universe.addContentLater(c);
	}
	
	public final void threshold(final Feature[] features, double[] thresholds, boolean[] isAboves) {
		double threshold;
		boolean isAbove;
		Feature feature;
		Float val;
	
		ArrayList<Spot> blobToShow = new ArrayList<Spot>(spots);
		ArrayList<Spot> blobToHide = new ArrayList<Spot>(spots.size());
		
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
		blobs.setVisible(blobToShow);
	}	

	public void resetTresholds() {
		blobs.setVisible(true);
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	
	private void setSpots(Collection<Spot> spots) {
		HashMap<Spot, Point4f> centers = new HashMap<Spot, Point4f>(spots.size());
		float[] pos;
		float[] coords;
		for(Spot spot : spots) {
			coords = spot.getCoordinates();
			pos = new float[] {coords[0], coords[1], coords[2], radius};
			centers.put(spot, new Point4f(pos));
		}
		blobs = new SpotGroupNode<Spot>(centers, color);
		this.spots = spots;
	}


}
