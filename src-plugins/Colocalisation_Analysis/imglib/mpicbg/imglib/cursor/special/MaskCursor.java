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
import imglib.mpicbg.imglib.cursor.special.meta.AlwaysTruePredicate;
import imglib.mpicbg.imglib.cursor.special.meta.AboveThresholdPredicate;

/**
 * A MaskCursor allows to specify a mask image for the image to walk
 * over.
 *
 * Author: Tom Kazimiers
 */
public class MaskCursor< T extends Type<T> & Comparable<T> > extends ConstraintCursor<T> implements LocalizableCursor<T> {
	// the curser of the original image
	LocalizableByDimCursor<T> imageCursor;
	// the mask image used for driving the cursor
	Image<T> mask;

	/**
	 * Creates a new MaskCursor, based on a cursor over an existing
	 * image and a mask that is used to drive the cursor. If the mask is
	 * "on" or "off" is specified by the "offValue". Every value in the
	 * mask that is langer than this, will be "on".
	 *
	 * It resumes the cursor to be reset and will require ownership
	 * of it, i.e. it will be closed if the MaskCursor gets closed.
	 *
	 * @param cursor The cursor over which the masked walk should happen.
	 * @param mask The mask for the cursor.
	 * @param offValue The value specifing the "off" state in the mask.
	 */
	public MaskCursor(LocalizableByDimCursor<T> cursor, Image<T> mask, T offValue) {
		super( cursor, mask.createCursor(),
			new AlwaysTruePredicate(),
			new AboveThresholdPredicate( offValue ) );
		// for masking we want the forward mode to be "And"
		setForwardMode( ForwardMode.And );
		imageCursor = cursor;
		this.mask = mask;
	}

	/**
	 * Gets the mask that is used to drive the image cursor.
	 */
	public Image<T> getMask() {
		return mask;
	}

	@Override
	public Iterator<T> iterator()
	{
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}
	@Override
	public T getType() {
		return getChannel1Type();
	}

	@Override
	public Image<T> getImage() {
		return cursor1.getImage();
	}

	@Override
	public int getArrayIndex() {
		return cursor1.getArrayIndex();
	}

	@Override
	public int getStorageIndex() {
		return cursor1.getStorageIndex();
	}

	@Override
	public Container<T> getStorageContainer() {
		return cursor1.getStorageContainer();
	}

	@Override
	public int[] createPositionArray() {
		return cursor1.createPositionArray();
	}

	@Override
	public int getNumDimensions() {
		return cursor1.getNumDimensions();
	}

	@Override
	public int[] getDimensions() {
		return cursor1.getDimensions();
	}

	@Override
	public void getDimensions( int[] position ) {
		cursor1.getDimensions( position );
	}

	/* Localizable implementatio */

	@Override
	public void getPosition( int[] position ) {
		imageCursor.getPosition( position );
	}

	@Override
	public int[] getPosition() {
		return imageCursor.getPosition();
	}

	@Override
	public int getPosition( int dim ) {
		return imageCursor.getPosition( dim );
	}

	@Override
	public String getPositionAsString() {
		return imageCursor.getPositionAsString();
	}
}

