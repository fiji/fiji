/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
    This file is part of the ImageJ plugin "Three Pane Crop".

    The ImageJ plugin "Three Pane Crop" is free software; you can
    redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software
    Foundation; either version 3 of the License, or (at your option)
    any later version.

    The ImageJ plugin "Three Pane Crop" is distributed in the hope
    that it will be useful, but WITHOUT ANY WARRANTY; without even the
    implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
    PURPOSE.  See the GNU General Public License for more details.

    In addition, as a special exception, the copyright holders give
    you permission to combine this program with free software programs or
    libraries that are released under the Apache Public License. 

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package stacks;

import ij.*;
import ij.gui.ImageCanvas;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;

public class Three_Pane_Crop implements PlugIn {

	ThreePaneCrop threePaneCrop;
	
	public void run( String argument ) {
		
		ImagePlus currentImage = WindowManager.getCurrentImage();

		if( currentImage == null ) {
			IJ.error( "There's no current image to crop." );
			return;
		}

		threePaneCrop = new ThreePaneCrop( );

		threePaneCrop.initialize( currentImage );
		
	}

}
