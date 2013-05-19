package fiji.util;

public class IntArray extends ArrayBase<int[], Integer> {
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
	@Override
	protected int[] getArray() {
		return baseArray;
	}

	// Implementation of callout to set the underlying array.
	@Override
	protected void setArray(int[] array) {
		baseArray = array;
	}

	@Override
	protected Integer valueOf(int index) {
		return Integer.valueOf(baseArray[index]);
	}

	// Append a value to the collection.
	public int add(int value) {
		int index = getAddIndex();
		baseArray[index] = value;
		return index;
	}

	// Insert a value into the collection.
	public int insert(int index, int value) {
		makeInsertSpace(index);
		baseArray[index] = value;
		return index;
	}

	// Get value from the collection.
	public int get(int index) {
		if (index < 0 || index >= actualSize)
			throw new ArrayIndexOutOfBoundsException("Invalid index value: " + index);
		return baseArray[index];
	}

	// Set the value at a position in the collection.
	public void set(int index, int value) {
		if (index < 0 || index >= actualSize)
			throw new ArrayIndexOutOfBoundsException("Invalid index value");
		baseArray[index] = value;
	}

	public boolean contains(int value) {
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
		IntArray array = new IntArray();
		array.ensureCapacity(5);
		array.insert(2, 1);
		array.insert(6, 2);
		System.out.println(array.toString());
	}
}