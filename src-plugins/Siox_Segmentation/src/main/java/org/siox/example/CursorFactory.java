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
import java.awt.geom.*;
import java.awt.image.*;

/**
 * Some static cursor creation utilities.
 *
 * @author Lars Knipping
 * @version 1.05
 */
public class CursorFactory
{
	// CHANGELOG
	// 2005-11-29 1.05 minor JavaDoc comments updates
	// 2005-11-22 1.04 added scale support
	// 2005-11-17 1.03 improved look of small disk cursors
	// 2005-11-14 1.02 added brushed area methods
	// 2005-11-10 1.01 minor comment updates
	// 2005-11-09 1.00 initial release

	/** Prevents outside instatiation. */
	private CursorFactory() {}

	/**
	 * Creates a filled square shaped cursor of given size with hotspot at the
	 * squares center.
	 *
	 * @param diameter Size (diameter) of the returned cursor.
	 * @param scale scale factor for the cursor circle. GFor values above one,
	 *       this not only increases the size of the cursor but also the
	 *       black and white "pixel" it is composed of.
	 * @param defaultCursor Cursor to be returned if requested cursor size
	 *        cannot be created on the underlying platform (at the time being
	 *        above 32 on Windows and above 64 on Linux) or is too small
	 *        to be visible (scaled diameter below two pixels diameter).
	 */
	public static Cursor createFilledSquareCursor(double diameter, double scale, Cursor defaultCursor)
	{
		final int size = (int) (diameter*scale);
		if (size < 2)
			return defaultCursor;
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension d = toolkit.getBestCursorSize(size, size);
		if (d.width<size || d.height<size) // well, bad luck
			return defaultCursor;
		final BufferedImage im =
		  new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
		final Graphics g = im.getGraphics();
		final int delta = Math.max(1, (int) scale);
		for (int i=0; i<3&&2*i*delta<size; ++i) {
			g.setColor((i%2==0) ? Color.white : Color.black);
			g.fillRect(i*delta, i*delta, size-2*i*delta, size-2*i*delta);
		}
		g.dispose();
		final Point hotspot = new Point(size/2, size/2);
		final String name = "FilledSquare"+size;
		return toolkit.createCustomCursor(im, hotspot, name);
	}

   /**
	 * Creates a filled circle shaped cursor of given size with hotspot at the
	 * disks center.
	 *
	 * @param diameter Size (diameter) of the returned cursor.
	 * @param scale scale factor for the cursor circle. GFor values above one,
	 *       this not only increases the size of the cursor but also the
	 *       black and white "pixel" it is composed of.
	 * @param defaultCursor Cursor to be returned if requested cursor size
	 *        cannot be created on the underlying platform (at the time being
	 *        above 32 on Windows and above 64 on Linux) or is too small
	 *        to be visible (scaled diameter below three pixels diameter).
	 */
	public static Cursor createDiskCursor(int diameter, float scale, Cursor defaultCursor)
	{
		// NOTE: odd diameters look much better here than with even ones
		final int size = (int) (diameter*scale+0.5);
		if (size <= 2)
			return defaultCursor;
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension d = toolkit.getBestCursorSize(size, size);
		if (d.width<size || d.height<size) // well, bad luck
			return defaultCursor;
		final BufferedImage im =
		  new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = (Graphics2D) im.getGraphics();
		// does not yield composition of scale-sized blocks:
		//g.transform(AffineTransform.getScaleInstance(scale, scale));
		//for (int i=0; i<3&&2*i<size; ++i) {
		//  g.setColor((i%2==0) ? Color.white : Color.black);
		//  g.fillOval(i, i, diameter-2*i, diameter-2*i);
		//}
		final BufferedImage diskImage = (scale<=1.0)
		  ? getImageWithDisk(size) : getImageWithDisk(diameter);
		g.drawImage(diskImage, 0, 0, size, size, null);
		g.dispose();
		final Point hotspot = new Point(size/2, size/2);
		final String name = "Disc"+size;
		return toolkit.createCustomCursor(im, hotspot, name);
	}

	private static BufferedImage getImageWithDisk(int size)
	{
		final BufferedImage im =
		  new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = (Graphics2D) im.getGraphics();
		// without antialiasing small circles drawn by Java look really ugly
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						   RenderingHints.VALUE_ANTIALIAS_ON);
		for (int i=0; i<3&&2*i<size; ++i) {
			g.setColor((i%2==0) ? Color.white : Color.black);
			g.fillOval(i, i, size-2*i, size-2*i);
		}
		g.dispose();
		return im;
	}

	/**
	 * Return the area covered by a rectangular brush moving in a
	 * straight line from <CODE>(x0, y0)</CODE> to <CODE>(x1, y1)</CODE>.
	 *<P>
	 * Mathemetically speaking, the returned area is the Minkowsky sum
	 * of the line segment <CODE>((x0, y0), (x1, y1))</CODE> with the
	 * axis parallel rectangle of size <CODE>(width, height)</CODE>
	 * and centered around the origin.
	 *
	 * @param x0 Start coordinate for brushing on x-axis.
	 * @param y0 Start coordinate for brushing on y-axis.
	 * @param x1 End coordinate for brushing on x-axis.
	 * @param y1 End coordinate for brushing on y-axis.
	 * @param width Horizontal diameter the cursor rectangle.
	 * @param height Vertical diameter the cursor rectangle.
	 */
	public static Area getAreaBrushedByRect(int x0, int y0, int x1, int y1, int width, int height)
	{
		if (x0 > x1)
			return getAreaBrushedByRect(x1, y1, x0, y0, width, height);
		 else if (x0==x1 && y0==y1)
			 return new Area(new Rectangle2D.Float(x0-width/2.F, y0-height/2.F, width, height));
		final GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 6);
		final float dx = width / 2.F;
		final float dy = height / 2.F;
		path.moveTo(x0-dx, y0-dy);
		path.lineTo(x0-dx, y0+dy);
		if (y0 <= y1) {
			path.lineTo(x1-dx, y1+dy);
			path.lineTo(x1+dx, y1+dy);
			path.lineTo(x1+dx, y1-dy);
			path.lineTo(x0+dx, y0-dy);
		} else {
			path.lineTo(x0+dx, y0+dy);
			path.lineTo(x1+dx, y1+dy);
			path.lineTo(x1+dx, y1-dy);
			path.lineTo(x1-dx, y1-dy);
		}
		path.closePath();
		return new Area(path);
	}

	/**
	 * Return the area covered by a disk shaped brush moving in a
	 * straight line from <CODE>(x0, y0)</CODE> to <CODE>(x1, y1)</CODE>.
	 *<P>
	 * Mathemetically speaking, the returned area is the Minkowsky sum
	 * of the line segment <CODE>((x0, y0), (x1, y1))</CODE> with the
	 * disk of diameter <CODE>diam</CODE> and centered around the origin.
	 *
	 * @param x0 Start coordinate for brushing on x-axis.
	 * @param y0 Start coordinate for brushing on y-axis.
	 * @param x1 End coordinate for brushing on x-axis.
	 * @param y1 End coordinate for brushing on y-axis.
	 * @param diam Diameter the cursor disk.
	 */
	public static Area getAreaBrushedByDisk(int x0, int y0, int x1, int y1, int diam)
	{
		if (x0 > x1)
			return getAreaBrushedByDisk(x1, y1, x0, y0, diam);
		else if (x0==x1 && y0==y1)
			return new Area(new Ellipse2D.Float(x0-diam/2.F, y0-diam/2.F, diam, diam));

		final float r = diam / 2.F;
		final Area circle0 = new Area(new Ellipse2D.Float(x0-r, y0-r, diam, diam));
		final Area circle1 = new Area(new Ellipse2D.Float(x1-r, y1-r, diam, diam));
		final float dx = x1 - x0, dy = y1 - y0;
		final double rScale = r / Math.sqrt(dx*dx + dy * dy);
		final float nx = (float) rScale*dy, ny = (float) -rScale*dx;
		final GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
		path.moveTo(x0+nx, y0+ny);
		path.lineTo(x0-nx, y0-ny);
		path.lineTo(x1-nx, y1-ny);
		path.lineTo(x1+nx, y1+ny);
		path.closePath();
		final Area area = new Area(path);
		area.add(circle0);
		area.add(circle1);
		return area;
	}
}
