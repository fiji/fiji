/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2012 Stephan Preibisch, Stephan Saalfeld, Tobias
 * Pietzsch, Albert Cardona, Barry DeZonia, Curtis Rueden, Lee Kamentsky, Larry
 * Lindsey, Johannes Schindelin, Christian Dietz, Grant Harris, Jean-Yves
 * Tinevez, Steffen Jaensch, Mark Longair, Nick Perry, and Jan Funke.
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

package fiji.plugin.trackmate.detection.subpixel;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;
import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class QuadraticSubpixelLocalization< T extends RealType<T> > implements Algorithm, Benchmark, MultiThreaded {
	
	protected final Img<T> laPlacian;
	protected final List<SubPixelLocalization<T>> peaks;
	
	protected int maxNumMoves = 4;
	protected boolean allowMaximaTolerance = false;
	protected boolean canMoveOutside = false;
	protected float maximaTolerance = 0.01f;
	
	protected final ImgFactory<DoubleType> doubleArrayFactory;
	protected boolean[] allowedToMoveInDim;
	
	protected long processingTime;
	protected int numThreads = 1;
	protected String errorMessage = "";
	
	/*
	 * CONSTRUCTOR
	 */
	
	public QuadraticSubpixelLocalization( final Img<T> laPlacian, final List<SubPixelLocalization<T>> peaks )	{
		setNumThreads();
		this.laPlacian = laPlacian;
		this.peaks = peaks;
		this.allowedToMoveInDim = new boolean[ laPlacian.numDimensions() ];
		
		// principally one can move in any dimension
		for ( int d = 0; d < allowedToMoveInDim.length; ++d )
			allowedToMoveInDim[ d ] = true;
		
		this.doubleArrayFactory = new ArrayImgFactory<DoubleType>();
	}
	
	/*
	 * SETTER & GETTERS
	 */
	
	public void setAllowMaximaTolerance( final boolean allowMaximaTolerance ) { this.allowMaximaTolerance = allowMaximaTolerance; }
	public void setCanMoveOutside( final boolean canMoveOutside ) { this.canMoveOutside = canMoveOutside; }
	public void setMaximaTolerance( final float maximaTolerance ) { this.maximaTolerance = maximaTolerance; }
	public void setMaxNumMoves( final int maxNumMoves ) { this.maxNumMoves = maxNumMoves; }
	public void setAllowedToMoveInDim( final boolean[] allowedToMoveInDim ) { this.allowedToMoveInDim = allowedToMoveInDim.clone(); }
	
	public boolean getAllowMaximaTolerance() { return allowMaximaTolerance; }
	public boolean getCanMoveOutside() { return canMoveOutside; }
	public float getMaximaTolerance() { return maximaTolerance; }
	public Img<T> getLaPlaceImage() { return laPlacian; }
	public int getMaxNumMoves() { return maxNumMoves; }
	public boolean[] getAllowedToMoveInDim() { return allowedToMoveInDim.clone(); }

	/*
	 * METHODS
	 */
	
	
	protected boolean handleFailure( final SubPixelLocalization<T> peak, final String error )	{
		peak.setLocationType( SubPixelLocalization.LocationType.INVALID );
		peak.setErrorMessage( error );
		return false;
	}
	
	@Override 
	public boolean process()
	{
		final long startTime = System.currentTimeMillis();
		
	    final AtomicInteger ai = new AtomicInteger( 0 );					
	    final Thread[] threads = SimpleMultiThreading.newThreads( getNumThreads() );
	    final int numThreads = threads.length;
	    
		for (int ithread = 0; ithread < threads.length; ++ithread)
	        threads[ithread] = new Thread(new Runnable()   {
	            public void run() {
	            	final int myNumber = ai.getAndIncrement();
	            	
	            	for ( int i = 0; i < peaks.size(); ++i ) {
	            		if ( i % numThreads == myNumber ) {
	            			final SubPixelLocalization<T> peak;	            			
	            			synchronized ( peaks ) { peak = peaks.get( i ); }
	            			analyzePeak( peak );
	            		}
	            	}
	            }
	        });
		
		SimpleMultiThreading.startAndJoin( threads );
		
		processingTime = System.currentTimeMillis() - startTime;
		
		return true;
	}
	
	public boolean analyzePeak( final SubPixelLocalization<T> peak ) {
		final int numDimensions = laPlacian.numDimensions(); 

		// the subpixel values
		final double[] subpixelLocation = new double[ numDimensions ];
		
		// the cursor for the computation (one that cannot move out of image)
		final RandomAccess<T> cursor;
		
		if ( canMoveOutside ) {
			cursor = Views.extendPeriodic(laPlacian).randomAccess();
		} else {
			cursor = laPlacian.randomAccess();
		}
		
		// the current hessian matrix and derivative vector
		Img<DoubleType> hessianMatrix = doubleArrayFactory.create( new int[] { cursor.numDimensions(), cursor.numDimensions() }, 
				new DoubleType());
		Img<DoubleType> derivativeVector = doubleArrayFactory.create( new int[] { cursor.numDimensions() }, 
				new DoubleType());
		
		// the inverse hessian matrix
		Matrix A, B, X;
		
		// the current value of the center
		cursor.setPosition(peak);
		T value = cursor.get().copy();
		
		boolean foundStableMaxima = true;
		boolean pointsValid = false;
		int numMoves = 0;
		
		long[] currentPosition = new long[peak.numDimensions()];
		peak.localize(currentPosition);
		
		// fit n-dimensional quadratic function to the extremum and 
		// if the extremum is shifted more than 0.5 in one or more 
		// directions we test wheather it is better there
		// until we 
		//   - converge (find a stable extremum)
		//   - move out of the image
		//   - achieved the maximal number of moves
		
		do
		{
			++numMoves;
			
			// move the cursor to the current positon
			cursor.setPosition( currentPosition );
			
			// store the center value
			value.set( cursor.get() );
			
			// compute the n-dimensional hessian matrix [numDimensions][numDimensions]
			// containing all second derivatives, e.g. for 3d:
			//
			// xx xy xz
			// yx yy yz
			// zx zy zz			
			hessianMatrix = getHessianMatrix( cursor, hessianMatrix );
						
			// compute the inverse of the hessian matrix
			A = invertMatrix( hessianMatrix );
			
			if ( A == null ) {
				return handleFailure( peak, "Cannot invert hessian matrix" );
			}
			
			// compute the n-dimensional derivative vector
			derivativeVector = getDerivativeVector( cursor, derivativeVector );
			B = getMatrix( derivativeVector );
			
			if ( B == null ) {
				return handleFailure( peak, "Cannot compute derivative vector" );
			}
			
			// compute the extremum of the n-dimensinal quadratic fit
			X = ( A.uminus() ).times( B );
			
			for ( int d = 0; d < numDimensions; ++d )
				subpixelLocation[ d ] = X.get( d, 0 );
			
			// test all dimensions for their change
			// if the absolute value of the subpixel location
			// is bigger than 0.5 we move into that direction
			foundStableMaxima = true;
			
			for ( int d = 0; d < numDimensions; ++d ) {
				// Normally, above an offset of 0.5 the base position
				// has to be changed, e.g. a subpixel location of 4.7
				// would mean that the new base location is 5 with an offset of -0.3
				//
				// If we allow an increasing maxima tolerance we will 
				// not change the base position that easily. Sometimes
				// it simply jumps from left to right and back, because
				// it is 4.51 (i.e. goto 5), then 4.49 (i.e. goto 4)
				// Then we say, ok, lets keep the base position even if 
				// the subpixel location is 0.6...
				
				final double threshold = allowMaximaTolerance ? 0.5 + numMoves * maximaTolerance : 0.5;
				
				if ( Math.abs( subpixelLocation[ d ] ) > threshold ) {
					if ( allowedToMoveInDim[ d ] ) {
						// move to another base location
						currentPosition[ d ] += Math.signum( subpixelLocation[ d ] );
						foundStableMaxima = false;
					} else {
						// set it to the position that is maximally away when keeping the current base position
						// e.g. if (0.7) do 4 -> 4.5 (although it should be 4.7, i.e. a new base position of 5) 
						// or  if (-0.9) do 4 -> 3.5 (although it should be 3.1, i.e. a new base position of 3)
						subpixelLocation[ d ] = Math.signum( subpixelLocation[ d ] ) * 0.5;
					}
				}				
			}
			
			// check validity of the new location if there is a need to move
			pointsValid = true;

			if ( !canMoveOutside )
				if ( !foundStableMaxima ) 
					for ( int d = 0; d < numDimensions; ++d )
						if ( currentPosition[ d ] <= 0 || currentPosition[ d ] >= laPlacian.dimension( d ) - 1 ) 
							pointsValid = false;
			
		} 
		while ( numMoves <= maxNumMoves && !foundStableMaxima && pointsValid );

		if ( !foundStableMaxima )
			return handleFailure( peak, "No stable extremum found." );

		if ( !pointsValid )
			return handleFailure( peak, "Moved outside of the image." );
		
		// compute the function value (intensity) of the fit
		double quadrFuncValue = 0;
		
		for ( int d = 0; d < numDimensions ; ++d )
			quadrFuncValue += X.get( d, 0 ) * B.get( d, 0 );
		
		quadrFuncValue /= 2.0;
				
		// set the results if everything went well
		
		// subpixel location
		for ( int d = 0; d < numDimensions; ++d )
			peak.setSubPixelLocationOffset( subpixelLocation[ d ], d );

		// pixel location
		peak.localize( currentPosition );

		// quadratic fit value
		final T quadraticFit = peak.getInterpolatedValue();
		quadraticFit.setReal( quadrFuncValue );
		
		// normal value
		peak.setValue( value );
		
		return true;
	}

	/**
	 * This method is called by the process method to allow to override how the matrix is inverted
	 */
	protected Matrix invertMatrix( final Img<DoubleType> matrixImage ) {
		final Matrix matrix = getMatrix( matrixImage );
		
		if ( matrix == null )
			return null;
		
		return computePseudoInverseMatrix( matrix, 0.001 );
	}
	
	/**
	 * This method is called by the process method to allow to override how the derivative vector is computed
	 */
	protected Img<DoubleType> getDerivativeVector( final RandomAccess<T> cursor, final Img<DoubleType> derivativeVector ) {
		computeDerivativeVector( cursor, derivativeVector );
		return derivativeVector;
	}
	
	/**
	 * This method is called by the process method to allow to override how the hessian matrix is computed
	 */
	protected Img<DoubleType> getHessianMatrix( final RandomAccess<T> cursor, final Img<DoubleType> hessianMatrix )	{
		computeHessianMatrix( cursor, hessianMatrix );
		return hessianMatrix;
	}

	/**
	 * Converts an {@link Img} into a matrix
	 * 
	 * @param maxtrixImage - the input {@link Img}
	 * @return a {@link Matrix} or null if the {@link Img} is not one or two-dimensional
	 */
	public static <S extends RealType<S>> Matrix getMatrix( final Img<S> maxtrixImage ) {
		final int numDimensions = maxtrixImage.numDimensions();
		
		if ( numDimensions > 2 )
			return null;
		
		final Matrix matrix;
		
		if ( numDimensions == 1) {
			matrix = new Matrix( (int) maxtrixImage.dimension( 0 ), 1 );
			final Cursor<S> cursor = maxtrixImage.localizingCursor();
			while ( cursor.hasNext() ) {
				cursor.fwd();			
				matrix.set( cursor.getIntPosition( 0 ), 0, cursor.get().getRealDouble() );
			}
			
		} else {
			matrix = new Matrix( (int)  maxtrixImage.dimension( 0 ), (int) maxtrixImage.dimension( 1 ) );
			final Cursor<S> cursor = maxtrixImage.localizingCursor();
			
			while ( cursor.hasNext() ) {
				cursor.fwd();			
				matrix.set( cursor.getIntPosition( 0 ), cursor.getIntPosition( 1 ), cursor.get().getRealDouble() );
			}
			
		}
		
		return matrix;
	}
	
	/**
	 * Computes the pseudo-inverse of a matrix using Singular Value Decomposition
	 * 
	 * @param M - the input {@link Matrix}
	 * @param threshold - the threshold for inverting diagonal elements (suggested 0.001)
	 * @return the inverted {@link Matrix} or an approximation with lowest possible squared error
	 */
	final public static Matrix computePseudoInverseMatrix( final Matrix M, final double threshold ) {
		final SingularValueDecomposition svd = new SingularValueDecomposition( M );

		Matrix U = svd.getU(); // U Left Matrix
		final Matrix S = svd.getS(); // W
		final Matrix V = svd.getV(); // VT Right Matrix

		double temp;

		// invert S
		for ( int j = 0; j < S.getRowDimension(); ++j )
		{
			temp = S.get( j, j );

			if ( temp < threshold ) // this is an inaccurate inverting of the matrix 
				temp = 1.0 / threshold;
			else 
				temp = 1.0 / temp;
			
			S.set( j, j, temp );
		}

		// transponse U
		U = U.transpose();

		//
		// compute result
		//
		return ((V.times(S)).times(U));
	}
	
	/**
	 * Computes the n-dimensional 1st derivative vector in 3x3x3...x3 environment for a certain {@link Img} location
	 * defined by the position of the {@link RandomAccess}.
	 * 
	 * @param cursor - the position for which to compute the Hessian Matrix
	 * @return Img<DoubleType> - the derivative, which is essentially a one-dimensional {@link DoubleType} {@link Img} of size [numDimensions]
	 */
	final public static <T extends RealType<T>> Img<DoubleType> computeDerivativeVector( final RandomAccess<T> cursor ) {
		final ImgFactory<DoubleType> factory = new ArrayImgFactory<DoubleType>();
		final Img<DoubleType> derivativeVector = factory.create( new int[] { cursor.numDimensions() }, new DoubleType() );
		
		computeDerivativeVector( cursor, derivativeVector );
		
		return derivativeVector;
	}

	/**
	 * Computes the n-dimensional 1st derivative vector in 3x3x3...x3 environment for a certain {@link Img} location
	 * defined by the position of the {@link RandomAccess}.
	 * 
	 * @param cursor - the position for which to compute the Hessian Matrix
	 * @param Img<DoubleType> - the derivative, which is essentially a one-dimensional {@link DoubleType} {@link Img} of size [numDimensions]
	 */
	final public static <T extends RealType<T>> void computeDerivativeVector( final RandomAccess<T> cursor, final Img<DoubleType> derivativeVector ) {
		// instantiate a cursor to traverse over the derivative vector we want to compute, the position defines the current dimension
		final Cursor<DoubleType> derivativeCursor = derivativeVector.localizingCursor();
		
		while ( derivativeCursor.hasNext() ) {
			derivativeCursor.fwd();
			
			final int dim = derivativeCursor.getIntPosition( 0 );
			
			// we compute the derivative for dimension A like this
			//
			// | a0 | a1 | a2 | 
			//        ^
			//        |
			//  Original position of image cursor
			//
			// d(a) = (a2 - a0)/2
			// we divide by 2 because it is a jump over two pixels
			
			cursor.fwd( dim );
			
			final double a2 = cursor.get().getRealDouble();
			
			cursor.bck( dim );
			cursor.bck( dim );
			
			final double a0 = cursor.get().getRealDouble();
			
			// back to the original position
			cursor.fwd( dim );
						
			derivativeCursor.get().setReal( (a2 - a0)/2 );
		}
	}
	
	/**
	 * Computes the n-dimensional Hessian Matrix in 3x3x3...x3 environment for a certain {@link Img} location
	 * defined by the position of the {@link RandomAccess}.
	 * 
	 * @param cursor - the position for which to compute the Hessian Matrix
	 * @return Img<DoubleType> - the hessian matrix, which is essentially a two-dimensional {@link DoubleType} {@link Img} of size [numDimensions][numDimensions]
	 */
	final public static <T extends RealType<T>> Img<DoubleType> computeHessianMatrix( final RandomAccess<T> cursor ) {
		final ImgFactory<DoubleType> factory = new ArrayImgFactory<DoubleType>();
		final Img<DoubleType> hessianMatrix = factory.create( new int[] { cursor.numDimensions(), cursor.numDimensions() }, 
				new DoubleType());
		
		computeHessianMatrix( cursor, hessianMatrix );
		
		return hessianMatrix;
	}

	/**
	 * Computes the n-dimensional Hessian Matrix in 3x3x3...x3 environment for a certain {@link Img} location
	 * defined by the position of the {@link RandomAccess}.
	 * 
	 * @param cursor - the position for which to compute the Hessian Matrix
	 * @param Img<DoubleType> - the hessian matrix, which is essentially a two-dimensional {@link DoubleType} {@link Img} of size [numDimensions][numDimensions]
	 */
	final public static <T extends RealType<T>> void computeHessianMatrix( final RandomAccess<T> cursor, final Img<DoubleType> hessianMatrix ) {
		// we need this for all diagonal elements
		final double temp = 2.0 * cursor.get().getRealDouble();
		
		// instantiate a cursor to traverse over the hessian matrix we want to compute, the position defines the current dimensions
		final Cursor<DoubleType> hessianCursor = hessianMatrix.localizingCursor();
		
		// another cursor to fill the redundant lower area of the matrix
		final RandomAccess<DoubleType> hessianCursorLowerHalf = hessianMatrix.randomAccess();
		
		while ( hessianCursor.hasNext() ) {
			hessianCursor.fwd();
			
			final int dimA = hessianCursor.getIntPosition( 0 );
			final int dimB = hessianCursor.getIntPosition( 1 );
			
			if ( dimA == dimB ) {
				// diagonal elements h(aa) for dimension a
				// computed from the row a in the input image
				//
				// | a0 | a1 | a2 | 
				//        ^
				//        |
				//  Original position of image cursor
				//
				// h(aa) = (a2-a1) - (a1-a0)
				//       = a2 - 2*a1 + a0
				
				cursor.fwd( dimA );
				
				final double a2 = cursor.get().getRealDouble();
				
				cursor.bck( dimA );
				cursor.bck( dimA );
				
				final double a0 = cursor.get().getRealDouble();
				
				// back to the original position
				cursor.fwd( dimA );		

				hessianCursor.get().set( a2 - temp + a0 );
				
			} else if ( dimB > dimA ) { 
				// we compute all elements above the diagonal (see below for explanation)
			
				// other elements h(ab) are computed as a combination
				// of dimA (dimension a) and dimB (dimension b), i.e. we always operate in a
				// two-dimensional plane
				// ______________________
				// | a0b0 | a1b0 | a2b0 |
				// | a0b1 | a1b1 | a2b1 |
				// | a0b2 | a1b2 | a2b2 |
				// ----------------------
				// where a1b1 is the original position of the cursor
				//
				// h(ab) = ( (a2b2-a0b2)/2 - (a2b0 - a0b0)/2 )/2
				//
				// we divide by 2 because these are always jumps over two pixels
				
				// we only have to do that if dimB > dimA, 
				// because h(ab) = h(ba)
				
				cursor.fwd( dimB );
				cursor.fwd( dimA );
				
				final double a2b2 = cursor.get().getRealDouble();
				
				cursor.bck( dimA );
				cursor.bck( dimA );

				final double a0b2 = cursor.get().getRealDouble();

				cursor.bck( dimB );
				cursor.bck( dimB );

				final double a0b0 = cursor.get().getRealDouble();

				cursor.fwd( dimA );
				cursor.fwd( dimA );

				final double a2b0 = cursor.get().getRealDouble();
				
				// back to the original position
				cursor.bck( dimA );
				cursor.fwd( dimB );
				
				hessianCursor.get().set( ( (a2b2-a0b2)/2 - (a2b0 - a0b0)/2 )/2 );
				
				// update the corresponding element below the diagonal
				hessianCursorLowerHalf.setPosition( dimB, 0 );
				hessianCursorLowerHalf.setPosition( dimA, 1 );
				
				hessianCursorLowerHalf.get().set( hessianCursor.get() );
			}
		}
		
	}
		
	@Override
	public boolean checkInput() {
		if ( errorMessage.length() > 0 )
		{
			return false;
		} else if ( laPlacian == null ) {
			errorMessage = "SubpixelLocalization: [Img<T> img] is null.";
			return false;
		} else if ( peaks == null ) {
			errorMessage = "SubpixelLocalization: [List<SubPixelLocalization<T>> peaks] is null.";
			return false;
		} else if ( peaks.size() == 0 ) {
			errorMessage = "SubpixelLocalization: [List<SubPixelLocalization<T>> peaks] is empty.";
			return false;
		} else
			return true;
	}	

	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads( final int numThreads ) { this.numThreads = numThreads; }

	@Override
	public int getNumThreads() { return numThreads; }	
	
	@Override
	public String getErrorMessage() { return errorMessage; }

	@Override
	public long getProcessingTime() { return processingTime; }
}
