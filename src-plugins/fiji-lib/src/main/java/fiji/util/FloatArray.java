package fiji.util;

public class FloatArray extends ArrayBase<float[], Float>
{
	protected float[] baseArray;

	public FloatArray(int size, int growth) {
		super(size, growth, Float.TYPE);
	}

	public FloatArray(int size) {
		super(size, Float.TYPE);
	}

	public FloatArray() {
		super(0, Float.TYPE);
	}

	// Implementation of callout to get the underlying array.
	@Override
	protected float[] getArray() {
		return baseArray;
	}

	// Implementation of callout to set the underlying array.
	@Override
	protected void setArray(float[] array) {
		baseArray = array;
	}

	@Override
	protected Float valueOf(int index) {
		return Float.valueOf(baseArray[index]);
	}

	// Append a value to the collection.
	public int add(float value) {
		int index = getAddIndex();
		baseArray[index] = value;
		return index;
	}

	// Insert a value into the collection.
	public int insert(int index, float value) {
		if (index < 0 || index > actualSize)
			throw new ArrayIndexOutOfBoundsException("Invalid index value");
		makeInsertSpace(index);
		baseArray[index] = value;
		return index;
	}

	// Get value from the collection.
	public float get(int index) {
		if (index < 0 || index >= actualSize)
			throw new ArrayIndexOutOfBoundsException("Invalid index value");
		return baseArray[index];
	}

	// Set the value at a position in the collection.
	public void set(int index, float value) {
		if (index < 0 || index >= actualSize)
			throw new ArrayIndexOutOfBoundsException("Invalid index value");
		baseArray[index] = value;
	}

	public boolean contains(float value) {
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
		FloatArray array = new FloatArray();
		array.ensureCapacity(5);
		array.insert(2, 1);
		array.insert(5, 2.2f);
		System.out.println(array.toString());
	}
}