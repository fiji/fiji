package mpicbg.imglib.cursor.special;

import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.type.Type;

/**
 * A LocalizableByDimCursor for the MaskedImage
 *
 * @author Johannes Schindelin
 */
public class MaskLocalizableByDimCursor< T extends Type<T> & Comparable<T> > extends MaskCursor<T> implements LocalizableByDimCursor<T> {
	// the cursor of the original image
	LocalizableByDimCursor<T> imageCursor;
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
	public MaskLocalizableByDimCursor(LocalizableByDimCursor<T> cursor, LocalizableByDimCursor<T> mask, T offValue, int[] offset) {
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

	public LocalNeighborhoodCursor<T> createLocalNeighborhoodCursor() {
		throw new RuntimeException("TODO");
	}

	public RegionOfInterestCursor<T> createRegionOfInterestCursor( final int[] offset, final int[] size ) {
		throw new RuntimeException("TODO");
	}

	public void setPosition( Localizable localizable ) {
		final int[] position = new int[getNumDimensions()];
		localizable.getPosition(position);
		for (int i = 0; i < position.length; i++)
			position[i] += offset[i];
		imageCursor.setPosition(position);
	}

	public void setPosition( int position[] ) {
		int[] p = new int[getNumDimensions()];
		for (int i = 0; i < p.length; i++)
			p[i] = position[i] + offset[i];
		imageCursor.setPosition(p);
	}

	public void setPosition( int position, int dim ) {
		throw new RuntimeException("TODO"); 
	}

	public void move( int steps, int dim ) {
		throw new RuntimeException("TODO");
	}

	public void moveTo( Localizable localizable ) {
		throw new RuntimeException("TODO");
	}

	public void moveTo( int[] position ) {
		throw new RuntimeException("TODO");
	}

	public void moveRel( int[] position ) {
		throw new RuntimeException("TODO");
	}

	public void fwd( int dim ) {
		throw new RuntimeException("TODO");
	}

	public void bck( int dim ) {
		throw new RuntimeException("TODO");
	}
}