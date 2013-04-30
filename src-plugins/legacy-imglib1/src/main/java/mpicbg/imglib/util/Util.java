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

package mpicbg.imglib.util;

import java.util.List;

import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.ExponentialMathType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class Util
{
	public static double log2( final double value )
	{
		return Math.log( value ) / Math.log( 2.0 );
	}

	public static double[] getArrayFromValue( final double value, final int numDimensions )
	{
		final double[] values = new double[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
			values[ d ] = value;

		return values;
	}

	public static float[] getArrayFromValue( final float value, final int numDimensions )
	{
		final float[] values = new float[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
			values[ d ] = value;

		return values;
	}

	public static int[] getArrayFromValue( final int value, final int numDimensions )
	{
		final int[] values = new int[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
			values[ d ] = value;

		return values;
	}

	final public static float computeDistance( final int[] position1, final int[] position2 )
	{
		float dist = 0;

		for ( int d = 0; d < position1.length; ++d )
		{
			final int pos = position2[ d ] - position1[ d ];

			dist += pos*pos;
		}

		return (float)Math.sqrt( dist );
	}

	final public static float computeLength( final int[] position )
	{
		float dist = 0;

		for ( int d = 0; d < position.length; ++d )
		{
			final int pos = position[ d ];

			dist += pos*pos;
		}

		return (float)Math.sqrt( dist );
	}

	public static long computeMedian( final long[] values )
	{
		final long temp[] = values.clone();
		long median;

		final int length = temp.length;

		quicksort( temp, 0, length - 1 );

		if (length % 2 == 1) //odd length
			median = temp[length / 2];
		else //even length
			median = (temp[length / 2] + temp[(length / 2) - 1]) / 2;

		return median;
	}

	public static double computeMedian( final double[] values )
	{
		final double temp[] = values.clone();
		double median;

		final int length = temp.length;

		quicksort( temp, 0, length - 1 );

		if (length % 2 == 1) //odd length
			median = temp[length / 2];
		else //even length
			median = (temp[length / 2] + temp[(length / 2) - 1]) / 2;

		return median;
	}

	/**
	 * Computes the percentile of a collection of doubles (percentile 0.5 roughly corresponds to median)
	 * @param values - the values
	 * @param percentile - the percentile [0...1]
	 * @return the corresponding value
	 */
	public static double computePercentile( final double[] values, final double percentile )
	{
		final double temp[] = values.clone();
		final int length = temp.length;
		
		quicksort( temp );
		
		return temp[ Math.min( length - 1, Math.max(0 ,(int)Math.round( (length - 1) * percentile ) ) ) ];
	}

	/**
	 * Computes the percentile of a collection of float (percentile 0.5 roughly corresponds to median)
	 * @param values - the values
	 * @param percentile - the percentile [0...1]
	 * @return the corresponding value
	 */
	public static float computePercentile( final float[] values, final float percentile )
	{
		final float temp[] = values.clone();
		final int length = temp.length;
		
		quicksort( temp );
		
		return temp[ Math.min( length - 1, Math.max(0 ,(int)Math.round( (length - 1) * percentile ) ) ) ];
	}

	public static double computeAverageDouble( final List<Double> values )
	{
		final double size = values.size();
		double avg = 0;

		for ( final double v : values )
			avg += v / size;

		return avg;
	}

	public static float computeAverageFloat( final List<Float> values )
	{
		final double size = values.size();
		double avg = 0;

		for ( final double v : values )
			avg += v / size;

		return (float)avg;
	}

	public static float computeMinimum( final List<Float> values )
	{
		float min = Float.MAX_VALUE;

		for ( final float v : values )
			if ( v < min )
				min = v;

		return min;
	}

	public static float computeMaximum( final List<Float> values )
	{
		float max = -Float.MAX_VALUE;

		for ( final float v : values )
			if ( v > max )
				max = v;

		return max;
	}

	public static float computeAverage( final float[] values )
	{
		final double size = values.length;
		double avg = 0;

		for ( final float v : values )
			avg += v / size;

		return (float)avg;
	}

	public static double computeAverage( final double[] values )
	{
		final double size = values.length;
		double avg = 0;

		for ( final double v : values )
			avg += v / size;

		return avg;
	}

	public static double computeMin( final double[] values )
	{
		double min = values[ 0 ];

		for ( final double v : values )
			if ( v < min )
				min = v;

		return min;
	}

	public static double computeMax( final double[] values )
	{
		double max = values[ 0 ];

		for ( final double v : values )
			if ( v > max )
				max = v;

		return max;
	}
	
	public static float computeMedian( final float[] values )
	{
		final float temp[] = values.clone();
		float median;

		final int length = temp.length;

		quicksort( temp, 0, length - 1 );

		if (length % 2 == 1) //odd length
			median = temp[length / 2];
		else //even length
			median = (temp[length / 2] + temp[(length / 2) - 1]) / 2;

		return median;
	}

	public static void quicksort( final long[] data, final int left, final int right )
	{
		if (data == null || data.length < 2)return;
		int i = left, j = right;
		long x = data[(left + right) / 2];
		do
		{
			while (data[i] < x) i++;
			while (x < data[j]) j--;
			if (i <= j)
			{
				long temp = data[i];
				data[i] = data[j];
				data[j] = temp;
				i++;
				j--;
			}
		}
		while (i <= j);
		if (left < j) quicksort(data, left, j);
		if (i < right) quicksort(data, i, right);
	}

	public static void quicksort( final double[] data ) { quicksort( data, 0, data.length - 1 ); }

	public static void quicksort( final double[] data, final int left, final int right )
	{
		if (data == null || data.length < 2)return;
		int i = left, j = right;
		double x = data[(left + right) / 2];
		do
		{
			while (data[i] < x) i++;
			while (x < data[j]) j--;
			if (i <= j)
			{
				double temp = data[i];
				data[i] = data[j];
				data[j] = temp;
				i++;
				j--;
			}
		}
		while (i <= j);
		if (left < j) quicksort(data, left, j);
		if (i < right) quicksort(data, i, right);
	}

	public static void quicksort( final float[] data ) { quicksort( data, 0, data.length - 1 ); }

	public static void quicksort( final float[] data, final int left, final int right )
	{
		if (data == null || data.length < 2)return;
		int i = left, j = right;
		float x = data[(left + right) / 2];
		do
		{
			while (data[i] < x) i++;
			while (x < data[j]) j--;
			if (i <= j)
			{
				float temp = data[i];
				data[i] = data[j];
				data[j] = temp;
				i++;
				j--;
			}
		}
		while (i <= j);
		if (left < j) quicksort(data, left, j);
		if (i < right) quicksort(data, i, right);
	}

	public static void quicksort( final double[] data, final int[] sortAlso, final int left, final int right )
	{
		if (data == null || data.length < 2)return;
		int i = left, j = right;
		double x = data[(left + right) / 2];
		do
		{
			while (data[i] < x) i++;
			while (x < data[j]) j--;
			if (i <= j)
			{
				double temp = data[i];
				data[i] = data[j];
				data[j] = temp;

				int temp2 = sortAlso[i];
				sortAlso[i] = sortAlso[j];
				sortAlso[j] = temp2;

				i++;
				j--;
			}
		}
		while (i <= j);
		if (left < j) quicksort(data, sortAlso, left, j);
		if (i < right) quicksort(data, sortAlso, i, right);
	}

	public static double gLog( final double z, final double c )
	{
		if (c == 0)
			return z;
		else
			return Math.log10((z + Math.sqrt(z * z + c * c)) / 2.0);
	}

	public static float gLog( final float z, final float c )
	{
		if (c == 0)
			return z;
		else
			return (float)Math.log10((z + Math.sqrt(z * z + c * c)) / 2.0);
	}

	public static double gLogInv( final double w, final double c )
	{
		if (c == 0)
			return w;
		else
			return Math.pow(10, w) - (((c * c) * Math.pow(10,-w)) / 4.0);
	}

	public static double gLogInv( final float w, final float c )
	{
		if (c == 0)
			return w;
		else
			return Math.pow(10, w) - (((c * c) * Math.pow(10,-w)) / 4.0);
	}

	public static boolean isApproxEqual( final float a, final float b, final float threshold )
	{
		if (a==b)
		  return true;
		else if (a + threshold > b && a - threshold < b)
		  return true;
		else
		  return false;
	}

	public static boolean isApproxEqual( final double a, final double b, final double threshold )
	{
		if (a==b)
		  return true;
		else if (a + threshold > b && a - threshold < b)
		  return true;
		else
		  return false;
	}

	public static int round( final float value )
	{
		return (int)( value + (0.5f * Math.signum( value ) ) );
	}

	public static long round( final double value )
	{
		return (long)( value + (0.5d * Math.signum( value ) ) );
	}

    /**
     * This method creates a gaussian kernel
     *
     * @param sigma Standard Derivation of the gaussian function
     * @param normalize Normalize integral of gaussian function to 1 or not...
     * @return double[] The gaussian kernel
     *
     */
    public static double[] createGaussianKernel1DDouble( final double sigma, final boolean normalize, final int precision )
    {
            int size = 3;
            final double[] gaussianKernel;

            if (sigma <= 0)
            {
                    gaussianKernel = new double[3];
                    gaussianKernel[1] = 1;
            }
            else
            {
                    size = Math.max(3, (2 * (int) (precision * sigma + 0.5) + 1));

                    final double two_sq_sigma = 2 * sigma * sigma;
                    gaussianKernel = new double[size];

                    for (int x = size / 2; x >= 0; --x)
                    {
                            final double val = Math.exp( -(x * x) / two_sq_sigma);

                            gaussianKernel[size / 2 - x] = val;
                            gaussianKernel[size / 2 + x] = val;
                    }
            }

            if (normalize)
            {
                    double sum = 0;
                    for (double value : gaussianKernel)
                            sum += value;

                    for (int i = 0; i < gaussianKernel.length; ++i)
                            gaussianKernel[i] /= sum;
            }

            return gaussianKernel;
    }

    /**
     * This method creates a gaussian kernel
     *
     * @param sigma Standard Derivation of the gaussian function
     * @param normalize Normalize integral of gaussian function to 1 or not...
     * @return double[] The gaussian kernel
     *
     */
    public static double[] createGaussianKernel1DDouble( final double sigma, final boolean normalize )
    {
            int size = 3;
            final double[] gaussianKernel;

            if (sigma <= 0)
            {
                    gaussianKernel = new double[3];
                    gaussianKernel[1] = 1;
            }
            else
            {
                    size = Math.max(3, (2 * (int) (3 * sigma + 0.5) + 1));

                    final double two_sq_sigma = 2 * sigma * sigma;
                    gaussianKernel = new double[size];

                    for (int x = size / 2; x >= 0; --x)
                    {
                            final double val = Math.exp( -(x * x) / two_sq_sigma);

                            gaussianKernel[size / 2 - x] = val;
                            gaussianKernel[size / 2 + x] = val;
                    }
            }

            if (normalize)
            {
                    double sum = 0;
                    for (double value : gaussianKernel)
                            sum += value;

                    for (int i = 0; i < gaussianKernel.length; ++i)
                            gaussianKernel[i] /= sum;
            }

            return gaussianKernel;
    }

    /**
     * This method creates a gaussian kernel
     *
     * @param sigma Standard Derivation of the gaussian function in the desired {@link Type}
     * @param normalize Normalize integral of gaussian function to 1 or not...
     * @return T[] The gaussian kernel
     *
     */
    public static < T extends ExponentialMathType<T> > T[] createGaussianKernel1D( final T sigma, final boolean normalize )
    {
            final T[] gaussianKernel;
            int kernelSize;

            final T zero = sigma.createVariable();
            final T two = sigma.createVariable();
            final T one = sigma.createVariable();
            final T minusOne = sigma.createVariable();
            final T two_sq_sigma = zero.createVariable();
            final T sum = sigma.createVariable();
            final T value = sigma.createVariable();
            final T xPos = sigma.createVariable();
            final T cs = sigma.createVariable();

            zero.setZero();
            one.setOne();

            two.setOne();
            two.add( one );

            minusOne.setZero();
            minusOne.sub( one );

            if ( sigma.compareTo( zero ) <= 0 )
            {
            		kernelSize = 3;
                    gaussianKernel = zero.createArray1D( 3 );
                    gaussianKernel[ 1 ].set( one );
            }
            else
            {
                	//size = Math.max(3, (int) (2 * (int) (3 * sigma + 0.5) + 1));
                	cs.set( sigma );
                	cs.mul( 3.0 );
                	cs.round();
                	cs.mul( 2.0 );
                	cs.add( one );

                	kernelSize = Util.round( cs.getRealFloat() );

                	// kernelsize has to be at least 3
            		kernelSize = Math.max( 3, kernelSize );

            		// kernelsize has to be odd
            		if ( kernelSize % 2 == 0 )
            			++kernelSize;

                    // two_sq_sigma = 2 * sigma * sigma;
                    two_sq_sigma.set( two );
                    two_sq_sigma.mul( sigma );
                    two_sq_sigma.mul( sigma );

                    gaussianKernel = zero.createArray1D( kernelSize );

                    for ( int i = 0; i < gaussianKernel.length; ++i )
                    	gaussianKernel[ i ] = zero.createVariable();

                    // set the xPos to kernelSize/2
                    xPos.setZero();
                    for ( int x = 1; x <= kernelSize / 2; ++x )
                    	xPos.add( one );

                    for ( int x = kernelSize / 2; x >= 0; --x )
                    {
                        //final double val = Math.exp( -(x * x) / two_sq_sigma );
                    	value.set( xPos );
                    	value.mul( xPos );
                    	value.mul( minusOne );
                    	value.div( two_sq_sigma );
                    	value.exp();

                        gaussianKernel[ kernelSize / 2 - x ].set( value );
                        gaussianKernel[ kernelSize / 2 + x ].set( value );

                        xPos.sub( one );
                    }
            }

            if (normalize)
            {
                    sum.setZero();

                    for ( final T val : gaussianKernel )
                            sum.add(  val );

                    for (int i = 0; i < gaussianKernel.length; ++i)
                            gaussianKernel[ i ].div( sum );
            }

            for ( int i = 0; i < gaussianKernel.length; ++i )
            	System.out.println( gaussianKernel[ i ] );

            return gaussianKernel;
    }

    public static int getSuggestedKernelDiameter( final double sigma )
    {
        int size = 3;

        if ( sigma > 0 )
            size = Math.max(3, (2 * (int) (3 * sigma + 0.5) + 1));

        return size;
    }

	public static String printCoordinates( final float[] value )
	{
		String out = "(Array empty)";

		if ( value == null || value.length == 0 )
			return out;
		else
			out = "(" + value[0];

		for ( int i = 1; i < value.length; i++ )
			out += ", " + value[ i ];

		out += ")";

		return out;
	}

	public static String printCoordinates( final int[] value )
	{
		String out = "(Array empty)";

		if ( value == null || value.length == 0 )
			return out;
		else
			out = "(" + value[0];

		for ( int i = 1; i < value.length; i++ )
			out += ", " + value[ i ];

		out += ")";

		return out;
	}

	public static String printCoordinates( final boolean[] value )
	{
		String out = "(Array empty)";

		if ( value == null || value.length == 0 )
			return out;
		else
			out = "(";

		if ( value[ 0 ] )
			out += "1";
		else
			out += "0";

		for ( int i = 1; i < value.length; i++ )
		{
			out += ", ";
			if ( value[ i ] )
				out += "1";
			else
				out += "0";
		}

		out += ")";

		return out;
	}

	public static int pow( final int a, final int b )
	{
		if (b == 0)
			return 1;
		else if (b == 1)
			return a;
		else
		{
			int result = a;

			for (int i = 1; i < b; i++)
				result *= a;

			return result;
		}
	}

	public static <T extends Type<T> & Comparable<T>> T max( final T value1, final T value2 )
	{
		if( value1.compareTo( value2 ) >= 0 )
			return value1;
		else
			return value2;
	}

	public static <T extends Type<T> & Comparable<T>> T min( final T value1, final T value2 )
	{
		if( value1.compareTo( value2 ) <= 0 )
			return value1;
		else
			return value2;
	}

	public static boolean[][] getRecursiveCoordinates( final int numDimensions )
	{
		boolean[][] positions = new boolean[ Util.pow( 2, numDimensions ) ][ numDimensions ];

		setCoordinateRecursive( numDimensions - 1, numDimensions, new int[ numDimensions ], positions );

		return positions;
	}

	/**
	 * recursively get coordinates covering all binary combinations for the given dimensionality
	 *
	 * example for 3d:
	 *
	 * x y z index
	 * 0 0 0 [0]
	 * 1 0 0 [1]
	 * 0 1 0 [2]
	 * 1 1 0 [3]
	 * 0 0 1 [4]
	 * 1 0 1 [5]
	 * 0 1 1 [6]
	 * 1 1 1 [7]
	 *
	 * All typical call will look like that:
	 *
	 * boolean[][] positions = new boolean[ MathLib.pow( 2, numDimensions ) ][ numDimensions ];
	 * MathLib.setCoordinateRecursive( numDimensions - 1, numDimensions, new int[ numDimensions ], positions );
	 *
	 * @param dimension - recusively changed current dimension, init with numDimensions - 1
	 * @param numDimensions - the number of dimensions
	 * @param location - recursively changed current state, init with new int[ numDimensions ]
	 * @param result - where the result will be stored when finished, needes a boolean[ MathLib.pow( 2, numDimensions ) ][ numDimensions ]
	 */
	public static void setCoordinateRecursive( final int dimension, final int numDimensions, final int[] location, final boolean[][] result )
	{
		final int[] newLocation0 = new int[ numDimensions ];
		final int[] newLocation1 = new int[ numDimensions ];

		for ( int d = 0; d < numDimensions; d++ )
		{
			newLocation0[ d ] = location[ d ];
			newLocation1[ d ] = location[ d ];
		}

		newLocation0[ dimension ] = 0;
		newLocation1[ dimension ] = 1;

		if ( dimension == 0 )
		{
			// compute the index in the result array ( binary to decimal conversion )
			int index0 = 0, index1 = 0;

			for ( int d = 0; d < numDimensions; d++ )
			{
				index0 += newLocation0[ d ] * pow( 2, d );
				index1 += newLocation1[ d ] * pow( 2, d );
			}

			// fill the result array
			for ( int d = 0; d < numDimensions; d++ )
			{
				result[ index0 ][ d ] = (newLocation0[ d ] == 1);
				result[ index1 ][ d ] = (newLocation1[ d ] == 1);
			}
		}
		else
		{
			setCoordinateRecursive( dimension - 1, numDimensions, newLocation0, result );
			setCoordinateRecursive( dimension - 1, numDimensions, newLocation1, result );
		}

	}

}
