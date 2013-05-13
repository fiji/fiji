package fiji.plugin.trackmate.visualization.hyperstack;

import ij.ImagePlus;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas.Overlay;

/**
 * The overlay class in charge of drawing the tracks on the hyperstack window.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2011
 */
public class TrackOverlay implements Overlay {
	protected final double[] calibration;
	protected final ImagePlus imp;
	/** Map of color vs track key. */
//	protected Map<Integer, Color> edgeColors;
	protected Collection<DefaultWeightedEdge> highlight = new HashSet<DefaultWeightedEdge>();
	protected Map<String, Object> displaySettings;
	protected final TrackMateModel model;
	private TrackColorGenerator colorGenerator;

	/*
	 * CONSTRUCTOR
	 */

	public TrackOverlay(final TrackMateModel model, final ImagePlus imp, final Map<String, Object> displaySettings) {
		this.model = model;
		this.calibration = TMUtils.getSpatialCalibration(model.getSettings().imp);
		this.imp = imp;
		this.displaySettings = displaySettings;
	}

	/*
	 * PUBLIC METHODS
	 */

	public void setHighlight(Collection<DefaultWeightedEdge> edges) {
		this.highlight = edges;
	}

	@Override
	public final synchronized void paint(final Graphics g, final int xcorner, final int ycorner, final double magnification) {
		boolean tracksVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_TRACKS_VISIBLE);
		if (!tracksVisible  || model.getTrackModel().getNFilteredTracks() == 0)
			return;

		final Graphics2D g2d = (Graphics2D)g;
		// Save graphic device original settings
		final AffineTransform originalTransform = g2d.getTransform();
		final Composite originalComposite = g2d.getComposite();
		final Stroke originalStroke = g2d.getStroke();
		final Color originalColor = g2d.getColor();	
		Spot source, target;

		// Deal with highlighted edges first: brute and thick display
		g2d.setStroke(new BasicStroke(4.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR);
		for (DefaultWeightedEdge edge : highlight) {
			source = model.getTrackModel().getEdgeSource(edge);
			target = model.getTrackModel().getEdgeTarget(edge);
			drawEdge(g2d, source, target, xcorner, ycorner, magnification);
		}

		// The rest
		final int currentFrame = imp.getFrame() - 1;
		final int trackDisplayMode = (Integer) displaySettings.get(TrackMateModelView.KEY_TRACK_DISPLAY_MODE);
		final int trackDisplayDepth = (Integer) displaySettings.get(TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH);
		final Map<Integer,Set<DefaultWeightedEdge>> trackEdges = model.getTrackModel().getTrackEdges(); 
		final Set<Integer> filteredTrackKeys = model.getTrackModel().getFilteredTrackIDs();
		
		g2d.setStroke(new BasicStroke(2.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		if (trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL || trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK) 
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

		// Determine bounds for limited view modes
		int minT = 0;
		int maxT = 0;
		switch (trackDisplayMode) {
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK:
			minT = currentFrame - trackDisplayDepth;
			maxT = currentFrame + trackDisplayDepth;
			break;
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK:
			minT = currentFrame;
			maxT = currentFrame + trackDisplayDepth;
			break;
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK:
			minT = currentFrame - trackDisplayDepth;
			maxT = currentFrame;
			break;
		}

		float transparency;
		switch (trackDisplayMode) {

		case TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE: {
			for (Integer trackID : filteredTrackKeys) {
				final Set<DefaultWeightedEdge> track = trackEdges.get(trackID);
				colorGenerator.setCurrentTrackID(trackID);
				for (DefaultWeightedEdge edge : track) {
					if (highlight.contains(edge))
						continue;

					source = model.getTrackModel().getEdgeSource(edge);
					target = model.getTrackModel().getEdgeTarget(edge);
					g2d.setColor(colorGenerator.color(edge));
					drawEdge(g2d, source, target, xcorner, ycorner, magnification);
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK: 
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK: 
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK: {

			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

			for (Integer trackID : filteredTrackKeys) {
				colorGenerator.setCurrentTrackID(trackID);
				final Set<DefaultWeightedEdge> track= trackEdges.get(trackID);

				for (DefaultWeightedEdge edge : track) {
					if (highlight.contains(edge))
						continue;

					source = model.getTrackModel().getEdgeSource(edge);
					int sourceFrame = source.getFeature(Spot.FRAME).intValue();
					if (sourceFrame < minT || sourceFrame >= maxT)
						continue;

					target = model.getTrackModel().getEdgeTarget(edge);
					g2d.setColor(colorGenerator.color(edge));
					drawEdge(g2d, source, target, xcorner, ycorner, magnification);
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD: {

			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			for (Integer trackID : filteredTrackKeys) {
				colorGenerator.setCurrentTrackID(trackID);
				final Set<DefaultWeightedEdge> track= trackEdges.get(trackID);

				for (DefaultWeightedEdge edge : track) {
					if (highlight.contains(edge))
						continue;

					source = model.getTrackModel().getEdgeSource(edge);
					int sourceFrame = source.getFeature(Spot.FRAME).intValue();
					if (sourceFrame < minT || sourceFrame >= maxT)
						continue;

					transparency = (float) (1 - Math.abs(sourceFrame-currentFrame) / trackDisplayDepth);
					target = model.getTrackModel().getEdgeTarget(edge);
					g2d.setColor(colorGenerator.color(edge));
					drawEdge(g2d, source, target, xcorner, ycorner, magnification, transparency);
				}
			}
			break;

		}


		}

		// Restore graphic device original settings
		g2d.setTransform( originalTransform );
		g2d.setComposite(originalComposite);
		g2d.setStroke(originalStroke);
		g2d.setColor(originalColor);


	}

	/* 
	 * PROTECTED METHODS
	 */

	protected void drawEdge(final Graphics2D g2d, final Spot source, final Spot target,
			final int xcorner, final int ycorner, final double magnification, final float transparency) {
		// Find x & y in physical coordinates
		final double x0i = source.getFeature(Spot.POSITION_X);
		final double y0i = source.getFeature(Spot.POSITION_Y);
		final double x1i = target.getFeature(Spot.POSITION_X);
		final double y1i = target.getFeature(Spot.POSITION_Y);
		// In pixel units
		final double x0p = x0i / calibration[0] + 0.5f;
		final double y0p = y0i / calibration[1] + 0.5f;
		final double x1p = x1i / calibration[0] + 0.5f;
		final double y1p = y1i / calibration[1] + 0.5f;
		// Scale to image zoom
		final double x0s = (x0p - xcorner) * magnification ;
		final double y0s = (y0p - ycorner) * magnification ;
		final double x1s = (x1p - xcorner) * magnification ;
		final double y1s = (y1p - ycorner) * magnification ;
		// Round
		final int x0 = (int) Math.round(x0s);
		final int y0 = (int) Math.round(y0s);
		final int x1 = (int) Math.round(x1s);
		final int y1 = (int) Math.round(y1s);

		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));
		g2d.drawLine(x0, y0, x1, y1);

	}

	protected void drawEdge(final Graphics2D g2d, final Spot source, final Spot target,
			final int xcorner, final int ycorner, final double magnification) {
		// Find x & y in physical coordinates
		final double x0i = source.getFeature(Spot.POSITION_X);
		final double y0i = source.getFeature(Spot.POSITION_Y);
		final double x1i = target.getFeature(Spot.POSITION_X);
		final double y1i = target.getFeature(Spot.POSITION_Y);
		// In pixel units
		final double x0p = x0i / calibration[0] + 0.5f;
		final double y0p = y0i / calibration[1] + 0.5f;
		final double x1p = x1i / calibration[0] + 0.5f;
		final double y1p = y1i / calibration[1] + 0.5f; // so that spot centers are displayed on the pixel centers
		// Scale to image zoom
		final double x0s = (x0p - xcorner) * magnification ;
		final double y0s = (y0p - ycorner) * magnification ;
		final double x1s = (x1p - xcorner) * magnification ;
		final double y1s = (y1p - ycorner) * magnification ;
		// Round
		final int x0 = (int) Math.round(x0s);
		final int y0 = (int) Math.round(y0s);
		final int x1 = (int) Math.round(x1s);
		final int y1 = (int) Math.round(y1s);

		g2d.drawLine(x0, y0, x1, y1);

	}

	/**
	 * Ignored.
	 */
	@Override
	public void setComposite(Composite composite) {	}

	public void setTrackColorGenerator(TrackColorGenerator colorGenerator) {
		this.colorGenerator = colorGenerator;
	}

}