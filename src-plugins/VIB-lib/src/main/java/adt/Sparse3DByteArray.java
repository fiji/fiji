package adt;

import java.util.HashMap;

/**
 * allows 3D byte data to be stored effeciently, if the data is mostly 0s
 * User: Tom Larkworthy
 * Date: 12-Jul-2006
 * Time: 19:16:00
 */
public class Sparse3DByteArray implements Byte3DArray{
	HashMap<Integer, HashMap<Integer, HashMap<Integer, Byte>>> data = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Byte>>>();

	public int xMin = Integer.MAX_VALUE, xMax = Integer.MIN_VALUE;
	public int yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
	public int zMin = Integer.MAX_VALUE, zMax = Integer.MIN_VALUE;


	/**
	 *don't put 0s in here unless you are overwriting a value becuase the class will create memory for them.
	 *
	 */
	public void put(int x, int y, int z, byte val) {
		xMin = Math.min(xMin, x);
		xMax = Math.max(xMax, x);
		yMin = Math.min(yMin, y);
		yMax = Math.max(yMax, y);
		zMin = Math.min(zMin, z);
		zMax = Math.max(zMax, z);


		HashMap<Integer, HashMap<Integer, Byte>> yzDim = data.get(x);
		if (yzDim == null) {
			yzDim = new HashMap<Integer, HashMap<Integer, Byte>>();
			data.put(x, yzDim);
		}

		HashMap<Integer, Byte> zDim = yzDim.get(y);

		if (zDim == null) {
			zDim = new HashMap<Integer, Byte>();
			yzDim.put(y, zDim);
		}

		zDim.put(z, val);
	}

	public byte get(int x, int y, int z) {
		HashMap<Integer, HashMap<Integer, Byte>> yzDim = data.get(x);


		if (yzDim == null) {
			return 0;
		}

		HashMap<Integer, Byte> zDim = yzDim.get(y);

		if (zDim == null) {
			return 0;
		}

		Byte val = zDim.get(z);
		if (val == null) return 0;
		return val;
	}

	/**
	 * returns a scaled double between 0 and 1
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public double getDouble(int x, int y, int z){
		return ByteProbability.BYTE_TO_DOUBLE[get(x,y,z) & 0xFF];
	}

	public int getxMin() {
		return xMin;
	}

	public int getxMax() {
		return xMax;
	}

	public int getyMin() {
		return yMin;
	}

	public int getyMax() {
		return yMax;
	}

	public int getzMin() {
		return zMin;
	}

	public int getzMax() {
		return zMax;
	}
}
