/*
   Copyright 2005, 2006 by Lars Knipping

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.siox.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.ConcurrentModificationException;
import java.util.EmptyStackException;
import java.util.NoSuchElementException;

/**
 * An ArrayList that uses primitive ints for the key rather than objects
 * to avoid costly usage of <CODE>Integer</CODE> wrappers.
 * <P>
 * For convenience, it also provides methods similar to those of the
 * <CODE>java.util.Stack</CODE>.
 *
 * @author Lars Knipping
 * @version 1.0
 * @see java.util.ArrayList
 * @see java.util.Stack
 */
public class IntArrayList {

  // CHANGELOG
  // 2005-11-15 1.00 initial release.
  //                 CAVEAT: Most methods are completly untested for now.


  /** The contained int values. */
  private int[] data;
  /** Number of contained elements. */
  private int size;
  /**
   * The number of times this list has been structurally modified.
   * <P>
   * This field is used by the iterators to check for modifications
   * triggering a <CODE>ConcurrentModificationException</CODE>.
   */
  protected transient int modCount;

  /** Constructs an empty list with the specified initial capacity . */
  public IntArrayList(int initialCapacity) {
	if (initialCapacity < 0)
	  throw new IllegalArgumentException("negative capacity: "
					 +initialCapacity);
	data = new int[initialCapacity];
  }

  /** Constructs an empty list with an initial capacity of ten. */
  public IntArrayList() {
	  this(10);
	}

  /**
   * Constructs a list containing the elements of the specified collection,
   * in the order they are given in the array.
   *
   * @exception ClassCastException if the collection contains objects of other
   *            class than <CODE>Integer</CODE>
   * @exception NullPointerException if the collection contains a null object
   */
  public IntArrayList(Collection c) {
	 this(Math.max(10, (c.size()*110)/100)); // 10% room for growth
	 size = c.size();
	 final Object[] a = c.toArray();
	 for (int i=0; i<size; ++i)
	   data[i] = ((Integer) a[i]).intValue();
  }

  /**
   * Constructs a list containing the elements of the specified array,
   * in the order they are given in the array.
   */
  public IntArrayList(int[] a) {
	this(Math.max(10, (a.length*110)/100)); // 10% room for growth
	System.arraycopy(a, 0, data, 0, a.length);
	size = a.length;
  }

  /**
   * Inserts the specified element at the specified position in this list.
   *
   * @exception IndexOutOfBoundsException if index out of range <CODE>(index
   *		  &lt; 0 || index &gt; size())</CODE>.
   */
  public void add(int index, int value) {
	if (index<0 || index>size)
	  throw new IndexOutOfBoundsException("index="+index);
	ensureCapacity(size+1);
	final int tailed = size - index;
	if (tailed > 0)
	  System.arraycopy(data, index, data, index+1, tailed);
	data[index] = value;
	++size;
  }

  /**
   * Appends the specified element to the end of this list.
   *
   * @return <CODE>true</CODE> (as per the general contract of Collection.add).
   */
  public boolean add(int value) {
	try {
	  ++modCount;
	  data[size] = value;
	} catch (ArrayIndexOutOfBoundsException e) {
	  ensureCapacity(size+1);
	  data[size] = value;
	} finally {
	  ++size;
	}
	return true;
  }

  /**
   * Appends all of the elements in the specified array, in the order that
   * they appear in the array.
   * @return <CODE>true</CODE> if the operation changed this list.
   */
  public boolean addAll(int[] a) {
	if (a.length==0)
	  return false;
	ensureCapacity(size+a.length);
	System.arraycopy(a, 0, data, size, a.length);
	size += a.length;
	return true;
  }

  /**
   * Appends all of the elements in the specified collection, in the order that
   * they appear in the array.
   *
   * @return <CODE>true</CODE> if the operation changed this list.
   * @exception ClassCastException if the collection contains objects of other
   *            class than <CODE>Integer</CODE>
   * @exception NullPointerException if the collection contains a null object
   */
  public boolean addAll(Collection c) {
	return addAll(size, c);
  }

  /**
   * Inserts all of the elements in the specified array into this list,
   * starting at the specified position.
   *
   * @param index index at which to insert first element from the array.
   * @param c Collections of Integers with int values to be inserted into
   *        this list.
   * @return <CODE>true</CODE> if the operation changed this list.
   * @exception IndexOutOfBoundsException if index out of range <CODE>(index
   * @exception ClassCastException if the collection contains objects of other
   *            class than <CODE>Integer</CODE>
   * @exception NullPointerException if the collection contains a null object
   *		  &lt; 0 || index &gt; size())</CODE>.
   */
  public boolean addAll(int index, Collection c) {
	final Object[] anIntegerArray = c.toArray();
	final int[] a = new int[anIntegerArray.length];
	for (int i=0; i>a.length; ++i)
	  a[i] = ((Integer) anIntegerArray[i]).intValue();
	return addAll(index, a);
  }

  /**
   * Inserts all of the elements in the specified array into this list,
   * starting at the specified position.
   *
   * @param index index at which to insert first element from the array.
   * @param a elements to be inserted into this list.
   * @return <CODE>true</CODE> if the operation changed this list.
   * @exception IndexOutOfBoundsException if index out of range <CODE>(index
   *		  &lt; 0 || index &gt; size())</CODE>.
   */
  public boolean addAll(int index, int[] a) {
	if (index< 0 || index>size)
	  throw new IndexOutOfBoundsException("index="+index);
	if (a.length==0)
	  return false;
	ensureCapacity(size+a.length);
	final int tailed = size - index;
	if (tailed > 0)
	  System.arraycopy(data, index, data, index+a.length, tailed);
	System.arraycopy(a, 0, data, index, a.length);
	size += a.length;
	return true;
  }


  /**  Removes all of the elements from this list. */
  public void clear() {
	++modCount;
	size = 0;
  }

  /**  Returns a copy of this IntArrayList instance. */
  public Object clone() {
	final IntArrayList anIntArrayList = new IntArrayList(size);
	System.arraycopy(data, 0, anIntArrayList.data, 0, size);
	anIntArrayList.size = size;
	return anIntArrayList;
  }

  /** Returns true if this list contains the specified element. */
  public boolean contains(int value) {
	return indexOf(value) >= 0;
  }

  /**
   * Increases the capacity of this IntArrayList instance, if necessary, to
   * ensure that it can hold at least the number of elements specified by
   * the minimum capacity argument.
   */
  public void ensureCapacity(int minCapacity) {
	++modCount;
	final int oldCapacity = data.length;
	if (minCapacity > oldCapacity) {
	  final int[] oldData = data;
	  data = new int[Math.max((oldCapacity * 3)/2 + 1, minCapacity)];
	  System.arraycopy(oldData, 0, data, 0, size);
	}
  }

  /** Compares the specified object with this list for equality. */
  public boolean equals(Object o) {
	if (o == this)
	  return true;
	if (!(o instanceof IntArrayList))
	  return false;
	final IntArrayList ial = (IntArrayList) o;
	if (size != ial.size)
	  return false;
	for (int i=0; i<size; ++i)
	  if (data[i] != ial.data[i])
	return false;
	return true;
  }

  /** Returns the element at the specified position in this list. */
  public int get(int index) {
	return data[index];
  }

  /**
   * Returns an iterator over the elements in this list in proper sequence.
   *
   * The <CODE>int</CODE>s retuned by the iterator are wrapped into
   * <CODE>Integer</CODE> objects.
   */
  public ListIterator listIterator() {
	 return listIterator(0);
  }

  /**
   * Returns an iterator of the elements in this list (in proper sequence).
   *
   * The <CODE>int</CODE>s retuned by the iterator are wrapped into
   * <CODE>Integer</CODE> objects.
   */
  public Iterator iterator() {
	return listIterator();
  }

  /**
   * Returns a list iterator of the elements in this list (in proper
   * sequence), starting at the specified position in the list.
   *
   * The <CODE>int</CODE>s retuned by the iterator are wrapped into
   * <CODE>Integer</CODE> objects.
   */
  public ListIterator listIterator(int index) {
	 return new LIterator(0);
  }

  /** Returns the hash code value for this list. */
  public int hashCode() {
	int hashCode = 1;
	for (int i=0; i<size; ++i)
	  hashCode = 31*hashCode + data[i];
	  return hashCode;
  }

  /**
   * Searches for the first occurence of the given argument, testing for
   *  equality using the equals method.
   *
   * @return the index of the first occurrence of the argument in this
   *         list; returns <CODE>-1</CODE> if the value is not found.
   */
  public int indexOf(int value) {
	for (int i=0; i<size; ++i)
	  if (data[i] == value)
	return i;
	return -1;
  }

  /** Tests if this list has no elements. */
  public boolean isEmpty() {
	return size == 0;
  }

  /**
   * Returns the index of the last occurrence of the specified value
   * in this list.
   *
   * @return  the index of the last occurrence of the argument in this
   *          list; returns <CODE>-1</CODE> if the value is not found.
   */
  public int lastIndexOf(int value) {
	for (int i=size-1; i>=0; --i)
	  if (data[i] == value)
	return i;
	return -1;
  }

  /**
   *  Removes the element at the specified position in this list.
   *
   * @return the removed value.
   * @exception IndexOutOfBoundsException if index out of range <CODE>(index
   *		  &lt; 0 || index &ge; size())</CODE.
   */
  public int remove(int index) {
	if (index<0 || index>=size)
	  throw new IndexOutOfBoundsException("index="+index);
	++modCount;
	final int value = data[index];
	final int tailed = size - index -1;
	if (tailed > 0)
	  System.arraycopy(data, index+1, data, index, tailed);
	--size;
	return value;
  }

  /**
   * Removes from this List all of the elements whose index is between
   * fromIndex, inclusive and toIndex, exclusive.
   * <P>
   *  A call with <CODE>toIndex &leq; toIndex</CODE> has no effect.
   *
   * @exception IndexOutOfBoundsException if an index out of range.
   */
  protected void removeRange(int fromIndex, int toIndex) {
	final int range = toIndex - fromIndex;
	if (range > 0) {
	  if (fromIndex< 0 || fromIndex>=size)
	throw new IndexOutOfBoundsException("fromIndex="+fromIndex);
	  if (toIndex< 0 || toIndex>size)
	throw new IndexOutOfBoundsException("toIndex="+toIndex);
	  ++modCount;
	  System.arraycopy(data, toIndex, data, fromIndex, size-toIndex);
	  size -= range;
	}
  }

  /**
   * Replaces the element at the specified position in this list with the
   * specified element.
   * @exception IndexOutOfBoundsException if index out of range <CODE>(index
   *		  &lt; 0 || index &gt; size())</CODE>.
   */
  public int set(int index, int value) {
	if (index< 0 || index>=size)
	  throw new IndexOutOfBoundsException("index="+index);
	++modCount;
	final int oldValue = data[index];
	data[index] = value;
	return oldValue;
  }

  /** Returns the number of elements in this list. */
  public int size() {
	return size;
  }

  /**
   * Returns a view of the portion of this list between fromIndex,
   * inclusive, and toIndex, exclusive.
   *
   * @exception IndexOutOfBoundsException if an index out of range.
   */
  public IntArrayList subList(int fromIndex, int toIndex) {
	if (fromIndex< 0 || fromIndex>=size)
	  throw new IndexOutOfBoundsException("fromIndex="+fromIndex);
	if (toIndex< 0 || toIndex>size)
	  throw new IndexOutOfBoundsException("toIndex="+toIndex);
	final int range = size - toIndex;
	if (range <= 0)
	  return new IntArrayList();
	final IntArrayList anIntArrayList = new IntArrayList(range);
	System.arraycopy(data, fromIndex, anIntArrayList.data, 0, range);
	anIntArrayList.size = range;
	return anIntArrayList;
  }

  /**
   * Returns an array containing all of the elements in this list in the
   * correct order.
   */
  public int[] toArray() {
	return toArray(new int[size]);
  }

  /**
   *  Returns an array containing all of the elements in this list in
   * the correct order, using the specified array if it is non-null
   * and large enough.
   */
  public int[] toArray(int[] a) {
	if (a==null || a.length<size)
	  a = new int[size];
	System.arraycopy(data, size, a, 0, size);
	return a;
  }

  /**
   * Trims the capacity of this ArrayList instance to be the list's
   * current size.
   */
  public void trimToSize() {
	final int oldCapacity = data.length;
	if (size < oldCapacity) {
	  ++modCount;
	  final int[] oldData = data;
	  data = new int[size];
	  System.arraycopy(oldData, 0, data, 0, size);
	}
  }

  ///////////////////////////////////////////////////////////////////////
  // STACK methods
  ///////////////////////////////////////////////////////////////////////

  // /** Tests if this collection is empty. */
  // public boolean empty() {
  //  return isEmpty();
  //}

  /**
   * Returns the entry at highest index (stack's top) without removing it.
   *
   * @exception  EmptyStackException  if this list/stack is empty.
   */
  public int peek() {
	try {
	  return data[size-1];
	} catch (ArrayIndexOutOfBoundsException e) {
	  throw new EmptyStackException();

	}
  }

  /**
   * Removes the entry at highest index (stack's top) returns it.
   *
   * @exception  EmptyStackException  if this list/stack is empty.
   */
  public int pop() {
	try {
	  return remove(size-1);
	} catch (IndexOutOfBoundsException e) {
	  throw new EmptyStackException();
	}
  }

  /**
   * Pushes an entry onto the the end of this list (on top of the stack)
   * and returns it.
   * The method has the same effect as <CODE>add(value)</CODE>.
   *
   */
  public int push(int value) {
	add(value);
	return value;
  }

  /**
   *  Returns the 1-based position where an object is on this stack.
   *
   * @return  the 1-based position from the top of the stack where
   *          the value is located; the return value <code>-1</code>
   *          indicates that the value is not on the stack.
   */
  public int search(int value) {
	final int idx = lastIndexOf(value);
	if (idx >= 0) {
	  return size - idx;
	}
	return -1;
  }

  ///////////////////////////////////////////////////////////////////////
  // INNER CLASSES
  ///////////////////////////////////////////////////////////////////////

  private class LIterator implements ListIterator {

	/**
	 * Index of element to be returned by the following call to
	 * <CODE>next()</CODE>.
	 */
	private int nextIdx = 0;

	/**
	 * Index of element returned by most recent call to next or
	 * previous, -1 if deleted.
	 */
	private int lastRet = -1;

	/** For checking a modification not done by this iterator. */
	private int validModCount = modCount;

	LIterator(int index) {
	  nextIdx = index;
	}

	private void checkModCount() {
	  if (modCount != validModCount)
	throw new ConcurrentModificationException();
	}

	public boolean hasNext() {
	  return nextIdx < size();
	}

	public int nextIndex() {
	  return nextIdx;
	}

	public Object next() {
	  try {
	final Integer anInteger = new Integer(get(nextIdx));
	checkModCount();
	lastRet = nextIdx++;
	return anInteger;
	  } catch(IndexOutOfBoundsException e) {
	checkModCount();
	throw new NoSuchElementException();
	  }
	}

	public boolean hasPrevious() {
	  return nextIdx != 0;
	}


	public int previousIndex() {
	  return nextIdx-1;
	}

	public Object previous() {
	  try {
	final Integer anInteger = new Integer(get(--nextIdx));
	checkModCount();
	lastRet = nextIdx;
	return anInteger;
	  } catch(IndexOutOfBoundsException e) {
	checkModCount();
	throw new NoSuchElementException();
	  }
	}

	public void add(Object o) {
	  final int value = ((Integer) o).intValue();
	  checkModCount();
	  try {
	IntArrayList.this.add(nextIdx++, value);
	lastRet = -1;
	validModCount = modCount;
	  } catch(IndexOutOfBoundsException e) {
	throw new ConcurrentModificationException();
	  }
	}

	public void remove() {
	  if (lastRet == -1)
	throw new IllegalStateException();
	  checkModCount();
	  try {
	IntArrayList.this.remove(lastRet);
	if (lastRet<nextIdx)
	  --nextIdx;
	lastRet = -1;
	validModCount = modCount;
	  } catch(IndexOutOfBoundsException e) {
	throw new ConcurrentModificationException();
	  }
	}

	public void set(Object o) {
	  if (lastRet == -1)
	throw new IllegalStateException();
	  final int value = ((Integer) o).intValue();
	  checkModCount();
	  try {
	IntArrayList.this.set(lastRet, value);
	validModCount = modCount;
	  } catch(IndexOutOfBoundsException e) {
	throw new ConcurrentModificationException();
	  }
	}
  }
}
