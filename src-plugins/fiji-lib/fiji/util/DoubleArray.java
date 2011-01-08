package fiji.util;

public class DoubleArray extends ArrayBase<double[]>
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
	@Override
	protected double[] getArray() {
		return baseArray;
	}

	// Implementation of callout to set the underlying array.
	@Override
	protected void setArray(double[] array) {
		baseArray = array;
	}

	// Append a value to the collection.
	public int add(double value) {
		int index = getAddIndex();
		baseArray[index] = value;
		return index;
	}

	// Insert a value into the collection.
	public void insert(int index, double value) {
		if (index < 0 || index > actualSize)
			throw new ArrayIndexOutOfBoundsException("Invalid index value");
		makeInsertSpace(index);
		baseArray[index] = value;
	}

	// Get value from the collection.
	public double get(int index) {
		if (index < 0 || index >= actualSize)
			throw new ArrayIndexOutOfBoundsException("Invalid index value");
		return baseArray[index];
	}

	// Set the value at a position in the collection.
	public void set(int index, double value) {
		if (index < 0 || index >= actualSize)
			throw new ArrayIndexOutOfBoundsException("Invalid index value");
		baseArray[index] = value;
	}

	public boolean contains(double value) {
		for (int i = 0; i < actualSize; i++)
			if (baseArray[i] == value)
				return true;
		return false;
	}

	public String toString() {
		StringBuilder result = new StringBuilder();
		String delimiter = "";
		for (int i = 0; i < actualSize; i++) {
			result.append(delimiter).append(baseArray[i]);
			delimiter = ", ";
		}
		return "[ " + result.toString() + " ]";
	}

	public static void main(String[] args) {
		DoubleArray array = new DoubleArray();
		array.ensureCapacity(5);
		array.insert(2, 1);
		array.insert(5, 2.2);
		System.out.println(array.toString());
	}
}