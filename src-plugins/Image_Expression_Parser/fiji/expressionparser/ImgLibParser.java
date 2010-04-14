package fiji.expressionparser;

import mpicbg.imglib.type.NumericType;

import org.nfunk.jep.JEP;
import org.nfunk.jep.type.NumberFactory;

public class ImgLibParser <T extends NumericType<T>> extends JEP {

	public ImgLibParser() {
		super();
		opSet = new ImgLibOperatorSet<T>();
	}

	public ImgLibParser(JEP j) {
		super(j);
		opSet = new ImgLibOperatorSet<T>();
	}

	public ImgLibParser(boolean traverseIn, boolean allowUndeclaredIn,
			boolean implicitMulIn, NumberFactory numberFactoryIn) {
		super(traverseIn, allowUndeclaredIn, implicitMulIn, numberFactoryIn);
		opSet = new ImgLibOperatorSet<T>();
	}

}
