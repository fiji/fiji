/* @author rich
 * Created on 21-Jul-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.xjep.function;


import org.lsmp.djep.xjep.CommandVisitorI;
import org.lsmp.djep.xjep.MacroFunction;
import org.lsmp.djep.xjep.XJep;
import org.nfunk.jep.ASTConstant;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Allows functions to be defined in equations.
 * <pre>
 * XJep > Define("half",1,"x/2")
 * XJep > half(5)
 * </pre>
 * <p>
 * Currently the Define function is handled in the preprocessing step
 * <pre>
 * Node n = xj.parse("Define(\"sumToX\",1,\"x*(x+1)/2\")");
 * Node preproc = xj.preprocess(n);
 * </pre>
 * and preproc will be null if a Define statement is encountered.
 * This is probably a bug.
 * 
 * TODO improve syntax. So can have Define("half(x)",x/2)
 * TODO work out how to simplify and evaluate Define statements which don't really have a value. 
 * TODO fix parser so can do half(x) = x/2
 *  
 * @author Rich Morris
 * Created on 21-Jul-2005
 */
public class Define extends PostfixMathCommand implements CommandVisitorI {
    private XJep xj;
    public Define(XJep xj) {
        this.numberOfParameters = -1;
        this.xj = xj;
    }

    /**
     *
     */

    public Node process(Node node, Node[] children, XJep xjep)
            throws ParseException {
       String funName = null;
        int nArgs=-1;
        String def = null;
        if(children[0] instanceof ASTConstant)
        {
            Object val = ((ASTConstant) children[0]).getValue();
            if(val instanceof String) {
                funName = (String) val;
            }
            else throw new ParseException("First argument to Define must be a string");
        }
        if(children[1] instanceof ASTConstant)
        {
            Object val = ((ASTConstant) children[1]).getValue();
            if(val instanceof Number) {
                nArgs = ((Number) val).intValue();
            }
            else throw new ParseException("Second argument to Define must be a integer");
        }
        if(children[2] instanceof ASTConstant)
        {
            Object val = ((ASTConstant) children[2]).getValue();
            if(val instanceof String) {
                def = (String) val;
            }
            else throw new ParseException("Third argument to Define must be a string");
        }

        MacroFunction mf = new MacroFunction(funName,nArgs,def,xj);
        xj.addFunction(funName,mf);
        return null;
    }
}
