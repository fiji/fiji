/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jep.type;

import org.nfunk.jep.ParseException;

/**
 * Default class for creating number objects. This class can be replaced by
 * other NumberFactory implementations if other number types are required. This
 * can be done using the 
 */
public class DoubleNumberFactory implements NumberFactory {
	public static Double ZERO = new Double(0.0);
	public static Double ONE = new Double(1.0);
	public static Double TWO = new Double(2.0);
	public static Double MINUSONE = new Double(-1.0);
	
	/**
	 * Creates a Double object initialized to the value of the parameter.
	 *
	 * @param value The initialization value for the returned object.
	 */
	public Object createNumber(String value) {	return new Double(value);	}
	public Object createNumber(double value) {	return new Double(value);	}
	public Object createNumber(Number value) {	return value;	}
	public Object createNumber(boolean value) { return (value?ONE:ZERO); }
	public Object createNumber(float value) {return new Double(value);	}
	public Object createNumber(int value) {return new Double(value);	}
	public Object createNumber(short value) {return new Double(value);	}
	public Object createNumber(Complex value)  throws ParseException {
		throw new ParseException("Cannot create a number from a Complex value");	
	}
	public Object getMinusOne() {return MINUSONE;}
	public Object getOne() {return ONE;}
	public Object getTwo() {return TWO;}
	public Object getZero() {return ZERO;}
}
