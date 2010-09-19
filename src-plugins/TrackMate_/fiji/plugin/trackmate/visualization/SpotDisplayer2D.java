package fiji.plugin.trackmate.visualization;

import ij.ImagePlus;
import ij.gui.ScrollbarWithLabel;
import ij.gui.StackWindow;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.util.gui.AbstractAnnotation;
import fiji.util.gui.OverlayedImageCanvas;

public class SpotDisplayer2D extends SpotDisplayer {

	/*
	 * INNER CLASSES
	 */
		
	public class SpotOverlay extends AbstractAnnotation {

		private float xcenter;
		private float ycenter;
		private float radius;
		private boolean isVisible = true;
		
		public SpotOverlay(float xcenter, float ycenter, float radius, Color color) {
			this.xcenter = xcenter;
			this.ycenter = ycenter;
			this.radius = radius;
			setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.00f ));
			setColor(color);
			setStroke(new BasicStroke(1));
		}
		
		public SpotOverlay(float xcenter, float ycenter, float radius) {
			this(xcenter, ycenter, radius, DEFAULT_COLOR);
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
	private HashMap<Spot, SpotOverlay> spotOverlays = new HashMap<Spot, SpotOverlay>();
	private TreeMap<Integer, List<Spot>> spotsToShow = new TreeMap<Integer, List<Spot>>();
	private Feature currentFeature;
	private float featureMaxValue;
	private float featureMinValue;

	public SpotDisplayer2D(final ImagePlus imp, final float radius, final float[] calibration) {
		this.radius = radius;
		this.imp = imp;
		this.calibration = calibration;
		this.radius = radius;
	}
	
	public SpotDisplayer2D(ImagePlus imp, final float radius) {
		this(imp, radius, 
				new float[] {(float) imp.getCalibration().pixelWidth, (float) imp.getCalibration().pixelHeight });
	}
	
	public SpotDisplayer2D(final ImagePlus imp) {
		this(imp, DEFAULT_DISPLAY_RADIUS);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public void render() {
		spotsToShow = spots;
		canvas = new OverlayedImageCanvas(imp);
		StackWindow window = new StackWindow(imp, canvas);
		ScrollbarWithLabel scrollbar = window.getZSelector();
		if (null != scrollbar)
			scrollbar.addAdjustmentListener(new AdjustmentListener() {
				@Override
				public void adjustmentValueChanged(AdjustmentEvent e) {
					displayFrame(imp.getFrame()-1);
				}
			});
		window.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				displayFrame(imp.getFrame() -1 );
			}
		});
		displayFrame(imp.getFrame() - 1);
	}
	
	
	@Override
	public void resetTresholds() {
		refresh(null, null, null);
	}

	@Override
	public void setColorByFeature(Feature feature) {
		currentFeature = feature;
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
		featureMinValue = min;
		featureMaxValue = max;
		displayFrame(imp.getFrame() - 1);
		imp.updateAndDraw();
	}
		
	@Override
	public void refresh(Feature[] features, double[] thresholds, boolean[] isAboves) {
		spotsToShow = threshold(features, thresholds, isAboves);
		displayFrame(imp.getFrame() - 1); // refresh overlay -> it now only has the displayed spots
		// Make all overlays invisible
		final int frame = imp.getFrame() -1; // 0 - based
		if (spots.get(frame) == null)
			return;
		imp.updateAndDraw();
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void displayFrame(int frame) {
		final List<Spot> spotsThisFrame = spotsToShow.get(frame);		
		spotOverlays.clear();
		canvas.clearOverlay();
		if (null == spotsThisFrame)
			return;
		SpotOverlay overlay;
		float[] coords = new float[3];
		Float val;
		for (Spot spot : spotsThisFrame) {
			spot.getPosition(coords);
			val = spot.getFeature(currentFeature);
			if (null == currentFeature || null == val)
				overlay = new SpotOverlay(coords[0], coords[1], radius);
			else
				overlay = new SpotOverlay(coords[0], coords[1], radius, colorMap.getPaint((val-featureMinValue)/(featureMaxValue-featureMinValue)));
			canvas.addOverlay(overlay);
			spotOverlays.put(spot, overlay);
		}
		
		if (displayTracks) {
			
		}
		
	}

}
