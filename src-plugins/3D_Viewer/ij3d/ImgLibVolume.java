package ij3d;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;

/**
 * This class encapsulates an mpicbg.imglib.Image object, for use in Marching Cubes.
 *
 * @author Albert Cardona
 */
public class ImgLibVolume<T extends RealType<T>> extends Volume {

	final Image<T> img;
	LocalizableByDimCursor<T> cursor = null;

	public ImgLibVolume(final Image<T> img, final float[] origin) throws Exception {
		super();
		if (img.getNumDimensions() < 3) throw new Exception("Image does not support at least 3 dimensions.");

		this.img = img;
		this.xDim = img.getDimension(0);
		this.yDim = img.getDimension(1);
		this.zDim = img.getDimension(2);

		this.pw = img.getCalibration(0);
		this.ph = img.getCalibration(1);
		this.pd = img.getCalibration(2);

		System.out.println("dims: " + xDim + ", " + yDim + ", " + zDim + " :: " + pw +", " + ph + ", " + pd);

		float xSpace = (float)pw;
		float ySpace = (float)ph;
		float zSpace = (float)pd;

		// real coords
		minCoord.x = origin[0];
		minCoord.y = origin[1];
		minCoord.z = origin[2];

		maxCoord.x = minCoord.x + xDim * xSpace;
		maxCoord.y = minCoord.y + yDim * ySpace;
		maxCoord.z = minCoord.z + zDim * zSpace;

		initLoader();
	}

	public Image<T> getImage() {
		return img;
	}

	/** Create the image cursor anew. */
	@Override
	protected void initLoader() {
		final T val = img.createType();
		val.setReal(0);
		this.cursor = img.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<T>(val));
	}

	/** Does nothing. */
	@Override
	public boolean setAverage(boolean a) {
		return false;
	}

	@Override
	public void setNoCheck(int x, int y, int z, int v) {
		cursor.setPosition(x, 0);
		cursor.setPosition(y, 1);
		cursor.setPosition(z, 2);
		cursor.getType().setReal(v);
	}

	@Override
	public void set(int x, int y, int z, int v) {
		setNoCheck(x, y, z, v);
	}

	/**
	 * Load the value at the specified position
	 * @param x
	 * @param y
	 * @param z
	 * @return value. Casted to int if it was a byte value before.
	 */
	public int load(final int x, final int y, final int z) {
		cursor.setPosition(x, 0);
		cursor.setPosition(y, 1);
		cursor.setPosition(z, 2);
		return (int) cursor.getType().getRealFloat();
	}

	protected int dataType = BYTE_DATA;

	public int getDataType() {
		return dataType;
	}

}
