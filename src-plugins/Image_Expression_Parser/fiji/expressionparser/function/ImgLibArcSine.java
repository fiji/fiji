package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibArcSine <T extends RealType<T>> extends
		SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise arc-sine</h3>" +
		"This function computes the arc sine of an ImgLib image, taking " +
		"each pixel as its operand. Calculations are done using Math.asin";

	
	@Override
	public float evaluate(T alpha) {
		return (float) Math.asin(alpha.getRealDouble());
	}

	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	@Override
	public String getFunctionString() {
		return "asin";
	}

}
