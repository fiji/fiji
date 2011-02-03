/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010 Mark Longair */

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

public interface TraceLoaderListener {

    public void gotVertex( int vertexIndex,
                           float x_scaled, float y_scaled, float z_scaled,
                           int x_image, int y_image, int z_image );

    public void gotLine( int fromVertexIndex, int toVertexIndex );

    public void gotWidth( int width );
    public void gotHeight( int height );
    public void gotDepth( int depth );

    public void gotSpacingX( float spacing_x );
    public void gotSpacingY( float spacing_y );
    public void gotSpacingZ( float spacing_z );

}
