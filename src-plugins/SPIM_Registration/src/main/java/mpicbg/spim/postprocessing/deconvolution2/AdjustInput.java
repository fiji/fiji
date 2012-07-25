package mpicbg.spim.postprocessing.deconvolution2;

import ij.IJ;

import java.util.ArrayList;
import java.util.Random;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.util.RealSum;

public class AdjustInput 
{	
	public static Random rnd = new Random( 14235235 );

	/**
	 * Norms an image so that the sum over all pixels is 1.
	 * 
	 * @param img - the {@link Image} to normalize
	 */
	final public static void normImage( final Image<FloatType> img )
	{
		final double sum = sumImage( img );	

		for ( final FloatType t : img )
			t.set( (float) ((double)t.get() / sum) );
	}
	
	/**
	 * @param img - the input {@link Image}
	 * @return - the sum of all pixels using {@link RealSum}
	 */
	final public static double sumImage( final Image<FloatType> img )
	{
		final RealSum sum = new RealSum();		

		for ( final FloatType t : img )
			sum.add( t.get() );

		return sum.getSum();
	}	

	public static double normAllImages( final ArrayList<LRFFT> data )
	{
		// the individual sums of the overlapping area
		//final double[] sums = new double[ data.size() ];
		int minNumOverlap = data.size();
		
		final RealSum sum = new RealSum();
		// the number of overlapping pixels
		long count = 0;
		
		final ArrayList<Cursor<FloatType>> cursorsImage = new ArrayList<Cursor<FloatType>>();
		final ArrayList<Cursor<FloatType>> cursorsWeight = new ArrayList<Cursor<FloatType>>();
		
		for ( final LRFFT fft : data )
		{
			cursorsImage.add( fft.getImage().createCursor() );
			if ( fft.getWeight() != null )
				cursorsWeight.add( fft.getWeight().createCursor() );
		}
		
		final Cursor<FloatType> cursor = cursorsImage.get( 0 );

		// sum overlapping area individually
		while ( cursor.hasNext() )
		{
			for ( final Cursor<FloatType> c : cursorsImage )
				c.fwd();
			
			for ( final Cursor<FloatType> c : cursorsWeight )
				c.fwd();
			
			// sum up individual intensities
			double sumLocal = 0;
			int countLocal = 0;
			
			for ( int i = 0; i < cursorsImage.size(); ++i )
			{
				if ( cursorsWeight.get( i ).getType().get() != 0 )
				{
					sumLocal += cursorsImage.get( i ).getType().get();
					countLocal++;
				}
			}

			// at least two overlap
			if ( countLocal > 1 )
			{
				sum.add( sumLocal );
				count += countLocal;
			}
			
			if ( countLocal > 0 )
				minNumOverlap = Math.min( countLocal, minNumOverlap );
		}

		
		IJ.log( "Min number of overlapping views: " + minNumOverlap );
		
		for ( final LRFFT view : data )
			for ( final FloatType t : view.getWeight() )
				t.mul( minNumOverlap );
		

		if ( count == 0 )
			return 1;
		
		// compute the average sum
		final double avg = sum.getSum() / (double)count;
		
		// return the average intensity in the overlapping area
		return avg;
	}

	/**
	 * Adds additive gaussian noise: i = i + gauss(x, sigma)
	 * 
	 * @param amount - how many times sigma
	 * @return the signal-to-noise ratio (measured)
	 */
	public static double addGaussianNoise( final Image< FloatType > img, final Random rnd, final float sigma, boolean onlyPositive )
	{
		for ( final FloatType f : img )
		{
			float newValue = f.get() + (float)( rnd.nextGaussian() * sigma );
			
			if ( onlyPositive )
				newValue = Math.max( 0, newValue );
			
			f.set( newValue );
		}
		
		return 1;
	}

	/**
	 * Adds additive and multiplicative gaussian noise: i = i*gauss(x,sigma) + gauss(x, sigma)
	 * 
	 * @param amount - how many times sigma
	 * @return the signal-to-noise ratio (measured)
	 */
	public static double addGaussianNoiseAddMul( final Image< FloatType > img, final Random rnd, final float sigma, boolean onlyPositive )
	{
		for ( final FloatType f : img )
		{
			final float value = f.get();
			float newValue = value*(1+(float)( rnd.nextGaussian() * sigma/3 )) + (float)( Math.abs( rnd.nextGaussian() ) * sigma );
			
			if ( onlyPositive )
				newValue = Math.max( 0, newValue );
			
			f.set( newValue );
		}
		
		return 1;
	}

	public static void translate( final Image< FloatType > img, final float[] vector )
	{
		final Image< FloatType > tmp = img.clone();
		
		final LocalizableCursor< FloatType > cursor1 = img.createLocalizableCursor();		
		final Interpolator< FloatType > interpolator = tmp.createInterpolator( new LinearInterpolatorFactory<FloatType>( new OutOfBoundsStrategyMirrorFactory<FloatType>() ) );
		
		final int numDimensions = img.getNumDimensions();
		final float[] pos = new float[ numDimensions ];
		
		while ( cursor1.hasNext() )
		{
			cursor1.fwd();
			
			for( int d = 0; d < numDimensions; ++d )
				pos[ d ] = cursor1.getPosition( d ) - vector[ d ];
			
			interpolator.setPosition( pos );
			cursor1.getType().set( interpolator.getType() );
		}
		
		cursor1.close();
		interpolator.close();
	}
	
	/**
	 * Adjusts an image so that the minimal intensity is minValue and the average is average
	 * 
	 * @param image - the image to norm
	 * @param minValue - the minimal value
	 * @param targetAverage - the average that we want to have
	 */
	public static void adjustImage( final Image<FloatType> image, final float minValue, final float targetAverage )
	{
		// first norm the image to an average of (targetAverage - minValue)
		final double avg = sumImage( image )/(double)image.getNumPixels();
		final double correction = ( targetAverage - minValue ) / avg;

		// correct 
		for ( final FloatType t : image )
			t.set( (float)( t.get() * correction ) );
			
		// now add minValue to all pixels
		for ( final FloatType t : image )
			t.set( t.get() + minValue );
		
		//System.out.println( sumImage( image )/(double)image.getNumPixels() );
	}

}
