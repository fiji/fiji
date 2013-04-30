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

package mpicbg.imglib.algorithm.transformation;

import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.type.Type;
import mpicbg.models.AffineModel2D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.Boundable;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.models.RigidModel2D;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class ImageTransform<T extends Type<T>> implements OutputAlgorithm<T>
{
	final protected InvertibleCoordinateTransform transform;
	final protected Boundable transformAsBoundable;
	final protected Image<T> img;
	final protected int numDimensions;
	protected InterpolatorFactory<T> interpolatorFactory;
	final protected boolean isAffine;

	ImageFactory<T> outputImageFactory;

	final int[] newDim;
	final float[] offset;

	Image<T> transformed;
	String errorMessage = "";

	public < BT extends InvertibleCoordinateTransform & Boundable >ImageTransform( final Image<T> img, final BT transform, final InterpolatorFactory<T> interpolatorFactory )
	{
		this.img = img;
		this.interpolatorFactory = interpolatorFactory;
		this.numDimensions = img.getNumDimensions();
		this.transform = transform;
		this.transformAsBoundable = transform;
		this.outputImageFactory = img.getImageFactory();

		if ( transform instanceof AffineModel3D ||
			 transform instanceof AffineModel2D ||
			 transform instanceof TranslationModel3D ||
			 transform instanceof TranslationModel2D ||
			 transform instanceof RigidModel2D )
				isAffine = true;
			else
				isAffine = false;

		// get image dimensions
		final int[] dimensions = img.getDimensions();

		//
		// first determine new min-max in all dimensions of the image
		// by transforming all the corner-points
		//
		final float[] min = new float[ numDimensions ];
		final float[] max = new float[ numDimensions ];
		for ( int i = 0; i < numDimensions; ++i )
			max[ i ] = dimensions[ i ] - 1;
		
		transformAsBoundable.estimateBounds( min, max );
		
		offset = new float[ numDimensions ];

		// get the final size for the new image
		newDim = new int[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
		{
			newDim[ d ] = ( int )( max[ d ] - min[ d ] + 1.0f );
			offset[ d ] = min[ d ];
		}
	}

	public void setOutputImageFactory( final ImageFactory<T> outputImageFactory ) { this.outputImageFactory = outputImageFactory; }
	public void setInterpolatorFactory( final InterpolatorFactory<T> interpolatorFactory ) { this.interpolatorFactory = interpolatorFactory; }
	public ImageFactory<T> getOutputImageFactory() { return this.outputImageFactory; }

	public float[] getOffset() { return offset.clone(); }
	public float getOffset( final int dim ) { return offset[ dim ]; }
	public void setOffset( final int dim, final int size ) { offset[ dim ] = size; }
	public void setOffset( final float[] offset )
	{
		for ( int d = 0; d < numDimensions; ++d )
			this.offset[ d ] = offset[ d ];
	}

	public InterpolatorFactory<T> getInterpolatorFactory() { return interpolatorFactory; }
	public int[] getNewImageSize() { return newDim.clone(); }
	public float getNewImageSize( final int dim ) { return newDim[ dim ]; }
	public void setNewImageSize( final int dim, final int size ) { newDim[ dim ] = size; }
	public void setNewImageSize( final int[] newDim )
	{
		for ( int d = 0; d < numDimensions; ++d )
			this.newDim[ d ] = newDim[ d ];
	}

	@Override
	public boolean checkInput()
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( img == null )
		{
			errorMessage = "AffineTransform: [Image<T> img] is null.";
			return false;
		}
		else if ( interpolatorFactory.getOutOfBoundsStrategyFactory() == null )
		{
			errorMessage = "AffineTransform: [OutOfBoundsStrategyFactory<T> of interpolatorFactory] is null.";
			return false;
		}
		else if ( interpolatorFactory == null )
		{
			errorMessage = "AffineTransform: [InterpolatorFactory<T> interpolatorFactory] is null.";
			return false;
		}
		else if ( transform == null )
		{
			errorMessage = "AffineTransform: [Transform3D transform] or [float[] transform] is null.";
			return false;
		}
		else
			return true;
	}

	@Override
	public String getErrorMessage() { return errorMessage; }

	@Override
	public Image<T> getResult() { return transformed; }

	@Override
	public boolean process()
	{
		if ( !checkInput() )
			return false;

		// create the new output image
		transformed = outputImageFactory.createImage( newDim );

		final LocalizableCursor<T> transformedIterator = transformed.createLocalizableCursor();
		final Interpolator<T> interpolator = img.createInterpolator( interpolatorFactory );

		try
		{
			final float[] tmp = new float[ numDimensions ];

			while (transformedIterator.hasNext())
			{
				transformedIterator.fwd();

				// we have to add the offset of our new image
				// relative to it's starting point (0,0,0)
				for ( int d = 0; d < numDimensions; ++d )
					tmp[ d ] = transformedIterator.getPosition( d ) + offset[ d ];

				// transform back into the original image
				//
				// in order to compute the voxels in the new object we have to apply
				// the inverse transform to all voxels of the new array and interpolate
				// the position in the original image
				transform.applyInverseInPlace( tmp );

				interpolator.moveTo( tmp );

				// does the same, but for affine typically slower
				// interpolator.setPosition( tmp );

				transformedIterator.getType().set( interpolator.getType() );
			}
		}
		catch ( NoninvertibleModelException e )
		{
			transformedIterator.close();
			interpolator.close();
			transformed.close();

			errorMessage = "ImageTransform.process(): " + e.getMessage();
			return false;
		}

		transformedIterator.close();
		interpolator.close();
		return true;
	}
}
