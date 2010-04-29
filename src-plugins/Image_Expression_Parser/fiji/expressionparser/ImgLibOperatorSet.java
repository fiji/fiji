package fiji.expressionparser;

import mpicbg.imglib.type.numeric.RealType;

import org.nfunk.jep.Operator;
import org.nfunk.jep.OperatorSet;

import fiji.expressionparser.function.ImgLibAdd;
import fiji.expressionparser.function.ImgLibComparison;
import fiji.expressionparser.function.ImgLibDivide;
import fiji.expressionparser.function.ImgLibLogical;
import fiji.expressionparser.function.ImgLibModulus;
import fiji.expressionparser.function.ImgLibMultiply;
import fiji.expressionparser.function.ImgLibPower;
import fiji.expressionparser.function.ImgLibSubtract;
import fiji.expressionparser.function.ImgLibUMinus;

public class ImgLibOperatorSet <T extends RealType<T>> extends OperatorSet {

	public ImgLibOperatorSet() {
		super();
		OP_ADD 			= new Operator("+", new ImgLibAdd<T>());
		OP_MULTIPLY    	= new Operator("*",new ImgLibMultiply<T>());
		OP_SUBTRACT 	= new Operator("-", new ImgLibSubtract<T>());
		OP_DIVIDE 		= new Operator("/", new ImgLibDivide<T>());
		OP_MOD 			= new Operator("%", new ImgLibModulus<T>());

		OP_UMINUS 		= new Operator("UMinus", "-", new ImgLibUMinus<T>());

		OP_GE 			= new Operator(">=", new ImgLibComparison.GreaterOrEqual<T>());
		OP_GT			= new Operator(">", new ImgLibComparison.GreaterThan<T>());
		OP_LE 			= new Operator("<=", new ImgLibComparison.LowerOrEqual<T>());
		OP_LT			= new Operator("<", new ImgLibComparison.LowerThan<T>());
		OP_EQ			= new Operator("==", new ImgLibComparison.Equal<T>());
		OP_NE			= new Operator("!=", new ImgLibComparison.NotEqual<T>());
		
		OP_AND			= new Operator("&&", new ImgLibLogical.And<T>());
		OP_OR			= new Operator("||", new ImgLibLogical.Or<T>());
		OP_NOT			= new Operator("!", new ImgLibLogical.Not<T>());
		
		OP_POWER  		= new Operator("^",new ImgLibPower<T>());
		
	}

}
