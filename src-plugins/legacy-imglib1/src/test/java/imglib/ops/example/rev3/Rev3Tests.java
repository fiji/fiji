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

package imglib.ops.example.rev3;

import static org.junit.Assert.assertEquals;
import imglib.ops.example.rev3.condition.ValueLessThan;
import imglib.ops.example.rev3.constraints.Constraints;
import imglib.ops.example.rev3.function.AverageFunction;
import imglib.ops.example.rev3.function.BinaryFunction;
import imglib.ops.example.rev3.function.ComposedImageFunction;
import imglib.ops.example.rev3.function.ConstantFunction;
import imglib.ops.example.rev3.function.ConvolutionFunction;
import imglib.ops.example.rev3.function.ImageFunction;
import imglib.ops.example.rev3.function.IntegerIndexedScalarFunction;
import imglib.ops.example.rev3.function.UnaryFunction;
import imglib.ops.example.rev3.operator.BinaryOperator;
import imglib.ops.example.rev3.operator.UnaryOperator;
import imglib.ops.example.rev3.operator.binary.AddOperator;
import imglib.ops.example.rev3.operator.binary.MultiplyOperator;
import imglib.ops.example.rev3.operator.unary.HalfOperator;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

import org.junit.Test;

/**
 * TODO
 *
 */
public class Rev3Tests
{

	// ************  private interface ********************************************************
	
	private static Image<UnsignedByteType> createImage(int width, int height)
	{
		ImageFactory<UnsignedByteType> factory = new ImageFactory<UnsignedByteType>(new UnsignedByteType(), new ArrayContainerFactory());
		
		return factory.createImage(new int[]{width,height});
	}

	private static Image<UnsignedByteType> createPopulatedImage(int width, int height, int[] values)
	{
		Image<UnsignedByteType> image = createImage(width, height);
		
		LocalizableByDimCursor<UnsignedByteType> cursor = image.createLocalizableByDimCursor();
		
		int[] position = new int[2];
		
		int i = 0;
		
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				position[0] = x;
				position[1] = y;
				cursor.setPosition(position);
				cursor.getType().setInteger(values[i++]);
			}
		}

		return image;
	}
	
	
	private static void assertImageValsEqual(int width, int height, int[] values, Image<UnsignedByteType> image)
	{
		LocalizableByDimCursor<UnsignedByteType> cursor = image.createLocalizableByDimCursor();

		int[] position = new int[2];
		
		int i = 0;
		
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				position[0] = x;
				position[1] = y;
				cursor.setPosition(position);
				assertEquals(values[i++], cursor.getType().getInteger());
			}
		}
	}

	// ************  Tests ********************************************************
	
	@Test
	public void testConstantFill()
	{
		Image<UnsignedByteType> outputImage = createPopulatedImage(3,3,new int[9]);
		
		ConstantFunction function = new ConstantFunction(43);
		
		Operation op = new Operation(outputImage, new int[3], new int[]{3,3}, function);
		
		op.execute();
		
		assertImageValsEqual(3,3,new int[]{43,43,43,43,43,43,43,43,43}, outputImage);
	}

	@Test
	public void testCopyOtherImage()
	{
		Image<UnsignedByteType> inputImage = createPopulatedImage(3,3,new int[]{1,2,3,4,5,6,7,8,9});
		
		Image<UnsignedByteType> outputImage = createPopulatedImage(3,3,new int[9]);
		
		assertImageValsEqual(3,3,new int[9], outputImage);

		ImageFunction function = new ImageFunction(inputImage);
		
		Operation op = new Operation(outputImage, new int[3], new int[]{3,3}, function);
		
		op.execute();
		
		assertImageValsEqual(3,3,new int[]{1,2,3,4,5,6,7,8,9}, inputImage);
		assertImageValsEqual(3,3,new int[]{1,2,3,4,5,6,7,8,9}, outputImage);
	}
	
	@Test
	public void testConvolve()
	{
		double[] kernel = new double[]{1,1,1,1,1,1,1,1,1};

		Image<UnsignedByteType> inputImage = createPopulatedImage(3,3,new int[]{1,2,3,4,5,6,7,8,9});
		
		Image<UnsignedByteType> outputImage = createPopulatedImage(3,3,new int[9]);
		
		assertImageValsEqual(3,3,new int[9], outputImage);

		ImageFunction imageFunction = new ImageFunction(inputImage);

		ConvolutionFunction convolver = new ConvolutionFunction(new int[]{3,3}, kernel, imageFunction);
		
		Operation op = new Operation(outputImage, new int[]{1,1}, new int[]{1,1}, convolver);
		
		op.execute();
		
		assertImageValsEqual(3,3,new int[]{0,0,0,0,45,0,0,0,0}, outputImage);
	}
	
	@Test
	public void testBinaryFunction()
	{
		Image<UnsignedByteType> leftImage = createPopulatedImage(3,3,new int[]{1,2,3,4,5,6,7,8,9});
		
		Image<UnsignedByteType> rightImage = createPopulatedImage(3,3,new int[]{10,20,30,40,50,60,70,80,90});

		Image<UnsignedByteType> outputImage = createPopulatedImage(3,3,new int[9]);

		assertImageValsEqual(3,3,new int[9], outputImage);

		ImageFunction leftImageFunction = new ImageFunction(leftImage);
		
		ImageFunction rightImageFunction = new ImageFunction(rightImage);

		BinaryOperator addOp = new AddOperator();
		
		BinaryFunction addFunc = new BinaryFunction(addOp, leftImageFunction, rightImageFunction);
		
		Operation op = new Operation(outputImage, new int[2], new int[]{3,3}, addFunc);
		
		op.execute();
		
		assertImageValsEqual(3,3,new int[]{11,22,33,44,55,66,77,88,99}, outputImage);
	}
	
	@Test
	public void testUnaryFunction()
	{
		Image<UnsignedByteType> inputImage = createPopulatedImage(3,3,new int[]{10,20,30,40,50,60,70,80,90});

		Image<UnsignedByteType> outputImage = createPopulatedImage(3,3,new int[9]);

		assertImageValsEqual(3,3,new int[9], outputImage);

		ImageFunction inputImageFunction = new ImageFunction(inputImage);
		
		UnaryOperator halfOp = new HalfOperator();
		
		UnaryFunction halfFunc = new UnaryFunction(halfOp, inputImageFunction);
		
		Operation op = new Operation(outputImage, new int[2], new int[]{3,3}, halfFunc);
		
		op.execute();
		
		assertImageValsEqual(3,3,new int[]{5,10,15,20,25,30,35,40,45}, outputImage);
	}

	@Test
	public void testComposedFunction()
	{
		// lets set an Image's values to half(2*Image1 + 3*Image2 + 4)
		
		Image<UnsignedByteType> inputImage1 = createPopulatedImage(3,3,new int[]{1,2,3,4,5,6,7,8,9});
		Image<UnsignedByteType> inputImage2 = createPopulatedImage(3,3,new int[]{5,10,15,20,25,30,35,40,45});
		Image<UnsignedByteType> outputImage = createPopulatedImage(3,3,new int[9]);

		MultiplyOperator multOp = new MultiplyOperator();
		AddOperator addOp = new AddOperator();
		HalfOperator halfOp = new HalfOperator();
		
		ImageFunction image1Func = new ImageFunction(inputImage1);
		ImageFunction image2Func = new ImageFunction(inputImage2);

		ConstantFunction two = new ConstantFunction(2);
		ConstantFunction three = new ConstantFunction(3);
		ConstantFunction four = new ConstantFunction(4);

		BinaryFunction term1 = new BinaryFunction(multOp, two, image1Func);
		
		BinaryFunction term2 = new BinaryFunction(multOp, three, image2Func);
		
		BinaryFunction twoTerms = new BinaryFunction(addOp, term1, term2);
		
		BinaryFunction threeTerms = new BinaryFunction(addOp, twoTerms, four);

		UnaryFunction totalFunc = new UnaryFunction(halfOp, threeTerms);
		
		Operation op = new Operation(outputImage, new int[2], new int[]{3,3}, totalFunc);
		
		op.execute();
		
		assertImageValsEqual(3,3,new int[]{11,19,28,36,45,53,62,70,79}, outputImage);  // NOTICE IT ROUNDS 0.5 UP ...
	}
	
	private class HalfPlanePositionFunction implements IntegerIndexedScalarFunction
	{
		private double xCoeff;
		private double yCoeff;
		
		public HalfPlanePositionFunction(double xCoeff, double yCoeff)
		{
			this.xCoeff = xCoeff;
			this.yCoeff = yCoeff;
		}

		@Override
		public double evaluate(int[] position)
		{
			return xCoeff*position[0] + yCoeff*position[1];
		}
	}
	
	@Test
	public void testConstraints()
	{
		// make an input image
		Image<UnsignedByteType> inputImage = createImage(9,9);
		LocalizableByDimCursor<UnsignedByteType> cursor = inputImage.createLocalizableByDimCursor();
		int[] pos = new int[2];
		int i = 0;
		for (int y = 0; y < 9; y++)
		{
			pos[1] = y;
			for (int x = 0; x < 9; x++)
			{
				pos[0] = x;
				cursor.setPosition(pos);
				cursor.getType().setReal(i++);
			}
		}

		// make an unassigned output image
		Image<UnsignedByteType> outputImage = createPopulatedImage(9,9,new int[81]);

		// make a constraint where we are interested in values to right of 
		
		ImageFunction imageFunction = new ImageFunction(inputImage);
		
		Operation op = new Operation(outputImage, new int[2], new int[]{9,9}, imageFunction);
		
		ValueLessThan lessThan22 = new ValueLessThan(22);

		HalfPlanePositionFunction halfPlaneEquation = new HalfPlanePositionFunction(2,3);
		
		Constraints constraints = new Constraints();
		
		constraints.addConstraint(halfPlaneEquation, lessThan22);
		
		op.setConstraints(constraints);
		
		op.execute();
		
		cursor = outputImage.createLocalizableByDimCursor();
		i = 0;
		for (int y = 0; y < 9; y++)
		{
			pos[1] = y;
			for (int x = 0; x < 9; x++)
			{
				pos[0] = x;
				cursor.setPosition(pos);
				double actualValue = cursor.getType().getRealDouble();
				double expectedValue;
				if (((2*x) + (3*y)) < 22)
					expectedValue = i;
				else
					expectedValue = 0;
				i++;
				assertEquals(expectedValue, actualValue, 0);
			}
		}
	}

	@Test
	public void testComposedImage()
	{
		// set an output image to the average of subregions of 3 planes
		
		// make input images
		
		LocalizableByDimCursor<UnsignedByteType> cursor;

		// make and populate an input image
		Image<UnsignedByteType> inputImage1 = createImage(9,9);
		cursor = inputImage1.createLocalizableByDimCursor();
		for (int x = 0; x < 9; x++)
		{
			for (int y = 0; y < 9; y++)
			{
				int[] pos = new int[]{x,y};
				cursor.setPosition(pos);
				cursor.getType().setReal(x*y);
			}
		}
		
		// make and populate an input image
		Image<UnsignedByteType> inputImage2 = createImage(9,9);
		cursor = inputImage2.createLocalizableByDimCursor();
		for (int x = 0; x < 9; x++)
		{
			for (int y = 0; y < 9; y++)
			{
				int[] pos = new int[]{x,y};
				cursor.setPosition(pos);
				cursor.getType().setReal(x);
			}
		}

		// make and populate an input image
		Image<UnsignedByteType> inputImage3 = createImage(9,9);
		cursor = inputImage3.createLocalizableByDimCursor();
		for (int x = 0; x < 9; x++)
		{
			for (int y = 0; y < 9; y++)
			{
				int[] pos = new int[]{x,y};
				cursor.setPosition(pos);
				cursor.getType().setReal(y);
			}
		}

		// make an unassigned output image
		
		int[] outputSpan = new int[]{5,5};
		
		Image<UnsignedByteType> outputImage = createImage(5,5);
		
		// compose a 3d image from the separate regions of the 2d images
		
		ComposedImageFunction composedImage = new ComposedImageFunction();
		composedImage.addImageRegion(inputImage1, new int[]{0,0}, outputSpan);
		composedImage.addImageRegion(inputImage2, new int[]{2,2}, outputSpan);
		composedImage.addImageRegion(inputImage3, new int[]{4,4}, outputSpan);
		
		// apply an average of x,y values along the z axis in the ComposedImage
		
		IntegerIndexedScalarFunction function = new AverageFunction(composedImage, new int[]{0,0,0}, new int[]{0,0,2});
		
		Operation op = new Operation(outputImage, new int[]{0,0}, outputSpan, function);
		
		op.execute();
		
		cursor = outputImage.createLocalizableByDimCursor();
		for (int x = 0; x < 5; x++)
		{
			for (int y = 0; y < 5; y++)
			{
				int[] pos = new int[]{x,y};
				cursor.setPosition(pos);
				assertEquals(Math.round(((x*y)+(x+2)+(y+4))/3.0), cursor.getType().getRealDouble(), 0);
			}
		}
	}
	
	// TODO
	// recreate all rev2 tests from NewFunctionlIdeas.java
}
