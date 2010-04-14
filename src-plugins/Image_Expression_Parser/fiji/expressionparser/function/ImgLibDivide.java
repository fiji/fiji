package fiji.expressionparser.function;

import mpicbg.imglib.type.NumericType;

public final class ImgLibDivide <T extends NumericType<T>> extends TwoOperandsPixelBasedFunction<T> {

	public ImgLibDivide() {
		numberOfParameters = 2;
	}

	@Override
	public final float evaluate(final T t1, final T t2) {
		return t1.getReal() / t2.getReal();
	}

	@Override
	public String toString() {
		return "/";
	}

}
