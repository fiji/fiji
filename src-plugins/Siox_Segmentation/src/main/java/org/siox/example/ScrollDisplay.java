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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.Arrays;
import javax.swing.*;

import org.siox.*;

/**
 * Image display with zooming and scrolling capabilities.
 * <P>
 * Also keeps track of alpha values applied to the pixels
 * to model known foreground and known kbackgropund and
 * offerss methods to apply the SIOX segmentation and
 * detail refinement to the image.
 *
 * @author Lars Knipping
 * @version 1.01
 *
 */
public class ScrollDisplay extends JScrollPane
{
	// CHANGELOG
	// 2005-12-09 1.01 added rulers
	// 2005-11-22 1.00 initial,release

	/** Constant for showing no ruler at all (default). */
	public static final int NO_RULER   = -1;
	/** Constant for showing an empty ruler, just reserving space. */
	public static final int EMPTY_RULER   = JRuler.NO_UNITS;
	/** Constant for showing a metric uler, showing centimeter units. */
	public static final int METRIC_RULER = JRuler.METRIC_UNITS;
	/** Constant for showing an inch ruler. */
	public static final int INCH_RULER   = JRuler.INCH_UNITS;
	/** Constant for showing an pixel measuring ruler. */
	public static final int PIXEL_RULER  = JRuler.PIXEL_UNITS;

	/** The image displaying panel inside this scroll pane. */
	private final ImagePane imagePane;
	/** The segmentator to segmentate the image. */
	protected final SioxSegmentator siox;
	/** Pixel data of the image to be segmentated. */
	private final int[] imgData;
	/**
	 * The confidence matrix that stores the segmentation result.
	 *
	 * A confidence of 0 means the pixel is background,
	 * a confidence of 1.0 means the pixel is foreground.
	 */
	private final float[] confMatrix;
	/** Optional rulers as header component. */
	private JRuler colJRuler, rowJRuler;
	/** Rulers used as icons in imagePane when image is smaller as imagePane. */
	private JRuler innerColJRuler, innerRowJRuler;
	/** Ruler mode set. */
	private int rulerMode = NO_RULER;

	public ScrollDisplay(BufferedImage image)
	{
		super(VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_ALWAYS);
		final int imgWidth=image.getWidth();
		final int imgHeight=image.getHeight();
		// convert image to ARGB
		final BufferedImage img=
		  new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
		img.getGraphics().drawImage(image, 0, 0, null);

		// create components
		imagePane=new ImagePane(img);
		setViewportView(imagePane);

		// handle switching between inner and outer ruler on resize:
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				validateRulers();
				imagePane.repaint(); // inner rulers may need repaint
			}
		});

		// other elements:
		siox=new SioxSegmentator(imgWidth, imgHeight, null);
		confMatrix=new float[imgWidth*imgHeight];
		imgData=new int[imgWidth*imgHeight];
		img.getRGB(0, 0, imgWidth, imgHeight, imgData, 0, imgWidth);
		// fill confidence matrix with background only
		Arrays.fill(confMatrix, 0);
	}

	/**
	 * Sets the rulers for this scroll display.
	 * <P>
	 * Note that changing from no ruler to rulers or vice versa invalidates
	 * the layout.
	 *
	 * @param type one of the constants <CODE>NO_RULER</CODE>
	 *        <CODE>EMPTY_RULER</CODE>, <CODE>METRIC_RULER</CODE>,
	 *        <CODE>INCH_RULER</CODE>, and <CODE>PIXEL_RULER</CODE>.
	 * @return <CODE>true</CODE> if the layout was invalideated by this.
	 */
	public boolean setRuler(int type)
	{
		 rulerMode = type;
		final Dimension iSize = new Dimension(imagePane.getImageWidth(), imagePane.getImageHeight());
		getViewport().setPreferredSize(iSize); // prevent resize to zoomed image size on relayout
		if (type == NO_RULER) {
			final boolean changed = colJRuler != null;
			setColumnHeaderView(colJRuler = null);
			setRowHeaderView(rowJRuler = null);
			imagePane.setTopIcon(null);
			imagePane.setLeftIcon(null);
			return changed;
		}
		// outer rulers active or only inner ones?
		final int outerType = (imagePane.getZoomScale() < 0) ? EMPTY_RULER : type;
		final JRuler cols = colJRuler, rows = rowJRuler;
		final JRuler iCols = innerColJRuler, iRows = innerRowJRuler;
		if (cols==null || rows==null || iCols==null || iRows==null) {
			final JRuler c = new JRuler(this, SwingConstants.HORIZONTAL, outerType, 1000);
			final JRuler ic = new JRuler(this, SwingConstants.HORIZONTAL, type, 1000);
			final JRuler r = new JRuler(this, SwingConstants.VERTICAL, outerType, 1000);
			final JRuler ir = new JRuler(this, SwingConstants.VERTICAL, type, 1000);
			this.setColumnHeaderView(colJRuler = c);
			this.setRowHeaderView(rowJRuler = r);
			imagePane.setTopIcon(innerColJRuler = ic);
			imagePane.setLeftIcon(innerRowJRuler = ir);
			validateRulers();
			return true;
		} else {
			validateRulers();
			return false;
		}
	}

	 /**
	 * Applies the given value to the confidence matrix entries
	 * within the selected area.
	 */
	public void applySelection(float confidenceVal)
	{
		final Area area = imagePane.getSelectionArea();
		if (area != null) {
			setConf(area, confidenceVal);
			imagePane.updateImage(imgData, confMatrix);
		}
	}

	/** Sets given confidence to all image pixels. */
	public void setConf(float confVal) {
		Arrays.fill(confMatrix, confVal);
	}

	/**
	 * Applies the new confidence to the confidence matrix within the
	 * given area.
	 */
	public void setConf(Area area, float confVal)
	{
		final Rectangle r = area.getBounds();
		final int w = imagePane.getImageWidth();
		final int h = imagePane.getImageHeight();
		final int x0 = Math.max(0, r.x);
		final int y0 = Math.max(0, r.y);
		final int xTo = Math.min(w-1, r.x+r.width);
		final int yTo = Math.min(h-1, r.y+r.height);
		for (int y=y0; y<yTo; ++y) {
			for (int x=x0; x<xTo; ++x) {
				if (area.contains(x, y)) {
					confMatrix[y*w+x] = confVal;
				}
			}
		}
		imagePane.updateImage(imgData, confMatrix);
	}

	/**
	 * Segmentates the image for the current known background and known
	 *  foreground regions.
	 *
	 * @param smoothes Number of smoothing cycles applied.
	 * @param multipart Flag to allow not only the biggest connected
	 *        component.
	 * @exception IllegalStateException if no image foreground was defined.
	 */
	public void segmentate(int smoothes, boolean multipart)
	throws IllegalStateException
	{
		siox.segmentate(imgData, confMatrix, smoothes, multipart?4:0);
		imagePane.updateImage(imgData, confMatrix);
	}

	/*
	 * Refines the segmentation by modifying the alpha value for regions
	 * which have characteristics to both foreground and background if they
	 * fall into the specified square (<EM>Detail Refinement Brush</EM>).
	 *
	 *
	 * @param area Area in which the reworking of the segmentation is
	 *        applied to.
	 * @param add Flag for mode of the refinement applied:
	 *        add (only modify pixels formerly classified as background)
	 *        or substract (only modify pixels formerly classified as
	 *        foreground).
	 * @param threshold Threshold for the add and sub refinement, deciding
	 *        at the confidence level to stop at.
	 *
	 * @exception IllegalStateException if there is no segmentation yet to be
	 *            refined.
	 */
	public void subpixelRefine(Area area, boolean add, float thresh)
	throws IllegalStateException
	{
		final String opName = add
		  ? SioxSegmentator.ADD_EDGE : SioxSegmentator.SUB_EDGE;
		siox.subpixelRefine(area, opName, thresh, confMatrix);
		imagePane.updateImage(imgData, confMatrix);
	}

	/** Returns the image pane shown by this scrollable pane. */
	public ImagePane getImagePane()
	{
		return imagePane;
	}

	/**
	 * Sets a new zoom and adjusts the scrolling to retain the image center.
	 *
	 * @param zoomFactor the strictly positive zoom factor.
	 * @param zoomIn determies if the image should be shown zoomed in
	 *       (magnified) or zoomed out (scaled down).
	 */
	public void setZoom(int zoomFactor, boolean zoomIn)
	{
		final int vpW = viewport.getWidth();
		final int vpH = viewport.getHeight();
		final Point oldOff = viewport.getViewPosition();
		// center of viewport in image coordinates:
		final int cx = imagePane.unzoomX(oldOff.x + vpW/2);
		final int cy = imagePane.unzoomY(oldOff.y + vpH/2);
		// set zoom
		imagePane.setZoom(zoomFactor, zoomIn);
		final double scale = imagePane.getZoomScale();
		// calculate old center in new panel coordinates:
		final int maxOffX = imagePane.zoomX(imagePane.getImageWidth())-vpW;
		final int maxOffY = imagePane.zoomY(imagePane.getImageHeight())-vpH;
		final int offX =
		  Math.max(0, Math.min(maxOffX, imagePane.zoomX(cx)-vpW/2));
		final int offY =
		  Math.max(0, Math.min(maxOffY, imagePane.zoomY(cy)-vpH/2));
		final int rulerOffX = (int)
		  ((vpW-imagePane.getImageWidth()*scale)/2.+0.5);
		final int rulerOffY = (int)
		  ((vpH-imagePane.getImageHeight()*scale)/2.+0.5);
		// restore image center
		viewport.setViewPosition(new Point(offX, offY));
		validateRulers();
		repaint();
	}

	/**
	 * Switches between inner and outer ruler being active depending to
	 * viewport size and scaled image size.
	 */
	private void validateRulers()
	{
		final int rm = rulerMode;
		final JRuler cols = colJRuler, rows = rowJRuler;
		final JRuler iCols = innerColJRuler, iRows = innerRowJRuler;
		if (rm==NO_RULER || cols==null || rows==null || iCols==null || iRows==null) {
			return;
		}
		final double scale = imagePane.getZoomScale();
		if (imagePane.getInnerXMargin() < iRows.getIconWidth()) {
			rows.setUnit(rm);
			iRows.setUnit(EMPTY_RULER);
		} else { // enough space for inner ruler
			rows.setUnit(EMPTY_RULER);
			iRows.setUnit(rm);
		}
		if (imagePane.getInnerYMargin() < iCols.getIconHeight()) {
			cols.setUnit(rm);
			iCols.setUnit(EMPTY_RULER);
		} else { // enough space for inner ruler
			cols.setUnit(EMPTY_RULER);
			iCols.setUnit(rm);
		}
	}
}
