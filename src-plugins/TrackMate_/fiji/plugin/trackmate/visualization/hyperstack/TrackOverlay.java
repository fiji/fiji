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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas.Overlay;

public class TrackOverlay implements Overlay {
	private float[] calibration;
	private ImagePlus imp;
	private HashMap<Spot, Color> edgeColors;
	private Collection<DefaultWeightedEdge> highlight = new HashSet<DefaultWeightedEdge>();
	private Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	private Map<String, Object> displaySettings;
	private TrackMateModel model;

	/*
	 * CONSTRUCTOR
	 */
	
	public TrackOverlay(final TrackMateModel model, final ImagePlus imp, final Map<String, Object> displaySettings) {
		this.model = model;
		this.calibration = model.getSettings().getCalibration();
		this.imp = imp;
		this.displaySettings = displaySettings;
		this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
		computeTrackColors();
	}
	
	/*
	 * PUBLIC METHODS
	 */

	public void computeTrackColors() {
		InterpolatePaintScale colorMap = (InterpolatePaintScale) displaySettings.get(TrackMateModelView.KEY_COLORMAP);
		List<Set<Spot>> tracks = new ConnectivityInspector<Spot, DefaultWeightedEdge>(model.getTrackGraph()).connectedSets();
		HashMap<Set<Spot>, Color> trackColors = new HashMap<Set<Spot>, Color>(tracks.size());
		int counter = 0;
		int ntracks = tracks.size();
		for(Set<Spot> track : tracks) {
			trackColors.put(track, colorMap.getPaint((float) counter / (ntracks-1)));
			counter++;
		}
		edgeColors = new HashMap<Spot, Color>(model.getFilteredSpots().getNSpots());
		Color color;
		for(Set<Spot> track : trackColors.keySet()) {
			color = trackColors.get(track);
			for(Spot spot : track)
				edgeColors.put(spot, color);
		}

	}
			
	public void setHighlight(Collection<DefaultWeightedEdge> edges) {
		this.highlight = edges;
	}

	@Override
	public void paint(final Graphics g, final int xcorner, final int ycorner, final double magnification) {
		boolean tracksVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_TRACKS_VISIBLE);
		final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = model.getTrackGraph();
		if (!tracksVisible  || graph == null)
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
		final int trackDisplayMode = (Integer) displaySettings.get(TrackMateModelView.KEY_TRACK_DISPLAY_MODE);
		Spot source, target;
		int frame;

		g2d.setStroke(new BasicStroke(2.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Set<DefaultWeightedEdge> edges = graph.edgeSet();
		final SpotCollection spots = model.getFilteredSpots();
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
		g2d.setColor(TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR);
		for (DefaultWeightedEdge edge : highlight) {
			source = graph.getEdgeSource(edge);
			target = graph.getEdgeTarget(edge);
			Integer iFrame = spots.getFrame(source);
			if (null != iFrame)
				drawEdge(g2d, source, target, iFrame, currentFrame, xcorner, ycorner, mag, TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE);
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
			final int xcorner, final int ycorner, final float magnification, final int localTrackDisplayMode) {
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
 
		// Track display mode.
		final int trackDisplayDepth = (Integer) displaySettings.get(TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH);
		switch (localTrackDisplayMode) {

		case TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE:
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
			g2d.drawLine(x0, y0, x1, y1);
			break;

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL: {
			final int frameDist = Math.abs(frame - currentFrame); 
			if (frameDist > trackDisplayDepth)
				return;
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - (float) frameDist / trackDisplayDepth));
			g2d.drawLine(x0, y0, x1, y1);
			break;
		}		

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD: {
			final int frameDist = frame - currentFrame; 
			if (frameDist < 0 || frameDist > trackDisplayDepth)
				return;
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - (float) frameDist / trackDisplayDepth));
			g2d.drawLine(x0, y0, x1, y1);
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD: {
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