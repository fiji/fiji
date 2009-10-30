package siox;

import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.ShapeRoiHelper;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import fiji.util.gui.OverlayedImageCanvas.Overlay;

public class RoiOverlay implements Overlay {
	Roi roi = null;
	Color color = Roi.getColor();
	Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	
	public RoiOverlay(){}
	
	public RoiOverlay(Roi roi, Composite composite, Color color)
	{
		setRoi( roi );
		setComposite( composite );
		setColor( color );
	}
	
	@Override
	public void paint(Graphics g, int x, int y, double magnification) {
		if ( null == this.roi )
			return;
		Shape shape = ShapeRoiHelper.getShape(new ShapeRoi(roi));
		final Rectangle roiBox = roi.getBounds();
		final Graphics2D g2d = (Graphics2D)g;
		final AffineTransform orig = g2d.getTransform();
		final AffineTransform at = new AffineTransform();
		at.scale( magnification, magnification );
		at.translate( roiBox.x - x, roiBox.y - y );
		at.concatenate( orig );
		
		g2d.setTransform( at );
		g2d.setComposite( this.composite );
		g2d.setColor( this.color );
		g2d.fill(shape);
		
		g2d.setTransform( orig );
	}
	
	public void setRoi(Roi roi)
	{
		this.roi = roi;
	}
	
	public void setComposite (Composite composite)
	{this.composite = composite;}
	
	public void setColor(Color color)
	{this.color = color;}
	
}
