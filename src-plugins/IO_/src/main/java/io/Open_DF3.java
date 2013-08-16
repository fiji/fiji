package io;

/**
 * Open df3 files as image stacks.  The very simple df3 format is described at
 * 
 * http://www.povray.org/documentation/view/3.6.1/374/
 * 
 *  
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */

import java.io.File;
import java.io.FileInputStream;

import ij.io.OpenDialog;
import ij.plugin.*;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.FloatProcessor;
import ij.IJ;

public class Open_DF3 implements PlugIn 
{
	public void run( String arg )
	{
		File f = new File( arg );
		if ( !f.exists() )
		{
			OpenDialog od = new OpenDialog( "Open df3 file.", null );
			String dir = od.getDirectory();
			String name = od.getFileName();
			f = new File( dir + name );
			if ( !f.exists() )
			{
				IJ.error( "File not found." );
				return;
			}
		}
		
		try
		{
			FileInputStream file = new FileInputStream( f );
			
			byte[] header = new byte[ 6 ];
			file.read( header );
			
			int width = ( ( 0xff & header[ 0 ] ) << 8 ) + ( 0xff & header[ 1 ] );
			int height = ( ( 0xff & header[ 2 ] ) << 8 ) + ( 0xff & header[ 3 ] );
			int depth = ( ( 0xff & header[ 4 ] ) << 8 ) + ( 0xff & header[ 5 ] );
			
			byte typeLength = ( byte )( f.length() / width / depth / height );
			
			ImageStack stack = new ImageStack( width, height );
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			for ( int z = 0; z < depth; ++z )
			{
				ImageProcessor ip = null;
				
				byte[] slice = new byte[ height * width * typeLength ];
				file.read( slice );
				
				switch ( typeLength )
				{
				case 1:
					ip = new ByteProcessor( width, height, slice, null );
					break;
				case 2:
					short[] shortSlice = new short[ height * width ];
					for ( int i = 0; i < shortSlice.length; ++i )
					{
						int j = 2 * i;
						int v = ( 0xff & slice[ j ] ) << 8;
						v += 0xff & slice[ j + 1 ];
						if ( v < min ) min = v;
						if ( v > max ) max = v;
						shortSlice[ i ] = ( short )v;
					}
					ip = new ShortProcessor( width, height, shortSlice, null );
					break;
				default: // case 4:
					float[] floatSlice = new float[ height * width ];
					for ( int i = 0; i < floatSlice.length; ++i )
					{
						int j = 4 * i;
						long v = ( 0xffl & slice[ j ] ) << 24;
						v += ( 0xffl & slice[ j + 1 ] ) << 16;
						v += ( 0xffl & slice[ j + 2 ] ) << 8;
						v += 0xffl & slice[ j + 3 ];
						if ( v < min ) min = v;
						if ( v > max ) max = v;
						floatSlice[ i ] = v;
					}
					ip = new FloatProcessor( width, height, floatSlice, null );
				}
				IJ.showProgress( z, depth );
				IJ.showStatus( "Reading: " + z + "/" + depth );
				stack.addSlice( null, ip );
			}
			ImagePlus imp = new ImagePlus( f.getName().replaceAll( ".df3$", "" ), stack );
			imp.setDisplayRange( min, max );
			imp.show();
		}
		catch ( Exception e )
		{
			IJ.error( "Opening '" + f + "' as df3 failed.\n" + e.getMessage() );
		}
	}
}
