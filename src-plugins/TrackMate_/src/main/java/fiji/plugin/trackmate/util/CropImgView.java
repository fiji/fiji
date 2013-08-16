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
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.Type;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * A simple wrapper that exploits {@link Views} to return a cropped view of a source {@link Img}
 * as an {@link Img}.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Sep 2012
 */
public class CropImgView<T  extends Type<T>> implements Img<T> {

	protected final IntervalView<T> interval;
	protected final IterableInterval<T> iterable;
	protected final RandomAccessible<T> img;
	protected final long[] min;
	protected final long[] max;
	private final ImgFactory<T> factory;

	public CropImgView(final RandomAccessible<T> source, final long[] min, final long[] max, ImgFactory<T> factory) {
		this.factory = factory;
		this.interval = Views.interval(source, min, max);
		this.iterable = Views.iterable(interval);
		this.img = source;
		this.min = min;
		this.max = max;
	}
	
	public CropImgView(final Img<T> source, final long[] min, final long[] max) {
		this(source, min, max, source.factory());
	}

	@Override
	public RandomAccess<T> randomAccess() {
		return interval.randomAccess();
	}

	@Override
	public RandomAccess<T> randomAccess(Interval ival) {
		return interval.randomAccess(ival);
	}

	@Override
	public int numDimensions() {
		return interval.numDimensions();
	}

	@Override
	public long min(int d) {
		return interval.min(d);
	}

	@Override
	public void min(long[] min) {
		interval.min(min);
	}

	@Override
	public void min(Positionable min) {
		interval.min(min);
	}

	@Override
	public long max(int d) {
		return interval.max(d);
	}

	@Override
	public void max(long[] max) {
		interval.max(max);
	}

	@Override
	public void max(Positionable max) {
		interval.max(max);
	}

	@Override
	public double realMin(int d) {
		return interval.realMin(d);
	}

	@Override
	public void realMin(double[] min) {
		interval.realMin(min);
	}

	@Override
	public void realMin(RealPositionable min) {
		interval.realMin(min);
	}

	@Override
	public double realMax(int d) {
		return interval.realMax(d);
	}

	@Override
	public void realMax(double[] max) {
		interval.realMax(max);
	}

	@Override
	public void realMax(RealPositionable max) {
		interval.realMax(max);
	}

	@Override
	public void dimensions(long[] dimensions) {
		interval.dimensions(dimensions);
	}

	@Override
	public long dimension(int d) {
		return interval.dimension(d);
	}

	@Override
	public Cursor<T> cursor() {
		return iterable.cursor();
	}

	@Override
	public Cursor<T> localizingCursor() {
		return iterable.localizingCursor();
	}

	@Override
	public long size() {
		return iterable.size();
	}

	@Override
	public T firstElement() {
		return iterable.firstElement();
	}

	@Override
	public Object iterationOrder() {
		return iterable.iterationOrder();
	}

	@Override
	@Deprecated
	public boolean equalIterationOrder(IterableRealInterval<?> f) {
		return iterable.equalIterationOrder(f);
	}

	@Override
	public Iterator<T> iterator() {
		return iterable.iterator();
	}

	@Override
	public ImgFactory<T> factory() {
		return factory;
	}

	@Override
	public Img<T> copy() {
		final Img< T > copy = factory.create( this, iterable.firstElement().createVariable() );

		Cursor< T > srcCursor = localizingCursor();
		RandomAccess< T > resAccess = copy.randomAccess();

		while ( srcCursor.hasNext() )
		{
			srcCursor.fwd();
			resAccess.setPosition( srcCursor );
			resAccess.get().set( srcCursor.get() );
		}

		return copy;
	}

}
