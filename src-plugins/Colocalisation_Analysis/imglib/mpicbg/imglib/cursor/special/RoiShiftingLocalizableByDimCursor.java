package imglib.mpicbg.imglib.cursor.special;

import java.util.Iterator;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.CursorImpl;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;

/**
 * A RoiShiftingLocalizableByDimCursor allows to specify a mask image for the
 * image to walk on. It is able to move freely within the "on" parts
 * of the mask.
 *
 * Author: Tom Kazimiers
 */
public class RoiShiftingLocalizableByDimCursor< T extends Type<T>> extends CursorImpl<T> implements LocalizableByDimCursor<T> {
	// the original LocalizableByDimCursor to drive
	LocalizableByDimCursor<T> cursor;
	// the offset to apply to requests
	int shiftingOffset[];
	//
	/**
	 * Creates a new RoiShiftingLocalizableByDimCursor, based on a Maskcursor for an
	 * existing image and a mask that is used to drive the cursor. If the mask is
	 * "on" or "off" is specified by the "offValue". Every value in the
	 * mask that is langer than this, will be "on". This variant is localizable,
	 * which means position information can be obtained.
	 *
	 * @param cursor The cursor over which the masked walk should happen.
	 * @param mask The mask for the cursor.
	 * @param offValue The value specifing the "off" state in the mask.
	 */
	public RoiShiftingLocalizableByDimCursor(LocalizableByDimCursor<T> cursor, int offset[]) {
		super( cursor.getImage().getContainer(), cursor.getImage() );
		shiftingOffset = offset;
		this.cursor = cursor;
	}

	@Override
	public String getPositionAsString() {
		return cursor.getPositionAsString();
	}

	@Override
	public void fwd( int dim ) {
		cursor.fwd( dim );
	}

	@Override
	public void bck( int dim ) {
		cursor.bck( dim );
	}

	@Override
	public void setPosition( int position[] ) {
		cursor.setPosition( position );
	}

	@Override
	public RegionOfInterestCursor<T> createRegionOfInterestCursor( final int[] offset,
			final int[] size ) {
		/* a ROI cursor for a MaskedImage must operate on a ROI relative to
		 * the offset of the masks/rois bounding box.
		 */
		int[] shiftedOffset = offset.clone();

		for (int d=0; d < getNumDimensions(); d++) {
			shiftedOffset[d] += shiftingOffset[d];
		}

		return new RegionOfInterestCursor<T>(this, shiftedOffset, size);
	}

	@Override
	public void move( int steps, int dim ) {
		cursor.move( steps, dim );
	}

	@Override
	public void moveTo( Localizable localizable ) {
		cursor.moveTo( localizable );
	}

	@Override
	public void moveTo( int position[] ) {
		cursor.moveTo( position );
	}

	@Override
	public void moveRel( int position[] ) {
		cursor.moveRel( position );
	}

	@Override
	public void setPosition( Localizable localizable ) {
		cursor.setPosition( localizable );
	}

	@Override
	public void setPosition( int position, int dim ) {
		cursor.setPosition( position, dim );
	}

	@Override
	public int getStorageIndex() {
		 return cursor.getStorageIndex();
	}

	@Override
	public void close() {
		cursor.close();
	}

	@Override
	public T getType() {
		return cursor.getType();
	}

	@Override
	public void reset() {
		cursor.reset();
	}

	@Override
	public boolean hasNext() {
		return cursor.hasNext();
	}

	@Override
	public void fwd() {
		cursor.fwd();
	}

	@Override
	public void getPosition( int[] pos ) {
		cursor.getPosition( pos );
	}

	@Override
	public int getPosition( final int dim ) {
		return cursor.getPosition( dim );
	}

	@Override
	public int[] getPosition() {
		return cursor.getPosition();
	}

	/* Not yet implemented methods follow */

	@Override
	public LocalNeighborhoodCursor<T> createLocalNeighborhoodCursor() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}
}
