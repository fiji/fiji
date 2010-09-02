package fiji.plugin.nperry.gui;

import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;

import javax.vecmath.Color3f;

public class SpotDisplayer {
	
	private ImagePlus imp;
	private Content imageContent;
	private ArrayList<SpotContent> blobs;

	
	public SpotDisplayer(Collection<Spot> spots, ImagePlus imp) {
		setSpots(spots);
		setImagePlus(imp);
	}
	
	public SpotDisplayer(Collection<Spot> spots) {
		this(spots, null);
	}
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	public Image3DUniverse render() {
		Image3DUniverse univ = new Image3DUniverse();
		if (imp != null) {
			imageContent = univ.addVoltex(imp);
			imageContent.setLocked(true);
		}
		
		for (SpotContent blob : blobs)
			univ.addContentLater(blob);
		return univ;		
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
	
	
	/*
	 * PRIVATE METHODS
	 */
	

	private void setImagePlus(final ImagePlus imp) {
		if (null == imp) { 
			this.imp = null;
			return;
		}
		ImagePlus copy = new Duplicator().run(imp, 1, imp.getNSlices());
		ContentCreator.convert(copy);
		this.imp = copy;
	}
	
	private void setSpots(Collection<Spot> spots) {
		blobs = new ArrayList<SpotContent>(spots.size());
		SpotContent blob;
		int index = 0;
		for (Spot spot : spots) {
			blob = new SpotContent(spot);
			blobs.add(blob);
			index++;
		}
	}

	public void resetTresholds() {
		for (SpotContent blob : blobs)
			blob.setVisible(true);
	}

}
