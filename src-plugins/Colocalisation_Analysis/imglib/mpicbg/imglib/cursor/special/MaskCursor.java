package imglib.mpicbg.imglib.cursor.special;

import java.util.Iterator;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.container.Container;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.integer.ByteType;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.Iterable;

/**
 * A MaskCursor allows to specify a mask image for the image to walk
 * over.
 */
public class MaskCursor<T extends Type<T>> implements Cursor<T> {
	// the curser of the original image
	LocalizableByDimCursor<T> imageCursor;
	// the curser of the original image
	LocalizableCursor<ByteType> maskCursor;
	// the mask image used for driving the cursor
	Image<ByteType> mask;

	/**
	 * Creates a new MetaCursor, based on a cursor over the existing
	 * image and a mask that is used to drive the cursor.
	 */
	public MaskCursor(LocalizableByDimCursor cursor, Image<ByteType> mask) {
		imageCursor = cursor;
		// create a cursor for walking over the mask
		maskCursor = mask.createLocalizableCursor();
		this.mask = mask;
	}

	/**
	 * Reset the images and the masks cursors.
	 */
	@Override
	public void reset() {
		imageCursor.reset();
		maskCursor.reset();
	}

	@Override
	public boolean isActive() {
		return imageCursor.isActive() && maskCursor.isActive();
	}

	@Override
	public Image<T> getImage() {
		return imageCursor.getImage();
	}

	/**
	 * Gets the mask that is used to drive the image cursor.
	 */
	public Image<ByteType> getMask() {
		return mask;
	}

	@Override
	public T getType() {
		return imageCursor.getType();
	}

	@Override
	public int getArrayIndex() {
		return imageCursor.getArrayIndex();
	}

	@Override
	public int getStorageIndex() {
		return imageCursor.getStorageIndex();
	}

	@Override
	public Container<T> getStorageContainer() {
		return imageCursor.getStorageContainer();
	}

	@Override
	public void setDebug( final boolean debug ) {
		imageCursor.setDebug( debug );
		maskCursor.setDebug( debug );
	}

	@Override
	public int[] createPositionArray() {
		return imageCursor.createPositionArray();
	}

	@Override
	public void close() {
		imageCursor.close();
		maskCursor.close();
	}

	@Override
	public Iterator<T> iterator()
	{
		return imageCursor.iterator();
	}

	@Override
	public void remove()
	{
		imageCursor.remove();
		maskCursor.remove();
	}

	@Override
	public T next()
	{
		fwd();
		return getType();
	}

	@Override
	public void fwd( final long steps )
	{
		// TODO
	}

	@Override
	public void fwd()
	{
		//if (i
		// TODO
	}

	@Override
	public int getNumDimensions()
	{
		return imageCursor.getNumDimensions();
	}

	@Override
	public int[] getDimensions() {
		return imageCursor.getDimensions();
	}

	@Override
	public void getDimensions( int[] position )
	{
		imageCursor.getDimensions( position );
	}

	@Override
	public boolean hasNext()
	{
		return imageCursor.hasNext();
	}
}
