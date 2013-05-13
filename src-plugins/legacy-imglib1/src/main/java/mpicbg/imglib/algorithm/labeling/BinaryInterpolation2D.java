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

package mpicbg.imglib.algorithm.labeling;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.integer.IntType;

/** Given two binary images of the same dimensions,
 * generate an interpolated image that sits somewhere
 * in between, as specified by the weight.
 * 
 * For each binary image, the edges are found
 * and then each pixel is assigned a distance to the nearest edge.
 * Inside, distance values are positive; outside, negative.
 * Then both processed images are compared, and wherever
 * the weighted sum is larger than zero, the result image
 * gets a pixel set to true (or white, meaning inside).
 * 
 * A weight of zero means that the first image is not present at all
 * in the interpolated image;
 * a weight of one means that the first image is present exclusively.
 * 
 * The code was originally created by Johannes Schindelin
 * in the VIB's vib.BinaryInterpolator class, for ij.ImagePlus.
 *
 * @author Albert Cardona
 * @author Johannes Schindelin
 */
public class BinaryInterpolation2D implements OutputAlgorithm<BitType>
{

	final private Image<BitType> img1, img2;
	private float weight;
	private Image<BitType> interpolated;
	private String errorMessage;
	private IDT2D idt1, idt2;

	public BinaryInterpolation2D(final Image<BitType> img1, final Image<BitType> img2, final float weight) {
		this.img1 = img1;
		this.img2 = img2;
		this.weight = weight;
	}

	/** NOT thread safe, stateful. */
	private final class IDT2D {
		final Image<IntType> result;
		final int w, h;
		final int[] position = new int[2];
		final LocalizableByDimCursor<BitType> csrc;
		final LocalizableByDimCursor<IntType> cout;

		IDT2D(final Image<BitType> img) {
			this.w = img.getDimension(0);
			this.h = img.getDimension(1);
			ImageFactory<IntType> f = new ImageFactory<IntType>(new IntType(), new ArrayContainerFactory());
			this.result = f.createImage(new int[]{w, h});

			// Set all result pixels to infinity
			final int infinity = (w + h) * 9;
			for (final IntType v : this.result) {
				v.set(infinity);
			}

			// init result pixels with those of the image:
			this.csrc = img.createLocalizableByDimCursor();
			this.cout = result.createLocalizableByDimCursor();

			int count = 0;
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					if (isBoundary(x, y)) {
						setOutValueAt(x, y, 0);
						count++;
					} else if (isJustOutside(x, y)) {
						setOutValueAt(x, y, -1);
					}
				}
			}

			if (count > 0) {
				propagate();
			}

			csrc.close();
			cout.close();
		}

		private final void setPosition(final LocalizableByDimCursor<?> c, final int x, final int y) {
			position[0] = x;
			position[1] = y;
			c.setPosition(position);
		}

		private final void setOutValueAt(final int x, final int y, final int value) {
			setPosition(cout, x, y);
			cout.getType().set(value);
		}

		private final int getSrcValueAt(final int x, final int y) {
			setPosition(csrc, x, y);
			return csrc.getType().get() ? 1 : 0;
		}

		private final int getOutValueAt(final int x, final int y) {
			setPosition(cout, x, y);
			return cout.getType().get();
		}

		// reads from result, writes to result
		private final void idt(final int x, final int y, final int dx, final int dy) {
			if (x + dx < 0 || y + dy < 0 ||
				x + dx >= w || y + dy >= h) {
				return;
			}
			int value = getOutValueAt(x + dx, y + dy);
			final int distance = (dx == 0 || dy == 0 ? 3 : 4);
			value += distance * (value < 0 ? -1 : 1);
			setPosition(cout, x, y);
			if (Math.abs(cout.getType().get()) > Math.abs(value)) {
				cout.getType().set(value);
			}
		}

		private final void propagate() {
			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++) {
					idt(i, j, -1, 0);
					idt(i, j, -1, -1);
					idt(i, j, 0, -1);
				}

			for (int j = h - 1; j >= 0; j--)
				for (int i = w - 1; i >= 0; i--) {
					idt(i, j, +1, 0);
					idt(i, j, +1, +1);
					idt(i, j, 0, +1);
				}

			for (int i = w - 1; i >= 0; i--)
				for (int j = h - 1; j >= 0; j--) {
					idt(i, j, +1, 0);
					idt(i, j, +1, +1);
					idt(i, j, 0, +1);
				}

			for (int i = 0; i < w; i++)
				for (int j = 0; j < h; j++) {
					idt(i, j, -1, 0);
					idt(i, j, -1, -1);
					idt(i, j, 0, -1);
				}
		}

		private final boolean isBoundary(final int x, final int y) {
			if (getSrcValueAt(x, y) == 0)
				return false;
			if (x <= 0 || getSrcValueAt(x - 1, y) == 0)
				return true;
			if (x >= w - 1 || getSrcValueAt(x + 1, y) == 0)
				return true;
			if (y <= 0 || getSrcValueAt(x, y - 1) == 0)
				return true;
			if (y >= h - 1 || getSrcValueAt(x, y + 1) == 0)
				return true;
			if (x <= 0 || y <= 0 || getSrcValueAt(x - 1, y - 1) == 0)
				return true;
			if (x <= 0 || y >= h - 1 || getSrcValueAt(x - 1, y + 1) == 0)
				return true;
			if (x >= w - 1 || y <= 0 || getSrcValueAt(x + 1, y - 1) == 0)
				return true;
			if (x >= w - 1 || y >= h - 1 || getSrcValueAt(x + 1, y + 1) == 0)
				return true;
			return false;
		}

		private final boolean isJustOutside(final int x, final int y) {
			if (getSrcValueAt(x, y) != 0)
				return false;
			if (x > 0 && getSrcValueAt(x - 1, y) != 0)
				return true;
			if (x < w - 1 && getSrcValueAt(x + 1, y) != 0)
				return true;
			if (y > 0 && getSrcValueAt(x, y - 1) != 0)
				return true;
			if (y < h - 1 && getSrcValueAt(x, y + 1) != 0)
				return true;
			if (x > 0 && y > 0 && getSrcValueAt(x - 1, y - 1) != 0)
				return true;
			if (x > 0 && y < h - 1 && getSrcValueAt(x - 1, y + 1) != 0)
				return true;
			if (x < w - 1 && y > 0 && getSrcValueAt(x + 1, y - 1) != 0)
				return true;
			if (x < w - 1 && y < h - 1 && getSrcValueAt(x + 1, y + 1) != 0)
				return true;
			return false;
		}
	}

	@Override
	public Image<BitType> getResult() {
		return interpolated;
	}
	
	@Override
	public boolean checkInput() {
		if (img1.getNumDimensions() < 2 || img2.getNumDimensions() < 2) {
			errorMessage = "Need at least 2 dimensions";
			return false;
		}
		if (img1.getDimension(0) != img2.getDimension(0) || img1.getDimension(1) != img2.getDimension(1)) {
			errorMessage = "Dimensions do not match";
			return false;
		}
		if (weight < 0 || weight > 1) {
			errorMessage = "Weight must be between 0 and 1, both inclusive.";
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	private final class NewITD2D implements Callable<IDT2D> {
		private final Image<BitType> img;
		NewITD2D(final Image<BitType> img) {
			this.img = img;
		}
		@Override
		public IDT2D call() throws Exception {
			return new IDT2D(img);
		}
	}

	/** After changing the weight, it's totally valid to call process() again,
	 * and then getResult(). */
	public void setWeight(final float weight) throws IllegalArgumentException {
		if (weight < 0 || weight > 1) {
			throw new IllegalArgumentException("Weight must be between 0 and 1, both inclusive.");
		}
		this.weight = weight;
	}

	@Override
	public boolean process() {
		this.interpolated = process(this.weight);
		return null != this.interpolated;
	}
	
	/** The first time, it will prepare the distance transform images, which are computed only once. */
	public Image<BitType> process(final float weight)
	{
		synchronized (this) {
			if (null == idt1 || null == idt2) {
				ExecutorService exec = Executors.newFixedThreadPool(Math.min(2, Runtime.getRuntime().availableProcessors()));
				Future<IDT2D> fu1 = exec.submit(new NewITD2D(img1));
				Future<IDT2D> fu2 = exec.submit(new NewITD2D(img2));
				exec.shutdown();

				try {
					this.idt1 = fu1.get();
					this.idt2 = fu2.get();
				} catch (InterruptedException ie) {
					throw new RuntimeException(ie);
				} catch (ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
		}

		// Cannot just img1.createNewImage() because the container may not be able to receive data,
		// such as the ShapeList container.
		final ImageFactory<BitType> f = new ImageFactory<BitType>(new BitType(), new ArrayContainerFactory());
		final Image<BitType> interpolated = f.createImage(new int[]{img1.getDimension(0), img1.getDimension(1)});

		if (img1.getContainer().compareStorageContainerCompatibility(img2.getContainer())) {
			final Cursor<IntType> c1 = idt1.result.createCursor();
			final Cursor<IntType> c2 = idt2.result.createCursor();
			final Cursor<BitType> ci = interpolated.createCursor();

			while (ci.hasNext()) {
				c1.fwd();
				c2.fwd();
				ci.fwd();

				if ((c1.getType().get() * weight) + (c2.getType().get() * (1 - weight)) > 0) {
					ci.getType().set(true);
				}
			}

			c1.close();
			c2.close();
			ci.close();
		} else {
			System.out.println("using option 2");
			final LocalizableByDimCursor<IntType> c1 = idt1.result.createLocalizableByDimCursor();
			final LocalizableByDimCursor<IntType> c2 = idt2.result.createLocalizableByDimCursor();
			final LocalizableByDimCursor<BitType> ci = interpolated.createLocalizableByDimCursor();

			while (ci.hasNext()) {
				ci.fwd();
				c1.setPosition(ci);
				c2.setPosition(ci);

				if (0 <= c1.getType().get() * weight + c2.getType().get() * (1 - weight)) {
					ci.getType().set(true);
				}
			}

			c1.close();
			c2.close();
			ci.close();
		}

		return interpolated;
	}
}
