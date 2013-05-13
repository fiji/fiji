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

package mpicbg.imglib.algorithm.scalespace;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.MultiThreaded;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.function.SubtractNorm;
import mpicbg.imglib.algorithm.gauss.GaussianConvolution;
import mpicbg.imglib.algorithm.gauss.GaussianConvolution2;
import mpicbg.imglib.algorithm.math.ImageCalculatorInPlace;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursorFactory;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.function.Function;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.NumericType;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class DifferenceOfGaussian < A extends Type<A>, B extends NumericType<B> & Comparable<B> > implements Algorithm, MultiThreaded, Benchmark
{
	public static enum SpecialPoint { INVALID, MIN, MAX };
	
	protected final Image<A> image;
	protected Image<B> dogImage;
	protected final ImageFactory<B> factory;
	protected final OutOfBoundsStrategyFactory<B> outOfBoundsFactory;
	
	final double[] sigma1, sigma2;
	final B normalizationFactor, minPeakValue, negMinPeakValue, zero, one, minusOne;
	
	protected final ArrayList<DifferenceOfGaussianPeak<B>> peaks = new ArrayList<DifferenceOfGaussianPeak<B>>();
	protected final Converter<A, B> converter;
	
	boolean computeConvolutionsParalell, keepDoGImage;
	long processingTime;
	int numThreads;
	String errorMessage = "";

	static private final double[] asArray( final int nDim, final double sigma )
	{
		final double[] s = new double[ nDim ];
		for (int i=0; i<nDim; ++i)
			s[ i ] = sigma;
		return s;
	}

	/** Calls the DifferenceOfGaussian constructor with the given sigmas copied into double[] arrays,
	 * one entry per {@param img} dimension. */
	public DifferenceOfGaussian( final Image<A> img, final ImageFactory<B> factory, final Converter<A, B> converter,
		    final OutOfBoundsStrategyFactory<B> outOfBoundsFactory, 
		    final double sigma1, final double sigma2, final B minPeakValue, final B normalizationFactor )
	{
		this( img, factory, converter, outOfBoundsFactory, asArray(img.getNumDimensions(), sigma1),
				asArray(img.getNumDimensions(), sigma2), minPeakValue, normalizationFactor );
	}

	/**
	 * Extracts local minima and maxima of a certain size. It therefore computes the difference of gaussian 
	 * for an {@link Image} and detects all local minima and maxima in 3x3x3x....3 environment, which is returned
	 * as an {@link ArrayList} of {@link DifferenceOfGaussianPeak}s. The two sigmas define the scale on which
	 * extrema are identified, it correlates with the size of the object. 
	 * Note that not only extrema of this size are found, but they will have the higher absolute values. Note as
	 * well that the values of the difference of gaussian image is also defined by the distance between the two
	 * sigmas. A normalization if necessary can be found in the {@link ScaleSpace} class.    
	 * 
	 * Also note a technical detail, the method findPeaks(Image<B> img) can be called on any image if the image
	 * from where the extrema should be computed already exists.
	 * 
	 * @param img - The input {@link Image}<A>
	 * @param factory - The {@link ImageFactory}<B> which defines the datatype in which the computation is performed
	 * @param converter - The {@link Converter}<A,B> which defines how to convert <A> into <B>
	 * @param outOfBoundsFactory - The {@link OutOfBoundsStrategyFactory} necessary for the {@link GaussianConvolution}
	 * @param sigma1 - The lower sigma
	 * @param sigma2 - The higher sigma
	 * @param minPeakValue - 
	 * @param normalizationFactor
	 */
	public DifferenceOfGaussian( final Image<A> img, final ImageFactory<B> factory, final Converter<A, B> converter,
			    final OutOfBoundsStrategyFactory<B> outOfBoundsFactory, 
			    final double[] sigma1, final double[] sigma2, final B minPeakValue, final B normalizationFactor )
	{
		this.processingTime = -1;
		this.computeConvolutionsParalell = true;
		setNumThreads();
	
		this.image = img;
		this.factory = factory;
		this.outOfBoundsFactory = outOfBoundsFactory;
		this.converter = converter;
		
		this.sigma1 = sigma1;
		this.sigma2 = sigma2;
		this.normalizationFactor = normalizationFactor;
		this.minPeakValue = minPeakValue;
		
		this.zero = factory.createType();
		this.zero.setZero();
		this.one = factory.createType();
		this.one.setOne();
		this.minusOne = factory.createType();
		this.minusOne.setZero();
		this.minusOne.sub( one );
		
		this.negMinPeakValue = minPeakValue.copy();
		this.negMinPeakValue.mul( minusOne );
		this.dogImage = null;
		this.keepDoGImage = false;
	}
	
	public void setMinPeakValue( final B value ) { this.minPeakValue.set( value ); }
	public B getMinPeakValue() { return minPeakValue.copy(); }
	public Image<B> getDoGImage() { return dogImage; }
	public void setKeepDoGImage( final boolean keepDoGImage ) { this.keepDoGImage = keepDoGImage; }
	public boolean getKeepDoGImage() { return keepDoGImage; }
	public ArrayList<DifferenceOfGaussianPeak<B>> getPeaks() { return peaks; }
	public void setComputeConvolutionsParalell( final boolean paralell ) { this.computeConvolutionsParalell = paralell; }
	public boolean getComputeConvolutionsParalell() { return computeConvolutionsParalell; }
	
	/**
	 * This method returns the {@link OutputAlgorithm} that will compute the Gaussian Convolutions, more efficient versions can override this method
	 * 
	 * @param sigma - the sigma of the convolution
	 * @param nThreads - the number of threads for this convolution
	 * @return
	 */
	protected OutputAlgorithm<B> getGaussianConvolution( final double[] sigma, final int nThreads )
	{
		final GaussianConvolution2<A, B> gauss = new GaussianConvolution2<A, B>( image, factory, outOfBoundsFactory, converter, sigma );
		
		return gauss;
	}
	
	/**
	 * Returns the function that does the normalized subtraction of the gauss images, more efficient versions can override this method
	 * @return - the Subtraction Function
	 */
	protected Function<B, B, B> getNormalizedSubtraction()
	{
		return new SubtractNorm<B>( normalizationFactor );
	}

	/**
	 * Checks if the absolute value of the current peak is high enough, more efficient versions can override this method
	 * @param value - the current value
	 * @return true if the absoluted value is high enough, otherwise false 
	 */
	protected boolean isPeakHighEnough( final B value )
	{
		if ( value.compareTo( zero ) >= 0 )
		{
			// is a positive extremum
			if ( value.compareTo( minPeakValue ) >= 0 )
				return true;
			else 
				return false;
		}
		else
		{
			// is a negative extremum
			if ( value.compareTo( negMinPeakValue ) <= 0 )
				return true;
			else
				return false;
		}
	}

	/**
	 * Checks if the current position is a minima or maxima in a 3^n neighborhood, more efficient versions can override this method
	 *  
	 * @param neighborhoodCursor - the {@link LocalNeighborhoodCursor}
	 * @param centerValue - the value in the center which is tested
	 * @return - if is a minimum, maximum or nothig
	 */
	protected SpecialPoint isSpecialPoint( final LocalNeighborhoodCursor<B> neighborhoodCursor, final B centerValue )
	{
		boolean isMin = true;
		boolean isMax = true;

		while ( (isMax || isMin) && neighborhoodCursor.hasNext() )
		{			
			neighborhoodCursor.fwd();
			
			final B value = neighborhoodCursor.getType(); 
			
			// it can still be a minima if the current value is bigger/equal to the center value
			isMin &= ( value.compareTo( centerValue ) >= 0 );
			
			// it can still be a maxima if the current value is smaller/equal to the center value
			isMax &= ( value.compareTo( centerValue ) <= 0 );
		}	
		
		// this mixup is intended, a minimum in the 2nd derivation is a maxima in image space and vice versa
		if ( isMin )
			return SpecialPoint.MAX;
		else if ( isMax )
			return SpecialPoint.MIN;
		else
			return SpecialPoint.INVALID;
	}
		
	@Override
	public boolean process()
	{
		final long startTime = System.currentTimeMillis();
		
		//
		// perform the gaussian convolutions transferring it to the new (potentially higher precision) type T
		//
		final int divisor = computeConvolutionsParalell ? 2 : 1;
		final OutputAlgorithm<B> conv1 = getGaussianConvolution( sigma1, Math.max( 1, getNumThreads() / divisor ) );
		final OutputAlgorithm<B> conv2 = getGaussianConvolution( sigma2, Math.max( 1, getNumThreads() / divisor ) );
		        
        final Image<B> gauss1, gauss2;
        
        if ( conv1.checkInput() && conv2.checkInput() )
        {       	
            final AtomicInteger ai = new AtomicInteger(0);					
            Thread[] threads = SimpleMultiThreading.newThreads( divisor );

        	for (int ithread = 0; ithread < threads.length; ++ithread)
                threads[ithread] = new Thread(new Runnable()
                {
                    public void run()
                    {
                    	final int myNumber = ai.getAndIncrement();
                    	if ( myNumber == 0 || !computeConvolutionsParalell )
                    	{
                    		if ( !conv1.process() )
                            	System.out.println( "Cannot compute gaussian convolution 1: " + conv1.getErrorMessage() );                    		
                    	}
                    	
                    	if ( myNumber == 1 || !computeConvolutionsParalell )
                    	{
                    		if ( !conv2.process() )
                    			System.out.println( "Cannot compute gaussian convolution 2: " + conv2.getErrorMessage() );
                    	}                    	
                    }
                });
        	
    		SimpleMultiThreading.startAndJoin( threads );       	
        }
        else
        {
        	errorMessage =  "Cannot compute gaussian convolutions: " + conv1.getErrorMessage() + " & " + conv2.getErrorMessage();
        
        	gauss1 = gauss2 = null;
        	return false;
        }
                
        if ( conv1.getErrorMessage().length() == 0 && conv2.getErrorMessage().length() == 0 )
        {
	        gauss1 = conv1.getResult();
	        gauss2 = conv2.getResult();
        }
        else
        {
        	gauss1 = gauss2 = null;
        	return false;        	
        }

        //
        // subtract the images to get the LaPlace image
        //
        final Function<B, B, B> function = getNormalizedSubtraction();        
        final ImageCalculatorInPlace<B, B> imageCalc = new ImageCalculatorInPlace<B, B>( gauss2, gauss1, function );
        
        if ( !imageCalc.checkInput() || !imageCalc.process() )
        {
        	errorMessage =  "Cannot subtract images: " + imageCalc.getErrorMessage();
        	
        	gauss1.close();
        	gauss2.close();
        	
        	return false;
        }

        gauss1.close();
        
        /*
        gauss2.setName( "laplace" );
        gauss2.getDisplay().setMinMax();
        ImageJFunctions.copyToImagePlus( gauss2 ).show();
        */
        
        //
        // Now we find minima and maxima in the DoG image
        //        
		peaks.clear();
		peaks.addAll( findPeaks( gauss2 ) );

		if ( keepDoGImage )
			dogImage = gauss2;
		else
			gauss2.close(); 
        		
        processingTime = System.currentTimeMillis() - startTime;
		
		return true;
	}
	
	public ArrayList<DifferenceOfGaussianPeak<B>> findPeaks( final Image<B> laPlace )
	{
	    final AtomicInteger ai = new AtomicInteger( 0 );					
	    final Thread[] threads = SimpleMultiThreading.newThreads( getNumThreads() );
	    final int nThreads = threads.length;
	    final int numDimensions = laPlace.getNumDimensions();
	    
	    final Vector< ArrayList<DifferenceOfGaussianPeak<B>> > threadPeaksList = new Vector< ArrayList<DifferenceOfGaussianPeak<B>> >();
	    
	    for ( int i = 0; i < nThreads; ++i )
	    	threadPeaksList.add( new ArrayList<DifferenceOfGaussianPeak<B>>() );

		for (int ithread = 0; ithread < threads.length; ++ithread)
	        threads[ithread] = new Thread(new Runnable()
	        {
	            public void run()
	            {
	            	final int myNumber = ai.getAndIncrement();
            	
	            	final ArrayList<DifferenceOfGaussianPeak<B>> myPeaks = threadPeaksList.get( myNumber );	
	            	final LocalizableByDimCursor<B> cursor = laPlace.createLocalizableByDimCursor();	            	
	            	final LocalNeighborhoodCursor<B> neighborhoodCursor = LocalNeighborhoodCursorFactory.createLocalNeighborhoodCursor( cursor );
	            	
	            	final int[] position = new int[ numDimensions ];
	            	final int[] dimensionsMinus2 = laPlace.getDimensions();

            		for ( int d = 0; d < numDimensions; ++d )
            			dimensionsMinus2[ d ] -= 2;
	            	
MainLoop:           while ( cursor.hasNext() )
	                {
	                	cursor.fwd();
	                	cursor.getPosition( position );
	                	
	                	if ( position[ 0 ] % nThreads == myNumber )
	                	{
	                		for ( int d = 0; d < numDimensions; ++d )
	                		{
	                			final int pos = position[ d ];
	                			
	                			if ( pos < 1 || pos > dimensionsMinus2[ d ] )
	                				continue MainLoop;
	                		}

	                		// if we do not clone it here, it might be moved along with the cursor
	                		// depending on the container type used
	                		final B currentValue = cursor.getType().copy();
	                		
	                		// it can never be a desired peak as it is too low
	                		if ( !isPeakHighEnough( currentValue ) )
                				continue;

                			// update to the current position
                			neighborhoodCursor.update();

                			// we have to compare for example 26 neighbors in the 3d case (3^3 - 1) relative to the current position
                			final SpecialPoint specialPoint = isSpecialPoint( neighborhoodCursor, currentValue ); 
                			if ( specialPoint != SpecialPoint.INVALID )
                				myPeaks.add( new DifferenceOfGaussianPeak<B>( position, currentValue, specialPoint ) );
                			
                			// reset the position of the parent cursor
                			neighborhoodCursor.reset();	                				                		
	                	}
	                }
                
	                cursor.close();
            }
        });
	
		SimpleMultiThreading.startAndJoin( threads );		

		// put together the list from the various threads	
		final ArrayList<DifferenceOfGaussianPeak<B>> dogPeaks = new ArrayList<DifferenceOfGaussianPeak<B>>();
		
		for ( final ArrayList<DifferenceOfGaussianPeak<B>> peakList : threadPeaksList )
			dogPeaks.addAll( peakList );		
		
		return dogPeaks;
	}
	
	@Override
	public boolean checkInput()
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( image == null )
		{
			errorMessage = "DifferenceOfGaussian: [Image<A> img] is null.";
			return false;
		}
		else if ( factory == null )
		{
			errorMessage = "DifferenceOfGaussian: [ImageFactory<B> img] is null.";
			return false;
		}
		else if ( outOfBoundsFactory == null )
		{
			errorMessage = "DifferenceOfGaussian: [OutOfBoundsStrategyFactory<B>] is null.";
			return false;
		}
		else
			return true;
	}

	@Override
	public String getErrorMessage() { return errorMessage; }

	@Override
	public long getProcessingTime() { return processingTime; }
	
	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads( final int numThreads ) { this.numThreads = numThreads; }

	@Override
	public int getNumThreads() { return numThreads; }	
}
