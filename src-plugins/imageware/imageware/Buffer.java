package imageware;

/**
 * Class Buffer.
 *
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */

public interface Buffer {
	public int 		getType();
	public String	getTypeToString();
	public int 		getDimension();
	public int[] 	getSize();
	public int 		getSizeX();
	public int 		getSizeY();
	public int 		getSizeZ();
	public int 		getWidth();
	public int 		getHeight();
	public int 		getDepth();
	public int 		getTotalSize();
	public boolean  isSameSize(ImageWare imageware);
	
	// get section
	public void 	getX   (int x, int y, int z, ImageWare buffer);
	public void 	getY   (int x, int y, int z, ImageWare buffer);
	public void 	getZ   (int x, int y, int z, ImageWare buffer);
	public void 	getXY  (int x, int y, int z, ImageWare buffer);
	public void 	getXZ  (int x, int y, int z, ImageWare buffer);
	public void 	getYZ  (int x, int y, int z, ImageWare buffer);
	public void 	getXYZ (int x, int y, int z, ImageWare buffer);
	
	public void 	getX   (int x, int y, int z, byte[] buffer);
	public void 	getY   (int x, int y, int z, byte[] buffer);
	public void 	getZ   (int x, int y, int z, byte[] buffer);
	public void 	getXY  (int x, int y, int z, byte[][] buffer);
	public void 	getXZ  (int x, int y, int z, byte[][] buffer);
	public void 	getYZ  (int x, int y, int z, byte[][] buffer);
	public void 	getXYZ  (int x, int y, int z, byte[][][] buffer);
	
	public void 	getX   (int x, int y, int z, short[] buffer);
	public void 	getY   (int x, int y, int z, short[] buffer);
	public void 	getZ   (int x, int y, int z, short[] buffer);
	public void 	getXY  (int x, int y, int z, short[][] buffer);
	public void 	getXZ  (int x, int y, int z, short[][] buffer);
	public void 	getYZ  (int x, int y, int z, short[][] buffer);
	public void 	getXYZ  (int x, int y, int z, short[][][] buffer);
	
	public void 	getX   (int x, int y, int z, float[] buffer);
	public void 	getY   (int x, int y, int z, float[] buffer);
	public void 	getZ   (int x, int y, int z, float[] buffer);
	public void 	getXY  (int x, int y, int z, float[][] buffer);
	public void 	getXZ  (int x, int y, int z, float[][] buffer);
	public void 	getYZ  (int x, int y, int z, float[][] buffer);
	public void 	getXYZ  (int x, int y, int z, float[][][] buffer);
	
	public void 	getX   (int x, int y, int z, double[] buffer);
	public void 	getY   (int x, int y, int z, double[] buffer);
	public void 	getZ   (int x, int y, int z, double[] buffer);
	public void 	getXY  (int x, int y, int z, double[][] buffer);
	public void 	getXZ  (int x, int y, int z, double[][] buffer);
	public void 	getYZ  (int x, int y, int z, double[][] buffer);
	public void 	getXYZ  (int x, int y, int z, double[][][] buffer);
	
	
	// put section
	public void 	putX   (int x, int y, int z, ImageWare buffer);
	public void 	putY   (int x, int y, int z, ImageWare buffer);
	public void 	putZ   (int x, int y, int z, ImageWare buffer);
	public void 	putXY  (int x, int y, int z, ImageWare buffer);
	public void 	putXZ  (int x, int y, int z, ImageWare buffer);
	public void 	putYZ  (int x, int y, int z, ImageWare buffer);
	public void 	putXYZ (int x, int y, int z, ImageWare buffer);
	
	public void 	putX   (int x, int y, int z, byte[] buffer);
	public void 	putY   (int x, int y, int z, byte[] buffer);
	public void 	putZ   (int x, int y, int z, byte[] buffer);
	public void 	putXY  (int x, int y, int z, byte[][] buffer);
	public void 	putXZ  (int x, int y, int z, byte[][] buffer);
	public void 	putYZ  (int x, int y, int z, byte[][] buffer);
	public void 	putXYZ  (int x, int y, int z, byte[][][] buffer);
	
	public void 	putX   (int x, int y, int z, short[] buffer);
	public void 	putY   (int x, int y, int z, short[] buffer);
	public void 	putZ   (int x, int y, int z, short[] buffer);
	public void 	putXY  (int x, int y, int z, short[][] buffer);
	public void 	putXZ  (int x, int y, int z, short[][] buffer);
	public void 	putYZ  (int x, int y, int z, short[][] buffer);
	public void 	putXYZ  (int x, int y, int z, short[][][] buffer);
	
	public void 	putX   (int x, int y, int z, float[] buffer);
	public void 	putY   (int x, int y, int z, float[] buffer);
	public void 	putZ   (int x, int y, int z, float[] buffer);
	public void 	putXY  (int x, int y, int z, float[][] buffer);
	public void 	putXZ  (int x, int y, int z, float[][] buffer);
	public void 	putYZ  (int x, int y, int z, float[][] buffer);
	public void 	putXYZ  (int x, int y, int z, float[][][] buffer);
	
	public void 	putX   (int x, int y, int z, double[] buffer);
	public void 	putY   (int x, int y, int z, double[] buffer);
	public void 	putZ   (int x, int y, int z, double[] buffer);
	public void 	putXY  (int x, int y, int z, double[][] buffer);
	public void 	putXZ  (int x, int y, int z, double[][] buffer);
	public void 	putYZ  (int x, int y, int z, double[][] buffer);
	public void 	putXYZ  (int x, int y, int z, double[][][] buffer);
	
	
	// get Slice in a specific type for a fast and direct access
	public Object[]	getVolume();
	
	public byte[] 	getSliceByte(int z);
	public short[] 	getSliceShort(int z);
	public float[] 	getSliceFloat(int z);
	public double[] getSliceDouble(int z);
	
}

