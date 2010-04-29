package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibSubtract <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

	public final static String DOCUMENTATION_STING = "<h3>Element-wise subtraction</h3> " +
		"This function subtracts its two operands, element-wise. " +
		"See 'element-wise addition' for details.";
	
	public ImgLibSubtract() {
		numberOfParameters = 2;
	}

	@Override
	public final <R extends RealType<R>> float evaluate(final R t1, final R t2) {
		return t1.getRealFloat() - t2.getRealFloat();
	}

	@Override
	public String getFunctionString() {
		return "-";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise subtract two operands";
	}

	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STING;
	}

}
