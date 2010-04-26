package vib;

public class IntArray extends ArrayBase
{
    protected int[] baseArray;
    
    public IntArray(int size, int growth) {
        super(size, growth, Integer.TYPE);
    }

    public IntArray(int size) {
        super(size, Integer.TYPE);
    }

    public IntArray() {
	super(0, Integer.TYPE);
    }

    // Implementation of callout to get the underlying array.
    protected Object getArray() {
        return baseArray;
    }

    // Implementation of callout to set the underlying array.
    protected void setArray(Object array) {
        baseArray = (int[]) array;
    }

    // Implementation of callout to initialize a portion of the array.
    protected void discardValues(int from, int to) {
        for (int i = from; i < to; i++) {
            baseArray[i] = 0;
        }
    }

    // Append a value to the collection.
    public int add(int value) {
        int index = getAddIndex();
        baseArray[index] = value;
        return index;
    }

    // Insert a value into the collection.
    public void add(int index, int value) {
        makeInsertSpace(index);
        baseArray[index] = value;
    }

    // Get value from the collection.
    public int get(int index) {
        if (index < countPresent) {
            return baseArray[index];
        } else {
            throw new ArrayIndexOutOfBoundsException("Invalid index value");
        }
    }

    // Set the value at a position in the collection.
    public void set(int index, int value) {
        if (index < countPresent) {
            baseArray[index] = value;
        } else {
            throw new ArrayIndexOutOfBoundsException("Invalid index value");
        }
    }

    public boolean contains(int value) {
	    for (int i = 0; i < countPresent; i++)
		    if (baseArray[i] == value)
			    return true;
	    return false;
    }

    // Convert to an array.
    public int[] buildArray() {
        return (int[]) buildArray(Integer.TYPE);
    }
}


