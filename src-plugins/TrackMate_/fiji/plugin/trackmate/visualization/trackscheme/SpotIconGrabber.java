package fiji.plugin.trackmate.visualization.trackscheme;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.util.Collection;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;

/**
 * This class is used to take a snapshot of a {@link Spot} object (or collection) from 
 * its coordinates and an {@link ImagePlus} that contain the pixel data.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Dec 2010
 */
public class SpotIconGrabber {

	private static final float ENLARGE_FACTOR = 1.1f;
	
	private ImagePlus imp;
	private float[] calibration;
	private float dt;
	
	public SpotIconGrabber(Settings settings) {
		this.imp = settings.imp;
		this.calibration = new float[] { settings.dx, settings.dy, settings.dz };
		this.dt = settings.dt;
	}
	
	public void updateIcon(SpotCollection spots) {
		for(int key : spots.keySet())
			updateIcon(spots.get(key));
	}
	
	public void updateIcon(Collection<Spot> spots) {
		for (Spot spot : spots)
			updateIcon(spot);
	}
	
	public void updateIcon(Spot spot) {
		final float radius = spot.getFeature(Feature.RADIUS); // physical units
		int slice = 1;
		if (calibration.length > 2)
			slice = Math.round(spot.getFeature(Feature.POSITION_Z) / calibration[2]) + 1;
		int frame = Math.round(spot.getFeature(Feature.POSITION_T) / dt) + 1;
		int index = imp.getStackIndex(1, slice, frame);
		int x = Math.round((spot.getFeature(Feature.POSITION_X) - ENLARGE_FACTOR*radius) / calibration[0]); 
		int y = Math.round((spot.getFeature(Feature.POSITION_Y) - ENLARGE_FACTOR*radius) / calibration[1]);
		int width = Math.round(2 * ENLARGE_FACTOR * radius / calibration[0]);
		int height = Math.round(2 * ENLARGE_FACTOR * radius / calibration[1]);
		Roi roi = new Roi(x, y, width, height);
		ImageProcessor ip = imp.getStack().getProcessor(index).duplicate();
		ip.setRoi(roi);
		ip = ip.crop();
		ip.resetMinAndMax();
		spot.setImage(ip.getBufferedImage());
	}
}
