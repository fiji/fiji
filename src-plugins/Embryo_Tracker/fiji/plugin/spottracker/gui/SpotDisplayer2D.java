package fiji.plugin.spottracker.gui;

import ij.ImagePlus;

import ij.gui.ImageWindow;
import ij.gui.OvalRoi;
import ij.gui.Roi;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.util.gui.OverlayedImageCanvas;
import fiji.util.gui.OverlayedImageCanvas.Overlay;

public class SpotDisplayer2D extends SpotDisplayer {

	/*
	 * INNER CLASS
	 */
	
	public static class SpotOverlay implements Overlay {

		private Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.00f );
		private Color color;
		private double xcenter;
		private double ycenter;
		private double radius;
		private boolean isVisible = true;
		
		public SpotOverlay(double xcenter, double ycenter, double radius) {
			this.xcenter = xcenter;
			this.ycenter = ycenter;
			this.radius = radius;
		}
		
		@Override
		public void paint(Graphics g, int x, int y, double magnification) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setComposite(Composite composite) {
			this.composite = composite;			
		}
		
		public void setTransparency(float transparency) {
			this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency);
		}
		
		public void setColor(Color color) {
			this.color = color;
		}
		
		public void setVisible(boolean visible) {
			this.isVisible   = visible;
		}
		
	}
	
	/*
	 * CONSTRUCTORS
	 */
	
	private final ImagePlus imp;
	private OverlayedImageCanvas canvas;

	public SpotDisplayer2D(Collection<Spot> spots, final ImagePlus imp, final float radius) {
		TreeMap<Integer, Collection<Spot>> spotsOverTime = new TreeMap<Integer, Collection<Spot>>();
		spotsOverTime.put(0, spots);
		this.radius = radius;
		this.spots = spotsOverTime;
		this.imp = imp;
	}
	
	public SpotDisplayer2D(TreeMap<Integer, Collection<Spot>> spots, ImagePlus imp, final float radius) {
		this.radius = radius;
		this.spots = spots;
		this.imp = imp;
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
		refresh(null, null, null);
	}
	
	
	@Override
	public void resetTresholds() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setColorByFeature(Feature feature) {
		// TODO Auto-generated method stub

	}

	@Override
	public void refresh(Feature[] features, double[] thresholds, boolean[] isAboves) {
		TreeMap<Integer, Collection<Spot>> spotsToShow = threshold(features, thresholds, isAboves);

		final int frame = imp.getFrame();
		final Collection<Spot> spotThisFrame = spotsToShow.get(frame-1);
		if (null == spotThisFrame)
			return;

		final int width = (int) (2*radius);
		final int height = (int) (2*radius);
		final double pixelHeight = imp.getCalibration().pixelHeight;
		final double pixelWidth = imp.getCalibration().pixelWidth;
		
		SpotOverlay overlay;
		double x, y;
		float[] coords;
		
		HashMap<Spot, SpotOverlay> overlays = new HashMap<Spot, SpotOverlay>(spots.size());
		for (Spot spot : spotThisFrame) {
			coords = spot.getCoordinates();
			x = coords[0]*pixelWidth;
			y = coords[1]*pixelHeight;
			overlay = new SpotOverlay(x, y, radius);
			canvas.addOverlay(overlay);
		}
		imp.updateAndDraw();
		
	}

}
