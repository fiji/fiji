/* @author rich
 * Created on 01-Feb-2004
 */
package org.lsmp.djep.matrixJep.nodeTypes;
import org.nfunk.jep.*;
import org.lsmp.djep.matrixJep.*;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.*;

/**
 * @author Rich Morris
 * Created on 01-Feb-2004
 */
public class ASTMVarNode extends ASTVarNode implements MatrixNodeI 
{
	private MatrixVariableI mvar=null;

	public ASTMVarNode(int i)	{super(i);}
	public Dimensions getDim() {return mvar.getDimensions();}
	public void setVar(Variable var)
	{
		mvar = (MatrixVariableI) var;
		super.setVar(var);
	}
	public MatrixValueI getMValue() {return mvar.getMValue();}
}
