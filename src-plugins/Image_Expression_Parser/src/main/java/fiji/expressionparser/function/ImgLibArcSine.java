package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibArcSine <T extends RealType<T>> extends
		SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise arc-sine</h3> " +
		"This function computes the arc sine of an ImgLib image, taking " +
		"each pixel as its operand. Calculations are done using <i>Math.asin</i>. " +
		"Values returned are in the range [-π/2, π/2]";

	public ImgLibArcSine() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R alpha) {
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
	
	@Override
	public String toString() {
		return "Pixel-wise arc sine";
	}

}
