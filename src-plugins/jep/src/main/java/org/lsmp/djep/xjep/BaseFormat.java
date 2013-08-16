/* @author rich
 * Created on 02-May-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.xjep;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import org.lsmp.djep.xjep.function.FromBase;
import org.lsmp.djep.xjep.function.ToBase;

/**
 * A Number format object which prints results in a specified base.
 * TODO Do something with the FieldPosition arguments.
 *  
 * @author Rich Morris
 * Created on 02-May-2005
 */
public class BaseFormat extends NumberFormat {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1805136353312272410L;
	int base;
    ToBase tb=null;
    FromBase fb=null;
    /**
     * 
     */
    private BaseFormat() {
        super();
        tb = new ToBase();
        fb = new FromBase();
    }

    /**
     * Create a new base format object 
     * @param base the base of number to use
     * @throws IllegalArgumentException if base is < 2 or > 36
     */
    public BaseFormat(int base) {
        super();
        this.base = base;
        tb = new ToBase(base);
        fb = new FromBase(base);
    }
    /**
     * Create a new base format object 
     * @param base the base of number to use
     * @param prefix prefix to appear before number
     * @throws IllegalArgumentException if base is < 2 or > 36
     */
    public BaseFormat(int base,String prefix) {
        super();
        this.base = base;
        tb = new ToBase(base,prefix);
        fb = new FromBase(base,prefix);
    }
    /**
     * Format a double value in specific base.
     * @param val the number to format
     * @param sb  the buffer to append to
     * @param fp  not used
     * @return the string buffer
     */
    public StringBuffer format(double val, StringBuffer sb,
            FieldPosition fp) {
        sb.append(tb.toBase(val,base,this.getMaximumFractionDigits()));
        return sb;
    }

    /**
     * Format a double value in specific base.
     * @param val the number to format
     * @param sb  the buffer to append to
     * @param fp  not used
     * @return the string buffer
     */
 
    public StringBuffer format(long val, StringBuffer sb, FieldPosition fp) {
        sb.append(tb.toBase(val,base));
        return sb;
    }

    /**
     * Not implemented
     */

    public Number parse(String arg0, ParsePosition arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
