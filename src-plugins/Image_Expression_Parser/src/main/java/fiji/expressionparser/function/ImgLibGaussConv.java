package fiji.expressionparser.function;

import mpicbg.imglib.algorithm.gauss.GaussianConvolution;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.nfunk.jep.ParseException;

import fiji.expressionparser.ImgLibUtils;

public final class ImgLibGaussConv <T extends RealType<T>> extends TwoOperandsAbstractFunction<T> {

	public ImgLibGaussConv() {
		numberOfParameters = 2;
	}

	public String toString() {
		return "Gaussian convolution";
	}

	@Override
	public String getDocumentationString() {
		return "<h3>Gaussian convolution</h3> " +
				"This function implements the isotropic gaussian convolution, as coded " +
				"in ImgLib, effectively implementing a gaussian filter. " +
				"Syntax: " +
				"<br><code>" + getFunctionString() + "(A, sigma)</code><br> " +
				"with A an image and sigma a number. Sigma is the standard deviation " +
				"of the gaussian kernel applied to image A.<br> " +
				"Input image is converted to <i>FloatType</i> then convolved. " +
				"If the source image is a 3D image, the convolution will be made in 3D as well. ";				
	}
	
	@Override
	public String getFunctionString() {
		return "gauss";
	}

	@Override
	public final <R extends RealType<R>> Image<FloatType> evaluate(final Image<R> img, final R alpha) throws ParseException {
		Image<FloatType> fimg = ImgLibUtils.copyToFloatTypeImage(img);
		OutOfBoundsStrategyMirrorFactory<FloatType> strategy = new OutOfBoundsStrategyMirrorFactory<FloatType>();
		GaussianConvolution<FloatType> gaussian_fiter = new GaussianConvolution<FloatType>(fimg, strategy, alpha.getRealDouble());
		gaussian_fiter.process();
		Image<FloatType> result = gaussian_fiter.getResult();
		result.setName("gauss("+img.getName()+", "+String.format("%.1f", alpha.getRealDouble())+")");
		return result;
	}

	@Override
	public final <R extends RealType<R>> float evaluate(final R t1, final R t2) throws ParseException{
			throw new ParseException("In function "+getFunctionString()
					+": Arguments must be one image and one number, got 2 numbers.");
	}

	@Override
	public final <R extends RealType<R>> Image<FloatType> evaluate(final Image<R> img1, final Image<R> img2) throws ParseException {
		throw new ParseException("In function "+getFunctionString()
				+": Arguments must be one image and one number, got 2 images.");
	}

	@Override
	public final <R extends RealType<R>> Image<FloatType> evaluate(final R alpha, Image<R> img) throws ParseException {
		throw new ParseException("In function "+getFunctionString()
			+": First argument must be one image and second one a number, in this order.");
	}

	
}
