package fiji.process;

import org.nfunk.jep.JEP;
import org.nfunk.jep.Operator;
import org.nfunk.jep.OperatorSet;

import fiji.parser.ImgLibOperatorSet;
import fiji.parser.ImgLibParser;
import fiji.parser.function.Multiply;

import ij.IJ;
import ij.ImagePlus;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.NumericType;
import mpicbg.imglib.type.numeric.FloatType;

public class Test_JEP {

	
	
	public static <T extends NumericType<T>> void main(String[] args) {
		System.out.println("Testing JEP extension");
		
		System.out.println("\nLoading image");
		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		Image<T> img = ImagePlusAdapter.wrap(imp);
		imp.show();
		
		String expression = "A * A";
		System.out.println("\nTrying expression: "+expression);
		
		ImgLibParser parser = new ImgLibParser();
		parser.addVariable("A", img);
		parser.parseExpression(expression);
		Image<FloatType> result = (Image<FloatType>) parser.getValueAsObject();
		
		ImagePlus target_imp = ImageJFunctions.copyToImagePlus(result);
		target_imp.show();
		target_imp.resetDisplayRange();
		target_imp.updateAndDraw();

		expression = "2 * A";
		System.out.println("\nTrying expression: "+expression);

		parser.parseExpression(expression);
		result = (Image<FloatType>) parser.getValueAsObject();
		
		target_imp = ImageJFunctions.copyToImagePlus(result);
		target_imp.show();
		target_imp.resetDisplayRange();
		target_imp.updateAndDraw();

	}
	
}
