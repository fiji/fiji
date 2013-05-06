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

package imglib.ops.example.rev3.function;

import java.util.ArrayList;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

// NOTES & TODO

// This is the last missing part to make rev 3 code do everything that rev 2 code could do plus more.

// Realize we could just compose any functions rather than image functions. that would be more
// general but I am doing a simple implementation.

// The idea here is that let's say I have 4 images that are each one plane and I want to average their values at each XY
// location. The plan is to compose these 4 2-D images into a single virtual 3-D "image" with Z == 4. Then I can use a
// AverageFunction on the XY locations across Z by passing correct axis deltas into the AverageFunction.

// note that as defined this composition allows one to treat multiple subregions of a plane in one image for example as
// a higher dimensional "image".

/** ComposedImageFunction
 * Composes a number of N dimensional Images into a N+1 dimension function
 *
 */
public final class ComposedImageFunction implements IntegerIndexedScalarFunction
{
	private ArrayList<SubImageInfo> subImages;
	private int[] subImageDimensions;
	private int[] subImagePosition;
	private int subImageSpanSize;
	
	private class SubImageInfo
	{
		int[] origin;
		ImageFunction function;
	}

	/** the only constructor. must use addImageRegion() to an instance before one can get values out of it via evaluate(). */
	public ComposedImageFunction()
	{
	}

	/** adds a subregion of an image to this COmposedImageFunction. The span of the region must match exactly the span of all previously added
	 * image subregions. the origin can vary (allowing multiple regions in a single image to be treated as separate planes in the composed image). */
	public void addImageRegion(Image<? extends RealType<?>> image, int[] origin, int[] span)
	{
		if (subImages == null)
		{
			subImages = new ArrayList<SubImageInfo>();
			subImageDimensions = span.clone();
			subImageSpanSize = span.length;
			subImagePosition = new int[subImageSpanSize];
		}

		if (span.length != subImageSpanSize)
			throw new IllegalArgumentException("span of new image subregion is not of the same dimensionality as existing images");

		for (int i = 0; i < subImageSpanSize; i++)
			if (span[i] != subImageDimensions[i])
				throw new IllegalArgumentException("span of new image subregion is not of the same dimension as existing image subregions");
				
		SubImageInfo info = new SubImageInfo();
		info.origin = origin;
		info.function = new ImageFunction(image);
		
		subImages.add(info);
	}
	
	@Override
	public double evaluate(int[] position)
	{
		SubImageInfo info = subImages.get(position[subImageSpanSize]);
		for (int i = 0; i < subImageSpanSize; i++)
			subImagePosition[i] = info.origin[i] + position[i];
		return info.function.evaluate(subImagePosition);
	}
	
}
