package fiji.expressionparser.function;

import java.util.Stack;

import mpicbg.imglib.algorithm.fft.Bandpass;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

public class ImgLibBandPassFilter <T extends RealType<T>> extends PostfixMathCommand implements ImgLibFunction<T>{

	public ImgLibBandPassFilter() {
		numberOfParameters = 3;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		Object param3 = stack.pop();
		Object param2 = stack.pop();
		Object param1 = stack.pop();
		Image<T> img;
		int begin_radius, end_radius;
		
		// Check classes
		if (param1 instanceof Image<?>) {
			img = (Image) param1;
		} else {
			throw new ParseException("In function '" + getFunctionString()
						+"': First operand must be an image.");
		}
		if (param2 instanceof FloatType) {
			begin_radius = (int) ( (FloatType) param2).get();
		} else {
			throw new ParseException("In function '" + getFunctionString()
					+"': Second and third operands must be numbers.");
		}
		if (param3 instanceof FloatType) {
			end_radius = (int) ( (FloatType) param3).get();
		} else {
			throw new ParseException("In function '" + getFunctionString()
					+"': Second and third operands must be numbers.");
		}
		
		// Do filter 
		Bandpass<T> filter = new Bandpass<T>(img, begin_radius, end_radius);
		filter.process();
		stack.push(filter.getResult());
	}
	
	@Override
	public String getDocumentationString() {
		return "<h3>FFT bandpass filter</h3> " +
		"This function filters its input in fourier space. " +
		"Syntax: " +
		"<br><code>" + getFunctionString() + "(A, begin_radius, end_radius)</code><br> " +
		"where A is an image, and begin_radius and  end_radius are integers. " +
		"Thw two radiuses are given in pixel units in the fourier space.";	}

	@Override
	public String getFunctionString() {
		return "bandpass";
	}

}
