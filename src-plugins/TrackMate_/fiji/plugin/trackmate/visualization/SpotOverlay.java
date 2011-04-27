package fiji.plugin.trackmate.visualization;

import ij.ImagePlus;
import ij.gui.ImageCanvas;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.util.gui.OverlayedImageCanvas.Overlay;

public class SpotOverlay implements Overlay {

		/** The spot collection this annotation should draw. */
		protected SpotCollection target;
		/** The color mapping of the target collection. */
		protected Map<Spot, Color> targetColor;
		protected Spot editingSpot;
		protected Collection<Spot> spotSelection;
		protected boolean spotVisible = true;
		private ImagePlus imp;
		private float[] calibration;
		private ImageCanvas canvas;
		private float radiusRatio = 1.0f;
		private Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
		
		/*
		 * CONSTRUCTOR
		 */
		
		public SpotOverlay(final ImagePlus imp, final float[] calibration) {
			this.imp = imp;
			this.calibration = calibration;
			this.canvas = imp.getCanvas();
		}

		/*
		 * METHODS
		 */
		
		public void setSpotVisible(boolean spotVisible) {
			this.spotVisible = spotVisible;			
		}
		
		public void setEditedSpot(Spot spot) {
			this.editingSpot = spot;
		}
		
		public void setSpotSelection(Collection<Spot> spots) {
			this.spotSelection = spots;
		}
		
		public void setTarget(SpotCollection target) {
			this.target = target;
		}
		
		public void setTargettColor(Map<Spot, Color> colors) {
			this.targetColor = colors;
		}
		
		public void setRadiusRatio(float radiusRatio) {
			this.radiusRatio = radiusRatio;
		}
		
		@Override
		public void paint(Graphics g, int xcorner, int ycorner, double magnification) {
			
			if (!spotVisible || null == target)
				return;
			
			final Graphics2D g2d = (Graphics2D)g;
			g2d.setComposite(composite);
			final int frame = imp.getFrame()-1;
			final float zslice = (imp.getSlice()-1) * calibration[2];
			final float mag = (float) magnification;
			
			// Deal with normal spots.
			g2d.setStroke(new BasicStroke((float) (1 / magnification)));
			Color color;
			List<Spot> spots = target.get(frame);
			if (null != spots) { 
				for (Spot spot : spots) {

					if (editingSpot == spot || (spotSelection != null && spotSelection.contains(spot)))
						continue;

					color = targetColor.get(spot);
					if (null == color)
						color = SpotDisplayer.DEFAULT_COLOR;
					g2d.setColor(color);
					drawSpot(g2d, spot, zslice, xcorner, ycorner, mag);

				}
			}
			
			// Deal with spot selection
			if (null != spotSelection) {
				g2d.setStroke(new BasicStroke((float) (2 / canvas.getMagnification())));
				g2d.setColor(SpotDisplayer.HIGHLIGHT_COLOR);
				Integer sFrame;
				for(Spot spot : spotSelection) {
					sFrame = target.getFrame(spot);
					if (null == sFrame || sFrame != frame)
						continue;
					drawSpot(g2d, spot, zslice, xcorner, ycorner, mag);
				}
			}
			
			// Deal with editing spot - we always draw it with its center at the current z, current t 
			// (it moves along with the current slice) 
			if (null != editingSpot) {
				g2d.setColor(SpotDisplayer.HIGHLIGHT_COLOR);
				g2d.setStroke(new BasicStroke((float) (2 / canvas.getMagnification()), 
						BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {5f, 5f} , 0));
				final float x = editingSpot.getFeature(Feature.POSITION_X);
				final float y = editingSpot.getFeature(Feature.POSITION_Y);
				final float radius = editingSpot.getFeature(Feature.RADIUS) / calibration[0] * mag;
				// In pixel units
				final float xp = x / calibration[0];
				final float yp = y / calibration[1];
				// Scale to image zoom
				final float xs = (xp - xcorner) * mag ;
				final float ys = (yp - ycorner) * mag;
				g2d.drawOval(Math.round(xs-radius*radiusRatio), Math.round(ys-radius*radiusRatio), 
						Math.round(2*radius*radiusRatio), Math.round(2*radius*radiusRatio) );		
			}

		}
		
		private final void drawSpot(final Graphics2D g2d, final Spot spot, final float zslice, final int xcorner, final int ycorner, final float magnification) {
			final float x = spot.getFeature(Feature.POSITION_X);
			final float y = spot.getFeature(Feature.POSITION_Y);
			final float z = spot.getFeature(Feature.POSITION_Z);
			final float dz2 = (z - zslice) * (z - zslice);
			final float radius = spot.getFeature(Feature.RADIUS)*radiusRatio;
			// In pixel units
			final float xp = x / calibration[0];
			final float yp = y / calibration[1];
			// Scale to image zoom
			final float xs = (xp - xcorner) * magnification ;
			final float ys = (yp - ycorner) * magnification ;
			
			if (dz2 >= radius*radius)
				g2d.fillOval(Math.round(xs - 2*magnification), Math.round(ys - 2*magnification), Math.round(4*magnification), Math.round(4*magnification));
			else {
				final float apparentRadius =  (float) (Math.sqrt(radius*radius - dz2) / calibration[0] * magnification); 
				g2d.drawOval(Math.round(xs - apparentRadius), Math.round(ys - apparentRadius), 
						Math.round(2 * apparentRadius), Math.round(2 * apparentRadius));			
			}
		}

		@Override
		public void setComposite(Composite composite) {
			this.composite = composite;
		}
		
		
	}