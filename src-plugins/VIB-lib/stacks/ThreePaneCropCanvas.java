/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
    This file is part of the ImageJ plugin "Three Pane Crop".

    The ImageJ plugin "Three Pane Crop" is free software; you can
    redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software
    Foundation; either version 3 of the License, or (at your option)
    any later version.

    The ImageJ plugin "Three Pane Crop" is distributed in the hope
    that it will be useful, but WITHOUT ANY WARRANTY; without even the
    implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
    PURPOSE.  See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package stacks;

import ij.*;
import ij.gui.*;

import java.awt.*;
import java.awt.event.*;

/* This represents one of the square handles of the crop region. */

class CropHandleRectangle {
	
	int plane;
	
	ImageCanvas c;
	
	public CropHandleRectangle( int in_plane, int side, ImageCanvas canvas ) {
		this.plane = in_plane;
		this.side = side;
		this.c = canvas;
	}
	
	int top_left_x;
	int top_left_y;
	
	int side;
	
	public void setTopLeft( int x, int y ) {
		this.top_left_x = x;
		this.top_left_y = y;
	}
	
	public void setSide( int side ) {
		this.side = side;
	}
	
	public int getSide( ) {
		return side;
	}
	
	public void draw( Graphics g ) {
		g.setColor( Color.MAGENTA );
		int real_side = (int)( side * c.getMagnification() );
		g.fillRect( c.screenX(top_left_x),
			    c.screenY(top_left_y),
			    real_side,
			    real_side );
		g.setColor( Color.BLUE );
		g.fillRect( c.screenX(top_left_x)+1,
			    c.screenY(top_left_y)+1,
			    real_side-2,
			    real_side-2 );
	}
	
	public boolean pointInside( int x, int y ) {
		return x >= top_left_x && x < top_left_x + side &&
			y >= top_left_y && y < top_left_y + side;
	}
	
}

public class ThreePaneCropCanvas extends ThreePanesCanvas {
	
	int crop_min_offscreen_x, crop_max_offscreen_x;
	int crop_min_offscreen_y, crop_max_offscreen_y;
	
	int offscreen_width;
	int offscreen_height;
	
	static final int HANDLE_NW = 1;
	static final int HANDLE_NE = 2;
	static final int HANDLE_SW = 3;
	static final int HANDLE_SE = 4;
	
	/* A value of 0 means that no handle is being dragged; non-zero
	   means that one of the above handles is: */
	
	int dragging;
	
	int x_pressed_at, y_pressed_at;
	
	int offset_x_in_handle, offset_y_in_handle;
	
	CropHandleRectangle nw, ne, sw, se;
	
	public ThreePaneCropCanvas( ImagePlus imagePlus, ThreePanes owner, int plane ) {
		
		super(imagePlus,owner,plane);
		
		offscreen_width = imagePlus.getWidth();
		offscreen_height = imagePlus.getHeight();
		
		crop_min_offscreen_x = 0;
		crop_max_offscreen_x = offscreen_width - 1;
		
		crop_min_offscreen_y = 0;
		crop_max_offscreen_y = offscreen_height - 1;
		
		nw = new CropHandleRectangle(plane,12,this);
		ne = new CropHandleRectangle(plane,12,this);
		sw = new CropHandleRectangle(plane,12,this);
		se = new CropHandleRectangle(plane,12,this);
		
		setHandlesFromBounds( );
		
	}
	
	private void setHandlesFromBounds( ) {
		
		nw.top_left_x = crop_min_offscreen_x;
		nw.top_left_y = crop_min_offscreen_y;
		
		ne.top_left_x = (crop_max_offscreen_x - ne.side) + 1;
		ne.top_left_y = crop_min_offscreen_y;
		
		sw.top_left_x = crop_min_offscreen_x;
		sw.top_left_y = (crop_max_offscreen_y - sw.side) + 1;
		
		se.top_left_x = (crop_max_offscreen_x - se.side) + 1;
		se.top_left_y = (crop_max_offscreen_y - se.side) + 1;
		
	}
	
	static public Object newThreePanesCanvas( ImagePlus imagePlus, ThreePaneCrop owner, int plane ) {
		return new ThreePaneCropCanvas( imagePlus, owner, plane );
	}

	public void setCropBounds( int offscreen_min_x, int offscreen_max_x,
				   int offscreen_min_y, int offscreen_max_y ) {
		
		crop_min_offscreen_x = offscreen_min_x;
		crop_max_offscreen_x = offscreen_max_x;
		crop_min_offscreen_y = offscreen_min_y;
		crop_max_offscreen_y = offscreen_max_y;
		
		setHandlesFromBounds( );
	}

	/* Keep another Graphics for double-buffering... */

	private int backBufferWidth;
	private int backBufferHeight;

	private Graphics backBufferGraphics;
	private Image backBufferImage;

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

	public void paint(Graphics g) {
		
		if(backBufferWidth!=getSize().width ||
		   backBufferHeight!=getSize().height ||
		   backBufferImage==null ||
		   backBufferGraphics==null)
			resetBackBuffer();
		
		super.paint(backBufferGraphics);
		drawOverlay(backBufferGraphics);
		nw.draw(backBufferGraphics);
		ne.draw(backBufferGraphics);
		sw.draw(backBufferGraphics);
		se.draw(backBufferGraphics);
		g.drawImage(backBufferImage,0,0,this);
	}
	
	protected void drawOverlay( Graphics g ) {
		
		super.drawOverlay( g );
		
		g.setColor( java.awt.Color.MAGENTA );
		
		g.drawLine( screenX(crop_min_offscreen_x),
			    screenY(0),
			    screenX(crop_min_offscreen_x),
			    screenY(offscreen_height) - 1 );
		
		g.drawLine( screenX(crop_max_offscreen_x + 1) - 1,
			    screenY(0),
			    screenX(crop_max_offscreen_x + 1) - 1,
			    screenY(offscreen_height) - 1 );
		
		g.drawLine( screenX(0),
			    screenY(crop_min_offscreen_y),
			    screenX(offscreen_width) - 1,
			    screenY(crop_min_offscreen_y) );
		
		g.drawLine( screenX(0),
			    screenY(crop_max_offscreen_y + 1) - 1,
			    screenX(offscreen_width) - 1,
			    screenY(crop_max_offscreen_y + 1) - 1 );
	}
	
	public void mousePressed(MouseEvent e) {
		
		/* Check whether in the handle the first click of a drag
		   was (and store that in offset_[xy]_in_handle).  Also 
		   record which handle is about to be dragged. */
		
		int x_pressed_at = offScreenX(e.getX());
		int y_pressed_at = offScreenY(e.getY());
		if( nw.pointInside( offScreenX(e.getX()), offScreenY(e.getY()) ) ) {
			dragging = HANDLE_NW;
			offset_x_in_handle = x_pressed_at - nw.top_left_x;
			offset_y_in_handle = y_pressed_at - nw.top_left_y;
		} else if( ne.pointInside( offScreenX(e.getX()), offScreenY(e.getY()) ) ) { 
			dragging = HANDLE_NE;
			offset_x_in_handle = (x_pressed_at - ne.top_left_x) - ne.side;
			offset_y_in_handle = y_pressed_at - ne.top_left_y;
		} else if( sw.pointInside( offScreenX(e.getX()), offScreenY(e.getY()) ) ) {
			dragging = HANDLE_SW;
			offset_x_in_handle = x_pressed_at - sw.top_left_x;
			offset_y_in_handle = (y_pressed_at - sw.top_left_y) - sw.side;
		} else if( se.pointInside( offScreenX(e.getX()), offScreenY(e.getY()) ) ) {
			dragging = HANDLE_SE;            
			offset_x_in_handle = (x_pressed_at - se.top_left_x) - se.side;
			offset_y_in_handle = (y_pressed_at - se.top_left_y) - se.side;
		}
	}
	
	public void mouseReleased(MouseEvent e) {
		dragging = 0;
	}
	
	public void mouseDragged(MouseEvent e) {
		((ThreePaneCrop)owner).handleDraggedTo(
			offScreenX(e.getX()) - offset_x_in_handle,
			offScreenY(e.getY()) - offset_y_in_handle,
			dragging,
			plane );
	}	
}
