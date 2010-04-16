package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibAdd <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

	public ImgLibAdd() {
		numberOfParameters = 2;
	}

	@Override
	public final float evaluate(final T t1, final T t2) {
		return t1.getRealFloat() + t2.getRealFloat();
	}

	@Override
	public String toString() {
		return "Pixel-wise add two operands";
}

	@Override
	public String getFunctionString() {
		return "+";
	}

}
