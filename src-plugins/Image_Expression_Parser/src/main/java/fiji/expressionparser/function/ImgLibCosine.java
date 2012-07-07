package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;


public final class ImgLibCosine <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise cosine</h3> " +
		"This function computes the cosine of an ImgLib image, taking " +
		"each pixel as its operand (must be in radians). " +
		"Calculations are done using <i>Math.cos</i>.";

	public ImgLibCosine() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R alpha) {
		return (float) Math.cos(alpha.getRealDouble());
	}

	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	@Override
	public String getFunctionString() {
		return "cos";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise cosine";
	}

}
