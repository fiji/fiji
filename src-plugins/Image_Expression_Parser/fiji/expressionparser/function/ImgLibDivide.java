package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibDivide <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = "<h3>Element-wise division</h3> " +
		"This function divides its two operands, element-wise. " +
		"See 'element-wise addition' for details.";
 
	
	public ImgLibDivide() {
		numberOfParameters = 2;
	}

	@Override
	public final <R extends RealType<R>> float evaluate(final R t1, final R t2) {
		return t1.getRealFloat() / t2.getRealFloat();
	}

	@Override
	public String toString() {
		return "Pixel-wise divide two operands";
	}

	@Override
	public String getFunctionString() {
		return "/";
	}
	
	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

}
