/* @author rich
 * Created on 01-Feb-2004
 */
package org.lsmp.djep.matrixJep.nodeTypes;
import org.nfunk.jep.*;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.*;

/**
 * @author Rich Morris
 * Created on 01-Feb-2004
 */
public class ASTMFunNode extends ASTFunNode implements MatrixNodeI 
{
	private MatrixValueI mvar=null;

	public ASTMFunNode(int i) {	super(i);}

	public Dimensions getDim()	{return mvar.getDim();	}

	public void setDim(Dimensions dim) {
		mvar = Tensor.getInstance(dim);
	}

	public MatrixValueI getMValue() {return mvar;}
	
}
