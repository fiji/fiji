/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package mpicbg.imglib.cursor.special;

import mpicbg.imglib.cursor.Localizable;
import mpicbg.imglib.type.Type;

/**
 * Abstract cursor that offers facilities to move the cursor <i>origin</i> (whatever this means
 * for the concrete implementation) by giving <b>physical</b> coordinates, converted
 * using a calibration <code>float[]</code> array. It is intended for concrete cursors 
 * than aim at being used as a ROI. 
 *
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
