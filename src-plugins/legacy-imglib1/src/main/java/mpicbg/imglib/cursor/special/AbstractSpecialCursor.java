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

import java.util.Iterator;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * This abstract cursor offer facilities for specialized cursor that are based 
 * on a {@link LocalizableByDimCursor} whose iteration domain is imposed.
 * This abstract class itself is not really interesting, see sub-classes.
 * @param <T>
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 9
 * @author 2010
 */
public abstract class AbstractSpecialCursor <T extends Type<T>> implements LocalizableCursor<T> {

	/*
	 * FIELDS
	 */
	
	/** The cursor that will be used internally to iterate in the domain. */ 
	protected LocalizableByDimCursor<T> cursor;
	/** The Image this cursors operates on. */
	protected Image<T> img;
	/** True if the iteration is not done yet. */
	protected boolean hasNext;
	
	/**
	 * Return the number of pixels this cursor will iterate on (or, the number of iterations
	 * it will do before exhausting). This is useful when one needs to know 
	 * the number of pixel iterated on in advance. For instance:
	 * <pre>
	 * DiscCursor<T> dc = new DiscCursor(img, center, 5);
	 * int arrraySize = sc.getNPixels();
	 * float[] pixelVal = new float[arraySize];
	 * int index = 0;
	 * while (sc.hasNext()) {
	 * 	sc.fwd();
	 * 	pixelVal[index] = sc.getType().getRealFloat();
	 * 	index++;
	 * }
	 * </pre>
	 */
	public abstract int getNPixels();
	
	
	/*
	 * CURSOR METHODS
	 * We simply forward them to the internal cursor
	 */
	
	
	@Override
	public void close() {
		cursor.close();
	}

	@Override
	public int[] createPositionArray() {
		return cursor.createPositionArray();
	}

	@Override
	public int getArrayIndex() {
		return cursor.getArrayIndex();
	}

	@Override
	public Image<T> getImage() {
		return img;
	}

	@Override
	public Container<T> getStorageContainer() {
		return cursor.getStorageContainer();
	}

	@Override
	public int getStorageIndex() {
		return cursor.getStorageIndex();
	}

	@Override
	public T getType() {
		return cursor.getType();
	}

	@Override
	public boolean isActive() {
		return cursor.isActive();
	}

		@Override
	public void setDebug(boolean debug) {
		cursor.setDebug(debug);
	}

	@Override
	public final boolean hasNext() {
		return hasNext;
	}

	@Override
	public final T next() {
		fwd();
		return cursor.getType();
	}

	@Override
	public void remove() {
		cursor.remove();
	}

	@Override
	public Iterator<T> iterator() {
		reset();
		return this;
	}

	@Override
	public void fwd(long steps) {
		for (int i = 0; i < steps; i++) 
			fwd();
	}

	@Override
	public int[] getDimensions() {
		return cursor.getDimensions();
	}

	@Override
	public void getDimensions(int[] position) {
		cursor.getDimensions(position);
	}

	@Override
	public int getNumDimensions() {
		return 3;
	}

	/*
	 * LOCALIZABLE METHODS
	 */
	
	@Override
	public int[] getPosition() {
		return cursor.getPosition();
	}

	@Override
	public void getPosition(int[] position) {
		cursor.getPosition(position);
	}

	@Override
	public int getPosition(int dim) {
		return cursor.getPosition(dim);
	}

	@Override
	public String getPositionAsString() {
		return cursor.getPositionAsString();
	}

	
}
