/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jep.type;

import org.nfunk.jep.ParseException;

/**
 * This interface can be implemented to create numbers of any object type.
 * By implementing this interface and calling the setNumberFactory() method of
 * the JEP class, the constants in an expression will be created with that
 * class.
 */
public interface NumberFactory {
	
	/**
	 * Creates a number object and initializes its value.
	 * @param value The initial value of the number as a string.
	 */
	public Object createNumber(String value) throws ParseException;
	/** Creates a number object with given double value. */
	public Object createNumber(double value) throws ParseException;
	/** Create a number object with given int value */
	public Object createNumber(int value) throws ParseException;
	/** Create a number object with given short value */
	public Object createNumber(short value) throws ParseException;
	/** Create a number object with given float value */
	public Object createNumber(float value) throws ParseException;
	/** Create a number object with given boolean value */
	public Object createNumber(boolean value) throws ParseException;
	/** Creates a number object from a class implementing Number,
	 * May actually just return the class. */
	public Object createNumber(Number value) throws ParseException;
	/** Create a number object with given Complex value */
	public Object createNumber(Complex value) throws ParseException;
	
	/** Return an object representing ZERO the additive identity. */
	public Object getZero();
	/** Return an object representing ONE the multiplicative identity. */
	public Object getOne();
	/** Return an object representing ZERO-ONE. */
	public Object getMinusOne();
	/** Return an object representing ONE+ONE. */
	public Object getTwo();
}
