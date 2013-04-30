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
import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.basictypecontainer.array.ArrayDataAccess;
import mpicbg.imglib.container.planar.PlanarContainer;
import mpicbg.imglib.cursor.imageplus.ImagePlusCursor;
import mpicbg.imglib.cursor.imageplus.ImagePlusCursor2D;
import mpicbg.imglib.cursor.imageplus.ImagePlusLocalizableByDimCursor;
import mpicbg.imglib.cursor.imageplus.ImagePlusLocalizableByDimOutOfBoundsCursor;
import mpicbg.imglib.cursor.imageplus.ImagePlusLocalizableCursor;
import mpicbg.imglib.cursor.imageplus.ImagePlusLocalizablePlaneCursor;
import mpicbg.imglib.exception.ImgLibException;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * A {@link Container} that stores data in an aray of 2d-slices each as a
 * linear array of basic types.  For types that are supported by ImageJ (byte,
 * short, int, float), an actual ImagePlus is created or used to store the
 * data.  Alternatively, an {@link ImagePlusContainer} can be created using
 * an already existing {@link ImagePlus} instance. 
 * 
 * {@link ImagePlusContainer ImagePlusContainers} provides a legacy layer to
 * apply imglib-based algorithm implementations directly on the data stored in
 * an ImageJ {@link ImagePlus}.  For all types that are suported by ImageJ, the
 * {@link ImagePlusContainer} provides access to the pixels of an
 * {@link ImagePlus} instance that can be accessed ({@see #getImagePlus()}.
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
public class ImagePlusContainer<T extends Type<T>, A extends ArrayDataAccess<A>> extends PlanarContainer<T,A>
{
	final protected ImagePlusContainerFactory factory;
	final protected int width, height, depth, frames, channels;
	
	protected ImagePlusContainer(
			final ImagePlusContainerFactory factory,
			final int width,
			final int height,
			final int depth,
			final int frames,
			final int channels,
			final int entitiesPerPixel )
	{
		super( factory, reduceDimensions( new int[]{ width, height, channels, depth, frames } ), entitiesPerPixel );
		
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.frames = frames;
		this.channels = channels;
		
		this.factory = factory; 
	}

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
	ImagePlusContainer( final ImagePlusContainerFactory factory, final int[] dim, final int entitiesPerPixel ) 
	{
		super( factory, dim, entitiesPerPixel );
		
		assert dim.length < 6 : "ImagePlusContainer can only handle up to 5 dimensions.";

		if ( dim.length > 0 )
			width = dim[ 0 ];
		else
			width = 1;

		if ( dim.length > 1 )
			height = dim[ 1 ];
		else
			height = 1;

		if ( dim.length > 2 )
			channels = dim[ 2 ];
		else
			channels = 1;

		if ( dim.length > 3 )
			depth = dim[ 3 ];
		else
			depth = 1;

		if ( dim.length > 4 )
			frames = dim[ 4 ];
		else
			frames = 1;
		
		this.factory = factory;
	}
	
	ImagePlusContainer( final ImagePlusContainerFactory factory, final A creator, final int[] dim, final int entitiesPerPixel ) 
	{
		this( factory, dim, entitiesPerPixel );
		
		mirror.clear();
		
		for ( int i = 0; i < slices; ++i )
			mirror.add( creator.createArray( width * height * entitiesPerPixel ) );
	}

	public ImagePlus getImagePlus() throws ImgLibException 
	{ 
		throw new ImgLibException( this, "has no ImagePlus instance, it is not a standard type of ImagePlus" ); 
	}

	/**
	 * Estimate the minimal required number of dimensions for a given
	 * {@link ImagePlus}, whereas width and height are always first.
	 * 
	 * E.g. a gray-scale 2d time series would have three dimensions
	 * [width,height,frames], a gray-scale 3d stack [width,height,depth] and a
	 * 2d composite image [width,height,channels] as well.  A composite 3d
	 * stack has four dimensions [width,height,channels,depth], as a time
	 * series five [width,height,channels,depth,frames].
	 * 
	 * @param imp
	 * @return
	 */
	protected static int[] reduceDimensions( final ImagePlus imp )
	{
		return reduceDimensions( imp.getDimensions() );
	}
	
	protected static int[] reduceDimensions( final int[] impDimensions )
	{
		/* ImagePlus is at least 2d, x,y are mapped to an index on a stack slice */
		int n = 2;
		for ( int d = 2; d < impDimensions.length; ++d )
			if ( impDimensions[ d ] > 1 ) ++n;
		
		final int[] dim = new int[ n ];
		dim[ 0 ] = impDimensions[ 0 ];
		dim[ 1 ] = impDimensions[ 1 ];
		
		n = 1;
		
		/* channels */
		if ( impDimensions[ 2 ] > 1 )
			dim[ ++n ] = impDimensions[ 2 ];
		
		/* depth */
		if ( impDimensions[ 3 ] > 1 )
			dim[ ++n ] = impDimensions[ 3 ];
		
		/* frames */
		if ( impDimensions[ 4 ] > 1 )
			dim[ ++n ] = impDimensions[ 4 ];
		
		return dim;
	}

	public int getWidth() { return width; }
	
	public int getHeight() { return height; }
	
	public int getChannels() { return channels; }
	
	public int getDepth() { return depth; }
	
	public int getFrames() { return frames; }

	@Override
	public ImagePlusCursor<T> createCursor( final Image<T> image ) 
	{
		if ( numDimensions == 2 )
			return new ImagePlusCursor2D< T >( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
		else
			return new ImagePlusCursor< T >( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
	}

	@Override
	public ImagePlusLocalizableCursor<T> createLocalizableCursor( final Image<T> image ) 
	{
		return new ImagePlusLocalizableCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
	}

	@Override
	public ImagePlusLocalizablePlaneCursor<T> createLocalizablePlaneCursor( final Image<T> image ) 
	{
		return new ImagePlusLocalizablePlaneCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
	}

	@Override
	public ImagePlusLocalizableByDimCursor<T> createLocalizableByDimCursor( final Image<T> image ) 
	{
		return new ImagePlusLocalizableByDimCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer() );
	}

	@Override
	public ImagePlusLocalizableByDimOutOfBoundsCursor<T> createLocalizableByDimCursor( final Image<T> image, OutOfBoundsStrategyFactory<T> outOfBoundsFactory ) 
	{
		return new ImagePlusLocalizableByDimOutOfBoundsCursor<T>( this, image, linkedType.duplicateTypeOnSameDirectAccessContainer(), outOfBoundsFactory );
	}
	
	@Override
	public ImagePlusContainerFactory getFactory() { return factory; }

}
