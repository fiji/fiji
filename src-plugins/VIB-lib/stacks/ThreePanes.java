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

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package stacks;

import ij.*;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.gui.*;

import java.awt.image.ColorModel;

import java.io.*;

public class ThreePanes implements PaneOwner {

	public static final int XY_PLANE = 0; // constant z
	public static final int XZ_PLANE = 1; // constant y
	public static final int ZY_PLANE = 2; // constant x

	protected ImagePlus xy;
	protected ImagePlus xz;
	protected ImagePlus zy;

	protected ThreePanesCanvas xy_canvas;
	protected ThreePanesCanvas xz_canvas;
	protected ThreePanesCanvas zy_canvas;

	protected ImageCanvas original_xy_canvas;

	protected StackWindow xy_window;
	protected StackWindow xz_window;
	protected StackWindow zy_window;

	protected boolean single_pane = false;

	public void findPointInStack( int x_in_pane, int y_in_pane, int plane, int [] point ) {

		switch( plane ) {

		case ThreePanes.XY_PLANE:
		{
			point[0] = x_in_pane;
			point[1] = y_in_pane;
			point[2] = xy.getCurrentSlice( ) - 1;
		}
		break;

		case ThreePanes.XZ_PLANE:
		{
			point[0] = x_in_pane;
			point[1] = xz.getCurrentSlice( ) - 1;
			point[2] = y_in_pane;
		}
		break;

		case ThreePanes.ZY_PLANE:
		{
			point[0] = zy.getCurrentSlice( ) - 1;
			point[1] = y_in_pane;
			point[2] = x_in_pane;
		}
		break;

		}

	}

	public void findPointInStackPrecise( double x_in_pane, double y_in_pane, int plane, double [] point ) {

		switch( plane ) {

		case ThreePanes.XY_PLANE:
		{
			point[0] = x_in_pane;
			point[1] = y_in_pane;
			point[2] = xy.getCurrentSlice( ) - 1;
		}
		break;

		case ThreePanes.XZ_PLANE:
		{
			point[0] = x_in_pane;
			point[1] = xz.getCurrentSlice( ) - 1;
			point[2] = y_in_pane;
		}
		break;

		case ThreePanes.ZY_PLANE:
		{
			point[0] = zy.getCurrentSlice( ) - 1;
			point[1] = y_in_pane;
			point[2] = x_in_pane;
		}
		break;

		}

	}

	public ThreePanesCanvas createCanvas( ImagePlus imagePlus, int plane ) {
		return new ThreePanesCanvas( imagePlus, this, plane );
	}

	public void mouseMovedTo( int off_screen_x, int off_screen_y, int in_plane, boolean shift_down ) {

		int point[] = new int[3];

		findPointInStack( off_screen_x, off_screen_y, in_plane, point );

		xy_canvas.setCrosshairs( point[0], point[1], point[2], true /* in_plane != XY_PLANE */ );
		if( ! single_pane ) {
			xz_canvas.setCrosshairs( point[0], point[1], point[2], true /* in_plane != XZ_PLANE */ );
			zy_canvas.setCrosshairs( point[0], point[1], point[2], true /* in_plane != ZY_PLANE */ );
		}

		if( shift_down )
			setSlicesAllPanes( point[0], point[1], point[2] );
	}

	public void setSlicesAllPanes( int new_x, int new_y, int new_z ) {

		xy.setSlice( new_z + 1 );
		if( ! single_pane ) {
			xz.setSlice( new_y + 1 );
			zy.setSlice( new_x + 1 );
		}
	}

	public void repaintAllPanes( ) {

		xy_canvas.repaint();
		if( ! single_pane ) {
			xz_canvas.repaint();
			zy_canvas.repaint();
		}
	}

	public void closeAndReset( ) {
		if( ! single_pane ) {
			zy.close();
			xz.close();
		}
		if( original_xy_canvas != null && xy != null && xy.getImage() != null )
			xy_window = new StackWindow( xy, original_xy_canvas );
	}

	public ThreePanes( ) {

	}

	public void checkMemory( ImagePlus imagePlus, int memoryMultipleNeeded ) {

		long sizeOfImagePlus =
			imagePlus.getWidth() *
			imagePlus.getHeight() *
			imagePlus.getStackSize() *
			( imagePlus.getBitDepth() / 8 );

		long bytesNeededEstimate = (memoryMultipleNeeded + 1) * sizeOfImagePlus;

		System.gc();
		long maxMemory = Runtime.getRuntime().maxMemory();

		if( bytesNeededEstimate > maxMemory ) {

			IJ.error("Warning",
				 "It looks as if the amount of memory required for the " +
				 "three pane view (" +
				 (bytesNeededEstimate / (1024 * 1024)) +
				 "MiB) exceeds the maximum memory available (" +
				 (maxMemory / (1024 * 1024)) +
				 "MiB)");
		}
	}

	int type;
	int bytesPerPixel;

	public static String imageTypeToString( int type ) {
		String result;
		switch (type) {
		case ImagePlus.GRAY8:
			result = "GRAY8 (8-bit grayscale (unsigned))";
			break;
		case ImagePlus.GRAY16:
			result = "GRAY16 (16-bit grayscale (unsigned))";
			break;
		case ImagePlus.GRAY32:
			result = "GRAY32 (32-bit floating-point grayscale)";
			break;
		case ImagePlus.COLOR_256:
			result = "COLOR_256 (8-bit indexed color)";
			break;
		case ImagePlus.COLOR_RGB:
			result = "COLOR_RGB (32-bit RGB color)";
			break;
		default:
			result = "Unknown (value: " + type + ")";
			break;
		}
		return result;
	}

	/* If memoryMultipleNeeded is 3, for example, that means that
	 * you will need 3 times the memory used by the supplied
	 * ImagePlus in order for the plugin not to warn you about
	 * free memory. */

	public void initialize( ImagePlus imagePlus ) {

		xy = imagePlus;

		type = xy.getType();

		bytesPerPixel = xy.getBitDepth() / 8;

		original_xy_canvas = imagePlus.getWindow().getCanvas();

		int width = xy.getWidth();
		int height = xy.getHeight();
		int depth = xy.getStackSize();

		ImageStack xy_stack=xy.getStack();

		ColorModel cm = null;

		// FIXME: should we save the LUT for other image types?
		if( type == ImagePlus.COLOR_256 )
			cm = xy_stack.getColorModel();

		if( ! single_pane ) {

			int zy_width = depth;
			int zy_height = height;
			ImageStack zy_stack = new ImageStack( zy_width, zy_height );

			int xz_width = width;
			int xz_height = depth;
			ImageStack xz_stack = new ImageStack( xz_width, xz_height );

			/* Just load in the complete stack for simplicity's
			 * sake... */

			byte [][] slices_data_b = new byte[depth][];
			int [][] slices_data_i = new int[depth][];
			float [][] slices_data_f = new float[depth][];
			short [][] slices_data_s = new short[depth][];

			for( int z = 0; z < depth; ++z ) {
				switch (type) {
				case ImagePlus.GRAY8:
				case ImagePlus.COLOR_256:
					slices_data_b[z] = (byte []) xy_stack.getPixels( z + 1 );
					break;
				case ImagePlus.GRAY16:
					slices_data_s[z] = (short []) xy_stack.getPixels( z + 1 );
					break;
				case ImagePlus.COLOR_RGB:
					slices_data_i[z] = (int []) xy_stack.getPixels( z + 1 );
					break;
				case ImagePlus.GRAY32:
					slices_data_f[z] = (float []) xy_stack.getPixels( z + 1 );
					break;
				}
			}

			IJ.showStatus("Generating XZ planes...");
			IJ.showProgress(0);

			// Create the ZY slices:

			switch (type) {

			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:

				for( int x_in_original = 0; x_in_original < width; ++x_in_original ) {

					byte [] sliceBytes = new byte[ zy_width * zy_height ];

					for( int z_in_original = 0; z_in_original < depth; ++z_in_original ) {
						for( int y_in_original = 0; y_in_original < height; ++y_in_original ) {

							int x_in_left = z_in_original;
							int y_in_left = y_in_original;

							sliceBytes[ y_in_left * zy_width + x_in_left ] =
								slices_data_b[ z_in_original ][ y_in_original * width + x_in_original ];
						}
					}

					ByteProcessor bp = new ByteProcessor( zy_width, zy_height );
					bp.setPixels( sliceBytes );
					zy_stack.addSlice( null, bp );
					IJ.showProgress( x_in_original / (double)width );
				}
				break;

			case ImagePlus.GRAY16:

				for( int x_in_original = 0; x_in_original < width; ++x_in_original ) {

					short [] sliceShorts = new short[ zy_width * zy_height ];

					for( int z_in_original = 0; z_in_original < depth; ++z_in_original ) {
						for( int y_in_original = 0; y_in_original < height; ++y_in_original ) {

							int x_in_left = z_in_original;
							int y_in_left = y_in_original;

							sliceShorts[ y_in_left * zy_width + x_in_left ] =
								slices_data_s[ z_in_original ][ y_in_original * width + x_in_original ];
						}
					}

					ShortProcessor sp = new ShortProcessor( zy_width, zy_height );
					sp.setPixels( sliceShorts );
					zy_stack.addSlice( null, sp );
					IJ.showProgress( x_in_original / (double)width );
				}
				break;


			case ImagePlus.COLOR_RGB:

				for( int x_in_original = 0; x_in_original < width; ++x_in_original ) {

					int [] sliceInts = new int[ zy_width * zy_height ];

					for( int z_in_original = 0; z_in_original < depth; ++z_in_original ) {
						for( int y_in_original = 0; y_in_original < height; ++y_in_original ) {

							int x_in_left = z_in_original;
							int y_in_left = y_in_original;

							sliceInts[ y_in_left * zy_width + x_in_left ] =
								slices_data_i[ z_in_original ][ y_in_original * width + x_in_original ];
						}
					}

					ColorProcessor cp = new ColorProcessor( zy_width, zy_height );
					cp.setPixels( sliceInts );
					zy_stack.addSlice( null, cp );
					IJ.showProgress( x_in_original / (double)width );
				}
				break;

			case ImagePlus.GRAY32:

				for( int x_in_original = 0; x_in_original < width; ++x_in_original ) {

					float [] sliceFloats = new float[ zy_width * zy_height ];

					for( int z_in_original = 0; z_in_original < depth; ++z_in_original ) {
						for( int y_in_original = 0; y_in_original < height; ++y_in_original ) {

							int x_in_left = z_in_original;
							int y_in_left = y_in_original;

							sliceFloats[ y_in_left * zy_width + x_in_left ] =
								slices_data_f[ z_in_original ][ y_in_original * width + x_in_original ];
						}
					}

					FloatProcessor fp = new FloatProcessor( zy_width, zy_height );
					fp.setPixels( sliceFloats );
					zy_stack.addSlice( null, fp );
					IJ.showProgress( x_in_original / (double)width );
				}
				break;

			}

			if( type == ImagePlus.COLOR_256 ) {
				if( cm != null ) {
					zy_stack.setColorModel(cm);
				}
			}

			IJ.showProgress( 1.0 );

			IJ.showStatus("Generating ZY planes...");
			IJ.showProgress(0);

			zy = new ImagePlus( "ZY planes of " + xy.getShortTitle(), zy_stack );

			// Create the XZ slices:

			switch (type) {

			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:

				for( int y_in_original = 0; y_in_original < height; ++y_in_original ) {

					byte [] sliceBytes = new byte[ xz_width * xz_height ];

					for( int z_in_original = 0; z_in_original < depth; ++z_in_original ) {

						// Now we can copy a complete row from
						// the original image to the XZ slice:

						int y_in_top = z_in_original;

						System.arraycopy( slices_data_b[z_in_original],
								  y_in_original * width,
								  sliceBytes,
								  y_in_top * xz_width,
								  width );

					}

					ByteProcessor bp = new ByteProcessor( xz_width, xz_height );
					bp.setPixels( sliceBytes );
					xz_stack.addSlice( null, bp );

					IJ.showProgress( y_in_original / (double)width );
				}
				break;

			case ImagePlus.GRAY16:

				for( int y_in_original = 0; y_in_original < height; ++y_in_original ) {

					short [] sliceShorts = new short[ xz_width * xz_height ];

					for( int z_in_original = 0; z_in_original < depth; ++z_in_original ) {

						// Now we can copy a complete row from
						// the original image to the XZ slice:

						int y_in_top = z_in_original;

						System.arraycopy( slices_data_s[z_in_original],
								  y_in_original * width,
								  sliceShorts,
								  y_in_top * xz_width,
								  width );

					}

					ShortProcessor sp = new ShortProcessor( xz_width, xz_height );
					sp.setPixels( sliceShorts );
					xz_stack.addSlice( null, sp );

					IJ.showProgress( y_in_original / (double)width );
				}
				break;

			case ImagePlus.COLOR_RGB:

				for( int y_in_original = 0; y_in_original < height; ++y_in_original ) {

					int [] sliceInts = new int[ xz_width * xz_height ];

					for( int z_in_original = 0; z_in_original < depth; ++z_in_original ) {

						// Now we can copy a complete row from
						// the original image to the XZ slice:

						int y_in_top = z_in_original;

						System.arraycopy( slices_data_i[z_in_original],
								  y_in_original * width,
								  sliceInts,
								  y_in_top * xz_width,
								  width );

					}

					ColorProcessor cp = new ColorProcessor( xz_width, xz_height );
					cp.setPixels( sliceInts );
					xz_stack.addSlice( null, cp );

					IJ.showProgress( y_in_original / (double)width );
				}
				break;

			case ImagePlus.GRAY32:

				for( int y_in_original = 0; y_in_original < height; ++y_in_original ) {

					float [] sliceFloats = new float[ xz_width * xz_height ];

					for( int z_in_original = 0; z_in_original < depth; ++z_in_original ) {

						// Now we can copy a complete row from
						// the original image to the XZ slice:

						int y_in_top = z_in_original;

						System.arraycopy( slices_data_f[z_in_original],
								  y_in_original * width,
								  sliceFloats,
								  y_in_top * xz_width,
								  width );

					}

					FloatProcessor fp = new FloatProcessor( xz_width, xz_height );
					fp.setPixels( sliceFloats );
					xz_stack.addSlice( null, fp );

					IJ.showProgress( y_in_original / (double)width );
				}
				break;


			}

			xz = new ImagePlus( "XZ planes of " + xy.getShortTitle(), xz_stack );

			if( type == ImagePlus.COLOR_256 ) {
				if( cm != null ) {
					xz_stack.setColorModel(cm);
				}
			}

			IJ.showProgress( 1.0 ); // Removes the progress indicator

		}

		System.gc();

		xy_canvas = createCanvas( xy, XY_PLANE );
		if( ! single_pane ) {
			xz_canvas = createCanvas( xz, XZ_PLANE );
			zy_canvas = createCanvas( zy, ZY_PLANE );
		}

		xy_window = new StackWindow( xy, xy_canvas );
		if( ! single_pane ) {
			xz_window = new StackWindow( xz, xz_canvas );
			zy_window = new StackWindow( zy, zy_canvas );
		}

	}

}
