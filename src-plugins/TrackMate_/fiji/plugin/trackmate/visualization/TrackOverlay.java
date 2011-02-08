package fiji.plugin.trackmate.visualization;

import ij.ImagePlus;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Color4f;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.visualization.HyperStackDisplayer.TrackOverlay;
import fiji.util.gui.AbstractAnnotation;

public class TrackOverlay extends AbstractAnnotation {
	protected ArrayList<Integer> X0 = new ArrayList<Integer>();
	protected ArrayList<Integer> Y0 = new ArrayList<Integer>();
	protected ArrayList<Integer> X1 = new ArrayList<Integer>();
	protected ArrayList<Integer> Y1 = new ArrayList<Integer>();
	protected ArrayList<Integer> frames = new ArrayList<Integer>();
	protected float lineThickness = 1.0f;
	private SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph;
	private SpotCollection spots;
	private List<Set<Spot>> tracks;
	private Map<Set<Spot>, Color4f> colors;
	private double radius;

	public TrackOverlay(
			SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph, 
			SpotCollection spots, 
			List<Set<Spot>> tracks, 
			Map<Set<Spot>, Color4f> colors, 
			double radius) {
		this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
		this.graph = graph;
		this.spots = spots;
		this.tracks = tracks;
		this.colors = colors;
		this.radius = radius;
		prepareWholeTrackOverlay();
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

	
}