/* @author rich
 * Created on 14-Feb-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.matrixJep.function;

import org.lsmp.djep.matrixJep.*;
import org.lsmp.djep.matrixJep.nodeTypes.*;
import org.lsmp.djep.vectorJep.function.VMap;
import org.lsmp.djep.vectorJep.values.*;
import org.lsmp.djep.vectorJep.Dimensions;
import org.nfunk.jep.*;

/**
 * @author Rich Morris
 * Created on 14-Feb-2005
 */
public class MMap extends VMap implements SpecialPreProcessorI,MatrixSpecialEvaluationI
{
	public MatrixNodeI preprocess(
		ASTFunNode node,
		MatrixPreprocessor visitor,
		MatrixJep jep,
		MatrixNodeFactory nf)
		throws ParseException
	{
		Variable vars[] = getVars(node.jjtGetChild(1));
		for(int i=0;i<vars.length;++i)
		{
			((MatrixVariable) vars[i]).setDimensions(Dimensions.ONE);
		}
		MatrixNodeI children[] = visitor.visitChildrenAsArray(node,null);
		return (MatrixNodeI) nf.buildFunctionNode(node,children,children[2].getDim());
	}

	public MatrixValueI evaluate(
		MatrixNodeI node,
		MatrixEvaluator visitor,
		MatrixJep jep)
		throws ParseException
	{
		int nChild = node.jjtGetNumChildren();
		if(nChild <3)
			throw new ParseException("Map must have three or more arguments");
		
		// First find the variables
		Variable vars[] = getVars(node.jjtGetChild(1));
			
		if(nChild != vars.length + 2)
			throw new ParseException("Map: number of arguments should match number of variables + 2");

		// Now evaluate third and subsequent arguments
		MatrixValueI inputs[] = new MatrixValueI[nChild-2];
		Dimensions dim=null;
		for(int i=0;i<nChild-2;++i)
		{
			Object out = node.jjtGetChild(i+2).jjtAccept(visitor,null);	
			if(out instanceof MatrixValueI)
			{
				inputs[i] = (MatrixValueI) out;
				if(i==0) dim = inputs[0].getDim();
				else
				{
					if(!dim.equals(inputs[i].getDim()))
						throw new ParseException("Map: dimensions of third and subsequent arguments must match");
				}
			}
			else
				throw new ParseException("Map: third and following arguments should be vectos or matricies");
		}

		// Now evaluate the function for each element
		MatrixValueI res = node.getMValue();
		for(int i=0;i<dim.numEles();++i)
		{
			for(int j=0;j<vars.length;++j)
			{
				((MatrixVariableI) vars[j]).getMValue().setEle(0,inputs[j].getEle(i));
				vars[j].setValidValue(true);
			}
			Scaler val = (Scaler) node.jjtGetChild(0).jjtAccept(visitor,null);
			res.setEle(i,val.getEle(0));
		}
		return res;
	}

}
