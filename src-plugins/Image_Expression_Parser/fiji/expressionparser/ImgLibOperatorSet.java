package fiji.expressionparser;

import mpicbg.imglib.type.NumericType;

import org.nfunk.jep.Operator;
import org.nfunk.jep.OperatorSet;

import fiji.expressionparser.function.ImgLibMultiply;

public class ImgLibOperatorSet <T extends NumericType<T>>extends OperatorSet {

	public ImgLibOperatorSet() {
		super();
		OP_MULTIPLY    =  new Operator("*",new ImgLibMultiply<T>());
	}

}
