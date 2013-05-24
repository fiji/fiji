/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package mpicbg.imglib.io;

import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.services.OMEXMLService;
import mpicbg.imglib.container.ContainerFactory;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizablePlaneCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.ByteType;
import mpicbg.imglib.type.numeric.integer.ShortType;
import mpicbg.imglib.type.numeric.integer.Unsigned12BitType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.real.FloatType;

/** @deprecated Use {@link ImageOpener} instead. */
/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class LOCI
{
	public static <T extends RealType<T>> Image<T> open( final String fileName )
	{
		return openLOCI( "", fileName, new ArrayContainerFactory() );
	}

	public static <T extends RealType<T>> Image<T> openLOCI( final String fileName, final ContainerFactory containerFactory )
	{
		return openLOCI( "", fileName, containerFactory );
	}

	public static <T extends RealType<T>> Image<T> openLOCI( final String path, final String fileName, final ContainerFactory containerFactory )
	{
		return openLOCI(path, fileName, containerFactory, -1, -1);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends RealType<T>> Image<T> openLOCI( String path, final String fileName, final ContainerFactory containerFactory, int from, int to)
	{
		path = checkPath( path );

		final String id = path + fileName;
		final IFormatReader r = new ChannelSeparator();
		
		try 
		{
			r.setId(id);
		
			final int pixelType = r.getPixelType();
			final int channels = r.getSizeC();
			final String pixelTypeString = FormatTools.getPixelTypeString(pixelType);
			
			
			if (!(pixelType == FormatTools.UINT8 || pixelType == FormatTools.UINT16 || pixelType == FormatTools.UINT32 || pixelType == FormatTools.FLOAT))
			{
				System.out.println("LOCI.openLOCI(): PixelType " + pixelTypeString + " not supported yet, returning. ");
				return null;
			}
			
			if ( channels > 1 && channels <= 3 && pixelType == FormatTools.UINT8 )
			{
				/* inconvertible types due to javac bug 6548436: return (Image<T>)openLOCIRGBALegacyType( path, fileName, new ImageFactory<RGBALegacyType>( new RGBALegacyType(), containerFactory ), from, to ); */
				return (Image)openLOCIRGBALegacyType( path, fileName, new ImageFactory<RGBALegacyType>( new RGBALegacyType(), containerFactory ), from, to );
			}			
			else if ( pixelType == FormatTools.FLOAT || pixelType == FormatTools.UINT32 )
			{
				/* inconvertible types due to javac bug 6548436: return (Image<T>)openLOCIFloatType( path, fileName, new ImageFactory<FloatType>( new FloatType(), containerFactory ), from, to ); */
				return (Image)openLOCIFloatType( path, fileName, new ImageFactory<FloatType>( new FloatType(), containerFactory ), from, to );
			}
			else if ( pixelType == FormatTools.UINT16 )
			{
				/* inconvertible types due to javac bug 6548436: return (Image<T>)openLOCIShortType( path, fileName, new ImageFactory<ShortType>( new ShortType(), containerFactory ), from, to ); */
				return (Image)openLOCIShortType( path, fileName, new ImageFactory<ShortType>( new ShortType(), containerFactory ), from, to );
			}
			else if ( pixelType == FormatTools.UINT8 )
			{
				/* inconvertible types due to javac bug 6548436: return (Image<T>)openLOCIByteType( path, fileName, new ImageFactory<ByteType>( new ByteType(), containerFactory ), from, to ); */
				return (Image)openLOCIUnsignedByteType( path, fileName, new ImageFactory<UnsignedByteType>( new UnsignedByteType(), containerFactory ), from, to );
			}
			else
			{
				return null;
			}
						
		}
		catch (IOException exc) { System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}
		catch (FormatException exc) {System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}		
	}

	public static Image<Unsigned12BitType> openLOCIUnsigned12BitType( final String fileName, final ContainerFactory factory )
	{
		return openLOCIUnsigned12BitType( fileName, new ImageFactory<Unsigned12BitType>( new Unsigned12BitType(), factory ) );
	}
	
	public static Image<Unsigned12BitType> openLOCIUnsigned12BitType( final String fileName, final ImageFactory<Unsigned12BitType> factory )
	{
		return openLOCIUnsigned12BitType( "", fileName, factory );
	}
	
	public static Image<Unsigned12BitType> openLOCIUnsigned12BitType( final String path, final String fileName, final ContainerFactory factory )
	{
		return openLOCIUnsigned12BitType(path, fileName, new ImageFactory<Unsigned12BitType>( new Unsigned12BitType(), factory ) );
	}

	public static Image<Unsigned12BitType> openLOCIUnsigned12BitType( final String path, final String fileName, final ImageFactory<Unsigned12BitType> factory )
	{
		return openLOCIUnsigned12BitType(path, fileName, factory, -1, -1 );
	}
	
	public static Image<Unsigned12BitType> openLOCIUnsigned12BitType( final String path, final String fileName, final ContainerFactory factory, int from, int to)
	{
		return openLOCIUnsigned12BitType( path, fileName, new ImageFactory<Unsigned12BitType>( new Unsigned12BitType(), factory ), from, to );
	}

	private static boolean createOMEXMLMetadata(final IFormatReader r)
	{
		try {
			final ServiceFactory serviceFactory = new ServiceFactory();
			final OMEXMLService service = serviceFactory
					.getInstance(OMEXMLService.class);
			final IMetadata omexmlMeta = service.createOMEXMLMetadata();
			r.setMetadataStore(omexmlMeta);
		} catch (final ServiceException e) {
			e.printStackTrace();
			return false;
		} catch (final DependencyException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static Image<Unsigned12BitType> openLOCIUnsigned12BitType( String path, final String fileName, final ImageFactory<Unsigned12BitType> factory, int from, int to )
	{				
		path = checkPath( path );
		final IFormatReader r = new ChannelSeparator();

		if ( !createOMEXMLMetadata(r) ) {
			try {
				r.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		final String id = path + fileName;
		
		try 
		{
			r.setId(id);
			
			final boolean isLittleEndian = r.isLittleEndian();			
			final int width = r.getSizeX();
			final int height = r.getSizeY();
			final int depth = r.getSizeZ();
			int timepoints = r.getSizeT();
			int channels = r.getSizeC();
			final int pixelType = r.getPixelType();
			final int bytesPerPixel = FormatTools.getBytesPerPixel(pixelType); 
			final String pixelTypeString = FormatTools.getPixelTypeString(pixelType);
			
			if ( timepoints > 1 )
			{
				System.out.println("LOCI.openLOCI(): More than one timepoint. Not implemented yet. Returning first timepoint");
				timepoints = 1;
			}
			
			if ( channels > 1 )
			{
				System.out.println("LOCI.openLOCI(): More than one channel. Image<ShortType> supports only 1 channel right now, returning the first channel.");
				channels = 1;
			}
			
			if (!(pixelType == FormatTools.UINT8 || pixelType == FormatTools.UINT16))
			{
				System.out.println("LOCI.openLOCI(): PixelType " + pixelTypeString + " not supported by ShortType, returning. ");
				return null;
			}
			
			final int start, end;			
			if (from < 0 || to < 0 || to < from)
			{
				start = 0; end = depth;
			}
			else 
			{
				start = from;
				if (to > depth)
					end = depth;
				else 
					end = to;
			}

			final Image<Unsigned12BitType> img;
			
			if ( end-start == 1)				
				img = factory.createImage( new int[]{ width, height }, fileName);
			else
				img = factory.createImage( new int[]{ width, height, end - start }, fileName);

			if (img == null)
			{
				System.out.println("LOCI.openLOCI():  - Could not create image.");
				return null;
			}
			else
			{
				System.out.println( "Opening '" + fileName + "' [" + width + "x" + height + "x" + depth + " type=" + pixelTypeString + " image=Image<ShortType>]" ); 
				img.setName( fileName );
			}
			
			// try read metadata
			try
			{
				applyMetaData( img, r );
			}
			catch (Exception e)
			{
				System.out.println( "Cannot read metadata: " + e );
			}
		
			final int t = 0;			
			final byte[][] b = new byte[channels][width * height * bytesPerPixel];
			
			final int[] planePos = new int[3];
			final int planeX = 0;
			final int planeY = 1;
									
			final LocalizablePlaneCursor<Unsigned12BitType> it = img.createLocalizablePlaneCursor();
			
			for (int z = start; z < end; z++)
			{	
				//System.out.println((z+1) + "/" + (end));
				
				// set the z plane iterator to the current z plane
				planePos[ 2 ] = z - start;
				it.reset( planeX, planeY, planePos );
				
				// read the data from LOCI
				for (int c = 0; c < channels; c++)
				{
					final int index = r.getIndex(z, c, t);
					r.openBytes(index, b[c]);	
				}
				
				// write data for that plane into the Image structure using the iterator
				if (channels == 1)
				{					
					if (pixelType == FormatTools.UINT8)
					{						
						while(it.hasNext())
						{
							it.fwd();
							it.getType().set( (short)(b[ 0 ][ it.getPosition( planeX )+it.getPosition( planeY )*width ] & 0xff) );
						}						
					}	
					else //if (pixelType == FormatTools.UINT16)
					{
						while(it.hasNext())
						{
							it.fwd();
							it.getType().set( getShortValue( b[ 0 ], ( it.getPosition( planeX )+it.getPosition( planeY )*width ) * 2, isLittleEndian ) );
						}
					}						
				}				
			}
			
			it.close();
			
			return img;			
			
		}
		catch (IOException exc) { System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}
		catch (FormatException exc) {System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}		
	}
	
	public static Image<ShortType> openLOCIShortType( final String fileName, final ContainerFactory factory )
	{
		return openLOCIShortType( fileName, new ImageFactory<ShortType>( new ShortType(), factory ) );
	}
	
	public static Image<ShortType> openLOCIShortType( final String fileName, final ImageFactory<ShortType> factory )
	{
		return openLOCIShortType( "", fileName, factory );
	}
	
	public static Image<ShortType> openLOCIShortType( final String path, final String fileName, final ContainerFactory factory )
	{
		return openLOCIShortType(path, fileName, new ImageFactory<ShortType>( new ShortType(), factory ) );
	}

	public static Image<ShortType> openLOCIShortType( final String path, final String fileName, final ImageFactory<ShortType> factory )
	{
		return openLOCIShortType(path, fileName, factory, -1, -1 );
	}
	
	public static Image<ShortType> openLOCIShortType( final String path, final String fileName, final ContainerFactory factory, int from, int to)
	{
		return openLOCIShortType( path, fileName, new ImageFactory<ShortType>( new ShortType(), factory ), from, to );
	}
	
	public static Image<ShortType> openLOCIShortType( String path, final String fileName, final ImageFactory<ShortType> factory, int from, int to )
	{				
		path = checkPath( path );
		final IFormatReader r = new ChannelSeparator();

		if ( !createOMEXMLMetadata(r) ) {
			try {
				r.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		final String id = path + fileName;

		final File dir = new File( path + fileName );
		
		// read many 2d-images if it is a directory
		if ( dir.isDirectory() )
		{
			final String[] files = dir.list();
			final int depth = dir.list().length;
			
			// get size of first image
			final Opener io = new Opener();
			ImagePlus imp2d = io.openImage( dir.getAbsolutePath() + File.separator + files[ 0 ] );

			System.out.println( "Opening '" + fileName + "' [" + imp2d.getWidth() + "x" + imp2d.getHeight() + "x" + depth + " type=" + imp2d.getProcessor().getClass().getSimpleName() + " image=Image<ShortType>]" );

			final Image<ShortType> output = factory.createImage( new int[] {imp2d.getWidth(), imp2d.getHeight(), depth }, fileName );			
			
			for ( int i = 0; i < depth; ++i )
			{
				imp2d = io.openImage( dir.getAbsolutePath() + File.separator + files[ i ] );
				final ShortProcessor ip = (ShortProcessor)imp2d.getProcessor();
				
				final LocalizablePlaneCursor<ShortType> cursorOut = output.createLocalizablePlaneCursor();
				cursorOut.reset( 0, 1, new int[]{ 0, 0, i } );
				
				while ( cursorOut.hasNext() )
				{
					cursorOut.fwd();
					cursorOut.getType().set( (short)ip.get( cursorOut.getPosition( 0 ), cursorOut.getPosition( 1 ) ) );
				}
			}			
			return output;
		}
		
		try 
		{
			r.setId(id);
			
			final boolean isLittleEndian = r.isLittleEndian();			
			final int width = r.getSizeX();
			final int height = r.getSizeY();
			final int depth = r.getSizeZ();
			int timepoints = r.getSizeT();
			int channels = r.getSizeC();
			final int pixelType = r.getPixelType();
			final int bytesPerPixel = FormatTools.getBytesPerPixel(pixelType); 
			final String pixelTypeString = FormatTools.getPixelTypeString(pixelType);
			
			if ( timepoints > 1 )
			{
				System.out.println("LOCI.openLOCI(): More than one timepoint. Not implemented yet. Returning first timepoint");
				timepoints = 1;
			}
			
			if ( channels > 1 )
			{
				System.out.println("LOCI.openLOCI(): More than one channel. Image<ShortType> supports only 1 channel right now, returning the first channel.");
				channels = 1;
			}
			
			if (!(pixelType == FormatTools.UINT8 || pixelType == FormatTools.UINT16))
			{
				System.out.println("LOCI.openLOCI(): PixelType " + pixelTypeString + " not supported by ShortType, returning. ");
				return null;
			}
			
			final int start, end;			
			if (from < 0 || to < 0 || to < from)
			{
				start = 0; end = depth;
			}
			else 
			{
				start = from;
				if (to > depth)
					end = depth;
				else 
					end = to;
			}

			final Image<ShortType> img;
			
			if ( end-start == 1)				
				img = factory.createImage( new int[]{ width, height }, fileName);
			else
				img = factory.createImage( new int[]{ width, height, end - start }, fileName);

			if (img == null)
			{
				System.out.println("LOCI.openLOCI():  - Could not create image.");
				return null;
			}
			else
			{
				System.out.println( "Opening '" + fileName + "' [" + width + "x" + height + "x" + depth + " type=" + pixelTypeString + " image=Image<ShortType>]" ); 
				img.setName( fileName );
			}
			
			// try read metadata
			try
			{
				applyMetaData( img, r );
			}
			catch (Exception e)
			{
				System.out.println( "Cannot read metadata: " + e );
			}
		
			final int t = 0;			
			final byte[][] b = new byte[channels][width * height * bytesPerPixel];
			
			final int[] planePos = new int[3];
			final int planeX = 0;
			final int planeY = 1;
									
			final LocalizablePlaneCursor<ShortType> it = img.createLocalizablePlaneCursor();
			
			for (int z = start; z < end; z++)
			{	
				//System.out.println((z+1) + "/" + (end));
				
				// set the z plane iterator to the current z plane
				planePos[ 2 ] = z - start;
				it.reset( planeX, planeY, planePos );
				
				// read the data from LOCI
				for (int c = 0; c < channels; c++)
				{
					final int index = r.getIndex(z, c, t);
					r.openBytes(index, b[c]);	
				}
				
				// write data for that plane into the Image structure using the iterator
				if (channels == 1)
				{					
					if (pixelType == FormatTools.UINT8)
					{						
						while(it.hasNext())
						{
							it.fwd();
							it.getType().set( (short)(b[ 0 ][ it.getPosition( planeX )+it.getPosition( planeY )*width ] & 0xff) );
						}						
					}	
					else //if (pixelType == FormatTools.UINT16)
					{
						while(it.hasNext())
						{
							it.fwd();
							it.getType().set( getShortValue( b[ 0 ], ( it.getPosition( planeX )+it.getPosition( planeY )*width ) * 2, isLittleEndian ) );
						}
					}						
				}				
			}
			
			it.close();
			
			return img;			
			
		}
		catch (IOException exc) { System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}
		catch (FormatException exc) {System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}		
	}
	
	protected static void applyMetaData( final Image<?> img, final IFormatReader reader )
	{
		for ( int d = 0; d < img.getNumDimensions(); ++d )
			img.setCalibration( 1, d );

		try
		{
			final MetadataRetrieve retrieve = (MetadataRetrieve)reader.getMetadataStore();
			
			float cal = retrieve.getPixelsPhysicalSizeX( 0 ).getValue().floatValue();
			if ( cal == 0)
			{
				cal = 1;
				System.out.println( "LOCI.openLOCI(): Warning, calibration for dimension 0 seems corrupted, setting to 1." );
			}
			
			img.setCalibration( cal , 0 );
			
			if ( img.getNumDimensions() >= 2 )
			{
				cal = retrieve.getPixelsPhysicalSizeY( 0 ).getValue().floatValue();
				if ( cal == 0)
				{
					cal = 1;
					System.out.println( "LOCI.openLOCI(): Warning, calibration for dimension 1 seems corrupted, setting to 1." );
				}
				img.setCalibration( cal, 1 );
			}
			
			if ( img.getNumDimensions() >= 3 )
			{
				cal = retrieve.getPixelsPhysicalSizeZ( 0 ).getValue().floatValue();
				if ( cal == 0)
				{
					cal = 1;
					System.out.println( "LOCI.openLOCI(): Warning, calibration for dimension 2 seems corrupted, setting to 1." );
				}
				img.setCalibration( cal, 2 );
			}

			if ( img.getNumDimensions() >= 4 )
			{
				cal = retrieve.getPixelsTimeIncrement( 0 ).floatValue();
				if ( cal == 0)
				{
					cal = 1;
					System.out.println( "LOCI.openLOCI(): Warning, calibration for dimension 3 seems corrupted, setting to 1." );
				}
				img.setCalibration( cal, 3 );
			}
			
		}
		catch( Exception e )
		{
			System.out.println( "LOCI.openLOCI(): Cannot read metadata, setting calibration to 1" );
			return;
		}
	}

	public static Image<FloatType> openLOCIFloatType( final String fileName, final ContainerFactory factory )
	{
		return openLOCIFloatType( fileName, new ImageFactory<FloatType>( new FloatType(), factory ) );
	}
	
	public static Image<FloatType> openLOCIFloatType( final String fileName, final ImageFactory<FloatType> factory )
	{
		return openLOCIFloatType( "", fileName, factory );
	}
	
	public static Image<FloatType> openLOCIFloatType( final String path, final String fileName, final ContainerFactory factory )
	{
		return openLOCIFloatType(path, fileName, new ImageFactory<FloatType>( new FloatType(), factory ) );
	}

	public static Image<FloatType> openLOCIFloatType( final String path, final String fileName, final ImageFactory<FloatType> factory )
	{
		return openLOCIFloatType(path, fileName, factory, -1, -1 );
	}
	
	public static Image<FloatType> openLOCIFloatType( final String path, final String fileName, final ContainerFactory factory, int from, int to)
	{
		return openLOCIFloatType( path, fileName, new ImageFactory<FloatType>( new FloatType(), factory ), from, to );
	}

	public static Image<FloatType> openLOCIFloatType( String path, final String fileName, final ImageFactory<FloatType> factory, int from, int to )
	{						
		path = checkPath( path );
		
		final File dir = new File( path + fileName );
		
		// read many 2d-images if it is a directory
		if ( dir.isDirectory() )
		{
			final String[] files = dir.list( new FilenameFilter() 
			{	
				@Override
				public boolean accept( final File dir, final String name) 
				{
					final File newFile = new File( dir, name );
					
					// ignore directories and hidden files
					if ( newFile.isHidden() || newFile.isDirectory() )
						return false;
					else
						return true;
				}
			});
			Arrays.sort( files );
			final int depth = files.length;
			
			// get size of first image
			final Opener io = new Opener();
			ImagePlus imp2d = io.openImage( dir.getAbsolutePath() + File.separator + files[ 0 ] );

			System.out.println( "Opening '" + fileName + "' [" + imp2d.getWidth() + "x" + imp2d.getHeight() + "x" + depth + " type=" + imp2d.getProcessor().getClass().getSimpleName() + " image=Image<FloatType>]" );

			final Image<FloatType> output = factory.createImage( new int[] {imp2d.getWidth(), imp2d.getHeight(), depth }, fileName );			
			
			for ( int i = 0; i < depth; ++i )
			{
				imp2d = io.openImage( dir.getAbsolutePath() + File.separator + files[ i ] );
				final ImageProcessor ip = imp2d.getProcessor();
				
				final LocalizablePlaneCursor<FloatType> cursorOut = output.createLocalizablePlaneCursor();
				cursorOut.reset( 0, 1, new int[]{ 0, 0, i } );
				
				while ( cursorOut.hasNext() )
				{
					cursorOut.fwd();
					cursorOut.getType().set( ip.getPixelValue( cursorOut.getPosition( 0 ), cursorOut.getPosition( 1 ) ) );
				}
			}			
			return output;
		}

		final IFormatReader r = new ChannelSeparator();

		if ( !createOMEXMLMetadata(r) ) {
			try {
				r.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		final String id = path + fileName;
		
		try 
		{
			r.setId( id );
						
			final boolean isLittleEndian = r.isLittleEndian();			
			final int width = r.getSizeX();
			final int height = r.getSizeY();
			final int depth = r.getSizeZ();				
			int timepoints = r.getSizeT();
			int channels = r.getSizeC();
			final int pixelType = r.getPixelType();
			final int bytesPerPixel = FormatTools.getBytesPerPixel(pixelType); 
			final String pixelTypeString = FormatTools.getPixelTypeString(pixelType);
			
			if ( timepoints > 1 )
			{
				System.out.println("LOCI.openLOCI(): More than one timepoint. Not implemented yet. Returning first timepoint");
				timepoints = 1;
			}
			
			if ( channels > 1 )
			{
				System.out.println("LOCI.openLOCI(): More than one channel. Image<FloatType> supports only 1 channel right now, returning the first channel.");
				channels = 1;
			}
			
			if (!(pixelType == FormatTools.UINT8 || pixelType == FormatTools.UINT16 || pixelType == FormatTools.UINT32 || pixelType == FormatTools.FLOAT))
			{
				System.out.println("LOCI.openLOCI(): PixelType " + pixelTypeString + " not supported by FloatType, returning. ");
				return null;
			}
			
			final int start, end;			
			if (from < 0 || to < 0 || to < from)
			{
				start = 0; end = depth;
			}
			else 
			{
				start = from;
				if (to > depth)
					end = depth;
				else 
					end = to;
			}

			final Image<FloatType> img;		
			
			if ( end-start == 1)				
				img = factory.createImage( new int[]{ width, height }, fileName);
			else
				img = factory.createImage( new int[]{ width, height, end - start }, fileName);

			if (img == null)
			{
				System.out.println("LOCI.openLOCI():  - Could not create image.");
				return null;
			}
			else
			{
				System.out.println( "Opening '" + fileName + "' [" + width + "x" + height + "x" + depth + " type=" + pixelTypeString + " image=Image<FloatType>]" );
				img.setName( fileName );
			}
			
			// try read metadata
			try
			{
				applyMetaData( img, r );
			}
			catch (Exception e)
			{
				System.out.println( "Cannot read metadata: " + e );
			}
		
			final int t = 0;			
			final byte[][] b = new byte[channels][width * height * bytesPerPixel];
			
			final int[] planePos = new int[3];
			final int planeX = 0;
			final int planeY = 1;
									
			final LocalizablePlaneCursor<FloatType> it = img.createLocalizablePlaneCursor();
			
			for (int z = start; z < end; z++)
			{	
				//System.out.println((z+1) + "/" + (end));
				
				// set the z plane iterator to the current z plane
				planePos[ 2 ] = z - start;
				it.reset( planeX, planeY, planePos );
				
				// read the data from LOCI
				for (int c = 0; c < channels; c++)
				{
					final int index = r.getIndex(z, c, t);
					r.openBytes(index, b[c]);	
				}
				
				// write data for that plane into the Image structure using the iterator
				if (channels == 1)
				{					
					if (pixelType == FormatTools.UINT8)
					{						
						while(it.hasNext())
						{
							it.fwd();
							it.getType().set( b[ 0 ][ it.getPosition( planeX )+it.getPosition( planeY )*width ] & 0xff );
						}
						
					}	
					else if (pixelType == FormatTools.UINT16)
					{
						while(it.hasNext())
						{
							it.fwd();
							it.getType().set( getShortValueInt( b[ 0 ], ( it.getPosition( planeX )+it.getPosition( planeY )*width ) * 2, isLittleEndian ) );
						}
					}						
					else if (pixelType == FormatTools.UINT32)
					{
						//TODO: Untested

						while(it.hasNext())
						{
							it.fwd();
							it.getType().set( getIntValue( b[ 0 ], ( it.getPosition( planeX )+it.getPosition( planeY )*width )*4, isLittleEndian ) );
						}

					}
					else if (pixelType == FormatTools.FLOAT)
					{
						while(it.hasNext())
						{
							it.fwd();
							it.getType().set( getFloatValue( b[ 0 ], ( it.getPosition( planeX )+it.getPosition( planeY )*width )*4, isLittleEndian ) );
						}

					}
				}				
			}
			
			it.close();
			
			return img;			
			
		}
		catch (IOException exc) { exc.printStackTrace(); System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}
		catch (FormatException exc) { exc.printStackTrace(); System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}		
	}

	public static Image<ByteType> openLOCIByteType( final String fileName, final ContainerFactory factory )
	{
		return openLOCIByteType( "", fileName, new ImageFactory<ByteType>( new ByteType(), factory ) );
	}

	public static Image<ByteType> openLOCIByteType( final String fileName, final ImageFactory<ByteType> factory )
	{
		return openLOCIByteType( "", fileName, factory );
	}
	
	public static Image<ByteType> openLOCIByteType( final String path, final String fileName, final ContainerFactory factory )
	{
		return openLOCIByteType(path, fileName, new ImageFactory<ByteType>( new ByteType(), factory ) );
	}

	public static Image<ByteType> openLOCIByteType( final String path, final String fileName, final ImageFactory<ByteType> factory )
	{
		return openLOCIByteType(path, fileName, factory, -1, -1 );
	}
	
	public static Image<ByteType> openLOCIByteType( String path, final String fileName, final ImageFactory<ByteType> factory, int from, int to )
	{				
		path = checkPath( path );
		final IFormatReader r = new ChannelSeparator();

		if ( !createOMEXMLMetadata(r) ) {
			try {
				r.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		final String id = path + fileName;
		
		try 
		{
			r.setId(id);
			
			final int width = r.getSizeX();
			final int height = r.getSizeY();
			final int depth = r.getSizeZ();
			int timepoints = r.getSizeT();
			int channels = r.getSizeC();
			final int pixelType = r.getPixelType();
			final int bytesPerPixel = FormatTools.getBytesPerPixel(pixelType); 
			final String pixelTypeString = FormatTools.getPixelTypeString(pixelType);
			
			if ( timepoints > 1 )
			{
				System.out.println("LOCI.openLOCI(): More than one timepoint. Not implemented yet. Returning first timepoint");
				timepoints = 1;
			}
			
			if ( channels > 1 )
			{
				System.out.println("LOCI.openLOCI(): More than one channel. Image<ByteType> supports only 1 channel right now, returning the first channel.");
				channels = 1;
			}
			
			if (!(pixelType == FormatTools.UINT8))
			{
				System.out.println("LOCI.openLOCI(): PixelType " + pixelTypeString + " not supported by ByteType, returning. ");
				return null;
			}
			
			final int start, end;			
			if (from < 0 || to < 0 || to < from)
			{
				start = 0; end = depth;
			}
			else 
			{
				start = from;
				if (to > depth)
					end = depth;
				else 
					end = to;
			}

			final Image<ByteType> img;
			
			if ( end-start == 1)				
				img = factory.createImage( new int[]{ width, height }, fileName);
			else
				img = factory.createImage( new int[]{ width, height, end - start }, fileName);

			if (img == null)
			{
				System.out.println("LOCI.openLOCI():  - Could not create image.");
				return null;
			}
			else
			{
				System.out.println( "Opening '" + fileName + "' [" + width + "x" + height + "x" + depth + " type=" + pixelTypeString + " image=Image<ByteType>]" ); 
				img.setName( fileName );
			}
			
			// try read metadata
			try
			{
				applyMetaData( img, r );
			}
			catch (Exception e)
			{
				System.out.println( "Cannot read metadata: " + e );
			}
		
			final int t = 0;			
			final byte[][] b = new byte[channels][width * height * bytesPerPixel];
			
			final int[] planePos = new int[3];
			final int planeX = 0;
			final int planeY = 1;
									
			LocalizablePlaneCursor<ByteType> it = img.createLocalizablePlaneCursor();
			final ByteType type = it.getType();

			
			for (int z = start; z < end; z++)
			{	
				//System.out.println((z+1) + "/" + (end));
				
				// set the z plane iterator to the current z plane
				planePos[ 2 ] = z - start;
				it.reset( planeX, planeY, planePos );
				
				// read the data from LOCI
				for (int c = 0; c < channels; c++)
				{
					final int index = r.getIndex(z, c, t);
					r.openBytes(index, b[c]);	
				}
				
				// write data for that plane into the Image structure using the iterator
				if (channels == 1)
				{					
						while(it.hasNext())
						{
							it.fwd();
							type.set( b[ 0 ][ it.getPosition( planeX )+it.getPosition( planeY )*width ] );
						}						
				}				
			}
			
			it.close();
			
			return img;			
			
		}
		catch (IOException exc) { System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}
		catch (FormatException exc) {System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}		
	}

	public static Image<UnsignedByteType> openLOCIUnsignedByteType( final String fileName, final ContainerFactory factory )
	{
		return openLOCIUnsignedByteType( "", fileName, new ImageFactory<UnsignedByteType>( new UnsignedByteType(), factory ) );
	}

	public static Image<UnsignedByteType> openLOCIUnsignedByteType( final String fileName, final ImageFactory<UnsignedByteType> factory )
	{
		return openLOCIUnsignedByteType( "", fileName, factory );
	}
	
	public static Image<UnsignedByteType> openLOCIUnsignedByteType( final String path, final String fileName, final ContainerFactory factory )
	{
		return openLOCIUnsignedByteType(path, fileName, new ImageFactory<UnsignedByteType>( new UnsignedByteType(), factory ) );
	}

	public static Image<UnsignedByteType> openLOCIUnsignedByteType( final String path, final String fileName, final ImageFactory<UnsignedByteType> factory )
	{
		return openLOCIUnsignedByteType(path, fileName, factory, -1, -1 );
	}
	
	public static Image<UnsignedByteType> openLOCIUnsignedByteType( String path, final String fileName, final ImageFactory<UnsignedByteType> factory, int from, int to )
	{				
		path = checkPath( path );
		final IFormatReader r = new ChannelSeparator();

		if ( !createOMEXMLMetadata(r) ) {
			try {
				r.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		final String id = path + fileName;
		
		try 
		{
			r.setId(id);
			
			final int width = r.getSizeX();
			final int height = r.getSizeY();
			final int depth = r.getSizeZ();
			int timepoints = r.getSizeT();
			int channels = r.getSizeC();
			final int pixelType = r.getPixelType();
			final int bytesPerPixel = FormatTools.getBytesPerPixel(pixelType); 
			final String pixelTypeString = FormatTools.getPixelTypeString(pixelType);
			
			if ( timepoints > 1 )
			{
				System.out.println("LOCI.openLOCI(): More than one timepoint. Not implemented yet. Returning first timepoint");
				timepoints = 1;
			}
			
			if ( channels > 1 )
			{
				System.out.println("LOCI.openLOCI(): More than one channel. Image<UnsignedByteType> supports only 1 channel right now, returning the first channel.");
				channels = 1;
			}
			
			if (!(pixelType == FormatTools.UINT8))
			{
				System.out.println("LOCI.openLOCI(): PixelType " + pixelTypeString + " not supported by UnsignedByteType, returning. ");
				return null;
			}
			
			final int start, end;			
			if (from < 0 || to < 0 || to < from)
			{
				start = 0; end = depth;
			}
			else 
			{
				start = from;
				if (to > depth)
					end = depth;
				else 
					end = to;
			}

			final Image<UnsignedByteType> img;
			
			if ( end-start == 1)				
				img = factory.createImage( new int[]{ width, height }, fileName);
			else
				img = factory.createImage( new int[]{ width, height, end - start }, fileName);

			if (img == null)
			{
				System.out.println("LOCI.openLOCI():  - Could not create image.");
				return null;
			}
			else
			{
				System.out.println( "Opening '" + fileName + "' [" + width + "x" + height + "x" + depth + " type=" + pixelTypeString + " image=Image<ByteType>]" ); 
				img.setName( fileName );
			}
			
			// try read metadata
			try
			{
				applyMetaData( img, r );
			}
			catch (Exception e)
			{
				System.out.println( "Cannot read metadata: " + e );
			}
		
			final int t = 0;			
			final byte[][] b = new byte[channels][width * height * bytesPerPixel];
			
			final int[] planePos = new int[3];
			final int planeX = 0;
			final int planeY = 1;
									
			LocalizablePlaneCursor<UnsignedByteType> it = img.createLocalizablePlaneCursor();
			final UnsignedByteType type = it.getType();

			
			for (int z = start; z < end; z++)
			{	
				//System.out.println((z+1) + "/" + (end));
				
				// set the z plane iterator to the current z plane
				planePos[ 2 ] = z - start;
				it.reset( planeX, planeY, planePos );
				
				// read the data from LOCI
				for (int c = 0; c < channels; c++)
				{
					final int index = r.getIndex(z, c, t);
					r.openBytes(index, b[c]);	
				}
				
				// write data for that plane into the Image structure using the iterator
				if (channels == 1)
				{					
						while(it.hasNext())
						{
							it.fwd();
							type.set( UnsignedByteType.getUnsignedByte( b[ 0 ][ it.getPosition( planeX )+it.getPosition( planeY )*width ] ) );
						}						
				}				
			}
			
			it.close();
			
			return img;			
			
		}
		catch (IOException exc) { System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}
		catch (FormatException exc) {System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}		
	}

	public static Image<RGBALegacyType> openLOCIRGBALegacyType( final String path, final String fileName, final ImageFactory<RGBALegacyType> factory )
	{
		return openLOCIRGBALegacyType(path, fileName, factory, -1, -1 );
	}
		
	public static Image<RGBALegacyType> openLOCIRGBALegacyType( String path, final String fileName, final ImageFactory<RGBALegacyType> factory, int from, int to )
	{				
		path = checkPath( path );
		final IFormatReader r = new ChannelSeparator();

		if ( !createOMEXMLMetadata(r) ) {
			try {
				r.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		final String id = path + fileName;
		
		try 
		{
			r.setId(id);
			
			final int width = r.getSizeX();
			final int height = r.getSizeY();
			final int depth = r.getSizeZ();
			int timepoints = r.getSizeT();
			int channels = r.getSizeC();
			final int pixelType = r.getPixelType();
			final int bytesPerPixel = FormatTools.getBytesPerPixel(pixelType); 
			final String pixelTypeString = FormatTools.getPixelTypeString(pixelType);
			
			if ( timepoints > 1 )
			{
				System.out.println("LOCI.openLOCI(): More than one timepoint. Not implemented yet. Returning first timepoint");
				timepoints = 1;
			}
			
			if ( channels > 3 )
			{
				System.out.println("LOCI.openLOCI(): More than one channel. Image<RGBALegacyType> supports only 3 channels right now, returning the first 3 channels.");
				channels = 3;
			}
			
			if (!(pixelType == FormatTools.UINT8))
			{
				System.out.println("LOCI.openLOCI(): PixelType " + pixelTypeString + " not supported by RGBALegacyType, returning. ");
				return null;
			}
			
			final int start, end;			
			if (from < 0 || to < 0 || to < from)
			{
				start = 0; end = depth;
			}
			else 
			{
				start = from;
				if (to > depth)
					end = depth;
				else 
					end = to;
			}

			final Image<RGBALegacyType> img;
			
			if ( end-start == 1)				
				img = factory.createImage( new int[]{ width, height }, fileName);
			else
				img = factory.createImage( new int[]{ width, height, end - start }, fileName);

			if (img == null)
			{
				System.out.println("LOCI.openLOCI():  - Could not create image.");
				return null;
			}
			else
			{
				System.out.println( "Opening '" + fileName + "' [" + width + "x" + height + "x" + depth + " channels=" + channels + " type=" + pixelTypeString + " image=RGBALegacyTypeImage]" ); 
				img.setName( fileName );
			}
			
			// try read metadata
			try
			{
				applyMetaData( img, r );
			}
			catch (Exception e)
			{
				System.out.println( "Cannot read metadata: " + e );
			}

			final int t = 0;			
			final byte[][] b = new byte[channels][width * height * bytesPerPixel];
			
			final int[] planePos = new int[3];
			final int planeX = 0;
			final int planeY = 1;
									
			final LocalizablePlaneCursor<RGBALegacyType> it = img.createLocalizablePlaneCursor();
			
			for (int z = start; z < end; z++)
			{	
				// set the z plane iterator to the current z plane
				planePos[ 2 ] = z - start;
				it.reset( planeX, planeY, planePos );
				
				// read the data from LOCI
				for (int c = 0; c < channels; c++)
				{
					final int index = r.getIndex(z, c, t);
					r.openBytes(index, b[c]);	
				}
				
				final byte[] col = new byte[ 3 ];
				
				// write data for that plane into the Image structure using the iterator
				while( it.hasNext() )
				{
					it.fwd();
					
					for ( int channel = 0; channel < channels; ++channel )
						col[ channels - channel - 1 ] = b[ channel ][ it.getPosition( planeX )+it.getPosition( planeY )*width ];						
					
					it.getType().set( RGBALegacyType.rgba( col[ 0 ], col[ 1 ], col[ 2 ], 0) );
				}						
			}
			
			it.close();
			
			return img;			
			
		}
		catch (IOException exc) { System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}
		catch (FormatException exc) {System.out.println("LOCI.openLOCI(): Sorry, an error occurred: " + exc.getMessage()); return null;}		
	}
	
	protected static String checkPath( String path )
	{
		if (path.length() > 1) 
		{
			path = path.replace('\\', '/');
			if (!path.endsWith("/"))
				path = path + "/";
		}
		
		return path;
	}

	private static final float getFloatValue( final byte[] b, final int i, final boolean isLittleEndian )
	{
		if ( isLittleEndian )
			return Float.intBitsToFloat( ((b[i+3] & 0xff) << 24)  + ((b[i+2] & 0xff) << 16)  +  ((b[i+1] & 0xff) << 8)  + (b[i] & 0xff) );
		else
			return Float.intBitsToFloat( ((b[i] & 0xff) << 24)  + ((b[i+1] & 0xff) << 16)  +  ((b[i+2] & 0xff) << 8)  + (b[i+3] & 0xff) );
	}

	private static final int getIntValue( final byte[] b, final int i, final boolean isLittleEndian )
	{
		// TODO: Untested
		if ( isLittleEndian )
			return ( ((b[i+3] & 0xff) << 24)  + ((b[i+2] & 0xff) << 16)  +  ((b[i+1] & 0xff) << 8)  + (b[i] & 0xff) );
		else
			return ( ((b[i] & 0xff) << 24)  + ((b[i+1] & 0xff) << 16)  +  ((b[i+2] & 0xff) << 8)  + (b[i+3] & 0xff) );
	}
	
	private static final short getShortValue( final byte[] b, final int i, final boolean isLittleEndian )
	{
		return (short)getShortValueInt( b, i, isLittleEndian );
	}

	private static final int getShortValueInt( final byte[] b, final int i, final boolean isLittleEndian )
	{
		if ( isLittleEndian )
			return ((((b[i+1] & 0xff) << 8)) + (b[i] & 0xff));
		else
			return ((((b[i] & 0xff) << 8)) + (b[i+1] & 0xff));
	}
	
}
