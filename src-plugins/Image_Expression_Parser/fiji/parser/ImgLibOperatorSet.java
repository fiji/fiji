package fiji.parser;

import org.nfunk.jep.Operator;
import org.nfunk.jep.OperatorSet;

import fiji.parser.function.Multiply;

public class ImgLibOperatorSet extends OperatorSet {

	public ImgLibOperatorSet() {
		super();
		OP_MULTIPLY    =  new Operator("*",new Multiply());
	}

}
