/* @author rich
 * Created on 06-Mar-2004
 */
package org.lsmp.djep.groupJep.groups;
import org.lsmp.djep.groupJep.*;
import org.nfunk.jep.type.*;
import org.nfunk.jep.*;
/**
 * Base abstract class for all groups.
 * 
 * @author Rich Morris
 * Created on 06-Mar-2004
 */
public abstract class Group implements GroupI {
	/**
	* Creates a default NumberFactory which calls
	* the {@link org.lsmp.djep.groupJep.GroupI#valueOf} method of the subclass
	* to create strings from numbers.
	*/
	private NumberFactory NumFac = new NumberFactory()	{
		public Object createNumber(String s) {
			return valueOf(s);
		}
		/** Return an object representing ZERO the additive identity. */
		public Object getZero() { return getZERO() ; }
        public Object createNumber(double value) throws ParseException {
            // TODO Auto-generated method stub
            return null;
        }
        public Object createNumber(int value) throws ParseException {
            // TODO Auto-generated method stub
            return null;
        }
        public Object createNumber(short value) throws ParseException {
            // TODO Auto-generated method stub
            return null;
        }
        public Object createNumber(float value) throws ParseException {
            // TODO Auto-generated method stub
            return null;
        }
        public Object createNumber(boolean value) throws ParseException {
            // TODO Auto-generated method stub
            return null;
        }
        public Object createNumber(Number value) throws ParseException {
            // TODO Auto-generated method stub
            return null;
        }
        public Object createNumber(Complex value) throws ParseException {
            // TODO Auto-generated method stub
            return null;
        }
        public Object getOne() {
            // TODO Auto-generated method stub
            return null;
        }
        public Object getMinusOne() {
            // TODO Auto-generated method stub
            return null;
        }
        public Object getTwo() {
            // TODO Auto-generated method stub
            return null;
        }
	};

	/** returns a number factory for creating group elements from strings */
	public NumberFactory getNumberFactory() { return NumFac; }

	/** adds the standard constants for this group.
	 * By default does nothing. */
	public void addStandardConstants(JEP j) {}
	
	/** adds the standard function for this group 
	* By default does nothing. */
	public void addStandardFunctions(JEP j) {}
	
	public String toString()
	{
		return "general group";
	}
	
	
	/** Default implementation.
	 * Returns true.
	 */
	public boolean isConstantPoly(Number a) {
		return true;
	}

}
