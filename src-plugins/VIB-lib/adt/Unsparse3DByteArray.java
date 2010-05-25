package adt;

/**
 * User: Tom Larkworthy
 * Date: 13-Jul-2006
 * Time: 20:10:20
 */
public class Unsparse3DByteArray implements Byte3DArray {
    byte[][][] data;
    int width, hieght, depth;

	public Unsparse3DByteArray(int width, int hieght, int depth) {
		this.depth = depth;
		this.width = width;
		this.hieght = hieght;
		data = new byte[width][hieght][depth+1];
	}

	public void put(int x, int y, int z, byte val) {
   		data[x][y][z] = val;
	}

	public byte get(int x, int y, int z) {
		return data[x][y][z];
	}

	public double getDouble(int x, int y, int z) {
		return ByteProbability.BYTE_TO_DOUBLE[data[x][y][z]&0xFF];
	}

	public int getxMin() {
		return 0;
	}

	public int getxMax() {
		return width-1;
	}

	public int getyMin() {
		return 0;
	}

	public int getyMax() {
		return hieght-1;
	}

	public int getzMin() {
		return 1;
	}

	public int getzMax() {
		return depth;
	}
}
