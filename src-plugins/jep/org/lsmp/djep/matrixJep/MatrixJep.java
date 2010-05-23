/* @author rich
 * Created on 19-Dec-2003
 */
package org.lsmp.djep.matrixJep;

import org.lsmp.djep.djep.*;
import org.lsmp.djep.djep.diffRules.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.Power;
import org.lsmp.djep.vectorJep.values.*;
import org.lsmp.djep.vectorJep.function.*;
import org.lsmp.djep.xjep.*;
import org.lsmp.djep.matrixJep.function.*;
import org.lsmp.djep.matrixJep.nodeTypes.*;
/**
 * An extension of JEP which allows advanced vector and matrix handling and differentation.
 *
 * @author Rich Morris
 * Created on 19-Dec-2003
 */
public class MatrixJep extends DJep {

	protected MatrixPreprocessor dec = new MatrixPreprocessor();
	protected MatrixVariableFactory mvf = new MatrixVariableFactory();
	protected MatrixEvaluator mev = new MatrixEvaluator();
	
	public MatrixJep() {
		super();
		nf = new MatrixNodeFactory(this);
		symTab = new DSymbolTable(mvf);
		opSet = new MatrixOperatorSet();
		this.parser.setInitialTokenManagerState(ParserConstants.NO_DOT_IN_IDENTIFIERS);

		Operator tens = ((MatrixOperatorSet) opSet).getMList();
		pv.addSpecialRule(tens,(PrintVisitor.PrintRulesI) tens.getPFMC());
		Operator eleAccess = ((MatrixOperatorSet) opSet).getElement();
		pv.addSpecialRule(eleAccess,(PrintVisitor.PrintRulesI) eleAccess.getPFMC());

		addDiffRule(new PassThroughDiffRule(tens.getName(),tens.getPFMC()));
		Operator cross = ((MatrixOperatorSet) opSet).getCross();
		addDiffRule(new MultiplyDiffRule(cross.getName(),cross));
		Operator dot = ((MatrixOperatorSet) opSet).getDot();
		addDiffRule(new MultiplyDiffRule(dot.getName(),dot));
	}

	public void addStandardFunctions()
	{
		super.addStandardFunctions();
		addFunction("pow",new Power());
		this.getFunctionTable().remove("if");
		addFunction("if",new MIf());
		addFunction("ele",new VEle());
		this.getFunctionTable().remove("diff");
		addFunction("diff",new MDiff());
		addFunction("len",new Length());
		addFunction("size",new Size());
		addFunction("diag",new Diagonal());
		addFunction("getdiag",new GetDiagonal());
		addFunction("trans",new Transpose());
		addFunction("det",new Determinant());
		addFunction("trace",new Trace());
		addFunction("vsum",new VSum());
		addFunction("Map",new MMap());
		this.getFunctionTable().remove("Sum");
		addFunction("Sum",new MSum(this));
	}

	/** Evaluate a node. If the result is a scaler it
	 * will be unwrapped, i.e. it will return a Double and not a Scaler.
	 */
	public Object evaluate(Node node) throws ParseException
	{
		Object res = mev.evaluate((MatrixNodeI) node,this);
		if(res instanceof Scaler)
			return ((Scaler) res).getEle(0);
		return res;
	}

	/** Evaluate a node. Does not unwrap scalers. */
	public Object evaluateRaw(Node node) throws ParseException
	{
		Object res = mev.evaluate((MatrixNodeI) node,this);
		return res;
	}

	/** Pre-processes an equation to allow the diff and eval operators to be used. */
	public Node preprocess(Node node) throws ParseException
	{
		return dec.preprocess(node,this);
	}

	/* (non-Javadoc)
	 * @see org.nfunk.jep.JEP#getValueAsObject()
	 */
	public Object getValueAsObject() {
		try
		{
			Object res = mev.evaluate((MatrixNodeI) getTopNode(),this);
			if(res instanceof Scaler)
				return ((Scaler) res).getEle(0);
			return res;
		}
		catch(Exception e)
		{
			this.errorList.addElement("Error during evaluation:");
			this.errorList.addElement(e.getMessage());
			return null;
		}
	}
}
