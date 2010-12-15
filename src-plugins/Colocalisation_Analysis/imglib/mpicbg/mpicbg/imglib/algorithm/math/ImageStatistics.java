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
package mpicbg.imglib.algorithm.math;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.util.RealSum;
import mpicbg.imglib.type.Type;

/**
 * This class contains some basic {@link Image} statistics
 * calculations.
 */
public class ImageStatistics {
	/**
	 * Calculates the mean of an image.
	 *
	 * @param img The image to calculate the mean of
	 * @return The mean of the image passed
	 */
	final public static <T extends RealType<T>> double getImageMean( final Image<T> img )
	{
		return getImageIntegral(img) / img.getNumPixels();
	}

	/**
	 * Calculates the integral of the pixel values of an image.
	 *
	 * @param img The image to calculate the integral of
	 * @return The pixel values integral of the image passed
	 */
	final public static <T extends RealType<T>> double getImageIntegral( final Image<T> img )
	{
		final RealSum sum = new RealSum();

		for ( final T type : img )
			sum.add( type.getRealDouble() );

		return sum.getSum();
	}

	/**
	 * Calculates the min of an image.
	 *
	 * @param img The image to calculate the min of
	 * @return The min of the image passed
	 */
	final public static <T extends Type<T> & Comparable<T>> T getImageMin( final Image<T> img )
	{
		final Cursor<T> cursor = img.createCursor();
		cursor.fwd();

		final T min = img.createType();
		min.set( cursor.getType() );

		while ( cursor.hasNext() )
		{
			cursor.fwd();

			final T currValue = cursor.getType();

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
	final public static <T extends Type<T> & Comparable<T>> T getImageMax( final Image<T> img ) {

		final Cursor<T> cursor = img.createCursor();
		cursor.fwd();

		final T max = img.createType();
		max.set( cursor.getType() );

		while ( cursor.hasNext() )
		{
			cursor.fwd();

			final T currValue = cursor.getType();

			if ( currValue.compareTo( max ) > 0 )
				max.set( currValue );
		}

        return max;
	}
}
