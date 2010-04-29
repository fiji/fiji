package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;


public final class ImgLibTangent <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise tangent</h3> " +
		"This function computes the tangent of an ImgLib image, taking " +
		"each pixel as its operand (must be in radians). " +
		"Calculations are done using Math.tan";

	public ImgLibTangent() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R alpha) {
		return (float) Math.tan(alpha.getRealDouble());
	}

	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	@Override
	public String getFunctionString() {
		return "tan";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise tangent";
	}

}
