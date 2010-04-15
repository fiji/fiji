package fiji.process;

import fiji.expressionparser.ImgLibParser;
import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;

public class Test_JEP {

	public static <T extends RealType<T>> void main(String[] args) {
		System.out.println("Testing JEP extension");
		
		System.out.println("\nLoading image");
		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		Image<T> img = ImagePlusAdapter.wrap(imp);
		imp.show();
		ImageProcessor ip = imp.getProcessor();
		float max = Float.NEGATIVE_INFINITY;
		float min = Float.POSITIVE_INFINITY;
		for (int index = 0; index < ip.getPixelCount(); index++) {
			if (ip.getf(index) > max) max = ip.getf(index); 
			if (ip.getf(index) < min) min = ip.getf(index); 
		}
		System.out.println(String.format("Min and max: %.2f - %.2f", min, max));
		
		System.out.println("\nImage converted to ImgLib: "+img);
		
		String[] expressions = {
				"A * A",
				"2 * A",
				"A + A - 2*A",
				"(2*A-A) / A",
		};
		
		for (String expression : expressions) {

			ImgLibParser<T> parser = new ImgLibParser<T>();				
			System.out.println("\nTrying expression: "+expression);		
			parser.addVariable("A", img);
			parser.parseExpression(expression);
			System.out.println("Checking for errors: "+parser.getErrorInfo());		
			Image<?> result = (Image<?>) parser.getValueAsObject();
			System.out.println("Resut is: "+result);		
			ImagePlus target_imp = ImageJFunctions.copyToImagePlus(result);
			target_imp.show();
			target_imp.resetDisplayRange();
			target_imp.updateAndDraw();
			FloatProcessor fp = (FloatProcessor) target_imp.getProcessor();
			float[] arr = (float[]) fp.getPixels();
			max = Float.NEGATIVE_INFINITY;
			min = Float.POSITIVE_INFINITY;
			for (int i = 0; i < arr.length; i++) {
				if (arr[i] > max) max = arr[i];
				if (arr[i] < min) min = arr[i];
			}
			System.out.println(String.format("Min and max: %.2f - %.2f", min, max));
			
		}
	}
	
}
