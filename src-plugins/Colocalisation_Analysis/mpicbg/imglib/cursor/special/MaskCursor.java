package mpicbg.imglib.cursor.special;

import java.util.Iterator;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.special.meta.AboveThresholdPredicate;
import mpicbg.imglib.cursor.special.meta.AlwaysTruePredicate;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * A MaskCursor allows to specify a mask image for the image to walk
 * over.
 *
 * Author: Tom Kazimiers
 */
public class MaskCursor< T extends Type<T> & Comparable<T> > extends ConstraintCursor<T> {
	// the cursor of the original image
	Cursor<T> imageCursor;
	// the mask image used for driving the cursor
	Image<T> mask;
	// the mask cursor
	Cursor<T> maskCursor;

	/**
	 * Creates a new MaskCursor, based on a cursor over an existing
	 * image and a mask that is used to drive the cursor. If the mask is
	 * "on" or "off" is specified by the "offValue". Every value in the
	 * mask that is langer than this, will be "on".
	 *
	 * It assumes the cursor to be reset and will require ownership
	 * of it, i.e. it will be closed if the MaskCursor gets closed.
	 *
	 * @param cursor The cursor over which the masked walk should happen.
	 * @param mask The mask for the cursor.
	 * @param offValue The value specifing the "off" state in the mask.
	 */
	public MaskCursor(Cursor<T> cursor, Cursor<T> mask, T offValue) {
		super( cursor, mask,
			new AlwaysTruePredicate<T>(),
			new AboveThresholdPredicate<T>( offValue ) );
		// for masking we want the forward mode to be "And"
		setForwardMode( ForwardMode.And );
		imageCursor = cursor;
		this.maskCursor = mask;
		this.mask = mask.getImage();
	}

	@Override
	public boolean hasNext() {
		/* The constraint cursor expects an image of the same size to evaluate
		 * constraints for us in "And" forward mode. If the mask gets out of
		 * bounds we need to handle that. For now we just reset it. This essentially
		 * repeats the mask over and over again until the end of the image is
		 * reached.
		 */
		if ( !super.hasNext() && !maskCursor.hasNext() && imageCursor.hasNext() ) {
			maskCursor.reset();
			hasNextChecked = false;
		}

		return super.hasNext();
	}

	/**
	 * Gets the mask that is used to drive the image cursor.
	 */
	public Image<T> getMask() {
		return mask;
	}

	public Iterator<T> iterator()
	{
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}
	public T getType() {
		return getChannel1Type();
	}

	public Image<T> getImage() {
		return cursor1.getImage();
	}

	public int getArrayIndex() {
		return cursor1.getArrayIndex();
	}

	public int getStorageIndex() {
		return cursor1.getStorageIndex();
	}

	public Container<T> getStorageContainer() {
		return cursor1.getStorageContainer();
	}

	public int[] createPositionArray() {
		return cursor1.createPositionArray();
	}

	public int getNumDimensions() {
		return cursor1.getNumDimensions();
	}

	public int[] getDimensions() {
		return cursor1.getDimensions();
	}

	public void getDimensions( int[] position ) {
		cursor1.getDimensions( position );
	}
}
