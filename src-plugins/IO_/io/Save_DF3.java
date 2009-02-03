package io;

/**
 * Save an image stack as in the df3 format.  The very simple df3 format is
 * described at
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

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.InputStream;

import ij.plugin.*;
import ij.gui.GenericDialog;
import ij.io.SaveDialog;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.WindowManager;
import ij.IJ;

public class Save_DF3 implements PlugIn 
{
	private String[] types = null;

	public void run( String args )
	{
		double max;
		boolean stretchContrastIsNegotiable = false;
		ImagePlus imp = WindowManager.getCurrentImage();
		if (null == imp) 
		{
			IJ.error( "You have no images open." );
			return;
		}

		if (imp.getStackSize() <= 1)
		{
			IJ.error( "This is not an image stack." );
			return;
		}
		
		switch ( imp.getType() )
		{
		case ImagePlus.COLOR_256:
		case ImagePlus.GRAY8:
			types = new String[]{
					"8-bit unsigned integer" };
			max = 255.0;
			stretchContrastIsNegotiable = true;
			break;
		case ImagePlus.GRAY16:
			types = new String[]{
				"8-bit unsigned integer",
				"16-bit unsigned integer" }; 
			max = 65535.0;
			stretchContrastIsNegotiable = true;
			break;
		default:
			types = new String[]{
				"8-bit unsigned integer",
				"16-bit unsigned integer",
				"32-bit unsigned integer" };
			max = Float.MAX_VALUE;
		}
		
		GenericDialog gd = new GenericDialog( "Save df3 file" );
		gd.addChoice( "Choose_data_output_format :", types, types[ types.length - 1 ] );
		gd.addCheckbox( "Create_POV-Ray_scene :", false );
		if ( stretchContrastIsNegotiable ) gd.addCheckbox( "Stretch_contrast :", true );
		gd.showDialog();
		if ( gd.wasCanceled() ) return;

		SaveDialog sd = new SaveDialog( "Save as ...", imp.getTitle(), ".df3" );
		String directory = sd.getDirectory();
		String name = sd.getFileName();

		if ( name == null || name == "" ) 
		{
			IJ.error( "No filename selected." );
			return;
		}
		
		int type = gd.getNextChoiceIndex();
		long typeScale = 1l;
		byte typeLength = 0; 
		switch ( type )
		{
		case 0:
			typeLength = 1;
			typeScale = 255l;
			break;
		case 1:
			typeLength = 2;
			typeScale = 65535l;
			break;
		case 2:
			typeLength = 4;
			typeScale = 4294967295l;
			break;
		}
		
		String fileName = directory + name;
		boolean createPov = gd.getNextBoolean();
		boolean stretchContrast = stretchContrastIsNegotiable ? gd.getNextBoolean() : true;
		
		ImageStack stack = imp.getStack();
		
		int width = stack.getWidth();
		int height = stack.getHeight();
		int length = width * height;
		
		int depth = stack.getSize();
		
		double min = 0.0;
		
		if ( stretchContrast )
		{
			min = Double.MAX_VALUE;
			max = Double.MIN_VALUE;
			
			for ( int z = 1; z <= depth; ++z )
			{
				IJ.showProgress( z, depth );
				IJ.showStatus( "Identifying contrast range: " + z + "/" + depth );
				
				ImageProcessor ips = stack.getProcessor( z );
				for ( int i = 0; i < length; ++i )
				{
					double v = ips.getf( i );
					if ( v < min ) min = v;
					if ( v > max ) max = v;
				}
			}
		}
		
		try
		{
			java.io.FileOutputStream file = new FileOutputStream( fileName, false );
		
			byte[] header = new byte[ 6 ];
			header[ 0 ] = ( byte )( width >> 8 );
			header[ 1 ] = ( byte )width;
			
			header[ 2 ] = ( byte )( height >> 8 );
			header[ 3 ] = ( byte )height;
			
			header[ 4 ] = ( byte )( depth >> 8 );
			header[ 5 ] = ( byte )depth;
			
			file.write( header );
			double d =  typeScale / ( max - min );
						
			
			for ( int z = 1; z <= depth; ++z )
			{
				IJ.showProgress( z, depth );
				IJ.showStatus( "Writing: " + z + "/" + depth );
				
				ImageProcessor ips = stack.getProcessor( z );
				
				byte[] bytes = new byte[ typeLength * length ];
				
				for ( int i = 0; i < length; ++i )
				{
					int j = typeLength * i;
					long pixel = Math.min( typeScale, Math.max( 0l, ( long )( ( ips.getf( i ) - min ) * d ) ) );

					switch ( type )
					{
					case 0:
						bytes[ j ] = ( byte )pixel;
						break;
					case 1:
						bytes[ j ] = ( byte )( pixel >> 8 );
						bytes[ j + 1 ] = ( byte )pixel;
						break;
					case 2:
						bytes[ j ] = ( byte )( pixel >> 24 );
						bytes[ j + 1 ] = ( byte )( pixel >> 16 );
						bytes[ j + 2 ] = ( byte )( pixel >> 8 );
						bytes[ j + 3 ] = ( byte )pixel;
					}
				}
				
				file.write( bytes );
			}
		
			file.close();
			IJ.showStatus( "Saved " + fileName + "." );
		
			if ( createPov )
			{
				String rawFileName = fileName.replaceAll( ".df3$", "" );
				try
				{
					InputStream is = getClass().getResourceAsStream( "df3_scene.pov" );
					byte[] bytes = new byte[ is.available() ];
					is.read( bytes );
					String povCode = new String( bytes );
					povCode = povCode.replaceAll( new String( "\\$file_name\\$" ), rawFileName );
					povCode = povCode.replaceAll( "\\$max_length\\$", new Integer( Math.max( width, Math.max( height, depth ) ) ).toString() );
					povCode = povCode.replaceAll( "\\$width\\$", new Integer( width ).toString() );
					povCode = povCode.replaceAll( "\\$height\\$", new Integer( height ).toString() );
					povCode = povCode.replaceAll( "\\$depth\\$", new Integer( depth ).toString() );
					
					PrintStream ps = new PrintStream( rawFileName + ".pov" ); 
					ps.print( povCode );
					ps.close();
				}
				catch ( Exception e )
				{
					IJ.error( "Error writing pov-file '" + rawFileName + ".pov'.\n" + e.getMessage() );
				}
			}
		}
		catch ( Exception e )
		{
			IJ.error( "Error writing df3-file '" + fileName + "'.\n" + e.getMessage() );
			return;
		}	
	}
}
