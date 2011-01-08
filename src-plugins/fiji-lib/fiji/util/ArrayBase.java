package fiji.util;

import java.lang.reflect.Array;

public abstract class ArrayBase
{
    protected int actualSize;
    protected int allocated;
    protected int maximumGrowth;

    public ArrayBase(int size, int growth, Class type) {
        Object array = Array.newInstance(type, size);
        allocated = size;
        maximumGrowth = growth;
        setArray(array);
    }

    public ArrayBase(int size, Class type) {
        this(size, Integer.MAX_VALUE, type);
    }
    protected abstract Object getArray();

    protected abstract void setArray(Object array);

    protected abstract void discardValues(int from, int to);

    // Implementation method to increase the underlying array size.
    protected void growArray(int required) {
        Object base = getArray();
        int size = Math.max(required,
            allocated + Math.min(allocated, maximumGrowth));
        Class type = base.getClass().getComponentType();
        Object grown = Array.newInstance(type, size);
        System.arraycopy(base, 0, grown, 0, allocated);
        allocated = size;
        setArray(grown);
    }

    // Get next add position for appending, increasing size if needed.
    protected int getAddIndex() {
        int index = actualSize++;
        if (actualSize > allocated) {
            growArray(actualSize);
        }
        return index;
    }

    // Make room to insert a value at a specified index.
    protected void makeInsertSpace(int index) {
        if (index >= 0 && index <= actualSize) {
            if (++actualSize > allocated) {
                growArray(actualSize);
            }
            if (index < actualSize - 1) {
                Object array = getArray();
                System.arraycopy(array, index, array, index + 1,
                    actualSize - index - 1);
            }
        } else {
            throw new ArrayIndexOutOfBoundsException("Invalid index value");
        }
    }

    // Remove a value from the collection.
    public void remove(int index) {
        if (index >= 0 && index < actualSize) {
            if (index < --actualSize){
                Object base = getArray();
                System.arraycopy(base, index + 1, base, index,
                    actualSize - index);
                discardValues(actualSize, actualSize + 1);
            }
        } else {
            throw new ArrayIndexOutOfBoundsException("Invalid index value");
        }
    }

    // Make sure we have at least a specified capacity.
    public void ensureCapacity(int min) {
        if (min > allocated) {
            growArray(min);
        }
    }

    // Set the collection empty.
    public void clear() {
        setSize(0);
    }

    // Get number of values in collection.
    public int size() {
        return actualSize;
    }

    // Set the size of the collection.
    public void setSize(int count) {
        if (count > allocated) {
            growArray(count);
        } else if (count < actualSize) {
            discardValues(count, actualSize);
        }
        actualSize = count;
    }

    // Convert to an array of specified type.
    protected Object buildArray(Class type) {
        Object copy = Array.newInstance(type, actualSize);
        System.arraycopy(getArray(), 0, copy, 0, actualSize);
        return copy;
    }
}

