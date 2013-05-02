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

package script.imglib.algorithm.fn;

import mpicbg.imglib.algorithm.transformation.ImageTransform;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.interpolation.nearestneighbor.NearestNeighborInterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.NumericType;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.AffineModel2D;
import mpicbg.models.AffineModel3D;
import script.imglib.color.Alpha;
import script.imglib.color.Blue;
import script.imglib.color.Green;
import script.imglib.color.RGBA;
import script.imglib.color.Red;
import script.imglib.math.Compute;

/** Convenient intermediate class to be able to operate directly on an {@link Image} argument in the constructor. */
/**
 * TODO
 *
 */
public abstract class AbstractAffine3D<T extends NumericType<T>> extends Image<T>
{
	static public enum Mode { LINEAR, NEAREST_NEIGHBOR };

	static public final Mode LINEAR = Mode.LINEAR;
	static public final Mode NEAREST_NEIGHBOR = Mode.NEAREST_NEIGHBOR;
	static public final Mode BEST = Mode.LINEAR;

	/** With a default {@link OutOfBoundsStrategyValueFactory} with @param outside. */
	@SuppressWarnings("unchecked")
	public AbstractAffine3D(final Image<T> img, final float[] matrix, final Mode mode, final Number outside) throws Exception {
		this(img, matrix, mode, new OutOfBoundsStrategyValueFactory<T>((T)withValue(img, img.createType(), outside))); // default value is zero
	}

	public AbstractAffine3D(final Image<T> img, final float[] matrix, final Mode mode, final OutOfBoundsStrategyFactory<T> oobf) throws Exception {
		super(process(img, matrix, mode, oobf).getContainer(), img.createType());
	}

	/** With a default {@link OutOfBoundsStrategyValueFactory} with @param outside. */
	@SuppressWarnings("unchecked")
	public AbstractAffine3D(final Image<T> img,
			final float scaleX, final float shearX,
			final float shearY, final float scaleY,
			final float translateX, final float translateY,
			final Mode mode, final Number outside) throws Exception {
		this(img, new float[]{scaleX, shearX, 0, translateX,
				  			   shearY, scaleY, 0, translateY,
				  			   0, 0, 1, 0}, mode, new OutOfBoundsStrategyValueFactory<T>((T)withValue(img, img.createType(), outside)));
	}

	public AbstractAffine3D(final Image<T> img,
			final float scaleX, final float shearX,
			final float shearY, final float scaleY,
			final float translateX, final float translateY,
			final Mode mode, final OutOfBoundsStrategyFactory<T> oobf) throws Exception {
		this(img, new float[]{scaleX, shearX, 0, translateX,
				  			   shearY, scaleY, 0, translateY,
				  			   0, 0, 1, 0}, mode, oobf);
	}
	

	@SuppressWarnings("unchecked")
	private static final NumericType<?> withValue(final Image<? extends NumericType<?>> img, final NumericType<?> type, final Number val) {
		final NumericType t = img.createType();
		if (RGBALegacyType.class.isAssignableFrom(t.getClass())) {
			int i = val.intValue();
			t.set((NumericType)new RGBALegacyType(i));
		} else {
			((RealType)t).setReal(val.doubleValue());
		}
		return t;
	}

	@SuppressWarnings("unchecked")
	static private final <N extends NumericType<N>>
						Image<N> process(final Image<N> img, final float[] matrix,
						final Mode mode, final OutOfBoundsStrategyFactory<N> oobf) throws Exception {
		if (matrix.length < 12) {
			throw new IllegalArgumentException("Affine transform in 2D requires a matrix array of 12 elements.");
		}
		final Type<?> type = img.createType();
		if (RGBALegacyType.class.isAssignableFrom(type.getClass())) { // type instanceof RGBALegacyType fails to compile
			return (Image)processRGBA((Image)img, matrix, mode, (OutOfBoundsStrategyFactory)oobf);
		} else if (type instanceof RealType<?>) {
			return (Image)processReal((Image)img, matrix, mode, (OutOfBoundsStrategyFactory)oobf);
		} else {
			throw new Exception("Affine transform: cannot handle type " + type.getClass());
		}
	}

	@SuppressWarnings("unchecked")
	static private final Image<RGBALegacyType> processRGBA(final Image<RGBALegacyType> img, final float[] m,
			final Mode mode, final OutOfBoundsStrategyFactory<RGBALegacyType> oobf) throws Exception {
		// Process each channel independently and then compose them back
		OutOfBoundsStrategyFactory<FloatType> ored, ogreen, oblue, oalpha;
		if (OutOfBoundsStrategyValueFactory.class.isAssignableFrom(oobf.getClass())) { // can't use instanceof
			final int val = ((OutOfBoundsStrategyValueFactory<RGBALegacyType>)oobf).getValue().get();
			ored = new OutOfBoundsStrategyValueFactory<FloatType>(new FloatType((val >> 16) & 0xff));
			ogreen = new OutOfBoundsStrategyValueFactory<FloatType>(new FloatType((val >> 8) & 0xff));
			oblue = new OutOfBoundsStrategyValueFactory<FloatType>(new FloatType(val & 0xff));
			oalpha = new OutOfBoundsStrategyValueFactory<FloatType>(new FloatType((val >> 24) & 0xff));
		} else {
			// Jump into the pool!
			try {
				ored = oobf.getClass().newInstance();
			} catch (Exception e) {
				System.out.println("Affine3D for RGBA: oops -- using a black OutOfBoundsStrategyValueFactory");
				ored = new OutOfBoundsStrategyValueFactory<FloatType>(new FloatType());
			}
			ogreen = ored;
			oblue = ored;
			oalpha = ored;
		}
		return new RGBA(processReal(Compute.inFloats(new Red(img)), m, mode, ored),
						processReal(Compute.inFloats(new Green(img)), m, mode, ogreen),
						processReal(Compute.inFloats(new Blue(img)), m, mode, oblue),
						processReal(Compute.inFloats(new Alpha(img)), m, mode, oalpha)).asImage();
	}

	static private final <R extends RealType<R>> Image<R> processReal(final Image<R> img, final float[] m,
			final Mode mode, final OutOfBoundsStrategyFactory<R> oobf) throws Exception {
		final InterpolatorFactory<R> inter;
		switch (mode) {
		case LINEAR:
			inter = new LinearInterpolatorFactory<R>(oobf);
			break;
		case NEAREST_NEIGHBOR:
			inter = new NearestNeighborInterpolatorFactory<R>(oobf);
			break;
		default:
			throw new IllegalArgumentException("Scale: don't know how to scale with mode " + mode);
		}

		final ImageTransform<R> transform;

		if (2 == img.getNumDimensions()) {
			// Transform the single-plane image in 2D
			AffineModel2D aff = new AffineModel2D();
			aff.set(m[0], m[4], m[1], m[5], m[3], m[7]);
			transform = new ImageTransform<R>(img, aff, inter);
		} else if (3 == img.getNumDimensions()) {
			// Transform the image in 3D, or each plane in 2D
			if (m.length < 12) {
				throw new IllegalArgumentException("Affine transform in 3D requires a matrix array of 12 elements.");
			}
			AffineModel3D aff = new AffineModel3D();
			aff.set(m[0], m[1], m[2], m[3],
					m[4], m[5], m[6], m[7],
					m[8], m[9], m[10], m[11]);
			transform = new ImageTransform<R>(img, aff, inter);
			// Ensure Z dimension is not altered if scaleZ is 1:
			if (Math.abs(m[10] - 1.0f) < 0.000001 && 0 == m[8] && 0 == m[9]) {
				int[] d = transform.getNewImageSize();
				d[2] = img.getDimension(2); // 0-based: '2' is the third dimension
				transform.setNewImageSize(d);
			}
		} else {
			throw new Exception("Affine transform: only 2D and 3D images are supported.");
		}

		if (!transform.checkInput() || !transform.process()) {
			throw new Exception("Could not affine transform the image: " + transform.getErrorMessage());
		}

		return transform.getResult();
	}
}
