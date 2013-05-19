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

import java.util.Arrays;

/**
 * TODO
 *
 */
public final class MedianFunction implements IntegerIndexedScalarFunction
{
	private final IntegerIndexedScalarFunction otherFunction;
	private final int[] loDeltas;
	private final int[] hiDeltas;
	private final int[] relPos;
	private final double[] workspace;
	
	public MedianFunction(IntegerIndexedScalarFunction otherFunction, int[] loDeltas, int[] hiDeltas)
	{
		this.otherFunction = otherFunction;
		this.loDeltas = loDeltas;
		this.hiDeltas = hiDeltas;
		
		if (loDeltas.length != 2) // TODO - hack - make work in 2d only to get started
			throw new IllegalArgumentException("onbly 2d median supported");
		
		relPos = new int[2];
		
		int numCols = hiDeltas[0] + 1 + Math.abs(loDeltas[0]);
		int numRows = hiDeltas[1] + 1 + Math.abs(loDeltas[1]);
		
		workspace = new double[numCols * numRows];
	}
	
	@Override
	public double evaluate(int[] position)
	{
		int numElements = 0;
		
		for (int dy = loDeltas[1]; dy <= hiDeltas[1]; dy++)
		{
			relPos[1] = position[1] + dy;
			for (int dx = loDeltas[0]; dx <= hiDeltas[0]; dx++)
			{
				relPos[0] = position[0] + dx;
				workspace[numElements++] = otherFunction.evaluate(relPos);
			}
		}

		Arrays.sort(workspace);  // TODO - slow but works
		
		double median;
		if ((numElements % 2) == 0)  // even number of elements - return the average of the middle two
		{
			double middle1 = workspace[numElements/2 - 1];

			double middle2 = workspace[numElements/2];
			
			median = (middle1 + middle2) / 2.0;
		}
		else  // odd number of elements -- return the middle one
			median = workspace[numElements/2];
		
		return median;
	}

}
