/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010 Mark Longair */

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

@SuppressWarnings("serial")
public class InteractiveTracerCanvas extends TracerCanvas implements KeyListener {

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

	public void keyPressed(KeyEvent e) {

		if( ! tracerPlugin.isReady() )
			return;

		int keyCode = e.getKeyCode();
		char keyChar = e.getKeyChar();

		boolean mac = IJ.isMacintosh();

		boolean shift_pressed = (keyCode == KeyEvent.VK_SHIFT);
		boolean join_modifier_pressed = mac ? keyCode == KeyEvent.VK_ALT : keyCode == KeyEvent.VK_CONTROL;

		if (verbose) System.out.println("keyCode=" + keyCode + " (" + KeyEvent.getKeyText(keyCode)
						+ ") keyChar=\"" + keyChar + "\" (" + (int)keyChar + ") "
						+ KeyEvent.getKeyModifiersText(flags));

		if( keyChar == 'y' || keyChar == 'Y' ) {

			// if (verbose) System.out.println( "Yes, running confirmPath" );
			tracerPlugin.confirmTemporary( );

		} else if( keyCode == KeyEvent.VK_ESCAPE ) {

			// if (verbose) System.out.println( "Yes, running cancelPath+" );
			tracerPlugin.cancelTemporary( );

		} else if( keyChar == 'f' || keyChar == 'F' ) {

			// if (verbose) System.out.println( "Finalizing that path" );
			tracerPlugin.finishedPath( );

		} else if( keyChar == 'v' || keyChar == 'V' ) {

			// if (verbose) System.out.println( "View paths as a stack" );
			tracerPlugin.makePathVolume( );

		} else if( keyChar == '5' ) {

			just_near_slices = ! just_near_slices;

		} else if( shift_pressed || join_modifier_pressed ) {

			/* This case is just so that when someone
			   starts holding down the modified they
			   immediately see the effect, rather than
			   having to wait for the next mouse move
			   event. */

			tracerPlugin.mouseMovedTo( last_x_in_pane_precise, last_y_in_pane_precise, plane, shift_pressed, join_modifier_pressed );

		}

		int modifiers = e.getModifiersEx();
		boolean shift_down = (modifiers & InputEvent.SHIFT_DOWN_MASK) > 0;
		boolean control_down = (modifiers & InputEvent.CTRL_DOWN_MASK) > 0;
		boolean alt_down = (modifiers & InputEvent.ALT_DOWN_MASK) > 0;

		if( shift_down && (control_down || alt_down) && (keyCode == KeyEvent.VK_A) ) {
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

		e.consume();
	}

	public void keyReleased(KeyEvent e) {}

	public void keyTyped(KeyEvent e) {}

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

				int x = myScreenX(unconfirmedSegment.getXUnscaled(n-1));
				int y = myScreenY(unconfirmedSegment.getYUnscaled(n-1));

				int rectX = x - spotDiameter / 2;
				int rectY = y - spotDiameter / 2;

				g.setColor(Color.BLUE);
				g.fillRect( rectX, rectY, spotDiameter, spotDiameter );

				g.setColor(Color.GREEN);
				g.drawRect( rectX, rectY, spotDiameter, spotDiameter );
			}
		}

		Path currentPathFromTracer = tracerPlugin.getCurrentPath();

		if( currentPathFromTracer != null ) {
			if( just_near_slices )
				currentPathFromTracer.drawPathAsPoints( this, g, Color.RED, plane, imp.getCurrentSlice() - 1, eitherSide );
			else
				currentPathFromTracer.drawPathAsPoints( this, g, Color.RED, plane );

			if( lastPathUnfinished && currentPath.size() == 0 ) {

				int x = myScreenX(tracerPlugin.last_start_point_x);
				int y = myScreenY(tracerPlugin.last_start_point_y);

				int rectX = x - spotDiameter / 2;
				int rectY = y - spotDiameter / 2;

				g.setColor(Color.BLUE);
				g.fillRect( rectX, rectY, spotDiameter, spotDiameter );

				if( currentPathFromTracer.startJoins != null ) {
					g.setColor(Color.GREEN);
					g.drawRect( rectX, rectY, spotDiameter, spotDiameter );
				}

			}
		}

	}
}
