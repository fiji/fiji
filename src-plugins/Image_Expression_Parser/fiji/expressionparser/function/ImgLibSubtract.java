package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibSubtract <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

	public ImgLibSubtract() {
		numberOfParameters = 2;
	}

	@Override
	public final float evaluate(final T t1, final T t2) {
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
		return "<h3>Element-wise subtraction</h3>" +
		"This function subtracts its two operands, element-wise. " +
		"See 'element-wise addition' for details.";
	}

}
