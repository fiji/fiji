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

import java.util.*;
import java.awt.*;

import ij.ImagePlus;

import stacks.ThreePanesCanvas;
import stacks.PaneOwner;
import stacks.ThreePanes;

public class TracerCanvas extends ThreePanesCanvas {

	private PathAndFillManager pathAndFillManager;

	public TracerCanvas( ImagePlus imagePlus,
			     PaneOwner owner,
			     int plane,
			     PathAndFillManager pathAndFillManager ) {

		super( imagePlus, owner, plane );
		this.pathAndFillManager = pathAndFillManager;
	}

	ArrayList<SearchThread> searchThreads = new ArrayList<SearchThread>();

	void addSearchThread( SearchThread s ) {
		synchronized (searchThreads) {
			searchThreads.add( s );
		}
	}

	void removeSearchThread( SearchThread s ) {
		synchronized (searchThreads) {
			int index = -1;
			for( int i = 0; i < searchThreads.size(); ++i ) {
				SearchThread inList = searchThreads.get(i);
				if( s == inList )
					index = i;
			}
			if( index >= 0 )
				searchThreads.remove( index );
		}
	}

	boolean just_near_slices = false;
	int eitherSide;

	@Override
	protected void drawOverlay(Graphics g) {

		/*
		int current_z = -1;

		if( plane == ThreePanes.XY_PLANE ) {
			current_z = imp.getCurrentSlice() - 1;
		}
		*/

		int current_z = imp.getCurrentSlice() - 1;

		synchronized (searchThreads) {
			for( Iterator<SearchThread> i = searchThreads.iterator(); i.hasNext(); )
				i.next().drawProgressOnSlice( plane, current_z, this, g );
		}

		boolean showOnlySelectedPaths = pathAndFillManager.plugin.getShowOnlySelectedPaths();

		Color selectedColor = pathAndFillManager.plugin.selectedColor;
		Color deselectedColor = pathAndFillManager.plugin.deselectedColor;

		if( pathAndFillManager != null ) {
			for( int i = 0; i < pathAndFillManager.size(); ++i ) {

				Path p = pathAndFillManager.getPath(i);
				if( p == null )
					continue;

				if( p.fittedVersionOf != null )
					continue;

				Path drawPath = p;

				// If the path suggests using the fitted version, draw that instead:
				if( p.useFitted ) {
					drawPath = p.fitted;
				}

				Color color = deselectedColor;
				if( pathAndFillManager.isSelected(p) ) {
					color = selectedColor;
				} else if( showOnlySelectedPaths )
					continue;

				if( just_near_slices ) {
					drawPath.drawPathAsPoints( this, g, color, plane, current_z, eitherSide );
				} else
					drawPath.drawPathAsPoints( this, g, color, plane );
			}
		}

		super.drawOverlay(g);

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
}
