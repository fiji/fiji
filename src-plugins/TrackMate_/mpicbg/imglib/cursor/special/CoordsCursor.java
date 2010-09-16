package mpicbg.imglib.cursor.special;

import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.type.Type;

/**
 * Abstract cursor that offers facilities to move the cursor <i>origin</i> (whatever this means
 * for the concrete implementation) by giving <b>physical</b> coordinates, converted
 * using a calibration <code>float[]</code> array. It is intended for concrete cursors 
 * than aim at being used as a ROI. 
 * @author Jean-Yves Tinevez
 */
public abstract class CoordsCursor <T extends Type<T>> extends AbstractSpecialCursor<T>	 {

	/*
	 * FIELDS
	 */
	
	/**
	 * Contain the pixel size for each of the cursor dimension.
	 */
	protected float[] calibration;
	/**
	 * Contain the origin of volume iterated by this cursor, (whatever this means
	 * for the concrete implementation). This origin is in pixel coordinates.
	 */
	protected int[] origin;
	/**
	 * Contain the relative position of the cursor, with respect to the {@link #origin},
	 * in pixel coordinates.
	 */
	protected int[] position;
	
	
	/*
	 * METHODS
	 */
	

	/**
	 * Move the center of the sphere to the location specified by the {@link Localizable} object. 
	 * This <b>resets</b> this cursor.
	 */
	public final void moveCenterTo(final Localizable localizable) {
		localizable.getPosition(origin);
		reset();
	}
	
	/**
	 * Move the center of the sphere to the pixel location specified by the array, in <b>pixel coordinates</b>.
	 * This <b>resets</b> this cursor.
	 */
	public final void moveCenterToPosition(final int[] icenter) {
		origin = icenter.clone();
		reset();
	}
	
	/**
	 * Move the center of the sphere to the location specified by the array, in <b>physical coordinates</b>.
	 * This <b>resets</b> this cursor.
	 */
	public final void moveCenterToCoordinates(final float[] coords) {
		for (int i = 0; i < origin.length; i++)
			origin[i] = Math.round(coords[i] / calibration[i]);
		reset();
	}

	/**
	 * Return the square distance measured from the center of the disc to the current
	 * cursor position, in physical units.
	 */
	public final double getDistanceSquared() {
		double sum = 0;
		for (int i = 0; i < position.length; i++)
			sum += position[i] * position[i] * calibration[i] * calibration[i];
		return sum;
	}
	
	/**
	 * Store the relative position of the current cursor with respect to the ball center in 
	 * the array given in argument. The position is returned in <b>pixel units</b> and as
	 * such, is stored in an <code>int</code> array.
	 */
	public final void getRelativePosition(int[] position) {
		for (int i = 0; i < position.length; i++) {
			position[i] = this.position[i];
		}
	}
	
	/**
	 * Return the relative calibrated position of this cursor in physical units.
	 */
	public final void getPhysicalRelativeCoordinates(double[] coordinates) {
		for (int i = 0; i < coordinates.length; i++) 
			coordinates[i] = position[i] * calibration[i];
	}
	
}
