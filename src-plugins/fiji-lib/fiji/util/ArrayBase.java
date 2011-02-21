package fiji.util;

import java.lang.reflect.Array;

public abstract class ArrayBase<ArrayType>
{
	protected Class type;
	protected int actualSize;
	protected int allocated;
	protected int maximumGrowth;

	public ArrayBase(int size, int growth, Class type) {
		this.type = type;
		ArrayType array = (ArrayType)Array.newInstance(type, size);
		allocated = size;
		maximumGrowth = growth;
		setArray(array);
	}

	public ArrayBase(int size, Class type) {
		this(size, Integer.MAX_VALUE, type);
	}
	protected abstract ArrayType getArray();

	protected abstract void setArray(ArrayType array);

	/// Make sure we have at least a specified capacity.
	public void ensureCapacity(int required) {
		if (required <= actualSize)
			return;
		if (required <= allocated) {
			actualSize = required;
			return;
		}
		ArrayType base = getArray();
		int size = Math.max(required, allocated + Math.min(allocated, maximumGrowth));
		ArrayType grown = (ArrayType)Array.newInstance(type, size);
		if (actualSize > 0)
			System.arraycopy(base, 0, grown, 0, actualSize);
		allocated = size;
		actualSize = required;
		setArray(grown);
	}

	/// Get next add position for appending, increasing size if needed.
	protected int getAddIndex() {
		ensureCapacity(actualSize + 1);
		return actualSize;
	}

	/// Make room to insert a value at a specified index.
	protected void makeInsertSpace(int index) {
		if (index < 0)
			throw new ArrayIndexOutOfBoundsException("Invalid index value");
		ensureCapacity(actualSize + 1);
		if (index < actualSize) {
			ArrayType array = getArray();
			System.arraycopy(array, index, array, index + 1, actualSize - index - 1);
		}
	}

	/// Remove a value from the collection.
	public void remove(int index) {
		if (index < 0 || index >= actualSize)
			throw new ArrayIndexOutOfBoundsException("Invalid index value: " + index);
		if (index < --actualSize) {
			ArrayType array = getArray();
			System.arraycopy(array, index + 1, array, index, actualSize - index);
		}
	}

	/// Make the collection empty.
	public void clear() {
		setSize(0);
	}

	/// Get number of values in collection.
	public int size() {
		return actualSize;
	}

	/// Set the size of the collection.
	public void setSize(int count) {
		if (count > allocated)
			ensureCapacity(count);
		actualSize = count;
	}

	/// Convert to an array of specified type.
	public ArrayType buildArray() {
		ArrayType copy = (ArrayType)Array.newInstance(type, actualSize);
		System.arraycopy(getArray(), 0, copy, 0, actualSize);
		return copy;
	}
}