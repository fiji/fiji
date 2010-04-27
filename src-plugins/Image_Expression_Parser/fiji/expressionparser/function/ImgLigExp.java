package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public class ImgLigExp <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise exponential</h3> " +
		"This function computes the exponential of an ImgLib image, taking " +
		"each pixel as its operand. " +
		"Calculations are done using <i>Math.exp</i>.";

	@Override
	public final float evaluate(T alpha) {
		return (float) Math.exp(alpha.getRealDouble());
	}

	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	public String getFunctionString() {
		return "exp";
	}
	
	@Override
	public String toString() {
		return "Pixel wise exponential";
	}

}
