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

import java.awt.*;
import java.awt.event.*;

import ij.gui.ColorChooser;

public class PathColorsCanvas extends Canvas implements MouseListener {

	SimpleNeuriteTracer plugin;
	public PathColorsCanvas( SimpleNeuriteTracer plugin, int width, int height ) {
		this.plugin = plugin;
		addMouseListener( this );
		setSize( width, height );
		selectedColor = plugin.selectedColor;
		deselectedColor = plugin.deselectedColor;
	}

	private Color selectedColor;
	private Color deselectedColor;

	@Override
	public void update( Graphics g ) {
		paint(g);
	}

	@Override
	public void paint( Graphics g ) {
		int width = getWidth();
		int height = getHeight();
		int leftWidth = width / 2;
		g.setColor( selectedColor );
		g.fillRect( 0, 0, leftWidth, height );
		g.setColor( deselectedColor );
		g.fillRect( leftWidth, 0, width - leftWidth, height );
	}

	public void mouseClicked(MouseEvent e) {
		int x = e.getX();
		ColorChooser chooser;
		if( x < getWidth() / 2 ) {
			chooser = new ColorChooser(
				"Colour for selected paths",
				selectedColor,
				false );
			Color newColor = chooser.getColor();
			if( newColor == null )
				return;
			selectedColor = newColor;
			plugin.setSelectedColor( newColor );
		} else {
			chooser = new ColorChooser(
				"Colour for deselected paths",
				deselectedColor,
				false );
			Color newColor = chooser.getColor();
			if( newColor == null )
				return;
			deselectedColor = newColor;
			plugin.setDeselectedColor( newColor );
		}
		repaint();
	}

	public void mouseEntered(MouseEvent e) { }

	public void mouseExited(MouseEvent e) { }

	public void mousePressed(MouseEvent e) { }

	public void mouseReleased(MouseEvent e) { }
}

