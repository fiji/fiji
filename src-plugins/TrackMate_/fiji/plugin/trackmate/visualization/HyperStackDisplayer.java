package fiji.plugin.trackmate.visualization;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.StackWindow;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.util.gui.AbstractAnnotation;
import fiji.util.gui.OverlayedImageCanvas;

public class HyperStackDisplayer extends SpotDisplayer implements MouseListener {

	/*
	 * INNER CLASSES
	 */
	
	private class TrackOverlay extends AbstractAnnotation {
		protected ArrayList<Integer> X0 = new ArrayList<Integer>();
		protected ArrayList<Integer> Y0 = new ArrayList<Integer>();
		protected ArrayList<Integer> X1 = new ArrayList<Integer>();
		protected ArrayList<Integer> Y1 = new ArrayList<Integer>();
		protected ArrayList<Integer> frames = new ArrayList<Integer>();
		protected float lineThickness = 1.0f;

		public TrackOverlay(Color color) {
			this.color = color;
			this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
		}

		@Override
		public void draw(Graphics2D g2d) {
			
			if (!trackVisible)
				return;
			
			g2d.setStroke(new BasicStroke((float) (lineThickness / canvas.getMagnification()),  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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
		
	private class SpotOverlay extends AbstractAnnotation {

		protected TreeMap<Integer, Map<Spot, Float>> R = new TreeMap<Integer, Map<Spot, Float>>();
		protected TreeMap<Integer, Map<Spot, Color>> colors = new TreeMap<Integer, Map<Spot, Color>>();
		protected float lineThickness = 1.0f;
		
		public SpotOverlay() {
			this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
			for (int i = 0; i < imp.getNFrames(); i++) {
				R.put(i, new HashMap<Spot, Float>());
				colors.put(i, new HashMap<Spot, Color>());
			}
		}

		@Override
		public void draw(Graphics2D g2d) {
			
			if (!spotVisible)
				return;
			
			final int frame = imp.getFrame()-1;
			final float zslice = (imp.getSlice()-1) * calibration[2];
			
			Map<Spot, Color> c = colors.get(frame);
			Map<Spot, Float> r = R.get(frame);
			Set<Spot> spotThisFrame = c.keySet();

			float x, y, z, radius, dz2;
			int apparentRadius;
			for (Spot spot : spotThisFrame) {
				g2d.setStroke(new BasicStroke((float) (lineThickness / canvas.getMagnification())));
				g2d.setColor(c.get(spot));
				radius = r.get(spot);
				x = spot.getFeature(Feature.POSITION_X);
				y = spot.getFeature(Feature.POSITION_Y);
				z = spot.getFeature(Feature.POSITION_Z);
				dz2 = (z - zslice) * (z - zslice);
				if (dz2 >= radius*radius)
					g2d.fillOval(Math.round(x/calibration[0]) - 2, Math.round(y/calibration[1]) - 2, 4, 4);
				else {
					apparentRadius = (int) Math.round( Math.sqrt(radius*radius - dz2) / calibration[0]); 
					g2d.drawOval(Math.round(x/calibration[0]) - apparentRadius, Math.round(y/calibration[1]) - apparentRadius, 
							2 * apparentRadius, 2 * apparentRadius);			
				}
			}
		}
		
		public void addSpot(final Spot spot, float radius, Color color, int frame) {
			R.get(frame).put(spot, radius);
			colors.get(frame).put(spot, color);
		}
	}
	
	private class HighlightSpotOverlay extends SpotOverlay {

		public HighlightSpotOverlay() {
			super();
			this.lineThickness = 2.0f;
		}
	}

	private class HighlightTrackOverlay extends TrackOverlay {
		
		public HighlightTrackOverlay() {
			super(HIGHLIGHT_COLOR);
			this.lineThickness = 2.0f;
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
	private StackWindow window;
	// For hightlight
	private HighlightSpotOverlay highlightSpotOverlay;
	private HighlightTrackOverlay highlightTrackOverlay;

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
	public void highlightSpots(final Set<Spot> spots) {
		if (null != highlightSpotOverlay)
			canvas.removeOverlay(highlightSpotOverlay);
		highlightSpotOverlay = new HighlightSpotOverlay();
		
		// Change target spots
		Spot thisSpot = null;
		int frame = 0;
		for (Spot spot : spots) {

			frame = - 1;
			for(int i : spotsToShow.keySet()) {
				List<Spot> spotThisFrame = spotsToShow.get(i);
				if (spotThisFrame.contains(spot)) {
					frame = i;
					break;
				}
			}
			if (frame == -1)
				continue;

			// Change settings of target spot 
			highlightSpotOverlay.addSpot(spot, radius * radiusRatio, HIGHLIGHT_COLOR, frame);
			// Keep reference for outer loop
			thisSpot = spot;
		}
		// Update displayed frame
		if (null == thisSpot)
			return;
		canvas.addOverlay(highlightSpotOverlay);
		int z = Math.round(thisSpot.getFeature(Feature.POSITION_Z) / calibration[2] ) + 1;
		imp.setPosition(1, z, frame+1);
//		window.setPosition(1, z, frame+1);
		imp.updateAndDraw();
	}
	
	@Override
	public void highlightEdges(Set<DefaultWeightedEdge> edges) {
		Spot source, target;
		int frame;
		if (null != highlightTrackOverlay)
			canvas.removeOverlay(highlightTrackOverlay);
		highlightTrackOverlay = new HighlightTrackOverlay();
		for (DefaultWeightedEdge edge : edges) {
			source = trackGraph.getEdgeSource(edge);
			target = trackGraph.getEdgeTarget(edge);
			frame = -1;
			for (int key : spotsToShow.keySet())
				if (spots.get(key).contains(source)) {
					frame = key;
					break;
				}
			highlightTrackOverlay.addEdge(source, target, frame);
		}
		canvas.addOverlay(highlightTrackOverlay);
		imp.updateAndDraw();
	}
	
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
			this.imp = NewImage.createByteImage("Empty", settings.width, settings.height, settings.nframes*settings.nslices, NewImage.FILL_BLACK);
			this.imp.setDimensions(1, settings.nslices, settings.nframes);
		}
		imp.setOpenAsHyperStack(true);
		canvas = new OverlayedImageCanvas(imp);
		window = new StackWindow(imp, canvas);
		window.show();
		canvas.addMouseListener(this);
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
	public void setTrackGraph(SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph) {
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
		Set<DefaultWeightedEdge> edges = trackGraph.edgeSet();
		int frame;
		for (DefaultWeightedEdge edge : edges) {
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

	@Override
	public void mouseClicked(MouseEvent e) {
		final int sx = e.getX();
		final int sy = e.getY();
		final int ix = canvas.offScreenX(sx);
		final int iy = canvas.offScreenY(sy);
		final float x = ix * calibration[0];
		final float y = iy * calibration[1];
		final float z = (imp.getSlice()-1) * calibration[2];
		final Spot clickLocation = new SpotImp(new float[] {x, y, z});
		final int frame = imp.getFrame() - 1;		
		final Set<Spot> highlight = getNClosestSpot(clickLocation, frame, 1);
		highlightSpots(highlight);		
	}


	@Override
	public void mousePressed(MouseEvent e) {}


	@Override
	public void mouseReleased(MouseEvent e) {}


	@Override
	public void mouseEntered(MouseEvent e) {}


	@Override
	public void mouseExited(MouseEvent e) {}

}
