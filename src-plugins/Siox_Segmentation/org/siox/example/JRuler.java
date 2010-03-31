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
import javax.swing.*;

/**
 * Ruler component for <CODE>ScrollDisplay</CODE>.
 *
 * @author Lars Knipping
 * @version 1.0
 */
public class JRuler extends JComponent implements Icon, SwingConstants {

	// CHANGELOG
	// 2005-12-09 1.00 initial release

	/** Screen resolution. */
	private static final int DOTS_PER_INCH =
	  Toolkit.getDefaultToolkit().getScreenResolution();
	/** Default font for labels. */
	private static final Font SUBTEXT_FONT =
	  UIManager.getLookAndFeelDefaults().getFont("Menu.acceleratorFont");
	/** Length for major tick marks. */
	private static final int MAJOR_TICKLENGTH = 10;
	/** Length for minor tick marks. */
	private static final int MINOR_TICKLENGTH = 5;

	/** Constant for showing an empty ruler, just reserving space. */
	public static final int NO_UNITS     = 0;
	/** Constant for showing a metric uler, showing centimeter units. */
	public static final int METRIC_UNITS = 1;
	/** Constant for showing an inch ruler. */
	public static final int INCH_UNITS   = 2;
	/** Constant for showing an pixel measuring ruler. */
	public static final int PIXEL_UNITS  = 3;

	/** The display this is connected to. */
	private final ScrollDisplay scrollDisplay;
	/** Vertical size for horizontal rulers and vice versa. */
	private int sizeForMarks;
	/** Maximum entry of this ruler in pixel. */
	private final int rulerLength;
	/** Orientation, <CODE>HORIZONTAL</CODE> or <CODE>VERTICAL</CODE>. */
	public final int orientation;
	/** Maximum supported scale factor. */
	private final int maxScale;
	/** What unit to show. */
	private int unitMode;

	/**
	 * Constructs a new ruler instance for use with scrollable areas and
	 * capable of scrolling.
	 *
	 * @param scrollDisplay the display this belongs to.
	 * @param orientation one of <CODE>SwingConstants.HORIZONTAL</CODE> and
	 *       <CODE>SwingConstants.VERTICAL</CODE>.
	 * @param unitMode ruler unit to show, one of the constants
	 *        <CODE>NO_UNITS</CODE>, <CODE>METRIC_UNITS</CODE>,
	 *        <CODE>INCH_UNITS</CODE>, and <CODE>PIXEL_UNITS</CODE>.
	 * @param maxScale the maximum scale factor this will be able to handle.
	 */
	protected JRuler(ScrollDisplay scrollDisplay, int orientation,
					 int unitMode, int maxScale)
	{
		this.scrollDisplay = scrollDisplay;
		this.orientation = orientation;
		this.maxScale = maxScale;
		if (orientation!=HORIZONTAL && orientation!=VERTICAL)
			throw new IllegalArgumentException("invalid orientation: "
											   +orientation);
		setUnit(unitMode);
		this.setFont(SUBTEXT_FONT);
		this.setForeground(UIManager.getColor("Label.foreground"));
		final FontMetrics fm = getFontMetrics(SUBTEXT_FONT);
		rulerLength = (orientation == HORIZONTAL)
		  ? scrollDisplay.getImagePane().getImageWidth()
		  : scrollDisplay.getImagePane().getImageHeight();
	}

	/** Returns size needed for max supported scale. */
	public Dimension getPreferredSize()
	{
		final FontMetrics fm = getFontMetrics(getFont());
		return (orientation == HORIZONTAL)
		  ? new Dimension(maxScale*rulerLength, sizeForMarks)
		  : new Dimension(sizeForMarks, maxScale*rulerLength);
	}

	/**
	 * Sets the units this ruler will show.
	 *
	 * @param unitMode ruler unit to show, one of the constants
	 *        <CODE>NO_UNITS</CODE>, <CODE>METRIC_UNITS</CODE>,
	 *        <CODE>INCH_UNITS</CODE>, and <CODE>PIXEL_UNITS</CODE>.
	 * @see #getUnit
	 */
	public void setUnit(int unitMode)
	{
		if (unitMode!=NO_UNITS && unitMode!=METRIC_UNITS
			&& unitMode!=INCH_UNITS && unitMode!=PIXEL_UNITS)
			throw new IllegalArgumentException("invalid mode: "+unitMode);
		this.unitMode = unitMode;
		repaint();
	}

	/**
	 * Returns the units this ruler shows.
	 *
	 * @see #getUnit
	 * @see #NO_UNITS
	 * @see #METRIC_UNITS
	 * @see #INCH_UNITS
	 * @see #PIXEL_UNITS
	 */
	public int getUnit()
	{
		return unitMode;
	}

	/** Sets the font to label major tick in. */
	public void setFont(Font font)
	{
		super.setFont(font);
		final FontMetrics fm = getFontMetrics(font);
		sizeForMarks = (orientation == HORIZONTAL)
		  ? (MAJOR_TICKLENGTH + 2 + fm.getHeight())
		  : (MAJOR_TICKLENGTH + 2 +
			 Math.max(fm.stringWidth("1888"), fm.stringWidth("0 pix")));
	}

	/** Paints this ruler component. */
	protected void paintComponent(Graphics g)
	{
		if (unitMode == NO_UNITS)
			return; // nothing to do
		final Rectangle rect = g.getClipBounds();
		final FontMetrics fm = g.getFontMetrics();

		final double scale = scrollDisplay.getImagePane().getZoomScale();
		if (scale > maxScale) // unsupported scale size
			return;

		final int pixPerMajorTick, unitsPerMajorTick;
		int minorPerMajorTicks;
		if (unitMode == METRIC_UNITS) { // label each centimeter
			pixPerMajorTick = (int) ((DOTS_PER_INCH*scale)/2.54);
			minorPerMajorTicks = 2;
			unitsPerMajorTick = 1;
		} else if (unitMode == INCH_UNITS) { // label each inch
			pixPerMajorTick = (int) (DOTS_PER_INCH*scale);
			minorPerMajorTicks = 4; // more minor ticks as cm
			unitsPerMajorTick = 1;
		} else {                    // pixel units
			if (scale < 4) {// label pixel at distance of 100 pixels
				pixPerMajorTick = (int) (100*scale);
				minorPerMajorTicks = 10;
				unitsPerMajorTick = 100;
			} else { // zoomed in, label pixel at distance of 10 pixels
				pixPerMajorTick = (int) (10*scale);
				minorPerMajorTicks = 10;
				unitsPerMajorTick = 10;
			}
		}
		if (pixPerMajorTick < 2) // scaled too strongly in to mark anything
			return;
		if (pixPerMajorTick / minorPerMajorTicks < 2)
			minorPerMajorTicks = 1; // minor ticks too dense, omit them

		// first and last tick locations determined by offsets
		final int paintOffset, valueOffset, pixLength;
		if (orientation == HORIZONTAL) {
			paintOffset = scrollDisplay.getImagePane().getInnerXMargin();
			valueOffset = (paintOffset > 0) ? 0 : scrollDisplay.getViewport().getViewPosition().x;
			pixLength = scrollDisplay.getViewport().getWidth();
		} else {
			paintOffset = scrollDisplay.getImagePane().getInnerYMargin();
			valueOffset =  (paintOffset > 0) ? 0 : scrollDisplay.getViewport().getViewPosition().y;
			pixLength = scrollDisplay.getViewport().getHeight();
		}
		final int pixPerMinorTick = Math.max(1, pixPerMajorTick/minorPerMajorTicks);
		final int minorStart = (valueOffset/pixPerMinorTick)*pixPerMinorTick;
		final int majorStart = (valueOffset/pixPerMajorTick)*pixPerMajorTick;
		final int rectEnd =
		  //(((valueOffset+rulerLength)/pixPerMinorTick)+1) * pixPerMinorTick;
		  (valueOffset/pixPerMinorTick+1) * pixPerMinorTick + pixLength;
		// not more than max ruler entry for scaled down images:
		final int end = Math.min(1+(int) (rulerLength*scale), rectEnd);
		// paint ticks and labels
		int nextLabel = Integer.MIN_VALUE;
		for (int pos=minorStart; pos<majorStart; pos+=pixPerMinorTick)
			drawTick(g, pos+paintOffset, MINOR_TICKLENGTH);
		for (int i=majorStart; i<end; i+=pixPerMajorTick) {
			final String text;
			final int n = (i/pixPerMajorTick)*unitsPerMajorTick;
			if (i+paintOffset >= nextLabel)
				nextLabel = drawLabel(g, i+paintOffset, n, unitsPerMajorTick);
			drawTick(g, i+paintOffset, MAJOR_TICKLENGTH);
			final int to = Math.min(i+pixPerMajorTick, end);
			for (int k=1; k<minorPerMajorTicks; ++k) {
				final int atUnit = n + (k*unitsPerMajorTick)/minorPerMajorTicks;
				final int pos = i+(k*pixPerMajorTick)/minorPerMajorTicks;
				if (pos > end)
					break;
				drawTick(g, pos+paintOffset, MINOR_TICKLENGTH);
			}
		}
	}

	/**
	 * Draws a label with given number for the given tick position, and
	 * returns minimum position of next tick where a label can be
	 * placed without intersecting this one.
	 */
	private int drawLabel(Graphics g, int tickPos, int no, int unitsPerTick)
	{
		final String label;
		final FontMetrics fm = g.getFontMetrics();
		if (no == 0) {
			label = (unitMode==METRIC_UNITS)?"0 cm":((unitMode==INCH_UNITS)?"0 in":"0 pix");
			// special case of 0: display the number within the jRuler + unit label
			if (orientation == HORIZONTAL) {
				g.drawString(label, tickPos, sizeForMarks-MAJOR_TICKLENGTH-3);
				return tickPos + fm.stringWidth(label+"_") + 2;
			} else {
				//int xoff = sizeForMarks-MAJOR_TICKLENGTH-3-fm.stringWidth(""+unitsPerTick);
				int xoff = sizeForMarks-(MAJOR_TICKLENGTH+3+fm.stringWidth(label));
				if (xoff+fm.stringWidth(label) >= sizeForMarks)
					xoff = sizeForMarks-fm.stringWidth(label)-1;
				final int yoff = fm.getAscent();
				g.drawString(label, Math.max(0, xoff), tickPos+yoff);
				return tickPos +fm.getHeight() + (yoff+1)/2 + 2;
			}
		} else {
			label = Integer.toString(no);
			if (orientation == HORIZONTAL) {
				final int off = fm.stringWidth(label)/2;
				g.drawString(label, tickPos-off, sizeForMarks-MAJOR_TICKLENGTH-3);
				return tickPos + fm.stringWidth(label+" ");
			} else {
				final int xoff = sizeForMarks-MAJOR_TICKLENGTH-3-fm.stringWidth(label);
				final int yoff = (g.getFontMetrics().getAscent()+1)/2;
				g.drawString(label, Math.max(0, xoff), tickPos+yoff);
				return tickPos + fm.getHeight();
			}
		}
	}

	/** Draws a tick at the given position. */
	private void drawTick(Graphics g, int pos, int length)
	{
		if (orientation == HORIZONTAL)
			g.drawLine(pos, sizeForMarks-1, pos, sizeForMarks-length-1);
		else
			g.drawLine(sizeForMarks-1, pos, sizeForMarks-length-1, pos);
	}

	/////////////////////////////////////////////////////////////////////
	// Icon interface methods for painting rulers inside ImagePane
	/////////////////////////////////////////////////////////////////////

	/** Returns width as icon. */
	public int getIconWidth()
	{
		return (orientation==HORIZONTAL)
		  ? rulerLength : getPreferredSize().width;
	}

	/** Returns height as icon. */
	public int getIconHeight() {
		return (orientation==HORIZONTAL)
		  ? getPreferredSize().height : rulerLength;
	}

	/** Draw the icon at the specified location. */
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		final Font f = g.getFont();
		g.translate(x, y);
		g.setFont(this.getFont());
		paintComponent(g);
		g.setFont(f);
		g.translate(-x, -y);
	}
}
