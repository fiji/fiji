/* @author rich
 * Created on 07-Mar-2004
 */
package org.lsmp.djep.vectorJep;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
import org.lsmp.djep.vectorJep.function.*;
/**
 * @author Rich Morris
 * Created on 07-Mar-2004
 */
public class VOperatorSet extends OperatorSet {

	/**
	 * 
	 */
	public VOperatorSet() {
		super();
		OP_ADD.setPFMC(new MAdd());
		OP_SUBTRACT.setPFMC(new MSubtract());
		OP_MULTIPLY.setPFMC(new MMultiply());
		OP_DIVIDE.setPFMC(new MDivide());
//		OP_MULTIPLY.setPFMC(new ElementMultiply());
		OP_POWER.setPFMC(new VPower());
		OP_UMINUS.setPFMC(new MUMinus());
		OP_DOT.setPFMC(new MDot());
		OP_CROSS.setPFMC(new ExteriorProduct());
		OP_LIST.setPFMC(new VList());
		OP_ELEMENT.setPFMC(new ArrayAccess());
//		OP_RANGE.setPFMC(new VRange());
	}

	/** When set the multiplication of vectors and matricies will be element by element.
	 * Otherwise multiplication will be matrix multiplication (the default).
	 * 
	 * @param flag
	 */
	public void setElementMultiply(boolean flag)
	{
		if(flag)
		{
			OP_MULTIPLY.setPFMC(new ElementMultiply());
			OP_DIVIDE.setPFMC(new ElementDivide());
			OP_GT.setPFMC(new ElementComparative(Comparative.GT));
			OP_LT.setPFMC(new ElementComparative(Comparative.LT));
			OP_EQ.setPFMC(new ElementComparative(Comparative.EQ));
			OP_LE.setPFMC(new ElementComparative(Comparative.LE));
			OP_GE.setPFMC(new ElementComparative(Comparative.GE));
			OP_NE.setPFMC(new ElementComparative(Comparative.NE));
		}
		else
		{
			OP_MULTIPLY.setPFMC(new MMultiply());
			OP_DIVIDE.setPFMC(new Divide());
			OP_GT.setPFMC(new Comparative(Comparative.GT));
			OP_LT.setPFMC(new Comparative(Comparative.LT));
			OP_EQ.setPFMC(new Comparative(Comparative.EQ));
			OP_LE.setPFMC(new Comparative(Comparative.LE));
			OP_GE.setPFMC(new Comparative(Comparative.GE));
			OP_NE.setPFMC(new Comparative(Comparative.NE));
		}
	}
}
