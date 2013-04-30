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

package mpicbg.imglib.labeling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mpicbg.imglib.container.DirectAccessContainer;
import mpicbg.imglib.container.DirectAccessContainerFactory;
import mpicbg.imglib.container.basictypecontainer.IntAccess;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.Display;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.TypeImpl;

/**
 * The LabelingType represents a labeling of a pixel with zero or more
 * labelings of type T. Each labeling names a distinct object in the
 * image space.
 * 
 * @param <T>
 *
 * @author Lee Kamentsky
 */
public class LabelingType<T extends Comparable<T>> extends TypeImpl<LabelingType<T>> implements Type<LabelingType<T>> {

	protected final DirectAccessContainer<LabelingType<T>, ? extends IntAccess > storage;
	protected final LabelingMapping<T, Integer> mapping;
	protected List<T> value = null;
	protected IntAccess access = null;
	protected long [] generation;
	/**
	 * A labeling that supports 2^31 different kinds of label assignments
	 * @param storage
	 */
	public LabelingType( DirectAccessContainer<LabelingType<T>, ? extends IntAccess> storage ) { 
		this.storage = storage;
		mapping = new LabelingMapping<T, Integer>(new Integer(0));
		generation = new long [1];
	}
	
	// this is the constructor if you want it to be a variable
	public LabelingType( List<T> value ) { 
		this.value = Collections.unmodifiableList(value);
		storage = null;
		mapping = null;
	}

	// this is the constructor if you want it to be a variable
	public LabelingType() { 
		storage = null;
		mapping = null;
		this.value = Collections.emptyList();
		this.value = Collections.unmodifiableList(this.value);
	}
	
	protected LabelingType(
			DirectAccessContainer<LabelingType<T>, ? extends IntAccess> storage,
			LabelingMapping<T, Integer> mapping,
			IntAccess access,
			long [] generation) {
		this.storage = storage;
		this.mapping = mapping;
		this.access = access;
		this.generation = generation;
	}
	
	/**
	 * Get the labels defined at the type's current pixel or 
	 * @return a list of the labelings at the current location.
	 */
	public List<T> getLabeling()
	{
		if (value != null) {
			return value;
		}
		return mapping.listAtIndex(access.getValue(i));
	}
	
	/**
	 * Set the labeling at the current pixel
	 * @param labeling
	 */
	public void setLabeling(List<T> labeling) {
		if (value != null) {
			value = labeling;
		} else {
			access.setValue(i, mapping.indexOf(labeling));
			generation[0]++;
		}
	}
	
	public void setLabeling(T [] labeling) {
		setLabeling(Arrays.asList(labeling));
	}
	
	/**
	 * Assign a pixel a single label
	 * @param label - the label to assign
	 */
	public void setLabel(T label) {
		List<T> labeling = new ArrayList<T>(1);
		labeling.add(label);
		setLabeling(labeling);
	}
	
	/**
	 * This method returns the canonical object for the given labeling.
	 * SetLabeling will work faster if you pass it the interned object
	 * instead of one created by you.
	 * 
	 * @param labeling
	 * @return
	 */
	public List<T> intern(List<T> labeling)
	{
		return mapping.intern(labeling);
	}
	
	/**
	 * Return the canonical labeling object representing the single labeling.
	 * SetLabeling will work faster if you use this object.
	 * @param label - a label for a pixel.
	 * @return - the canonical labeling with the single label.
	 */
	public List<T> intern(T label) {
		List<T> labeling = new ArrayList<T>(1);
		labeling.add(label);
		return intern(labeling);
	}
	@Override
	public int getEntitiesPerPixel() {
		return 1;
	}

	@Override
	public DirectAccessContainer<LabelingType<T>, ? extends IntAccess> createSuitableDirectAccessContainer(
			DirectAccessContainerFactory storageFactory, int[] dim) {
		final DirectAccessContainer<LabelingType<T>, ? extends IntAccess> container = storageFactory.createIntInstance(dim, 1);
		// create a Type that is linked to the container
		final LabelingType<T> linkedType = new LabelingType<T>( container );
		
		container.setLinkedType( linkedType );
		return container;
	}

	@Override
	public void updateContainer(Cursor<?> c) {
		access = storage.update(c);
	}

	@Override
	public LabelingType<T> createVariable() {
		return new LabelingType<T>();
	}

	@Override
	public LabelingType<T> copy() {
		return new LabelingType<T>(getLabeling());
	}

	@Override
	public LabelingType<T> duplicateTypeOnSameDirectAccessContainer() {
		return new LabelingType<T>(this.storage, this.mapping, this.access, 
				this.generation);
	}

	@Override
	public void set(LabelingType<T> c) {
		setLabeling(c.getLabeling());
	}

	@Override
	@SuppressWarnings("unchecked")
	public LabelingType<T>[] createArray1D(int size1) {
		LabelingType<T> [] result = (LabelingType<T> [])(
				java.lang.reflect.Array.newInstance(getClass(), size1));
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public LabelingType<T>[][] createArray2D(int size1, int size2) {
		LabelingType<T> [] row1 = createArray1D(size2);
		LabelingType<T> [][] result = (LabelingType<T>[][])(
				java.lang.reflect.Array.newInstance(row1.getClass(), size1));
		switch(size1) {
		case 0:
			break;
		case 1:
			result[0] = row1;
			break;
		default:
			result[0] = row1;
			for (int i=1; i<size1; i++) {
				result[i] = createArray1D(size2);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public LabelingType<T>[][][] createArray3D(int size1, int size2, int size3) {
		LabelingType<T> [][] row1 = createArray2D(size2,size3);
		LabelingType<T> [][][] result = (LabelingType<T>[][][])(
				java.lang.reflect.Array.newInstance(row1.getClass(), size1));
		switch(size1) {
		case 0:
			break;
		case 1:
			result[0] = row1;
			break;
		default:
			result[0] = row1;
			for (int i=1; i<size1; i++) {
				result[i] = createArray2D(size2, size3);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return getLabeling().toString();
	}

	class DefaultDisplay extends Display<LabelingType<T>> {

		public DefaultDisplay(Image<LabelingType<T>> img) {
			super(img);
			min = 0;
			max = 0;
		}

		@Override
		public void setMinMax() {
			final Cursor<LabelingType<T>> c = img.createCursor();
			
			if ( !c.hasNext() )
			{
				min = 0;
				max = 0;
				return;
			}
			
			c.fwd();
			min = max = valueAt(c);

			while ( c.hasNext() )
			{
				c.fwd();
				
				final long value = valueAt(c);

				if ( value > max )
					max = value;
				else if ( value < min )
					min = value;
			}
			
			c.close();
			
		}
		private long valueAt(Cursor<LabelingType<T>> c) {
			return c.getType().mapping.indexOf(c.getType().getLabeling());
		}
		
		private long valueAt(LabelingType<T> v) {
			return v.mapping.indexOf(v.getLabeling());
		}

		@Override
		public float get32Bit(LabelingType<T> c) {
			return valueAt(c);
		}

		@Override
		public float get32BitNormed(LabelingType<T> c) {
			return normFloat(valueAt(c));
		}

		@Override
		public byte get8BitSigned( LabelingType<T> c) {
			return (byte)Math.round( normFloat( valueAt(c)) * 255 ); 
		}
		@Override
		public short get8BitUnsigned( LabelingType<T> c) { return (short)Math.round( normFloat( valueAt(c) ) * 255 ); }			
		
	}
	@Override
	public Display<LabelingType<T>> getDefaultDisplay(
			Image<LabelingType<T>> image) {
		return new DefaultDisplay(image);
	}

	/**
	 * Get the labels known by the type
	 * @return a list of all labels in the type's associated storage
	 */
	List<T> getLabels() {
		return mapping.getLabels();
	}

	/**
	 * The underlying storage has an associated generation which is
	 * incremented every time the storage is modified. For cacheing, it's
	 * often convenient or necessary to know whether the storage has
	 * changed to know when the cache is invalid. The strategy is to
	 * save the generation number at the time of cacheing and invalidate
	 * the cache if the number doesn't match.
	 * 
	 * @return the generation of the underlying storage
	 */
	long getGeneration() {
		return generation[0];
	}
}
