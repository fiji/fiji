package fiji.expressionparser.function;

import mpicbg.imglib.algorithm.math.NormalizeImageFloat;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.nfunk.jep.ParseException;

public final class ImgLibNormalize <T extends RealType<T>> extends SingleOperandAbstractFunction<T> {

	public ImgLibNormalize() {
		numberOfParameters = 1;
	}
	
	@Override
	public final <R extends RealType<R>> Image<FloatType> evaluate(final Image<R> img) throws ParseException {
		NormalizeImageFloat<R> normalizer = new NormalizeImageFloat<R>(img); 
		normalizer.process();
		return normalizer.getResult();
	}

	@Override
	public <R extends RealType<R>> Image<FloatType> evaluate(R alpha) throws ParseException {
		throw new ParseException("In function "+getFunctionString()
				+": Normalizing is not defined on scalars.");
	}

	@Override
	public String getDocumentationString() {
		return "<h3>Image normalization</h3> " +
		"This function normalizes its input, so that the sum of the output's pixel values " +
		"is equal to 1. " +
		"Syntax: " +
		"<br><code>" + getFunctionString() + "(A)</code><br> ";
	}

	@Override
	public String getFunctionString() {
		return "normalize";
	}

}
