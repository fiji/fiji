package fiji.build;

import java.util.Iterator;

/**
 * An aggregator for multiple iterators
 */
public class MultiIterable<T> implements Iterable<T> {
	protected Iterable<T>[] list;

	public MultiIterable(Iterable<T>... list) {
		this.list = list;
	}

	protected class MultiIterator implements Iterator<T> {
		protected int index;
		protected Iterator<T> iterator;

		public MultiIterator() {
			index = 0;
			iterator = list[0].iterator();
			if (!iterator.hasNext())
				findNext();
		}

		public boolean hasNext() {
			return index < list.length && iterator != null;
		}

		public T next() {
			T result = iterator.next();
			findNext();
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		protected void findNext() {
			while (!iterator.hasNext())
				if (++index < list.length)
					iterator = list[index].iterator();
				else {
					iterator = null;
					return;
				}
		}
	}

	public Iterator<T> iterator() {
		return new MultiIterator();
	}
}
