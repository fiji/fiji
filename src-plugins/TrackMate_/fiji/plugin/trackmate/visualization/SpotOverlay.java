package fiji.plugin.trackmate.visualization;

import ij.ImagePlus;
import ij.gui.ImageCanvas;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.util.gui.AbstractAnnotation;

public class SpotOverlay extends AbstractAnnotation {

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
		private float radius;
		
		/*
		 * CONSTRUCTOR
		 */
		
		public SpotOverlay(final ImagePlus imp, final float[] calibration, final float radius) {
			this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
			this.imp = imp;
			this.calibration = calibration;
			this.canvas = imp.getCanvas();
			this.radius = radius;
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
		
		public void setRadius(float radius) {
			this.radius = radius;
		}
		
		@Override
		public void draw(Graphics2D g2d) {
			
			if (!spotVisible || null == target)
				return;
			
			final int frame = imp.getFrame()-1;
			final float zslice = (imp.getSlice()-1) * calibration[2];
			
			
			// Deal with normal spots.
			g2d.setStroke(new BasicStroke((float) (1 / canvas.getMagnification())));
			Color color;
			List<Spot> spots = target.get(frame);
			if (null == spots)
				return;
			for (Spot spot : spots) {
				
				if (editingSpot == spot || (spotSelection != null && spotSelection.contains(spot)))
					continue;
				
				color = targetColor.get(spot);
				if (null == color)
					color = SpotDisplayer.DEFAULT_COLOR;
				g2d.setColor(color);
				drawSpot(g2d, spot, zslice);
				
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
					drawSpot(g2d, spot, zslice);
				}
			}
			
			// Deal with edting spot
			if (null != editingSpot){
				g2d.setColor(SpotDisplayer.HIGHLIGHT_COLOR);
				g2d.setStroke(new BasicStroke((float) (2 / canvas.getMagnification()), 
						BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {4, 4} , 0));
				drawSpot(g2d, editingSpot, zslice);
			}

		}
		
		private final void drawSpot(final Graphics2D g2d, final Spot spot, final float zslice) {
			final float x = spot.getFeature(Feature.POSITION_X);
			final float y = spot.getFeature(Feature.POSITION_Y);
			final float z = spot.getFeature(Feature.POSITION_Z);
			final float dz2 = (z - zslice) * (z - zslice);
			if (dz2 >= radius*radius)
				g2d.fillOval(Math.round(x/calibration[0]) - 2, Math.round(y/calibration[1]) - 2, 4, 4);
			else {
				final int apparentRadius = (int) Math.round( Math.sqrt(radius*radius - dz2) / calibration[0]); 
				g2d.drawOval(Math.round(x/calibration[0]) - apparentRadius, Math.round(y/calibration[1]) - apparentRadius, 
						2 * apparentRadius, 2 * apparentRadius);			
			}
		}

		
		
	}