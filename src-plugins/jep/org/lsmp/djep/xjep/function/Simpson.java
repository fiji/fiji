/* @author rich
 * Created on 18-Nov-2003
 */
package org.lsmp.djep.xjep.function;

import org.nfunk.jep.*;

/**
 * The Simpson rule for approximation to a definite integral.
 * h * (y0 + yn + 4(y1+y3+...+y_(n-1)) + 2(y2+y4+...+y_(n-2)) ) /3
 * where h = (xn-x0)/n, yi = f(xi)
 * Simpson(x^2,x,0,10,0.5) 
 * finds an approximation for int(x^2) where x runs from 0 to 10 in steps of 
 * h=0.5. 
 *
 * @author Rich Morris
 * Created on 10-Sept-2004
 */
public class Simpson extends Trapezium {

	public Simpson()
	{
		super("Simpson");
	}

		
	public Object evaluate(Object elements[]) throws ParseException
	{
		if(elements.length % 2 != 1 || elements.length <2)
			throw new ParseException("Simpson: there should be an odd number of ordinates, its"+elements.length);

		Object ret = add.add(elements[0],elements[elements.length-1]);
		for(int i=1;i<elements.length-1;++i) { 
			//TODO could be quicker
			if(i %2 == 0)
				ret = add.add(ret,
						mul.mul(TWO,elements[i]));
			else
				ret = add.add(ret,
					mul.mul(FOUR,elements[i]));
		}
		return mul.mul(ret,THIRD);
	}
}
