/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2012 Stephan Preibisch, Stephan Saalfeld, Tobias
 * Pietzsch, Albert Cardona, Barry DeZonia, Curtis Rueden, Lee Kamentsky, Larry
 * Lindsey, Johannes Schindelin, Christian Dietz, Grant Harris, Jean-Yves
 * Tinevez, Steffen Jaensch, Mark Longair, Nick Perry, and Jan Funke.
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

package fiji.plugin.trackmate.util;

import java.util.Iterator;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.IterableRealInterval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RealPositionable;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgPlus;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.TransformBuilder;
import net.imglib2.view.Views;

/**
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com>
 */
public class HyperSliceImgPlus< T > extends ImgPlus< T > {

	/** The number of dimension in the target {@link ImgPlus}. Equals the number of dimensions
	 * in the source image minus one.  */
	protected final int nDimensions;
	/** The source {@link ImgPlus}. */
	protected final ImgPlus< T > source;
	/** The transform used to access the source from coordinates in the target. */
	protected final MixedTransform transformToSource;
	/** The iterable built by wrapping the {@link #fullViewRandomAccessible}. */
	protected final IterableInterval<T> fullViewIterable;
	/** An optimized RandomAccess over the transformed source. */
	protected RandomAccessible< T > fullViewRandomAccessible;
	/** The transformed source. */
	protected final  MixedTransformView<T> mtv;
	/** The dimension to freeze. */
	protected final int targetDimension;
	/** The target freeze-dimension position. */ 
	protected final long dimensionPosition;

	/*
	 * CONSTRUCTOR
	 */

	public HyperSliceImgPlus( ImgPlus< T > source, final int d, final long pos ) {
		super(source);

		final int m = source.numDimensions();
		this.nDimensions = m - 1;
		this.targetDimension = d;
		this.dimensionPosition = pos;

		// Prepare reslice
		final long[] min = new long[ nDimensions ];
		final long[] max = new long[ nDimensions ];

		final MixedTransform t = new MixedTransform( nDimensions, m );
		final long[] translation = new long[ m ];
		translation[ d ] = pos;
		final boolean[] zero = new boolean[ m ];
		final int[] component = new int[ m ];

		/* Determine transform component & iterable bounds
		 * and defines calibration of the target ImgPlus	 */
		for ( int e = 0; e < m; ++e ) {
			if ( e < d ) {

				zero[ e ] = false;
				component[ e ] = e;
				min[ e ] = source.min( e );
				max[ e ] = source.max( e );
				setCalibration( source.calibration(e), e);
				setAxis( source.axis(e), e);

			} else if ( e > d ) {

				zero[ e ] = false;
				component[ e ] = e - 1;
				min[ e - 1] = source.min( e );
				max[ e - 1] = source.max( e );
				setCalibration( source.calibration(e), e-1);
				setAxis( source.axis(e), e-1);

			} else {

				zero[ e ] = true;
				component[ e ] = 0;

			}
		}

		// Set target name
		setName("Hypserslice of "+source.getName()+" at dim "+d+"="+pos);

		// Create transform and transformed view
		t.setTranslation( translation );
		t.setComponentZero( zero );
		t.setComponentMapping( component );
		this.transformToSource = t;
		this.mtv = new MixedTransformView<T>(source, t);

		// Copy calibration and axes
		int index = 0;
		for (int i = 0; i < m; i++) {
			if (i != d) {
				setCalibration( source.calibration(i) , index );
				setAxis( source.axis(i), index);
				index++;
			}
		}

		this.source = source;
		this.fullViewRandomAccessible = TransformBuilder.getEfficientRandomAccessible( null, mtv );
		this.fullViewIterable =  Views.iterable( Views.interval(fullViewRandomAccessible, min, max) );
	}

	@Override
	public RandomAccess< T > randomAccess( final Interval interval ) {
		return TransformBuilder.getEfficientRandomAccessible( interval, mtv ).randomAccess();
	}

	@Override
	public RandomAccess< T > randomAccess() {
		return fullViewRandomAccessible.randomAccess();
	}


	@Override
	public int numDimensions() {
		return nDimensions;
	}

	@Override
	public long min(final int d) {
		if (d < targetDimension)
			return source.min( d );
		else 
			return source.min( d + 1 );
	}

	@Override
	public void min(final long[] min) {
		for (int d = 0; d < nDimensions; d++) {
			if (d < targetDimension)
				min[d] = source.min( d );
			else 
				min[d] = source.min( d + 1 );
		}
	}

	@Override
	public void min(final Positionable min) {
		for (int d = 0; d < nDimensions; d++) {
			if (d < targetDimension)
				min.setPosition( source.min( d ), d);
			else 
				min.setPosition( source.min( d + 1 ), d);
		}
	}

	@Override
	public long max(final int d) {
		if (d < targetDimension)
			return source.max( d );
		else 
			return source.max( d + 1 );
	}

	@Override
	public void max(final long[] max) {
		for (int d = 0; d < nDimensions; d++) {
			if (d < targetDimension)
				max[d] = source.max( d );
			else 
				max[d] = source.max( d + 1 );
		}
	}

	@Override
	public void max(final Positionable max) {
		for (int d = 0; d < nDimensions; d++) {
			if (d < targetDimension)
				max.setPosition( source.max( d ), d);
			else 
				max.setPosition( source.max( d + 1 ), d);
		}
	}

	@Override
	public void dimensions(final long[] dimensions) {
		for (int d = 0; d < nDimensions; d++) {
			if (d < targetDimension)
				dimensions[ d ] = source.dimension( d);
			else 
				dimensions[ d ] = source.dimension( d + 1 );
		}
	}

	@Override
	public long dimension(final int d) {
		if (d < targetDimension)
			return source.dimension( d );
		else 
			return source.dimension(d + 1);
	}

	@Override
	public double realMin(final int d) {
		if (d < targetDimension)
			return source.realMin( d );
		else 
			return source.realMin( d + 1 );
	}

	@Override
	public void realMin(final double[] min) {
		for (int d = 0; d < nDimensions; d++) {
			if (d < targetDimension)
				min[ d ] = source.realMin( d );
			else 
				min[ d ] = source.realMin( d + 1 );
		}
	}

	@Override
	public void realMin(final RealPositionable min) {
		for (int d = 0; d < nDimensions; d++) {
			if (d < targetDimension)
				min.setPosition( source.realMin( d ), d);
			else 
				min.setPosition( source.realMin( d + 1 ), d);
		}
	}

	@Override
	public double realMax(final int d) {
		if (d < targetDimension)
			return source.realMax( d );
		else 
			return source.realMax( d + 1 );
	}

	@Override
	public void realMax(final double[] max) {
		for (int d = 0; d < nDimensions; d++) {
			if (d < targetDimension)
				max[ d ] = source.realMax( d );
			else 
				max[ d ] = source.realMax( d + 1 );
		}
	}

	@Override
	public void realMax(final RealPositionable max) {
		for (int d = 0; d < nDimensions; d++) {
			if (d < targetDimension)
				max.setPosition( source.realMax( d ), d );
			else 
				max.setPosition( source.realMax( d + 1 ), d );
		}
	}

	@Override
	public Cursor<T> cursor() {
		return fullViewIterable.cursor();
	}

	@Override
	public Cursor<T> localizingCursor() {
		return fullViewIterable.localizingCursor();
	}

	@Override
	public long size() {
		long size = 1;
		for (int d = 0; d < nDimensions; d++) {
			size *= dimension(d);
		}
		return size;
	}

	@Override
	public T firstElement() {
		return source.firstElement();
	}

	@Override
	public Object iterationOrder() {
		return fullViewIterable;
	}

	@Override
	public boolean equalIterationOrder( final IterableRealInterval< ? > f ) {
		return iterationOrder().equals( f.iterationOrder() );
	}

	@Override
	public Iterator<T> iterator() {
		return fullViewIterable.iterator();
	}

	@Override
	public ImgFactory<T> factory() {
		return source.factory();
	}

	@Override
	public ImgPlus<T> copy() {
		return new HyperSliceImgPlus<T>(source, targetDimension, dimensionPosition);
	}
}

