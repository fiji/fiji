package fiji.build;

import java.util.Iterator;

public class ByteCodeAnalyzer {
	byte[] buffer;
	int[] poolOffsets;
	int endOffset;

	public ByteCodeAnalyzer(byte[] buffer) {
		this.buffer = buffer;
		if ((int)getU4(0) != 0xcafebabe)
			throw new RuntimeException("No class");
		getConstantPoolOffsets();
	}

	public String getPathForClass() {
		int thisOffset = dereferenceOffset(endOffset + 2);
		if (getU1(thisOffset) != 7)
			throw new RuntimeException("Parse error");
		return getString(dereferenceOffset(thisOffset + 1));
	}

	int dereferenceOffset(int offset) {
		int index = getU2(offset);
		return poolOffsets[index - 1];
	}

	void getConstantPoolOffsets() {
		int poolCount = getU2(8) - 1;
		poolOffsets = new int[poolCount];
		int offset = 10;
		for (int i = 0; i < poolCount; i++) {
			poolOffsets[i] = offset;
			int tag = getU1(offset);
			if (tag == 7 || tag == 8)
				offset += 3;
			else if (tag == 9 || tag == 10 || tag == 11 ||
					tag == 3 || tag == 4 ||
					tag == 12)
				offset += 5;
			else if (tag == 5 || tag == 6) {
				poolOffsets[++i] = offset;
				offset += 9;
			}
			else if (tag == 1)
				offset += 3 + getU2(offset + 1);
			else
				throw new RuntimeException("Unknown tag"
					+ " " + tag);
		}
		endOffset = offset;
	}

	protected class ClassNameIterator implements Iterator<String> {
		int index;

		ClassNameIterator() {
			index = -1;
			findNext();
		}

		void findNext() {
			while (++index < poolOffsets.length)
				if (getU1(poolOffsets[index]) == 7)
					break;
		}

		public boolean hasNext() {
			return index < poolOffsets.length;
		}

		public String next() {
			int offset = poolOffsets[index];
			findNext();
			return getString(dereferenceOffset(offset + 1));
		}

		public void remove()
				throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
	}

	public Iterable<String> getClassNames() {
		return new Iterable<String>() {
			public Iterator<String> iterator() {
				return new ClassNameIterator();
			}
		};
	}

	public String toString() {
		String result = "";
		for (int i = 0; i < poolOffsets.length; i++) {
			int offset = poolOffsets[i];
			result += "index #" + i + ": "
				+ format(offset) + "\n";
			int tag = getU1(offset);
			if (tag == 5 || tag == 6)
				i++;
		}
		return result;
	}

	int getU1(int offset) {
		return buffer[offset] & 0xff;
	}

	int getU2(int offset) {
		return getU1(offset) << 8 | getU1(offset + 1);
	}

	long getU4(int offset) {
		return ((long)getU2(offset)) << 16 | getU2(offset + 2);
	}

	String getString(int offset) {
		try {
			return new String(buffer, offset + 3,
					getU2(offset + 1), "UTF-8");
		} catch (Exception e) { return ""; }
	}

	String format(int offset) {
		int tag = getU1(offset);
		int u2 = getU2(offset + 1);
		String result = "offset: " + offset + "(" + tag + "), ";
		if (tag == 7)
			return result + "class " + u2;
		if (tag == 9)
			return result + "field " + u2 + ", "
				+ getU2(offset + 3);
		if (tag == 10)
			return result + "method " + u2 + ", "
				+ getU2(offset + 3);
		if (tag == 11)
			return result + "interface method " + u2 + ", "
				+ getU2(offset + 3);
		if (tag == 8)
			return result + "string #" + u2;
		if (tag == 3)
			return result + "integer " + getU4(offset + 1);
		if (tag == 4)
			return result + "float " + getU4(offset + 1);
		if (tag == 12)
			return result + "name and type " + u2 + ", "
				+ getU2(offset + 3);
		if (tag == 5)
			return result + "long "
				+ getU4(offset + 1) + ", "
				+ getU4(offset + 5);
		if (tag == 6)
			return result + "double "
				+ getU4(offset + 1) + ", "
				+ getU4(offset + 5);
		if (tag == 1)
			return result + "utf8 " + u2
				+ " " + getString(offset);
		return result + "unknown";
	}
}