package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;


public final class ImgLibCosine <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise cosine</h3>" +
		"This function computes the cosine of an ImgLib image, taking" +
		"each pixel as its operand (must be in radians). " +
		"Calculations are done using Math.cos";

	@Override
	public final float evaluate(T alpha) {
		return (float) Math.cos(alpha.getRealDouble());
	}

	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	public String getFunctionString() {
		return "cos";
	}

}
