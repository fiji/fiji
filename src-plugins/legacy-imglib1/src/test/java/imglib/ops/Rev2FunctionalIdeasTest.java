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

package imglib.ops;

import static org.junit.Assert.assertEquals;
import imglib.ops.condition.PixelOnBorder;
import imglib.ops.condition.ValueGreaterThan;
import imglib.ops.condition.ValueLessThan;
import imglib.ops.function.p1.UnaryOperatorFunction;
import imglib.ops.function.pn.AvgFunction;
import imglib.ops.function.pn.ConstFunction;
import imglib.ops.operation.AssignOperation;
import imglib.ops.operator.UnaryOperator;
import imglib.ops.operator.unary.Sqr;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

import org.junit.Test;

@SuppressWarnings("unchecked")

/**
 * TODO
 *
 */
public class Rev2FunctionalIdeasTest
{
	// ************  private interface ********************************************************
	
	private static Image<UnsignedByteType> createImage()
	{
		ImageFactory<UnsignedByteType> factory = new ImageFactory<UnsignedByteType>(new UnsignedByteType(), new ArrayContainerFactory());
		
		return factory.createImage(new int[]{3,3});
	}

	private static Image<UnsignedByteType> createPopulatedImage(int[] values)
	{
		Image<UnsignedByteType> image = createImage();
		
		LocalizableByDimCursor<UnsignedByteType> cursor = image.createLocalizableByDimCursor();
		
		int[] position = new int[2];
		
		int i = 0;
		
		for (int y = 0; y < 3; y++)
		{
			for (int x = 0; x < 3; x++)
			{
				position[0] = x;
				position[1] = y;
				cursor.setPosition(position);
				cursor.getType().setInteger(values[i++]);
			}
		}

		return image;
	}
	
	
	private static void assertImageValsEqual(int[] values, Image<UnsignedByteType> image)
	{
		LocalizableByDimCursor<UnsignedByteType> cursor = image.createLocalizableByDimCursor();

		int[] position = new int[2];
		
		int i = 0;
		
		for (int y = 0; y < 3; y++)
		{
			for (int x = 0; x < 3; x++)
			{
				position[0] = x;
				position[1] = y;
				cursor.setPosition(position);
				assertEquals(values[i++], cursor.getType().getInteger());
			}
		}
	}
	
	// ************  tests ********************************************************

	@Test
	public void testOneImageSquaring()
	{
		//System.out.println("square all input values");
		
		Image<UnsignedByteType> image0 = createPopulatedImage(new int[]{1,2,3,4,5,6,7,8,9});
		
		UnaryOperator op = new Sqr();
		
		UnaryOperatorFunction<UnsignedByteType> function = new UnaryOperatorFunction<UnsignedByteType>(op);
		
		AssignOperation<UnsignedByteType> operation = new AssignOperation<UnsignedByteType>(new Image[]{image0}, image0, function);
		
		operation.execute();
		
		assertImageValsEqual(new int[]{1,4,9,16,25,36,49,64,81}, image0);

		//System.out.println("  success");
	}
	
	@Test
	public void testOneImageInputConditionGreater()
	{
		//System.out.println("square those where input values are greater than 4");
		
		Image<UnsignedByteType> image0 = createPopulatedImage(new int[]{1,2,3,4,5,6,7,8,9});
		
		UnaryOperator op = new Sqr();
		
		UnaryOperatorFunction<UnsignedByteType> function = new UnaryOperatorFunction<UnsignedByteType>(op);

		AssignOperation<UnsignedByteType> operation = new AssignOperation<UnsignedByteType>(new Image[]{image0}, image0, function);

		operation.setInputCondition(0, new ValueGreaterThan<UnsignedByteType>(4));
		
		operation.execute();
		
		assertImageValsEqual(new int[]{1,2,3,4,25,36,49,64,81}, image0);
		
		//System.out.println("  success");
	}
	
	@Test
	public void testOneImageOutputConditionLess()
	{
		//System.out.println("square those where original output values are less than 7");
		
		Image<UnsignedByteType> image0 = createPopulatedImage(new int[]{1,2,3,4,5,6,7,8,9});
		
		UnaryOperator op = new Sqr();
		
		UnaryOperatorFunction<UnsignedByteType> function = new UnaryOperatorFunction<UnsignedByteType>(op);

		AssignOperation<UnsignedByteType> operation = new AssignOperation<UnsignedByteType>(new Image[]{image0}, image0, function);

		operation.setOutputCondition(new ValueLessThan<UnsignedByteType>(7));
		
		operation.execute();
		
		assertImageValsEqual(new int[]{1,4,9,16,25,36,7,8,9}, image0);

		//System.out.println("  success");
	}
	
	@Test
	public void testSecondImageFromOneImageSquaring()
	{
		//System.out.println("square one image into another");
		
		Image<UnsignedByteType> image0 = createPopulatedImage(new int[]{1,2,3,4,5,6,7,8,9});
		
		Image<UnsignedByteType> image1 = createPopulatedImage(new int[]{0,0,0,0,0,0,0,0,0});
		
		UnaryOperator op = new Sqr();
		
		UnaryOperatorFunction<UnsignedByteType> function = new UnaryOperatorFunction<UnsignedByteType>(op);

		AssignOperation<UnsignedByteType> operation = new AssignOperation<UnsignedByteType>(new Image[]{image0}, image1, function);

		operation.execute();
		
		assertImageValsEqual(new int[]{1,2,3,4,5,6,7,8,9}, image0);
		assertImageValsEqual(new int[]{1,4,9,16,25,36,49,64,81}, image1);

		//System.out.println("  success");
	}
	
	@Test
	public void testThirdImageFromTwoImagesAveraging()
	{
		//System.out.println("average two images into third");
		
		Image<UnsignedByteType> image0 = createPopulatedImage(new int[]{1,2,3,4,5,6,7,8,9});
		
		Image<UnsignedByteType> image1 = createPopulatedImage(new int[]{11,12,13,14,15,16,17,18,19});

		Image<UnsignedByteType> image2 = createPopulatedImage(new int[]{0,0,0,0,0,0,0,0,0});
		
		AvgFunction<UnsignedByteType> function = new AvgFunction<UnsignedByteType>();

		AssignOperation<UnsignedByteType> operation = new AssignOperation<UnsignedByteType>(new Image[]{image0,image1}, image2, function);

		operation.execute();
		
		assertImageValsEqual(new int[]{1,2,3,4,5,6,7,8,9}, image0);
		assertImageValsEqual(new int[]{11,12,13,14,15,16,17,18,19}, image1);
		assertImageValsEqual(new int[]{6,7,8,9,10,11,12,13,14}, image2);

		//System.out.println("  success");
	}
	
	@Test
	public void testEverythingAveraging()
	{
		//System.out.println("average two images into third conditionally");
		
		Image<UnsignedByteType> image0 = createPopulatedImage(new int[]{1,2,3,
																		4,5,6,
																		7,8,9});
		
		Image<UnsignedByteType> image1 = createPopulatedImage(new int[]{11,12,13,
																		14,15,16,
																		17,18,19});

		Image<UnsignedByteType> image2 = createPopulatedImage(new int[]{5,5,6,
																		6,7,7,
																		8,8,9});
		
		AvgFunction<UnsignedByteType> function = new AvgFunction<UnsignedByteType>();

		AssignOperation<UnsignedByteType> operation = new AssignOperation<UnsignedByteType>(new Image[]{image0,image1}, image2, function);

		operation.setInputCondition(0, new ValueLessThan<UnsignedByteType>(8));
		operation.setInputRegion(0, new int[]{0,1}, new int[]{2,2});

		operation.setInputCondition(1, new ValueGreaterThan<UnsignedByteType>(14));
		operation.setInputRegion(1, new int[]{0,1}, new int[]{2,2});

		operation.setOutputRegion(new int[]{0,1}, new int[]{2,2});
		
		operation.execute();
		
		assertImageValsEqual(new int[]{1,2,3,4,5,6,7,8,9}, image0);
		assertImageValsEqual(new int[]{11,12,13,14,15,16,17,18,19}, image1);
		assertImageValsEqual(new int[]{5,5,6,6,10,7,12,8,9}, image2);

		//System.out.println("  success");
	}

	@Test
	public void testTwoNonOverlappingRegionsInSameImage()
	{
		//System.out.println("average nonoverlapping regions of a single images into a third");
		
		Image<UnsignedByteType> image0 = createPopulatedImage(new int[]{1,2,3,
																		4,5,6,
																		7,8,9});
		
		Image<UnsignedByteType> image1 = createPopulatedImage(new int[]{0,0,0,
																		0,0,0,
																		0,0,0});

		AvgFunction<UnsignedByteType> function = new AvgFunction<UnsignedByteType>();

		AssignOperation<UnsignedByteType> operation = new AssignOperation<UnsignedByteType>(new Image[]{image0,image0}, image1, function);

		operation.setInputRegion(0, new int[]{0,0}, new int[]{3,1});

		operation.setInputRegion(1, new int[]{0,2}, new int[]{3,1});
		
		operation.setOutputRegion(new int[]{0,2}, new int[]{3,1});

		operation.execute();
		
		assertImageValsEqual(new int[]{1,2,3,4,5,6,7,8,9}, image0);
		assertImageValsEqual(new int[]{0,0,0,0,0,0,4,5,6}, image1);

		//System.out.println("  success");
	}

	@Test
	public void testSpatialCondition()
	{
		Image<UnsignedByteType> image0 = createPopulatedImage(
				new int[]{0,0,0,
						0,255,0,
						0,255,0});

		Image<UnsignedByteType> image1 = createPopulatedImage(
				new int[]{0,0,0,
						0,0,0,
						0,0,0});
		
		ConstFunction<UnsignedByteType> function = new ConstFunction<UnsignedByteType>(1);

		AssignOperation<UnsignedByteType> operation = new AssignOperation<UnsignedByteType>(new Image[]{image0}, image1, function);
		
		PixelOnBorder<UnsignedByteType> condition = new PixelOnBorder<UnsignedByteType>(image0, 255);
		
		operation.setInputCondition(0, condition);
		
		operation.execute();
		
		assertImageValsEqual(new int[]{0,0,0,0,255,0,0,255,0}, image0);
		assertImageValsEqual(new int[]{0,0,0,0,1,0,0,1,0}, image1);
	}
}
