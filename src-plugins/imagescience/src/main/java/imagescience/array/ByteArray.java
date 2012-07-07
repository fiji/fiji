package imagescience.array;

import java.util.NoSuchElementException;

/** A dynamic array of {@code byte} numbers. Provides more flexibility than {@code byte[]} objects and more efficiency than {@code java.util.Vector} objects. */
public class ByteArray {
	
	private int capacity = 100;
	private int increment = 100;
	private byte[] bytes = null;
	private int length = 0;
	
	/** Default constructor. Results in a new array of zero length but with an initial capacity and capacity increment of 100 elements. */
	public ByteArray() {
		
		bytes = new byte[capacity];
	}
	
	/** Length constructor. Results in a new array of given length. The capacity of the array is set to the same value, with a capacity increment of 100 elements.
		
		@param length the length (and capacity) of the new array.
		
		@exception IllegalArgumentException if {@code length} is less than {@code 0}.
	*/
	public ByteArray(final int length) {
		
		if (length < 0) throw new IllegalArgumentException("Length less than 0");
		bytes = new byte[length];
		this.length = length;
		capacity = length;
	}
	
	/** Capacity constructor. Results in a new array of zero length but with given initial capacity and capacity increment.
		
		@param capacity the capacity of the new array.
		
		@param increment the capacity increment of the new array.
		
		@exception IllegalArgumentException if {@code capacity} or {@code increment} is less than {@code 0}.
	*/
	public ByteArray(final int capacity, final int increment) {
		
		if (capacity < 0) throw new IllegalArgumentException("Initial capacity less than 0");
		if (increment < 0) throw new IllegalArgumentException("Capacity increment less than 0");
		bytes = new byte[capacity];
		this.capacity = capacity;
		this.increment = increment;
	}
	
	/** Array constructor. Results in a new array that uses the given array as initial internal array. The length and capacity of the new array are both set to the length of the given array. The capacity increment is 100 elements.
		
		@param array the array initially used as internal array.
		
		@exception NullPointerException if {@code array} is {@code null}.
	*/
	public ByteArray(final byte[] array) {
		
		bytes = array;
		length = array.length;
		capacity = length;
	}
	
	/** Returns a handle to the internal array. The same as method {@link #get()}.
		
		@return a handle to the internal array. The length of the returned array is equal to the value returned by {@link #capacity()}. By first calling {@link #trim()}, the length of the returned array will be equal to the actual number of elements in the array, that is the value returned by {@link #length()}.
	*/
	public byte[] array() { return bytes; }
	
	/** Returns the number of elements in the array. The same as method {@link #size()}.
		
		@return the number of elements in the array.
	*/
	public int length() { return length; }
	
	/** Sets the length of the array to the given length. The same as method {@link #size(int)}.
		
		@param length the new length of the array. If the value of this parameter is less than the current length, the current length is simply set to the given length, without changing the capacity of the array. If it is larger than the current length, the capacity of the array is adjusted as necessary. The capacity increment is retained.
		
		@exception IllegalArgumentException if {@code length} is less than {@code 0}.
	*/
	public void length(final int length) {
		
		if (length < 0) throw new IllegalArgumentException("Length less than 0");
		else if (length <= capacity) this.length = length;
		else {
			final byte[] a = new byte[length];
			for (int i=0; i<this.length; ++i) a[i] = bytes[i];
			bytes = a; this.length = capacity = length;
		}
	}
	
	/** Returns the number of elements in the array. The same as method {@link #length()}.
		
		@return the number of elements in the array.
	*/
	public int size() { return length; }
	
	/** Sets the length of the array to the given length. The same as method {@link #length(int)}.
		
		@param length the new length of the array. If the value of this parameter is less than the current length, the current length is simply set to the given length, without changing the capacity of the array. If it is larger than the current length, the capacity of the array is adjusted as necessary. The capacity increment is retained.
		
		@exception IllegalArgumentException if {@code length} is less than {@code 0}.
	*/
	public void size(final int length) { length(length); }
	
	/** Indicates whether this array has no elements.
		
		@return {@code true} if the array has no elements, that is if the length of the array is {@code 0}; {@code false} otherwise.
	*/
	public boolean empty() { return (length == 0); }
	
	/** Returns the capacity of the array.
		
		@return the capacity of the array.
	*/
	public int capacity() { return capacity; }
	
	/** Returns the capacity increment of the array.
		
		@return the capacity increment of the array.
	*/
	public int increment() { return increment; }
	
	/** Sets the capacity increment of the array.
		
		@param increment the new capacity increment of the array.
		
		@exception IllegalArgumentException if {@code increment} is less than {@code 0}.
	*/
	public void increment(final int increment) {
		
		if (increment < 0) throw new IllegalArgumentException("Capacity increment less than 0");
		this.increment = increment;
	}
	
	/** Returns a handle to the internal array. The same as method {@link #array()}.
		
		@return a handle to the internal array. The length of the returned array is equal to the value returned by {@link #capacity()}. By first calling {@link #trim()}, the length of the returned array will be equal to the actual number of elements in the array, that is the value returned by {@link #length()}.
	*/
	public byte[] get() { return bytes; }
	
	/** Returns the element at the given index in the array.
		
		@param index the index.
		
		@return the element at the given index in the array.
		
		@exception ArrayIndexOutOfBoundsException if {@code index} is less than {@code 0} or larger than or equal to the length of the array.
	*/
	public byte get(final int index) {
		
		if (index < 0 || index >= length) throw new ArrayIndexOutOfBoundsException();
		return bytes[index];
	}
	
	/** Returns the first element of the array.
		
		@return the first element of the array.
		
		@exception NoSuchElementException if the length of the array is	{@code 0}.
	*/
	public byte first() {
		
		if (length == 0) throw new NoSuchElementException();
		return bytes[0];
	}
	
	/** Returns the last element of the array.
		
		@return the last element of the array.
		
		@exception NoSuchElementException if the length of the array is	{@code 0}.
	*/
	public byte last() {
		
		if (length == 0) throw new NoSuchElementException();
		return bytes[length-1];
	}
	
	/** Appends the array with the given {@code byte}. The same as method {@link #append(byte)}.
		
		@param b the {@code byte} to be appended to the array.
		
		@exception IllegalStateException if the length of the array is equal to the capacity, and the capacity increment is {@code 0}.
	*/
	public void add(final byte b) {
		
		if (length == capacity) inccap();
		bytes[length++] = b;
	}
	
	/** Appends the array with the given {@code byte}. The same as method {@link #add(byte)}.
		
		@param b the {@code byte} to be appended to the array.
		
		@exception IllegalStateException if the length of the array is equal to the capacity, and the capacity increment is {@code 0}.
	*/
	public void append(final byte b) {
		
		if (length == capacity) inccap();
		bytes[length++] = b;
	}
	
	/** Inserts the given {@code byte} at the given index in the array.
		
		@param b the {@code byte} to be inserted in the array.
		
		@param index the index at which {@code b} is inserted. The indices of the elements originally at this index and higher are increased by {@code 1}.
		
		@exception ArrayIndexOutOfBoundsException if {@code index} is less than {@code 0} or larger than or equal to the length of the array.
		
		@exception IllegalStateException if the length of the array is equal to the capacity, and the capacity increment is {@code 0}.
	*/
	public void insert(final byte b, final int index) {
		
		if (index < 0 || index >= length) throw new ArrayIndexOutOfBoundsException();
		if (length == capacity) inccap();
		for (int i=length; i>index; --i) bytes[i] = bytes[i-1];
		bytes[index] = b;
		++length;
	}
	
	/** Replaces the element at the given index in the array by the given {@code byte}.
		
		@param b the {@code byte} to be placed in the array.
		
		@param index the index at which {@code b} is to be placed.
		
		@exception ArrayIndexOutOfBoundsException if {@code index} is less than {@code 0} or larger than or equal to the length of the array.
	*/
	public void set(final byte b, final int index) {
		
		if (index < 0 || index >= length) throw new ArrayIndexOutOfBoundsException();
		bytes[index] = b;
	}
	
	/** Sets the internal array to the given array.
		
		@param array the array to which the internal array is to be set. The length and capacity of the array are both set to the length of the given array. The capacity increment is retained.
		
		@exception NullPointerException if {@code array} is {@code null}.
	*/
	public void set(final byte[] array) {
		
		bytes = array;
		length = array.length;
		capacity = length;
	}
	
	/** Removes all elements. The same as method {@link #clear()}. The length of the array is set to {@code 0} but the capacity and capacity increment are retained. */
	public void reset() { length = 0; }
	
	/** Removes all elements. The same as method {@link #reset()}. The length of the array is set to {@code 0} but the capacity and capacity increment are retained. */
	public void clear() { length = 0; }
	
	/** Trims the capacity of the array down to the length of the array. */
	public void trim() {
		
		final byte[] a = new byte[length];
		for (int i=0; i<length; ++i) a[i] = bytes[i];
		bytes = a; this.length = capacity = length;
	}
	
	/** Removes the element at the given index from the array.
		
		@param index the index whose element is to be removed from the array. The indices of the elements at the next index and higher are decreased by {@code 1}.
		
		@exception ArrayIndexOutOfBoundsException if {@code index} is less than {@code 0} or larger than or equal to the length of the array.
	*/
	public void remove(final int index) {
		
		if (index < 0 || index >= length) throw new ArrayIndexOutOfBoundsException();
		for (int i=index+1; i<length; ++i) bytes[i-1] = bytes[i];
		--length;
	}
	
	/** Duplicates the array.
		
		@return a new {@code ByteArray} object that is an exact copy of this object. All information is copied and no memory is shared between this and the returned object.
	*/
	public ByteArray duplicate() {
		
		final ByteArray a = new ByteArray(capacity,increment);
		for (int i=0; i<length; ++i) a.bytes[i] = bytes[i];
		a.length = length;
		return a;
	}
	
	/** Ensures that the capacity of the array is at least the given capacity.
		
		@param capacity the minimum capacity that the array is ensured to have.
	*/
	public void ensure(final int capacity) {
		
		if (this.capacity < capacity) {
			this.capacity = capacity;
			final byte[] a = new byte[capacity];
			for (int i=0; i<length; ++i) a[i] = bytes[i];
			bytes = a;
		}
	}
	
	/** Indicates whether this array contains the same data as the given array.
		
		@param array the array to compare this array with.
	
		@return {@code true} if {@code array} is not {@code null}, has the same length as this array, and each element of {@code array} has the exact same value as the corresponding element of this array; {@code false} if this is not the case.
	*/
	public boolean equals(final ByteArray array) {
		
		if (array != null) {
			if (array.length == length) {
				for (int i=0; i<length; ++i) {
					if (array.bytes[i] != bytes[i])
						return false;
				}
				return true;
			}
		}
		return false;
	}
	
	private void inccap() {
		
		if (increment == 0) throw new IllegalStateException("Capacity increment is 0");
		capacity += increment;
		final byte[] a = new byte[capacity];
		for (int i=0; i<length; ++i) a[i] = bytes[i];
		bytes = a;
	}
	
}
