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
import java.awt.event.InputEvent;

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

	public void doProcess(final MouseEvent me) {
		if( me.getID() != MouseEvent.MOUSE_PRESSED )
			return;
		if( me.isConsumed() || Toolbar.getToolId() != Toolbar.WAND )
			return;
		Picker picker = univ.getPicker();
		Content c = picker.getPickedContent(me.getX(), me.getY());
		if (null == c)
			return;

		/*
		ImagePlus imp = c.getImage();
		if (null == imp) {
			IJ.log("Cannot segment non-image object!");
			return null; // not a volume
		}
		*/

		Point3d point = picker.getPickPointGeometry(c, me.getX(), me.getY());

		// FIXME: occurs elsewhere, DRY:
		boolean mac = IJ.isMacintosh();

		boolean shift_key_down = (me.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
		boolean joiner_modifier_down = mac ? ((me.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) : ((me.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0);

		tracerPlugin.clickForTrace(point,joiner_modifier_down);
		me.consume();
	}

}
