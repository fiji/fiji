package fiji.plugin.nperry.gui;

import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;

import javax.vecmath.Color3f;

import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class SpotDisplayer {
	
	private ArrayList<SpotContent> blobs;
	private float transparency = 0.5f;
	private Color3f color = new Color3f(Color.YELLOW);
	private float radius = 5;

	
	public SpotDisplayer(Collection<Spot> spots) {
		setSpots(spots);
	}
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void render(Image3DUniverse universe) {
		for (SpotContent blob : blobs)
			universe.addContentLater(blob);
	}
	
	public final void threshold(final Feature[] features, double[] thresholds, boolean[] isAboves) {
		double threshold;
		boolean isAbove;
		Feature feature;
		Float val;
		
		for (SpotContent blob : blobs)
			blob.setVisible(true);

		ArrayList<SpotContent> blobCopy = new ArrayList<SpotContent>(blobs);
		ArrayList<SpotContent> blobToRemove = new ArrayList<SpotContent>(blobs);
		
		SpotContent blob;
		
		for (int i = 0; i < features.length; i++) {

			threshold = thresholds[i];
			feature = features[i];
			isAbove = isAboves[i];

			blobToRemove.clear();
			if (isAbove) {
				for (int j = 0; j < blobCopy.size(); j++) {
					blob = blobCopy.get(j);
					val = blob.getFeature(feature);
					if (null == val)
						continue;
					if ( val < threshold) {
						blobToRemove.add(blob); 
						blob.setVisible(false);
					}
				}

			} else {
				for (int j = 0; j < blobCopy.size(); j++) {
					blob = blobCopy.get(j);
					val = blob.getFeature(feature);
					if (null == val)
						continue;
					if ( val > threshold) {
						blobToRemove.add(blob); 
						blob.setVisible(false);
					}
				}
				
			}
			blobCopy.removeAll(blobToRemove);
		}
	}
	
	public final void setColoringBy(final Feature feature) {
		if (null == feature) 
			for (SpotContent blob : blobs)
				blob.setColor(SpotContent.DEFAULT_COLOR);
		else {
			Float min = Float.POSITIVE_INFINITY;
			Float max = Float.NEGATIVE_INFINITY;
			Float val;
			for (SpotContent blob : blobs) {
				val = blob.getFeature(feature);
				if (null == val)
					continue;
				if (val < min) min = val;
				if (val > max) max = val;
			}
			Color3f color;
			float hue;
			for (SpotContent blob : blobs) {
				val = blob.getFeature(feature);
				if (null == val)
					continue;
				hue = (val-min) / (max - min);
				color = new Color3f(new Color(Color.HSBtoRGB(hue, 1, 1)));
				blob.setColor(color);
			}
		}
	}
	

	public void resetTresholds() {
		for (SpotContent blob : blobs)
			blob.setVisible(true);
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	
	private void setSpots(Collection<Spot> spots) {
		blobs = new ArrayList<SpotContent>(spots.size());
		SpotContent blob;
		int index = 0;
		for (Spot spot : spots) {
			blob = new SpotContent(spot, radius, color, transparency);
			blobs.add(blob);
			index++;
		}
	}


}
