package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

import org.nfunk.jep.ParseException;

public final class ImgLibArcTangent2 <T extends RealType<T>> extends
		TwoOperandsPixelBasedAbstractFunction<T> {

	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise arc-tangent</h3> " +
		"This function computes the two operands arc-tangent of two ImgLib images, taking " +
		"each pixel of the two images as its two operands. Calculations are done using <i>Math.atan2</i>. " +
		"Values returned are in the range [-π, π]." ;

	public ImgLibArcTangent2() {
		numberOfParameters = 2;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException {
		return (float) Math.atan2(t1.getRealDouble(), t2.getRealDouble());
	}

	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	@Override
	public String getFunctionString() {
		return "atan2";
	}
	
	@Override
	public String toString() {
		return "Pixel-wise, two operands, arc tangent"; 
	}

}
