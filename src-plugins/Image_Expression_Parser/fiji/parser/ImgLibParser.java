package fiji.parser;

import org.nfunk.jep.JEP;
import org.nfunk.jep.type.NumberFactory;

public class ImgLibParser extends JEP {

	public ImgLibParser() {
		super();
		opSet = new ImgLibOperatorSet();
	}

	public ImgLibParser(JEP j) {
		super(j);
		opSet = new ImgLibOperatorSet();
	}

	public ImgLibParser(boolean traverseIn, boolean allowUndeclaredIn,
			boolean implicitMulIn, NumberFactory numberFactoryIn) {
		super(traverseIn, allowUndeclaredIn, implicitMulIn, numberFactoryIn);
		opSet = new ImgLibOperatorSet();
	}

}
