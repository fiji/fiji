package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;


public final class ImgLibSquareRoot <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise square root</h3>" +
		"This function computes the square root of an ImgLib image, taking " +
		"each pixel as its operand. " +
		"Calculations are done using <i>Math.sqrt</i>.";

	public ImgLibSquareRoot() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R alpha) {
		return (float) Math.sqrt(alpha.getRealDouble());
	}

	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	@Override
	public String getFunctionString() {
		return "sqrt";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise square root";
	}

}
