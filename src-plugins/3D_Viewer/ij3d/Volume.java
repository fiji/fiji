package ij3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.IJ;
import javax.vecmath.Point3d;

/**
 * This class encapsulates an image stack and provides various methods for
 * retrieving data. It is possible to control the loaded color channels of
 * RGB images, and to specify whether or not to average several channels
 * (and merge them in this way into one byte per pixel).
 * 
 * Depending on these settings, and on the type of image given at construction
 * time, the returned data type is one of INT_DATA or BYTE_DATA.
 * 
 * @author Benjamin Schmid
 */
public class Volume {

	/** Data is read as int data */
	public static final int INT_DATA = 0;
	/** Data is read as byte data */
	public static final int BYTE_DATA = 1;
	
	/** The image holding the data */
	public final ImagePlus imp;

	/** The loader, initialized depending on the data type */
	protected Loader loader;

	/** 
	 * Indicates in which format the data is loaded. This depends on
	 * the image type and on the number of selected channels.
	 * May be one of INT_DATA or BYTE_DATA
	 */
	protected int dataType;

	/** Flag indicating that the channels should be averaged */
	protected boolean average = false;

	/** Channels in RGB images which should be loaded */
	protected boolean[] channels = new boolean[] {true, true, true};

	/** The dimensions of the data */
	public int xDim, yDim, zDim;

	/** The calibration of the data */
	public double pw, ph, pd;


	/** The minimum coordinate of the data */
	public final Point3d minCoord = new Point3d();

	/** The maximum coordinate of the data */
	public final Point3d maxCoord = new Point3d();

	/** Create instance with a null imp. */
	protected Volume() {
		this.imp = null;
	}

	/**
	 * Initializes this Volume with the specified image.
	 * All channels are used.
	 * @param imp
	 */
	public Volume(ImagePlus imp) {
		this(imp, new boolean[] {true, true, true});
	}

	/**
	 * Initializes this Volume with the specified image and channels.
	 * @param imp
	 * @param ch A boolean[] array of length three, which indicates whether
	 * the red, blue and green channel should be read. This has only an
	 * effct when reading color images.
	 */
	public Volume(ImagePlus imp, boolean[] ch) {
		this.channels = ch;
		this.imp = imp;
		xDim = imp.getWidth();
		yDim = imp.getHeight();
		zDim = imp.getStackSize();
		Calibration c = imp.getCalibration();
		pw = c.pixelWidth;
		ph = c.pixelHeight;
		pd = c.pixelDepth;
		
		float xSpace = (float)pw;
		float ySpace = (float)ph;
		float zSpace = (float)pd;

		// real coords
		minCoord.x = c.xOrigin;
		minCoord.y = c.yOrigin;
		minCoord.z = c.zOrigin;

		maxCoord.x = minCoord.x + xDim * xSpace;
		maxCoord.y = minCoord.y + yDim * ySpace;
		maxCoord.z = minCoord.z + zDim * zSpace;

		initLoader();
	}

	/**
	 * Get the current set data type. This is one of BYTE_DATA or INT_DATA.
	 * The data type specifies in which format the data is read:
	 * This method returns INT_DATA, if for example the image is of type
	 * RGB and more than one channels should be read.
	 * If only one channels is read, or if the type of the image is 8-bit,
	 * it will return BYTE_DATA.
	 * @return The type of the returned data.
	 */
	public int getDataType() {
		return dataType;
	}

	/**
	 * If true, build an average byte from the specified channels 
	 * (for each pixel).
	 * @return true if the value for 'average' has changed.
	 */
	public boolean setAverage(boolean a) {
		if(average != a) {
			this.average = a;
			initLoader();
			return true;
		}
		return false;
	}

	/**
	 * Returns true if specified channels are being averaged when
	 * reading the image data.
	 * @return
	 */
	public boolean isAverage() {
		return average;
	}

	/**
	 * Specify the channels which should be read from the image.
	 * This only affects RGB images.
	 * @return true if the channels settings has changed.
	 */
	public boolean setChannels(boolean[] ch) {
		if(ch[0] == channels[0] && 
			ch[1] == channels[1] && 
			ch[2] == channels[2])
			return false;
		channels = ch;
		initLoader();
		return true;
	}

	/*
	 * Initializes the specific loader which is used for the current
	 * settings. The choice depends on the specific values of channels,
	 * average and data type.
	 */
	protected void initLoader() {

		boolean[] c = channels;
		int usedCh = 0;
		for(int i = 0; i < 3; i++)
			if(channels[i]) usedCh++;
		switch(imp.getType()) {
			case ImagePlus.GRAY8:
				loader = new ByteLoader(imp);
				dataType = BYTE_DATA;
				break;
			case ImagePlus.COLOR_RGB:
				if(usedCh == 1) {
					loader = new ByteFromIntLoader(imp, c);
					dataType = BYTE_DATA;
				} else if(usedCh == 2) {
					if(average) {
						loader = new ByteFromIntLoader(imp, c);
						dataType = BYTE_DATA;
					} else {
						loader = new IntFromIntLoader(imp, c);
						dataType = INT_DATA;
					}
				} else {
					if(average) {
						loader = new ByteFromIntLoader(imp, c);
						dataType = BYTE_DATA;
					} else {
						loader = new IntLoader(imp);
						dataType = INT_DATA;
					}
				}
				break;
			default: 
				IJ.error("image format not supported");
				break;
		}
	}

	public void setNoCheck(int x, int y, int z, int v) {
		loader.setNoCheck(x, y, z, v);
	}
	
	public void set(int x, int y, int z, int v) {
		loader.set(x, y, z, v);
	}

	/**
	 * Load the value at the specified position
	 * @param x
	 * @param y
	 * @param z
	 * @return value. Casted to int if it was a byte value before.
	 */
	public int load(int x, int y, int z) {
		return loader.load(x, y, z);
	}

	/**
	 * Abstract interface for the loader classes.
	 */
	protected interface Loader {
		int load(int x, int y, int z);
		void set(int x, int y, int z, int v);
		void setNoCheck(int x, int y, int z, int v);
	}

	/*
	 * This class loads bytes from byte data.
	 */
	protected class ByteLoader implements Loader {
		protected byte[][] fData;
		protected int w;
		
		protected ByteLoader(ImagePlus imp) {
			ImageStack stack = imp.getStack();
			int d = imp.getStackSize();
			w = imp.getWidth();
			fData = new byte[d][];
			for (int z = 0; z < d; z++)
				fData[z] = (byte[])stack.getPixels(z+1);
		}

		public final int load(int x, int y, int z) {
			return (int)fData[z][y * w + x] & 0xff;
		}

		public void setNoCheck(int x, int y, int z, int v) {
			fData[z][y * w + x] = (byte)v;
		}
		
		public void set(int x, int y, int z, int v) {
			if(x >= 0 && x < xDim &&
					y >= 0 && y < yDim && z > 0 && z < zDim) {
				setNoCheck(x, y, z, v);
			}
		}
	}

	/*
	 * This class loads all channels from int data and returns
	 * it as int array.
	 */
	protected class IntLoader implements Loader {
		protected int[][] fData;
		protected int w;

		protected IntLoader(ImagePlus imp) {
			ImageStack stack = imp.getStack();
			int d = imp.getStackSize();
			w = imp.getWidth();
			fData = new int[d][];
			for (int z = 0; z < d; z++)
				fData[z] = (int[])stack.getPixels(z+1);
			adjustAlphaChannel();
		}

		protected final void adjustAlphaChannel() {
			for(int z = 0; z < fData.length; z++) {
				for(int i = 0; i < fData[z].length; i++) {
					int v = fData[z][i];
					int r = (v&0xff0000)>>16;
					int g = (v&0xff00)>>8;
					int b = (v&0xff);
					int a = ((r + g + b) / 3) << 24;
					fData[z][i] = (v & 0xffffff) + a;
				}
			}
		}

		public final int load(int x, int y, int z) {
			return fData[z][y * w + x];
		}

		public void setNoCheck(int x, int y, int z, int v) {
			fData[z][y * w + x] = v;
		}
		
		public void set(int x, int y, int z, int v) {
			if(x >= 0 && x < xDim &&
					y >= 0 && y < yDim && z > 0 && z < zDim) {
				this.setNoCheck(x, y, z, v);
			}
		}
	}

	/*
	 * Loads the specified channels from int data
	 * This class should only be used if not all channels are
	 * used. Otherwise, it's faster to use the IntLoader.
	 */
	protected class IntFromIntLoader implements Loader {
		protected int[][] fData;
		protected int w;
		protected int mask = 0xffffff;
		protected boolean[] ch = new boolean[] {true, true, true};
		protected int usedCh = 3;

		protected IntFromIntLoader(ImagePlus imp, boolean[] channels) {
			ImageStack stack = imp.getStack();
			int d = imp.getStackSize();
			fData = new int[d][];
			for (int z = 0; z < d; z++)
				fData[z] = (int[])stack.getPixels(z+1);

			ch = channels;
			usedCh = 0;
			mask = 0xff000000;
			if(ch[0]) { usedCh++; mask |= 0xff0000; }
			if(ch[1]) { usedCh++; mask |= 0xff00; }
			if(ch[2]) { usedCh++; mask |= 0xff; }
			adjustAlphaChannel();
		}

		protected final void adjustAlphaChannel() {
			for(int z = 0; z < fData.length; z++) {
				for(int i = 0; i < fData[z].length; i++) {
					int v = fData[z][i];
					int n = 0;
					if(ch[0]) n += (v & 0xff0000) >> 16;
					if(ch[1]) n += (v & 0xff00) >> 8;
					if(ch[2]) n += (v & 0xff);
					int a = (n / usedCh) << 24;
					fData[z][i] = (v & 0xffffff) + a;
				}
			}
		}
		
		public void set(int x, int y, int z, int v) {
			if(x >= 0 && x < xDim &&
					y >= 0 && y < yDim && z > 0 && z < zDim) {
				setNoCheck(x, y, z, v);
			}
		}

		public final int load(int x, int y, int z) {
			return fData[z][y * w + x] & mask;
		}

		public void setNoCheck(int x, int y, int z, int v) {
			fData[z][y * w + x] = v;
		}
	}

	/*
	 * Loads from the specified channels an average byte from int
	 * data.
	 */
	protected class ByteFromIntLoader implements Loader {
		protected int[][] fdata;
		protected int w;
		protected boolean[] channels = new boolean[] {true, true, true};
		protected int usedCh = 3;

		protected ByteFromIntLoader(ImagePlus imp, boolean[] channels) {
			this.channels = channels;
			ImageStack stack = imp.getStack();
			int d = imp.getStackSize();
			w = imp.getWidth();
			fdata = new int[d][];
			for (int z = 0; z < d; z++)
				fdata[z] = (int[])stack.getPixels(z+1);
			usedCh = 0;
			for(int i = 0; i < 3; i++)
				if(channels[i]) usedCh++;
		}

		public void setNoCheck(int x, int y, int z, int v) {
			fdata[z][y * w + x] = v;
		}

		public void set(int x, int y, int z, int v) {
			if(x >= 0 && x < xDim &&
					y >= 0 && y < yDim && z > 0 && z < zDim) {
				setNoCheck(x, y, z, v);
			}
		}

		final public int load(int x, int y, int z) {
			int v = fdata[z][y*w + x], n = 0;
			if(channels[0]) n += (v & 0xff0000) >> 16;
			if(channels[1]) n += (v & 0xff00) >> 8;
			if(channels[2]) n += (v & 0xff);
			return (n /= usedCh);
		}
	}
}
