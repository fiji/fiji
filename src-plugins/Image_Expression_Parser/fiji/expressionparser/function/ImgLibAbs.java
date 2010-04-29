package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibAbs <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise absolute value</h3> " +
		"This function computes the absolute value of an ImgLib image, taking " +
		"each pixel as its operand. " +
		"Calculations are done using <i>Math.abs</i>.";

	public ImgLibAbs() {
		numberOfParameters = 1;
	}

	@Override
	public final <R extends RealType<R>> float evaluate(final R alpha) {
		return (float) Math.abs(alpha.getRealDouble());
	}

	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	public String getFunctionString() {
		return "abs";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise absolute value";
	}

}
