package mpicbg.imglib.cursor.special;

import java.util.Iterator;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.predicate.MaskPredicate;
import mpicbg.imglib.cursor.special.predicate.Predicate;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.logic.BitType;

public class TwinCursor<T extends Type<T>> implements Cursor<T> {
		final protected PredicateCursor<BitType> mask;
		final protected LocalizableByDimCursor<T> channel1;
		final protected LocalizableByDimCursor<T> channel2;
		/*
		 * For performance, we keep one position array (to avoid 
		 * having to create a new array in every single step).
		 */
		final protected int[] position;
		/* To avoid calling next() too often */
		protected boolean gotNext;

		public TwinCursor(final LocalizableByDimCursor<T> channel1,
				final LocalizableByDimCursor<T> channel2,
				final LocalizableCursor<BitType> mask) {
			final Predicate<BitType> predicate = new MaskPredicate();
			this.mask = new PredicateCursor<BitType>(mask, predicate);
			this.channel1 = channel1;
			this.channel2 = channel2;
			position = mask.getPosition();
		}

		final public boolean hasNext() {
			gotNext = false;
			return mask.hasNext();
		}

		final public void getNext() {
			if (gotNext)
				return;
			mask.next();
			mask.getPosition(position);
			channel1.setPosition(position);
			channel2.setPosition(position);
			gotNext = true;
		}

		final public T getChannel1() {
			getNext();
			return channel1.getType();
		}

		final public T getChannel2() {
			getNext();
			return channel2.getType();
		}

		@Override
		public void reset() {
			gotNext = false;
			mask.reset();
		}

		@Override
		public void fwd() {
			if (hasNext())
				getNext();
		}

		@Override
		public void fwd(long arg0) {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public T next() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public Iterator<T> iterator() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public int[] getDimensions() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public void getDimensions(int[] arg0) {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public int getNumDimensions() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public void close() {
			mask.close();
			channel1.close();
			channel2.close();
		}

		@Override
		public int[] createPositionArray() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public int getArrayIndex() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public Image<T> getImage() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public Container<T> getStorageContainer() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public int getStorageIndex() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public T getType() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public boolean isActive() {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}

		@Override
		public void setDebug(boolean arg0) {
			throw new UnsupportedOperationException("This method has not been implemented, yet.");
		}
	}