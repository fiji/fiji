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
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.Type;

/**
 * This class implements a {@link LocalizableCursor} that iterates over all the pixel within the volume
 * of a 3D ball, whose center and radius are given at construction. It is made so that
 * if the ball volume is made of N pixels, this cursor will go exactly over N iterations 
 * before exhausting. 
 * It takes a spatial calibration into account, which may be non isotropic, so that the region
 * iterated over is a sphere in <b>physical coordinates</b>.
 * <p>
 * The center of the sphere can be set to a {@link LocalizableCursor} position, allowing
 * to use this cursor in neighborhood processing operations. The two cursors (this one and 
 * the main one) are not linked in any way, so the method to move the sphere center must
 * be called every time. For instance:
 * <pre>
 * Image<T> img;
 * // ...
 * float radius = 10;
 * LocalizableCursorr< T > mainCursor = img.createLocalizableCursor();
 * SphereCursor< T > sphereCursor = new SphereCursorr< T >(img, mainCursor, radius);
 * while (mainCursor.hasNext()) {
 * 	mainCursor.fwd();
 * 	sphereCursor.moveCenterTo(mainCursor);
 * 	while (sphereCursor.hasNext()) {
 * 		sphereCursor.fwd();
 * 		// Have fun here ...
 * 	}
 * }
 * sphereCursor.close();
 * mainCursor.close();
 * </pre>
 * 
 * <p>
 * Bounding box: if we assume for instance that the calibration is 1 in every direction,
 * then the whole ball will be contained in a cube which side is <code>2 * ceil(radius) + 1</code>.
 * <p>
 * The iteration order is always the same. Iteration starts from the middle Z plane, and fill circles
 * away from this plane in alternating fashion: <code>Z = 0, 1, -1, 2, -2, ...</code>. For each 
 * circle, lines are drawn in the X positive direction from the middle line and away from it also in
 * an alternating fashion: <code>Y = 0, 1, -1, 2, -2, ...</code>. To parse all the pixels,
 * a line-scan algorithm is used, relying on McIlroy's algorithm to compute ellipse bounds
 * efficiently.
 * <p>
 * Internally, this cursor relies on a {@link LocalizableByDimCursor}. It makes intensive use
 * of states to avoid calling the {@link Math#sqrt(double)} method. 
 * 
 * @param <T>
 * @author Jean-Yves Tinevez (jeanyves.tinevez@gmail.com) -  August
 * @author 2010
 */
public final class SphereCursor<T extends Type<T>> extends DomainCursor<T> {

	/** The state of the cursor. */
	private CursorState state, nextState;
	/** Store the position index. */
	/** For mirroring, indicate if we must take the mirror in the Z direction. */
	private boolean mirrorZ;
	/** When drawing a line, the line length. */
	private int rx;
	/** The XY circle radius at height Z. */
	private int ry;
	/** Store XY circle radiuses for all Z */
	private int[] rys;
	/** Store X line bounds for all Y */
	private int[] rxs;
	/** Indicate whether we finished all Z planes. */
	private boolean doneZ = false;
	/** Is true when all Z and Y have been done, just the last line is to be drawn. */
	private boolean allDone;
	/**
	 * Indicates what state the cursor is currently in, so as to choose the right routine 
	 * to get coordinates */
	private enum CursorState {
		DRAWING_LINE					,
		INITIALIZED						,
		INCREMENT_Y						,
		MIRROR_Y						,
		INCREMENT_Z						;
	}

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Construct a {@link SphereCursor} on a 3D image with a given spatial calibration.
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 * @param outOfBoundsFactory  the {@link OutOfBoundsStrategyFactory} that will be used to handle off-bound locations
	 */
	public SphereCursor(final Image<T> img, final float[] center, float radius, final float[] calibration, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory) {
		if (img.getDimensions().length != 3) 
			throw new IllegalArgumentException(
					String.format("SphereCursor: must get a 3D image, got a %dD image.", img.getDimensions().length));
		this.img = img;
		this.size = radius;
		this.cursor = img.createLocalizableByDimCursor(outOfBoundsFactory);		
		if (null == calibration) 
			this.calibration = new float[] { 1, 1, 1 };
		else 
			this.calibration = calibration.clone();
		origin = new int[3];
		for (int i = 0; i < 3; i++) 
			origin[i] = Math.round(center[i] / calibration[i]);

		// Instantiate it once, and with large size, so that we do not have to instantiate every time we move in Z
		rxs = new int [ (int) (Math.max(Math.ceil(radius/this.calibration[1]), Math.ceil(radius/this.calibration[0]))  +  1) ];
		rys = new int[(int) (Math.ceil(radius/this.calibration[2])+1)];
		reset();
	}
	
	/**
	 * Construct a {@link SphereCursor} on a 3D image with a given spatial calibration,
	 * using a default {@link OutOfBoundsStrategyValueFactory} to handle off-bounds locations.
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 */
	public SphereCursor(final Image<T> img, final float[] center, float radius, final float[] calibration) {
		this(img, center, radius, calibration, new OutOfBoundsStrategyValueFactory<T>());
	}

	/**
	 * Construct a {@link SphereCursor} on a 3D image with a given spatial calibration,
	 * using a default {@link OutOfBoundsStrategyValueFactory} to handle off-bounds locations.
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 */
	public SphereCursor(final Image<T> img, final double[] center, double radius, final double[] calibration) {
		// Simply cast to float
		this(img, 
				new float[] {(float)center[0], (float) center[1], (float) center[2]}, 
				(float) radius, 
				null == calibration ? null : new float[] {(float) calibration[0], (float) calibration[1], (float) calibration[2] } );
	}

	/**
	 * Construct a {@link SphereCursor} on a 3D image, using the spatial calibration
	 * stored in the image and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations.
	 * 
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @see Image#setCalibration(float[])
	 */
	public SphereCursor(final Image<T> img, final float[] center, float radius) {
		this(img, center, radius, img.getCalibration());
	}

	/**
	 * Construct a {@link SphereCursor} on a 3D image, using the spatial calibration
	 * stored in the image and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations.
	 * 
	 * @param img  the image, must be 3D
	 * @param center  the ball center, in physical units
	 * @param radius  the ball radius, in physical units
	 * @see Image#setCalibration(float[])
	 */
	public SphereCursor(final Image<T> img, final double[] center, double radius) {
		// Simply cast to float
		this(img, 
				new float[] {(float)center[0], (float) center[1], (float) center[2]}, 
				(float) radius, 
				img.getCalibration() );
	}

	/**
	 * Construct a {@link SphereCursor} on a 3D image with a given spatial calibration
	 * and a given {@link OutOfBoundsStrategyFactory} to handle off-bounds locations.
 	 * The center of the sphere is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image, must be 3D
	 * @param centerCursor  the localizable object which position will set the sphere center 
	 * @param radius  the ball radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 */
	public SphereCursor(final Image<T> img, final Localizable centerCursor, float radius, final float[] calibration,
			OutOfBoundsStrategyFactory<T> outOfBoundsFactory) {
		if (img.getDimensions().length != 3) 
			throw new IllegalArgumentException(
					String.format("SphereCursor: must get a 3D image, got a %dD image.", img.getDimensions().length));
		this.img = img;
		this.size = radius;
		this.cursor = img.createLocalizableByDimCursor(outOfBoundsFactory);		
		if (null == calibration) 
			this.calibration  = new float[] { 1, 1, 1};
		else
			this.calibration = calibration.clone();
		origin = centerCursor.getPosition();
		// Instantiate it once, and with large size, so that we do not have to instantiate every time we move in Z
		rxs = new int [ Math.max(Math.round(radius/calibration[1]), Math.round(radius/calibration[0]))  +  1 ];
		rys = new int[Math.round(radius/calibration[2])+1];
		reset();
	}
	
	/**
	 * Construct a {@link SphereCursor} on a 3D image, using the given spatial calibration
	 * and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations. The center of the sphere
	 * is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image, must be 3D
	 * @param centerCursor  the cursor which position will set the sphere center 
	 * @param radius  the ball radius, in physical units
	 */
	public SphereCursor(final Image<T> img, final Localizable centerCursor, float radius, final float[] calibration) {
		this(img, centerCursor, radius, calibration, new OutOfBoundsStrategyValueFactory<T>());
	}
	
	
	/**
	 * Construct a {@link SphereCursor} on a 3D image, using the spatial calibration
	 * stored in the image and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations. The center of the sphere
	 * is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image, must be 3D
	 * @param centerCursor  the localizable object which position will set the sphere center 
	 * @param radius  the ball radius, in physical units
	 */
	public SphereCursor(final Image<T> img, final Localizable centerCursor, float radius) {
		this(img, centerCursor, radius, img.getCalibration(), new OutOfBoundsStrategyValueFactory<T>());
	}
	
	/*
	 * SPECIFIC SPHEREVOLUME METHODS
	 */
	
	
	@Override
	public int getNPixels() {
		int pixel_count = 0;
		final int nzplanes = Math.round(size/calibration[0]); // half nbr of planes
		final int[] local_rys = new int[nzplanes+1];
		final int[] local_rxs = new int [ Math.max(Math.round(size/calibration[1]), Math.round(size/calibration[0]))  +  1 ];
		int local_ry, local_rx;

		// Get all XY circles radiuses
		Utils.getXYEllipseBounds(Math.round(size/calibration[1]), Math.round(size/calibration[2]), local_rys); 
		
		// Deal with plane Z = 0
		Utils.getXYEllipseBounds(Math.round(size/calibration[0]), Math.round(size/calibration[1]), local_rxs);
		local_ry = local_rys[0];
		local_rx = local_rxs[0]; // middle line
		pixel_count += 2 * local_rx + 1;
		for (int i = 1; i <= local_ry; i++) {
			local_rx = local_rxs[i];
			pixel_count += 2 * (2 * local_rx + 1); // Twice because we mirror
		}
		
		// Deal with other planes
		for (int j = 1; j <= nzplanes; j++) {
			local_ry = local_rys[j];
			if (local_ry ==0) 
				continue;
			Utils.getXYEllipseBounds(Math.round(local_ry*calibration[1]/calibration[0]), local_ry, local_rxs); 
			local_rx = local_rxs[0]; // middle line
			pixel_count += 2 * (2 * local_rx + 1); // twice we mirror in Z
			for (int i = 1; i <= local_ry; i++) {
				local_rx = local_rxs[i];
				pixel_count += 4 * (2 * local_rx + 1); // 4 times because we mirror in Z and in Y
			}
		}
		return pixel_count;
	}
	
	
	/**
	 * Change the radius of the sphere this cursor iterates on.  This <b>resets</b> this cursor.
	 * @param size  the radius to set
	 */
	@Override
	public void setSize(float size) {
		this.size = size;
		// Instantiate it once, and with large size, so that we do not have to instantiate every time we move in Z
		rxs = new int [ Math.max(Math.round(size/calibration[1]), Math.round(size/calibration[0]))  +  1 ];		
		rys = new int[Math.round(size/calibration[2])+1];
		reset();
	}
		
	/**
	 * Return the current inclination with respect to this sphere center. Will be in
	 * the range [0, π]. 
	 * <p>
	 * In spherical coordinates, the inclination is the angle 
	 * between the Z axis and the line OM where O is the sphere center and M is 
	 * the point location.
	 */
	public final double getTheta() {
		return Math.acos( position[2] * calibration[2] / Math.sqrt( getDistanceSquared() ) );
	}
	
	/**
	 * Return the azimuth of the spherical coordinates of this cursor, with respect 
	 * to its center. Will be in the range ]-π, π].
	 * <p>
	 * In spherical coordinates, the azimuth is the angle measured in the plane XY between 
	 * the X axis and the line OH where O is the sphere center and H is the orthogonal 
	 * projection of the point M on the XY plane.
	 */
	public final double getPhi() {
		return Math.atan2(position[1]*calibration[1], position[0]*calibration[0]);
	}

	
	@Override
	public final void fwd() {

		switch(state) {

		case DRAWING_LINE:

			cursor.fwd(0);
			position[0]++;
			if (position[0] >= rx) {
				state = nextState;
				if (allDone)
					hasNext = false;
			}
			break;

		case INITIALIZED:

			// Compute XY circle radiuses for all Z in advance
			Utils.getXYEllipseBounds(Math.round(size/calibration[1]), Math.round(size/calibration[2]), rys); 
			ry = rys[0] ;
			
			Utils.getXYEllipseBounds(Math.round(size/calibration[0]), Math.round(size/calibration[1]), rxs); 
			rx = rxs[0] ; 
			cursor.setPosition(origin[0] - rx, 0);
			cursor.setPosition(origin[1], 1);
			cursor.setPosition(origin[2], 2);
			position[0] = -rx;
			state = CursorState.DRAWING_LINE;
			nextState = CursorState.INCREMENT_Y;
			break;

		case INCREMENT_Y:

			position[1] = -position[1] + 1; // y should be negative (coming from mirroring or init = 0)
			rx = rxs[position[1]];

			cursor.setPosition(origin[1] + position[1], 1);
			position[0] = -rx;
			cursor.setPosition(origin[0] - rx, 0);
			nextState = CursorState.MIRROR_Y;
			if (rx ==0)
				state = CursorState.MIRROR_Y;
			else
				state = CursorState.DRAWING_LINE;				
			break;

		case MIRROR_Y:

			position[0] = -rx;
			position[1] = - position[1];
			cursor.setPosition(origin[1] + position[1], 1);
			cursor.setPosition(origin[0] - rx, 0);
			if (position[1] <= - ry) {
				if (doneZ) 
					allDone  = true ;
				else
					nextState = CursorState.INCREMENT_Z;
			}
			else 
				nextState = CursorState.INCREMENT_Y;
			if (rx ==0)
				if (allDone)
					hasNext = false;
				else
					state = nextState;
			else
				state = CursorState.DRAWING_LINE;

			break;

		case INCREMENT_Z:

			if (mirrorZ) {

				position[2] = - position[2];
				mirrorZ = false;
				if (position[2] <= - Math.round(size/calibration[2])) 
					doneZ = true;

			} else {

				position[2] = - position[2] + 1;
				ry = rys[position[2]];
				mirrorZ = true;
			}

			Utils.getXYEllipseBounds(Math.round(ry*calibration[1]/calibration[0]), ry, rxs); 
			rx = rxs[0] ; 
			
			cursor.setPosition(origin[0]-rx, 0);
			cursor.setPosition(origin[1], 1);
			cursor.setPosition(origin[2] + position[2], 2);
			position[0] = -rx;
			position[1] = 0;
			state = CursorState.DRAWING_LINE;
			nextState = CursorState.INCREMENT_Y;
			break;
		}
	}

	@Override
	public final void reset() {
		cursor.reset();
		state = CursorState.INITIALIZED;
		mirrorZ = false;
		doneZ = false;
		allDone = false;
		position = new int[3];
		hasNext = true;
	}

}
