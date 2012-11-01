package imageware;

import ij.ImageStack;

import java.awt.Image;

/**
 * Class FloatAccess.
 *
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */

public class FloatAccess extends FloatBuffer implements Access {

	//------------------------------------------------------------------
	//
	//	Constructors section
	//
	//------------------------------------------------------------------
	protected FloatAccess(int nx, int ny, int nz) 		{ super(nx, ny, nz); }
	protected FloatAccess(Image image, int mode)		{ super(image, mode); }

	protected FloatAccess(ImageStack stack, int mode) 	{ super(stack, mode); }
	protected FloatAccess(ImageStack stack, byte chan)  { super(stack, chan); }

	protected FloatAccess(byte[] array, int mode) 				{ super(array, mode); }
	protected FloatAccess(byte[][] array, int mode) 	{ super(array, mode); }
	protected FloatAccess(byte[][][] array, int mode) 	{ super(array, mode); }
	protected FloatAccess(short[] array, int mode) 		{ super(array, mode); }
	protected FloatAccess(short[][] array, int mode) 	{ super(array, mode); }
	protected FloatAccess(short[][][] array, int mode) 	{ super(array, mode); }
	protected FloatAccess(float[] array, int mode) 		{ super(array, mode); }
	protected FloatAccess(float[][] array, int mode) 	{ super(array, mode); }
	protected FloatAccess(float[][][] array, int mode) 	{ super(array, mode); }
	protected FloatAccess(double[] array, int mode) 	{ super(array, mode); }
	protected FloatAccess(double[][] array, int mode) 	{ super(array, mode); }
	protected FloatAccess(double[][][] array, int mode) { super(array, mode); }
	
	//------------------------------------------------------------------
	//
	//	getPixel section
	//
	//------------------------------------------------------------------
	
	/**
	* Get a pixel at specific position without specific boundary conditions
	*
	* If the positions is outside of this imageware, the method return 0.0.
	*
	* @param	x		position in the X axis
	* @param	y		position in the Y axis
	* @param	z		position in the Z axis
	* @return	a pixel value
	*/
	public double getPixel(int x, int y, int z) {
		if (x >= nx) 	return 0.0;
		if (y >= ny) 	return 0.0;
		if (z >= nz) 	return 0.0;
		if (x < 0) 		return 0.0;
		if (y < 0) 		return 0.0;
		if (z < 0) 		return 0.0;
		return ((float[])data[z])[x+y*nx];
	}

	/**
	* Get a pixel at specific position with specific boundary conditions
	*
	* If the positions is outside of this imageware, the method apply the 
	* boundary conditions to return a value.
	*
	* @param	x		position in the X axis
	* @param	y		position in the Y axis
	* @param	z		position in the Z axis
	* @return	a pixel value
	*/
	public double getPixel(int x, int y, int z, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw new ArrayStoreException(
					"\n-------------------------------------------------------\n" +
					"Error in imageware package\n" +
					"Unable to put a pixel \n" + 
					"at the position (" + x + "," + y + "," + z + ".\n" +
					"-------------------------------------------------------\n"
				);
		}
		int xp = x;
		while (xp < 0)
			xp += xperiod;
		while (xp >= nx) {
			xp = xperiod - xp;
			xp = (xp < 0 ? -xp : xp);
		}
		int yp = y;
		while (yp < 0)
			yp += yperiod;
		while (yp >= ny) {
			yp = yperiod - yp;
			yp = (yp < 0 ? -yp : yp);
		}
		int zp = z;
		while (zp < 0)
			zp += zperiod;
		while (zp >= nz) {
			zp = zperiod - zp;
			zp = (zp < 0 ? -zp : zp);
		}
		return ((float[])data[zp])[xp+yp*nx];
	}

	/**
	* Get a interpolated pixel value at specific position without specific boundary conditions.
	*
	* If the positions is not on the pixel grid, the method return a interpolated
	* value of the pixel (linear interpolation). If the positions is outside of this
	* imageware, the method return 0.0.
	*
	* @param	x		position in the X axis
	* @param	y		position in the Y axis
	* @param	z		position in the Z axis
	* @return	a interpolated value
	*/
	public double getInterpolatedPixel(double x, double y, double z) {
		if (x > nx-1) 	return 0.0;
		if (y > ny-1) 	return 0.0;
		if (z > nz-1) 	return 0.0;
		if (x < 0) 		return 0.0;
		if (y < 0) 		return 0.0;
		if (z < 0) 		return 0.0;
		double output = 0.0;
		/*
		int i = (x >= 0.0 ? ((int)x) : ((int)x - 1));
		int j = (y >= 0.0 ? ((int)y) : ((int)y - 1));
		int k = (z >= 0.0 ? ((int)z) : ((int)z - 1));
		*/
		int i = (x >= 0.0 ? ((int)x) : ((int)x - 1));
		int j = (y >= 0.0 ? ((int)y) : ((int)y - 1));
		int k = (z >= 0.0 ? ((int)z) : ((int)z - 1));
		boolean fi = (i==nx-1);
		boolean fj = (j==ny-1);
		boolean fk = (k==nz-1);
		int index = i+j*nx;	
		switch(getDimension()) {	
			case 1:
				double v1_0 = (double)(((float[])data[k])[index]);
				double v1_1 = (fi ? v1_0 : (double)(((float[])data[k])[index+1]));
				double dx1 = x - (double)i;
				return v1_1*dx1 - v1_0 * (dx1-1.0);
			case 2:
				double v2_00 = (double)(((float[])data[k])[index]);
				double v2_10 = (fi ? v2_00 : (double)(((float[])data[k])[index+1]));
				double v2_01 = (fj ? v2_00 : (double)(((float[])data[k])[index+nx]));
				double v2_11 = (fi ? (fj ? v2_00 : v2_01) : (double)(((float[])data[k])[index+1+nx]));
				double dx2 = x - (double)i;
				double dy2 = y - (double)j;
				return (dx2*(v2_11*dy2-v2_10*(dy2-1.0)) - (dx2-1.0)*(v2_01*dy2-v2_00*(dy2-1.0)));
			case 3:				
				double v3_000 = (double)(((float[])data[k])[index]);
				double v3_100 = (fi ? v3_000 : (double)(((float[])data[k])[index+1]));
				double v3_010 = (fj ? v3_000 : (double)(((float[])data[k])[index+nx]));
				double v3_110 = (fi ? ( fj ? v3_000 : v3_010 ) : (double)(((float[])data[k])[index+1+nx]));
				double v3_001 = (fk ? v3_000 : (double)(((float[])data[k+1])[index]));
				double v3_011 = (fk ? ( fj ? v3_000 : v3_010) : (double)(((float[])data[k+1])[index+1]));
				double v3_101 = (fk ? ( fi ? v3_000 : v3_100) : (double)(((float[])data[k+1])[index+nx]));
				double v3_111 = (fk ? ( fj ? ( fi ? v3_000 : v3_100) : v3_110) :(double)(((float[])data[k+1])[index+1+nx]));
				double dx3 = x - (double)i;
				double dy3 = y - (double)j;
				double dz3 = z - (double)k;
				double z1 = (dx3*(v3_110*dy3-v3_100*(dy3-1.0)) - (dx3-1.0)*(v3_010*dy3-v3_000*(dy3-1.0)));
				double z2 = (dx3*(v3_111*dy3-v3_101*(dy3-1.0)) - (dx3-1.0)*(v3_011*dy3-v3_001*(dy3-1.0)));
				return z2*dz3-z1*(dz3-1.0);
		}
		return output;
	}

	/**
	* Get a interpolated pixel value at specific position with specific boundary conditions.
	*
	* If the positions is not on the pixel grid, the method return a interpolated
	* value of the pixel (linear interpolation).  
	* If the positions is outside of this imageware, the method apply the 
	* boundary conditions to return a value.
	*
	* @param	x		position in the X axis
	* @param	y		position in the Y axis
	* @param	z		position in the Z axis
	* @param	boundaryConditions	MIRROR or PERIODIC boundary conditions
	* @return	a interpolated value
	*/
	public double getInterpolatedPixel(double x, double y, double z, byte boundaryConditions) {
		double output = 0.0;
		int i = (x >= 0.0 ? ((int)x) : ((int)x - 1));
		int j = (y >= 0.0 ? ((int)y) : ((int)y - 1));
		int k = (z >= 0.0 ? ((int)z) : ((int)z - 1));
		switch(getDimension()) {	
			case 1:
				double v1_0 = getPixel(i, j, k, boundaryConditions);
				double v1_1 = getPixel(i+1, j, k, boundaryConditions);
				double dx1 = x - (double)i;
				return v1_1*dx1 - v1_0 * (dx1-1.0);
			case 2:
				double v2_00 = getPixel(i, j, k, boundaryConditions);
				double v2_10 = getPixel(i+1, j, k, boundaryConditions);
				double v2_01 = getPixel(i, j+1, k, boundaryConditions);
				double v2_11 = getPixel(i+1, j+1, k, boundaryConditions);
				double dx2 = x - (double)i;
				double dy2 = y - (double)j;
				return (dx2*(v2_11*dy2-v2_10*(dy2-1.0)) - (dx2-1.0)*(v2_01*dy2-v2_00*(dy2-1.0)));
			case 3:				
				double v3_000 = getPixel(i, j, k, boundaryConditions);
				double v3_100 = getPixel(i+1, j, k, boundaryConditions);
				double v3_010 = getPixel(i, j+1, k, boundaryConditions);
				double v3_110 = getPixel(i+1, j+1, k, boundaryConditions);
				double v3_001 = getPixel(i, j, k+1, boundaryConditions);
				double v3_011 = getPixel(i+1, j, k+1, boundaryConditions);
				double v3_101 = getPixel(i, j+1, k+1, boundaryConditions);
				double v3_111 = getPixel(i+1, j+1, k+1, boundaryConditions);
				double dx3 = x - (double)i;
				double dy3 = y - (double)j;
				double dz3 = z - (double)k;
				double z1 = (dx3*(v3_110*dy3-v3_100*(dy3-1.0)) - (dx3-1.0)*(v3_010*dy3-v3_000*(dy3-1.0)));
				double z2 = (dx3*(v3_111*dy3-v3_101*(dy3-1.0)) - (dx3-1.0)*(v3_011*dy3-v3_001*(dy3-1.0)));
				return z2*dz3-z1*(dz3-1.0);
		}
		return output;
	}

	//------------------------------------------------------------------
	//
	//	putPixel section
	//
	//------------------------------------------------------------------
	
	/**
	* Put a pixel at specific position
	*
	* If the positions is outside of this imageware, the method does nothing.
	*
	* @param	x		position in the X axis
	* @param	y		position in the Y axis
	* @param	z		position in the Z axis
	*/
	public void putPixel(int x, int y, int z, double value) {
		if (x >= nx) 	return;
		if (y >= ny) 	return;
		if (z >= nz) 	return;
		if (x < 0) 		return;
		if (y < 0) 		return;
		if (z < 0) 		return;
		((float[])data[z])[x+y*nx] = (float)value;
	}

	
	//------------------------------------------------------------------
	//
	// getBounded section
	//
	//------------------------------------------------------------------

	
	/**
	* Get an array from the imageware at the position (x,y,z) in X axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 1D array to get into the imageware
	*/
	public void getBoundedX(int x, int y, int z, byte[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (x < 0 ? -x : 0);
			int offset = (x+iinf) + (y)*nx;
			int leni = buffer.length;
			if (x+leni < 0) return;
			if (y < 0) 		return;
			if (z < 0) 		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (byte)(tmp[offset]);
				offset++;
			}
		}
		catch(Exception e) {
			throw_get("X", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in X axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 1D array to get into the imageware
	*/
	public void getBoundedX(int x, int y, int z, short[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (x < 0 ? -x : 0);
			int offset = (x+iinf) + (y)*nx;
			int leni = buffer.length;
			if (x+leni < 0) return;
			if (y < 0) 		return;
			if (z < 0) 		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (short)(tmp[offset]);
				offset++;
			}
		}
		catch(Exception e) {
			throw_get("X", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in X axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 1D array to get into the imageware
	*/
	public void getBoundedX(int x, int y, int z, float[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (x < 0 ? -x : 0);
			int offset = (x+iinf) + (y)*nx;
			int leni = buffer.length;
			if (x+leni < 0) return;
			if (y < 0) 		return;
			if (z < 0) 		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (float)(tmp[offset]);
				offset++;
			}
		}
		catch(Exception e) {
			throw_get("X", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in X axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 1D array to get into the imageware
	*/
	public void getBoundedX(int x, int y, int z, double[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (x < 0 ? -x : 0);
			int offset = (x+iinf) + (y)*nx;
			int leni = buffer.length;
			if (x+leni < 0) return;
			if (y < 0) 		return;
			if (z < 0) 		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (double)(tmp[offset]);
				offset++;
			}
		}
		catch(Exception e) {
			throw_get("X", "Bounded check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in Y axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 1D array to get into the imageware
	*/
	public void getBoundedY(int x, int y, int z, byte[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (y < 0 ? -y : 0);
			int offset = (x) + (y+iinf)*nx;
			int leni = buffer.length;
			if (x < 0) 		return;
			if (y+leni < 0) return;
			if (z < 0)		return;
			int isup = (y+leni >= ny ? ny-y : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (byte)(tmp[offset]);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_get("Y", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Y axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 1D array to get into the imageware
	*/
	public void getBoundedY(int x, int y, int z, short[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (y < 0 ? -y : 0);
			int offset = (x) + (y+iinf)*nx;
			int leni = buffer.length;
			if (x < 0) 		return;
			if (y+leni < 0) return;
			if (z < 0)		return;
			int isup = (y+leni >= ny ? ny-y : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (short)(tmp[offset]);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_get("Y", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Y axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 1D array to get into the imageware
	*/
	public void getBoundedY(int x, int y, int z, float[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (y < 0 ? -y : 0);
			int offset = (x) + (y+iinf)*nx;
			int leni = buffer.length;
			if (x < 0) 		return;
			if (y+leni < 0) return;
			if (z < 0)		return;
			int isup = (y+leni >= ny ? ny-y : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (float)(tmp[offset]);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_get("Y", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Y axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 1D array to get into the imageware
	*/
	public void getBoundedY(int x, int y, int z, double[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (y < 0 ? -y : 0);
			int offset = (x) + (y+iinf)*nx;
			int leni = buffer.length;
			if (x < 0) 		return;
			if (y+leni < 0) return;
			if (z < 0)		return;
			int isup = (y+leni >= ny ? ny-y : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (double)(tmp[offset]);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_get("Y", "Bounded check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in Z axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 1D array to get into the imageware
	*/
	public void getBoundedZ(int x, int y, int z, byte[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (z < 0 ? -z : 0);
			int k = z+iinf;
			int offset = (x) + (y)*nx;
			int leni = buffer.length;
			if (x < 0)		return;
			if (y < 0)		return;
			if (z+leni < 0)	return;
			int isup = (z+leni >= nz ? nz-z : leni);
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (byte)(((float[])data[k])[offset]);
				k++;
			}
		}
		catch(Exception e) {
			throw_get("Z", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Z axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 1D array to get into the imageware
	*/
	public void getBoundedZ(int x, int y, int z, short[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (z < 0 ? -z : 0);
			int k = z+iinf;
			int offset = (x) + (y)*nx;
			int leni = buffer.length;
			if (x < 0)		return;
			if (y < 0)		return;
			if (z+leni < 0)	return;
			int isup = (z+leni >= nz ? nz-z : leni);
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (short)(((float[])data[k])[offset]);
				k++;
			}
		}
		catch(Exception e) {
			throw_get("Z", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Z axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 1D array to get into the imageware
	*/
	public void getBoundedZ(int x, int y, int z, float[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (z < 0 ? -z : 0);
			int k = z+iinf;
			int offset = (x) + (y)*nx;
			int leni = buffer.length;
			if (x < 0)		return;
			if (y < 0)		return;
			if (z+leni < 0)	return;
			int isup = (z+leni >= nz ? nz-z : leni);
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (float)(((float[])data[k])[offset]);
				k++;
			}
		}
		catch(Exception e) {
			throw_get("Z", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Z axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 1D array to get into the imageware
	*/
	public void getBoundedZ(int x, int y, int z, double[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (z < 0 ? -z : 0);
			int k = z+iinf;
			int offset = (x) + (y)*nx;
			int leni = buffer.length;
			if (x < 0)		return;
			if (y < 0)		return;
			if (z+leni < 0)	return;
			int isup = (z+leni >= nz ? nz-z : leni);
			for (int i=iinf; i<isup; i++) {
				buffer[i] = (double)(((float[])data[k])[offset]);
				k++;
			}
		}
		catch(Exception e) {
			throw_get("Z", "Bounded check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in XY axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 2D array to get into the imageware
	*/
	public void getBoundedXY(int x, int y, int z, byte[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0)	return;
			if (z < 0)		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			float[] tmp = (float[])data[z];
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y+j)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (byte)(tmp[offset]);
					offset++;
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XY axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 2D array to get into the imageware
	*/
	public void getBoundedXY(int x, int y, int z, short[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0)	return;
			if (z < 0)		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			float[] tmp = (float[])data[z];
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y+j)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (short)(tmp[offset]);
					offset++;
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XY axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 2D array to get into the imageware
	*/
	public void getBoundedXY(int x, int y, int z, float[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0)	return;
			if (z < 0)		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			float[] tmp = (float[])data[z];
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y+j)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (float)(tmp[offset]);
					offset++;
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XY axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 2D array to get into the imageware
	*/
	public void getBoundedXY(int x, int y, int z, double[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0)	return;
			if (z < 0)		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			float[] tmp = (float[])data[z];
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y+j)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (double)(tmp[offset]);
					offset++;
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Bounded check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in XZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 2D array to get into the imageware
	*/
	public void getBoundedXZ(int x, int y, int z, byte[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int	iinf = (x < 0 ? -x : 0);
			int	jinf = (z < 0 ? -z : 0);
			int	k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y < 0)		return;
			if (z+lenj < 0) return;
			int	isup = (x+leni >= nx ? nx-x : leni);
			int	jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (byte)(((float[])data[z])[offset]);
					offset++;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 2D array to get into the imageware
	*/
	public void getBoundedXZ(int x, int y, int z, short[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int	iinf = (x < 0 ? -x : 0);
			int	jinf = (z < 0 ? -z : 0);
			int	k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y < 0)		return;
			if (z+lenj < 0) return;
			int	isup = (x+leni >= nx ? nx-x : leni);
			int	jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (short)(((float[])data[z])[offset]);
					offset++;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 2D array to get into the imageware
	*/
	public void getBoundedXZ(int x, int y, int z, float[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int	iinf = (x < 0 ? -x : 0);
			int	jinf = (z < 0 ? -z : 0);
			int	k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y < 0)		return;
			if (z+lenj < 0) return;
			int	isup = (x+leni >= nx ? nx-x : leni);
			int	jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (float)(((float[])data[z])[offset]);
					offset++;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 2D array to get into the imageware
	*/
	public void getBoundedXZ(int x, int y, int z, double[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int	iinf = (x < 0 ? -x : 0);
			int	jinf = (z < 0 ? -z : 0);
			int	k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y < 0)		return;
			if (z+lenj < 0) return;
			int	isup = (x+leni >= nx ? nx-x : leni);
			int	jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (double)(((float[])data[z])[offset]);
					offset++;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Bounded check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in YZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 2D array to get into the imageware
	*/
	public void getBoundedYZ(int x, int y, int z, byte[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (y < 0 ? -y : 0);
			int jinf = (z < 0 ? -z : 0);
			int k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x < 0)		return;
			if (y+leni < 0) return;
			if (z+lenj < 0) return;
			int isup = (y+leni >= ny ? ny-y : leni);
			int jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = x + (y+iinf)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (byte)(((float[])data[z])[offset]);
					offset+=nx;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in YZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 2D array to get into the imageware
	*/
	public void getBoundedYZ(int x, int y, int z, short[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (y < 0 ? -y : 0);
			int jinf = (z < 0 ? -z : 0);
			int k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x < 0)		return;
			if (y+leni < 0) return;
			if (z+lenj < 0) return;
			int isup = (y+leni >= ny ? ny-y : leni);
			int jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = x + (y+iinf)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (short)(((float[])data[z])[offset]);
					offset+=nx;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in YZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 2D array to get into the imageware
	*/
	public void getBoundedYZ(int x, int y, int z, float[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (y < 0 ? -y : 0);
			int jinf = (z < 0 ? -z : 0);
			int k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x < 0)		return;
			if (y+leni < 0) return;
			if (z+lenj < 0) return;
			int isup = (y+leni >= ny ? ny-y : leni);
			int jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = x + (y+iinf)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (float)(((float[])data[z])[offset]);
					offset+=nx;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in YZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 2D array to get into the imageware
	*/
	public void getBoundedYZ(int x, int y, int z, double[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (y < 0 ? -y : 0);
			int jinf = (z < 0 ? -z : 0);
			int k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x < 0)		return;
			if (y+leni < 0) return;
			if (z+lenj < 0) return;
			int isup = (y+leni >= ny ? ny-y : leni);
			int jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = x + (y+iinf)*nx;
				for (int i=iinf; i<isup; i++) {
					buffer[i][j] = (double)(((float[])data[z])[offset]);
					offset+=nx;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Bounded check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in XYZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 3D array to get into the imageware
	*/
	public void getBoundedXYZ(int x, int y, int z, byte[][][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int kinf = (z < 0 ? -z : 0);
			int ko = z+kinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0) return;
			if (z+lenk < 0) return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			int ksup = (z+lenk >= nz ? nz-z : lenk);
			for (int k=kinf; k<ksup; k++) {
				float[] tmp = (float[])data[ko];
				for (int j=jinf; j<jsup; j++) {
					offset = (x+iinf) + (y+j)*nx;
					for (int i=iinf; i<isup; i++) {
						buffer[i][j][k] = (byte)(tmp[offset]);
						offset++;
					}
				}
				ko++;
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XYZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 3D array to get into the imageware
	*/
	public void getBoundedXYZ(int x, int y, int z, short[][][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int kinf = (z < 0 ? -z : 0);
			int ko = z+kinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0) return;
			if (z+lenk < 0) return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			int ksup = (z+lenk >= nz ? nz-z : lenk);
			for (int k=kinf; k<ksup; k++) {
				float[] tmp = (float[])data[ko];
				for (int j=jinf; j<jsup; j++) {
					offset = (x+iinf) + (y+j)*nx;
					for (int i=iinf; i<isup; i++) {
						buffer[i][j][k] = (short)(tmp[offset]);
						offset++;
					}
				}
				ko++;
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XYZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 3D array to get into the imageware
	*/
	public void getBoundedXYZ(int x, int y, int z, float[][][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int kinf = (z < 0 ? -z : 0);
			int ko = z+kinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0) return;
			if (z+lenk < 0) return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			int ksup = (z+lenk >= nz ? nz-z : lenk);
			for (int k=kinf; k<ksup; k++) {
				float[] tmp = (float[])data[ko];
				for (int j=jinf; j<jsup; j++) {
					offset = (x+iinf) + (y+j)*nx;
					for (int i=iinf; i<isup; i++) {
						buffer[i][j][k] = (float)(tmp[offset]);
						offset++;
					}
				}
				ko++;
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XYZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 3D array to get into the imageware
	*/
	public void getBoundedXYZ(int x, int y, int z, double[][][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int kinf = (z < 0 ? -z : 0);
			int ko = z+kinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0) return;
			if (z+lenk < 0) return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			int ksup = (z+lenk >= nz ? nz-z : lenk);
			for (int k=kinf; k<ksup; k++) {
				float[] tmp = (float[])data[ko];
				for (int j=jinf; j<jsup; j++) {
					offset = (x+iinf) + (y+j)*nx;
					for (int i=iinf; i<isup; i++) {
						buffer[i][j][k] = (double)(tmp[offset]);
						offset++;
					}
				}
				ko++;
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Bounded check", buffer, x, y, z);
		}
	}

	
	
	//------------------------------------------------------------------
	//
	// getBlock section
	//
	//------------------------------------------------------------------

	
	/**
	* Get an array from the imageware at the start position (x,y,z) in X axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockX(int x, int y, int z, byte[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			float[] tmp = (float[])data[zp];
			for (int i=0; i<leni; i++) {
				xp = x + i;
				while (xp < 0)
					xp += xperiod;
				while (xp >= nx) {
					xp = xperiod - xp;
					xp = (xp < 0 ? -xp : xp);
				}
				buffer[i] = (byte)(tmp[xp + yp]);
			}
		}
		catch(Exception e) {
			throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in X axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockX(int x, int y, int z, short[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			float[] tmp = (float[])data[zp];
			for (int i=0; i<leni; i++) {
				xp = x + i;
				while (xp < 0)
					xp += xperiod;
				while (xp >= nx) {
					xp = xperiod - xp;
					xp = (xp < 0 ? -xp : xp);
				}
				buffer[i] = (short)(tmp[xp + yp]);
			}
		}
		catch(Exception e) {
			throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in X axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockX(int x, int y, int z, float[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			float[] tmp = (float[])data[zp];
			for (int i=0; i<leni; i++) {
				xp = x + i;
				while (xp < 0)
					xp += xperiod;
				while (xp >= nx) {
					xp = xperiod - xp;
					xp = (xp < 0 ? -xp : xp);
				}
				buffer[i] = (float)(tmp[xp + yp]);
			}
		}
		catch(Exception e) {
			throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in X axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockX(int x, int y, int z, double[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			float[] tmp = (float[])data[zp];
			for (int i=0; i<leni; i++) {
				xp = x + i;
				while (xp < 0)
					xp += xperiod;
				while (xp >= nx) {
					xp = xperiod - xp;
					xp = (xp < 0 ? -xp : xp);
				}
				buffer[i] = (double)(tmp[xp + yp]);
			}
		}
		catch(Exception e) {
			throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the start position (x,y,z) in Y axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockY(int x, int y, int z, byte[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			float[] tmp = (float[])data[zp];
			for (int i=0; i<leni; i++) {
				yp = y + i;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				buffer[i] = (byte)(tmp[xp + yp*nx]);
			}
		}
		catch(Exception e) {
			throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in Y axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockY(int x, int y, int z, short[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			float[] tmp = (float[])data[zp];
			for (int i=0; i<leni; i++) {
				yp = y + i;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				buffer[i] = (short)(tmp[xp + yp*nx]);
			}
		}
		catch(Exception e) {
			throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in Y axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockY(int x, int y, int z, float[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			float[] tmp = (float[])data[zp];
			for (int i=0; i<leni; i++) {
				yp = y + i;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				buffer[i] = (float)(tmp[xp + yp*nx]);
			}
		}
		catch(Exception e) {
			throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in Y axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockY(int x, int y, int z, double[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			float[] tmp = (float[])data[zp];
			for (int i=0; i<leni; i++) {
				yp = y + i;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				buffer[i] = (double)(tmp[xp + yp*nx]);
			}
		}
		catch(Exception e) {
			throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the start position (x,y,z) in Z axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockZ(int x, int y, int z, byte[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			int xyp = xp + yp*nx;
			for (int i=0; i<leni; i++) {
				zp = z + i;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				buffer[i] = (byte)(((float[])data[zp])[xyp]);
			}
		}
		catch(Exception e) {
			throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in Z axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockZ(int x, int y, int z, short[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			int xyp = xp + yp*nx;
			for (int i=0; i<leni; i++) {
				zp = z + i;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				buffer[i] = (short)(((float[])data[zp])[xyp]);
			}
		}
		catch(Exception e) {
			throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in Z axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockZ(int x, int y, int z, float[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			int xyp = xp + yp*nx;
			for (int i=0; i<leni; i++) {
				zp = z + i;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				buffer[i] = (float)(((float[])data[zp])[xyp]);
			}
		}
		catch(Exception e) {
			throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in Z axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockZ(int x, int y, int z, double[] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			int xyp = xp + yp*nx;
			for (int i=0; i<leni; i++) {
				zp = z + i;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				buffer[i] = (double)(((float[])data[zp])[xyp]);
			}
		}
		catch(Exception e) {
			throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* get an array into the imageware at the start position (x,y,z) in XY axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXY(int x, int y, int z, byte[][] buffer, byte boundaryConditions) {

		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			float[] tmp = (float[])data[zp];
			for (int j=0; j<lenj; j++) {
				yp = y + j;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				yp *= nx;
				for (int i=0; i<leni; i++) {
					xp = x + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (byte)(tmp[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* get an array into the imageware at the start position (x,y,z) in XY axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXY(int x, int y, int z, short[][] buffer, byte boundaryConditions) {

		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			float[] tmp = (float[])data[zp];
			for (int j=0; j<lenj; j++) {
				yp = y + j;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				yp *= nx;
				for (int i=0; i<leni; i++) {
					xp = x + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (short)(tmp[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* get an array into the imageware at the start position (x,y,z) in XY axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXY(int x, int y, int z, float[][] buffer, byte boundaryConditions) {

		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			float[] tmp = (float[])data[zp];
			for (int j=0; j<lenj; j++) {
				yp = y + j;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				yp *= nx;
				for (int i=0; i<leni; i++) {
					xp = x + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (float)(tmp[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* get an array into the imageware at the start position (x,y,z) in XY axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXY(int x, int y, int z, double[][] buffer, byte boundaryConditions) {

		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			float[] tmp = (float[])data[zp];
			for (int j=0; j<lenj; j++) {
				yp = y + j;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				yp *= nx;
				for (int i=0; i<leni; i++) {
					xp = x + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (double)(tmp[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the start position (x,y,z) in XZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXZ(int x, int y, int z, byte[][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			for (int j=0; j<lenj; j++) {
				zp = z + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - yp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					xp = x + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (byte)(((float[])data[zp])[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in XZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXZ(int x, int y, int z, short[][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			for (int j=0; j<lenj; j++) {
				zp = z + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - yp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					xp = x + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (short)(((float[])data[zp])[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in XZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXZ(int x, int y, int z, float[][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			for (int j=0; j<lenj; j++) {
				zp = z + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - yp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					xp = x + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (float)(((float[])data[zp])[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in XZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXZ(int x, int y, int z, double[][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			for (int j=0; j<lenj; j++) {
				zp = z + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - yp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					xp = x + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (double)(((float[])data[zp])[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the start position (x,y,z) in YZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockYZ(int x, int y, int z, byte[][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			for (int j=0; j<lenj; j++) {
				zp = z + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					yp = y + i;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					buffer[i][j] = (byte)(((float[])data[zp])[xp + yp*nx]);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in YZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockYZ(int x, int y, int z, short[][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			for (int j=0; j<lenj; j++) {
				zp = z + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					yp = y + i;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					buffer[i][j] = (short)(((float[])data[zp])[xp + yp*nx]);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in YZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockYZ(int x, int y, int z, float[][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			for (int j=0; j<lenj; j++) {
				zp = z + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					yp = y + i;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					buffer[i][j] = (float)(((float[])data[zp])[xp + yp*nx]);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in YZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockYZ(int x, int y, int z, double[][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			for (int j=0; j<lenj; j++) {
				zp = z + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					yp = y + i;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					buffer[i][j] = (double)(((float[])data[zp])[xp + yp*nx]);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the start position (x,y,z) in XYZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 3D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXYZ(int x, int y, int z, byte[][][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++) {
				zp = z + k;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				float[] tmp = (float[])data[zp];
				for (int j=0; j<lenj; j++) {
					yp = y + j;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					yp *= nx;
					for (int i=0; i<leni; i++) {
						xp = x + i;
						while (xp < 0)
							xp += xperiod;
						while (xp >= nx) {
							xp = xperiod - xp;
							xp = (xp < 0 ? -xp : xp);
						}
						buffer[i][j][k] = (byte)(tmp[xp + yp]);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in XYZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 3D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXYZ(int x, int y, int z, short[][][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++) {
				zp = z + k;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				float[] tmp = (float[])data[zp];
				for (int j=0; j<lenj; j++) {
					yp = y + j;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					yp *= nx;
					for (int i=0; i<leni; i++) {
						xp = x + i;
						while (xp < 0)
							xp += xperiod;
						while (xp >= nx) {
							xp = xperiod - xp;
							xp = (xp < 0 ? -xp : xp);
						}
						buffer[i][j][k] = (short)(tmp[xp + yp]);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in XYZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 3D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXYZ(int x, int y, int z, float[][][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++) {
				zp = z + k;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				float[] tmp = (float[])data[zp];
				for (int j=0; j<lenj; j++) {
					yp = y + j;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					yp *= nx;
					for (int i=0; i<leni; i++) {
						xp = x + i;
						while (xp < 0)
							xp += xperiod;
						while (xp >= nx) {
							xp = xperiod - xp;
							xp = (xp < 0 ? -xp : xp);
						}
						buffer[i][j][k] = (float)(tmp[xp + yp]);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the start position (x,y,z) in XYZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 3D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getBlockXYZ(int x, int y, int z, double[][][] buffer, byte boundaryConditions) {
		int xperiod=0;
		int yperiod=0;
		int zperiod=0;
		switch(boundaryConditions) {
			case ImageWare.MIRROR:
				xperiod = (nx <= 1 ? 1: 2*nx - 2);
				yperiod = (ny <= 1 ? 1: 2*ny - 2);
				zperiod = (nz <= 1 ? 1: 2*nz - 2);
				break;
			case ImageWare.PERIODIC:	
				xperiod = nx;
				yperiod = ny;
				zperiod = nz;
				break;
			default:
				throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
		
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++) {
				zp = z + k;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				float[] tmp = (float[])data[zp];
				for (int j=0; j<lenj; j++) {
					yp = y + j;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					yp *= nx;
					for (int i=0; i<leni; i++) {
						xp = x + i;
						while (xp < 0)
							xp += xperiod;
						while (xp >= nx) {
							xp = xperiod - xp;
							xp = (xp < 0 ? -xp : xp);
						}
						buffer[i][j][k] = (double)(tmp[xp + yp]);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	
	//------------------------------------------------------------------
	//
	// getBlock section
	//
	//------------------------------------------------------------------

	
	/**
	* Get an array from the imageware at the center position (x,y,z) in X axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodX(int x, int y, int z, byte[] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			int xs = x - leni/2;
			float[] tmp = ((float[])data[zp]);
			for (int i=0; i<leni; i++) {
				xp = xs + i;
				while (xp < 0)
					xp += xperiod;
				while (xp >= nx) {
					xp = xperiod - xp;
					xp = (xp < 0 ? -xp : xp);
				}
				buffer[i] = (byte)(tmp[xp + yp]);
			}
		}
		catch(Exception e) {
			throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in X axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodX(int x, int y, int z, short[] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			int xs = x - leni/2;
			float[] tmp = ((float[])data[zp]);
			for (int i=0; i<leni; i++) {
				xp = xs + i;
				while (xp < 0)
					xp += xperiod;
				while (xp >= nx) {
					xp = xperiod - xp;
					xp = (xp < 0 ? -xp : xp);
				}
				buffer[i] = (short)(tmp[xp + yp]);
			}
		}
		catch(Exception e) {
			throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in X axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodX(int x, int y, int z, float[] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			int xs = x - leni/2;
			float[] tmp = ((float[])data[zp]);
			for (int i=0; i<leni; i++) {
				xp = xs + i;
				while (xp < 0)
					xp += xperiod;
				while (xp >= nx) {
					xp = xperiod - xp;
					xp = (xp < 0 ? -xp : xp);
				}
				buffer[i] = (float)(tmp[xp + yp]);
			}
		}
		catch(Exception e) {
			throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in X axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodX(int x, int y, int z, double[] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			int xs = x - leni/2;
			float[] tmp = ((float[])data[zp]);
			for (int i=0; i<leni; i++) {
				xp = xs + i;
				while (xp < 0)
					xp += xperiod;
				while (xp >= nx) {
					xp = xperiod - xp;
					xp = (xp < 0 ? -xp : xp);
				}
				buffer[i] = (double)(tmp[xp + yp]);
			}
		}
		catch(Exception e) {
			throw_get("X", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the center position (x,y,z) in Y axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodY(int x, int y, int z, byte[] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			int ys = y - leni/2;
			float[] tmp = ((float[])data[zp]);
			for (int i=0; i<leni; i++) {
				yp = ys + i;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				buffer[i] = (byte)(tmp[xp + yp*nx]);
			}
		}
		catch(Exception e) {
			throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in Y axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodY(int x, int y, int z, short[] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			int ys = y - leni/2;
			float[] tmp = ((float[])data[zp]);
			for (int i=0; i<leni; i++) {
				yp = ys + i;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				buffer[i] = (short)(tmp[xp + yp*nx]);
			}
		}
		catch(Exception e) {
			throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in Y axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodY(int x, int y, int z, float[] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			int ys = y - leni/2;
			float[] tmp = ((float[])data[zp]);
			for (int i=0; i<leni; i++) {
				yp = ys + i;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				buffer[i] = (float)(tmp[xp + yp*nx]);
			}
		}
		catch(Exception e) {
			throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in Y axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodY(int x, int y, int z, double[] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			int ys = y - leni/2;
			float[] tmp = ((float[])data[zp]);
			for (int i=0; i<leni; i++) {
				yp = ys + i;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				buffer[i] = (double)(tmp[xp + yp*nx]);
			}
		}
		catch(Exception e) {
			throw_get("Y", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the center position (x,y,z) in Z axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodZ(int x, int y, int z, byte[] buffer, byte boundaryConditions) {

		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			int xyp = xp + yp*nx;
			int zs = z - leni/2;
			for (int i=0; i<leni; i++) {
				zp = zs + i;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				buffer[i] = (byte)(((float[])data[zp])[xyp]);
			}
		}
		catch(Exception e) {
			throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in Z axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodZ(int x, int y, int z, short[] buffer, byte boundaryConditions) {

		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			int xyp = xp + yp*nx;
			int zs = z - leni/2;
			for (int i=0; i<leni; i++) {
				zp = zs + i;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				buffer[i] = (short)(((float[])data[zp])[xyp]);
			}
		}
		catch(Exception e) {
			throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in Z axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodZ(int x, int y, int z, float[] buffer, byte boundaryConditions) {

		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			int xyp = xp + yp*nx;
			int zs = z - leni/2;
			for (int i=0; i<leni; i++) {
				zp = zs + i;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				buffer[i] = (float)(((float[])data[zp])[xyp]);
			}
		}
		catch(Exception e) {
			throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in Z axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 1D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodZ(int x, int y, int z, double[] buffer, byte boundaryConditions) {

		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			int xyp = xp + yp*nx;
			int zs = z - leni/2;
			for (int i=0; i<leni; i++) {
				zp = zs + i;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				buffer[i] = (double)(((float[])data[zp])[xyp]);
			}
		}
		catch(Exception e) {
			throw_get("Z", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* get an array into the imageware at the center position (x,y,z) in XY axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXY(int x, int y, int z, byte[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			int xs = x - leni/2;
			int ys = y - lenj/2;
			float[] tmp = ((float[])data[zp]);
			for (int j=0; j<lenj; j++) {
				yp = ys + j;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				yp *= nx;
				for (int i=0; i<leni; i++) {
					xp = xs + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (byte)(tmp[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* get an array into the imageware at the center position (x,y,z) in XY axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXY(int x, int y, int z, short[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			int xs = x - leni/2;
			int ys = y - lenj/2;
			float[] tmp = ((float[])data[zp]);
			for (int j=0; j<lenj; j++) {
				yp = ys + j;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				yp *= nx;
				for (int i=0; i<leni; i++) {
					xp = xs + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (short)(tmp[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* get an array into the imageware at the center position (x,y,z) in XY axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXY(int x, int y, int z, float[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			int xs = x - leni/2;
			int ys = y - lenj/2;
			float[] tmp = ((float[])data[zp]);
			for (int j=0; j<lenj; j++) {
				yp = ys + j;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				yp *= nx;
				for (int i=0; i<leni; i++) {
					xp = xs + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (float)(tmp[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* get an array into the imageware at the center position (x,y,z) in XY axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXY(int x, int y, int z, double[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			zp = z;
			while (zp < 0)
				zp += zperiod;
			while (zp >= nz) {
				zp = zperiod - zp;
				zp = (zp < 0 ? -zp : zp);
			}
			int xs = x - leni/2;
			int ys = y - lenj/2;
			float[] tmp = ((float[])data[zp]);
			for (int j=0; j<lenj; j++) {
				yp = ys + j;
				while (yp < 0)
					yp += yperiod;
				while (yp >= ny) {
					yp = yperiod - yp;
					yp = (yp < 0 ? -yp : yp);
				}
				yp *= nx;
				for (int i=0; i<leni; i++) {
					xp = xs + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (double)(tmp[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the center position (x,y,z) in XZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXZ(int x, int y, int z, byte[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			int xs = x - leni/2;
			int zs = z - lenj/2;
			for (int j=0; j<lenj; j++) {
				zp = zs + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - yp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					xp = xs + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (byte)(((float[])data[zp])[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in XZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXZ(int x, int y, int z, short[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			int xs = x - leni/2;
			int zs = z - lenj/2;
			for (int j=0; j<lenj; j++) {
				zp = zs + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - yp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					xp = xs + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (short)(((float[])data[zp])[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in XZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXZ(int x, int y, int z, float[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			int xs = x - leni/2;
			int zs = z - lenj/2;
			for (int j=0; j<lenj; j++) {
				zp = zs + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - yp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					xp = xs + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (float)(((float[])data[zp])[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in XZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXZ(int x, int y, int z, double[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			yp = y;
			while (yp < 0)
				yp += yperiod;
			while (yp >= ny) {
				yp = yperiod - yp;
				yp = (yp < 0 ? -yp : yp);
			}
			yp *= nx;
			int xs = x - leni/2;
			int zs = z - lenj/2;
			for (int j=0; j<lenj; j++) {
				zp = zs + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - yp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					xp = xs + i;
					while (xp < 0)
						xp += xperiod;
					while (xp >= nx) {
						xp = xperiod - xp;
						xp = (xp < 0 ? -xp : xp);
					}
					buffer[i][j] = (double)(((float[])data[zp])[xp + yp]);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the center position (x,y,z) in YZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodYZ(int x, int y, int z, byte[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			int ys = y - leni/2;
			int zs = z - lenj/2;
			for (int j=0; j<lenj; j++) {
				zp = zs + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					yp = ys + i;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					buffer[i][j] = (byte)(((float[])data[zp])[xp + yp*nx]);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in YZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodYZ(int x, int y, int z, short[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			int ys = y - leni/2;
			int zs = z - lenj/2;
			for (int j=0; j<lenj; j++) {
				zp = zs + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					yp = ys + i;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					buffer[i][j] = (short)(((float[])data[zp])[xp + yp*nx]);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in YZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodYZ(int x, int y, int z, float[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			int ys = y - leni/2;
			int zs = z - lenj/2;
			for (int j=0; j<lenj; j++) {
				zp = zs + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					yp = ys + i;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					buffer[i][j] = (float)(((float[])data[zp])[xp + yp*nx]);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in YZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 2D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodYZ(int x, int y, int z, double[][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			xp = x;
			while (xp < 0)
				xp += xperiod;
			while (xp >= nx) {
				xp = xperiod - xp;
				xp = (xp < 0 ? -xp : xp);
			}
			int ys = y - leni/2;
			int zs = z - lenj/2;
			for (int j=0; j<lenj; j++) {
				zp = zs + j;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				for (int i=0; i<leni; i++) {
					yp = ys + i;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					buffer[i][j] = (double)(((float[])data[zp])[xp + yp*nx]);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the center position (x,y,z) in XYZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 3D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXYZ(int x, int y, int z, byte[][][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			int xs = x - leni/2;
			int ys = y - lenj/2;
			int zs = z - lenk/2;
			for (int k=0; k<lenk; k++) {
				zp = zs + k;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				float[] tmp = ((float[])data[zp]);
				for (int j=0; j<lenj; j++) {
					yp = ys + j;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					yp *= nx;
					for (int i=0; i<leni; i++) {
						xp = xs + i;
						while (xp < 0)
							xp += xperiod;
						while (xp >= nx) {
							xp = xperiod - xp;
							xp = (xp < 0 ? -xp : xp);
						}
						buffer[i][j][k] = (byte)(tmp[xp + yp]);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in XYZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 3D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXYZ(int x, int y, int z, short[][][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			int xs = x - leni/2;
			int ys = y - lenj/2;
			int zs = z - lenk/2;
			for (int k=0; k<lenk; k++) {
				zp = zs + k;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				float[] tmp = ((float[])data[zp]);
				for (int j=0; j<lenj; j++) {
					yp = ys + j;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					yp *= nx;
					for (int i=0; i<leni; i++) {
						xp = xs + i;
						while (xp < 0)
							xp += xperiod;
						while (xp >= nx) {
							xp = xperiod - xp;
							xp = (xp < 0 ? -xp : xp);
						}
						buffer[i][j][k] = (short)(tmp[xp + yp]);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in XYZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 3D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXYZ(int x, int y, int z, float[][][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			int xs = x - leni/2;
			int ys = y - lenj/2;
			int zs = z - lenk/2;
			for (int k=0; k<lenk; k++) {
				zp = zs + k;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				float[] tmp = ((float[])data[zp]);
				for (int j=0; j<lenj; j++) {
					yp = ys + j;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					yp *= nx;
					for (int i=0; i<leni; i++) {
						xp = xs + i;
						while (xp < 0)
							xp += xperiod;
						while (xp >= nx) {
							xp = xperiod - xp;
							xp = (xp < 0 ? -xp : xp);
						}
						buffer[i][j][k] = (float)(tmp[xp + yp]);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the center position (x,y,z) in XYZ axis.
	* Apply boundary conditions to get the outside area.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 3D array to get into the imageware
	* @param	boundaryConditions	mirror or periodic boundary conditions
	*/
	public void getNeighborhoodXYZ(int x, int y, int z, double[][][] buffer, byte boundaryConditions) {
		int xperiod = (boundaryConditions == ImageWare.MIRROR ? (nx <= 1 ? 1: 2*nx - 2) : nx);
		int yperiod = (boundaryConditions == ImageWare.MIRROR ? (ny <= 1 ? 1: 2*ny - 2) : ny);
		int zperiod = (boundaryConditions == ImageWare.MIRROR ? (nz <= 1 ? 1: 2*nz - 2) : nz);
		int xp, yp, zp;
		try {
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			int xs = x - leni/2;
			int ys = y - lenj/2;
			int zs = z - lenk/2;
			for (int k=0; k<lenk; k++) {
				zp = zs + k;
				while (zp < 0)
					zp += zperiod;
				while (zp >= nz) {
					zp = zperiod - zp;
					zp = (zp < 0 ? -zp : zp);
				}
				float[] tmp = ((float[])data[zp]);
				for (int j=0; j<lenj; j++) {
					yp = ys + j;
					while (yp < 0)
						yp += yperiod;
					while (yp >= ny) {
						yp = yperiod - yp;
						yp = (yp < 0 ? -yp : yp);
					}
					yp *= nx;
					for (int i=0; i<leni; i++) {
						xp = xs + i;
						while (xp < 0)
							xp += xperiod;
						while (xp >= nx) {
							xp = xperiod - xp;
							xp = (xp < 0 ? -xp : xp);
						}
						buffer[i][j][k] = (double)(tmp[xp + yp]);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "Mirror or periodic boundaray conditions", buffer, x, y, z);
		}
	}


	
	//------------------------------------------------------------------
	//
	// putBounded section
	//
	//------------------------------------------------------------------

	
	/**
	* Put an array into the imageware at the position (x,y,z) in X axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 1D array to put into the imageware
	*/
	public void putBoundedX(int x, int y, int z, byte[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (x < 0 ? -x : 0);
			int offset = (x+iinf) + (y)*nx;
			int leni = buffer.length;
			if (x+leni < 0) return;
			if (y < 0) 		return;
			if (z < 0) 		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {					
				tmp[offset] = (float)(buffer[i] & 0xFF);
				offset++;
			}
		}
		catch(Exception e) {
			throw_put("X", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in X axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 1D array to put into the imageware
	*/
	public void putBoundedX(int x, int y, int z, short[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (x < 0 ? -x : 0);
			int offset = (x+iinf) + (y)*nx;
			int leni = buffer.length;
			if (x+leni < 0) return;
			if (y < 0) 		return;
			if (z < 0) 		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {					
				tmp[offset] = (float)(buffer[i] & 0xFFFF);
				offset++;
			}
		}
		catch(Exception e) {
			throw_put("X", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in X axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 1D array to put into the imageware
	*/
	public void putBoundedX(int x, int y, int z, float[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (x < 0 ? -x : 0);
			int offset = (x+iinf) + (y)*nx;
			int leni = buffer.length;
			if (x+leni < 0) return;
			if (y < 0) 		return;
			if (z < 0) 		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {					
				tmp[offset] = (float)(buffer[i]);
				offset++;
			}
		}
		catch(Exception e) {
			throw_put("X", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in X axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 1D array to put into the imageware
	*/
	public void putBoundedX(int x, int y, int z, double[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (x < 0 ? -x : 0);
			int offset = (x+iinf) + (y)*nx;
			int leni = buffer.length;
			if (x+leni < 0) return;
			if (y < 0) 		return;
			if (z < 0) 		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {					
				tmp[offset] = (float)(buffer[i]);
				offset++;
			}
		}
		catch(Exception e) {
			throw_put("X", "Bounded check", buffer, x, y, z);
		}
	}


	/**
	* Put an array into the imageware at the position (x,y,z) in Y axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 1D array to put into the imageware
	*/
	public void putBoundedY(int x, int y, int z, byte[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (y < 0 ? -y : 0);
			int offset = (x) + (y+iinf)*nx;
			int leni = buffer.length;
			if (x < 0) 		return;
			if (y+leni < 0) return;
			if (z < 0)		return;
			int isup = (y+leni >= ny ? ny-y : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				tmp[offset] = (float)(buffer[i] & 0xFF);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_put("Y", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in Y axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 1D array to put into the imageware
	*/
	public void putBoundedY(int x, int y, int z, short[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (y < 0 ? -y : 0);
			int offset = (x) + (y+iinf)*nx;
			int leni = buffer.length;
			if (x < 0) 		return;
			if (y+leni < 0) return;
			if (z < 0)		return;
			int isup = (y+leni >= ny ? ny-y : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				tmp[offset] = (float)(buffer[i] & 0xFFFF);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_put("Y", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in Y axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 1D array to put into the imageware
	*/
	public void putBoundedY(int x, int y, int z, float[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (y < 0 ? -y : 0);
			int offset = (x) + (y+iinf)*nx;
			int leni = buffer.length;
			if (x < 0) 		return;
			if (y+leni < 0) return;
			if (z < 0)		return;
			int isup = (y+leni >= ny ? ny-y : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				tmp[offset] = (float)(buffer[i]);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_put("Y", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in Y axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 1D array to put into the imageware
	*/
	public void putBoundedY(int x, int y, int z, double[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny) 	return;
			if (z >= nz) 	return;
			int iinf = (y < 0 ? -y : 0);
			int offset = (x) + (y+iinf)*nx;
			int leni = buffer.length;
			if (x < 0) 		return;
			if (y+leni < 0) return;
			if (z < 0)		return;
			int isup = (y+leni >= ny ? ny-y : leni);
			float[] tmp = (float[])data[z];
			for (int i=iinf; i<isup; i++) {
				tmp[offset] = (float)(buffer[i]);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_put("Y", "Bounded check", buffer, x, y, z);
		}
	}


	/**
	* Put an array into the imageware at the position (x,y,z) in Z axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 1D array to put into the imageware
	*/
	public void putBoundedZ(int x, int y, int z, byte[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (z < 0 ? -z : 0);
			int k = z+iinf;
			int offset = (x) + (y)*nx;
			int leni = buffer.length;
			if (x < 0)		return;
			if (y < 0)		return;
			if (z+leni < 0)	return;
			int isup = (z+leni >= nz ? nz-z : leni);
			for (int i=iinf; i<isup; i++) {
				((float[])data[k])[offset] = (float)(buffer[i] & 0xFF);
				k++;
			}
		}
		catch(Exception e) {
			throw_put("Z", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in Z axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 1D array to put into the imageware
	*/
	public void putBoundedZ(int x, int y, int z, short[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (z < 0 ? -z : 0);
			int k = z+iinf;
			int offset = (x) + (y)*nx;
			int leni = buffer.length;
			if (x < 0)		return;
			if (y < 0)		return;
			if (z+leni < 0)	return;
			int isup = (z+leni >= nz ? nz-z : leni);
			for (int i=iinf; i<isup; i++) {
				((float[])data[k])[offset] = (float)(buffer[i] & 0xFFFF);
				k++;
			}
		}
		catch(Exception e) {
			throw_put("Z", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in Z axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 1D array to put into the imageware
	*/
	public void putBoundedZ(int x, int y, int z, float[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (z < 0 ? -z : 0);
			int k = z+iinf;
			int offset = (x) + (y)*nx;
			int leni = buffer.length;
			if (x < 0)		return;
			if (y < 0)		return;
			if (z+leni < 0)	return;
			int isup = (z+leni >= nz ? nz-z : leni);
			for (int i=iinf; i<isup; i++) {
				((float[])data[k])[offset] = (float)(buffer[i]);
				k++;
			}
		}
		catch(Exception e) {
			throw_put("Z", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in Z axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 1D array to put into the imageware
	*/
	public void putBoundedZ(int x, int y, int z, double[] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (z < 0 ? -z : 0);
			int k = z+iinf;
			int offset = (x) + (y)*nx;
			int leni = buffer.length;
			if (x < 0)		return;
			if (y < 0)		return;
			if (z+leni < 0)	return;
			int isup = (z+leni >= nz ? nz-z : leni);
			for (int i=iinf; i<isup; i++) {
				((float[])data[k])[offset] = (float)(buffer[i]);
				k++;
			}
		}
		catch(Exception e) {
			throw_put("Z", "Bounded check", buffer, x, y, z);
		}
	}

		
	/**
	* Put an array into the imageware at the position (x,y,z) in XY axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 2D array to put into the imageware
	*/
	public void putBoundedXY(int x, int y, int z, byte[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0)	return;
			if (z < 0)		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			float[] tmp = (float[])data[z];
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y+j)*nx;
				for (int i=iinf; i<isup; i++) {
					tmp[offset] = (float)(buffer[i][j] & 0xFF);
					offset++;
				}
			}
		}
		catch(Exception e) {
			throw_put("XY", "Bounded check", buffer, x, y, z);
		}
	}
		
	/**
	* Put an array into the imageware at the position (x,y,z) in XY axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 2D array to put into the imageware
	*/
	public void putBoundedXY(int x, int y, int z, short[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0)	return;
			if (z < 0)		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			float[] tmp = (float[])data[z];
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y+j)*nx;
				for (int i=iinf; i<isup; i++) {
					tmp[offset] = (float)(buffer[i][j] & 0xFFFF);
					offset++;
				}
			}
		}
		catch(Exception e) {
			throw_put("XY", "Bounded check", buffer, x, y, z);
		}
	}
		
	/**
	* Put an array into the imageware at the position (x,y,z) in XY axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 2D array to put into the imageware
	*/
	public void putBoundedXY(int x, int y, int z, float[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0)	return;
			if (z < 0)		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			float[] tmp = (float[])data[z];
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y+j)*nx;
				for (int i=iinf; i<isup; i++) {
					tmp[offset] = (float)(buffer[i][j]);
					offset++;
				}
			}
		}
		catch(Exception e) {
			throw_put("XY", "Bounded check", buffer, x, y, z);
		}
	}
		
	/**
	* Put an array into the imageware at the position (x,y,z) in XY axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 2D array to put into the imageware
	*/
	public void putBoundedXY(int x, int y, int z, double[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0)	return;
			if (z < 0)		return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			float[] tmp = (float[])data[z];
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y+j)*nx;
				for (int i=iinf; i<isup; i++) {
					tmp[offset] = (float)(buffer[i][j]);
					offset++;
				}
			}
		}
		catch(Exception e) {
			throw_put("XY", "Bounded check", buffer, x, y, z);
		}
	}


	/**
	* Put an array into the imageware at the position (x,y,z) in XZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 2D array to put into the imageware
	*/
	public void putBoundedXZ(int x, int y, int z, byte[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int	iinf = (x < 0 ? -x : 0);
			int	jinf = (z < 0 ? -z : 0);
			int	k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y < 0)		return;
			if (z+lenj < 0) return;
			int	isup = (x+leni >= nx ? nx-x : leni);
			int	jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y)*nx;
				for (int i=iinf; i<isup; i++) {
					((float[])data[k])[offset] = (float)(buffer[i][j] & 0xFF);
					offset++;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_put("YZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in XZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 2D array to put into the imageware
	*/
	public void putBoundedXZ(int x, int y, int z, short[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int	iinf = (x < 0 ? -x : 0);
			int	jinf = (z < 0 ? -z : 0);
			int	k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y < 0)		return;
			if (z+lenj < 0) return;
			int	isup = (x+leni >= nx ? nx-x : leni);
			int	jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y)*nx;
				for (int i=iinf; i<isup; i++) {
					((float[])data[k])[offset] = (float)(buffer[i][j] & 0xFFFF);
					offset++;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_put("YZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in XZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 2D array to put into the imageware
	*/
	public void putBoundedXZ(int x, int y, int z, float[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int	iinf = (x < 0 ? -x : 0);
			int	jinf = (z < 0 ? -z : 0);
			int	k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y < 0)		return;
			if (z+lenj < 0) return;
			int	isup = (x+leni >= nx ? nx-x : leni);
			int	jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y)*nx;
				for (int i=iinf; i<isup; i++) {
					((float[])data[k])[offset] = (float)(buffer[i][j]);
					offset++;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_put("YZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in XZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 2D array to put into the imageware
	*/
	public void putBoundedXZ(int x, int y, int z, double[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int	iinf = (x < 0 ? -x : 0);
			int	jinf = (z < 0 ? -z : 0);
			int	k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x+leni < 0)	return;
			if (y < 0)		return;
			if (z+lenj < 0) return;
			int	isup = (x+leni >= nx ? nx-x : leni);
			int	jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = (x+iinf) + (y)*nx;
				for (int i=iinf; i<isup; i++) {
					((float[])data[k])[offset] = (float)(buffer[i][j]);
					offset++;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_put("YZ", "Bounded check", buffer, x, y, z);
		}
	}


	/**
	* Put an array into the imageware at the position (x,y,z) in YZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 2D array to put into the imageware
	*/
	public void putBoundedYZ(int x, int y, int z, byte[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (y < 0 ? -y : 0);
			int jinf = (z < 0 ? -z : 0);
			int k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x < 0)		return;
			if (y+leni < 0) return;
			if (z+lenj < 0) return;
			int isup = (y+leni >= ny ? ny-y : leni);
			int jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = x + (y+iinf)*nx;
				for (int i=iinf; i<isup; i++) {
					((float[])data[k])[offset] = (float)(buffer[i][j] & 0xFF);
					offset+=nx;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_put("XZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in YZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 2D array to put into the imageware
	*/
	public void putBoundedYZ(int x, int y, int z, short[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (y < 0 ? -y : 0);
			int jinf = (z < 0 ? -z : 0);
			int k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x < 0)		return;
			if (y+leni < 0) return;
			if (z+lenj < 0) return;
			int isup = (y+leni >= ny ? ny-y : leni);
			int jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = x + (y+iinf)*nx;
				for (int i=iinf; i<isup; i++) {
					((float[])data[k])[offset] = (float)(buffer[i][j] & 0xFFFF);
					offset+=nx;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_put("XZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in YZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 2D array to put into the imageware
	*/
	public void putBoundedYZ(int x, int y, int z, float[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (y < 0 ? -y : 0);
			int jinf = (z < 0 ? -z : 0);
			int k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x < 0)		return;
			if (y+leni < 0) return;
			if (z+lenj < 0) return;
			int isup = (y+leni >= ny ? ny-y : leni);
			int jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = x + (y+iinf)*nx;
				for (int i=iinf; i<isup; i++) {
					((float[])data[k])[offset] = (float)(buffer[i][j]);
					offset+=nx;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_put("XZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in YZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 2D array to put into the imageware
	*/
	public void putBoundedYZ(int x, int y, int z, double[][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (y < 0 ? -y : 0);
			int jinf = (z < 0 ? -z : 0);
			int k = z+jinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			if (x < 0)		return;
			if (y+leni < 0) return;
			if (z+lenj < 0) return;
			int isup = (y+leni >= ny ? ny-y : leni);
			int jsup = (z+lenj >= nz ? nz-z : lenj);
			for (int j=jinf; j<jsup; j++) {
				offset = x + (y+iinf)*nx;
				for (int i=iinf; i<isup; i++) {
					((float[])data[k])[offset] = (float)(buffer[i][j]);
					offset+=nx;
				}
				k++;
			}
		}
		catch(Exception e) {
			throw_put("XZ", "Bounded check", buffer, x, y, z);
		}
	}


	/**
	* Put an array into the imageware at the position (x,y,z) in XYZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 3D array to put into the imageware
	*/
	public void putBoundedXYZ(int x, int y, int z, byte[][][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int kinf = (z < 0 ? -z : 0);
			int ko = z+kinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0) return;
			if (z+lenk < 0) return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			int ksup = (z+lenk >= nz ? nz-z : lenk);
			for (int k=kinf; k<ksup; k++) {
				float[] tmp = (float[])data[ko];
				for (int j=jinf; j<jsup; j++) {
					offset = (x+iinf) + (y+j)*nx;
					for (int i=iinf; i<isup; i++) {
						tmp[offset] = (float)(buffer[i][j][k] & 0xFF);
						offset++;
					}
				}
				ko++;
			}
		}
		catch(Exception e) {
			throw_put("XYZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in XYZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 3D array to put into the imageware
	*/
	public void putBoundedXYZ(int x, int y, int z, short[][][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int kinf = (z < 0 ? -z : 0);
			int ko = z+kinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0) return;
			if (z+lenk < 0) return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			int ksup = (z+lenk >= nz ? nz-z : lenk);
			for (int k=kinf; k<ksup; k++) {
				float[] tmp = (float[])data[ko];
				for (int j=jinf; j<jsup; j++) {
					offset = (x+iinf) + (y+j)*nx;
					for (int i=iinf; i<isup; i++) {
						tmp[offset] = (float)(buffer[i][j][k] & 0xFFFF);
						offset++;
					}
				}
				ko++;
			}
		}
		catch(Exception e) {
			throw_put("XYZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in XYZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 3D array to put into the imageware
	*/
	public void putBoundedXYZ(int x, int y, int z, float[][][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int kinf = (z < 0 ? -z : 0);
			int ko = z+kinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0) return;
			if (z+lenk < 0) return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			int ksup = (z+lenk >= nz ? nz-z : lenk);
			for (int k=kinf; k<ksup; k++) {
				float[] tmp = (float[])data[ko];
				for (int j=jinf; j<jsup; j++) {
					offset = (x+iinf) + (y+j)*nx;
					for (int i=iinf; i<isup; i++) {
						tmp[offset] = (float)(buffer[i][j][k]);
						offset++;
					}
				}
				ko++;
			}
		}
		catch(Exception e) {
			throw_put("XYZ", "Bounded check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in XYZ axis.
	* Copy only the bounded area, intersection between the array and the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 3D array to put into the imageware
	*/
	public void putBoundedXYZ(int x, int y, int z, double[][][] buffer) {
		try {
			if (x >= nx) 	return;
			if (y >= ny)	return;
			if (z >= nz)	return;
			int iinf = (x < 0 ? -x : 0);
			int jinf = (y < 0 ? -y : 0);
			int kinf = (z < 0 ? -z : 0);
			int ko = z+kinf;
			int offset = 0;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			if (x+leni < 0)	return;
			if (y+lenj < 0) return;
			if (z+lenk < 0) return;
			int isup = (x+leni >= nx ? nx-x : leni);
			int jsup = (y+lenj >= ny ? ny-y : lenj);
			int ksup = (z+lenk >= nz ? nz-z : lenk);
			for (int k=kinf; k<ksup; k++) {
				float[] tmp = (float[])data[ko];
				for (int j=jinf; j<jsup; j++) {
					offset = (x+iinf) + (y+j)*nx;
					for (int i=iinf; i<isup; i++) {
						tmp[offset] = (float)(buffer[i][j][k]);
						offset++;
					}
				}
				ko++;
			}
		}
		catch(Exception e) {
			throw_put("XYZ", "Bounded check", buffer, x, y, z);
		}
	}


	
}	// end of class