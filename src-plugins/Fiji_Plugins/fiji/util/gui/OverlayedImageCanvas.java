package fiji.util.gui;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;

import ij.ImagePlus;
import ij.gui.ImageCanvas;

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

	public OverlayedImageCanvas(ImagePlus image) {
		super(image);
		overlays = new ArrayList<Overlay>();
	}

	public void addOverlay(Overlay overlay) {
		overlays.add(overlay);
	}

	public void removeOverlay(Overlay overlay) {
		overlays.remove(overlay);
	}

	public void paint(Graphics g) {
		super.paint(g);
		Rectangle src = getSrcRect();
		for (Overlay overlay : overlays)
			overlay.paint(g, src.x, src.y, magnification);
	}

	public interface Overlay {
		void paint(Graphics g, int x, int y, double magnification);
	}
}
