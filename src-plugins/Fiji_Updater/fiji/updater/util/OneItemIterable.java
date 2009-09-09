package fiji.updater.util;

import java.util.Iterator;

public class OneItemIterable<T> implements Iterable<T> {
	T justOne;

	public OneItemIterable(T justOne) {
		this.justOne = justOne;
	}

	public Iterator<T> iterator() {
		return new Iterator<T>() {
			boolean isFirst = true;

			public boolean hasNext() { return isFirst; }

			public T next() {
				isFirst = false;
				return justOne;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
