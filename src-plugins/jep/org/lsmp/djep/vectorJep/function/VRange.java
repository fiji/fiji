/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.lsmp.djep.vectorJep.function;

import org.nfunk.jep.*;
import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.values.*;

/**
 * A enhanced version of List, allows matrices and tensors.
 * During evaluation this function converts lists of values into the appropriate 
 * @see org.lsmp.djep.vectorJep.values.MatrixValueI MatrixValueI
 * type.
 * 
 * @author Rich Morris
 * Created on 27-Nov-2003
 */
public class VRange extends org.nfunk.jep.function.Range
	implements NaryOperatorI
{
	public VRange()
	{
		numberOfParameters = -1;
	}

	/** Calculates the dimension of this node with given dimensions of children. */
	public Dimensions calcDim(Dimensions dims[]) throws ParseException
	{
		return Dimensions.UNKNOWN;
	}
	
	/** Calculates the value of this node.
	 * 
	 */
	public MatrixValueI calcValue(MatrixValueI res,
		MatrixValueI inputs[]) throws ParseException
	{
		Object out;
		
		if(inputs.length == 2)
		{
			Object lastObj = inputs[1];
			Object firstObj  = inputs[0];
			double last = ((Number) lastObj).doubleValue();
			double first = ((Number) firstObj).doubleValue();
			double diff = last-first;
			int steps = 1+(int) diff;
			out=genRange(first,1.0,steps);
		}
		else if(inputs.length == 3)
		{
			Object incObj = inputs[2];
			Object lastObj = inputs[1];
			Object firstObj  = inputs[0];
			double inc = ((Number) incObj).doubleValue();
			double last = ((Number) lastObj).doubleValue();
			double first = ((Number) firstObj).doubleValue();
			double diff = (last-first)/inc;
			int steps = 1+(int) diff;
			out=genRange(first,inc,steps);
		}
		else if(inputs.length == 4)
		{
			Object stepsObj = inputs[4];
			//Object incObj = inputs[3];
			Object lastObj = inputs[2];
			Object firstObj  = inputs[0];
			int steps = ((Number) stepsObj).intValue();
			double last = ((Number) lastObj).doubleValue();
			double first = ((Number) firstObj).doubleValue();
			double inc = (last-first)/(steps-1);
			out=genRange(first,inc,steps);
		}
		else throw new ParseException("Range:only a maximum of four arguments can be specified");

		throw new ParseException("VRange: calcValue not implemented");
	}

	public Object genRange(double low, double inc, int steps) {
		MVector res = new MVector(steps);
		for(int i=0;i<steps;++i)
			res.setEle(i,new Double(low+inc*i));
		return res;
	}
	
	
}
