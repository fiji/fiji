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
		private float radiusRatio = 1.0f;
		
		/*
		 * CONSTRUCTOR
		 */
		
		public SpotOverlay(final ImagePlus imp, final float[] calibration) {
			this.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
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
			
			// Deal with editing spot - we always draw it with its center at the current z, current t 
			// (it moves along with the current slice) 
			if (null != editingSpot) {
				g2d.setColor(SpotDisplayer.HIGHLIGHT_COLOR);
				g2d.setStroke(new BasicStroke((float) (2 / canvas.getMagnification()), 
						BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {1.5f, 1.5f} , 0));
				final float x = editingSpot.getFeature(Feature.POSITION_X);
				final float y = editingSpot.getFeature(Feature.POSITION_Y);
				final float radius = editingSpot.getFeature(Feature.RADIUS);
				g2d.drawOval(Math.round((x-radius*radiusRatio)/calibration[0]),
						Math.round((y-radius*radiusRatio)/calibration[1]) , 
						Math.round(2*radius*radiusRatio/calibration[0]), 
						Math.round(2*radius*radiusRatio/calibration[1]));		
			}

		}
		
		private final void drawSpot(final Graphics2D g2d, final Spot spot, final float zslice) {
			final float x = spot.getFeature(Feature.POSITION_X);
			final float y = spot.getFeature(Feature.POSITION_Y);
			final float z = spot.getFeature(Feature.POSITION_Z);
			final float dz2 = (z - zslice) * (z - zslice);
			final float radius = spot.getFeature(Feature.RADIUS)*radiusRatio;
			if (dz2 >= radius*radius)
				g2d.fillOval(Math.round(x/calibration[0]) - 2, Math.round(y/calibration[1]) - 2, 4, 4);
			else {
				final int apparentRadius = (int) Math.round( Math.sqrt(radius*radius - dz2) / calibration[0]); 
				g2d.drawOval(Math.round(x/calibration[0]) - apparentRadius, Math.round(y/calibration[1]) - apparentRadius, 
						2 * apparentRadius, 2 * apparentRadius);			
			}
		}

		
		
	}