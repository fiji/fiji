package fiji.plugin.spottracker.gui;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import fiji.plugin.spottracker.Featurable;
import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.TrackNode;
import fiji.util.gui.AbstractAnnotation;
import fiji.util.gui.OverlayedImageCanvas;

public class SpotDisplayer2D <K extends Featurable> extends SpotDisplayer<K> {

	/*
	 * INNER CLASSES
	 */
	
	public class TrackOverlay<L extends Featurable> extends AbstractAnnotation {

		private TrackNode<L> node;
		private float[] fCoords = new float[2];
		private float[] tCoords = new float[2];
		
		
		public TrackOverlay(TrackNode<L> node, Color color) {
			this.node = node;
			setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.00f ));
			setColor(color);
			setStroke(new BasicStroke(1));
		}
		
		@Override
		public void draw(Graphics2D g2d) {
			Set<TrackNode<L>> prevSpots = node.getParents();
			if (null == prevSpots)
				return;
			for(TrackNode<L> to : node.getParents())
				draw(node, to, g2d);
		}
		
		private final void draw(final TrackNode<L> from, final TrackNode<L> to, final Graphics2D g2d) {
			from.getObject().getPosition(fCoords);
			to.getObject().getPosition(tCoords);
			int x1 = (int) fCoords[0];
			int y1 = (int) fCoords[1];
			int x2 = (int) tCoords[0];
			int y2 = (int) tCoords[1];
			g2d.drawLine(x1, y1, x2, y2);
			Set<TrackNode<L>> parents = to.getParents();
			for (TrackNode<L> node : parents)
				draw(to, node, g2d);
		}
	}
	
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
	private HashMap<Featurable, SpotOverlay> spotOverlays = new HashMap<Featurable, SpotOverlay>();
	private TreeMap<Integer, Collection<TrackNode<K>>> spotsToShow = new TreeMap<Integer, Collection<TrackNode<K>>>();
	private Feature currentFeature;
	private float featureMaxValue;
	private float featureMinValue;

	public SpotDisplayer2D(Collection<TrackNode<K>> spots, final ImagePlus imp, final float radius, final float[] calibration) {
		TreeMap<Integer, Collection<TrackNode<K>>> spotsOverTime = new TreeMap<Integer, Collection<TrackNode<K>>>();
		spotsOverTime.put(0, spots);
		this.radius = radius;
		this.tracks = spotsOverTime;
		this.imp = imp;
		this.calibration = calibration;
	}
	
	public SpotDisplayer2D(Collection<TrackNode<K>> spots, final ImagePlus imp, final float radius) {
		this(spots, imp, radius, 
				new float[] {(float) imp.getCalibration().pixelWidth, (float) imp.getCalibration().pixelHeight});
	}
	
	public SpotDisplayer2D(TreeMap<Integer, Collection<TrackNode<K>>> spots, ImagePlus imp, final float radius, float[] calibration) {
		this.radius = radius;
		this.tracks = spots;
		this.imp = imp;
		this.calibration = calibration;
	}
	
	public SpotDisplayer2D(TreeMap<Integer, Collection<TrackNode<K>>> spots, ImagePlus imp, final float radius) {
		this(spots, imp, radius, 
				new float[] {(float) imp.getCalibration().pixelWidth, (float) imp.getCalibration().pixelHeight });
	}
	
	public SpotDisplayer2D(TreeMap<Integer, Collection<TrackNode<K>>> spots, final ImagePlus imp) {
		this(spots, imp, DEFAULT_DISPLAY_RADIUS);
	}
	
	public SpotDisplayer2D(Collection<TrackNode<K>> spots, final ImagePlus imp) {
		this(spots, imp, DEFAULT_DISPLAY_RADIUS);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public void render() {
		spotsToShow = tracks;
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
		for (int key : tracks.keySet()) {
			for (TrackNode<K> node : tracks.get(key)) {
				val = node.getObject().getFeature(feature);
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
		if (tracks.get(frame) == null)
			return;
		imp.updateAndDraw();
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void displayFrame(int frame) {
		final Collection<TrackNode<K>> spotsThisFrame = spotsToShow.get(frame);		
		spotOverlays.clear();
		canvas.clearOverlay();
		if (null == spotsThisFrame)
			return;
		SpotOverlay overlay;
		float[] coords = new float[2];
		Float val;
		for (TrackNode<K> node : spotsThisFrame) {
			node.getObject().getPosition(coords);
			val = node.getObject().getFeature(currentFeature);
			if (null == currentFeature || null == val)
				overlay = new SpotOverlay(coords[0], coords[1], radius);
			else
				overlay = new SpotOverlay(coords[0], coords[1], radius, colorMap.getPaint((val-featureMinValue)/(featureMaxValue-featureMinValue)));
			canvas.addOverlay(overlay);
			spotOverlays.put(node.getObject(), overlay);
		}
		
		if (displayTracks) {
			
			if (frame == 0) 
				return;
			
			TrackOverlay<K> trackOverkay;
			for (TrackNode<K> spot : spotsThisFrame) {
				trackOverkay = new TrackOverlay<K>(spot, DEFAULT_COLOR);
				canvas.addOverlay(trackOverkay);
			}
		}
		
	}

}
