package mpicbg.imglib.cursor.special;

import mpicbg.imglib.cursor.CursorImpl;
import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.type.Type;

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
	// indicates if destination position data should be projected
	boolean reducePositions;
	// the number of the dimensions of the underlying cursor
	int dimensions;

	/**
	 * Creates a new RoiShiftingLocalizableByDimCursor, based on
	 * another cursor and an offset. Requests for a RegionOfInterestCursor
	 * will be shifted by this offset. This is especially helpful for
	 * creating ROIs in masked cursors.
	 *
	 * The cursor offers to project position information used as a
	 * destination into the space of the underlying cursor. This can
	 * be helpful if one wants to ignore higher dimensions like if
	 * a mask should be repeated in a stack.
	 *
	 * @param cursor The cursor over which the masked walk should happen.
	 * @param offset The offset by which ROICursor requests get shifted
	 * @param projectPositions
	 */
	public RoiShiftingLocalizableByDimCursor(LocalizableByDimCursor<T> cursor, int offset[], boolean reducePositions) {
		super( cursor.getImage().getContainer(), cursor.getImage() );
		shiftingOffset = offset;
		this.cursor = cursor;
		this.reducePositions = reducePositions;
		dimensions = cursor.getNumDimensions();
	}

	public String getPositionAsString() {
		return cursor.getPositionAsString();
	}

	public void fwd( int dim ) {
		cursor.fwd( dim );
	}

	public void bck( int dim ) {
		cursor.bck( dim );
	}

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

	public void move( int steps, int dim ) {
		cursor.move( steps, dim );
	}

	public void moveTo( Localizable localizable ) {
		cursor.moveTo( localizable );
	}

	public void moveTo( int position[] ) {
		cursor.moveTo( position );
	}

	public void moveRel( int position[] ) {
		cursor.moveRel( position );
	}

	public void setPosition( int position[] ) {
		/* If positions of other dimensionality should be possible
		 * to be set (projectPositions = true), every element of the
		 * positions array form is used separately.
		 */
		if (reducePositions) {
			for (int i=0; i < position.length; i++)
				setPosition(position[i], i);
		} else {
			cursor.setPosition( position );
		}
	}

	public void setPosition( Localizable localizable ) {
		/* If positions of other dimensionality should be possible
		 * to be set (projectPositions = true), the array form of
		 * the position is used and used separately.
		 */
		if (reducePositions)
			setPosition(localizable.getPosition());
		else
			cursor.setPosition( localizable );
	}

	public void setPosition( int position, int dim ) {
		/* If positions of other dimensionality should be possible
		 * to be set (projectPositions = true), the passed position
		 * element is only used if it is within the underlying
		 * cursors bounds. It is ignored otherwise.
		 */
		if (reducePositions) {
			if (dim < dimensions) {
				cursor.setPosition( position, dim );
			}
		} else
			cursor.setPosition( position, dim );
	}

	public int getStorageIndex() {
		 return cursor.getStorageIndex();
	}

	public void close() {
		cursor.close();
	}

	public T getType() {
		return cursor.getType();
	}

	public void reset() {
		cursor.reset();
	}

	public boolean hasNext() {
		return cursor.hasNext();
	}

	public void fwd() {
		cursor.fwd();
	}

	public void getPosition( int[] pos ) {
		cursor.getPosition( pos );
	}

	public int getPosition( final int dim ) {
		return cursor.getPosition( dim );
	}

	public int[] getPosition() {
		return cursor.getPosition();
	}

	/* Not yet implemented methods follow */

	public LocalNeighborhoodCursor<T> createLocalNeighborhoodCursor() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}
}
