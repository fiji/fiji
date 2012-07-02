package imageware;

/**
 * Class Access.
 *
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */

public interface Access extends Buffer {
	
	// getPixel section
	public double 	getPixel(int x, int y, int z);
	public double 	getPixel(int x, int y, int z, byte boundaryConditions);
	public double 	getInterpolatedPixel(double x, double y, double z);
	public double 	getInterpolatedPixel(double x, double y, double z, byte boundaryConditions);
	
	// putPixel section
	public void 	putPixel(int x, int y, int z, double value);
	
	// getBounded section
	
	public void 	getBoundedX   (int x, int y, int z, byte[] buffer);
	public void 	getBoundedY   (int x, int y, int z, byte[] buffer);
	public void 	getBoundedZ   (int x, int y, int z, byte[] buffer);
	public void 	getBoundedXY  (int x, int y, int z, byte[][] buffer);
	public void 	getBoundedXZ  (int x, int y, int z, byte[][] buffer);
	public void 	getBoundedYZ  (int x, int y, int z, byte[][] buffer);
	public void 	getBoundedXYZ (int x, int y, int z, byte[][][] buffer);
	
	public void 	getBoundedX   (int x, int y, int z, short[] buffer);
	public void 	getBoundedY   (int x, int y, int z, short[] buffer);
	public void 	getBoundedZ   (int x, int y, int z, short[] buffer);
	public void 	getBoundedXY  (int x, int y, int z, short[][] buffer);
	public void 	getBoundedXZ  (int x, int y, int z, short[][] buffer);
	public void 	getBoundedYZ  (int x, int y, int z, short[][] buffer);
	public void 	getBoundedXYZ (int x, int y, int z, short[][][] buffer);
	
	public void 	getBoundedX   (int x, int y, int z, float[] buffer);
	public void 	getBoundedY   (int x, int y, int z, float[] buffer);
	public void 	getBoundedZ   (int x, int y, int z, float[] buffer);
	public void 	getBoundedXY  (int x, int y, int z, float[][] buffer);
	public void 	getBoundedXZ  (int x, int y, int z, float[][] buffer);
	public void 	getBoundedYZ  (int x, int y, int z, float[][] buffer);
	public void 	getBoundedXYZ (int x, int y, int z, float[][][] buffer);
	
	public void 	getBoundedX   (int x, int y, int z, double[] buffer);
	public void 	getBoundedY   (int x, int y, int z, double[] buffer);
	public void 	getBoundedZ   (int x, int y, int z, double[] buffer);
	public void 	getBoundedXY  (int x, int y, int z, double[][] buffer);
	public void 	getBoundedXZ  (int x, int y, int z, double[][] buffer);
	public void 	getBoundedYZ  (int x, int y, int z, double[][] buffer);
	public void 	getBoundedXYZ (int x, int y, int z, double[][][] buffer);
	
	
	// getBlock with boundary conditions section
	
	public void 	getBlockX  (int x, int y, int z, byte[] buffer, byte boundaryConditions);
	public void 	getBlockY  (int x, int y, int z, byte[] buffer, byte boundaryConditions);
	public void 	getBlockZ  (int x, int y, int z, byte[] buffer, byte boundaryConditions);
	public void 	getBlockXY (int x, int y, int z, byte[][] buffer, byte boundaryConditions);
	public void 	getBlockXZ (int x, int y, int z, byte[][] buffer, byte boundaryConditions);
	public void 	getBlockYZ (int x, int y, int z, byte[][] buffer, byte boundaryConditions);
	public void 	getBlockXYZ(int x, int y, int z, byte[][][] buffer, byte boundaryConditions);
	
	public void 	getBlockX  (int x, int y, int z, short[] buffer, byte boundaryConditions);
	public void 	getBlockY  (int x, int y, int z, short[] buffer, byte boundaryConditions);
	public void 	getBlockZ  (int x, int y, int z, short[] buffer, byte boundaryConditions);
	public void 	getBlockXY (int x, int y, int z, short[][] buffer, byte boundaryConditions);
	public void 	getBlockXZ (int x, int y, int z, short[][] buffer, byte boundaryConditions);
	public void 	getBlockYZ (int x, int y, int z, short[][] buffer, byte boundaryConditions);
	public void 	getBlockXYZ(int x, int y, int z, short[][][] buffer, byte boundaryConditions);
	
	public void 	getBlockX  (int x, int y, int z, float[] buffer, byte boundaryConditions);
	public void 	getBlockY  (int x, int y, int z, float[] buffer, byte boundaryConditions);
	public void 	getBlockZ  (int x, int y, int z, float[] buffer, byte boundaryConditions);
	public void 	getBlockXY (int x, int y, int z, float[][] buffer, byte boundaryConditions);
	public void 	getBlockXZ (int x, int y, int z, float[][] buffer, byte boundaryConditions);
	public void 	getBlockYZ (int x, int y, int z, float[][] buffer, byte boundaryConditions);
	public void 	getBlockXYZ(int x, int y, int z, float[][][] buffer, byte boundaryConditions);
	
	public void 	getBlockX  (int x, int y, int z, double[] buffer, byte boundaryConditions);
	public void 	getBlockY  (int x, int y, int z, double[] buffer, byte boundaryConditions);
	public void 	getBlockZ  (int x, int y, int z, double[] buffer, byte boundaryConditions);
	public void 	getBlockXY (int x, int y, int z, double[][] buffer, byte boundaryConditions);
	public void 	getBlockXZ (int x, int y, int z, double[][] buffer, byte boundaryConditions);
	public void 	getBlockYZ (int x, int y, int z, double[][] buffer, byte boundaryConditions);
	public void 	getBlockXYZ(int x, int y, int z, double[][][] buffer, byte boundaryConditions);
	
	
	// getNeighborhood with boundary conditions section
	
	public void 	getNeighborhoodX  (int x, int y, int z, byte[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodY  (int x, int y, int z, byte[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodZ  (int x, int y, int z, byte[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXY (int x, int y, int z, byte[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXZ (int x, int y, int z, byte[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodYZ (int x, int y, int z, byte[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXYZ(int x, int y, int z, byte[][][] buffer, byte boundaryConditions);
	
	public void 	getNeighborhoodX  (int x, int y, int z, short[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodY  (int x, int y, int z, short[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodZ  (int x, int y, int z, short[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXY (int x, int y, int z, short[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXZ (int x, int y, int z, short[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodYZ (int x, int y, int z, short[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXYZ(int x, int y, int z, short[][][] buffer, byte boundaryConditions);
	
	public void 	getNeighborhoodX  (int x, int y, int z, float[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodY  (int x, int y, int z, float[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodZ  (int x, int y, int z, float[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXY (int x, int y, int z, float[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXZ (int x, int y, int z, float[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodYZ (int x, int y, int z, float[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXYZ(int x, int y, int z, float[][][] buffer, byte boundaryConditions);
	
	public void 	getNeighborhoodX  (int x, int y, int z, double[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodY  (int x, int y, int z, double[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodZ  (int x, int y, int z, double[] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXY (int x, int y, int z, double[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXZ (int x, int y, int z, double[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodYZ (int x, int y, int z, double[][] buffer, byte boundaryConditions);
	public void 	getNeighborhoodXYZ(int x, int y, int z, double[][][] buffer, byte boundaryConditions);
	
	
	// putBounded section
	
	public void 	putBoundedX   (int x, int y, int z, byte[] buffer);
	public void 	putBoundedY   (int x, int y, int z, byte[] buffer);
	public void 	putBoundedZ   (int x, int y, int z, byte[] buffer);
	public void 	putBoundedXY  (int x, int y, int z, byte[][] buffer);
	public void 	putBoundedXZ  (int x, int y, int z, byte[][] buffer);
	public void 	putBoundedYZ  (int x, int y, int z, byte[][] buffer);
	public void 	putBoundedXYZ  (int x, int y, int z, byte[][][] buffer);
	
	public void 	putBoundedX   (int x, int y, int z, short[] buffer);
	public void 	putBoundedY   (int x, int y, int z, short[] buffer);
	public void 	putBoundedZ   (int x, int y, int z, short[] buffer);
	public void 	putBoundedXY  (int x, int y, int z, short[][] buffer);
	public void 	putBoundedXZ  (int x, int y, int z, short[][] buffer);
	public void 	putBoundedYZ  (int x, int y, int z, short[][] buffer);
	public void 	putBoundedXYZ  (int x, int y, int z, short[][][] buffer);
	
	public void 	putBoundedX   (int x, int y, int z, float[] buffer);
	public void 	putBoundedY   (int x, int y, int z, float[] buffer);
	public void 	putBoundedZ   (int x, int y, int z, float[] buffer);
	public void 	putBoundedXY  (int x, int y, int z, float[][] buffer);
	public void 	putBoundedXZ  (int x, int y, int z, float[][] buffer);
	public void 	putBoundedYZ  (int x, int y, int z, float[][] buffer);
	public void 	putBoundedXYZ  (int x, int y, int z, float[][][] buffer);
	
	public void 	putBoundedX   (int x, int y, int z, double[] buffer);
	public void 	putBoundedY   (int x, int y, int z, double[] buffer);
	public void 	putBoundedZ   (int x, int y, int z, double[] buffer);
	public void 	putBoundedXY  (int x, int y, int z, double[][] buffer);
	public void 	putBoundedXZ  (int x, int y, int z, double[][] buffer);
	public void 	putBoundedYZ  (int x, int y, int z, double[][] buffer);
	public void 	putBoundedXYZ  (int x, int y, int z, double[][][] buffer);
	
}
