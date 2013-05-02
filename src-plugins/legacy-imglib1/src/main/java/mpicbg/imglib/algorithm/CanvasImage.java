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

package mpicbg.imglib.algorithm;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class CanvasImage<T extends Type<T>> implements OutputAlgorithm<T>, Benchmark
{
	final Image<T> input;
	final Image<T> output;
	final OutOfBoundsStrategyFactory<T> outOfBoundsFactory;
	final int numDimensions;
	final int[] newSize, offset, location;
	
	String errorMessage = "";
	int numThreads;
	long processingTime;
	
	/**
	 * Increase or decrease size of the image in all dimensions
	 * 
	 * @param input - the input image
	 * @param newSize - the size of the new image
	 * @param outOfBoundsFactory - what to do when extending the image
	 */
	public CanvasImage( final Image<T> input, final int[] newSize, final int[] offset, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory )
	{
		this.input = input;
		this.outOfBoundsFactory = outOfBoundsFactory;
		this.numDimensions = input.getNumDimensions();
		
		this.newSize = newSize.clone();
		this.location = new int[ numDimensions ];
		this.offset = offset;
		this.processingTime = -1;
		
		if ( newSize == null || newSize.length != numDimensions )
		{
			errorMessage = "newSize is invalid: null or not of same dimensionality as input image";
			this.output = null;
		}
		else if ( offset == null || offset.length != numDimensions )
		{
			errorMessage = "offset is invalid: null or not of same dimensionality as input image";
			this.output = null;			
		}
		else
		{
			for ( int d = 0; d < numDimensions; ++d )
				if ( outOfBoundsFactory == null && offset[ d ] < 0 )
					errorMessage = "no OutOfBoundsStrategyFactory given but image size should increase, that is not possible";

			if ( errorMessage.length() == 0 )
				this.output = input.createNewImage( newSize );
			else
				this.output = null;
		}
	}
	
	public int[] getOffset() { return offset.clone(); }
	
	public CanvasImage( final Image<T> input, final int[] newSize, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory )
	{		
		this( input, newSize, computeOffset(input, newSize), outOfBoundsFactory ); 
	}
	
	private static int[] computeOffset( final Image<?> input, final int[] newSize )
	{
		final int offset[] = new int[ input.getNumDimensions() ];
		
		for ( int d = 0; d < input.getNumDimensions(); ++d )
			offset[ d ] = ( input.getDimension( d ) - newSize[ d ] ) / 2;
		
		return offset;
	}
	
	
	/**
	 * This constructor can be called if the image is only cropped, then there is no {@link OutOfBoundsStrategyFactory} necessary.
	 * It will fail if the image size is increased.
	 *   
	 * @param input - the input image
	 * @param newSize - the size of the new image
	 */
	public CanvasImage( final Image<T> input, final int[] newSize )
	{
		this( input, newSize, null );
	}
	
	@Override
	public boolean process() 
	{
		final long startTime = System.currentTimeMillis();

		final LocalizableCursor<T> outputCursor = output.createLocalizableCursor();
		final LocalizableByDimCursor<T> inputCursor;
		
		if ( outOfBoundsFactory == null)
			inputCursor = input.createLocalizableByDimCursor( );
		else
			inputCursor = input.createLocalizableByDimCursor( outOfBoundsFactory );

		while ( outputCursor.hasNext() )
		{
			outputCursor.fwd();
			outputCursor.getPosition( location );
			
			for ( int d = 0; d < numDimensions; ++d )
				location[ d ] += offset[ d ];
			
			inputCursor.moveTo( location );
			outputCursor.getType().set( inputCursor.getType() );
		}

		outputCursor.close();
		inputCursor.close();

        processingTime = System.currentTimeMillis() - startTime;

        return true;		
	}
	
	@Override
	public long getProcessingTime() { return processingTime; }
	
	@Override
	public Image<T> getResult() { return output; }

	@Override
	public boolean checkInput() 
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( input == null )
		{
			errorMessage = "Input image is null";
			return false;
		}
		else if ( output == null )
		{
			errorMessage = "Output image is null, maybe not enough memory";
			return false;
		}
		else
		{
			return true;
		}
	}

	@Override
	public String getErrorMessage() 
	{
		if ( errorMessage.length() > 0 )
			errorMessage =  "CanvasImage(): " + errorMessage;
			
		return errorMessage;
	}
	
}
