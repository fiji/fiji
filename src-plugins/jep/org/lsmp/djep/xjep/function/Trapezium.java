/* @author rich
 * Created on 18-Nov-2003
 */
package org.lsmp.djep.xjep.function;

import org.nfunk.jep.*;
import org.nfunk.jep.function.*;

/**
 * The trapezium rule for approximation to a definite integral.
 * h * (y0 + yn + 2(y1+y2+...+y_(n-1)) /2
 * where h = (xn-x0)/n, yi = f(xi)
 * Trapezium(x^2,x,0,10,0.5) 
 * finds an approximation for int(x^2) where x runs from 0 to 10 in steps of 
 * h=0.5. 
 * 
 * @author Rich Morris
 * Created on 10-Sept-2004
 */
public class Trapezium extends SumType {

	static Add add = new Add();
	static Multiply mul = new Multiply();
	static Double HALF = new Double(1.0/2);
	static Double THIRD = new Double(1.0/3);
	static Double TWO = new Double(2);
	static Double FOUR = new Double(4);
	
	public Trapezium()
	{
		super("Trapezium");
	}
	public Trapezium(String name) { super(name); }
		
	public Object evaluate(Object elements[]) throws ParseException
	{
		if(elements.length <2)
			throw new ParseException("Trapezium: there should two or more ordinates, its"+elements.length);

		Object ret = mul.mul(HALF,
				add.add(elements[0],
						elements[elements.length-1]));
		for(int i=1;i<elements.length-1;++i)
		{
			ret = add.add(ret,elements[i]);
		}
		return ret;
	}
	
	public Object evaluate(
			Node node,
			Variable var,
			double min, double max, double inc,
			EvaluatorI pv)
			throws ParseException {
				
				int i=0;
				double val;
				Object[] res=new Object[(int) ((max-min)/inc)+1];	
				for(i=0,val=min;val<=max;++i,val=min+i*inc)
				{
					var.setValue(new Double(val));
					
					res[i] = pv.eval(node);
				}
				Object ret = evaluate(res);
				return mul.mul(ret,new Double(inc));
		}

}
