package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibCeil <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise ceiling</h3> " +
		"This function computes the ceiling of an ImgLib image, by returning" +
		"the integer value that is greater than or equal to each pixel value" +
		"of its operand. " +
		"Calculations are done using <i>Math.ceil</i>.";

	public ImgLibCeil() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R alpha) {
		return (float) Math.ceil(alpha.getRealDouble());
	}

	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	public String getFunctionString() {
		return "ceil";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise ceil";
	}

}
