package fiji.util.gui;

import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;

/**
 * Extension of ImageCanvas to allow multiple overlays
 * 
 * @author Ignacio Arganda-Carreras and Johannes Schindelin
 *
 */
public class OverlayedImageCanvas extends ImageCanvas {
	
	/** Generated serial version UID */
	private static final long serialVersionUID = -9005735333215207618L;
	protected Collection<Overlay> overlays;

	private int backBufferWidth;
	private int backBufferHeight;

	private Graphics backBufferGraphics;
	private Image backBufferImage;
	protected Composite backBufferComposite;
	
	public OverlayedImageCanvas(ImagePlus image) {
		super(image);
		overlays = new ArrayList<Overlay>();
	}

	public void addOverlay(Overlay overlay) {
		overlays.add(overlay);
	}

	/**
	 * Add the collection of overlays to the display list of this canvas
	 * @param overlays  the overlay collection to add
	 */
	public void addOverlay(Collection<Overlay> overlays) {
		overlays.addAll(overlays);
	}

	
	public void removeOverlay(Overlay overlay) {
		overlays.remove(overlay);
	}
	

	/**
	 * Remove all {@link Overlay} components from this canvas.
	 */
	public void clearOverlay() {
		overlays.clear();
	}

	public void paint(Graphics g) {
		
		if(backBufferWidth!=getSize().width ||
				backBufferHeight!=getSize().height ||
				backBufferImage==null ||
				backBufferGraphics==null)
			resetBackBuffer();
		
		final Rectangle src = getSrcRect();
		
				
		synchronized(this) {						
			super.paint(backBufferGraphics);								
			for (Overlay overlay : overlays)			
				overlay.paint(backBufferGraphics, src.x, src.y, magnification);	
			
			final Roi roi = super.imp.getRoi();
			if(roi != null)
				roi.draw(backBufferGraphics);
		}

		g.drawImage(backBufferImage,0,0,this);		
	}
	
	private void resetBackBuffer() {

		if(backBufferGraphics!=null){
			backBufferGraphics.dispose();
			backBufferGraphics=null;
		}

		if(backBufferImage!=null){
			backBufferImage.flush();
			backBufferImage=null;
		}

		backBufferWidth=getSize().width;
		backBufferHeight=getSize().height;

		backBufferImage=createImage(backBufferWidth,backBufferHeight);
	    backBufferGraphics=backBufferImage.getGraphics();

	}
	

	public interface Overlay {	
		/**
		 * Set the composite that will be used to paint this overlay.
		 */
		public void setComposite (Composite composite);
		/**
		 * Paint this overlay on the given graphic device.
		 * @param g  the graphic device provided by the {@link OverlayedImageCanvas}
		 * @param x  the top-left corner x-coordinate of the image rectangle currently displayed in the {@link OverlayedImageCanvas}
		 * @param y  the top-left corner y-coordinate  
		 * @param magnification  the {@link OverlayedImageCanvas} current magnification
		 */
		void paint(Graphics g, int x, int y, double magnification);
	}
}
