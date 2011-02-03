package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public class ImgLibLog <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise natural logarithm</h3> " +
		"This function computes the natural logarithm (base <i>e</i>) " +
		"of an ImgLib image, taking " +
		"each pixel as its operand. " +
		"Calculations are done using <i>Math.log</i>.";

	public ImgLibLog() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(R alpha) {
		return (float) Math.log(alpha.getRealDouble());
	}

	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	public String getFunctionString() {
		return "log";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise logarithm";
	}

}
