package fiji.updater;

import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BinaryPList extends HashMap {
	protected int offsetByteSize, refByteSize;
	protected long offsetCount, topLevelOffsetIndex, offsetTableOffset;
	protected long[] offsetTable;
	protected RandomAccessFile file;

	public static BinaryPList readDock() throws IOException {
		String path = System.getenv("HOME") + "/Library/Preferences/com.apple.dock.plist";
		return new BinaryPList(new RandomAccessFile(path, "r"));
	}

	public BinaryPList(RandomAccessFile file) throws IOException {
		this.file = file;

		file.seek(0);
		byte[] buffer = new byte[6];
		file.readFully(buffer);
		if (!"bplist".equals(new String(buffer)))
			throw new IOException("Not a binary property list");

		file.seek(file.length() - 26);
		offsetByteSize = file.readByte() & 0xff;
		refByteSize = file.readByte() & 0xff;
		offsetCount = file.readLong();
		topLevelOffsetIndex = file.readLong();
		offsetTableOffset = file.readLong();

		offsetTable = new long[(int)offsetCount];
		file.seek(offsetTableOffset);
		for (int i = 0; i < offsetCount; i++)
			offsetTable[i] = readOffset();

		file.seek(offsetTable[(int)topLevelOffsetIndex]);
		Object root = read();
		if (root instanceof Map) {
			Map map = (Map)root;
			for (Object key : map.keySet())
				put(key, map.get(key));
		}
		else
			put("root", root);
		file.close();
	}

	protected long readOffset() throws IOException {
		return readVariableSizeLong(offsetByteSize);
	}

	protected int readObjRef() throws IOException {
		return (int)readVariableSizeLong(refByteSize);
	}

	protected long readVariableSizeLong(int byteSize) throws IOException {
		switch (byteSize) {
			case 0:
			case 1: return file.readByte() & 0xff;
			case 2: return file.readShort() & 0xffff;
			case 4: return file.readInt() & 0xffffffffl;
			case 8: return file.readLong();
		}
		throw new IOException("Illegal byte size @" + hex(file.getFilePointer()) + ": " + byteSize);
	}

	protected int getSize(int size /* nybble */) throws IOException {
		if (size < 0xf)
			return size;
		return ((Integer)read()).intValue();
	}

	protected byte[] readByteArray(int count) throws IOException {
		byte[] result = new byte[count];
		file.readFully(result);
		return result;
	}

	protected int[] readObjRefArray(int count) throws IOException {
		int[] result = new int[count];
		for (int i = 0; i < count; i++)
			result[i] = readObjRef();
		return result;
	}

	/*
	 * From http://www.opensource.apple.com/source/CF/CF-476.10/CFBinaryPList.c:
	 *
	 * HEADER
	 *	magic number ("bplist")
	 *	file format version
	 *
	 * OBJECT TABLE
	 *	variable-sized objects
	 *
	 *	Object Formats (marker byte followed by additional info in some cases)
	 *	null	0000 0000
	 *	bool	0000 1000			// false
	 *	bool	0000 1001			// true
	 *	fill	0000 1111			// fill byte
	 *	int	0001 nnnn	...		// # of bytes is 2^nnnn, big-endian bytes
	 *	real	0010 nnnn	...		// # of bytes is 2^nnnn, big-endian bytes
	 *	date	0011 0011	...		// 8 byte float follows, big-endian bytes
	 *	data	0100 nnnn	[int]	...	// nnnn is number of bytes unless 1111 then int count follows, followed by bytes
	 *	string	0101 nnnn	[int]	...	// ASCII string, nnnn is # of chars, else 1111 then int count, then bytes
	 *	string	0110 nnnn	[int]	...	// Unicode string, nnnn is # of chars, else 1111 then int count, then big-endian 2-byte uint16_t
	 *		0111 xxxx			// unused
	 *	uid	1000 nnnn	...		// nnnn+1 is # of bytes
	 *		1001 xxxx			// unused
	 *	array	1010 nnnn	[int]	objref*	// nnnn is count, unless '1111', then int count follows
	 *		1011 xxxx			// unused
	 *	set	1100 nnnn	[int]	objref* // nnnn is count, unless '1111', then int count follows
	 *	dict	1101 nnnn	[int]	keyref* objref*	// nnnn is count, unless '1111', then int count follows
	 *		1110 xxxx			// unused
	 *		1111 xxxx			// unused
	 *
	 * OFFSET TABLE
	 *	list of ints, byte size of which is given in trailer
	 *	-- these are the byte offsets into the file
	 *	-- number of these is in the trailer
	 *
	 * TRAILER
	 *	byte size of offset ints in offset table
	 *	byte size of object refs in arrays and dicts
	 *	number of offsets in offset table (also is number of objects)
	 *	element # in offset table which is top level object
	 *	offset table offset
	 *
	 */
	protected Object read() throws IOException {
		byte b = file.readByte();
		int size = b & 0xf;
		switch ((b & 0xff) >> 4) {
			case 0x0:
				switch (b & 0xff) {
					case 0x0: return null;
					case 0x8: return Boolean.FALSE;
					case 0x9: return Boolean.TRUE;
				}
				break;
			case 0x1:
				return new Integer((int)readVariableSizeLong(size));
			case 0x2:
			case 0x3:
			case 0x4:
				size = getSize(size);
				return readByteArray(size);
			case 0x5:
				size = getSize(size);
				return new String(readByteArray(size), "ASCII");
			case 0x6:
				size = getSize(size);
				return new String(readByteArray(size), "UTF-16");
			case 0x8:
			case 0xa: {
				size = getSize(size);
				int[] refs = readObjRefArray(size);
				Object[] array = new Object[size];
				for (int i = 0; i < size; i++)
					array[i] = readRef(refs[i]);
				return array;
			}
			case 0xc: {
				size = getSize(size);
				int[] refs = readObjRefArray(size);
				Set set = new HashSet();
				for (int i = 0; i < size; i++)
					set.add(readRef(refs[i]));
				return set;
			}
			case 0xd: {
				size = getSize(size);
				int[] refs = readObjRefArray(2 * size);
				Map map = new HashMap();
				for (int i = 0; i < size; i++)
					map.put(readRef(refs[i]), readRef(refs[i + size]));
				return map;
			}
		}
		throw new IOException("Invalid type @" + hex(file.getFilePointer() - 1) + ": " + hex((b & 0xff) >> 4) + ", " + hex(b & 0xf));
	}

	protected Object readRef(int ref) throws IOException {
		return readAt(offsetTable[ref]);
	}

	protected Object readAt(long offset) throws IOException {
		long filePointer = file.getFilePointer();
		file.seek(offset);
		Object result = read();
		file.seek(filePointer);
		return result;
	}

	protected final static String hexNumbers = "0123456789abcdef";

	public static String hex(long number) {
		int shift;
		for (shift = 64 - 8; shift > 0 && (number >> shift) == 0; shift -= 8)
			; // do nothing
		StringBuffer result = new StringBuffer();
		result.append("0x");
		for (; shift >= 0; shift -= 8) {
			int h = (int)((number >> shift) & 0xff);
			result.append(hexNumbers.charAt(h >> 4));
			result.append(hexNumbers.charAt(h & 0xf));
		}
		return result.toString();
	}

	public String toString() {
		return append(new StringBuffer(), this, "").toString();
	}

	protected static StringBuffer append(StringBuffer buffer, Object object, String indent) {
		if (object == null)
			buffer.append(indent + "(null)\n");
		else if (object instanceof Map) {
			Map map = (Map)object;
			for (Object key : map.keySet()) {
				buffer.append(indent + key + ": {\n");
				append(buffer, map.get(key), indent + "    ");
				buffer.append(indent + "}\n");
			}
		}
		else if (object instanceof Set) {
			Set set = (Set)object;
			for (Object key : set) {
				buffer.append(indent + "{\n");
				append(buffer, key, indent + "    ");
				buffer.append(indent + "}\n");
			}
		}
		else if (object instanceof Object[]) {
			Object[] array = (Object[])object;
			for (int i = 0; i < array.length; i++) {
				buffer.append(indent + i + ": {\n");
				append(buffer, array[i], indent + "    ");
				buffer.append(indent + "}\n");
			}
		}
		else
			buffer.append(indent + object + "\n");
		return buffer;
	}

	public int getPersistentApp(String fileLabel) {
		Object[] array = (Object[])getPersistentApps();
		for (int i = 0; array != null && i < array.length; i++)
			if ((array[i] instanceof Map) && fileLabel.equals(get((Map)array[i],
					new String[] { "tile-data", "file-label" }, 0)))
				return i;
		return -1;
	}

	protected Object get(Map map, String[] multiKey, int keyOffset) {
		Object object = map.get(multiKey[keyOffset]);
		if (object == null)
			return null;
		if (++keyOffset >= multiKey.length)
			return object;
		if (object instanceof Map)
			return get((Map)object, multiKey, keyOffset);
		return null;
	}

	public Object[] getPersistentApps() {
		Object object = get("persistent-apps");
		if (object == null || !(object instanceof Object[]))
			return null;
		return (Object[])object;
	}

	public String[] getPersistentAppNames() {
		Object[] array = (Object[])getPersistentApps();
		if (array == null)
			return null;

		String[] result = new String[array.length];
		for (int i = 0; i < array.length; i++)
			if (array[i] instanceof Map) {
				Object object = get((Map)array[i], new String[] { "tile-data", "file-label" }, 0);
				if (object instanceof String)
					result[i] = (String)object;
			}
		return result;
	}

	public String getPersistentAppURL(String fileLabel) {
		int index = getPersistentApp(fileLabel);
		if (index < 0)
			return null;
		Map map = (Map)getPersistentApps()[index];
		Object object = get(map, new String[] { "tile-data", "file-data", "_CFURLString" }, 0);
		if (object instanceof String)
			return (String)object;
		return null;
	}

	public static void main(String[] args) {
		try {
			BinaryPList dock = readDock();
			System.err.println(append(new StringBuffer(), dock.getPersistentAppNames(), "").toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}