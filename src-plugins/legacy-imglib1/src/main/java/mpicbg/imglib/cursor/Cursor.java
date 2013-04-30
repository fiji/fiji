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

package mpicbg.imglib.cursor;

import java.util.Iterator;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.container.array.Array;
import mpicbg.imglib.container.cell.CellContainer;
import mpicbg.imglib.cursor.vector.Dimensionality;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * <h2>The Cursor interface</h2>
 * 
 * <h3>Implementation</h3>
 * 
 * The {@link Cursor} is responsible for iterating over the image. Therefore it has to be implemented 
 * for each type of {@link Container} like {@link Array}, {@link CellContainer}, ... 
 * 
 * <h3>Type support</h3>
 * 
 * The {@link Cursor} does not know which {@link Type} of {@link Image} it is working on as there this
 * is not important for its positioning. It is typed to the {@link Type} so that the {@link Cursor} is
 * able to return the correct instance of {@link Type} when calling the getType() method.
 * 
 * <h3>Traversal policy</h3>
 * 
 * The {@link Cursor} class itself is only capable of iterating over all pixels of the {@link Image}, 
 * there is no guaranteed order in which the pixels are iterated, this depends on the implementation for
 * the specific {@link Container}.
 * <p>
 * However, it is <b>guaranteed</b> that for two images having the same {@link Container}, the 
 * traverse path will be the same.  For instance, if the following is true:
 * <p>
 * <pre>
  		Container&lt T &gt c1 = img1.getContainer(); 
  		Container&lt S &gt c2 = img2.getContainer();
 		if ( c1.compareStorageContainerCompatibility( c2 ) )  
 * </pre>
 * then, it is ensured that 
 * <pre>
 * 		{
 * 			Cursor&lt T &gt cursor1 = img1.createCursor();
 * 			Cursor&lt S &gt cursor2 = img2.createCursor();
 * 			while ( cursor1.hasNext() )
			{
				cursor1.fwd();
				cursor2.fwd();
			}
		}
 * </pre>
 * will visit the <b>same pixel positions</b>.
 * <p>
 * If the two {@link Container} are not the same, then {@link LocalizableCursor} and {@link LocalizableByDimCursor} 
 * have to be used:
 * <pre>
 * 		else {
 * 			LocalizableCursor&lt T &gt cursor1 = img1.createLocalizableCursor();
 * 			LocalizableByDimCursor&lt S &gt cursor2 = img2.createLocalizableByDimCursor();
 * 			while ( cursor1.hasNext() )
 * 			{
 * 				cursor1.fwd();
 * 				cursor2.moveTo( cursor1 );
 * 			}
 * 		}
 * </pre>
 * This snippet will also traverse images the same way, but this is less efficient than the previous solution 
 * with identical containers.
 *<p>
 * 
 * @param <T> - the {@link Type} this {@link Cursor} works on
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public interface Cursor<T extends Type<T>> extends Iterator<T>, java.lang.Iterable<T>, Iterable, Dimensionality
{	
	public void reset();			
	public boolean isActive();	

	public Image<T> getImage();
	public T getType();
	public int getArrayIndex();
	public int getStorageIndex();
	public Container<T> getStorageContainer();
	public void setDebug( final boolean debug );
	public int[] createPositionArray();
	
	public void close();
}
