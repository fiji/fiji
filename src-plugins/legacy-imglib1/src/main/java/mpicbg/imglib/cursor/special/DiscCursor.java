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
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * TODO
 *
 */
public final class DiscCursor <T extends RealType<T>>  extends DomainCursor<T> {

	/** The state of the cursor. */
	private CursorState state, nextState;
	/** When drawing a line, the line length. */
	private int rx;
	/** Store X line bounds for all Y */
	private int[] rxs;

	private boolean allDone;
	
	/**
	 * Indicates what state the cursor is currently in, so as to choose the right routine 
	 * to get coordinates */
	private enum CursorState {
		DRAWING_LINE					,
		INITIALIZED						,
		INCREMENT_Y						,
		MIRROR_Y						;
	}
	
	/*
	 * CONSTRUCTORS
	 */
	
	/**
	 * Construct a {@link DiscCursor} on an image with a given spatial calibration.
	 * @param img  the image
	 * @param center  the disc center, in physical units
	 * @param radius  the disc radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 * @param outOfBoundsFactory  the {@link OutOfBoundsStrategyFactory} that will be used to handle off-bound locations
	 */
	public DiscCursor(final Image<T> img, final float[] center, float radius, final float[] calibration, final OutOfBoundsStrategyFactory<T> outOfBoundsFactory) {
		this.img = img;
		this.size = radius;
		this.cursor = img.createLocalizableByDimCursor(outOfBoundsFactory);		
		if (null == calibration)
			this.calibration = new float[] {1, 1, 1};
		 else 
			this.calibration = calibration.clone();
		this.origin = new int[img.getNumDimensions()];
		for (int i = 0; i < origin.length; i++)
			origin[i] = Math.round(center[i] / this.calibration[i]);
		rxs = new int [ (int) (Math.max(Math.ceil(radius/this.calibration[0]), Math.ceil(radius/this.calibration[1]))  +  1) ];
		reset();
	}
	
	/**
	 * Construct a {@link DiscCursor} on an image with a given spatial calibration,
	 * using a default {@link OutOfBoundsStrategyValueFactory} to handle off-bounds locations.
	 * @param img  the image
	 * @param center  the disc center, in physical units
	 * @param radius  the disc radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 */
	public DiscCursor(final Image<T> img, final float[] center, float radius, final float[] calibration) {
		this(img, center, radius, calibration, new OutOfBoundsStrategyValueFactory<T>());
	}

	/**
	 * Construct a {@link DiscCursor} on an image, using the spatial calibration
	 * stored in the image and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations.
	 * 
	 * @param img  the image
	 * @param center  the disc center, in physical units
	 * @param radius  the disc radius, in physical units
	 * @see Image#setCalibration(float[])
	 */
	public DiscCursor(final Image<T> img, final float[] center, float radius) {
		this(img, center, radius, img.getCalibration());
	}

	/**
	 * Construct a {@link DiscCursor} on an image with a given spatial calibration
	 * and a given {@link OutOfBoundsStrategyFactory} to handle off-bounds locations.
 	 * The center of the disc is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image
	 * @param centerCursor  the localizable object which position will set the disc center 
	 * @param radius  the disc radius, in physical units
	 * @param calibration  the spatial calibration (pixel size); if <code>null</code>, 
	 * a calibration of 1 in all directions will be used
	 */
	public DiscCursor(final Image<T> img, final Localizable centerCursor, float radius, final float[] calibration, OutOfBoundsStrategyFactory<T> outOfBoundsFactory) {
		this.img = img;
		this.size = radius;
		this.cursor = img.createLocalizableByDimCursor(outOfBoundsFactory);		
		if (null == calibration) 
			this.calibration = new float[] {1, 1, 1};
		else
			this.calibration = calibration.clone();		
		this.origin = centerCursor.getPosition();
		rxs = new int [ Math.max(Math.round(radius/calibration[0]), Math.round(radius/calibration[1]))  +  1 ];
		reset();
	}
	
	/**
	 * Construct a {@link DiscCursor} on an image, using the given spatial calibration
	 * and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations. The center of the disc
	 * is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image
	 * @param centerCursor  the cursor which position will set the disc center 
	 * @param radius  the disc radius, in physical units
	 */
	public DiscCursor(final Image<T> img, final Localizable centerCursor, float radius, final float[] calibration) {
		this(img, centerCursor, radius, calibration, new OutOfBoundsStrategyValueFactory<T>());
	}
	
	
	/**
	 * Construct a {@link DiscCursor} on an, using the spatial calibration
	 * stored in the image and a default {@link OutOfBoundsStrategyValueFactory}
	 * to handle off-bounds locations. The center of the disc
	 * is set by the {@link Localizable} given in argument.
	 * 
	 * @param img  the image
	 * @param centerCursor  the localizable object which position will set the disc center 
	 * @param radius  the disc radius, in physical units
	 */
	public DiscCursor(final Image<T> img, final Localizable centerCursor, float radius) {
		this(img, centerCursor, radius, img.getCalibration(), new OutOfBoundsStrategyValueFactory<T>());
	}
	
	/*
	 * METHODS
	 */
	
	/**
	 * Change the radius of the disc this cursor iterates on.  This <b>resets</b> this cursor.
	 * @param  size  the radius to set, in physical units.
	 */
	@Override
	public void setSize(float size) {
		this.size = size;
		rxs = new int [ Math.max(Math.round(size/calibration[0]), Math.round(size/calibration[1]))  +  1 ];		
		reset();
	}
	
	@Override
	public int getNPixels() {
		int pixel_count = 0;
		final int[] local_rxs = new int [ Math.max(Math.round(size/calibration[1]), Math.round(size/calibration[0]))  +  1 ];
		Utils.getXYEllipseBounds(Math.round(size/calibration[0]), Math.round(size/calibration[1]), local_rxs);
		int local_rx;

		pixel_count += 2 * local_rxs[0] + 1;
		for (int i = 1; i <= Math.round(size/calibration[1]); i++) {
			local_rx = local_rxs[i];
			pixel_count += 2 * (2 * local_rx + 1); // Twice because we mirror
		}
		return pixel_count;
	}
	

	/**
	 * Return the azimuth of the spherical coordinates of this cursor, with respect 
	 * to its center. Will be in the range ]-π, π].
	 * <p>
	 * In cylindrical coordinates, the azimuth is the angle measured between 
	 * the X axis and the line OM where O is the sphere center and M is the cursor location
	 */
	public final double getPhi() {
		return Math.atan2(position[1]*calibration[1], position[0]*calibration[0]);
	}

	@Override
	public void reset() {
		cursor.reset();
		state = CursorState.INITIALIZED;
		position = new int[img.getNumDimensions()];
		hasNext = true;
		allDone = false;
	}
	
	@Override
	public void fwd() {
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

			// Compute circle radiuses in advance
			Utils.getXYEllipseBounds(Math.round(size/calibration[0]), Math.round(size/calibration[1]), rxs);
			
			rx = rxs[0] ; 
			cursor.setPosition(origin);
			cursor.setPosition(origin[0] - rx, 0);
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
			if (position[1] <= - Math.round(size/calibration[1]))
				allDone  = true;
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
		}
	}
	
}
