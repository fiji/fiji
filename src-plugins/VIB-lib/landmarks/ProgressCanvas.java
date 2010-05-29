/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package landmarks;

import ij.gui.StackWindow;
import ij.gui.ImageCanvas;
import ij.process.ColorProcessor;
import ij.*;

import java.awt.Graphics;
import java.awt.Color;

public class ProgressCanvas extends ImageCanvas {

	protected ImagePlus imagePlus;
	
	public ProgressCanvas( ImagePlus imagePlus ) {
		super( imagePlus );
		this.imagePlus = imagePlus;
	}

	protected void drawOverlay( Graphics g ) {
		
		int sliceZeroIndexed = imagePlus.getCurrentSlice() - 1;

		if( drawFixed && fixed_z == sliceZeroIndexed ) {
			int x = screenX(fixed_x);
			int y = screenY(fixed_y);
			int x_pixel_width = screenX(fixed_x+1) - x;
			int y_pixel_height = screenY(fixed_y+1) - y;
			drawCrosshairs( g, Color.magenta, x + (x_pixel_width / 2), y + (y_pixel_height / 2) );			
		}
		
		if( drawTransformed && transformed_z == sliceZeroIndexed ) {
			int x = screenX(transformed_x);
			int y = screenY(transformed_y);
			int x_pixel_width = screenX(transformed_x+1) - x;
			int y_pixel_height = screenY(transformed_y+1) - y;
			drawCrosshairs( g, Color.green, x + (x_pixel_width / 2), y + (y_pixel_height / 2) );			
		}
		
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		drawOverlay(g);
	}
	
	protected void drawCrosshairs( Graphics g, Color c, int x_on_screen, int y_on_screen ) {
		g.setColor( c );
		int hairLength = 8;
		g.drawLine( x_on_screen, y_on_screen + 1, x_on_screen, y_on_screen + (hairLength - 1) );
		g.drawLine( x_on_screen, y_on_screen - 1, x_on_screen, y_on_screen - (hairLength - 1) );
		g.drawLine( x_on_screen + 1, y_on_screen, x_on_screen + (hairLength - 1), y_on_screen );
		g.drawLine( x_on_screen - 1, y_on_screen, x_on_screen - (hairLength - 1), y_on_screen );
	}
	
	int fixed_x, fixed_y, fixed_z;
	int transformed_x, transformed_y, transformed_z;
	
	boolean drawFixed = false;
	boolean drawTransformed = false;

	public void setCrosshairs( int x, int y, int z, boolean fixed ) {

		System.out.println("Setting crosshairs (fixed: "+fixed+") to "+x+","+y+","+z);

		if( fixed ) {
			fixed_x = x;
			fixed_y = y;
			fixed_z = z;
			drawFixed = true;
		} else {
			transformed_x = x;
			transformed_y = y;
			transformed_z = z;
			drawTransformed = true;			
		}

	}
	
}
