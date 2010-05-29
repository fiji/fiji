/* @author rich
 * Created on 18-Nov-2003
 */
package org.lsmp.djep.xjep.function;

import org.nfunk.jep.*;
import org.nfunk.jep.function.*;

/**
 * A min function Min(x^2,x,1,10) finds the min of x^2 with x running from 1 to 10.
 *
 * @author Rich Morris
 * Created on 10-Sept-2004
 */
public class Min extends SumType {

	static Comparative comp = new Comparative(Comparative.LE);

	public Min()
	{
		super("Min");
	}

		
	public Object evaluate(Object elements[]) throws ParseException
	{
		Object ret;
		ret = elements[0];
		for(int i=1;i<elements.length;++i)
		{
			if(comp.lt(elements[i],ret))
			ret = elements[i];
		}
		return ret;
	}
}
