package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibArcTangent <T extends RealType<T>> extends
		SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise arc-tangent</h3> " +
		"This function computes the arc tangent of an ImgLib image, taking " +
		"each pixel as its operand. Calculations are done using <i>Math.atan</i>. " +
		"Values returned are in the range [-π/2, π/2].";
	
	public ImgLibArcTangent() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R t) {
		return (float) Math.atan(t.getRealDouble());
	}

	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	@Override
	public String getFunctionString() {
		return "atan";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise arc tangent";
	}

}
