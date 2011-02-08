package fiji.plugin.trackmate.visualization;

import ij.ImagePlus;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.visualization.SpotDisplayer.TrackDisplayMode;
import fiji.util.gui.AbstractAnnotation;

public class TrackOverlay extends AbstractAnnotation {
	protected float lineThickness = 1.0f;
	private SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph;
	private SpotCollection spots;
	private List<Set<Spot>> tracks;
	private float[] calibration;
	private ImagePlus imp;
	private HashMap<Spot, Color> edgeColors;
	private SpotDisplayer.TrackDisplayMode trackDisplayMode = TrackDisplayMode.ALL_WHOLE_TRACKS;
	private boolean trackVisible = true;
	private int trackDisplayDepth = 10;

	public TrackOverlay(
			final ImagePlus imp, 
			final float[] calibration, 
			final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph, 
			final SpotCollection spots, 
			final Map<Set<Spot>, Color> colors) {
		this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
		this.calibration = calibration;
		this.graph = graph;
		this.spots = spots;
		this.tracks = new ConnectivityInspector<Spot, DefaultWeightedEdge>(graph).connectedSets();
		setTrackColor(colors);
		this.imp = imp;
	}

	private void setTrackColor(Map<Set<Spot>, Color> colors) {
		edgeColors = new HashMap<Spot, Color>(spots.getNSpots());
		Color color;
		for(Set<Spot> track : colors.keySet()) {
			color = colors.get(track);
			for(Spot spot : track)
				edgeColors.put(spot, color);
		}
	}

	@Override
	public void draw(Graphics2D g2d) {
		if (null == tracks || !trackVisible)
			return;

		g2d.setStroke(new BasicStroke((float) (lineThickness /  imp.getCanvas().getMagnification()),  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		final int currentFrame = imp.getFrame() - 1;
		int frameDist;
		int x0, x1, y0, y1;
		Spot source, target;
		int frame;

		Set<DefaultWeightedEdge> edges = graph.edgeSet();
		for (DefaultWeightedEdge edge : edges) {
			// Find x & y
			source = graph.getEdgeSource(edge);
			target = graph.getEdgeTarget(edge);
			x0 = Math.round(source.getFeature(Feature.POSITION_X) / calibration[0]);
			y0 = Math.round(source.getFeature(Feature.POSITION_Y) / calibration[1]);
			x1 = Math.round(target.getFeature(Feature.POSITION_X) / calibration[0]);
			y1 = Math.round(target.getFeature(Feature.POSITION_Y) / calibration[1]);
			// Find to what frame it belongs to
			frame = spots.getFrame(source);
			// Color
			g2d.setColor(edgeColors.get(source));

			// Track display mode
			switch (trackDisplayMode ) {

			case ALL_WHOLE_TRACKS:
				g2d.drawLine(x0, y0, x1, y1);
				break;

			case LOCAL_WHOLE_TRACKS: {
				frameDist = Math.abs(frame - currentFrame); 
				if (frameDist > trackDisplayDepth)
					continue;
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - (float) frameDist / trackDisplayDepth));
				g2d.drawLine(x0, y0, x1, y1);
				break;
			}		

			case LOCAL_FORWARD_TRACKS: {
				frameDist = frame - currentFrame; 
				if (frameDist < 0 || frameDist > trackDisplayDepth)
					continue;
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - (float) frameDist / trackDisplayDepth));
				g2d.drawLine(x0, y0, x1, y1);
				break;
			}

			case LOCAL_BACKWARD_TRACKS: {
				frameDist = currentFrame - frame; 
				if (frameDist < 0 || frameDist > trackDisplayDepth)
					continue;
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1  - (float) frameDist / trackDisplayDepth));
				g2d.drawLine(x0, y0, x1, y1);
				break;
			}
			}


		}


	}


}