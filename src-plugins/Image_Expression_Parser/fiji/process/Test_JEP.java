package fiji.process;

import fiji.parser.ImgLibParser;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.NumericType;

public class Test_JEP {

	
	
	public static <T extends NumericType<T>> void main(String[] args) {
		System.out.println("Testing JEP extension");
		
		System.out.println("\nLoading image");
		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		Image<T> img = ImagePlusAdapter.wrap(imp);
		imp.show();
		
		String expression = "A * A";
		System.out.println("\nTrying expression: "+expression);
		
		ImgLibParser<T> parser = new ImgLibParser<T>();
		parser.addVariable("A", img);
		parser.parseExpression(expression);
		Image<?> result = (Image<?>) parser.getValueAsObject();
		
		ImagePlus target_imp = ImageJFunctions.copyToImagePlus(result);
		target_imp.show();
		target_imp.resetDisplayRange();
		target_imp.updateAndDraw();

		expression = "2 * A";
		System.out.println("\nTrying expression: "+expression);

		parser.parseExpression(expression);
		result = (Image<?>) parser.getValueAsObject();
		
		target_imp = ImageJFunctions.copyToImagePlus(result);
		target_imp.show();
		target_imp.resetDisplayRange();
		target_imp.updateAndDraw();

	}
	
}
