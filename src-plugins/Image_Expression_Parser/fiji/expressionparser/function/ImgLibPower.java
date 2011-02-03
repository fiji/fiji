package fiji.expressionparser.function;

import mpicbg.imglib.type.numeric.RealType;

import org.nfunk.jep.ParseException;

public final class ImgLibPower <T extends RealType<T>> extends
		TwoOperandsPixelBasedAbstractFunction<T> {
	
	public static final String DOCUMENTATION_STRING = 
		"<h3>Element-wise power</h3> " +
		"This function computes the value of the first argument raised to the power of the second " +
		"argument, pixel by pixel, with singleton exapnsion. " +
		"Calculations are done using <i>Math.pow</i>." ;


	public ImgLibPower() {
		numberOfParameters = 2;
	}
	
	@Override
	public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException {
		return (float) Math.pow(t1.getRealDouble(), t2.getRealDouble());
	}

	@Override
	public String getDocumentationString() {
		return DOCUMENTATION_STRING;
	}

	@Override
	public String getFunctionString() {
		return "pow";
	}
	
	@Override
	public String toString() {
		return "Value of first argument to the power of the second argument";
	}

}
