package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

public final class ImgLibAdd <T extends RealType<T>> extends TwoOperandsPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = "<h3>Element-wise addition</h3>" +
		"This function adds its two operands, element-wise. " +
		"The mode depends on the type of the arguments. " +
		"If A and B are images and alpha and beta are numbers, then: " +
		"<ul>" +
		"	<li><code>A+B</code> will return an image in which each pixel is the sum of the corresponding " +
		"pixels in A and B. This operation is defined only if A and B have the same number of pixels. " +
		"	<li><code>A+alpha</code>will do singleton expansion and return a new image in which each pixel " +
		"is the sum of the corresponding pixel value in A plus alpha. " +
		"	<li><code>alpha+beta</code>simply sums the two numbers. " +
		"</ul>";

	public ImgLibAdd() {
		numberOfParameters = 2;
	}

	@Override
	public final <R extends RealType<R>> float evaluate(final R t1, final R t2) {
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

	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

}
