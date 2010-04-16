package fiji.expressionparser.function;

import mpicbg.imglib.algorithm.gauss.GaussianConvolution;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.nfunk.jep.ParseException;

import fiji.expressionparser.ImgLibUtils;

public class ImgLibGaussConv <T extends RealType<T>> extends TwoOperandsAbstractFunction<T> {

	public ImgLibGaussConv() {
		numberOfParameters = 2;
	}

	public String toString() {
		return "Gaussian convolution";
	}

	@Override
	public String getFunctionString() {
		return "gauss";
	}

	@Override
	public Image<FloatType> evaluate(Image<T> img, T alpha) throws ParseException {
		Image<FloatType> fimg = ImgLibUtils.copyToFloatTypeImage(img);
		OutOfBoundsStrategyMirrorFactory<FloatType> strategy = new OutOfBoundsStrategyMirrorFactory<FloatType>();
		GaussianConvolution<FloatType> gaussian_fiter = new GaussianConvolution<FloatType>(fimg, strategy, alpha.getRealDouble());
		gaussian_fiter.process();
		Image<FloatType> result = gaussian_fiter.getResult();
		result.setName("gauss("+img.getName()+", "+String.format("%.1f", alpha.getRealDouble())+")");
		return result;
	}

	@Override
	public float evaluate(T t1, T t2) throws ParseException{
			throw new ParseException("In function "+getFunctionString()
					+": Arguments must be one image and one number, got 2 numbers.");
	}

	@Override
	public Image<FloatType> evaluate(Image<T> img1, Image<T> img2) throws ParseException {
		throw new ParseException("In function "+getFunctionString()
				+": Arguments must be one image and one number, got 2 images.");
	}

}
