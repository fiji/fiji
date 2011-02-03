package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibFloor <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise floor</h3> " +
		"This function computes the floor of an ImgLib image, by returning" +
		"the integer value that is less than or equal to each pixel value" +
		"of its operand. " +
		"Calculations are done using <i>Math.floor</i>.";

	public ImgLibFloor() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R alpha) {
		return (float) Math.floor(alpha.getRealDouble());
	}

	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	public String getFunctionString() {
		return "floor";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise floor";
	}

}
