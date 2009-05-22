package fiji.utilities.gui;

/*
 * This code is a modified form of the standard ImageCanvas which comes
 * with ImageJ, but modified to extend JPanel rather than Canvas.
 * 
 * Mixing AWT and Swing Objects was causing no end of headaches, so
 * keeping it all swing makes life much easier.
 * 
 * I've taken out a lot of the functionality of the original ImageCanvas
 * since I don't need it for this application.  I may even strip out
 * more code, since I probably don't need everything I've currently got.
 * 
 * This code was originally public domain, and I will release the
 * modifications I've made to it under the same conditions.
 * 
 * Simon Andrews 21/04/2008
 */

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;

import ij.util.Java2;
import ij.util.Tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.JPanel;

/** This is a Canvas used to display images in a Window. */
public class JImagePanel extends JPanel implements Cloneable {

	protected static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
	protected static Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
	protected static Cursor moveCursor = new Cursor(Cursor.MOVE_CURSOR);
	protected static Cursor crosshairCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);

	public static boolean usePointer = Prefs.usePointerCursor;

	protected ImagePlus imp;
	protected boolean imageUpdated;
	protected Rectangle srcRect;
	protected int imageWidth, imageHeight;

	private BasicStroke listStroke;
	private static Color showAllColor = new Color(128, 255, 255);
	private static Color labelColor;

	protected ImageJ ij;
	protected double magnification;
	protected int dstWidth, dstHeight;

	protected int xMouseStart;
	protected int yMouseStart;
	protected int xSrcStart;
	protected int ySrcStart;
	protected int flags;

	private Image offScreenImage;
	private int offScreenWidth = 0;
	private int offScreenHeight = 0;

	public JImagePanel(ImagePlus imp) {
		this.imp = imp;
		ij = IJ.getInstance();
		int width = imp.getWidth();
		int height = imp.getHeight();
		imageWidth = width;
		imageHeight = height;
		srcRect = new Rectangle(0, 0, imageWidth, imageHeight);
		setDrawingSize(imageWidth, (int)(imageHeight));
		magnification = 1.0;
		addKeyListener(ij);  // ImageJ handles keyboard shortcuts
		setFocusTraversalKeysEnabled(false);
	}

	void updateImage(ImagePlus imp) {
		this.imp = imp;
		int width = imp.getWidth();
		int height = imp.getHeight();
		imageWidth = width;
		imageHeight = height;
		srcRect = new Rectangle(0, 0, imageWidth, imageHeight);
		setDrawingSize(imageWidth, (int)imageHeight);
		magnification = 1.0;
	}

	/** Update this JImagePanel to have the same zoom and scale settings as the one specified. */
	void update(JImagePanel ic) {
		if (ic==null || ic==this || ic.imp==null)
			return;
		if (ic.imp.getWidth()!=imageWidth || ic.imp.getHeight()!=imageHeight)
			return;
		srcRect = new Rectangle(ic.srcRect.x, ic.srcRect.y, ic.srcRect.width, ic.srcRect.height);
		setMagnification(ic.magnification);
		setDrawingSize(ic.dstWidth, ic.dstHeight);
	}

	public void setDrawingSize(int width, int height) {
		dstWidth = width;
		dstHeight = height;
		setSize(dstWidth, dstHeight);
	}

	/** ImagePlus.updateAndDraw calls this method to get paint 
	  to update the image from the ImageProcessor. */
	public void setImageUpdated() {
		imageUpdated = true;
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		super.paint(g);
		try {
			if (imageUpdated) {
				imageUpdated = false;
				imp.updateImage();
			}
			Java2.setBilinearInterpolation(g, Prefs.interpolateScaledImages);
			Image img = imp.getImage();
			if (img!=null)
				g.drawImage(img, 0, 0, (int)(srcRect.width*magnification), (int)(srcRect.height*magnification),
						srcRect.x, srcRect.y, srcRect.x+srcRect.width, srcRect.y+srcRect.height, null);
		}
		catch(OutOfMemoryError e) {IJ.outOfMemory("Paint");}
	}

	int getSliceNumber(String label) {
		if (label==null) return -1;
		int slice = -1;
		if (label.length()>4 && label.charAt(4)=='-' && label.length()>=14)
			slice = (int)Tools.parseDouble(label.substring(0,4),-1);
		return slice;
	}

	void initGraphics(Graphics g, Color c) {
		if (labelColor==null) {
			int red = showAllColor.getRed();
			int green = showAllColor.getGreen();
			int blue = showAllColor.getBlue();
			if ((red+green+blue)/3<128)
				labelColor = Color.white;
			else
				labelColor = Color.black;
		}
		if (c!=null) {
			g.setColor(c);
			if (listStroke!=null) ((Graphics2D)g).setStroke(listStroke);
		} else
			g.setColor(showAllColor);
	}

	// Use double buffer to reduce flicker when drawing complex ROIs.
	// Author: Erik Meijering
	void paintDoubleBuffered(Graphics g) {
		final int srcRectWidthMag = (int)(srcRect.width*magnification);
		final int srcRectHeightMag = (int)(srcRect.height*magnification);
		if (offScreenImage==null || offScreenWidth!=srcRectWidthMag || offScreenHeight!=srcRectHeightMag) {
			offScreenImage = createImage(srcRectWidthMag, srcRectHeightMag);
			offScreenWidth = srcRectWidthMag;
			offScreenHeight = srcRectHeightMag;
		}
		try {
			if (imageUpdated) {
				imageUpdated = false;
				imp.updateImage();
			}
			Graphics offScreenGraphics = offScreenImage.getGraphics();
			Java2.setBilinearInterpolation(offScreenGraphics, Prefs.interpolateScaledImages);
			Image img = imp.getImage();
			if (img!=null)
				offScreenGraphics.drawImage(img, 0, 0, srcRectWidthMag, srcRectHeightMag,
						srcRect.x, srcRect.y, srcRect.x+srcRect.width, srcRect.y+srcRect.height, null);
			g.drawImage(offScreenImage, 0, 0, null);
		}
		catch(OutOfMemoryError e) {IJ.outOfMemory("Paint");}
	}

	long firstFrame;
	int frames, fps;

	public Dimension getPreferredSize() {
		return new Dimension(dstWidth, dstHeight);
	}

	int count;

	/** Returns the mouse event modifiers. */
	public int getModifiers() {
		return flags;
	}

	/** Sets the cursor based on the current tool and cursor location. */

	/**Converts a screen x-coordinate to an offscreen x-coordinate.*/
	public int offScreenX(int sx) {
		return srcRect.x + (int)(sx/magnification);
	}

	/**Converts a screen y-coordinate to an offscreen y-coordinate.*/
	public int offScreenY(int sy) {
		return srcRect.y + (int)(sy/magnification);
	}

	/**Converts a screen x-coordinate to a floating-point offscreen x-coordinate.*/
	public double offScreenXD(int sx) {
		return srcRect.x + sx/magnification;
	}

	/**Converts a screen y-coordinate to a floating-point offscreen y-coordinate.*/
	public double offScreenYD(int sy) {
		return srcRect.y + sy/magnification;

	}

	/**Converts an offscreen x-coordinate to a screen x-coordinate.*/
	public int screenX(int ox) {
		return  (int)((ox-srcRect.x)*magnification);
	}

	/**Converts an offscreen y-coordinate to a screen y-coordinate.*/
	public int screenY(int oy) {
		return  (int)((oy-srcRect.y)*magnification);
	}

	/**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
	public int screenXD(double ox) {
		return  (int)((ox-srcRect.x)*magnification);
	}

	/**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
	public int screenYD(double oy) {
		return  (int)((oy-srcRect.y)*magnification);
	}

	public double getMagnification() {
		return magnification;
	}

	public void setMagnification(double magnification) {
		setMagnification2(magnification);
	}

	void setMagnification2(double magnification) {
		if (magnification>32.0) magnification = 32.0;
		if (magnification<0.03125) magnification = 0.03125;
		this.magnification = magnification;
		imp.setTitle(imp.getTitle());
	}

	public Rectangle getSrcRect() {
		return srcRect;
	}

	void setSrcRect(Rectangle srcRect) {
		this.srcRect = srcRect;
	}

	void adjustSourceRect(double newMag, int x, int y) {
		int w = (int)Math.round(dstWidth/newMag);
		if (w*newMag<dstWidth) w++;
		int h = (int)Math.round(dstHeight/newMag);
		if (h*newMag<dstHeight) h++;
		x = offScreenX(x);
		y = offScreenY(y);
		Rectangle r = new Rectangle(x-w/2, y-h/2, w, h);
		if (r.x<0) r.x = 0;
		if (r.y<0) r.y = 0;
		if (r.x+w>imageWidth) r.x = imageWidth-w;
		if (r.y+h>imageHeight) r.y = imageHeight-h;
		srcRect = r;
		setMagnification(newMag);
	}
}
