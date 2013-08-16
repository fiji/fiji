/* @author rich
 * Created on 14-Feb-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.vectorJep.function;

import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.CallbackEvaluationI;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * evaluates a function on every element of a vector or matrix.
 * Map(x^2,x,[1,2,3]) -> [1,4,9]
 * Map(x^y,[x,y],[1,2,3],[1,2,3]) -> [1,4,27]
 * First argument is a equation, second argument is the name or names of variables.
 * Third and subsequent arguments are vectors or matrices, they must have the same dimensions
 * and the number of subsequent arguments must match the number of variables specified in the second argument.
 * 
 * @author Rich Morris
 * Created on 14-Feb-2005
 */
public class VMap
	extends PostfixMathCommand
	implements NaryOperatorI, CallbackEvaluationI
{

	/**
	 * 
	 */
	public VMap()
	{
		super();
		this.numberOfParameters = -1;
	}

	public Dimensions calcDim(Dimensions[] dims) throws ParseException
	{
		return dims[2];
	}

	public MatrixValueI calcValue(MatrixValueI res, MatrixValueI[] inputs)
		throws ParseException
	{
		return null;
	}

	public static Variable[] getVars(Node varsNode) throws ParseException
	{
		Variable vars[]=null;
		if(varsNode instanceof ASTFunNode 
			&& ((ASTFunNode) varsNode).getPFMC() instanceof VList )
		{
			int nVars = varsNode.jjtGetNumChildren();
			vars = new Variable[nVars];
			for(int i=0;i<nVars;++i)
			{
				Node n = varsNode.jjtGetChild(i);
				if(n instanceof ASTVarNode)
				{
					vars[i] = ((ASTVarNode) n).getVar();
				}
				else
					throw new ParseException("Map: second argument should be list of variables");
			}
		}
		else if(varsNode instanceof ASTVarNode)
		{
			vars = new Variable[1];
			vars[0] = ((ASTVarNode) varsNode).getVar();
		}
		else
			throw new ParseException("Map: second argument should be a variable or list of variables");
		return vars;
	}
	
	public boolean checkNumberOfParameters(int n) {
		return (n >= 3);
	}

	public Object evaluate(
		Node node,
		EvaluatorI pv)
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
			Object out = pv.eval(node.jjtGetChild(i+2));
			if(out instanceof MatrixValueI)
			{
				inputs[i] = (MatrixValueI) out;
				if(i==0) dim = inputs[0].getDim();
				else
				{
					if(!dim.equals(inputs[i].getDim()))
						throw new ParseException("Map: dimensions of thrid and subsequent arguments must match");
				}
			}
			else
				throw new ParseException("Map: third and following arguments should be vectos or matricies");
		}

		// Now evaluate the function for each element
		MatrixValueI res = Tensor.getInstance(dim);
		for(int i=0;i<dim.numEles();++i)
		{
			for(int j=0;j<vars.length;++j)
				vars[j].setValue(inputs[j].getEle(i));
			Object val = pv.eval(node.jjtGetChild(0));
			res.setEle(i,val);
		}
		return res;
	}

}
