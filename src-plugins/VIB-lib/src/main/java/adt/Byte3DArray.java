package adt;

/**
 * User: Tom Larkworthy
 * Date: 13-Jul-2006
 * Time: 20:10:43
 */
public interface Byte3DArray {
	
	void put(int x, int y, int z, byte val);

	byte get(int x, int y, int z);

	double getDouble(int x, int y, int z);

	int getxMin();

	int getxMax();

	int getyMin();

	int getyMax();

	int getzMin();

	int getzMax();
}
