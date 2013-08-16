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

import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.Point;
import java.awt.event.InputEvent;

import javax.swing.SwingUtilities;

import ij.IJ;

import ij3d.behaviors.InteractiveBehavior;
import ij3d.behaviors.Picker;
import ij3d.Image3DUniverse;
import ij3d.Content;
import ij.gui.Toolbar;

import javax.vecmath.Point3d;

/* This class is based on Albert Cardona's code in
 * Blob_Segmentation_in_3D.java */

public class PointSelectionBehavior extends InteractiveBehavior {

	protected SimpleNeuriteTracer tracerPlugin;

	public PointSelectionBehavior(Image3DUniverse univ, SimpleNeuriteTracer tracerPlugin) {
		super(univ);
		this.tracerPlugin = tracerPlugin;
	}

	public void doProcess(final KeyEvent e) {

		if( e.getID() != KeyEvent.KEY_TYPED )
			return;

		if( e.isConsumed() )
			return;

		if( ! tracerPlugin.isReady() )
			return;

		final int keyCode = e.getKeyCode();
		final char keyChar = e.getKeyChar();

		SwingUtilities.invokeLater( new Runnable() {
				public void run() {

					if( keyChar == 'y' || keyChar == 'Y' ) {

						tracerPlugin.confirmTemporary( );
						e.consume();

					} else if( keyCode == KeyEvent.VK_ESCAPE ) {

						tracerPlugin.cancelTemporary( );
						e.consume();

					} else if( keyChar == 'n' || keyChar == 'N' ) {

						tracerPlugin.cancelTemporary( );
						e.consume();

					} else if( keyChar == 'f' || keyChar == 'F' ) {

						tracerPlugin.finishedPath( );
						e.consume();

					} else if( keyChar == 'v' || keyChar == 'V' ) {

						tracerPlugin.makePathVolume( );
						e.consume();

					} else if( keyChar == '5' ) {

						tracerPlugin.getXYCanvas().toggleJustNearSlices();
						e.consume();

					} else if( keyChar == 'g' || keyChar == 'G' ) {

						Point p = univ.getCanvas().getMousePosition();
						if( p == null )
							return;
						Picker picker = univ.getPicker();
						Content c = picker.getPickedContent(p.x, p.y);
						if (null == c)
							return;
						final Point3d point = picker.getPickPointGeometry(c, p.x, p.y);
						double diagonalLength = tracerPlugin.getStackDiagonalLength();

						/* Find the nearest point on any path - we'll
						   select that path... */

						NearPoint np = tracerPlugin.getPathAndFillManager().nearestPointOnAnyPath( point.x,
															   point.y,
															   point.z,
															   diagonalLength);
						if( np == null ) {
							IJ.error("BUG: No nearby path was found within "+diagonalLength+" of the pointer");
							return;
						}

						Path path = np.getPath();
						tracerPlugin.selectPath( path, keyChar == 'G' );
						e.consume();
					}

				}} );
	}

	public void doProcess(final MouseEvent me) {
		if( me.isConsumed() || Toolbar.getToolId() != Toolbar.WAND )
			return;
		int mouseEventID = me.getID();
		/* It's nice to still be able to zoom with the mouse wheel, so
		   don't consume this event. */
		if( mouseEventID == MouseEvent.MOUSE_WHEEL )
			return;
		me.consume();
		if( mouseEventID != MouseEvent.MOUSE_CLICKED )
			return;
		Picker picker = univ.getPicker();
		Content c = picker.getPickedContent(me.getX(), me.getY());
		if (null == c)
			return;

		final Point3d point = picker.getPickPointGeometry(c, me.getX(), me.getY());

		boolean mac = IJ.isMacintosh();

		boolean shift_key_down = (me.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
		final boolean joiner_modifier_down = mac ? ((me.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) : ((me.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0);

		SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					tracerPlugin.clickForTrace(point,joiner_modifier_down);
				}} );
	}
}
