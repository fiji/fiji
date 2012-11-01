/* @author rich
 * Created on 26-Jul-2003
 */
package org.lsmp.djep.xjep;
import org.lsmp.djep.xjep.function.*;
import org.nfunk.jep.*;

/**
 * An OperatorSet where the operators have information about their commutativity etc.
 * 
 * @see XOperator
 * @author Rich Morris
 * Created on 26-Jul-2003
 */
public class XOperatorSet extends OperatorSet {
	
	private void annotateOperators(OperatorSet o) {
	OP_GT     =  new XOperator(o.getGT(),XOperator.BINARY+XOperator.LEFT+XOperator.TRANSITIVE);
	OP_LT     =  new XOperator(o.getLT(),XOperator.BINARY+XOperator.LEFT+XOperator.TRANSITIVE);
	OP_EQ     =  new XOperator(o.getEQ(),XOperator.BINARY+XOperator.LEFT+XOperator.EQUIVILENCE);
	OP_LE     =  new XOperator(o.getLE(),XOperator.BINARY+XOperator.LEFT+XOperator.REFLEXIVE+XOperator.TRANSITIVE);
	OP_GE     =  new XOperator(o.getGE(),XOperator.BINARY+XOperator.LEFT+XOperator.REFLEXIVE+XOperator.TRANSITIVE);
	OP_NE     =  new XOperator(o.getNE(),XOperator.BINARY+XOperator.LEFT+XOperator.SYMMETRIC);

	OP_AND    =  new XOperator(o.getAnd(),XOperator.BINARY+XOperator.LEFT+XOperator.COMMUTATIVE+XOperator.ASSOCIATIVE+XOperator.USE_BINDING_FOR_PRINT);
	OP_OR     =  new XOperator(o.getOr(),XOperator.BINARY+XOperator.LEFT+XOperator.COMMUTATIVE+XOperator.ASSOCIATIVE);
	OP_NOT    = new XOperator(o.getNot(),XOperator.UNARY+XOperator.RIGHT+XOperator.PREFIX+XOperator.SELF_INVERSE);

	OP_ADD   =  new XOperator(o.getAdd(),XOperator.BINARY+XOperator.LEFT+XOperator.COMMUTATIVE+XOperator.ASSOCIATIVE);
	OP_SUBTRACT  =  new XOperator(o.getSubtract(),XOperator.BINARY+XOperator.LEFT+XOperator.COMPOSITE+XOperator.USE_BINDING_FOR_PRINT);
	OP_UMINUS =  new XOperator(o.getUMinus(),XOperator.UNARY+XOperator.RIGHT+XOperator.PREFIX+XOperator.SELF_INVERSE);

	OP_MULTIPLY    =  new XOperator(o.getMultiply(),XOperator.BINARY+XOperator.LEFT+XOperator.COMMUTATIVE+XOperator.ASSOCIATIVE);
	OP_DIVIDE = new XOperator(o.getDivide(),XOperator.BINARY+XOperator.LEFT+XOperator.COMPOSITE);
	OP_MOD    = new XOperator(o.getMod(),XOperator.BINARY+XOperator.LEFT);
	/** unary division i.e. 1/x or x^(-1) **/ 
	OP_UDIVIDE =  new XOperator("UDivide","^-1",null,XOperator.UNARY+XOperator.RIGHT+XOperator.PREFIX+XOperator.SELF_INVERSE);

	OP_POWER  = new XOperator(o.getPower(),XOperator.BINARY+XOperator.LEFT);

	OP_ASSIGN = new XOperator("=",new XAssign(),XOperator.BINARY+XOperator.RIGHT); // 

	OP_DOT = new XOperator(o.getDot(),XOperator.BINARY+XOperator.LEFT); // 
	OP_CROSS = new XOperator(o.getCross(),XOperator.BINARY+XOperator.LEFT); // 
	OP_LIST = new XOperator(o.getList(),XOperator.NARY+XOperator.RIGHT); // 
	OP_ELEMENT = new XOperator(o.getElement(),XOperator.NARY+XOperator.RIGHT); // 
//	OP_RANGE = new XOperator(o.getRange(),XOperator.NARY+XOperator.RIGHT); //
	setPrecedenceTable(new Operator[][] 
		{	{OP_UMINUS},
			{OP_NOT},
			{OP_POWER},
			{OP_MULTIPLY,OP_DIVIDE,OP_MOD,OP_DOT,OP_CROSS},
			{OP_ADD,OP_SUBTRACT},
			{OP_LT,OP_LE},
			{OP_GT,OP_GE},
			{OP_EQ},
			{OP_NE},
			{OP_AND},
			{OP_OR},
			{OP_ASSIGN},
			});
	//printOperators();

	// 		
	((XOperator) OP_ADD).setInverseOp(OP_UMINUS);
	((XOperator) OP_ADD).setBinaryInverseOp(OP_SUBTRACT);
	((XOperator) OP_SUBTRACT).setRootOp(OP_ADD);
	((XOperator) OP_SUBTRACT).setInverseOp(OP_UMINUS);
	((XOperator) OP_UMINUS).setRootOp(OP_ADD);
	((XOperator) OP_UMINUS).setBinaryInverseOp(OP_SUBTRACT);
		
	((XOperator) OP_MULTIPLY).setInverseOp(OP_UDIVIDE);
	((XOperator) OP_MULTIPLY).setBinaryInverseOp(OP_DIVIDE);
	((XOperator) OP_DIVIDE).setRootOp(OP_MULTIPLY);
	((XOperator) OP_DIVIDE).setInverseOp(OP_UDIVIDE);
	((XOperator) OP_UDIVIDE).setRootOp(OP_MULTIPLY);
	((XOperator) OP_UDIVIDE).setBinaryInverseOp(OP_DIVIDE);
		
	// Set distributive over
	((XOperator) OP_UMINUS).setDistributiveOver(OP_ADD); // -(a+b) -> (-a) + (-b)
	((XOperator) OP_UMINUS).setDistributiveOver(OP_SUBTRACT); // -(a-b) -> (-a) - (-b)

	((XOperator) OP_MULTIPLY).setDistributiveOver(OP_ADD); // a*(b+c) -> a*b + a*c
	((XOperator) OP_MULTIPLY).setDistributiveOver(OP_SUBTRACT); // a*(b-c) -> a*b - a*c
	((XOperator) OP_MULTIPLY).setDistributiveOver(OP_UMINUS); // a*(-b) -> -(a*b)
	}

	/** Creates the operator set from a given set. Will
	 * use the names and pfmc's but adds info about the operators properties.
	 * Note changes pfmc for = from Assign to XAssign 	*/
	public XOperatorSet(OperatorSet opSet)
	{
		annotateOperators(opSet);
	}

	/** Create the standard set of operators. */
	public XOperatorSet()
	{
		annotateOperators(this);
	}
	
	/** 
	 * Sets the precedences of the operators according to order in the supplied array.
	 * For example
	 * <pre>
	 * 		setPrecedenceTable(new Operator[][] 
	 *		{	{OP_UMINUS},
	 *			{OP_NOT},
	 *			{OP_MUL,OP_DIV,OP_MOD},
	 *			{OP_PLUS,OP_MINUS},
	 *			{OP_LT,OP_LE},
	 *			{OP_GT,OP_GE},
	 *			{OP_EQ},
	 *			{OP_NE},
	 *			{OP_AND},
	 *			{OP_OR},
	 *			});
	 * </pre>
	 */

	public static final void setPrecedenceTable(Operator[][] precArray)
	{
		for(int i=0;i<precArray.length;++i)
			for(int j=0;j<precArray[i].length;++j)
				((XOperator) precArray[i][j]).setPrecedence(i);
	}
	
	/** Prints all the operators, with verbose representations of each operators properties. 
	 * 
	 */
	public void printOperators()
	{
		Operator ops[] = getOperators();
		int maxPrec = -1;
		for(int i=0;i<ops.length;++i)
			if(((XOperator) ops[i]).getPrecedence()>maxPrec) maxPrec=((XOperator) ops[i]).getPrecedence();
		for(int j=-1;j<=maxPrec;++j)
			for(int i=0;i<ops.length;++i)
				if(((XOperator) ops[i]).getPrecedence()==j)
					System.out.println(((XOperator) ops[i]).toFullString());
	}

}
