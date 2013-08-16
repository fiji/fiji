/* @author rich
 * Created on 27-Jul-2003
 */
package org.lsmp.djep.vectorJep.function;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
import java.util.*;

/**
 * An extension of the Divide class with vectors and matricies.
 * Must faster (1/3) if used with MatrixJep and calcValue routines.
 *  
 * @author Rich Morris
 * Created on 27-Jul-2003
 * TODO add handeling of tensors
 * @since 2.3.2 Improved error reporting
 */
public class MDivide extends Divide implements BinaryOperatorI {
	
	protected Add add = new Add();
	protected Subtract sub = new Subtract();
	
	public MDivide()
	{
		//add = (Add) Operator.OP_ADD.getPFMC();
		//sub = (Subtract) Operator.OP_SUBTRACT.getPFMC();
		numberOfParameters = 2;
	}

	/**
	 *  Need to redo this as the standard jep version assumes commutivity.
	 */	
	public void run(Stack stack) throws ParseException 
	{
		checkStack(stack); // check the stack
		//if(this.curNumberOfParameters!=2) throw new ParseException("Multiply: should have two children its got "+stack.size());
		Object param2 = stack.pop();
		Object param1 = stack.pop();
		Object product = div(param1, param2);
		stack.push(product);
		return;
	}

	/**
	 * Divide two objects.
	 */
	public Object div(Object param1, Object param2) throws ParseException 
	{
		if(param1 instanceof MatrixValueI && param2 instanceof MatrixValueI)
		{
			return div((MatrixValueI) param1,(MatrixValueI) param2);
		}
		else if(param1 instanceof MatrixValueI)
		{
			MatrixValueI l = (MatrixValueI) param1;
			MatrixValueI res = Tensor.getInstance(l.getDim());
			for(int i=0;i<res.getNumEles();++i)
				res.setEle(i,super.div(l.getEle(i),param2));
			return res;
		}
		else if(param2 instanceof MatrixValueI)
		{
			MatrixValueI r = (MatrixValueI) param2;
			MatrixValueI res = Tensor.getInstance(r.getDim());
			for(int i=0;i<res.getNumEles();++i)
				res.setEle(i,super.div(param1,r.getEle(i)));
			return res;
		}
		return super.div(param1,param2);
	}
		
	/**
	 * Divide two objects.
	 */
	public Object div(MatrixValueI param1, MatrixValueI param2) throws ParseException 
	{
		Dimensions dims = this.calcDim(param1.getDim(),param2.getDim());
		MatrixValueI res = Tensor.getInstance(dims);
		return this.calcValue(res,param1,param2);
	}

	public Dimensions calcDim(Dimensions l,Dimensions r) throws ParseException
	{
		int rrank = r.rank();
		if(rrank!=0) throw new ParseException("MDivide: right hand side must be a scaler. It has dimension "+r.toString());
		return l;
	}

	public MatrixValueI calcValue(MatrixValueI res,MatrixValueI param1,MatrixValueI param2) throws ParseException
	{
		if(param2 instanceof Scaler)
		{
			for(int i=0;i<param1.getDim().numEles();++i)
				res.setEle(i,super.div(param1.getEle(i),param2.getEle(0)));
		}
		else throw new ParseException("MDivide: right hand side must be a scaler. It has dimension "+param2.getDim().toString());
		return res;
	}
}
