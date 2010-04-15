package fiji.expressionparser.function;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import mpicbg.imglib.algorithm.gauss.GaussianConvolution;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;

public class ImgLibGaussConv <T extends RealType<T>> extends PostfixMathCommand {

	public ImgLibGaussConv() {
		numberOfParameters = 2;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		
		checkStack(stack);
		Object param2 = stack.pop();
		Object param1 = stack.pop();
		
		if ( !(param1 instanceof Image<?>) ) {
			throw new ParseException("In function "+this.getClass().getSimpleName()
					+": First operand must be and imglib image, got a "+param1.getClass().getSimpleName() );
		}
		if ( !(param2 instanceof Number) ) {
			throw new ParseException("In function "+this.getClass().getSimpleName()
					+": Second operand must be a scalar, got a "+param1.getClass().getSimpleName() );
		}
		Image<T> img = (Image<T>) param1;
		Number sigma = (Number) param2;
		OutOfBoundsStrategyMirrorFactory<T> strategy = new OutOfBoundsStrategyMirrorFactory<T>();
		GaussianConvolution<T> gaussian_fiter = new GaussianConvolution<T>(img, strategy, sigma.doubleValue());
		gaussian_fiter.process();
		Image<T> result = gaussian_fiter.getResult();
		result.setName("gauss("+img.getName()+", "+String.format("%.1f", sigma.doubleValue())+")");
		stack.push(result);
		return;
	}
	
	public String toString() {
		return "GaussConv";
	}
}
