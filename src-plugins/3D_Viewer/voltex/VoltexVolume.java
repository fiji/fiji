package voltex;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import ij.ImagePlus;
import ij3d.Volume;
import ij.IJ;

import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
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
public class VoltexVolume extends Volume {

	/** The textures' size. These are powers of two. */
	public final int xTexSize, yTexSize, zTexSize;

	/** The texGenScale */
	public final float xTexGenScale, yTexGenScale, zTexGenScale;

	/** The mid point in the data */
	public final Point3d volRefPt = new Point3d();

	/** The ColorModel used for 8-bit textures */
	protected static final ColorModel greyCM = createGreyColorModel();

	/** The ColorModel used for RGB textures */
	protected static final ColorModel rgbCM = createRGBColorModel();

	protected ComponentCreator compCreator;

	private ImageUpdater updater = new ImageUpdater();

	private byte[][] xy;
	private byte[][] xz;
	private byte[][] yz;

	private ImageComponent2D[] xyComp;
	private ImageComponent2D[] xzComp;
	private ImageComponent2D[] yzComp;

	/**
	 * Initializes this Volume with the specified image.
	 * All channels are used.
	 * @param imp
	 */
	public VoltexVolume(ImagePlus imp) {
		this(imp, new boolean[] {true, true, true});
	}

	/**
	 * Initializes this Volume with the specified image and channels.
	 * @param imp
	 * @param ch A boolean[] array of length three, which indicates whether
	 * the red, blue and green channel should be read. This has only an
	 * effct when reading color images.
	 */
	public VoltexVolume(ImagePlus imp, boolean[] ch) {

		super(imp, ch);
		// tex size is next power of two greater than max - min
		// regarding pixels
		xTexSize = powerOfTwo(xDim);
		yTexSize = powerOfTwo(yDim);
		zTexSize = powerOfTwo(zDim);

		float xSpace = (float)pw;
		float ySpace = (float)ph;
		float zSpace = (float)pd;

		// xTexSize is the pixel dim of the file in x-dir, e.g. 256
		// xSpace is the normalised length of a pixel
		xTexGenScale = (float)(1.0 / (xSpace * xTexSize));
		yTexGenScale = (float)(1.0 / (ySpace * yTexSize));
		zTexGenScale = (float)(1.0 / (zSpace * zTexSize));

		// the min and max coords are for the usable area of the texture,
		volRefPt.x = (maxCoord.x + minCoord.x) / 2;
		volRefPt.y = (maxCoord.y + minCoord.y) / 2;
		volRefPt.z = (maxCoord.z + minCoord.z) / 2;

		initLoader2();
		createImageComponents();
		updateData();
	}

	private void createImageComponents() {
		for(int z = 0; z < zDim; z++)
			xyComp[z] = compCreator.createImageComponent(xy[z], xTexSize, yTexSize);
		for(int y = 0; y < yDim; y++)
			xzComp[y] = compCreator.createImageComponent(xz[y], xTexSize, zTexSize);
		for(int x = 0; x < xDim; x++)
			yzComp[x] = compCreator.createImageComponent(yz[x], yTexSize, zTexSize);
	}

	public void updateData() {
		for(int z = 0; z < zDim; z++) {
			loadZ(z, xy[z]);
			xyComp[z].updateData(updater, 0, 0, xTexSize, yTexSize);
		}
		for(int y = 0; y < yDim; y++) {
			loadY(y, xz[y]);
			xzComp[y].updateData(updater, 0, 0, xTexSize, zTexSize);
		}
		for(int x = 0; x < xDim; x++) {
			loadX(x, yz[x]);
			yzComp[x].updateData(updater, 0, 0, yTexSize, zTexSize);
		}
	}

	public ImageComponent2D getImageComponentZ(int index) {
		return xyComp[index];
	}

	public ImageComponent2D getImageComponentY(int index) {
		return xzComp[index];
	}

	public ImageComponent2D getImageComponentX(int index) {
		return yzComp[index];
	}

	public void setNoCheckNoUpdate(int x, int y, int z, int v) {
		((Loader)loader).setNoCheckNoUpdate(x, y, z, v);
	}

	/*
	 * Initializes the specific loader which is used for the current
	 * settings. The choice depends on the specific values of channels,
	 * average and data type.
	 */
	@Override
	protected void initLoader() {
	}

	@Override
	public boolean setAverage(boolean average) {
		if(super.setAverage(average)) {
			initLoader2();
			createImageComponents();
			updateData();
			return true;
		}
		return false;
	}

	@Override
	public boolean setChannels(boolean[] ch) {
		if(super.setChannels(ch)) {
			initLoader2();
			createImageComponents();
			updateData();
			return true;
		}
		return false;
	}

	private void initLoader2() {
		boolean[] c = channels;
		int usedCh = 0;
		for(int i = 0; i < 3; i++)
			if(channels[i]) usedCh++;
		switch(imp.getType()) {
			case ImagePlus.GRAY8:
				loader = new ByteLoader(imp);
				compCreator = new GreyComponentCreator();
				dataType = BYTE_DATA;
				break;
			case ImagePlus.COLOR_RGB:
				if(usedCh == 1) {
					loader = new ByteFromIntLoader(imp, c);
					compCreator = new GreyComponentCreator();
					dataType = BYTE_DATA;
				} else if(usedCh == 2) {
					if(average) {
						loader = new ByteFromIntLoader(imp, c);
						dataType = BYTE_DATA;
						compCreator = new GreyComponentCreator();
					} else {
						loader = new IntFromIntLoader(imp, c);
						compCreator = new ColorComponentCreator();
						dataType = INT_DATA;
					}
				} else {
					if(average) {
						loader = new ByteFromIntLoader(imp, c);
						compCreator = new GreyComponentCreator();
						dataType = BYTE_DATA;
					} else {
						loader = new IntLoader(imp);
						compCreator = new ColorComponentCreator();
						dataType = INT_DATA;
					}
				}
				break;
			default:
				IJ.error("image format not supported");
				break;
		}
	}

	/**
	 * Calculate the next power of two to the given value.
	 * @param value
	 * @return
	 */
	protected static int powerOfTwo(int value) {
		int retval = 1;
		while (retval < value) {
			retval *= 2;
		}
		return retval;
	}

	/**
	 * Loads a xy-slice at the given z position.
	 * @param z
	 * @param dst must be an byte[] array of the correct length.
	 * (If the data type is INT_DATA, it must be 4 times as long).
	 */
	private void loadZ(int z, byte[] dst) {
		((Loader)loader).loadZ(z, dst);
	}

	/**
	 * Loads a xz-slice at the given y position.
	 * @param y
	 * @param dst must be an byte[] array of the correct length.
	 * (If the data type is INT_DATA, it must be 4 times as long).
	 */
	private void loadY(int y, byte[] dst) {
		((Loader)loader).loadY(y, dst);
	}

	/**
	 * Loads a yz-slice at the given x position.
	 * @param x
	 * @param dst must be an byte[] array of the correct length.
	 * (If the data type is INT_DATA, it must be 4 times as long).
	 */
	private void loadX(int x, byte[] dst) {
		((Loader)loader).loadX(x, dst);
	}

	private static final ColorModel createGreyColorModel() {
		byte[] r = new byte[256], g = new byte[256], b = new byte[256];
		for(int i = 0; i < 256; i++)
			r[i] = (byte)i;
		return new IndexColorModel(8, 256, r, g, b);
	}

	private static final ColorModel createRGBColorModel() {
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		int[] nBits = { 8, 8, 8, 8 };
		return new ComponentColorModel(
				cs, nBits, true, false, BufferedImage.TRANSLUCENT,
				DataBuffer.TYPE_BYTE);
	}

	/* **********************************************************************
	 * The ImageUpdater which is needed for dynamically updating the textures
	 ***********************************************************************/

	private class ImageUpdater implements ImageComponent2D.Updater {
		public void updateData(ImageComponent2D comp, int x, int y, int w, int h) {
		}
	}

	/* **********************************************************************
	 * The ComponentCreator interface and implementing classes
	 ***********************************************************************/

	/**
	 * Abstract class which defines the interface for creating the
	 * different ImageComponents.
	 */
	private abstract class ComponentCreator {

		ComponentCreator() {
			xyComp = new ImageComponent2D[zDim];
			xzComp = new ImageComponent2D[yDim];
			yzComp = new ImageComponent2D[xDim];
		}

		/**
		 * Create the ImageComponent2D out of the specified pixel array,
		 * width and height
		 */
		abstract ImageComponent2D createImageComponent(byte[] pix, int w, int h);
	}

	/**
	 * Creates an ImageComponent2D for 8-bit textures.
	 */
	private final class GreyComponentCreator extends ComponentCreator {

		ImageComponent2D createImageComponent(byte[] pix, int w, int h) {
			DataBufferByte db = new DataBufferByte(pix, w * h, 0);
			SampleModel smod = greyCM.createCompatibleSampleModel(w, h);
			WritableRaster raster = Raster.createWritableRaster(smod, db, null);

			BufferedImage bImage = new BufferedImage(
					greyCM, raster, false, null);
			ImageComponent2D bComp = new ImageComponent2D(
					ImageComponent.FORMAT_CHANNEL8, w, h, true, true);
			bComp.setCapability(ImageComponent.ALLOW_IMAGE_WRITE);
			bComp.set(bImage);
			return bComp;
		}
	}

	/**
	 * Loads the ImageComponent2Ds for RGBA-textures.
	 */
	private final class ColorComponentCreator extends ComponentCreator {
		ImageComponent2D createImageComponent(byte[] pix, int w, int h) {
			int[] bandOffset = { 0, 1, 2, 3 };

			DataBufferByte db = new DataBufferByte(pix, w * h * 4, 0);
			WritableRaster raster = Raster.createInterleavedRaster(
							db, w, h, w * 4, 4, bandOffset, null);

			BufferedImage bImage =  new BufferedImage(
					rgbCM, raster, false, null);
			ImageComponent2D bComp = new ImageComponent2D(
					ImageComponent.FORMAT_RGBA, w, h, true, true);
			bComp.setCapability(ImageComponent.ALLOW_IMAGE_WRITE);
			bComp.set(bImage);
			return bComp;
		}
	}

	/* **********************************************************************
	 * The Loader interface and the implementing classes
	 * *********************************************************************/

	/**
	 * Abstract interface for the loader classes.
	 */
	protected interface Loader extends Volume.Loader {
		/**
		 * Loads an xy-slice, with the given z value
		 * (x changes fastest) and stores the data in the provided object
		 */
		void loadZ(int z, byte[] dst);

		/**
		 * Loads an xz-slice, with the given y value
		 * (x changes fastest) and stores the data in the provided object
		 */
		void loadY(int y, byte[] dst);

		/**
		 * Loads an yz-slice, with the given x value
		 * (y changes fastest) and stores the data in the provided object
		 */
		void loadX(int x, byte[] dst);

		/**
		 * Only set the values, without updating the ImageComponent2Ds.
		 */
		void setNoCheckNoUpdate(int x, int y, int z, int v);
	}

	/**
	 * This class loads bytes from byte data.
	 */
	private final class ByteLoader extends Volume.ByteLoader implements Loader {
		ByteLoader(ImagePlus imp) {
			super(imp);
			xy = new byte[zDim][xTexSize * yTexSize];
			xz = new byte[yDim][xTexSize * zTexSize];
			yz = new byte[xDim][yTexSize * zTexSize];
		}

		public void setNoCheck(int x, int y, int z, int v) {
			super.setNoCheck(x, y, z, v);
			xy[z][y * xTexSize + x] = (byte)v;
			xz[y][z * xTexSize + x] = (byte)v;
			yz[x][z * yTexSize + y] = (byte)v;
			xyComp[z].updateData(updater, x, y, 1, 1);
			xzComp[y].updateData(updater, x, z, 1, 1);
			yzComp[x].updateData(updater, y, z, 1, 1);
		}

		public void setNoCheckNoUpdate(int x, int y, int z, int v) {
			super.setNoCheck(x, y, z, v);
		}

		public void set(int x, int y, int z, int v) {
			if(x >= 0 && x < xDim &&
					y >= 0 && y < yDim && z >= 0 && z < zDim) {
				setNoCheck(x, y, z, v);
			}
		}

		public void loadZ(int zValue, byte[] dst) {
			byte[] src = fData[zValue];
			for (int y=0; y < yDim; y++){
				int offsSrc = y * xDim;
				int offsDst = y * xTexSize;
				System.arraycopy(src, offsSrc, dst, offsDst, xDim);
			}
		}

		public void loadY(int yValue, byte[] dst)  {
			for (int z=0; z < zDim; z++){
				byte[] src = fData[z];
				int offsSrc = yValue * xDim;
				int offsDst = z * xTexSize;
				System.arraycopy(src, offsSrc, dst, offsDst, xDim);
			}
		}

		public void loadX(int xValue, byte[] dst)  {
			for (int z=0; z < zDim; z++){
				int offsDst = z * yTexSize;
				for (int y=0; y < yDim; y++){
					int offsSrc = y * xDim + xValue;
					dst[offsDst + y] = fData[z][offsSrc];
				}
			}
		}
	}

	/**
	 * This class loads all channels from int data and returns
	 * it as int array.
	 */
	private final class IntLoader extends Volume.IntLoader implements Loader {
		IntLoader(ImagePlus imp) {
			super(imp);
			xy = new byte[zDim][4 * xTexSize * yTexSize];
			xz = new byte[yDim][4 * xTexSize * zTexSize];
			yz = new byte[xDim][4 * yTexSize * zTexSize];
		}

		public void setNoCheckNoUpdate(int x, int y, int z, int v) {
			super.setNoCheck(x, y, z, v);
		}

		public void setNoCheck(int x, int y, int z, int v) {
			super.setNoCheck(x, y, z, v);
			int a = (v & 0xff000000) >> 24;
			int r = (v & 0xff0000) >> 16;
			int g = (v & 0xff00) >> 8;
			int b = (v & 0xff);

			int i = 4 * (y * xTexSize + x);
			xy[z][i++] = (byte)r;
			xy[z][i++] = (byte)g;
			xy[z][i++] = (byte)b;
			xy[z][i++] = (byte)a;
			xyComp[z].updateData(updater, x, y, 1, 1);

			i = 4 * (z * xTexSize + x);
			xz[y][i++] = (byte)r;
			xz[y][i++] = (byte)g;
			xz[y][i++] = (byte)b;
			xz[y][i++] = (byte)a;
			xzComp[y].updateData(updater, x, z, 1, 1);

			i = 4 * (z * yTexSize + y);
			yz[x][i++] = (byte)r;
			yz[x][i++] = (byte)g;
			yz[x][i++] = (byte)b;
			yz[x][i++] = (byte)a;
			yzComp[x].updateData(updater, y, z, 1, 1);
		}

		public void set(int x, int y, int z, int v) {
			if(x >= 0 && x < xDim &&
					y >= 0 && y < yDim && z >= 0 && z < zDim) {
				setNoCheck(x, y, z, v);
			}
		}

		public void loadZ(int zValue, byte[] dst) {
			int[] src = fData[zValue];
			for (int y=0; y < yDim; y++){
				int offsSrc = y * xDim;
				int offsDst = y * xTexSize * 4;
				for(int x = 0; x < xDim; x++) {
					int c = src[offsSrc + x];
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					int a = Math.min(255, r + g + b);
					dst[offsDst++] = (byte)r;
					dst[offsDst++] = (byte)g;
					dst[offsDst++] = (byte)b;
					dst[offsDst++] = (byte)a;
				}
			}
		}

		public void loadY(int yValue, byte[] dst)  {
			for (int z=0; z < zDim; z++){
				int[] src = fData[z];
				int offsSrc = yValue * xDim;
				int offsDst = z * xTexSize * 4;
				for(int x = 0; x < xDim; x++) {
					int c = src[offsSrc + x];
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					int a = Math.min(255, r + g + b);
					dst[offsDst++] = (byte)r;
					dst[offsDst++] = (byte)g;
					dst[offsDst++] = (byte)b;
					dst[offsDst++] = (byte)a;
				}
			}
		}

		public void loadX(int xValue, byte[] dst)  {
			for (int z=0; z < zDim; z++){
				int offsDst = z * yTexSize * 4;
				for (int y=0; y < yDim; y++){
					int offsSrc = y * xDim + xValue;
					int c = fData[z][offsSrc];
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					int a = Math.min(255, r + g + b);
					dst[offsDst++] = (byte)r;
					dst[offsDst++] = (byte)g;
					dst[offsDst++] = (byte)b;
					dst[offsDst++] = (byte)a;
				}
			}
		}
	}

	/**
	 * This class loads the specified channels from int data
	 * This class should only be used if not all channels are
	 * used. Otherwise, it's faster to use the IntLoader.
	 */
	private final class IntFromIntLoader extends Volume.IntFromIntLoader implements Loader {
		IntFromIntLoader(ImagePlus imp, boolean[] channels) {
			super(imp, channels);
			xy = new byte[zDim][4 * xTexSize * yTexSize];
			xz = new byte[yDim][4 * xTexSize * zTexSize];
			yz = new byte[xDim][4 * yTexSize * zTexSize];
		}

		public void setNoCheckNoUpdate(int x, int y, int z, int v) {
			super.setNoCheck(x, y, z, v);
		}

		public void setNoCheck(int x, int y, int z, int v) {
			super.setNoCheck(x, y, z, v);
			int a = (v & 0xff000000) >> 24;
			int r = (v & 0xff0000) >> 16;
			int g = (v & 0xff00) >> 8;
			int b = (v & 0xff);

			int i = 4 * (y * xTexSize + x);
			xy[z][i++] = ch[0] ? (byte)r : 0;
			xy[z][i++] = ch[1] ? (byte)g : 0;
			xy[z][i++] = ch[2] ? (byte)b : 0;
			xy[z][i++] = (byte)a;
			xyComp[z].updateData(updater, x, y, 1, 1);

			i = 4 * (z * xTexSize + x);
			xz[y][i++] = ch[0] ? (byte)r : 0;
			xz[y][i++] = ch[1] ? (byte)g : 0;
			xz[y][i++] = ch[2] ? (byte)b : 0;
			xz[y][i++] = (byte)a;
			xzComp[y].updateData(updater, x, z, 1, 1);

			i = 4 * (z * yTexSize + y);
			yz[x][i++] = ch[0] ? (byte)r : 0;
			yz[x][i++] = ch[1] ? (byte)g : 0;
			yz[x][i++] = ch[2] ? (byte)b : 0;
			yz[x][i++] = (byte)a;
			yzComp[x].updateData(updater, y, z, 1, 1);
		}

		public void set(int x, int y, int z, int v) {
			if(x >= 0 && x < xDim &&
					y >= 0 && y < yDim && z >= 0 && z < zDim) {
				setNoCheck(x, y, z, v);
			}
		}

		public void loadZ(int zValue, byte[] dst) {
			int[] src = fData[zValue];
			for (int y=0; y < yDim; y++){
				int offsSrc = y * xDim;
				int offsDst = y * xTexSize * 4;
				for (int x=0; x < xDim; x++){
					int c = src[offsSrc + x];
					int a = (c & 0xff000000) >> 24;
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					dst[offsDst++] = ch[0] ? (byte)r : 0;
					dst[offsDst++] = ch[1] ? (byte)g : 0;
					dst[offsDst++] = ch[2] ? (byte)b : 0;
					dst[offsDst++] = (byte)a;
				}
			}
		}

		public void loadY(int yValue, byte[] dst)  {
			for (int z=0; z < zDim; z++){
				int[] src = fData[z];
				int offsSrc = yValue * xDim;
				int offsDst = z * xTexSize * 4;
				for (int x=0; x < xDim; x++){
					int c = src[offsSrc + x];
					int a = (c & 0xff000000) >> 24;
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					dst[offsDst++] = ch[0] ? (byte)r : 0;
					dst[offsDst++] = ch[1] ? (byte)g : 0;
					dst[offsDst++] = ch[2] ? (byte)b : 0;
					dst[offsDst++] = (byte)a;
				}
			}
		}

		public void loadX(int xValue, byte[] dst)  {
			for (int z=0; z < zDim; z++){
				int offsDst = z * yTexSize * 4;
				for (int y=0; y < yDim; y++){
					int offsSrc = y * xDim + xValue;
					int c = fData[z][offsSrc];
					int a = (c & 0xff000000) >> 24;
					int r = (c & 0xff0000) >> 16;
					int g = (c & 0xff00) >> 8;
					int b = c & 0xff;
					dst[offsDst++] = ch[0] ? (byte)r : 0;
					dst[offsDst++] = ch[1] ? (byte)g : 0;
					dst[offsDst++] = ch[2] ? (byte)b : 0;
					dst[offsDst++] = (byte)a;
				}
			}
		}
	}

	/**
	 * This class loads from the specified channels an average byte from int
	 * data.
	 */
	private final class ByteFromIntLoader extends Volume.ByteFromIntLoader implements Loader {
		ByteFromIntLoader(ImagePlus imp, boolean[] channels) {
			super(imp, channels);
			xy = new byte[zDim][xTexSize * yTexSize];
			xz = new byte[yDim][xTexSize * zTexSize];
			yz = new byte[xDim][yTexSize * zTexSize];
		}

		public void setNoCheckNoUpdate(int x, int y, int z, int v) {
			super.setNoCheck(x, y, z, v);
		}

		public void setNoCheck(int x, int y, int z, int v) {
			super.setNoCheck(x, y, z, v);
			int l = 0;
			if(channels[0]) l += (v & 0xff0000) >> 16;
			if(channels[1]) l += (v & 0xff00) >> 8;
			if(channels[2]) l += (v & 0xff);
			l /= usedCh;
			xy[z][y * xTexSize + x] = (byte)l;
			xz[y][z * xTexSize + x] = (byte)l;
			yz[x][z * yTexSize + y] = (byte)l;
			xyComp[z].updateData(updater, x, y, 1, 1);
			xzComp[y].updateData(updater, x, z, 1, 1);
			yzComp[x].updateData(updater, y, z, 1, 1);
		}

		public void set(int x, int y, int z, int v) {
			if(x >= 0 && x < xDim &&
					y >= 0 && y < yDim && z >= 0 && z < zDim) {
				setNoCheck(x, y, z, v);
			}
		}

		public void loadZ(int zValue, byte[] dst) {
			int[] src = fdata[zValue];
			for (int y=0; y < yDim; y++){
				int offssrc = y * xDim;
				int offsdst = y * xTexSize;
				for(int x = 0; x < xDim; x++) {
					int v = src[offssrc + x];
					int n = 0;
					if(channels[0]) n += (v&0xff0000)>>16;
					if(channels[1]) n += (v&0xff00)>>8;
					if(channels[2]) n += (v&0xff);
					n /= usedCh;
					dst[offsdst + x] = (byte)n;
				}
			}
		}

		public void loadY(int yValue, byte[] dst)  {
			for (int z=0; z < zDim; z++){
				int[] src = fdata[z];
				int offssrc = yValue * xDim;
				int offsdst = z * xTexSize;
				for(int x = 0; x < xDim; x++) {
					int v = src[offssrc + x];
					int n = 0;
					if(channels[0]) n += (v&0xff0000)>>16;
					if(channels[1]) n += (v&0xff00)>>8;
					if(channels[2]) n += (v&0xff);
					n /= usedCh;
					dst[offsdst + x] = (byte)n;
				}
			}
		}

		public void loadX(int xValue, byte[] dst)  {
			for (int z=0; z < zDim; z++){
				int[] src = fdata[z];
				int offsdst = z * yTexSize;
				for (int y=0; y < yDim; y++){
					int offssrc = y * xDim + xValue;
					int v = src[offssrc];
					int n = 0;
					if(channels[0]) n += (v&0xff0000)>>16;
					if(channels[1]) n += (v&0xff00)>>8;
					if(channels[2]) n += (v&0xff);
					n /= usedCh;
					dst[offsdst + y] = (byte)n;
				}
			}
		}
	}
}
