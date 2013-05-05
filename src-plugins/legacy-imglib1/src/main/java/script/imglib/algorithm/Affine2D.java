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
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package script.imglib.algorithm;

import java.awt.geom.AffineTransform;

import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.NumericType;

/** 
* Expects matrix values in the same order that {@link AffineTransform} uses.
* 
* The constructors accept either an {@link Image} or an {@link IFunction} from which an {@link Image} is generated. */
/**
 * TODO
 *
 */
public class Affine2D<N extends NumericType<N>> extends Affine3D<N>
{
	/** Affine transform the image with the best interpolation mode available. */
	public Affine2D(final Object fn,
			final Number scaleX, final Number shearX, 
			final Number shearY, final Number scaleY,
			final Number translateX, final Number translateY) throws Exception {
		this(fn, scaleX.floatValue(), shearX.floatValue(), translateX.floatValue(),
				 shearY.floatValue(), scaleY.floatValue(), translateY.floatValue(),
				 Affine3D.BEST, 0);
	}

	/** Affine transform the image with the best interpolation mode available. */
	public Affine2D(final Object fn,
			final Number scaleX, final Number shearX, 
			final Number shearY, final Number scaleY,
			final Number translateX, final Number translateY,
			final Mode mode, final Number outside) throws Exception {
		super(fn, scaleX.floatValue(), shearX.floatValue(), translateX.floatValue(),
				  shearY.floatValue(), scaleY.floatValue(), translateY.floatValue(),
				  mode, outside);
	}

	public Affine2D(final Object fn, final AffineTransform aff) throws Exception {
		this(fn, aff, Affine3D.BEST);
	}

	public Affine2D(final Object fn, final AffineTransform aff, final Mode mode) throws Exception {
		this(fn, aff, mode, 0);
	}

	public Affine2D(final Object fn, final AffineTransform aff, final Number outside) throws Exception {
		this(fn, aff, Affine3D.BEST, outside);
	}

	public Affine2D(final Object fn, final AffineTransform aff, final Mode mode, final Number outside) throws Exception {
		super(fn, (float)aff.getScaleX(), (float)aff.getShearX(),
				  (float)aff.getShearY(), (float)aff.getScaleY(),
				  (float)aff.getTranslateX(), (float)aff.getTranslateY(),
				  mode, outside);
	}

	public Affine2D(final Object fn,
					final float scaleX, final float shearX,
					final float shearY, final float scaleY,
					final float translateX, final float translateY,
					final Mode mode, final OutOfBoundsStrategyFactory<N> oobf) throws Exception
	{
		super(fn, scaleX, shearX, translateX,
				  shearY, scaleY, translateY, mode, oobf);
	}
}
