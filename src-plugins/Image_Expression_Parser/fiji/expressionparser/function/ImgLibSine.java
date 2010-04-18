package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;


public final class ImgLibSine <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise sine</h3> " +
		"This function computes the sine of an ImgLib image, taking " +
		"each pixel as its operand (must be in radians). " +
		"Calculations are done using <i>Math.sin</i>.";

	@Override
	public final float evaluate(T alpha) {
		return (float) Math.sin(alpha.getRealDouble());
	}

	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	public String getFunctionString() {
		return "sin";
	}
	
	@Override
	public String toString() {
		return "Pixel wise sine";
	}

}
