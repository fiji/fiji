/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009 Mark Longair */

/*
  This file is part of the ImageJ plugin "Auto Tracer".

  The ImageJ plugin "Auto Tracer" is free software; you can
  redistribute it and/or modify it under the terms of the GNU General
  Public License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  The ImageJ plugin "Auto Tracer" is distributed in the hope that it
  will be useful, but WITHOUT ANY WARRANTY; without even the implied
  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.io.*;
import ij.process.*;

import java.io.*;
import java.awt.*;
import java.util.StringTokenizer;

public class Load_Auto_Traces implements PlugIn, TraceLoaderListener {

	int width = -1, height = -1, depth = -1;
	float spacing_x = Float.MIN_VALUE;
	float spacing_y = Float.MIN_VALUE;
	float spacing_z = Float.MIN_VALUE;

	byte [][] values = null;

	public void gotVertex( int vertexIndex,
			       float x_scaled, float y_scaled, float z_scaled,
			       int x_image, int y_image, int z_image ) {

		if( values == null ) {
			if( width < 0 ||
			    height < 0 ||
			    depth < 0 ||
			    spacing_x == Float.MIN_VALUE ||
			    spacing_y == Float.MIN_VALUE ||
			    spacing_z == Float.MIN_VALUE ) {

				throw new RuntimeException("Some metadata was missing from the comments before the first vertex.");
			}
			values = new byte[depth][];
			for( int z = 0; z < depth; ++z )
				values[z] = new byte[width*height];
		}

		if( z_image >= depth ) {
			System.out.println("z_image: "+z_image+" was too large for depth: "+depth);
			System.out.println("z_scaled was: "+z_scaled);
		}

		values[z_image][y_image*width+x_image] = (byte)255;
	}

	public void gotLine( int fromVertexIndex, int toVertexIndex ) {
		// Do nothing...
	}

	public void gotWidth( int width ) {
		this.width = width;
	}

	public void gotHeight( int height ) {
		this.height = height;
	}

	public void gotDepth( int depth ) {
		this.depth = depth;
	}

	public void gotSpacingX( float spacing_x ) {
		this.spacing_x = spacing_x;
	}

	public void gotSpacingY( float spacing_y ) {
		this.spacing_y = spacing_y;
	}

	public void gotSpacingZ( float spacing_z ) {
		this.spacing_z = spacing_z;
	}

	public void run( String ignored ) {

                OpenDialog od;

                od = new OpenDialog("Select traces.obj file...",
                                    null,
                                    null );

                String fileName = od.getFileName();
                String directory = od.getDirectory();

                if( fileName == null )
			return;

		System.out.println("Got "+fileName);

		boolean success = SinglePathsGraph.loadWithListener( directory + fileName, this );

		if( ! success ) {
			IJ.error( "Loading " + directory + fileName );
			return;
		}

		ImageStack stack = new ImageStack(width,height);

		for( int z = 0; z < depth; ++z ) {
			ByteProcessor bp = new ByteProcessor(width,height);
			bp.setPixels(values[z]);
			stack.addSlice("",bp);
		}

		ImagePlus imagePlus=new ImagePlus(fileName,stack);
		imagePlus.show();

	}

}

