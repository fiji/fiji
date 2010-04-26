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
 * @author Rich Morris
 * Created on 02-May-2005
 */
public class FromBase extends PostfixMathCommand {
    int globalBase=-1;
    String prefix=null;
    /**
     * 
     */
    public FromBase() {
        super();
        this.numberOfParameters = 2;
    }

    public FromBase(int base) {
        super();
        this.numberOfParameters = 1;
        globalBase = base;
    }

    public FromBase(int base,String prefix) {
        super();
        this.numberOfParameters = 1;
        globalBase = base;
        this.prefix = prefix;
    }

    public void run(Stack s) throws ParseException {
        int nargs = this.curNumberOfParameters;
        if(globalBase == -1 && nargs != 2)
            throw new ParseException("fromBase: number of arguments should be 2");
        if(globalBase != -1 && nargs != 1)
            throw new ParseException("fromBase: number of arguments should be 1");
        // find the base
        int base=globalBase;
        if(globalBase == -1) {
	        Object rhs = s.pop();
	        if(rhs instanceof Number)
	            base = ((Number) rhs).intValue();
	        else
	            throw new ParseException("toBase: second argument should be an integer");
        }
        Object lhs = s.pop();
        if(lhs instanceof String) {
            try {
                Object res = fromBase((String) lhs,base);
                s.push(res);
                return;
            } catch(NumberFormatException e) {
                throw new ParseException(e.getMessage());
            }
        }
        throw new ParseException("fromBase: first arg should be a string");
    }
    
    public Object fromBase(String str,int base) throws NumberFormatException {
        boolean sign = str.startsWith("-");
        if(sign) str = str.substring(1);
        
        // remove prefix
        if(prefix!=null) {
            if(str.startsWith(prefix)) {
                str = str.substring(prefix.length());
            } else 
                throw new NumberFormatException("fromBase: string must start with prefix "+prefix);
        }
        
        // work with decimal part
        int ind = str.indexOf('.');
        if(ind==-1) {
            double val = Long.parseLong(str,base);
            if(sign) val = -val;
            return new Double(val);
        }
        String intpart = str.substring(0,ind);
        String fractpart = str.substring(ind+1);
        long intlong = Long.parseLong(intpart,base);
        double fractlong = Long.parseLong(fractpart,base);
        double val = intlong + fractlong / Math.pow(base,fractpart.length());
        if(sign) val = -val;
        return new Double(val);
    }
}
