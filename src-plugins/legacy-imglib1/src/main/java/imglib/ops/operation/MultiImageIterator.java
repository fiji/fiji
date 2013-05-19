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

package imglib.ops.operation;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

//TODO
//Figure out Imglib's preferred way to handle linked cursors. Can they work where span dimensionality differs?
//    (a 2D Image to run against a plane in a 5D Image)  Or do I avoid ROICurs and use some transformational view
//    where dims exactly match?

@SuppressWarnings("unchecked")
/**
 * TODO
 *
 */
public class MultiImageIterator<T extends RealType<T>>  // don't want to implement full Cursor API
{
	private Image<T>[] images;
	private int[][] origins;
	private int[][] spans;
	private RegionOfInterestCursor<T>[] cursors;
	
	// -----------------  public interface ------------------------------------------

	public MultiImageIterator(Image<T>[] images)
	{
		this.images = images;
		int totalImages = images.length;
		origins = new int[totalImages][];
		spans = new int[totalImages][];
		for (int i = 0; i < totalImages; i++)
		{
			origins[i] = new int[images[i].getNumDimensions()];
			spans[i] = images[i].getDimensions().clone();
		}
		cursors = new RegionOfInterestCursor[totalImages];
	}

	public void setRegion(int i, int[] origin, int[] span)
	{
		origins[i] = origin;
		spans[i] = span;
	}
	
	public RegionOfInterestCursor<T>[] getSubcursors()
	{
		return cursors;
	}

	/** call after subregions defined and before first hasNext() or fwd() call. tests that all subregions defined are compatible. */
	void initialize()  // could call lazily in hasNext() or fwd() but a drag on performance
	{
		// make sure all specified regions are shape compatible : for now just test num elements in spans are same
		long totalSamples = numInSpan(spans[0]);
		for (int i = 1; i < spans.length; i++)
			if (numInSpan(spans[i]) != totalSamples)
				throw new IllegalArgumentException("incompatible span shapes");

		for (int i = 0; i < images.length; i++)
		{
			LocalizableByDimCursor<T> dimCursor = images[i].createLocalizableByDimCursor();
			cursors[i] = new RegionOfInterestCursor<T>(dimCursor, origins[i], spans[i]);
		}
	}
	
	public boolean hasNext()
	{
		boolean hasNext = cursors[0].hasNext();
		
		for (int i = 1; i < cursors.length; i++)
			if (hasNext != cursors[i].hasNext())
				throw new IllegalArgumentException("linked cursors are out of sync");
		
		return hasNext;
	}
	
	public void fwd()
	{
		for (int i = 0; i < cursors.length; i++)
			cursors[i].fwd();
	}
	
	// -----------------  private interface ------------------------------------------

	private long numInSpan(int[] span)  // TODO - call Imglib equivalent instead
	{
		long total = 1;
		for (int axisLen : span)
			total *= axisLen;
		return total;
	}
}

