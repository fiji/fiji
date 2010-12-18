package fiji.plugin.trackmate.visualization;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.StackWindow;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.util.gui.AbstractAnnotation;
import fiji.util.gui.OverlayedImageCanvas;

public class SpotDisplayer2D extends SpotDisplayer {

	/*
	 * INNER CLASSES
	 */
	
	public class TrackOverlay extends AbstractAnnotation {
		private ArrayList<Integer> X0 = new ArrayList<Integer>();
		private ArrayList<Integer> Y0 = new ArrayList<Integer>();
		private ArrayList<Integer> X1 = new ArrayList<Integer>();
		private ArrayList<Integer> Y1 = new ArrayList<Integer>();
		private ArrayList<Integer> frames = new ArrayList<Integer>();

		public TrackOverlay(Color color) {
			this.color = color;
			this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
		}

		@Override
		public void draw(Graphics2D g2d) {
			g2d.setStroke(new BasicStroke((float) (1 / canvas.getMagnification())));

			switch (trackDisplayMode) {

			case DO_NOT_DISPLAY:
				return;

			case ALL_WHOLE_TRACKS:
				for (int i = 0; i < frames.size(); i++) 
					g2d.drawLine(X0.get(i), Y0.get(i), X1.get(i), Y1.get(i));
				break;

			case LOCAL_WHOLE_TRACKS: {
				final int currentFrame = imp.getFrame()-1;
				int frameDist;
				for (int i = 0; i < frames.size(); i++) {
					frameDist = Math.abs(frames.get(i) - currentFrame); 
					if (frameDist > trackDisplayDepth)
						continue;
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - (float) frameDist / trackDisplayDepth));
					g2d.drawLine(X0.get(i), Y0.get(i), X1.get(i), Y1.get(i));
				}				
				break;
			}

			case LOCAL_FORWARD_TRACKS: {
				final int currentFrame = imp.getFrame()-1;
				int frameDist;
				for (int i = 0; i < frames.size(); i++) {
					frameDist = frames.get(i) - currentFrame; 
					if (frameDist < 0 || frameDist > trackDisplayDepth)
						continue;
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - (float) frameDist / trackDisplayDepth));
					g2d.drawLine(X0.get(i), Y0.get(i), X1.get(i), Y1.get(i));
				}
				break;
			}

			case LOCAL_BACKWARD_TRACKS: {
				final int currentFrame = imp.getFrame()-1;
				int frameDist;
				for (int i = 0; i < frames.size(); i++) {
					frameDist = currentFrame - frames.get(i); 
					if (frameDist < 0 || frameDist > trackDisplayDepth)
						continue;
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1  - (float) frameDist / trackDisplayDepth));
					g2d.drawLine(X0.get(i), Y0.get(i), X1.get(i), Y1.get(i));
				}
				break;
			}

			}

		}
		
		public void addEdge(final Spot source, final Spot target, final int frame) {
			X0.add(Math.round(source.getFeature(Feature.POSITION_X) / calibration[0]) );
			Y0.add(Math.round(source.getFeature(Feature.POSITION_Y) / calibration[1]) );
			X1.add(Math.round(target.getFeature(Feature.POSITION_X) / calibration[0]) );
			Y1.add(Math.round(target.getFeature(Feature.POSITION_Y) / calibration[1]) );
			frames.add(frame);
		}
		
	}
		
	public class SpotOverlay extends AbstractAnnotation {

		private TreeMap<Integer, List<Integer>> X = new TreeMap<Integer, List<Integer>>();
		private TreeMap<Integer, List<Integer>> Y = new TreeMap<Integer, List<Integer>>();
		private TreeMap<Integer, List<Integer>> R = new TreeMap<Integer, List<Integer>>();
		private TreeMap<Integer, List<Color>> colors = new TreeMap<Integer, List<Color>>();
		
		public SpotOverlay() {
			this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
			for (int i = 0; i < imp.getNFrames(); i++) {
				X.put(i, new ArrayList<Integer>());
				Y.put(i, new ArrayList<Integer>());
				R.put(i, new ArrayList<Integer>());
				colors.put(i, new ArrayList<Color>());
			}
		}

		@Override
		public void draw(Graphics2D g2d) {
			g2d.setStroke(new BasicStroke((float) (1 / canvas.getMagnification())));
			final int frame = imp.getFrame()-1;
			List<Color> c = colors.get(frame);
			List<Integer> r = R.get(frame);
			List<Integer> x = X.get(frame);
			List<Integer> y = Y.get(frame);

			for (int i = 0; i < c.size(); i++) {
				g2d.setColor(c.get(i));
				g2d.drawOval(x.get(i) - r.get(i), y.get(i) - r.get(i), 2 * r.get(i), 2 * r.get(i));			
			}
		}
		
		public void addSpot(final Spot spot, float radius, Color color, int frame) {
			X.get(frame).add(Math.round(spot.getFeature(Feature.POSITION_X) / calibration[0] ));
			Y.get(frame).add(Math.round(spot.getFeature(Feature.POSITION_Y) / calibration[1] ));
			R.get(frame).add(Math.round(radius / calibration[0]));
			colors.get(frame).add(color);
		}
		
	}
	
	/*
	 * CONSTRUCTORS
	 */
	
	private ImagePlus imp;
	private OverlayedImageCanvas canvas;
	private float[] calibration;
	private SpotOverlay spotOverlay;
	/** Contains the track overlay objects. */
	private HashMap<Set<Spot>, TrackOverlay> wholeTrackOverlays = new HashMap<Set<Spot>, TrackOverlay>();
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
		if (null == imp) {
			float max_x = 0;
			float max_y = 0;
			float x, y;
			int max_t = 0;
			for (int frame : spots.keySet()) {
				for(Spot spot : spots.get(frame)) {
					x = spot.getFeature(Feature.POSITION_X);
					y = spot.getFeature(Feature.POSITION_Y);
					if (x > max_x)
						max_x = x;
					if (y > max_y)
						max_y = y;
				}
				max_t = frame;
			}
			this.imp = NewImage.createByteImage("Empty", (int)max_x+1, (int)max_y+1, max_t+1, NewImage.FILL_BLACK);
			this.imp.setDimensions(1, 1, max_t+1);
		}
		canvas = new OverlayedImageCanvas(imp);
		StackWindow window = new StackWindow(imp, canvas);
		window.show();
		prepareSpotOverlay();
		prepareWholeTrackOverlay();
		refresh();
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
		prepareSpotOverlay();
		refresh();
	}
		
	@Override
	public void refresh() {
		imp.updateAndDraw();
	}
		
	@Override
	public void setTrackGraph(SimpleGraph<Spot, DefaultEdge> trackGraph) {
		super.setTrackGraph(trackGraph);
		prepareWholeTrackOverlay();
	}
	
	@Override
	public void setSpotsToShow(TreeMap<Integer, List<Spot>> spotsToShow) {
		super.setSpotsToShow(spotsToShow);
		prepareSpotOverlay();
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void prepareSpotOverlay() {
		if (null == spotsToShow)
			return;
		canvas.removeOverlay(spotOverlay);
		spotOverlay = new SpotOverlay();
		Color spotColor;
		Float val;
		for(int frame : spotsToShow.keySet()) {
			List<Spot> spotThisFrame = spotsToShow.get(frame);
			for(Spot spot : spotThisFrame) {
				val = spot.getFeature(currentFeature);
				if (null == currentFeature || null == val)
					spotColor = color;
				else
					spotColor = colorMap.getPaint((val-featureMinValue)/(featureMaxValue-featureMinValue));
				spotOverlay.addSpot(spot, radius, spotColor, frame);
			}
		}
		canvas.addOverlay(spotOverlay);
	}
	

	
	private void prepareWholeTrackOverlay() {
		if (null == tracks)
			return;
		for (TrackOverlay wto : wholeTrackOverlays.values()) 
			canvas.removeOverlay(wto);
		wholeTrackOverlays.clear();
		for (Set<Spot> track : tracks)
			wholeTrackOverlays.put(track, new TrackOverlay(trackColors.get(track)));
		
		Spot source, target;
		Set<DefaultEdge> edges = trackGraph.edgeSet();
		int frame;
		
		for (DefaultEdge edge : edges) {
			source = trackGraph.getEdgeSource(edge);
			target = trackGraph.getEdgeTarget(edge);
			// Find to what frame it belongs to
			frame = -1;
			for (int key : spots.keySet())
				if (spots.get(key).contains(source)) {
					frame = key;
					break;
				}
			// Find to what track it belongs to
			for (Set<Spot> track : tracks) {
				if (track.contains(source)) {
					wholeTrackOverlays.get(track).addEdge(source, target, frame);
					break;
				}
			}
		}
		
		for (TrackOverlay wto : wholeTrackOverlays.values())
			canvas.addOverlay(wto);
	}

}
