package siox;

import ij.process.ImageProcessor;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import fiji.util.gui.OverlayedImageCanvas.Overlay;

public class ImageOverlay implements Overlay{

	ImageProcessor imp = null;
	Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
	
	public ImageOverlay(){}
	
	public ImageOverlay(ImageProcessor imp){
		this.imp = imp;
	}
	
	@Override
	public void paint(Graphics g, int x, int y, double magnification) {
		if ( null == this.imp )
			return;
		
		Graphics2D g2d = (Graphics2D)g;		
		
		final AffineTransform orig = g2d.getTransform();
		final AffineTransform at = new AffineTransform();
		at.scale( magnification, magnification );
		at.translate( - x, - y );
		at.concatenate( orig );
		
		g2d.setTransform( at );
		
				
		g2d.setComposite(composite);
		g2d.drawImage(imp.getBufferedImage(), null, null);	
		
		g2d.setTransform( orig );
	}
	
	public void setComposite (Composite composite)
	{this.composite = composite;}
	
	public void setImage ( ImageProcessor imp)
	{this.imp = imp;}

}
