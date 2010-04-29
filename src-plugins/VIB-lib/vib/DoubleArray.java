package vib;

public class DoubleArray extends ArrayBase
{
    protected double[] baseArray;
    
    public DoubleArray(int size, int growth) {
        super(size, growth, Double.TYPE);
    }

    public DoubleArray(int size) {
        super(size, Double.TYPE);
    }

    public DoubleArray() {
	super(0, Double.TYPE);
    }

    // Implementation of callout to get the underlying array.
    protected Object getArray() {
        return baseArray;
    }

    // Implementation of callout to set the underlying array.
    protected void setArray(Object array) {
        baseArray = (double[]) array;
    }

    // Implementation of callout to initialize a portion of the array.
    protected void discardValues(int from, int to) {
        for (int i = from; i < to; i++) {
            baseArray[i] = 0;
        }
    }

    // Append a value to the collection.
    public int add(double value) {
        int index = getAddIndex();
        baseArray[index] = value;
        return index;
    }

    // Insert a value into the collection.
    public void add(int index, double value) {
        makeInsertSpace(index);
        baseArray[index] = value;
    }

    // Get value from the collection.
    public double get(int index) {
        if (index < countPresent) {
            return baseArray[index];
        } else {
            throw new ArrayIndexOutOfBoundsException("Invalid index value");
        }
    }

    // Set the value at a position in the collection.
    public void set(int index, double value) {
        if (index < countPresent) {
            baseArray[index] = value;
        } else {
            throw new ArrayIndexOutOfBoundsException("Invalid index value");
        }
    }

    public boolean contains(double value) {
	    for (int i = 0; i < countPresent; i++)
		    if (baseArray[i] == value)
			    return true;
	    return false;
    }

    // Convert to an array.
    public double[] buildArray() {
        return (double[]) buildArray(Double.TYPE);
    }
}


