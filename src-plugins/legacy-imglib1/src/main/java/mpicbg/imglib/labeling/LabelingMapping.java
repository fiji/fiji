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

package mpicbg.imglib.labeling;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * The LabelingMapping maps a set of labelings of a pixel to an index
 * value which can be more compactly stored than the set of labelings.
 * The service it provides is an "intern" function that supplies a
 * canonical object for each set of labelings in a container.
 * 
 * For example, say pixels are labeled with strings and a particular
 * pixel is labeled as belonging to both "Foo" and "Bar" and this
 * is the first label assigned to the container. The caller will ask
 * for the index of { "Foo", "Bar" } and get back the number, "1".
 * LabelingMapping will work faster if the caller first interns
 * { "Foo", "Bar" } and then requests the mapping of the returned object.
 *  
 * @param <T>
 * @param <N>
 *
 * @author Lee Kamentsky
 */
class LabelingMapping<T extends Comparable<T>, N extends Number> {
	Constructor<N> constructor;
	N instance;
	@SuppressWarnings("unchecked")
	public LabelingMapping(N srcInstance) {
		instance = srcInstance;
		Class<? extends Number> c = srcInstance.getClass();
		try {
			constructor = (Constructor<N>) c.getConstructor(new Class [] { String.class });
		} catch (SecurityException e) {
			e.printStackTrace();
			throw new AssertionError(e.getMessage());
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			throw new AssertionError("Number class cannot be constructed from a string");
		}
		List<T> background = Collections.emptyList(); 
		intern(background);
	}
	private static class InternedList<T1 extends Comparable<T1>, N extends Number> implements List<T1>
	{
		private final List<T1> value;
		final N index;
		final LabelingMapping<T1,N> owner;
		public InternedList(List<T1> src, N index, LabelingMapping<T1,N> owner) {
			value = Collections.unmodifiableList(src);
			this.index = index;
			this.owner = owner;
		}

		@Override
		public int size() {
			return value.size();
		}

		@Override
		public boolean isEmpty() {
			return value.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return value.contains(o);
		}

		@Override
		public Iterator<T1> iterator() {
			return value.iterator();
		}

		@Override
		public Object[] toArray() {
			return value.toArray();
		}

		@Override
		public boolean add(T1 e) {
			return value.add(e);
		}

		@Override
		public boolean remove(Object o) {
			return value.remove(o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return value.containsAll(c);
		}

		@Override
		public boolean addAll(Collection<? extends T1> c) {
			return value.addAll(c);
		}

		@Override
		public boolean addAll(int index, Collection<? extends T1> c) {
			return value.addAll(index, c);
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return value.removeAll(c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return value.retainAll(c);
		}

		@Override
		public void clear() {
			value.clear();
		}

		@Override
		public T1 get(int index) {
			return value.get(index);
		}

		@Override
		public T1 set(int index, T1 element) {
			return value.set(index, element);
		}

		@Override
		public void add(int index, T1 element) {
			value.add(index, element);
		}

		@Override
		public T1 remove(int index) {
			return value.remove(index);
		}

		@Override
		public int indexOf(Object o) {
			return value.indexOf(o);
		}

		@Override
		public int lastIndexOf(Object o) {
			return value.lastIndexOf(o);
		}

		@Override
		public ListIterator<T1> listIterator() {
			return value.listIterator();
		}

		@Override
		public ListIterator<T1> listIterator(int index) {
			return value.listIterator(index);
		}

		@Override
		public List<T1> subList(int fromIndex, int toIndex) {
			return value.subList(fromIndex, toIndex);
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return value.toArray(a);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return value.hashCode();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof InternedList) {
				@SuppressWarnings("rawtypes")
				InternedList iobj = (InternedList)obj;
				return value.equals(iobj.value);
			}
			return value.equals(obj);
		}
	}

	protected Map<List<T>, InternedList<T, N>> internedLists = 
		new HashMap<List<T>, InternedList<T, N>>();
	protected ArrayList<InternedList<T,N>> listsByIndex = 
		new ArrayList<InternedList<T, N>>();
	
	/**
	 * Return the canonical list for the given list
	 * 
	 * @param src
	 * @return
	 */
	public List<T> intern(List<T> src) {
		return internImpl(src);
	}
	
	@SuppressWarnings("unchecked")
	private InternedList<T,N> internImpl(List<T> src) {
		InternedList<T,N> interned;
		if (src instanceof InternedList) {
			interned = (InternedList<T,N>)src;
			if (interned.owner == this)
				return interned;
		}
		List<T> copy = new ArrayList<T>(src);
		Collections.sort(copy);
		interned = internedLists.get(copy);
		if (interned == null) {
			int intIndex = listsByIndex.size();
			N index;
			try {
				index = constructor.newInstance(Integer.toString(intIndex));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				throw new AssertionError(e.getMessage());
			} catch (InstantiationException e) {
				e.printStackTrace();
				throw new AssertionError(e.getMessage());
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new AssertionError(e.getMessage());
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				if (e.getTargetException() instanceof NumberFormatException) {
					throw new AssertionError(String.format("Too many labels (or types of multiply-labeled pixels): %d maximum", intIndex));
				}
				throw new AssertionError(e.getMessage());
			}
			interned = new InternedList<T, N>(src, index, this);
			listsByIndex.add(interned);
			internedLists.put(interned, interned);
		}
		return interned;
	}

	public List<T> intern(T [] src) {
		return intern(Arrays.asList(src));
	}
	public N indexOf(List<T> key) {
		InternedList<T,N> interned = internImpl(key); 
		return interned.index;
	}
	public N indexOf(T [] key) {
		return indexOf(intern(key));
	}
	
	public List<T> listAtIndex(int index) {
		return listsByIndex.get(index);
	}
	/**
	 * @return the labels defined in the mapping.
	 */
	public List<T> getLabels() {
		HashSet<T> result = new HashSet<T>();
		for (InternedList<T,N> instance: listsByIndex) {
			for (T label: instance) {
				result.add(label);
			}
		}
		return new ArrayList<T>(result);
	}
}
