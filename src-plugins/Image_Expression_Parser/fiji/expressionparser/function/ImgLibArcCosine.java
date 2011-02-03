package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibArcCosine <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise arc-cosine</h3> " +
		"This function computes the arc cosine of an ImgLib image, taking " +
		"each pixel as its operand. Calculations are done using <i>Math.acos</i>. " +
		"Values returned are in the range [0, π].";
	
	public ImgLibArcCosine() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R t) {
		return (float) Math.acos(t.getRealDouble());
	}

	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	@Override
	public String getFunctionString() {
		return "acos";
	}

	@Override
	public String toString() {
		return "Pixel-wise arc cosine";
	}
}
