package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibArcCosine <T extends RealType<T>> extends SingleOperandPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise arc-cosine</h3>" +
		"This function computes the arc cosine of an ImgLib image, taking" +
		"each pixel as its operand. Calculations are done using Math.acos";
	
	@Override
	public final float evaluate(T t) {
		return (float) Math.acos(t.getRealDouble());
	}

	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	public String getFunctionString() {
		return "acos";
	}

}
