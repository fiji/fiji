package fiji.expressionparser;

import mpicbg.imglib.type.NumericType;

import org.nfunk.jep.Operator;
import org.nfunk.jep.OperatorSet;

import fiji.expressionparser.function.ImgLibAdd;
import fiji.expressionparser.function.ImgLibDivide;
import fiji.expressionparser.function.ImgLibMultiply;
import fiji.expressionparser.function.ImgLibSubtract;

public class ImgLibOperatorSet <T extends NumericType<T>> extends OperatorSet {

	public ImgLibOperatorSet() {
		super();
		OP_ADD 			= new Operator("+", new ImgLibAdd<T>());
		OP_MULTIPLY    	= new Operator("*",new ImgLibMultiply<T>());
		OP_SUBTRACT 	= new Operator("-", new ImgLibSubtract<T>());
		OP_DIVIDE 		= new Operator("/", new ImgLibDivide<T>());
	}

}
