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
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.container.imageplus;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ShortProcessor;
import mpicbg.imglib.container.basictypecontainer.array.ShortArray;
import mpicbg.imglib.exception.ImgLibException;
import mpicbg.imglib.type.Type;

/**
 * {@link ImagePlusContainer} for short-stored data.
 * 
 *   Johannes Schindelin
 *
 * @author Funke
 * @author Preibisch
 * @author Rueden
 * @author Saalfeld
 * @author Schindelin
 * @author Jan Funke
 * @author Stephan Preibisch
 * @author Curtis Rueden
 * @author Stephan Saalfeld
 */
public class ShortImagePlus<T extends Type<T>> extends ImagePlusContainer<T, ShortArray> 
{
	final protected ImagePlus imp;	
	
	/**
	 * Standard constructor as called by default factories.
	 * 
	 * <em>Note that this constructor does not know about the meaning of
	 * dimensions > 1, and will use them in the {@link ImagePlus} default order
	 * x,y,c,z,t.  That is, from two dimensions, it will create an x,y image,
	 * from three dimensions, an x,y,c image, and from four dimensions, an
	 * x,y,c,z image.</em>
	 * 
	 * @param factory
	 * @param dim
	 * @param entitiesPerPixel
	 */
	public ShortImagePlus( final ImagePlusContainerFactory factory, final int[] dim, final int entitiesPerPixel ) 
	{
		super( factory, dim, entitiesPerPixel );
		
		mirror.clear();
		
		if ( entitiesPerPixel == 1 )
		{
			final ImageStack stack = new ImageStack( width, height );
			for ( int i = 0; i < slices; ++i )
				stack.addSlice( "", new ShortProcessor( width, height ) );
			imp = new ImagePlus( "image", stack );
			imp.setDimensions( channels, depth, frames );
			if ( slices > 1 )
				imp.setOpenAsHyperStack( true );
			
			for ( int t = 0; t < frames; ++t )
				for ( int z = 0; z < depth; ++z )
					for ( int c = 0; c < channels; ++c )
						mirror.add( new ShortArray( ( short[] )imp.getStack().getProcessor( imp.getStackIndex( c + 1, z + 1 , t + 1 ) ).getPixels() ) );
		}
		else
		{
			imp = null;
			for ( int i = 0; i < slices; ++i )
				mirror.add( new ShortArray( width * height * entitiesPerPixel ) );
		}
	}

	public ShortImagePlus( final ImagePlus imp, final ImagePlusContainerFactory factory ) 
	{
		super(
				factory,
				imp.getWidth(),
				imp.getHeight(),
				imp.getNSlices(),
				imp.getNFrames(),
				imp.getNChannels(),
				1 );
		
		this.imp = imp;
		
		mirror.clear();
		
		for ( int t = 0; t < frames; ++t )
			for ( int z = 0; z < depth; ++z )
				for ( int c = 0; c < channels; ++c )
					mirror.add( new ShortArray( ( short[] )imp.getStack().getProcessor( imp.getStackIndex( c + 1, z + 1 , t + 1 ) ).getPixels() ) );
	}

	/**
	 * This has to be overwritten, otherwise two different instances exist (one in the imageplus, one in the mirror)
	 */
	@Override
	public void setPlane( final int no, final ShortArray plane ) 
	{
		// TODO: this should work, but does not for plane 0, why??? 
		//mirror.set( no, plane );		
		//imp.getStack().setPixels( plane.getCurrentStorageArray(), no + 1 );
		
		System.arraycopy( plane.getCurrentStorageArray(), 0, mirror.get( no ).getCurrentStorageArray(), 0, plane.getCurrentStorageArray().length );
	}

	@Override
	public void close() 
	{
		super.close();
		if ( imp != null )
			imp.close(); 
	}

	@Override
	public ImagePlus getImagePlus() throws ImgLibException 
	{
		if ( imp == null )
			throw new ImgLibException( this, "has no ImagePlus instance, it is not a standard type of ImagePlus (" + entitiesPerPixel + " entities per pixel)" ); 
		else
			return imp;
	}
}

