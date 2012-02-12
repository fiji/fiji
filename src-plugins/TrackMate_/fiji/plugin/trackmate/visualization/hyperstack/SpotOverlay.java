package fiji.plugin.trackmate.visualization.hyperstack;

import ij.ImagePlus;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.renderer.InterpolatePaintScale;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas.Overlay;

/**
 * The overlay class in charge of drawing the spot images on the hyperstack window.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2011
 */
public class SpotOverlay implements Overlay {

	private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 12);
	private static final boolean DEBUG = false;

	/** The color mapping of the target collection. */
	protected Map<Spot, Color> targetColor;
	protected Spot editingSpot;
	protected ImagePlus imp;
	protected float[] calibration;
	protected Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	protected FontMetrics fm;
	protected Collection<Spot> spotSelection = new ArrayList<Spot>();
	protected Map<String, Object> displaySettings;
	protected TrackMateModel model;

	/*
	 * CONSTRUCTOR
	 */

	public SpotOverlay(final TrackMateModel model, final ImagePlus imp, final Map<String, Object> displaySettings) {
		this.model = model;
		this.imp = imp;
		this.calibration = model.getSettings().getCalibration();
		this.displaySettings = displaySettings; 
		computeSpotColors();
	}

	/*
	 * METHODS
	 */

	
	@Override
	public void paint(Graphics g, int xcorner, int ycorner, double magnification) {

		boolean spotVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_SPOTS_VISIBLE);
		if (!spotVisible  || null == model.getFilteredSpots())
			return;

		final Graphics2D g2d = (Graphics2D)g;
		// Save graphic device original settings
		final AffineTransform originalTransform = g2d.getTransform();
		final Composite originalComposite = g2d.getComposite();
		final Stroke originalStroke = g2d.getStroke();
		final Color originalColor = g2d.getColor();
		final Font originalFont = g2d.getFont();
		
		g2d.setComposite(composite);
		g2d.setFont(LABEL_FONT);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		fm = g2d.getFontMetrics();
		
		final int frame = imp.getFrame()-1;
		final float zslice = (imp.getSlice()-1) * calibration[2];
		final float mag = (float) magnification;

		// Deal with normal spots.
		g2d.setStroke(new BasicStroke(1.0f));
		Color color;
		final SpotCollection target = model.getFilteredSpots();
		List<Spot> spots = target .get(frame);
		if (null != spots) { 
			for (Spot spot : spots) {

				if (editingSpot == spot || (spotSelection  != null && spotSelection.contains(spot)))
					continue;

				color = targetColor.get(spot);
				if (null == color)
					color = AbstractTrackMateModelView.DEFAULT_COLOR;
				g2d.setColor(color);
				drawSpot(g2d, spot, zslice, xcorner, ycorner, mag);

			}
		}

		// Deal with spot selection
		if (null != spotSelection) {
			g2d.setStroke(new BasicStroke(2.0f));
			g2d.setColor(TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR);
			Integer sFrame;
			for(Spot spot : spotSelection) {
				sFrame = target.getFrame(spot);
				if (DEBUG)
					System.out.println("[SpotOverlay] For spot "+spot+" in selection, found frame "+sFrame);
				if (null == sFrame || sFrame != frame)
					continue;
				drawSpot(g2d, spot, zslice, xcorner, ycorner, mag);
			}
		}

		// Deal with editing spot - we always draw it with its center at the current z, current t 
		// (it moves along with the current slice) 
		if (null != editingSpot) {
			g2d.setColor(TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR);
			g2d.setStroke(new BasicStroke(1.0f,	BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {5f, 5f} , 0));
			final float x = editingSpot.getFeature(Spot.POSITION_X);
			final float y = editingSpot.getFeature(Spot.POSITION_Y);
			final float radius = editingSpot.getFeature(Spot.RADIUS) / calibration[0] * mag;
			// In pixel units
			final float xp = x / calibration[0] - 0.5f;
			final float yp = y / calibration[1] - 0.5f;
			// Scale to image zoom
			final float xs = (xp - xcorner) * mag ;
			final float ys = (yp - ycorner) * mag;
			float radiusRatio = (Float) displaySettings.get(TrackMateModelView.KEY_SPOT_RADIUS_RATIO);
			g2d.drawOval(Math.round(xs-radius*radiusRatio ), Math.round(ys-radius*radiusRatio), 
					Math.round(2*radius*radiusRatio), Math.round(2*radius*radiusRatio) );		
		}
		
		// Restore graphic device original settings
		g2d.setTransform( originalTransform );
		g2d.setComposite(originalComposite);
		g2d.setStroke(originalStroke);
		g2d.setColor(originalColor);
		g2d.setFont(originalFont);
	}
	
	public void computeSpotColors() {
		final String feature = (String) displaySettings.get(TrackMateModelView.KEY_SPOT_COLOR_FEATURE);
		// Get min & max
		float min = Float.POSITIVE_INFINITY;
		float max = Float.NEGATIVE_INFINITY;
		Float val;
		for (int ikey : model.getSpots().keySet()) {
			for (Spot spot : model.getSpots().get(ikey)) {
				val = spot.getFeature(feature);
				if (null == val)
					continue;
				if (val > max) max = val;
				if (val < min) min = val;
			}
		}
		targetColor = new HashMap<Spot, Color>( model.getSpots().getNSpots());
		for(Spot spot : model.getSpots()) {
			val = spot.getFeature(feature);
			InterpolatePaintScale  colorMap = (InterpolatePaintScale) displaySettings.get(TrackMateModelView.KEY_COLORMAP);
			if (null == feature || null == val)
				targetColor.put(spot, TrackMateModelView.DEFAULT_COLOR);
			else
				targetColor.put(spot, colorMap .getPaint((val-min)/(max-min)) );
		}
	}
	
	@Override
	public void setComposite(Composite composite) {
		this.composite = composite;
	}

	public void setSpotSelection(Collection<Spot> spots) {
		this.spotSelection = spots;
	}

	protected void drawSpot(final Graphics2D g2d, final Spot spot, final float zslice, final int xcorner, final int ycorner, final float magnification) {
		final float x = spot.getFeature(Spot.POSITION_X);
		final float y = spot.getFeature(Spot.POSITION_Y);
		final float z = spot.getFeature(Spot.POSITION_Z);
		final float dz2 = (z - zslice) * (z - zslice);
		float radiusRatio = (Float) displaySettings.get(TrackMateModelView.KEY_SPOT_RADIUS_RATIO);
		final float radius = spot.getFeature(Spot.RADIUS)*radiusRatio;
		// In pixel units
		final float xp = x / calibration[0] - 0.5f;
		final float yp = y / calibration[1] - 0.5f; // so that spot centers are displayed on the pixel centers
		// Scale to image zoom
		final float xs = (xp - xcorner) * magnification ;
		final float ys = (yp - ycorner) * magnification ;

		if (dz2 >= radius*radius)
			g2d.fillOval(Math.round(xs - 2*magnification), Math.round(ys - 2*magnification), Math.round(4*magnification), Math.round(4*magnification));
		else {
			final float apparentRadius =  (float) (Math.sqrt(radius*radius - dz2) / calibration[0] * magnification); 
			g2d.drawOval(Math.round(xs - apparentRadius), Math.round(ys - apparentRadius), 
					Math.round(2 * apparentRadius), Math.round(2 * apparentRadius));		
			boolean spotNameVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_DISPLAY_SPOT_NAMES);
			if (spotNameVisible ) {
				String str = spot.toString();
				int xindent = fm.stringWidth(str) / 2;
				int yindent = fm.getAscent() / 2;
				g2d.drawString(spot.toString(), xs-xindent, ys+yindent);
			}
		}
	}
	

	


}