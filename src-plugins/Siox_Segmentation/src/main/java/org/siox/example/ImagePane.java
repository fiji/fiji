/*
   Copyright 2005, 2006 by Gerald Friedland, Kristian Jantz and Lars Knipping

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package org.siox.example;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.Arrays;
import javax.swing.*;
import javax.imageio.ImageIO;

import org.siox.util.*;

/**
 * GUI Component to display a BufferedImage with a zoom factor
 * and an optional selection.
 *
 * @author Kristian Jantz, Gerald Friedland, Lars Knipping
 * @version 1.11
 */
public class ImagePane extends JComponent
{
	// CHANGELOG
	// 2005-12-09 1.11 added inner ruler support, fixed cropping inaccuracies
	// 2005-11-30 1.10 fixed one pixel offset in unzoom-methods for zoomIn
	// 2005-11-29 1.09 introduced elliptic and lasso selection
	// 2005-11-28 1.08 fixed clip in paintComponent for max coordinates
	//                 and unzoom coordinated out of range
	// 2005-11-24 1.07 added bg tiles feature
	// 2005-11-22 1.06 added zoom support
	// 2005-11-15 1.05 minor cleanups
	// 2005-11-10 1.04 minor comment updated
	// 2005-11-04 1.03 support for save reminder, XOR Drawing of selection
	// 2005-11-03 1.02 renamed some variables, minor fixes
	// 2005-11-03 1.01 added comments, added final modifier to some vars
	// 2005-10-25 1.00 initial release

	/** Constant for rectangle shaped selections. */
	public final static String RECTANGLE_SELECTION = "Rectangle";
	/** Constant for ellipse shaped selections. */
	public final static String ELLIPSE_SELECTION = "Ellipse";
	/** Constant for lasso type selections. */
	public final static String LASSO_SELECTION = "Lasso";

	/** Local copy of original image, used for reset. */
	private final BufferedImage origImage;
	/** Working instance of image. */
	private final BufferedImage currImage;
	/** Image dimension. */
	private final int iWidth, iHeight;
	/** Temporary ARGB arry for alpha blending operations on image. */
	private final int[] tmpBuffer;
	/**
	 * Current selection, a <CODE>Rectangle2D</CODE>, a <CODE>Ellipse2D</CODE>,
	 * or a general path enclosing the current selection (lasso selection).
	 * Set to <CODE>null</CODE> when not visible.
	 */
	private Shape selection=null;
	/** Start point of current selection. */
	private Point2D selectionAnchor = new Point2D.Float(0.F, 0.F);
	/** Image has unsaved changes? */
	private boolean unsavedChanges=false;
	/** The scaling factor the image should be shown with, must be greater zero. */
	private int zoomFactor = 1;
	/** Scale up or scale down? */
	private boolean zoomIn = true;
	/** Background image tile as icon. */
	private Icon bgTileIcon = null;
	/**
	 * To be drawn next to image, if inner offset allows them to be lie
	 * fully within this component.
	 */
	private Icon topIcon, leftIcon; // used to display rulers inside this

	/**
	 * Constructs a new image pane for displaying the given image.
	 */
	public ImagePane(BufferedImage image)
	{
		currImage=image;
		iWidth=currImage.getWidth();
		iHeight=currImage.getHeight();
		tmpBuffer=new int[iWidth*iHeight];
		origImage=
		  new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_ARGB);
		origImage.getGraphics().drawImage(currImage, 0, 0, null);
		setZoom(1, true);
	}

	/**
	 * Returns horizontal image offset from centering if image is snmaller
	 * than component, zero otherwise.
	 */
	public int getInnerXMargin()
	{
		final double scale = (zoomIn) ? zoomFactor : (1.0/zoomFactor);
		return  Math.max(0, (int) ((getWidth()-iWidth*scale)/2.+0.5));
	}

	 /**
	 * Returns vertical image offset from centering if image is snmaller
	 * than component, zero otherwise.
	 */
	public int getInnerYMargin()
	{
		final double scale = (zoomIn) ? zoomFactor : (1.0/zoomFactor);
		return Math.max(0, (int) ((getHeight()-iHeight*scale)/2.+0.5));
	}

	/**
	 * Sets icon be drawn above image, if vertical inner offset allows
	 * it to be placed completely within this component.
	 */
	protected void setTopIcon(Icon topIcon) // used for ruler inside this
	{
		this.topIcon = topIcon;
	}

	/**
	 * Sets icon be drawn left to image, if horizontal inner offset allows
	 * it to be placed completely within this component.
	 */
	protected void setLeftIcon(Icon leftIcon) // used for ruler inside this
	{
		this.leftIcon = leftIcon;
	}

	/**
	 * Draws the component.
	 */
	public void paintComponent(Graphics graphics)
	{
		final Graphics2D g = (Graphics2D) graphics.create();
		// center if scaled image smaller than component
		final int offX = getInnerXMargin(), offY = getInnerYMargin();
		if (offX > 0) {
		  final Icon icon = leftIcon;
		  if (icon != null &&  offX>=icon.getIconWidth())
			icon.paintIcon(this, g, offX-icon.getIconWidth(), 0);
		}
		if (offY > 0) {
		  final Icon icon = topIcon;
		  if (icon != null && offY>=icon.getIconHeight())
			icon.paintIcon(this, g, 0, offY-icon.getIconHeight());
		}

		g.translate(offX, offY);
		if (zoomFactor != 1) {
			final double scale = (zoomIn) ? zoomFactor : (1.0/zoomFactor);
			g.transform(AffineTransform.getScaleInstance(scale, scale));
		}
		final Shape clip = g.getClip();
		if (clip != null) {
			final Rectangle2D r = new Rectangle2D.Double(0, 0, iWidth-1, iHeight-1);
			Rectangle2D.intersect(clip.getBounds2D(), r, r);
			g.setClip(r);
		} else
			g.setClip(0, 0, iWidth-1, iHeight-1);
		final Icon icon = bgTileIcon;
		if (icon!= null) { // fill bg with tiles
			final int iconWidth = icon.getIconWidth();
			final int iconHeight = icon.getIconHeight();
			for (int x=0; x<iWidth; x+=iconWidth)
				for (int y=0; y<iHeight; y+=iconHeight)
				  icon.paintIcon(this, g, x, y);
		}
		g.drawImage(currImage, 0, 0, null);
		final Shape shape = selection;
		if (shape != null) {
			g.setColor(Color.white);
			g.setXORMode(Color.black);
			g.draw(shape);
			if (shape instanceof GeneralPath) {
				final Point2D p = ((GeneralPath) shape).getCurrentPoint();
				if (!selectionAnchor.equals(p)) {
					// close path with dashed line
					final float[] dash = new float[] {3.0F, 5.0F };
					g.setStroke(new BasicStroke(1.0F, BasicStroke.CAP_SQUARE,
												BasicStroke.JOIN_MITER, 10.0F,
												dash, 0.F));

					g.drawLine((int) selectionAnchor.getX(), (int) selectionAnchor.getY(),
							   (int) p.getX(), (int) p.getY());
				}
			}
		}
		g.dispose();
	}

	/**
	 * Selects to given point coordinates.
	 * <P>
	 * If current selection is a rectangle or an ellipse, the point
	 * defines the new corner opposite to the starting point.
	 * For lasso selection the point is added to the enclosing path.
	 * No effect if there is there is no current selection.
	 *
	 * @return if there is a current selection the point is added to.
	 */
	public boolean selectTo(int x, int y)
	{
		  final Shape shape = selection;
		if (shape == null) {
			return false;
		}
		if (shape instanceof GeneralPath) {
			((GeneralPath) shape).lineTo(x, y);
		} else {
			((RectangularShape) shape).setFrameFromDiagonal(selectionAnchor.getX(), selectionAnchor.getY(), x, y);
		}
		repaint();
		return true;
	}

	/**
	 * Start selection mode with first point at given coordinates.
	 *
	 * @param x horizontal start coordinate for selection.
	 * @param y vertical start coordinate for selection.
	 * @param selectionMode type of selection started, one of
	 *        <CODE>RECTANGLE_SELECTION</CODE>, <CODE>ELLIPSE_SELECTION</CODE>,
	 *        and <CODE>LASSO_SELECTION</CODE>.
	 * @exception IllegalArgumentException if the selectionMode is none on the
	 *        predefined selection modes.
	 * @see #RECTANGLE_SELECTION
	 * @see #ELLIPSE_SELECTION
	 * @see #LASSO_SELECTION
	 */
	public void startSelection(int x, int y, String selectionMode)
	{
		selectionAnchor.setLocation(x, y);
		if (RECTANGLE_SELECTION.equals(selectionMode)) {
			selection = new Rectangle2D.Float(x, y, 0.F, 0.F);
		} else if (ELLIPSE_SELECTION.equals(selectionMode)) {
			selection = new Ellipse2D.Float(x, y, 0.F, 0.F);
		} else if (LASSO_SELECTION.equals(selectionMode)) {
			final GeneralPath gp = new GeneralPath();
			gp.moveTo(x, y);
			selection = gp;
		} else {
		  throw new IllegalArgumentException("unknown selection mode: "
											 +selectionMode);
		}
	}

	/**
	 * Remove any selection.
	 */
	public void clearSelection()
	{
		selection=null;
		repaint();
	}

	 /**
	 * Returns the area of selection or <CODE>null<CODE> for no current selection.
	 */
	public Area getSelectionArea()
	{
		final Shape shape = selection;
		return (shape == null) ? null : new Area(shape);
	}

	/** Sets an icon as background image tile, <CODE>null</CODE> for none. */
	public void setBackgroundTile(Icon tileIcon)
	{
		this.bgTileIcon = tileIcon;
		repaint();
	}

	/**
	 * Sets the display zoom to the given factor, adjusting
	 * this components size as a side effect.
	 *
	 * @param factor the strictly positive zoom factor.
	 * @param zoomIn determies if the image should be shown zoomed in
	 *       (magnified) or zoomed out (scaled down).
	 */
	public void setZoom(int factor, boolean zoomIn)
	{
		if (zoomFactor < 1)
			throw new IllegalArgumentException("nonpositive zoom factor: "
											   +zoomFactor);
		  this.zoomFactor = factor;
		this.zoomIn = zoomIn;
		final int w = zoomIn
		  ? (iWidth*zoomFactor) : Math.max(1, (iWidth+iWidth-1)/zoomFactor);
		final int h = zoomIn
		  ? (iHeight*zoomFactor) : Math.max(1, (iHeight+iHeight-1)/zoomFactor);
		final Dimension size = new Dimension(Math.max(iWidth, w), Math.max(iHeight, h));
		setPreferredSize(size);
		setMinimumSize(size);
		setMaximumSize(size);
		setSize(size);
	}

	/** Returns the icon used background image tile, <CODE>null</CODE> for none. */
	public Icon getBackgroundTile()
	{
		return bgTileIcon;
	}

	/** Returns given zoom as a scaling factor. */
	public double getZoomScale()
	{
		return zoomIn ? ((double) zoomFactor) : (1.0/zoomFactor);
	}

	/** Returns the horizontal size of the unscaled image to be shown.. */
	public int getImageWidth()
	{
	  return iWidth;
	}

	/** Returns the vertical size of the unscaled image to be shown.. */
	public int getImageHeight()
	{
		return iHeight;
	}

	/** Applies the current zoom to a horizontal image space position. */
	public int zoomX(int x)
	{
		final double scale = (zoomIn) ? zoomFactor : (1.0/zoomFactor);
		final int off = (int) ((getWidth()-iWidth*scale)/2.+0.5);
		return off + (zoomIn ? (x*zoomFactor) : ((x+zoomFactor-1)/zoomFactor));
	}

	/** Applies the current zoom to a vertical image space position. */
	public int zoomY(int y)
	{
		final double scale = (zoomIn) ? zoomFactor : (1.0/zoomFactor);
		final int off = (int) ((getHeight()-iHeight*scale)/2.+0.5);
		return off + (zoomIn ? (y*zoomFactor) : ((y+zoomFactor-1)/zoomFactor));
	}

	/** Converts a zoomed horizontal position back to image space. */
	public int unzoomX(int x)
	{
		final double scale = (zoomIn) ? zoomFactor : (1.0/zoomFactor);
		x -= (int) ((getWidth()-iWidth*scale)/2.+0.5);
		final int imageX = !zoomIn
		  ? (x*zoomFactor+(zoomFactor-1)/2) : (x/zoomFactor);
		return Math.max(0, Math.min(imageX, iWidth-1));
	}

	/** Converts a zoomed vertical position back to image space. */
	public int unzoomY(int y)
	{
		final double scale = (zoomIn) ? zoomFactor : (1.0/zoomFactor);
		y -= (int) ((getHeight()-iHeight*scale)/2.+0.5);
		final int imageY = !zoomIn
		  ? (y*zoomFactor+(zoomFactor-1)/2) : (y/zoomFactor);
		return Math.max(0, Math.min(imageY, iHeight-1));
	}

	/**
	 * Reset image to original copy, clearing any selection.
	 */
	public void resetPane()
	{
		// reset selection
		selection=null;
		// avoid artifacts on transparent image parts by clearing image
		Arrays.fill(tmpBuffer, 0);
		currImage.setRGB(0, 0, iWidth, iHeight, tmpBuffer, 0, iWidth);
		// draw backup image to working copy
		currImage.getGraphics().drawImage(origImage, 0, 0, null);
		unsavedChanges=false; // well, your opinion might vary ...
		repaint();
	}

	/**
	 * Returns wether changes to image were made after last save.
	 */
	public boolean hasUnsavedChanges()
	{
		return unsavedChanges;
	}

	/**
	 * Sets image to given values.
	 *
	 * @param imgData RGB values for new image.
	 * @param cm Alpha-channel of new image given as confidence matrix.
	 */
	public void updateImage(int[] imgData, float[] cm)
	{
		for (int k=0; k<imgData.length; k++) {
		  tmpBuffer[k]=Utils.setAlpha(cm[k], imgData[k]);
	}
		currImage.setRGB(0, 0, iWidth, iHeight, tmpBuffer, 0, iWidth);
		unsavedChanges=true;
		repaint();
	}

	/**
	 * Stores current image as PNG to given file.
	 */
	public void storeCurrentImage(File outputFile) throws IOException
	{
		ImageIO.write(currImage, "PNG", outputFile);
		unsavedChanges=false;
	}
}
