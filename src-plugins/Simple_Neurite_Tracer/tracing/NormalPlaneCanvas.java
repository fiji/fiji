/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import ij.*;
import ij.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

class NormalPlaneCanvas extends ImageCanvas {

	HashMap<Integer,Integer> indexToValidIndex = new HashMap<Integer,Integer>();

	public NormalPlaneCanvas( ImagePlus imp,
				  SimpleNeuriteTracer plugin,
				  double [] centre_x_positions,
				  double [] centre_y_positions,
				  double [] radiuses,
				  double [] scores,
				  double [] modeRadiuses,
				  double [] angles,
				  boolean [] valid,
				  Path fittedPath ) {
		super(imp);
		tracerPlugin = plugin;
		this.centre_x_positions = centre_x_positions;
		this.centre_y_positions = centre_y_positions;
		this.radiuses = radiuses;
		this.scores = scores;
		this.modeRadiuses = modeRadiuses;
		this.angles = angles;
		this.valid = valid;
		this.fittedPath = fittedPath;
		int slices = imp.getStackSize();
		for( int i = 0; i < scores.length; ++i )
			if( scores[i] > maxScore )
				maxScore = scores[i];
		int a = 0;
		for( int i = 0; i < valid.length; ++i ) {
			if( valid[i] ) {
				indexToValidIndex.put(i,a);
				++a;
			}
		}
	}

	double maxScore = -1;

	double [] centre_x_positions;
	double [] centre_y_positions;
	double [] radiuses;
	double [] scores;
	double [] modeRadiuses;
	boolean [] valid;
	double [] angles;

	Path fittedPath;

	SimpleNeuriteTracer tracerPlugin;

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

	@Override
	public void paint(Graphics g) {

		if(backBufferWidth!=getSize().width ||
		   backBufferHeight!=getSize().height ||
		   backBufferImage==null ||
		   backBufferGraphics==null)
			resetBackBuffer();

		super.paint(backBufferGraphics);
		drawOverlay(backBufferGraphics);
		g.drawImage(backBufferImage,0,0,this);
	}

	int last_slice = -1;

	protected void drawOverlay(Graphics g) {

		int z = imp.getCurrentSlice() - 1;

		if( z != last_slice ) {
			Integer fittedIndex = indexToValidIndex.get(z);
			if( fittedIndex != null ) {
				int px = fittedPath.getXUnscaled(fittedIndex.intValue());
				int py = fittedPath.getYUnscaled(fittedIndex.intValue());
				int pz = fittedPath.getZUnscaled(fittedIndex.intValue());
				tracerPlugin.setSlicesAllPanes( px, py, pz );
				tracerPlugin.setCrosshair( px, py, pz );
				last_slice = z;
			}
		}

		if( valid[z] )
			g.setColor(Color.RED);
		else
			g.setColor(Color.MAGENTA);

		System.out.println("radiuses["+z+"] is: "+radiuses[z]);

		int x_top_left = screenXD( centre_x_positions[z] - radiuses[z] );
		int y_top_left = screenYD( centre_y_positions[z] - radiuses[z] );

		g.fillRect( screenXD(centre_x_positions[z])-2,
			    screenYD(centre_y_positions[z])-2,
			    5,
			    5 );

		int diameter = screenXD(centre_x_positions[z] + radiuses[z]) - screenXD(centre_x_positions[z] - radiuses[z]);

		g.drawOval( x_top_left, y_top_left, diameter, diameter );

		double proportion = scores[z] / maxScore;
		int drawToX = (int)( proportion * ( imp.getWidth() - 1 ) );
		if( valid[z] )
			g.setColor(Color.GREEN);
		else
			g.setColor(Color.RED);
		g.fillRect( screenX(0),
			    screenY(0),
			    screenX(drawToX) - screenX(0),
			    screenY(2) - screenY(0) );

		int modeOvalX = screenXD( imp.getWidth() / 2.0 - modeRadiuses[z] );
		int modeOvalY = screenYD( imp.getHeight() / 2.0 - modeRadiuses[z] );
		int modeOvalDiameter = screenXD( imp.getWidth() / 2.0 + modeRadiuses[z] ) - modeOvalX;

		g.setColor(Color.YELLOW);
		g.drawOval( modeOvalX,
			    modeOvalY,
			    modeOvalDiameter,
			    modeOvalDiameter );

		// Show the angle between this one and the other two
		// so we can see where the path is "pinched":
		g.setColor(Color.GREEN);
		double h = (imp.getWidth() * 3) / 8.0;
		double centreX = imp.getWidth() / 2.0;
		double centreY = imp.getHeight() / 2.0;
		double halfAngle = angles[z] / 2;
		double rightX = centreX + h * Math.sin(halfAngle);
		double rightY = centreY - h * Math.cos(halfAngle);
		double leftX = centreX + h * Math.sin(-halfAngle);
		double leftY = centreX - h * Math.cos(halfAngle);
		g.drawLine( screenXD(centreX),
			    screenYD(centreY),
			    screenXD(rightX),
			    screenYD(rightY) );
		g.drawLine( screenXD(centreX),
			    screenYD(centreY),
			    screenXD(leftX),
			    screenYD(leftY) );
	}

}
