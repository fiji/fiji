/* @author rich
 * Created on 10-Dec-2004
 */
package org.lsmp.djep.vectorJep;

import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
import org.lsmp.djep.vectorJep.function.*;
import org.lsmp.djep.vectorJep.values.*;
/**
 * @author Rich Morris
 * Created on 10-Dec-2004
 */
public class VectorEvaluator extends EvaluatorVisitor {

	/**
	 * Visit a function node. The values of the child nodes
	 * are first pushed onto the stack. Then the function class associated
	 * with the node is used to evaluate the function.
	 * <p>
	 * If a function implements SpecialEvaluationI then the
	 * evaluate method of PFMC is called.
	 */
	public Object visit(ASTFunNode node, Object data) throws ParseException {
		if (node == null)
			return null;
		PostfixMathCommandI pfmc = node.getPFMC();

		// check if the function class is set
		if (pfmc == null)
			throw new ParseException(
				"No function class associated with " + node.getName());

		int numChild = node.jjtGetNumChildren();
		
		// Some operators (=) need a special method for evaluation
		// as the pfmc.run method does not have enough information
		// in such cases we call the evaluate method which passes
		// all available info. Note evaluating the children is
		// the responsibility of the evaluate method. 
		if (pfmc instanceof SpecialEvaluationI) {
			return ((SpecialEvaluationI) node.getPFMC()).evaluate(
				node,data,this,stack,this.symTab);
		}
		if(pfmc instanceof CallbackEvaluationI) {
			Object value = ((CallbackEvaluationI) pfmc).evaluate(node,this);
			stack.push(value);
			return value;
		}
		if (debug == true) {
			System.out.println(
				"Stack size before childrenAccept: " + stack.size());
		}

		// evaluate all children (each leaves their result on the stack)

		data = node.childrenAccept(this, data);

		if (debug == true) {
			System.out.println(
				"Stack size after childrenAccept: " + stack.size());
		}

		if (pfmc.getNumberOfParameters() == -1) {
			// need to tell the class how many parameters it can take off
			// the stack because it accepts a variable number of params
			pfmc.setCurNumberOfParameters(numChild);
		}

		// try to run the function

		if(pfmc instanceof UnaryOperatorI ||
		pfmc instanceof BinaryOperatorI ||
		pfmc instanceof NaryOperatorI || 
		pfmc instanceof Comparative)
			pfmc.run(stack);
		else if(numChild == 0)
		{
			pfmc.run(stack);
		}
		else /* perform operations element by element */
		{
			if(stack.peek() instanceof MatrixValueI)
			{
				MatrixValueI args[] = new MatrixValueI[node.jjtGetNumChildren()];
				args[numChild-1] = (MatrixValueI) stack.pop();
				Dimensions lastDim = args[numChild-1].getDim();
				for(int i=numChild-2;i>=0;--i)
				{
					Object val = stack.pop();
					if(!(val instanceof MatrixValueI))
						throw new ParseException("All arguments of function must be same dimension");
					args[i]=(MatrixValueI) val;
					if(!lastDim.equals(args[i].getDim()))
						throw new ParseException("All arguments of function must be same dimension");
				}
				MatrixValueI res = Tensor.getInstance(lastDim);
				for(int i=0;i<lastDim.numEles();++i)
				{
					for(int j=0;j<numChild;++j)
						stack.push(args[j].getEle(i));
					pfmc.run(stack);
					res.setEle(i,stack.pop());
				}
				stack.push(res);
			}
			else pfmc.run(stack);
		}

		if (debug == true) {
			System.out.println("Stack size after run: " + stack.size());
		}

		return data;
	}

}
