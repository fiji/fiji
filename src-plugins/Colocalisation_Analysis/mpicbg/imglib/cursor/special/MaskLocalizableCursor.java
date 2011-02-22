package mpicbg.imglib.cursor.special;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.type.Type;

/**
 * A MaskCursor allows to specify a mask image for the image to walk
 * over.
 *
 * Author: Tom Kazimiers
 */
public class MaskLocalizableCursor< T extends Type<T> & Comparable<T> > extends MaskCursor<T> implements LocalizableCursor<T> {
	// the cursor of the original image
	LocalizableCursor<T> imageCursor;
	// the offset of the masks bounding box
	final int[] offset;

	/**
	 * Creates a new MaskLocalizableCursor, based on a MaskCursor for an
	 * existing image and a mask that is used to drive the cursor. If the mask is
	 * "on" or "off" is specified by the "offValue". Every value in the
	 * mask that is larger than this, will be "on". This variant is localizable,
	 * which means position information can be obtained.
	 *
	 * @param mask The mask for the cursor.
	 * @param cursor The cursor over which the masked walk should happen.
	 * @param offValue The value specifying the "off" state in the mask.
	 */
	public MaskLocalizableCursor(LocalizableCursor<T> cursor, LocalizableByDimCursor<T> mask, T offValue, int[] offset) {
		super( cursor, mask, offValue );
		imageCursor = cursor;
		this.offset = offset;
	}

	public void getPosition( int[] position ) {
		imageCursor.getPosition( position );
		for (int d=0; d < getNumDimensions(); d++) {
			position[d] -= offset[d];
		}
	}

	public int[] getPosition() {
		int[] position = imageCursor.createPositionArray();
		getPosition( position );
		return position;
	}

	public int getPosition( int dim ) {
		return imageCursor.getPosition( dim ) - offset[dim];
	}

	public String getPositionAsString() {
		return imageCursor.getPositionAsString();
	}
}
