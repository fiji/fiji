/**
 * Copyright (c) 2009--2010, Stephan Preibisch & Stephan Saalfeld
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.  Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution.  Neither the name of the Fiji project nor
 * the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Dan White & Tom Kazimiers
 */
package net.imglib2.algorithm.math;

import net.imglib2.Cursor;
import net.imglib2.TwinCursor;
import net.imglib2.img.Img;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.RealSum;

/**
 * This class contains some basic {@link Img} statistics
 * calculations.
 */
public class ImageStatistics {
	/**
	 * Calculates the number of pixels in the image.
	 *
	 * @param img The image to calculate the mean of
	 * @param mask The mask to respect
	 * @return The mean of the image passed
	 */
	final public static <T extends RealType<T>> long getNumPixels( final Img<T> img )
	{
		long numPixels = 1;
		for (int d=0; d<img.numDimensions(); ++d)
			numPixels = numPixels * img.dimension(d);
		
		return numPixels;
	} 

	/**
	 * Calculates the mean of an image with respect to a mask.
	 *
	 * @param img The image to calculate the mean of
	 * @param mask The mask to respect
	 * @return The mean of the image passed
	 */
	final public static <T extends RealType<T>> double getImageMean( final Img<T> img, final Img<BitType>  mask )
	{
		final RealSum sum = new RealSum();
		long numPixels = 0;
		// create cursor to walk an image with respect to a mask
		final TwinCursor<T> cursor = new TwinCursor<T>(
				img.randomAccess(),
				img.randomAccess(),
				mask.cursor());
		while (cursor.hasNext()) {
			sum.add(cursor.getChannel1().getRealDouble());
			++numPixels;
		}

		return sum.getSum() / numPixels;
	}

	/**
	 * Calculates the mean of an image.
	 *
	 * @param img The image to calculate the mean of
	 * @return The mean of the image passed
	 */
	final public static <T extends RealType<T>> double getImageMean( final Img<T> img )
	{
		// Count all values using the RealSum class.
        // It prevents numerical instabilities when adding up millions of pixels
        RealSum realSum = new RealSum();
        long count = 0;
 
        for ( final T type : img )
        {
            realSum.add( type.getRealDouble() );
            ++count;
        }

        return realSum.getSum() / count;
	}

	/**
	 * Calculates the integral of the pixel values of an image.
	 *
	 * @param img The image to calculate the integral of
	 * @return The pixel values integral of the image passed
	 */
	final public static <T extends RealType<T>> double getImageIntegral( final Img<T> img )
	{
		final RealSum sum = new RealSum();

		for ( final T type : img )
			sum.add( type.getRealDouble() );

		return sum.getSum();
	}

	/**
	 * Calculates the integral of the pixel values of an image.
	 *
	 * @param img The image to calculate the integral of
	 * @return The pixel values integral of the image passed
	 */
	final public static <T extends RealType<T>> double getImageIntegral( final Img<T> img, Img<BitType> mask )
	{
		final RealSum sum = new RealSum();
		// create cursor to walk an image with respect to a mask
		final TwinCursor<T> cursor = new TwinCursor<T>(
				img.randomAccess(),
				img.randomAccess(),
				mask.cursor());
		while (cursor.hasNext())
			sum.add( cursor.getChannel1().getRealDouble() );

		return sum.getSum();
	}

	/**
	 * Calculates the min of an image.
	 *
	 * @param img The image to calculate the min of
	 * @return The min of the image passed
	 */
	final public static <T extends Type<T> & Comparable<T>> T getImageMin( final Img<T> img )
	{
		final Cursor<T> cursor = img.cursor();
		cursor.fwd();
		// copy first element as current maximum
		final T min = cursor.get().copy();

		while ( cursor.hasNext() )
		{
			cursor.fwd();

			final T currValue = cursor.get();

			if ( currValue.compareTo( min ) < 0 )
				min.set( currValue );
		}

        return min;
	 }

	/**
	 * Calculates the min of an image with respect to a mask.
	 *
	 * @param img The image to calculate the min of
	 * @param mask The mask to respect
	 * @return The min of the image passed
	 */
	final public static <T extends Type<T> & Comparable<T>> T getImageMin( final Img<T> img, final Img<BitType> mask )
	{
		// create cursor to walk an image with respect to a mask
		final TwinCursor<T> cursor = new TwinCursor<T>(
				img.randomAccess(),
				img.randomAccess(),
				mask.cursor());
		// forward one step to get the first value
		cursor.fwd();
		// copy first element as current minimum
		final T min = cursor.getChannel1().copy();

		while ( cursor.hasNext() ) {
			cursor.fwd();

			final T currValue = cursor.getChannel1();

			if ( currValue.compareTo( min ) < 0 )
				min.set( currValue );
		}

        return min;
	 }

	/**
	 * Calculates the max of an image.
	 *
	 * @param img The image to calculate the max of
	 * @return The max of the image passed
	 */
	final public static <T extends Type<T> & Comparable<T>> T getImageMax( final Img<T> img ) {

		final Cursor<T> cursor = img.cursor();
		cursor.fwd();
		// copy first element as current maximum
		final T max = cursor.get().copy();

		while ( cursor.hasNext() )
		{
			cursor.fwd();

			final T currValue = cursor.get();

			if ( currValue.compareTo( max ) > 0 )
				max.set( currValue );
		}

        return max;
	}
	/**
	 * Calculates the max of an image with respect to a mask.
	 *
	 * @param img The image to calculate the min of
	 * @param mask The mask to respect
	 * @return The min of the image passed
	 */
	final public static <T extends Type<T> & Comparable<T>> T getImageMax( final Img<T> img, final Img<BitType> mask )
	{
		// create cursor to walk an image with respect to a mask
		final TwinCursor<T> cursor = new TwinCursor<T>(
				img.randomAccess(),
				img.randomAccess(),
				mask.cursor());
		// forward one step to get the first value
		cursor.fwd();
		final T max = cursor.getChannel1().copy();

		while ( cursor.hasNext() ) {
			cursor.fwd();

			final T currValue = cursor.getChannel1();

			if ( currValue.compareTo( max ) > 0 )
				max.set( currValue );
		}

        return max;
	 }
}
