/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
    This file is part of the ImageJ plugins "Simple Neurite Tracer"
    and "Three Pane Crop".

    The ImageJ plugins "Three Pane Crop" and "Simple Neurite Tracer"
    are free software; you can redistribute them and/or modify them
    under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    The ImageJ plugins "Simple Neurite Tracer" and "Three Pane Crop"
    are distributed in the hope that they will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
    License for more details.

    In addition, as a special exception, the copyright holders give
    you permission to combine this program with free software programs or
    libraries that are released under the Apache Public License. 

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package stacks;

public interface PaneOwner {

	public void mouseMovedTo( int x, int y, int plane, boolean shift_down );

	public void zoom( boolean zoomIn, int x, int y, int plane );

}
