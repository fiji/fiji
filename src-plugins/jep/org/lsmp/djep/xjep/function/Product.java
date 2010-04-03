/* @author rich
 * Created on 18-Nov-2003
 */
package org.lsmp.djep.xjep.function;

import org.nfunk.jep.*;
import org.nfunk.jep.function.*;

/**
 * A product function product(x^2,x,1,10) finds the product of x^2 with x running from 1 to 10.
 *
 * @author Rich Morris
 * Created on 10-Sept-2004
 */
public class Product extends SumType {

	static Multiply mul = new Multiply();

	public Product()
	{
		super("Product");
	}

		
	public Object evaluate(Object elements[]) throws ParseException
	{
		Object ret;
		ret = elements[0];
		for(int i=1;i<elements.length;++i)
		{
			ret = mul.mul(ret,elements[i]);
		}
		return ret;
	}
}
