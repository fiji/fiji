/* @author rich
 * Created on 02-May-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.xjep.function;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Convert a number to a string in a given base.
 * toBase(val,12) converts to base 12 numbers.
 * toBase(val,16,3) converts to base 12 with 3 hex digits after decimal place.
 * toHex(val) converts to base 16
 * toHex(val,3) converts to base 16 with 3 hex digits after decimal place.
 * A prefix can be specified in the constructor. If set
 * this will be appended to the number (after minus sign for negative values).
 * 
 * @author Rich Morris
 * Created on 02-May-2005
 * @see java.lang.Long#toString(long, int) 
 */
public class ToBase extends PostfixMathCommand {
    int globalBase=-1;
    String prefix="";
    /**
     * Constructor where base is specified as a function argument.
     */
    public ToBase() {
        super();
        this.numberOfParameters = -1;
    }
    /**
     * Constructor with specified base.
     * @param base the base to use
     * @throws IllegalArgumentException if base is < 2 or > 36
     */
    public ToBase(int base) {
        if(base < Character.MIN_RADIX || base > Character.MAX_RADIX)
            throw new IllegalArgumentException("base must be between "+Character.MIN_RADIX+" and "+Character.MIN_RADIX);
 
        globalBase = base;
        numberOfParameters = -1;
    }
    /**
     * Constructor with specified base and a given prefix.
     * For example 0x to proceed hexadecimal numbers.
     * @param base the base to use
     * @param prefix the string to prefix numbers with.
     * @throws IllegalArgumentException if base is < 2 or > 36
     */
    public ToBase(int base,String prefix) {
        if(base < Character.MIN_RADIX || base > Character.MAX_RADIX)
            throw new IllegalArgumentException("base must be between "+Character.MIN_RADIX+" and "+Character.MIN_RADIX);
 
        globalBase = base;
        numberOfParameters = -1;
        this.prefix = prefix;
    }
    
    public boolean checkNumberOfParameters(int n) {
        if(globalBase == -1) { return (n==2||n==3); }
        else				 {return (n==1||n==2); }
	}
	public void run(Stack s) throws ParseException {
        int narg = curNumberOfParameters;
        int digits=0;
        int base = 0;
        
        if(!checkNumberOfParameters(narg))
            throw new ParseException("toBase: can only have 1,2 or 3 arguments");

        if(narg==3 || (globalBase != -1 && narg==2)) {
            try {
            digits = ((Number) s.pop()).intValue();
            } catch(ClassCastException e) {
                throw new ParseException("toBase: last argument should be an integer");
            }
        }

        if(globalBase == -1) {
	        Object rhs = s.pop();
	        if(rhs instanceof Number)
	            base = ((Number) rhs).intValue();
	        else
	            throw new ParseException("toBase: second argument should be an integer");
        } else {
            base = globalBase;
        }
        if(base < Character.MIN_RADIX || base > Character.MAX_RADIX)
            throw new ParseException("base must be between "+Character.MIN_RADIX+" and "+Character.MIN_RADIX);

        Object lhs = s.pop();
        String res=null;
        if(lhs instanceof Integer || lhs instanceof Short || lhs instanceof Long)
            res = toBase(((Number) lhs).longValue(),base);
        else if(lhs instanceof Float || lhs instanceof Double || lhs instanceof Number)
            res = toBase(((Number) lhs).doubleValue(),base,digits);
        else
            throw new ParseException("toBase: Cannot convert object of type "+lhs.getClass().getName());
    	s.push(res);
	}
    
    /**
     * Converts a number to a give base.
     * @param num number to convert
     * @param base base to use
     * @return String representation
     * @throws IllegalArgumentException if base is < 2 or > 36
     */
    public String toBase(long num,int base) {
        if(base < Character.MIN_RADIX || base > Character.MAX_RADIX)
            throw new IllegalArgumentException("base must be between "+Character.MIN_RADIX+" and "+Character.MIN_RADIX);
        if(num<0)
            return '-' + prefix + Long.toString(num,base);
        return prefix + Long.toString(num,base);
    }
    
    /**
     * Converts a number to a give base.
     * @param val number to convert
     * @param base base to use
     * @param digits number of digits after decimal place
     * @return String representation
     * @throws IllegalArgumentException if base is < 2 or > 36
     */
    public String toBase(double val,int base,int digits) {
        if(base < Character.MIN_RADIX || base > Character.MAX_RADIX)
            throw new IllegalArgumentException("base must be between "+Character.MIN_RADIX+" and "+Character.MIN_RADIX);

        StringBuffer sb = new StringBuffer();
        if(val<0.0) { 
            val = -val; 
            sb.append('-');
        }
        sb.append(prefix);
        val = val * Math.pow(base,digits);
        long round = Math.round(val);
        String s = Long.toString(round,base);
        if(s.length()<=digits) {
            sb.append("0.");
            for(int i=0;i<digits-s.length();++i)
                sb.append('0');
            sb.append(s);
        }
        else if(digits>0) {
            sb.append(s);
            sb.insert(sb.length()-digits,'.');
        }
        else
            sb.append(s);
        return sb.toString();
    }
}
