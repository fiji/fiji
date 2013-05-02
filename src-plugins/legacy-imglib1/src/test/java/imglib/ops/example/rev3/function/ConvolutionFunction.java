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

// TODO - could be derived from a plain ScalarFunction taking real coords. probably want to do this eventually. a discrete one and a continuous one.

/**
 * TODO
 *
 */
public final class ConvolutionFunction implements IntegerIndexedScalarFunction
{
	private final IntegerIndexedScalarFunction otherFunction;
	private final int[] kernelDimensions;
	private final double[] kernelValues;
	private final int[] relPos;
	
	public ConvolutionFunction(int[] kernelDimensions, double[] kernelValues, IntegerIndexedScalarFunction otherFunction)
	{
		// TODO - hack - only work in two dims to get working
		if (kernelDimensions.length != 2)
			throw new IllegalArgumentException("temporarily only allowing 2d convolution");
		
		// TODO - hack - only work in odd dimensions to get things working
		if (((kernelDimensions[0] %2) == 0) || ((kernelDimensions[1] %2) == 0))
			throw new IllegalArgumentException("temporarily only allowing odd numbers for kernel dimensions");
		
		this.relPos = new int[2];  // temporary workspace : TODO - this is a 2d hack

		this.otherFunction = otherFunction;
		this.kernelDimensions = kernelDimensions;
		this.kernelValues = kernelValues;
	}
	
	@Override
	public double evaluate(int[] position)
	{
		double sum = 0;

		int xHalfRange = kernelDimensions[0] / 2;
		
		int yHalfRange = kernelDimensions[1] / 2;
		
		for (int y = -yHalfRange; y <= yHalfRange; y++)
		{
			relPos[1] = position[1] + y;
			
			for (int x = -xHalfRange; x <= xHalfRange; x++)
			{
				relPos[0] = position[0] + x;
				
				double value = otherFunction.evaluate(relPos);
				
				int kPos = relPos[1]*kernelDimensions[0] + relPos[0];
				
				double kernelValue = kernelValues[kPos];
				
				sum += kernelValue * value;
			}
		}
		
		return sum;
	}
}
