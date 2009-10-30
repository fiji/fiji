package siox;

import ij.IJ;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.ShapeRoiHelper;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

import fiji.util.gui.OverlayedImageCanvas.Overlay;

public class RoiOverlay implements Overlay {
	Roi roi = null;
	Composite composite = AlphaComposite.getInstance(AlphaComposite.DST_OVER);
	Color color = Roi.getColor();
	
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
		
		// TODO Auto-generated method stub
		Shape shape = ShapeRoiHelper.getShape(new ShapeRoi(roi), g, x, y, magnification);
		final Graphics2D g2d = (Graphics2D)g;
		g2d.setComposite( this.composite );
		g2d.setColor( this.color );
		g2d.draw(shape);
		
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
