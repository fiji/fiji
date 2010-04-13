package fiji.parser;

import mpicbg.imglib.type.NumericType;

import org.nfunk.jep.Operator;
import org.nfunk.jep.OperatorSet;

import fiji.parser.function.Multiply;

public class ImgLibOperatorSet <T extends NumericType<T>>extends OperatorSet {

	public ImgLibOperatorSet() {
		super();
		OP_MULTIPLY    =  new Operator("*",new Multiply<T>());
	}

}
