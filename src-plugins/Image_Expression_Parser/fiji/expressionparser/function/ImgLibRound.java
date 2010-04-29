package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibRound <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise round</h3> " +
		"This function returns a rounded ImgLib image, by " +
		"rounding each pixel of its operand to the closest integer. " +
		"Calculations are done using <i>Math.round</i>.";

	public ImgLibRound() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R alpha) {
		return (float) Math.round(alpha.getRealDouble());
	}

	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	public String getFunctionString() {
		return "round";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise round";
	}

}
