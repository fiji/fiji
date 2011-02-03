package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibUMinus  <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise minus</h3> " +
		"This function computes the negative of an ImgLib image, by returning" +
		"the opposite value of each pixel of its operand.";

	public ImgLibUMinus() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R alpha) {
		return -alpha.getRealFloat();
	}

	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	public String getFunctionString() {
		return "-";
	}
	
	@Override
	public String toString() {
		return "Unitary minus";
	}

}
