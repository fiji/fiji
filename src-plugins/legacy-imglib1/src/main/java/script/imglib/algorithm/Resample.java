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

import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.interpolation.nearestneighbor.NearestNeighborInterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.NumericType;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import script.imglib.algorithm.fn.AbstractAffine3D.Mode;
import script.imglib.color.Alpha;
import script.imglib.color.Blue;
import script.imglib.color.Green;
import script.imglib.color.RGBA;
import script.imglib.color.Red;
import script.imglib.math.Compute;

/** Resample an image in all its dimensions by a given scaling factor and interpolation mode.
 *  
 *  An image of 2000x2000 pixels, when resampled by 2, will result in an image of dimensions 4000x4000.
 *  
 *  Mathematically this is not a scaling operation and can be proved to be wrong.
 *  For proper scaling, see {@link Scale2D} and {@link Scale3D}. */
/**
 * TODO
 *
 */
public class Resample<N extends NumericType<N>> extends Image<N>
{
	static public final Mode LINEAR = Affine3D.LINEAR;
	static public final Mode NEAREST_NEIGHBOR = Affine3D.NEAREST_NEIGHBOR;
	static public final Mode BEST = Affine3D.BEST;

	/** Resample an {@link Image} with the best possible mode. */
	public Resample(final Image<N> img, final Number scale) throws Exception {
		this(img, asDimArray(img, scale), BEST);
	}

	public Resample(final Image<N> img, final Number scale, final Mode mode) throws Exception {
		this(img, asDimArray(img, scale), mode);
	}

	public Resample(final Image<N> img, final int[] dimensions) throws Exception {
		this(img, dimensions, BEST);
	}

	public Resample(final Image<N> img, final int[] dimensions, final Mode mode) throws Exception {
		super(process(img, dimensions, mode).getContainer(), img.createType());
	}

	static private final int[] asDimArray(final Image<?> img, final Number scale) {
		final int[] dim = new int[img.getNumDimensions()];
		final double s = scale.doubleValue();
		for (int i=0; i<dim.length; i++) {
			dim[i] = (int)((img.getDimension(i) * s) + 0.5);
		}
		return dim;
	}

	@SuppressWarnings("unchecked")
	static private final <N extends NumericType<N>> Image<N> process(final Image<N> img, int[] dim, final Mode mode) throws Exception {
		// Pad dim array with missing dimensions
		if (dim.length != img.getNumDimensions()) {
			int[] d = new int[img.getNumDimensions()];
			int i = 0;
			for (; i<dim.length; i++) d[i] = dim[i];
			for (; i<img.getNumDimensions(); i++) d[i] = img.getDimension(i);
			dim = d;
		}
		final Type<?> type = img.createType();
		if (RGBALegacyType.class.isAssignableFrom(type.getClass())) { // type instanceof RGBALegacyType fails to compile
			return (Image)processRGBA((Image)img, dim, mode);
		} else if (type instanceof RealType<?>) {
			return (Image)processReal((Image)img, dim, mode);
		} else {
			throw new Exception("Affine transform: cannot handle type " + type.getClass());
		}
	}

	static private final Image<RGBALegacyType> processRGBA(final Image<RGBALegacyType> img, final int[] dim, final Mode mode) throws Exception {
		// Process each channel independently and then compose them back
		return new RGBA(processReal(Compute.inFloats(new Red(img)), dim, mode),
						processReal(Compute.inFloats(new Green(img)), dim, mode),
						processReal(Compute.inFloats(new Blue(img)), dim, mode),
						processReal(Compute.inFloats(new Alpha(img)), dim, mode)).asImage();
	}

	static private final <T extends RealType<T>> Image<T> processReal(final Image<T> img, final int[] dim, final Mode mode) throws Exception {

		final Image<T> res = img.getImageFactory().createImage(dim);

		InterpolatorFactory<T> ifac;
		switch (mode) {
		case LINEAR:
			ifac = new LinearInterpolatorFactory<T>(new OutOfBoundsStrategyMirrorFactory<T>());
			break;
		case NEAREST_NEIGHBOR:
			ifac = new NearestNeighborInterpolatorFactory<T>(new OutOfBoundsStrategyMirrorFactory<T>());
			break;
		default:
			throw new Exception("Resample: unknown mode!");
		}

		final Interpolator<T> inter = ifac.createInterpolator(img);
		final LocalizableCursor<T> c2 = res.createLocalizableCursor();
		final float[] s = new float[dim.length];
		for (int i=0; i<s.length; i++) s[i] = (float)img.getDimension(i) / dim[i];
		final int[] d = new int[dim.length];
		final float[] p = new float[dim.length];
		while (c2.hasNext()) {
			c2.fwd();
			c2.getPosition(d);
			for (int i=0; i<d.length; i++) p[i] = d[i] * s[i];
			inter.moveTo(p);
			c2.getType().set(inter.getType());			
		}
		return res;
	}
}
