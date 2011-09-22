/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2011 Mark Longair */

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

import ij.IJ;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;

public class QueueJumpingKeyListener implements KeyListener {

	protected SimpleNeuriteTracer tracerPlugin;
	static final protected boolean verbose = SimpleNeuriteTracer.verbose;
	protected InteractiveTracerCanvas canvas;

	ArrayList<KeyListener> listeners = new ArrayList<KeyListener>();

	public QueueJumpingKeyListener( SimpleNeuriteTracer tracerPlugin, InteractiveTracerCanvas canvas) {
		this.tracerPlugin = tracerPlugin;
		this.canvas = canvas;
	}

	public void keyPressed(KeyEvent e) {

		if( ! tracerPlugin.isReady() )
			return;

		int keyCode = e.getKeyCode();
		char keyChar = e.getKeyChar();

		boolean mac = IJ.isMacintosh();

		boolean shift_pressed = (keyCode == KeyEvent.VK_SHIFT);
		boolean join_modifier_pressed = mac ? keyCode == KeyEvent.VK_ALT : keyCode == KeyEvent.VK_CONTROL;

		int modifiers = e.getModifiersEx();
		boolean shift_down = (modifiers & InputEvent.SHIFT_DOWN_MASK) > 0;
		boolean control_down = (modifiers & InputEvent.CTRL_DOWN_MASK) > 0;
		boolean alt_down = (modifiers & InputEvent.ALT_DOWN_MASK) > 0;

		if (verbose) System.out.println("keyCode=" + keyCode + " (" + KeyEvent.getKeyText(keyCode)
						+ ") keyChar=\"" + keyChar + "\" (" + (int)keyChar + ") "
						+ KeyEvent.getKeyModifiersText(canvas.getModifiers()));

		if( keyChar == 'y' || keyChar == 'Y' ) {

			// if (verbose) System.out.println( "Yes, running confirmPath" );
			tracerPlugin.confirmTemporary( );
			e.consume();

		} else if( keyCode == KeyEvent.VK_ESCAPE ) {

			// if (verbose) System.out.println( "Yes, running cancelPath+" );
			tracerPlugin.cancelTemporary( );
			e.consume();

		} else if( keyChar == 'n' || keyChar == 'N' ) {

			tracerPlugin.cancelTemporary( );
			e.consume();

		} else if( keyChar == 'f' || keyChar == 'F' ) {

			// if (verbose) System.out.println( "Finalizing that path" );
			tracerPlugin.finishedPath( );
			e.consume();

		} else if( keyChar == 'v' || keyChar == 'V' ) {

			// if (verbose) System.out.println( "View paths as a stack" );
			tracerPlugin.makePathVolume( );
			e.consume();

		} else if( keyChar == '5' ) {

			canvas.toggleJustNearSlices();
			e.consume();

		} else if( keyChar == 'm' || keyChar == 'M' ) {

			canvas.clickAtMaxPoint();
			e.consume();

		} else if( keyChar == 'g' || keyChar == 'G' ) {

			canvas.selectNearestPathToMousePointer( shift_down || control_down );
			e.consume();

		} else if( shift_pressed || join_modifier_pressed ) {

			/* This case is just so that when someone
			   starts holding down the modified they
			   immediately see the effect, rather than
			   having to wait for the next mouse move
			   event. */

			canvas.fakeMouseMoved( shift_pressed, join_modifier_pressed  );
			e.consume();
		}

		if( shift_down && (control_down || alt_down) && (keyCode == KeyEvent.VK_A) ) {
			canvas.startShollAnalysis();
			e.consume();
		}

		for(KeyListener kl : listeners) {
			if( e.isConsumed() )
				break;
			kl.keyPressed(e);
		}

	}

	public void keyReleased(KeyEvent e) {
		for(KeyListener kl : listeners) {
			if( e.isConsumed() )
				break;
			kl.keyReleased(e);
		}
	}

	public void keyTyped(KeyEvent e) {
		for(KeyListener kl : listeners) {
			if( e.isConsumed() )
				break;
			kl.keyTyped(e);
		}
	}

	/** This method should add the other key listeners in
	    'laterKeyListeners' that will be called for 'source' if
	    this key listener isn't interested in the key press. */
	public void addOtherKeyListeners(KeyListener [] laterKeyListeners) {
		ArrayList<KeyListener> newListeners = new ArrayList<KeyListener>(Arrays.asList(laterKeyListeners));
		listeners.addAll(newListeners);
	}

}
