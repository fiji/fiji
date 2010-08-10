/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
    This file is part of the ImageJ plugins "Simple Neurite Tracer"
    and "Three Pane Crop".

    The ImageJ plugins "Three Pane Crop" and "Simple Neurite Tracer"
    are free software; you can redistribute them and/or modify them
    under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    The ImageJ plugins "Simple Neurite Tracer" and "Three Pane Crop"
    are distributed in the hope that they will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
    License for more details.

    In addition, as a special exception, the copyright holders give
    you permission to combine this program with free software programs or
    libraries that are released under the Apache Public License. 

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package stacks;

import ij.*;
import ij.gui.*;

import java.awt.*;
import java.awt.event.*;

public class ThreePanesCanvas extends ImageCanvas {

	protected PaneOwner owner;
	protected int plane;

	protected ThreePanesCanvas( ImagePlus imagePlus, PaneOwner owner, int plane ) {
		super(imagePlus);
		this.owner = owner;
		this.plane = plane;
	}

	protected ThreePanesCanvas( ImagePlus imagePlus, int plane) {
		super(imagePlus);
		this.plane = plane;
	}

	static public Object newThreePanesCanvas( ImagePlus imagePlus, PaneOwner owner, int plane ) {
		return new ThreePanesCanvas( imagePlus, owner, plane );
	}

	public void setPaneOwner(PaneOwner owner) {
		this.owner = owner;
	}

	protected void drawOverlay( Graphics g ) {

		if( draw_crosshairs ) {

			int ix = (int)Math.round( current_x );
			int iy = (int)Math.round( current_y );
			int iz = (int)Math.round( current_z );

			if( plane == ThreePanes.XY_PLANE ) {
				int x = myScreenXD(current_x);
				int y = myScreenYD(current_y);
				drawCrosshairs( g, Color.red, x, y );
			} else if( plane == ThreePanes.XZ_PLANE ) {
				int x = myScreenXD(current_x);
				int y = myScreenYD(current_z);
				drawCrosshairs( g, Color.red, x, y );
			} else if( plane == ThreePanes.ZY_PLANE ) {
				int x = myScreenXD(current_z);
				int y = myScreenYD(current_y);
				drawCrosshairs( g, Color.red, x, y );
			}
		}
	}

	public void paint(Graphics g) {
		super.paint(g);
		drawOverlay(g);
	}

	public void mouseClicked( MouseEvent e ) {

	}

	public void mouseMoved(MouseEvent e) {

		super.mouseMoved(e);

		double off_screen_x = offScreenX(e.getX());
		double off_screen_y = offScreenY(e.getY());

		boolean shift_key_down = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;

		owner.mouseMovedTo( (int)off_screen_x, (int)off_screen_y, plane, shift_key_down );
	}


	// ------------------------------------------------------------------------

	protected void drawCrosshairs( Graphics g, Color c, int x_on_screen, int y_on_screen ) {
		g.setColor( c );
		int hairLength = 8;
		g.drawLine( x_on_screen, y_on_screen + 1, x_on_screen, y_on_screen + (hairLength - 1) );
		g.drawLine( x_on_screen, y_on_screen - 1, x_on_screen, y_on_screen - (hairLength - 1) );
		g.drawLine( x_on_screen + 1, y_on_screen, x_on_screen + (hairLength - 1), y_on_screen );
		g.drawLine( x_on_screen - 1, y_on_screen, x_on_screen - (hairLength - 1), y_on_screen );
	}

	public void setCrosshairs( double x, double y, double z, boolean display ) {
		current_x = x;
		current_y = y;
		current_z = z;
		draw_crosshairs = display;
	}

	private double current_x, current_y, current_z;
	boolean draw_crosshairs;

	// ------------------------------------------------------------------------

	/* These are the "a pixel is not a little square" versions of
	   these methods.  (It's not so easy to do anything about the
	   box filter reconstruction.)
	*/

	/**Converts a screen x-coordinate to an offscreen x-coordinate.*/
	public int myOffScreenX(int sx) {
		return srcRect.x + (int)((sx - magnification/2)/magnification);
	}

	/**Converts a screen y-coordinate to an offscreen y-coordinate.*/
	public int myOffScreenY(int sy) {
		return srcRect.y + (int)((sy - magnification/2)/magnification);
	}

	/**Converts a screen x-coordinate to a floating-point offscreen x-coordinate.*/
	public double myOffScreenXD(int sx) {
		return srcRect.x + (sx - magnification/2)/magnification;
	}

	/**Converts a screen y-coordinate to a floating-point offscreen y-coordinate.*/
	public double myOffScreenYD(int sy) {
		return srcRect.y + (sy - magnification/2)/magnification;
	}

	/**Converts an offscreen x-coordinate to a screen x-coordinate.*/
	public int myScreenX(int ox) {
		return  (int)Math.round((ox-srcRect.x)*magnification+magnification/2);
	}

	/**Converts an offscreen y-coordinate to a screen y-coordinate.*/
	public int myScreenY(int oy) {
		return  (int)Math.round((oy-srcRect.y)*magnification+magnification/2);
	}

	/**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
	public int myScreenXD(double ox) {
		return  (int)Math.round((ox-srcRect.x)*magnification+magnification/2);
	}

	/**Converts a floating-point offscreen x-coordinate to a screen x-coordinate.*/
	public int myScreenYD(double oy) {
		return  (int)Math.round((oy-srcRect.y)*magnification+magnification/2);
	}
}
