/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010, 2011 Mark Longair */

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
import java.awt.*;
import java.awt.event.*;
import stacks.ThreePanes;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("serial")
public class InteractiveTracerCanvas extends TracerCanvas {

	static final boolean verbose = SimpleNeuriteTracer.verbose;

	boolean fillTransparent = false;

	Color transparentGreen = new Color( 0, 128, 0, 128 );

	public void setFillTransparent( boolean transparent ) {
		this.fillTransparent = transparent;
	}

	// -------------------------------------------------------------

	private SimpleNeuriteTracer tracerPlugin;

	public SimpleNeuriteTracer getTracerPlugin() {
		return tracerPlugin;
	}

	InteractiveTracerCanvas( ImagePlus imp, SimpleNeuriteTracer plugin, int plane, PathAndFillManager pathAndFillManager ) {
		super(imp,plugin,plane,pathAndFillManager);
		tracerPlugin = plugin;
		// SimpleNeuriteTracer.toastKeyListeners( IJ.getInstance(), "InteractiveTracerCanvas constructor" );
		// addKeyListener( this );
	}

	private Path unconfirmedSegment;
	private Path currentPath;
	private boolean lastPathUnfinished;

	public void setPathUnfinished( boolean unfinished ) {
		this.lastPathUnfinished = unfinished;
	}

	public void setTemporaryPath( Path path ) {
		this.unconfirmedSegment = path;
	}

	public void setCurrentPath( Path path ) {
		this.currentPath = path;
	}

	public void toggleJustNearSlices( ) {
		just_near_slices = ! just_near_slices;
	}

	public void fakeMouseMoved( boolean shift_pressed, boolean join_modifier_pressed ) {
		tracerPlugin.mouseMovedTo( last_x_in_pane_precise, last_y_in_pane_precise, plane, shift_pressed, join_modifier_pressed );
	}

	public void startShollAnalysis( ) {
		if( pathAndFillManager.anySelected() ) {
			double [] p = new double[3];
			tracerPlugin.findPointInStackPrecise( last_x_in_pane_precise, last_y_in_pane_precise, plane, p );
			PointInImage pointInImage = pathAndFillManager.nearestJoinPointOnSelectedPaths( p[0], p[1], p[2] );
			new ShollAnalysisDialog(
				"Sholl analysis for tracing of "+tracerPlugin.getImagePlus().getTitle(),
				pointInImage.x,
				pointInImage.y,
				pointInImage.z,
				pathAndFillManager,
				tracerPlugin.getImagePlus());
		} else {
			IJ.error("You must have a path selected in order to start Sholl analysis");
		}
	}

	public void selectNearestPathToMousePointer( boolean addToExistingSelection ) {

		if( pathAndFillManager.size() == 0 ) {
			IJ.error("There are no paths yet, so you can't select one with 'g'");
			return;
		}

		double [] p = new double[3];
		tracerPlugin.findPointInStackPrecise( last_x_in_pane_precise, last_y_in_pane_precise, plane, p );

		double diagonalLength = tracerPlugin.getStackDiagonalLength();

		/* Find the nearest point on any path - we'll
		   select that path... */

		NearPoint np = pathAndFillManager.nearestPointOnAnyPath( p[0] * tracerPlugin.x_spacing,
									 p[1] * tracerPlugin.y_spacing,
									 p[2] * tracerPlugin.z_spacing,
									 diagonalLength);

		if( np == null ) {
			IJ.error("BUG: No nearby path was found within "+diagonalLength+" of the pointer");
			return;
		}

		Path path = np.getPath();

		/* FIXME: in fact shift-G for multiple
		   selections doesn't work, since in ImageJ
		   that's a shortcut for taking a screenshot.
		   Holding down control doesn't work since
		   that's already used to restrict the
		   cross-hairs to the selected path.  Need to
		   find some way around this ... */

		tracerPlugin.selectPath( path, addToExistingSelection );
	}

	@Override
	public void mouseMoved( MouseEvent e ) {

		if( ! tracerPlugin.isReady() )
			return;

		int rawX = e.getX();
		int rawY = e.getY();

		last_x_in_pane_precise = myOffScreenXD(rawX);
		last_y_in_pane_precise = myOffScreenYD(rawY);

		boolean mac = IJ.isMacintosh();

		boolean shift_key_down = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
		boolean joiner_modifier_down = mac ? ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) : ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0);

		super.mouseMoved(e);

		tracerPlugin.mouseMovedTo( last_x_in_pane_precise, last_y_in_pane_precise, plane, shift_key_down, joiner_modifier_down );
	}

	double last_x_in_pane_precise = Double.MIN_VALUE;
	double last_y_in_pane_precise = Double.MIN_VALUE;

	@Override
	public void mouseClicked( MouseEvent e ) {

		if( ! tracerPlugin.isReady() )
			return;

		int currentState = tracerPlugin.resultsDialog.getState();

		if( currentState == NeuriteTracerResultsDialog.LOADING ||
		    currentState == NeuriteTracerResultsDialog.SAVING ) {

			// Do nothing

		} else if( currentState == NeuriteTracerResultsDialog.WAITING_FOR_SIGMA_POINT ) {

			tracerPlugin.launchPaletteAround(
				myOffScreenX(e.getX()),
				myOffScreenY(e.getY()),
				imp.getCurrentSlice() - 1 );

		} else if( currentState == NeuriteTracerResultsDialog.WAITING_FOR_SIGMA_CHOICE	) {

			IJ.error( "You must close the sigma palette to continue" );

		} else if( tracerPlugin.setupTrace ) {
			boolean join = IJ.isMacintosh() ? e.isAltDown() : e.isControlDown();
			tracerPlugin.clickForTrace( myOffScreenXD(e.getX()), myOffScreenYD(e.getY()), plane, join );
		} else
			IJ.error( "BUG: No operation chosen" );
	}

	protected void drawSquare( Graphics g,
				   PointInImage p,
				   Color fillColor, Color edgeColor,
				   int side ) {

		int x, y;

		if( plane == ThreePanes.XY_PLANE ) {
			x = myScreenXD(p.x/tracerPlugin.x_spacing);
			y = myScreenYD(p.y/tracerPlugin.y_spacing);
		} else if( plane == ThreePanes.XZ_PLANE ) {
			x = myScreenXD(p.x/tracerPlugin.x_spacing);
			y = myScreenYD(p.z/tracerPlugin.z_spacing);
		} else { // plane is ThreePanes.ZY_PLANE
			x = myScreenXD(p.z/tracerPlugin.z_spacing);
			y = myScreenYD(p.y/tracerPlugin.y_spacing);
		}

		int rectX = x - side / 2;
		int rectY = y - side / 2;

		g.setColor(fillColor);
		g.fillRect( rectX, rectY, side, side );

		if( edgeColor != null ) {
			g.setColor(edgeColor);
			g.drawRect( rectX, rectY, side, side );
		}
	}

	@Override
	protected void drawOverlay(Graphics g) {

		if( tracerPlugin.loading )
		    return;

		FillerThread filler = tracerPlugin.filler;
		if( filler != null ) {
			filler.setDrawingColors( fillTransparent ? transparentGreen : Color.GREEN,
						 fillTransparent ? transparentGreen : Color.GREEN );
			filler.setDrawingThreshold( filler.getThreshold() );
		}

		super.drawOverlay(g);

		double magnification = getMagnification();
		int pixel_size = magnification < 1 ? 1 : (int)magnification;
		if( magnification >= 4 )
			pixel_size = (int) (magnification / 2);

		int spotDiameter = 5 * pixel_size;

		if( unconfirmedSegment != null ) {
			unconfirmedSegment.drawPathAsPoints( this, g, Color.BLUE, plane );

			if( unconfirmedSegment.endJoins != null ) {

				int n = unconfirmedSegment.size();
				PointInImage p = unconfirmedSegment.getPointInImage(n-1);
				drawSquare( g, p, Color.BLUE, Color.GREEN, spotDiameter );
			}
		}

		Path currentPathFromTracer = tracerPlugin.getCurrentPath();

		if( currentPathFromTracer != null ) {
			if( just_near_slices )
				currentPathFromTracer.drawPathAsPoints( this, g, Color.RED, plane, imp.getCurrentSlice() - 1, eitherSide );
			else
				currentPathFromTracer.drawPathAsPoints( this, g, Color.RED, plane );

			if( lastPathUnfinished && currentPath.size() == 0 ) {

				PointInImage p = new PointInImage( tracerPlugin.last_start_point_x * tracerPlugin.x_spacing,
								   tracerPlugin.last_start_point_y * tracerPlugin.y_spacing,
								   tracerPlugin.last_start_point_z * tracerPlugin.z_spacing);

				Color edgeColour = null;
				if( currentPathFromTracer.startJoins != null )
					edgeColour = Color.GREEN;

				drawSquare( g, p, Color.BLUE, edgeColour, spotDiameter );
			}
		}

	}
}
