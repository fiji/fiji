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
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.util.gui.AbstractAnnotation;
import fiji.util.gui.OverlayedImageCanvas;

public class HyperStackDisplayer extends SpotDisplayer {

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
			
			if (!trackVisible)
				return;
			
			g2d.setStroke(new BasicStroke((float) (1 / canvas.getMagnification())));

			switch (trackDisplayMode) {

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
		private TreeMap<Integer, List<Integer>> Z = new TreeMap<Integer, List<Integer>>();
		private TreeMap<Integer, List<Integer>> R = new TreeMap<Integer, List<Integer>>();
		private TreeMap<Integer, List<Color>> colors = new TreeMap<Integer, List<Color>>();
		
		public SpotOverlay() {
			this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
			for (int i = 0; i < imp.getNFrames(); i++) {
				X.put(i, new ArrayList<Integer>());
				Y.put(i, new ArrayList<Integer>());
				Z.put(i, new ArrayList<Integer>());
				R.put(i, new ArrayList<Integer>());
				colors.put(i, new ArrayList<Color>());
			}
		}

		@Override
		public void draw(Graphics2D g2d) {
			
			if (!spotVisible)
				return;
			
			g2d.setStroke(new BasicStroke((float) (1 / canvas.getMagnification())));
			final int frame = imp.getFrame()-1;
			final float zslice = (imp.getSlice()-1) * calibration[2];
			
			List<Color> c = colors.get(frame);
			List<Integer> r = R.get(frame);
			List<Integer> x = X.get(frame);
			List<Integer> y = Y.get(frame);
			List<Integer> z = Z.get(frame);

			float radius, dz2, zi;
			int apparentRadius;
			for (int i = 0; i < c.size(); i++) {
				g2d.setColor(c.get(i));
				radius = r.get(i) * calibration[0];
				zi = z.get(i) * calibration[2];
				dz2 = (zi - zslice) * (zi - zslice);
				if (dz2 >= radius*radius)
					g2d.fillOval(x.get(i) - 2, y.get(i) - 2, 4, 4);
				else {
					apparentRadius = (int) Math.round( Math.sqrt(radius*radius - dz2) / calibration[0]); 
					g2d.drawOval(x.get(i) - apparentRadius, y.get(i) - apparentRadius, 2 * apparentRadius, 2 * apparentRadius);			
				}
			}
		}
		
		public void addSpot(final Spot spot, float radius, Color color, int frame) {
			X.get(frame).add(Math.round(spot.getFeature(Feature.POSITION_X) / calibration[0] ));
			Y.get(frame).add(Math.round(spot.getFeature(Feature.POSITION_Y) / calibration[1] ));
			Z.get(frame).add(Math.round(spot.getFeature(Feature.POSITION_Z) / calibration[2] ));
			R.get(frame).add(Math.round(radius / calibration[0]));
			colors.get(frame).add(color);
		}
		
	}
	
	private ImagePlus imp;
	private OverlayedImageCanvas canvas;
	private float[] calibration;
	private SpotOverlay spotOverlay;
	/** Contains the track overlay objects. */
	private HashMap<Set<Spot>, TrackOverlay> wholeTrackOverlays = new HashMap<Set<Spot>, TrackOverlay>();
	private Feature currentFeature;
	private float featureMaxValue;
	private float featureMinValue;
	private Settings settings;
	private boolean trackVisible = true;
	private boolean spotVisible = true;

	/*
	 * CONSTRUCTORS
	 */
	
	public HyperStackDisplayer(final Settings settings) {
		this.radius = settings.segmenterSettings.expectedRadius;
		this.imp = settings.imp;
		this.calibration = new float[] { settings.dx, settings.dy, settings.dz };
		this.settings = settings;
	}
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public void setTrackVisible(boolean displayTrackSelected) {
		trackVisible = displayTrackSelected;
	}
	
	@Override
	public void setSpotVisible(boolean displaySpotSelected) {
		spotVisible = displaySpotSelected;		
	}
	
	@Override
	public void render() {
		if (null == imp) {
			this.imp = NewImage.createByteImage("Empty", settings.width, settings.height, settings.nframes, NewImage.FILL_BLACK);
			this.imp.setDimensions(1, 1, settings.nframes);
		}
		imp.setOpenAsHyperStack(true);
		canvas = new OverlayedImageCanvas(imp);
		StackWindow window = new StackWindow(imp, canvas);
		window.show();
		prepareSpotOverlay();
		prepareWholeTrackOverlay();
		refresh();
	}
	
	@Override
	public void setRadiusDisplayRatio(float ratio) {
		super.setRadiusDisplayRatio(ratio);
		prepareSpotOverlay();
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
	
	@Override
	public void clear() {
		canvas.clearOverlay();
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
				spotOverlay.addSpot(spot, radius * radiusRatio, spotColor, frame);
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
			for (int key : spotsToShow.keySet())
				if (spots.get(key).contains(source)) {
					frame = key;
					break;
				}
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
