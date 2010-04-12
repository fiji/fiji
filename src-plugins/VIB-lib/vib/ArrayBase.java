package vib;

import java.lang.reflect.Array;

public abstract class ArrayBase
{

    protected int countPresent;
    protected int countLimit;
    protected int maximumGrowth;

    public ArrayBase(int size, int growth, Class type) {
        Object array = Array.newInstance(type, size);
        countLimit = size;
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
            countLimit + Math.min(countLimit, maximumGrowth));
        Class type = base.getClass().getComponentType();
        Object grown = Array.newInstance(type, size);
        System.arraycopy(base, 0, grown, 0, countLimit);
        countLimit = size;
        setArray(grown);
    }

    // Get next add position for appending, increasing size if needed.
    protected int getAddIndex() {
        int index = countPresent++;
        if (countPresent > countLimit) {
            growArray(countPresent);
        }
        return index;
    }

    // Make room to insert a value at a specified index.
    protected void makeInsertSpace(int index) {
        if (index >= 0 && index <= countPresent) {
            if (++countPresent > countLimit) {
                growArray(countPresent);
            }
            if (index < countPresent - 1) {
                Object array = getArray();
                System.arraycopy(array, index, array, index + 1,
                    countPresent - index - 1);
            }
        } else {
            throw new ArrayIndexOutOfBoundsException("Invalid index value");
        }
    }

    // Remove a value from the collection.
    public void remove(int index) {
        if (index >= 0 && index < countPresent) {
            if (index < --countPresent){
                Object base = getArray();
                System.arraycopy(base, index + 1, base, index,
                    countPresent - index);
                discardValues(countPresent, countPresent + 1);
            }
        } else {
            throw new ArrayIndexOutOfBoundsException("Invalid index value");
        }
    }

    // Make sure we have at least a specified capacity.
    public void ensureCapacity(int min) {
        if (min > countLimit) {
            growArray(min);
        }
    }

    // Set the collection empty.
    public void clear() {
        setSize(0);
    }

    // Get number of values in collection.
    public int size() {
        return countPresent;
    }

    // Set the size of the collection.
    public void setSize(int count) {
        if (count > countLimit) {
            growArray(count);
        } else if (count < countPresent) {
            discardValues(count, countPresent);
        }
        countPresent = count;
    }

    // Convert to an array of specified type.
    protected Object buildArray(Class type) {
        Object copy = Array.newInstance(type, countPresent);
        System.arraycopy(getArray(), 0, copy, 0, countPresent);
        return copy;
    }
}

