package fiji.plugin.trackmate.visualization.hyperstack;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_COLORING;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij.ImagePlus;
import ij.gui.Roi;

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
import java.util.Iterator;
import java.util.Map;

/**
 * The overlay class in charge of drawing the spot images on the hyperstack window.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2011
 */
public class SpotOverlay extends Roi {

	private static final long serialVersionUID = 1L;
	private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 12);
	private static final boolean DEBUG = false;

	protected Spot editingSpot;
	protected final double[] calibration;
	protected Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	protected FontMetrics fm;
	protected Collection<Spot> spotSelection = new ArrayList<Spot>();
	protected Map<String, Object> displaySettings;
	protected final Model model;

	/*
	 * CONSTRUCTOR
	 */

	public SpotOverlay(final Model model, final ImagePlus imp, final Map<String, Object> displaySettings) {
		super(0, 0, imp);
		this.model = model;
		this.imp = imp;
		this.calibration = TMUtils.getSpatialCalibration(imp);
		this.displaySettings = displaySettings; 
	}

	/*
	 * METHODS
	 */


	@Override
	public void drawOverlay(Graphics g) {
		int xcorner = ic.offScreenX(0);
		int ycorner = ic.offScreenY(0);
		double magnification = getMagnification();
		SpotCollection spots = model.getSpots();
		
		boolean spotVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_SPOTS_VISIBLE);
		if (!spotVisible  || spots.getNSpots(true) == 0)
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

		final double zslice = (imp.getSlice()-1) * calibration[2];
		final double mag = magnification;
		final int frame = imp.getFrame()-1;

		// Deal with normal spots.
		@SuppressWarnings("unchecked")
		FeatureColorGenerator<Spot> colorGenerator = (FeatureColorGenerator<Spot>) displaySettings.get(KEY_SPOT_COLORING);
		g2d.setStroke(new BasicStroke(1.0f));
		for (Iterator<Spot> iterator = spots.iterator(frame, true); iterator.hasNext();) {
			Spot spot = iterator.next();

			if (editingSpot == spot || (spotSelection  != null && spotSelection.contains(spot)))
				continue;

			Color color = colorGenerator.color(spot);
			g2d.setColor(color);
			drawSpot(g2d, spot, zslice, xcorner, ycorner, mag);

		}

		// Deal with spot selection
		if (null != spotSelection) {
			g2d.setStroke(new BasicStroke(2.0f));
			g2d.setColor(TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR);
			for(Spot spot : spotSelection) {
				int sFrame = spot.getFeature(Spot.FRAME).intValue();
				if (DEBUG)
					System.out.println("[SpotOverlay] For spot "+spot+" in selection, found frame "+sFrame);
				if (sFrame != frame)
					continue;
				drawSpot(g2d, spot, zslice, xcorner, ycorner, mag);
			}
		}

		// Deal with editing spot - we always draw it with its center at the current z, current t 
		// (it moves along with the current slice) 
		if (null != editingSpot) {
			g2d.setColor(TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR);
			g2d.setStroke(new BasicStroke(1.0f,	BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {5f, 5f} , 0));
			final double x = editingSpot.getFeature(Spot.POSITION_X);
			final double y = editingSpot.getFeature(Spot.POSITION_Y);
			final double radius = editingSpot.getFeature(Spot.RADIUS) / calibration[0] * mag;
			// In pixel units
			final double xp = x / calibration[0] + 0.5d;
			final double yp = y / calibration[1] + 0.5d;
			// Scale to image zoom
			final double xs = (xp - xcorner) * mag ;
			final double ys = (yp - ycorner) * mag;
			double radiusRatio = (Float) displaySettings.get(TrackMateModelView.KEY_SPOT_RADIUS_RATIO);
			g2d.drawOval( (int) Math.round(xs-radius*radiusRatio ), (int) Math.round(ys-radius*radiusRatio), 
					(int) Math.round(2*radius*radiusRatio), 	(int) Math.round(2*radius*radiusRatio) );		
		}

		// Restore graphic device original settings
		g2d.setTransform( originalTransform );
		g2d.setComposite(originalComposite);
		g2d.setStroke(originalStroke);
		g2d.setColor(originalColor);
		g2d.setFont(originalFont);
	}

	public void setSpotSelection(Collection<Spot> spots) {
		this.spotSelection = spots;
	}

	protected void drawSpot(final Graphics2D g2d, final Spot spot, final double zslice, final int xcorner, final int ycorner, final double magnification) {
		final double x = spot.getFeature(Spot.POSITION_X);
		final double y = spot.getFeature(Spot.POSITION_Y);
		final double z = spot.getFeature(Spot.POSITION_Z);
		final double dz2 = (z - zslice) * (z - zslice);
		double radiusRatio = (Float) displaySettings.get(TrackMateModelView.KEY_SPOT_RADIUS_RATIO);
		final double radius = spot.getFeature(Spot.RADIUS) * radiusRatio;
		// In pixel units
		final double xp = x / calibration[0] + 0.5f;
		final double yp = y / calibration[1] + 0.5f; // so that spot centers are displayed on the pixel centers
		// Scale to image zoom
		final double xs = (xp - xcorner) * magnification ;
		final double ys = (yp - ycorner) * magnification ;

		if (dz2 >= radius*radius)
			g2d.fillOval((int) Math.round(xs - 2*magnification), (int) Math.round(ys - 2*magnification), 
					(int) Math.round(4*magnification), 	(int) Math.round(4*magnification));
		else {
			final double apparentRadius = Math.sqrt(radius*radius - dz2) / calibration[0] * magnification;
			g2d.drawOval((int) Math.round(xs - apparentRadius), (int) Math.round(ys - apparentRadius), 
					(int) Math.round(2 * apparentRadius), (int) Math.round(2 * apparentRadius));		
			boolean spotNameVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_DISPLAY_SPOT_NAMES);
			if (spotNameVisible ) {
				String str = spot.toString();
				
				int xindent = fm.stringWidth(str);
				int xtext = (int) (xs + apparentRadius + 5);
				if (xtext + xindent > imp.getWindow().getWidth()) {
					xtext = (int) (xs - apparentRadius - 5 - xindent);
				}

				int yindent = fm.getAscent() / 2;
				int ytext = (int) ys + yindent;;
				
				g2d.drawString(spot.toString(), xtext, ytext);
			}
		}
	}





}