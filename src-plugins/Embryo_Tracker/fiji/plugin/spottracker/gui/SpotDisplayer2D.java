package fiji.plugin.spottracker.gui;

import ij.ImagePlus;
import ij.gui.ImageWindow;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.util.gui.AbstractAnnotation;
import fiji.util.gui.OverlayedImageCanvas;

public class SpotDisplayer2D extends SpotDisplayer {

	/*
	 * INNER CLASS
	 */
	
	public class SpotOverlay extends AbstractAnnotation {

		private float xcenter;
		private float ycenter;
		private float radius;
		private boolean isVisible = true;
		
		public SpotOverlay(float xcenter, float ycenter, float radius) {
			this.xcenter = xcenter;
			this.ycenter = ycenter;
			this.radius = radius;
			setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.00f ));
			setColor(SpotDisplayer2D.DEFAULT_COLOR);
			setStroke(new BasicStroke(1));
		}
		

		public void setTransparency(float transparency) {
			setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1-transparency));
		}
		
		public void setVisible(boolean visible) {
			this.isVisible   = visible;
		}

		@Override
		public void draw(Graphics2D g2d) {
			if (!isVisible)
				return;
			final int x = Math.round((xcenter - radius) / calibration[0]); // We use the calibration field of the outer instance 
			final int y = Math.round((ycenter - radius) / calibration[1]);
			final int width = Math.round(2 * radius / calibration[0]);
			final int height = Math.round(2 * radius / calibration[1]);
			g2d.drawOval(x, y, width, height);			
		}
		
	}
	
	/*
	 * CONSTRUCTORS
	 */
	
	private final ImagePlus imp;
	private OverlayedImageCanvas canvas;
	private float[] calibration;
	private TreeMap<Integer, HashMap<Spot, SpotOverlay>> spotOverlays;

	public SpotDisplayer2D(Collection<Spot> spots, final ImagePlus imp, final float radius) {
		TreeMap<Integer, Collection<Spot>> spotsOverTime = new TreeMap<Integer, Collection<Spot>>();
		spotsOverTime.put(0, spots);
		this.radius = radius;
		this.spots = spotsOverTime;
		this.imp = imp;
		this.calibration = new float[] {(float) imp.getCalibration().pixelWidth, (float) imp.getCalibration().pixelHeight };
	}
	
	public SpotDisplayer2D(TreeMap<Integer, Collection<Spot>> spots, ImagePlus imp, final float radius) {
		this.radius = radius;
		this.spots = spots;
		this.imp = imp;
		this.calibration = new float[] {(float) imp.getCalibration().pixelWidth, (float) imp.getCalibration().pixelHeight };
	}
	
	public SpotDisplayer2D(TreeMap<Integer, Collection<Spot>> spots, final ImagePlus imp) {
		this(spots, imp, DEFAULT_DISPLAY_RADIUS);
	}
	
	public SpotDisplayer2D(Collection<Spot> spots, final ImagePlus imp) {
		this(spots, imp, DEFAULT_DISPLAY_RADIUS);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void render() {
		canvas = new OverlayedImageCanvas(imp);
		new ImageWindow(imp, canvas);
		
		spotOverlays = new TreeMap<Integer, HashMap<Spot, SpotOverlay>>();
		HashMap<Spot, SpotOverlay> map;
		SpotOverlay overlay;
		float[] coords;
		for (int key : spots.keySet()) {
			map = new  HashMap<Spot, SpotOverlay>(spots.size());
			for (Spot spot : spots.get(key)) {
				coords = spot.getCoordinates();
				overlay = new SpotOverlay(coords[0], coords[1], radius);
				canvas.addOverlay(overlay);
				map.put(spot, overlay);
			}
			spotOverlays.put(key, map);			
		}
		refresh(null, null, null);
	}
	
	
	@Override
	public void resetTresholds() {
		refresh(null, null, null);
	}

	@Override
	public void setColorByFeature(Feature feature) {
		if (null == feature) {
			for(int key : spotOverlays.keySet())
				for (SpotOverlay overlay : spotOverlays.get(key).values())
					overlay.setColor(color);
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
			HashMap<Spot, SpotOverlay> spotOverlaysInFrame;
			for (int key : spots.keySet()) {
				spotThisFrame = spots.get(key);
				spotOverlaysInFrame = spotOverlays.get(key);
				for ( Spot spot : spotThisFrame) {
					val = spot.getFeature(feature);
					if (null == val) 
						spotOverlaysInFrame.get(spot).setColor(color);
					else
						spotOverlaysInFrame.get(spot).setColor(colorMap.getPaint((val-min)/(max-min)));
				}
			}
		}
		imp.updateAndDraw();
	}

	@Override
	public void refresh(Feature[] features, double[] thresholds, boolean[] isAboves) {
		TreeMap<Integer, Collection<Spot>> spotsToShow = threshold(features, thresholds, isAboves);
		// Make all overlays invisible
		for (int key : spots.keySet()) 
			for (Spot spot : spots.get(key))
				spotOverlays.get(key).get(spot).setVisible(false);		
		// Select the ones in the currently displayed frame 
		final int frame = imp.getFrame();
		final Collection<Spot> spotThisFrame = spotsToShow.get(frame-1);
		final HashMap<Spot, SpotOverlay> spotOverlayThisFrame = spotOverlays.get(frame-1);
		// Resurrect selected spots
		for (Spot spot : spotThisFrame) 
			spotOverlayThisFrame.get(spot).setVisible(true);
		// Refresh
		imp.updateAndDraw();
	}

}
