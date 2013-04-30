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

package mpicbg.imglib.interpolation.linear;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.InterpolatorImpl;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.NumericType;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class LinearInterpolator<T extends NumericType<T>> extends InterpolatorImpl<T>
{
	final LocalizableByDimCursor<T> cursor;
	final T tmp1, tmp2;
	
	// the offset in each dimension and a temporary array for computing the global coordinates
	final int[] baseDim, location;
	
	// the weights and inverse weights in each dimension
	final float[][] weights;
	
	// to save the temporary values in each dimension when computing the final value
	// the value in [ 0 ][ 0 ] will be the interpolated value
	final T[][] tree;
	
	// the half size of the second array in each tree step - speedup
	final int[] halfTreeLevelSizes;
		
	// the locations where to initially grab pixels from
	final boolean[][] positions;
	
	protected LinearInterpolator( final Image<T> img, final InterpolatorFactory<T> interpolatorFactory, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory )
	{
		this( img, interpolatorFactory, outOfBoundsStrategyFactory, true );
	}
	
	protected LinearInterpolator( final Image<T> img, final InterpolatorFactory<T> interpolatorFactory, final OutOfBoundsStrategyFactory<T> outOfBoundsStrategyFactory, boolean initGenericStructures )
	{
		super(img, interpolatorFactory, outOfBoundsStrategyFactory);

		// Principle of interpolation used
		//
		// example: 3d
		//
		// STEP 1 - Interpolate in dimension 0 (x)
		//
		//   ^
		// y |
		//   |        _
		//   |        /|     [6]     [7]
		//   |     z /        *<----->*       
  		//   |      /        /       /|
		//   |     /    [2] /   [3] / |    
		//   |    /        *<----->*  * [5]
		//   |   /         |       | /
		//   |  /          |       |/
		//   | /           *<----->*
		//   |/           [0]     [1]
		//   *--------------------------> 
		//                             x
		//
		// STEP 2 - Interpolate in dimension 1 (y)
		//
		//   [2][3]   [6][7]
		//      *-------*
		//      |       |
		//      |       |       
		//      |       |
		//      *-------*
		//   [0][1]   [4][5]
		//
		//     [2]     [3]
		//      *<----->*
		//      |       |
		//      |       |       
		//      |       |
		//      *<----->*
		//     [0]     [1]
		//
		// STEP 3 - Interpolate in dimension 1 (z)
		//
		//   [2][3]  
		//      *    
		//      |     
		//      |      
		//      |    
		//      *    
		//   [0][1]  
		//
		//     [0]     [1]
		//      *<----->*
		//
		// yiels the interpolated value in 3 dimensions
		
		cursor = img.createLocalizableByDimCursor( outOfBoundsStrategyFactory );
		tmp1 = img.createType();
		tmp2 = img.createType();

		baseDim = new int[ numDimensions ];
		location = new int[ numDimensions ];
		weights = new float[ numDimensions ][ 2 ];

		if ( initGenericStructures )
		{		
			// create the temporary datastructure for computing the actual interpolation
			//
			// example: 3d-image
			//
			// 3d: get values from image and interpolate in dimension 0
			//     see above and below which coordinates are [0]...[7]
			//
			//              [0] [1] [2] [3] [4] [5] [6] [7]
			// interp in 3d  |   |   |   |   |   |   |   |
			// store in 2d:  \   /   \   /   \   /   \   /
			//                [0]     [1]     [2]     [3] 
			// interp in 2d    \       /       \       /
			// and store in     \     /         \     /
			// 1d                \   /           \   /
			//                    [0]             [1]
			// interpolate in 1d   \               /       
			// and store            \             / 
			// the final             \           /
			// result                 \         /
			//                         \       / 
			//                          \     / 
			//                           \   /
			//  final interpolated value  [0]
	
			tree = tmp1.createArray2D( numDimensions + 1, 1 );
			halfTreeLevelSizes = new int[ numDimensions + 1 ];
			
			for ( int d = 0; d < tree.length; d++ )
			{
				tree[ d ] = tmp1.createArray1D( Util.pow( 2, d ));
				
				for ( int i = 0; i < tree[ d ].length; i++ )
					tree[ d ][ i ] = img.createType();
	
				halfTreeLevelSizes[ d ] = tree[ d ].length / 2;
			}
						
			// recursively get the coordinates we need for interpolation
			// ( relative location to the offset in each dimension )
			//
			// example for 3d:
			//
			//  x y z index
			//  0 0 0 [0]
			//  1 0 0 [1] 
			//  0 1 0 [2]
			//  1 1 0 [3] 
			// 	0 0 1 [4] 
			// 	1 0 1 [5] 
			// 	0 1 1 [6] 
			// 	1 1 1 [7] 
			
			positions = new boolean[ Util.pow( 2, numDimensions ) ][ numDimensions ];
			Util.setCoordinateRecursive( numDimensions - 1, numDimensions, new int[ numDimensions ], positions );
						
			moveTo( position );
		}
		else
		{
			tree = null;
			positions = null;
			halfTreeLevelSizes = null;
		}
	}
	
	@Override
	public void close() { cursor.close(); }

	@Override
	public T getType() { return tree[ 0 ][ 0 ]; }

	@Override
	public void moveTo( final float[] position )
	{
        // compute the offset (Math.floor) in each dimension
		for (int d = 0; d < numDimensions; d++)
		{
			this.position[ d ] = position[ d ];
			
			baseDim[ d ] = position[ d ] > 0 ? (int)position[ d ]: (int)position[ d ]-1;			
			cursor.move( baseDim[ d ] - cursor.getPosition(d), d );
		}

        // compute the weights [0...1] in each dimension and the inverse (1-weight) [1...0]
		for (int d = 0; d < numDimensions; d++)
		{
			final float w = position[ d ] - baseDim[ d ];
			
			weights[ d ][ 1 ] = w;
			weights[ d ][ 0 ] = 1 - w;
		}
		
		//
		// compute the output value
		//
		
		// the the values from the image
		for ( int i = 0; i < positions.length; ++i )
		{
			// move to the position
			for ( int d = 0; d < numDimensions; ++d )
				if ( positions[ i ][ d ] )
					cursor.fwd(d);

			tree[ numDimensions ][ i ].set( cursor.getType() );
			
			// move back to the offset position
			for ( int d = 0; d < numDimensions; ++d )
				if ( positions[ i ][ d ] )
					cursor.bck(d);
		}
		
		// interpolate down the tree as shown above
		for ( int d = numDimensions; d > 0; --d )
		{
			for ( int i = 0; i < halfTreeLevelSizes[ d ]; i++ )
			{
				tmp1.set( tree[ d ][ i*2 ] );
				tmp2.set( tree[ d ][ i*2+1 ] );
				
				//tmp1.mul( weights[d - 1][ 0 ] );
				//tmp2.mul( weights[d - 1][ 1 ] );
				tmp1.mul( weights[ numDimensions - d ][ 0 ] );
				tmp2.mul( weights[ numDimensions - d ][ 1 ] );
				
				tmp1.add( tmp2 );
				
				tree[ d - 1 ][ i ].set( tmp1 );
			}
		}
	}
	
	@Override
	public void setPosition( final float[] position )
	{
        // compute the offset (Math.floor) in each dimension
		for (int d = 0; d < numDimensions; d++)
		{
			this.position[ d ] = position[ d ];

			baseDim[ d ] = position[ d ] > 0 ? (int)position[ d ]: (int)position[ d ]-1;
		}
			
	    cursor.setPosition( baseDim );
		
        // compute the weights [0...1] in each dimension and the inverse (1-weight) [1...0]
		for (int d = 0; d < numDimensions; d++)
		{
			final float w = position[ d ] - baseDim[ d ];
			
			weights[ d ][ 1 ] = w;
			weights[ d ][ 0 ] = 1 - w;
		}
		
		//
		// compute the output value
		//
		
		// the the values from the image
		for ( int i = 0; i < positions.length; ++i )
		{
			// move to the position
			for ( int d = 0; d < numDimensions; ++d )
				if ( positions[ i ][ d ] )
					cursor.fwd(d);

			tree[ numDimensions ][ i ].set( cursor.getType() );
			
			// move back to the offset position
			for ( int d = 0; d < numDimensions; ++d )
				if ( positions[ i ][ d ] )
					cursor.bck(d);
		}
		
		// interpolate down the tree as shown above
		for ( int d = numDimensions; d > 0; --d )
		{
			for ( int i = 0; i < halfTreeLevelSizes[ d ]; i++ )
			{
				tmp1.set( tree[ d ][ i*2 ] );
				tmp2.set( tree[ d ][ i*2+1 ] );
				
				tmp1.mul( weights[ numDimensions - d ][ 0 ] );
				tmp2.mul( weights[ numDimensions - d ][ 1 ] );
				
				tmp1.add( tmp2 );
				
				tree[ d - 1 ][ i ].set( tmp1 );
			}
		}
	}
}
