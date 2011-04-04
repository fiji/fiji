package imageware;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;

/**
 * Class ByteBuffer.
 *
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */

public class ByteBuffer implements Buffer {

	protected Object[] data = null;
	protected  int nx = 0;
	protected int ny = 0;
	protected int nz = 0;
	protected int nxy = 0;
	
	/**
	* Constructor of a empty 3D byte buffer.
	*
	* @param nx		size of the 3D buffer in the X axis
	* @param ny		size of the 3D buffer in the Y axis
	* @param nz		size of the 3D buffer in the Z axis
	*/
	protected ByteBuffer(int nx, int ny, int nz) {
		this.nx = nx;
		this.ny = ny;
		this.nz = nz;
		if (nx <= 0 || ny <= 0 || nz <= 0)
			throw_constructor(nx, ny, nz);
		allocate();
	}
	
	/**
	* Constructor of a byte buffer from a object Image of Java.
	*
	* @param image	source to build a new imageware
	*/
	protected ByteBuffer(Image image, int mode) {
		if (image == null) {
			throw_constructor();
		}
        ImageObserver observer = null;
        this.nx = image.getWidth(observer);
        this.ny = image.getHeight(observer);
        this.nz = 1;
        this.nxy = nx*ny;	        
		byte[] pixels = new byte[nxy];
		PixelGrabber pg = new PixelGrabber(image, 0, 0, nx, ny, false);
		try {
			pg.grabPixels();
			pixels = (byte[])(pg.getPixels());
		} 
		catch (Exception e) {
			throw_constructor();
		}
		allocate();
        for (int k=0; k<nxy; k++)
            ((byte[])data[0])[k] = (byte)(pixels[k] & 0xFF);
	}
	
		
	/**
	* Constructor of a byte buffer from a ImageStack.
	*
	* New data are allocated if the mode is CREATE, the imageware
	* use the data of ImageJ if the mode is WRAP.
	*
	* @param stack	source to build a new imageware
	* @param mode	WRAP or CREATE
	*/
	protected ByteBuffer(ImageStack stack, int mode) {
		if (stack == null) {
			throw_constructor();
		}
		this.nx = stack.getWidth();
		this.ny = stack.getHeight();
		this.nz = stack.getSize();
		this.nxy = nx*ny;
		switch(mode) {
		case ImageWare.WRAP:
			this.data = stack.getImageArray();
			break;
		case ImageWare.CREATE:
			allocate();
			ImageProcessor ip = stack.getProcessor(1);
			if (ip instanceof ByteProcessor) {
				Object[] vol = stack.getImageArray();
				for (int z=0; z<nz; z++) {
					byte[] slice = (byte[])vol[z];
					for (int k=0; k<nxy; k++) {
						((byte[])data[z])[k] = (byte)(slice[k] & 0xFF);
					}
				}
			}
			else if (ip instanceof ShortProcessor) {
				Object[] vol = stack.getImageArray();
				for (int z=0; z<nz; z++) {
					short[] slice = (short[])vol[z];
					for (int k=0; k<nxy; k++) {
						((byte[])data[z])[k] = (byte)(slice[k] & 0xFFFF);
					}
				}
			}
			else if (ip instanceof FloatProcessor) {
				Object[] vol = stack.getImageArray();
				for (int z=0; z<nz; z++) {
					float[] slice = (float[])vol[z];
					for (int k=0; k<nxy; k++) {
						((byte[])data[z])[k] = (byte)slice[k];
					}
				}
			}
			else if (ip instanceof ColorProcessor) {
				double r, g, b;
				int c;
				ColorProcessor cp;
				int[] pixels;
				for (int z=0; z<nz; z++) {
					cp = (ColorProcessor)stack.getProcessor(z+1);
					pixels = (int[])cp.getPixels();
					for (int k=0; k<nxy; k++) {
						c = pixels[k];
						r = (double)((c & 0xFF0000)>>16);
						g = (double)((c & 0xFF00)>>8);
						b = (double)((c & 0xFF));
						((byte[])data[z])[k] = (byte)((r+g+b)/3.0);
					}
				}
			}
			else {
				throw_constructor();
			}
			break;
		default:
			throw_constructor();
			break;
		}	
	}	

		
	/**
	* Constructor of a byte buffer from a specific color channel of ImageStack.
	*
	* New data are always allocated. If it is a gray image the imageware is 
	* created and fill up with data of the source ImageStack. If it is a color
	* image only the selected channel is used to create this imageware.
	*
	* @param stack	source to build a new imageware
	* @param channel	RED, GREEN or BLUE
	*/
	protected ByteBuffer(ImageStack stack, byte channel) {
		if (stack == null) {
			throw_constructor();
		}
		this.nx = stack.getWidth();
		this.ny = stack.getHeight();
		this.nz = stack.getSize();
		this.nxy = nx*ny;
		allocate();
		ImageProcessor ip = stack.getProcessor(1);
		if (ip instanceof ByteProcessor) {
			Object[] vol = stack.getImageArray();
			for (int z=0; z<nz; z++) {
				byte[] slice = (byte[])vol[z];
				for (int k=0; k<nxy; k++) {
					((byte[])data[z])[k] = (byte)(slice[k] & 0xFF);
				}
			}
		}
		else if (ip instanceof ShortProcessor) {
			Object[] vol = stack.getImageArray();
			for (int z=0; z<nz; z++) {
				short[] slice = (short[])vol[z];
				for (int k=0; k<nxy; k++) {
					((byte[])data[z])[k] = (byte)(slice[k] & 0xFFFF);
				}
			}
		}
		else if (ip instanceof FloatProcessor) {
			Object[] vol = stack.getImageArray();
			for (int z=0; z<nz; z++) {
				float[] slice = (float[])vol[z];
				for (int k=0; k<nxy; k++) {
					((byte[])data[z])[k] = (byte)slice[k];
				}
			}
		}
		else if (ip instanceof ColorProcessor) {
			ColorProcessor cp;
			int[] pixels;
			for (int z=0; z<nz; z++) {
				cp = (ColorProcessor)stack.getProcessor(z+1);
				pixels = (int[])cp.getPixels();
				switch(channel) {
				case ImageWare.RED:
					for (int k=0; k<nxy; k++) {
						((byte[])data[z])[k] = (byte)((pixels[k] & 0xFF0000)>>16);
					}
					break;
				case ImageWare.GREEN:
					for (int k=0; k<nxy; k++) {
						((byte[])data[z])[k] = (byte)((pixels[k] & 0xFF00)>>8);
					}
					break;
				case ImageWare.BLUE:
					for (int k=0; k<nxy; k++) {
						((byte[])data[z])[k] = (byte)(pixels[k] & 0xFF);
					}
					break;
				default:
					throw_constructor();
				}
			}
		}
		else {
			throw_constructor();
		}
	}


	/**
	* Constructor of a byte buffer from a byte array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(byte[] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
		this.nx = array.length;
		this.ny = 1;
		this.nz = 1;
		allocate();
		putX(0, 0, 0, array);
	}
	
	/**
	* Constructor of a byte buffer from a byte array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(byte[][] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
		this.nx = array.length;
		this.ny = array[0].length;
		this.nz = 1;
		allocate();
		putXY(0, 0, 0, array);
	}
	
	/**
	* Constructor of a byte buffer from a byte array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(byte[][][] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
		this.nx = array.length;
		this.ny = array[0].length;
		this.nz = array[0][0].length;
		allocate();
		putXYZ(0, 0, 0, array);
	}
	
	/**
	* Constructor of a byte buffer from a short array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(short[] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
			this.nx = array.length;
			this.ny = 1;
			this.nz = 1;
			allocate();
			putX(0, 0, 0, array);
	}
	
	/**
	* Constructor of a byte buffer from a short array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(short[][] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
		this.nx = array.length;
		this.ny = array[0].length;
		this.nz = 1;
		allocate();
		putXY(0, 0, 0, array);
	}
	
	/**
	* Constructor of a byte buffer from a short array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(short[][][] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
		this.nx = array.length;
		this.ny = array[0].length;
		this.nz = array[0][0].length;
		allocate();
		putXYZ(0, 0, 0, array);
	}
	
	/**
	* Constructor of a byte buffer from a float array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(float[] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
		this.nx = array.length;
		this.ny = 1;
		this.nz = 1;
		allocate();
		putX(0, 0, 0, array);
	}
	
	/**
	* Constructor of a byte buffer from a float array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(float[][] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
		this.nx = array.length;
		this.ny = array[0].length;
		this.nz = 1;
		allocate();
		putXY(0, 0, 0, array);
	}
	
	/**
	* Constructor of a byte buffer from a float array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(float[][][] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
		this.nx = array.length;
		this.ny = array[0].length;
		this.nz = array[0][0].length;
		allocate();
		putXYZ(0, 0, 0, array);
	}
	
	/**
	* Constructor of a byte buffer from a double array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(double[] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
		this.nx = array.length;
		this.ny = 1;
		this.nz = 1;
		allocate();
		putX(0, 0, 0, array);
	}
	
	/**
	* Constructor of a byte buffer from a double array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(double[][] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
		this.nx = array.length;
		this.ny = array[0].length;
		this.nz = 1;
		allocate();
		putXY(0, 0, 0, array);
	}
	
	/**
	* Constructor of a byte buffer from a double array.
	*
	* @param array	source to build this new imageware
	*/
	protected ByteBuffer(double[][][] array, int mode) {
		if (array == null) {
			throw_constructor();
		}
		this.nx = array.length;
		this.ny = array[0].length;
		this.nz = array[0][0].length;
		allocate();
		putXYZ(0, 0, 0, array);
	}
		
	/**
	* Return the type of this imageware.
	*
	* @return	the type of this imageware
	*/
	public int getType() {
		return ImageWare.BYTE;
	}
	
	/**
	* Return the type of this imageware in a string format.
	*
	* @return	the type of this imageware translated in a string format
	*/
	public String getTypeToString() {
		return "Byte";
	}
	
	/**
	* Return the number of dimension of this imageware (1, 2 or 3).
	*
	* @return	the number of dimension of this imageware
	*/
	public int getDimension() {
		int dims = 0;
		dims += (nx > 1 ? 1 : 0);
		dims += (ny > 1 ? 1 : 0);
		dims += (nz > 1 ? 1 : 0);
		return dims;
	}

	/**
	* Return the size of the imageware int[0] : x, int[1] : y, int[2] : z.
	*
	* @return	an array given the size of the imageware
	*/
	public int[] getSize() {
		int[] size = {nx, ny, nz};
		return size;
	}

	/**
	* Return the size in the X axis.
	*
	* @return	the size in the X axis
	*/
	public int getSizeX() {
		return nx;
	}
	
	/**
	* Return the size in the Y axis.
	*
	* @return	the size in the Y axis
	*/
	public int getSizeY() {
		return ny;
	}
	
	/**
	* Return the size in the Z axis.
	*
	* @return	the size in the Z axis
	*/
	public int getSizeZ() {
		return nz;
	}
	
	/**
	* Return the size in the X axis.
	*
	* @return	the size in the X axis
	*/
	public int getWidth() {
		return nx;
	}
	
	/**
	* Return the size in the Y axis.
	*
	* @return	the size in the Y axis
	*/
	public int getHeight() {
		return ny;
	}
	
	/**
	* Return the size in the Z axis.
	*
	* @return	the size in the Z axis
	*/
	public int getDepth() {
		return nz;
	}
	
	/**
	* Return the number of pixels in the imageware.
	*
	* @return	number of pixels in the imageware
	*/
	public int getTotalSize() {
		return nxy*nz;
	}
	
	/**
	* Return true is this imageware has the same size the imageware
	* given as parameter.
	*
	* @param	imageware	imageware to be compared
	* @return	true if the imageware of the same size than this imageware
	*/
	public boolean isSameSize(ImageWare imageware) {
		if (nx != imageware.getSizeX())
			return false;
		if (ny != imageware.getSizeY())
			return false;
		if (nz != imageware.getSizeZ())
			return false;
		return true;
	}
	
	//------------------------------------------------------------------
	//
	// put Section
	//
	//------------------------------------------------------------------

		/**
	* Put an array into the imageware at the position (x,y,z) in X axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	ImageWare object to put into the imageware
	*/
	public void putX(int x, int y, int z, ImageWare buffer) {
		int bnx = buffer.getSizeX();
		double buf[] = new double[bnx];
		buffer.getX(0, 0, 0, buf);
		putX(x, y, z, buf);
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in Y axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	ImageWare object to put into the imageware
	*/
	public void putY(int x, int y, int z, ImageWare buffer) {
		int bny = buffer.getSizeY();
		double buf[] = new double[bny];
		buffer.getY(0, 0, 0, buf);
		putY(x, y, z, buf);
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in Z axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	ImageWare object to put into the imageware
	*/
	public void putZ(int x, int y, int z, ImageWare buffer) {
		int bnz = buffer.getSizeZ();
		double buf[] = new double[bnz];
		buffer.getZ(0, 0, 0, buf);
		putZ(x, y, z, buf);
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in XY axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	ImageWare object to put into the imageware
	*/
	public void putXY(int x, int y, int z, ImageWare buffer) {
		int bnx = buffer.getSizeX();
		int bny = buffer.getSizeY();
		double buf[][] = new double[bnx][bny];
		buffer.getXY(0, 0, 0, buf);
		putXY(x, y, z, buf);
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	ImageWare object to put into the imageware
	*/
	public void putXZ(int x, int y, int z, ImageWare buffer) {
		int bnx = buffer.getSizeX();
		int bnz = buffer.getSizeZ();
		double buf[][] = new double[bnx][bnz];
		buffer.getXZ(0, 0, 0, buf);
		putXZ(x, y, z, buf);
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in YZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	ImageWare object to put into the imageware
	*/
	public void putYZ(int x, int y, int z, ImageWare buffer) {
		int bny = buffer.getSizeY();
		int bnz = buffer.getSizeZ();
		double buf[][] = new double[bny][bnz];
		buffer.getYZ(0, 0, 0, buf);
		putYZ(x, y, z, buf);
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XYZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	ImageWare object to put into the imageware
	*/
	public void putXYZ(int x, int y, int z, ImageWare buffer) {
		int bnx = buffer.getSizeX();
		int bny = buffer.getSizeY();
		int bnz = buffer.getSizeZ();
		double buf[][][] = new double[bnx][bny][bnz];
		buffer.getXYZ(0, 0, 0, buf);
		putXYZ(x, y, z, buf);
	}			


	/**
	* Put an array into the imageware at the position (x,y,z) in X axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 1D array to put into the imageware
	*/
	public void putX(int x, int y, int z, byte[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
						
			System.arraycopy(buffer, 0, tmp, offset, leni);
		}
		catch(Exception e) {
			throw_put("X", "No check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in X axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 1D array to put into the imageware
	*/
	public void putX(int x, int y, int z, short[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			
			for (int i=0; i<leni; i++) {
				tmp[offset] = (byte)(buffer[i] & 0xFFFF); 
				offset++; 
			}
		}
		catch(Exception e) {
			throw_put("X", "No check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in X axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 1D array to put into the imageware
	*/
	public void putX(int x, int y, int z, float[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			
			for (int i=0; i<leni; i++) {
				tmp[offset] = (byte)(buffer[i]); 
				offset++; 
			}
		}
		catch(Exception e) {
			throw_put("X", "No check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in X axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 1D array to put into the imageware
	*/
	public void putX(int x, int y, int z, double[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			
			for (int i=0; i<leni; i++) {
				tmp[offset] = (byte)(buffer[i]); 
				offset++; 
			}
		}
		catch(Exception e) {
			throw_put("X", "No check", buffer, x, y, z);
		}
	}


	/**
	* Put an array into the imageware at the position (x,y,z) in Y axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 1D array to put into the imageware
	*/
	public void putY(int x, int y, int z, byte[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			for (int i=0; i<leni; i++) {
				tmp[offset] = (byte)(buffer[i] & 0xFF);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_put("Y", "No check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in Y axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 1D array to put into the imageware
	*/
	public void putY(int x, int y, int z, short[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			for (int i=0; i<leni; i++) {
				tmp[offset] = (byte)(buffer[i] & 0xFFFF);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_put("Y", "No check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in Y axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 1D array to put into the imageware
	*/
	public void putY(int x, int y, int z, float[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			for (int i=0; i<leni; i++) {
				tmp[offset] = (byte)(buffer[i]);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_put("Y", "No check", buffer, x, y, z);
		}
	}

	/**
	* Put an array into the imageware at the position (x,y,z) in Y axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 1D array to put into the imageware
	*/
	public void putY(int x, int y, int z, double[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			for (int i=0; i<leni; i++) {
				tmp[offset] = (byte)(buffer[i]);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_put("Y", "No check", buffer, x, y, z);
		}
	}
	
	
	/**
	* Put an array into the imageware at the position (x,y,z) in Z axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	bybytete 1D array to put into the imageware
	*/
	public void putZ(int x, int y, int z, byte[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			for (int i=0; i<leni; i++) {
				((byte[])data[z])[offset] = (byte)(buffer[i] & 0xFF);
				z++;
			}
		}
		catch(Exception e) {
			throw_put("Z", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in Z axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byshortte 1D array to put into the imageware
	*/
	public void putZ(int x, int y, int z, short[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			for (int i=0; i<leni; i++) {
				((byte[])data[z])[offset] = (byte)(buffer[i] & 0xFFFF);
				z++;
			}
		}
		catch(Exception e) {
			throw_put("Z", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in Z axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byfloatte 1D array to put into the imageware
	*/
	public void putZ(int x, int y, int z, float[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			for (int i=0; i<leni; i++) {
				((byte[])data[z])[offset] = (byte)(buffer[i]);
				z++;
			}
		}
		catch(Exception e) {
			throw_put("Z", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in Z axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	bydoublete 1D array to put into the imageware
	*/
	public void putZ(int x, int y, int z, double[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			for (int i=0; i<leni; i++) {
				((byte[])data[z])[offset] = (byte)(buffer[i]);
				z++;
			}
		}
		catch(Exception e) {
			throw_put("Z", "No check", buffer, x, y, z);
		}
	}
	
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XY axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 2D array to put into the imageware
	*/
	public void putXY(int x, int y, int z, byte[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			byte[] tmp = (byte[])data[z];
			for (int j=0; j<lenj; j++) {
				offset = x + (y+j) * nx;
				for (int i=0; i<leni; i++, offset++) {
					tmp[offset] = (byte)(buffer[i][j] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_put("XY", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XY axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 2D array to put into the imageware
	*/
	public void putXY(int x, int y, int z, short[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			byte[] tmp = (byte[])data[z];
			for (int j=0; j<lenj; j++) {
				offset = x + (y+j) * nx;
				for (int i=0; i<leni; i++, offset++) {
					tmp[offset] = (byte)(buffer[i][j] & 0xFFFF);
				}
			}
		}
		catch(Exception e) {
			throw_put("XY", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XY axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 2D array to put into the imageware
	*/
	public void putXY(int x, int y, int z, float[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			byte[] tmp = (byte[])data[z];
			for (int j=0; j<lenj; j++) {
				offset = x + (y+j) * nx;
				for (int i=0; i<leni; i++, offset++) {
					tmp[offset] = (byte)(buffer[i][j]);
				}
			}
		}
		catch(Exception e) {
			throw_put("XY", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XY axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 2D array to put into the imageware
	*/
	public void putXY(int x, int y, int z, double[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			byte[] tmp = (byte[])data[z];
			for (int j=0; j<lenj; j++) {
				offset = x + (y+j) * nx;
				for (int i=0; i<leni; i++, offset++) {
					tmp[offset] = (byte)(buffer[i][j]);
				}
			}
		}
		catch(Exception e) {
			throw_put("XY", "No check", buffer, x, y, z);
		}
	}
	
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 2D array to put into the imageware
	*/
	public void putXZ(int x, int y, int z, byte[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++) {
				offset = x + j*nx;
				for (int i=0; i<leni; i++, offset++) {
					((byte[])data[z])[offset] = (byte)(buffer[i][j] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_put("YZ", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 2D array to put into the imageware
	*/
	public void putXZ(int x, int y, int z, short[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++) {
				offset = x + j*nx;
				for (int i=0; i<leni; i++, offset++) {
					((byte[])data[z])[offset] = (byte)(buffer[i][j] & 0xFFFF);
				}
			}
		}
		catch(Exception e) {
			throw_put("YZ", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 2D array to put into the imageware
	*/
	public void putXZ(int x, int y, int z, float[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++) {
				offset = x + j*nx;
				for (int i=0; i<leni; i++, offset++) {
					((byte[])data[z])[offset] = (byte)(buffer[i][j]);
				}
			}
		}
		catch(Exception e) {
			throw_put("YZ", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 2D array to put into the imageware
	*/
	public void putXZ(int x, int y, int z, double[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++) {
				offset = x + j*nx;
				for (int i=0; i<leni; i++, offset++) {
					((byte[])data[z])[offset] = (byte)(buffer[i][j]);
				}
			}
		}
		catch(Exception e) {
			throw_put("YZ", "No check", buffer, x, y, z);
		}
	}
	
	
	/**
	* Put an array into the imageware at the position (x,y,z) in YZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 2D array to put into the imageware
	*/
	public void putYZ(int x, int y, int z, byte[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++, offset=(x+nx*y))
			for (int i=0; i<leni; i++, offset+=nx) {
				((byte[])data[z])[offset] = (byte)(buffer[i][j] & 0xFF);
			}
		}
		catch(Exception e) {
			throw_put("XZ", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in YZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 2D array to put into the imageware
	*/
	public void putYZ(int x, int y, int z, short[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++, offset=(x+nx*y))
			for (int i=0; i<leni; i++, offset+=nx) {
				((byte[])data[z])[offset] = (byte)(buffer[i][j] & 0xFFFF);
			}
		}
		catch(Exception e) {
			throw_put("XZ", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in YZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 2D array to put into the imageware
	*/
	public void putYZ(int x, int y, int z, float[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++, offset=(x+nx*y))
			for (int i=0; i<leni; i++, offset+=nx) {
				((byte[])data[z])[offset] = (byte)(buffer[i][j]);
			}
		}
		catch(Exception e) {
			throw_put("XZ", "No check", buffer, x, y, z);
		}
	}
	
	/**
	* Put an array into the imageware at the position (x,y,z) in YZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 2D array to put into the imageware
	*/
	public void putYZ(int x, int y, int z, double[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++, offset=(x+nx*y))
			for (int i=0; i<leni; i++, offset+=nx) {
				((byte[])data[z])[offset] = (byte)(buffer[i][j]);
			}
		}
		catch(Exception e) {
			throw_put("XZ", "No check", buffer, x, y, z);
		}
	}
	
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XYZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	byte 3D array to put into the imageware
	*/
	public void putXYZ(int x, int y, int z, byte[][][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++, z++) {
				byte[] tmp = (byte[])data[z];
				for (int j=0; j<lenj; j++) {
					offset = x + (j+y)*nx;
					for (int i=0; i<leni; i++, offset++) {
						tmp[offset] = (byte)(buffer[i][j][k] & 0xFF);
					}
				}
			}
		}
		catch(Exception e) {
			throw_put("XYZ", "No check", buffer, x, y, z);
		}
	}			
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XYZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	short 3D array to put into the imageware
	*/
	public void putXYZ(int x, int y, int z, short[][][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++, z++) {
				byte[] tmp = (byte[])data[z];
				for (int j=0; j<lenj; j++) {
					offset = x + (j+y)*nx;
					for (int i=0; i<leni; i++, offset++) {
						tmp[offset] = (byte)(buffer[i][j][k] & 0xFFFF);
					}
				}
			}
		}
		catch(Exception e) {
			throw_put("XYZ", "No check", buffer, x, y, z);
		}
	}			
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XYZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	float 3D array to put into the imageware
	*/
	public void putXYZ(int x, int y, int z, float[][][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++, z++) {
				byte[] tmp = (byte[])data[z];
				for (int j=0; j<lenj; j++) {
					offset = x + (j+y)*nx;
					for (int i=0; i<leni; i++, offset++) {
						tmp[offset] = (byte)(buffer[i][j][k]);
					}
				}
			}
		}
		catch(Exception e) {
			throw_put("XYZ", "No check", buffer, x, y, z);
		}
	}			
	
	/**
	* Put an array into the imageware at the position (x,y,z) in XYZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to put the buffer
	* @param	y		Y starting position to put the buffer
	* @param	z		Z starting position to put the buffer
	* @param	buffer	double 3D array to put into the imageware
	*/
	public void putXYZ(int x, int y, int z, double[][][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++, z++) {
				byte[] tmp = (byte[])data[z];
				for (int j=0; j<lenj; j++) {
					offset = x + (j+y)*nx;
					for (int i=0; i<leni; i++, offset++) {
						tmp[offset] = (byte)(buffer[i][j][k]);
					}
				}
			}
		}
		catch(Exception e) {
			throw_put("XYZ", "No check", buffer, x, y, z);
		}
	}			
	


	//------------------------------------------------------------------
	//
	// get Section
	//
	//------------------------------------------------------------------

		/**
	* Get an array from the imageware at the position (x,y,z) in X axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	ImageWare object to get into the imageware
	*/
	public void getX(int x, int y, int z, ImageWare buffer) {
		int bnx = buffer.getSizeX();
		double buf[] = new double[bnx];
		getX(x, y, z, buf);
		buffer.putX(0, 0, 0, buf);
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Y axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	ImageWare object to get into the imageware
	*/
	public void getY(int x, int y, int z, ImageWare buffer) {
		int bny = buffer.getSizeY();
		double buf[] = new double[bny];
		getY(x, y, z, buf);
		buffer.putY(0, 0, 0, buf);
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Z axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	ImageWare object to get into the imageware
	*/
	public void getZ(int x, int y, int z, ImageWare buffer) {
		int bnz = buffer.getSizeZ();
		double buf[] = new double[bnz];
		getZ(x, y, z, buf);
		buffer.putZ(0, 0, 0, buf);
	}

	/**
	* get an array into the imageware at the position (x,y,z) in XY axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	ImageWare object to get into the imageware
	*/
	public void getXY(int x, int y, int z, ImageWare buffer) {
		int bnx = buffer.getSizeX();
		int bny = buffer.getSizeY();
		double buf[][] = new double[bnx][bny];
		getXY(x, y, z, buf);
		buffer.putXY(0, 0, 0, buf);
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	ImageWare object to get into the imageware
	*/
	public void getXZ(int x, int y, int z, ImageWare buffer) {
		int bnx = buffer.getSizeX();
		int bnz = buffer.getSizeZ();
		double buf[][] = new double[bnx][bnz];
		getXZ(x, y, z, buf);
		buffer.putXZ(0, 0, 0, buf);
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in YZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	ImageWare object to get into the datase
	*/
	public void getYZ(int x, int y, int z, ImageWare buffer) {
		int bny = buffer.getSizeY();
		int bnz = buffer.getSizeZ();
		double buf[][] = new double[bny][bnz];
		getYZ(x, y, z, buf);
		buffer.putYZ(0, 0, 0, buf);
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XYZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	ImageWare object to get into the imageware
	*/
	public void getXYZ(int x, int y, int z, ImageWare buffer) {
		int bnx = buffer.getSizeX();
		int bny = buffer.getSizeY();
		int bnz = buffer.getSizeZ();
		double buf[][][] = new double[bnx][bny][bnz];
		getXYZ(x, y, z, buf);
		buffer.putXYZ(0, 0, 0, buf);
	}



	/**
	* Get an array from the imageware at the position (x,y,z) in X axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 1D array to get into the imageware
	*/
	public void getX(int x, int y, int z, byte[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
						
			System.arraycopy(tmp, offset, buffer, 0, leni);
		}
		catch(Exception e) {
			throw_get("X", "No check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in X axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 1D array to get into the imageware
	*/
	public void getX(int x, int y, int z, short[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			
			for (int i=0; i<leni; i++) {
				buffer[i] = (short)(tmp[offset] & 0xFF);
				offset++;
			}
		}
		catch(Exception e) {
			throw_get("X", "No check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in X axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 1D array to get into the imageware
	*/
	public void getX(int x, int y, int z, float[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			
			for (int i=0; i<leni; i++) {
				buffer[i] = (float)(tmp[offset] & 0xFF);
				offset++;
			}
		}
		catch(Exception e) {
			throw_get("X", "No check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in X axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 1D array to get into the imageware
	*/
	public void getX(int x, int y, int z, double[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			
			for (int i=0; i<leni; i++) {
				buffer[i] = (double)(tmp[offset] & 0xFF);
				offset++;
			}
		}
		catch(Exception e) {
			throw_get("X", "No check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in Y axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 1D array to get into the imageware
	*/
	public void getY(int x, int y, int z, byte[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			for (int i=0; i<leni; i++) {
				buffer[i] = (byte)(tmp[offset] & 0xFF);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_get("X", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Y axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 1D array to get into the imageware
	*/
	public void getY(int x, int y, int z, short[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			for (int i=0; i<leni; i++) {
				buffer[i] = (short)(tmp[offset] & 0xFF);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_get("X", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Y axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 1D array to get into the imageware
	*/
	public void getY(int x, int y, int z, float[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			for (int i=0; i<leni; i++) {
				buffer[i] = (float)(tmp[offset] & 0xFF);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_get("X", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Y axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 1D array to get into the imageware
	*/
	public void getY(int x, int y, int z, double[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			byte[] tmp = (byte[])data[z];
			for (int i=0; i<leni; i++) {
				buffer[i] = (double)(tmp[offset] & 0xFF);
				offset+=nx;
			}
		}
		catch(Exception e) {
			throw_get("X", "No check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in Z axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 1D array to get into the imageware
	*/
	public void getZ(int x, int y, int z, byte[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			for (int i=0; i<leni; i++) {
				buffer[i] = (byte)(((byte[])data[z])[offset] & 0xFF);
				z++;
			}
		}
		catch(Exception e) {
			throw_get("Y", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Z axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 1D array to get into the imageware
	*/
	public void getZ(int x, int y, int z, short[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			for (int i=0; i<leni; i++) {
				buffer[i] = (short)(((byte[])data[z])[offset] & 0xFF);
				z++;
			}
		}
		catch(Exception e) {
			throw_get("Y", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Z axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 1D array to get into the imageware
	*/
	public void getZ(int x, int y, int z, float[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			for (int i=0; i<leni; i++) {
				buffer[i] = (float)(((byte[])data[z])[offset] & 0xFF);
				z++;
			}
		}
		catch(Exception e) {
			throw_get("Y", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in Z axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 1D array to get into the imageware
	*/
	public void getZ(int x, int y, int z, double[] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			for (int i=0; i<leni; i++) {
				buffer[i] = (double)(((byte[])data[z])[offset] & 0xFF);
				z++;
			}
		}
		catch(Exception e) {
			throw_get("Y", "No check", buffer, x, y, z);
		}
	}


	/**
	* get an array into the imageware at the position (x,y,z) in XY axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 2D array to get into the imageware
	*/
	public void getXY(int x, int y, int z, byte[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			byte[] tmp = (byte[])data[z];
			for (int j=0; j<lenj; j++) {
				offset = x + (y+j) * nx;
				for (int i=0; i<leni; i++, offset++) {
					buffer[i][j] = (byte)(tmp[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "No check", buffer, x, y, z);
		}
	}

	/**
	* get an array into the imageware at the position (x,y,z) in XY axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 2D array to get into the imageware
	*/
	public void getXY(int x, int y, int z, short[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			byte[] tmp = (byte[])data[z];
			for (int j=0; j<lenj; j++) {
				offset = x + (y+j) * nx;
				for (int i=0; i<leni; i++, offset++) {
					buffer[i][j] = (short)(tmp[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "No check", buffer, x, y, z);
		}
	}

	/**
	* get an array into the imageware at the position (x,y,z) in XY axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 2D array to get into the imageware
	*/
	public void getXY(int x, int y, int z, float[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			byte[] tmp = (byte[])data[z];
			for (int j=0; j<lenj; j++) {
				offset = x + (y+j) * nx;
				for (int i=0; i<leni; i++, offset++) {
					buffer[i][j] = (float)(tmp[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "No check", buffer, x, y, z);
		}
	}

	/**
	* get an array into the imageware at the position (x,y,z) in XY axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 2D array to get into the imageware
	*/
	public void getXY(int x, int y, int z, double[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			byte[] tmp = (byte[])data[z];
			for (int j=0; j<lenj; j++) {
				offset = x + (y+j) * nx;
				for (int i=0; i<leni; i++, offset++) {
					buffer[i][j] = (double)(tmp[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("XY", "No check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in XZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 2D array to get into the imageware
	*/
	public void getXZ(int x, int y, int z, byte[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++) {
				offset = x + y*nx;
				for (int i=0; i<leni; i++, offset++) {
					buffer[i][j] = (byte)(((byte[])data[z])[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 2D array to get into the imageware
	*/
	public void getXZ(int x, int y, int z, short[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++) {
				offset = x + y*nx;
				for (int i=0; i<leni; i++, offset++) {
					buffer[i][j] = (short)(((byte[])data[z])[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 2D array to get into the imageware
	*/
	public void getXZ(int x, int y, int z, float[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++) {
				offset = x + y*nx;
				for (int i=0; i<leni; i++, offset++) {
					buffer[i][j] = (float)(((byte[])data[z])[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 2D array to get into the imageware
	*/
	public void getXZ(int x, int y, int z, double[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++) {
				offset = x + y*nx;
				for (int i=0; i<leni; i++, offset++) {
					buffer[i][j] = (double)(((byte[])data[z])[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("XZ", "No check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in YZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 2D array to get into the datase
	*/
	public void getYZ(int x, int y, int z, byte[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++, offset=(x+nx*y)) {
				for (int i=0; i<leni; i++, offset+=nx) {
					buffer[i][j] = (byte)(((byte[])data[z])[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in YZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 2D array to get into the datase
	*/
	public void getYZ(int x, int y, int z, short[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++, offset=(x+nx*y)) {
				for (int i=0; i<leni; i++, offset+=nx) {
					buffer[i][j] = (short)(((byte[])data[z])[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in YZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 2D array to get into the datase
	*/
	public void getYZ(int x, int y, int z, float[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++, offset=(x+nx*y)) {
				for (int i=0; i<leni; i++, offset+=nx) {
					buffer[i][j] = (float)(((byte[])data[z])[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in YZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 2D array to get into the datase
	*/
	public void getYZ(int x, int y, int z, double[][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			for (int j=0; j<lenj; j++, z++, offset=(x+nx*y)) {
				for (int i=0; i<leni; i++, offset+=nx) {
					buffer[i][j] = (double)(((byte[])data[z])[offset] & 0xFF);
				}
			}
		}
		catch(Exception e) {
			throw_get("YZ", "No check", buffer, x, y, z);
		}
	}


	/**
	* Get an array from the imageware at the position (x,y,z) in XYZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	byte 3D array to get into the imageware
	*/
	public void getXYZ(int x, int y, int z, byte[][][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++, z++) {
				byte[] tmp = (byte[])data[z];
				for (int j=0; j<lenj; j++) {
					offset = x + (j+y)*nx;
					for (int i=0; i<leni; i++, offset++) {
						buffer[i][j][k] = (byte)(tmp[offset] & 0xFF);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XYZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	short 3D array to get into the imageware
	*/
	public void getXYZ(int x, int y, int z, short[][][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++, z++) {
				byte[] tmp = (byte[])data[z];
				for (int j=0; j<lenj; j++) {
					offset = x + (j+y)*nx;
					for (int i=0; i<leni; i++, offset++) {
						buffer[i][j][k] = (short)(tmp[offset] & 0xFF);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XYZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	float 3D array to get into the imageware
	*/
	public void getXYZ(int x, int y, int z, float[][][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++, z++) {
				byte[] tmp = (byte[])data[z];
				for (int j=0; j<lenj; j++) {
					offset = x + (j+y)*nx;
					for (int i=0; i<leni; i++, offset++) {
						buffer[i][j][k] = (float)(tmp[offset] & 0xFF);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "No check", buffer, x, y, z);
		}
	}

	/**
	* Get an array from the imageware at the position (x,y,z) in XYZ axis.
	* No check are performed if the array is outside of the imageware.
	*
	* @param	x		X starting position to get the buffer
	* @param	y		Y starting position to get the buffer
	* @param	z		Z starting position to get the buffer
	* @param	buffer	double 3D array to get into the imageware
	*/
	public void getXYZ(int x, int y, int z, double[][][] buffer) {
		try {
			int offset = x+y*nx;
			int leni = buffer.length;
			int lenj = buffer[0].length;
			int lenk = buffer[0][0].length;
			for (int k=0; k<lenk; k++, z++) {
				byte[] tmp = (byte[])data[z];
				for (int j=0; j<lenj; j++) {
					offset = x + (j+y)*nx;
					for (int i=0; i<leni; i++, offset++) {
						buffer[i][j][k] = (double)(tmp[offset] & 0xFF);
					}
				}
			}
		}
		catch(Exception e) {
			throw_get("XYZ", "No check", buffer, x, y, z);
		}
	}



		
	//------------------------------------------------------------------
	//
	//	Private Section
	//
	//------------------------------------------------------------------
	
	/**
	* Prepare a complete error message from the errors coming the constructors.
	*/
	protected void throw_constructor() {
		throw new ArrayStoreException(
			"\n-------------------------------------------------------\n" +
			"Error in imageware package\n" +
			"Unable to create a byte imageware.\n" +
			"-------------------------------------------------------\n"
		);
	}
	
	/**
	* Prepare a complete error message from the errors coming the constructors.
	*/
	protected void throw_constructor(int nx, int ny, int nz) {
		throw new ArrayStoreException(
			"\n-------------------------------------------------------\n" +
			"Error in imageware package\n" +
			"Unable to create a byte imageware " + 
			nx + "," + ny + "," + nz + "].\n" +
			"-------------------------------------------------------\n"
		);
	}

	/**
	* Prepare a complete error message from the errors coming the get routines.
	*/
	protected void throw_get(String direction, String border, Object buffer, int x, int y, int z) {
		int leni = 0;
		int lenj = 0;
		int lenk = 0;
		String type = " unknown type";
		if (buffer instanceof byte[]) {
			leni = ((byte[])buffer).length;
			type = " 1D byte";
		}
		else if (buffer instanceof short[]) {
			leni = ((short[])buffer).length;
			type = " 1D short";
		}
		else if (buffer instanceof float[]) {
			leni = ((float[])buffer).length;
			type = " 1D float";
		}
		else if (buffer instanceof double[]) {
			leni = ((double[])buffer).length;
			type = " 1D double";
		}
		else if (buffer instanceof byte[][]) {
			leni = ((byte[][])buffer).length;
			lenj = ((byte[][])buffer)[0].length;
			type = " 2D byte";
		}
		else if (buffer instanceof short[][]) {
			leni = ((short[][])buffer).length;
			lenj = ((short[][])buffer)[0].length;
			type = " 2D short";
		}
		else if (buffer instanceof float[][]) {
			leni = ((float[][])buffer).length;
			lenj = ((float[][])buffer)[0].length;
			type = " 2D float";
		}
		else if (buffer instanceof double[][]) {
			leni = ((double[][])buffer).length;
			lenj = ((double[][])buffer)[0].length;
			type = " 2D double";
		}
		else if (buffer instanceof byte[][][]) {
			leni = ((byte[][][])buffer).length;
			lenj = ((byte[][][])buffer)[0].length;
			lenk = ((byte[][][])buffer)[0][0].length;
			type = " 3D byte";
		}
		else if (buffer instanceof short[][][]) {
			leni = ((short[][][])buffer).length;
			lenj = ((short[][][])buffer)[0].length;
			lenk = ((short[][][])buffer)[0][0].length;
			type = " 3D short";
		}
		else if (buffer instanceof float[][][]) {
			leni = ((float[][][])buffer).length;
			lenj = ((float[][][])buffer)[0].length;
			lenk = ((float[][][])buffer)[0][0].length;
			type = " 3D float";
		}
		else if (buffer instanceof double[][][]) {
			leni = ((double[][][])buffer).length;
			lenj = ((double[][][])buffer)[0].length;
			lenk = ((double[][][])buffer)[0][0].length;
			type = " 3D double";
		}
		throw new ArrayStoreException(
			"\n-------------------------------------------------------\n" +
			"Error in imageware package\n" +
			"Unable to get a" + type + " buffer [" + 
			(leni==0?"":(""+leni)) +  
			(lenj==0?"":(","+lenj)) +  
			(lenk==0?"":(","+lenk)) +  
			"] \n" + 
			"from the byte imageware [" + nx + "," + ny + "," + nz + "]\n" +
			"at the position (" + x + "," + y + "," + z + ") in direction " + 
			direction + "\n" +
			"using " + border + ".\n" +
			"-------------------------------------------------------\n"
		);
	}

	/**
	* Prepare a complete error message from the errors coming the put routines.
	*/
	protected void throw_put(String direction, String border, Object buffer, int x, int y, int z) {
		int leni = 0;
		int lenj = 0;
		int lenk = 0;
		String type = " unknown type";
		if (buffer instanceof byte[]) {
			leni = ((byte[])buffer).length;
			type = " 1D byte";
		}
		else if (buffer instanceof short[]) {
			leni = ((short[])buffer).length;
			type = " 1D short";
		}
		else if (buffer instanceof float[]) {
			leni = ((float[])buffer).length;
			type = " 1D float";
		}
		else if (buffer instanceof double[]) {
			leni = ((double[])buffer).length;
			type = " 1D double";
		}
		else if (buffer instanceof byte[][]) {
			leni = ((byte[][])buffer).length;
			lenj = ((byte[][])buffer)[0].length;
			type = " 2D byte";
		}
		else if (buffer instanceof short[][]) {
			leni = ((short[][])buffer).length;
			lenj = ((short[][])buffer)[0].length;
			type = " 2D short";
		}
		else if (buffer instanceof float[][]) {
			leni = ((float[][])buffer).length;
			lenj = ((float[][])buffer)[0].length;
			type = " 2D float";
		}
		else if (buffer instanceof double[][]) {
			leni = ((double[][])buffer).length;
			lenj = ((double[][])buffer)[0].length;
			type = " 2D double";
		}
		else if (buffer instanceof byte[][][]) {
			leni = ((byte[][][])buffer).length;
			lenj = ((byte[][][])buffer)[0].length;
			lenk = ((byte[][][])buffer)[0][0].length;
			type = " 3D byte";
		}
		else if (buffer instanceof short[][][]) {
			leni = ((short[][][])buffer).length;
			lenj = ((short[][][])buffer)[0].length;
			lenk = ((short[][][])buffer)[0][0].length;
			type = " 3D short";
		}
		else if (buffer instanceof float[][][]) {
			leni = ((float[][][])buffer).length;
			lenj = ((float[][][])buffer)[0].length;
			lenk = ((float[][][])buffer)[0][0].length;
			type = " 3D float";
		}
		else if (buffer instanceof double[][][]) {
			leni = ((double[][][])buffer).length;
			lenj = ((double[][][])buffer)[0].length;
			lenk = ((double[][][])buffer)[0][0].length;
			type = " 3D double";
		}
		throw new ArrayStoreException(
			"\n-------------------------------------------------------\n" +
			"Error in imageware package\n" +
			"Unable to put a" + type + " buffer [" + 
			(leni==0?"":(""+leni)) +  
			(lenj==0?"":(","+lenj)) +  
			(lenk==0?"":(","+lenk)) +  
			"] \n" + 
			"into the byte imageware [" + nx + "," + ny + "," + nz + "]\n" +
			"at the position (" + x + "," + y + "," + z + ") in direction " + 
			direction + "\n" +
			"using " + border + ".\n" +
			"-------------------------------------------------------\n"
		);
	}
	
	//------------------------------------------------------------------
	//
	//	Get slice fast and direct access Section
	//
	//------------------------------------------------------------------

	/**
	* Get a reference of the whole volume data.
	*
	* @return	a reference of the data of this imageware
	*/
	public Object[] getVolume() {
		return data;
	}
		
	/**
	* Get a specific slice, fast and direct access, but only for
	* byte imageware.
	*
	* @param z	number of the requested slice
	* @return	a reference of the data of one slice of this imageware
	*/
	public byte[] getSliceByte(int z) {
		return (byte[])data[z];
	}
	
	/**
	* Get a specific slice, fast and direct access, but only for
	* short imageware.
	*
	* @param z	number of the requested slice
	* @return	a reference of the data of one slice of this imageware
	*/
	public short[] getSliceShort(int z) {
		return null;
	}

	/**
	* Get a specific slice, fast and direct access, but only for
	* float imageware.
	*
	* @param z	number of the requested slice
	* @return	a reference of the data of one slice of this imageware
	*/
	public float[] getSliceFloat(int z) {
		return null;
	}

	/**
	* Get a specific slice, fast and direct access, but only for
	* double imageware.
	*
	* @param z	number of the requested slice
	* @return	a reference of the data of one slice of this imageware
	*/
	public double[] getSliceDouble(int z) {
		return null;
	}
		
	/**
	* Allocate a buffer of size [nx,ny,nz].
	*/
	private void allocate() {
		try {
			this.data = new Object[nz];
			this.nxy = nx*ny;
			for(int z=0; z<nz; z++)
				this.data[z] = new byte[nxy];
		} 
		catch(Exception e) {
			throw_constructor(nx, ny, nz);
		}
	}
	
}	// end of class