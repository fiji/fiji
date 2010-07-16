package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

import org.nfunk.jep.ParseException;

public final class ImgLibModulus <T extends RealType<T>> extends
		TwoOperandsPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise modulus</h3> " +
		"This function computes the modulus of two ImgLib images, with the Java meaning " +
		"of it, taking each pixel of the two images as its two operands. ";

	public ImgLibModulus() {
		numberOfParameters = 2;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException {
		return t1.getRealFloat() % t2.getRealFloat();
	}

	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	@Override
	public String getFunctionString() {
		return "%";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise, two operands, arc tangent"; 
	}

}
