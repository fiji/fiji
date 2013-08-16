/* @author rich
 * Created on 10-Sep-2004
 */
package org.lsmp.djep.xjep.function;


import org.nfunk.jep.*;
import org.nfunk.jep.function.*;

/**
 * @author Rich Morris
 * Created on 10-Sep-2004
 */
public class MinArg extends SumType {

	static Comparative comp = new Comparative(Comparative.LE);

	public MinArg() {
		super("MinArg");
	}

	public Object evaluate(Object[] elements) throws ParseException {
		throw new ParseException("MinArg: call to evaluate(Object[] elements) should not have happened.");
	}

	public Object evaluate(
		Node node,
		Variable var,
		double min,
		double max,
		double inc,
		EvaluatorI pv)
		throws ParseException {

			int i=0;
			double val;
			Object minVal=null;
			Object minArg=null;
			for(i=0,val=min;val<=max;++i,val=min+i*inc)
			{
				Object curArg = new Double(val); 
				var.setValue(curArg);
				
				Object res = pv.eval(node);
				if(i==0)
				{
					minVal = res;
					minArg = curArg;
				}
				else if(comp.lt(res,minVal))
				{
					minVal = res;
					minArg = curArg;
				}
			}
			return minArg;

	}

}
