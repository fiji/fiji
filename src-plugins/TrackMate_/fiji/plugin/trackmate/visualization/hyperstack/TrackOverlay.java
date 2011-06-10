package fiji.plugin.trackmate.visualization.hyperstack;

import ij.ImagePlus;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView.TrackDisplayMode;
import fiji.util.gui.OverlayedImageCanvas.Overlay;

public class TrackOverlay implements Overlay {
	private SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph;
	private SpotCollection spots;
	private float[] calibration;
	private ImagePlus imp;
	private HashMap<Spot, Color> edgeColors;
	private TrackMateModelView.TrackDisplayMode trackDisplayMode = TrackDisplayMode.ALL_WHOLE_TRACKS;
	private boolean trackVisible = true;
	private int trackDisplayDepth = 10;
	private Collection<DefaultWeightedEdge> highlight = new HashSet<DefaultWeightedEdge>();
	private Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

	/*
	 * CONSTRUCTOR
	 */
	
	public TrackOverlay(final ImagePlus imp, final float[] calibration) {
		this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
		this.calibration = calibration;
		this.imp = imp;
	}
	
	/*
	 * PUBLIC METHODS
	 */

	public void setTrackColor(final Map<Set<Spot>, Color> colors) {
		edgeColors = new HashMap<Spot, Color>(spots.getNSpots());
		Color color;
		for(Set<Spot> track : colors.keySet()) {
			color = colors.get(track);
			for(Spot spot : track)
				edgeColors.put(spot, color);
		}
	}
	
	/**
	 * Set the tracks that should be plotted by this class.
	 * <p>
	 * We require the corresponding {@link SpotCollection} to be set in the same time: the {@link SimpleWeightedGraph}
	 * contains the information about the edges of the track, but does not contain any information about
	 * what frame each edge belong to. This information is retrieved from the given {@link SpotCollection}, which
	 * must be made with the same {@link Spot} objects that of the vertices of the graph.
	 */
	public void setTrackGraph(final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph, final SpotCollection spots) {
		this.graph = graph;
		this.spots = spots;
	}
	
	public void setTrackVisible(boolean trackVisible) {
		this.trackVisible = trackVisible;
	}
	
	public void setHighlight(Collection<DefaultWeightedEdge> edges) {
		this.highlight = edges;
	}

	
	public void setDisplayTrackMode(TrackDisplayMode mode, int displayDepth) {
		this.trackDisplayMode = mode;
		this.trackDisplayDepth = displayDepth;
	}

	@Override
	public void paint(final Graphics g, final int xcorner, final int ycorner, final double magnification) {
		if (!trackVisible || graph == null)
			return;
		
		final Graphics2D g2d = (Graphics2D)g;
		// Save graphic device original settings
		final AffineTransform originalTransform = g2d.getTransform();
		final Composite originalComposite = g2d.getComposite();
		final Stroke originalStroke = g2d.getStroke();
		final Color originalColor = g2d.getColor();		
		
		g2d.setComposite(composite);
		final float mag = (float) magnification;
		final int currentFrame = imp.getFrame() - 1;
		Spot source, target;
		int frame;

		g2d.setStroke(new BasicStroke(2.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Set<DefaultWeightedEdge> edges = graph.edgeSet();
		for (DefaultWeightedEdge edge : edges) {
			if (highlight.contains(edge))
				continue;
			
			source = graph.getEdgeSource(edge);
			target = graph.getEdgeTarget(edge);
			// Find to what frame it belongs to
			frame = spots.getFrame(source);
			// Color
			g2d.setColor(edgeColors.get(source));
			// Draw
			drawEdge(g2d, source, target, frame, currentFrame, xcorner, ycorner, mag, trackDisplayMode);
		}

		// Deal with highlighted edges
		g2d.setStroke(new BasicStroke(4.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(TrackMateModelView.HIGHLIGHT_COLOR);
		for (DefaultWeightedEdge edge : highlight) {
			source = graph.getEdgeSource(edge);
			target = graph.getEdgeTarget(edge);
			Integer iFrame = spots.getFrame(source);
			if (null != iFrame)
				drawEdge(g2d, source, target, iFrame, currentFrame, xcorner, ycorner, mag, TrackDisplayMode.ALL_WHOLE_TRACKS);
		}
		
		// Restore graphic device original settings
		g2d.setTransform( originalTransform );
		g2d.setComposite(originalComposite);
		g2d.setStroke(originalStroke);
		g2d.setColor(originalColor);
	}


	/* 
	 * PRIVATE METHODS
	 */
	
	private final void drawEdge(final Graphics2D g2d, final Spot source, final Spot target, final int frame, final int currentFrame,
			final int xcorner, final int ycorner, final float magnification, final TrackDisplayMode localTrackDisplayMode) {
		// Find x & y in physical coordinates
		final float x0i = source.getFeature(Feature.POSITION_X);
		final float y0i = source.getFeature(Feature.POSITION_Y);
		final float x1i = target.getFeature(Feature.POSITION_X);
		final float y1i = target.getFeature(Feature.POSITION_Y);
		// In pixel units
		final float x0p = x0i / calibration[0];
		final float y0p = y0i / calibration[1];
		final float x1p = x1i / calibration[0];
		final float y1p = y1i / calibration[1];
		// Scale to image zoom
		final float x0s = (x0p - xcorner) * magnification ;
		final float y0s = (y0p - ycorner) * magnification ;
		final float x1s = (x1p - xcorner) * magnification ;
		final float y1s = (y1p - ycorner) * magnification ;
		// Round
		final int x0 = Math.round(x0s);
		final int y0 = Math.round(y0s);
		final int x1 = Math.round(x1s);
		final int y1 = Math.round(y1s);
 
		// Track display mode
		switch (localTrackDisplayMode) {

		case ALL_WHOLE_TRACKS:
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
			g2d.drawLine(x0, y0, x1, y1);
			break;

		case LOCAL_WHOLE_TRACKS: {
			final int frameDist = Math.abs(frame - currentFrame); 
			if (frameDist > trackDisplayDepth)
				return;
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - (float) frameDist / trackDisplayDepth));
			g2d.drawLine(x0, y0, x1, y1);
			break;
		}		

		case LOCAL_FORWARD_TRACKS: {
			final int frameDist = frame - currentFrame; 
			if (frameDist < 0 || frameDist > trackDisplayDepth)
				return;
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - (float) frameDist / trackDisplayDepth));
			g2d.drawLine(x0, y0, x1, y1);
			break;
		}

		case LOCAL_BACKWARD_TRACKS: {
			final int frameDist = currentFrame - frame; 
			if (frameDist <= 0 || frameDist > trackDisplayDepth)
				return;
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1  - (float) frameDist / trackDisplayDepth));
			g2d.drawLine(x0, y0, x1, y1);
			break;
		}
		}
	}

	@Override
	public void setComposite(Composite composite) {
		this.composite = composite;
	}

}