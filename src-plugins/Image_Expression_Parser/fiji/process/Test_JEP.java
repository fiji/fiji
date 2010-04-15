package fiji.process;

import fiji.expressionparser.ImgLibParser;
import fiji.expressionparser.function.ImgLibGaussConv;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;

public class Test_JEP {

	public static <T extends RealType<T>> void main(String[] args) {
		System.out.println("Testing JEP extension");
		
//		System.out.println("\nLoading image");
//		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		
		System.out.println("\nCreating point image.");
		ImagePlus imp = NewImage.createFloatImage("Point", 128, 128, 32, NewImage.FILL_BLACK);
		float[] px = (float[]) imp.getStack().getPixels(16);
		px[128*128/2+64] = 1e3f;

		Image<T> img = ImagePlusAdapter.wrap(imp);
		imp.show();

		float max = Float.NEGATIVE_INFINITY;
		float min = Float.POSITIVE_INFINITY;
		for (int i = 0; i < imp.getStackSize(); i++) {
			ImageProcessor ip = imp.getStack().getProcessor(i+1);
			for (int index = 0; index < ip.getPixelCount(); index++) {
				if (ip.getf(index) > max) max = ip.getf(index); 
				if (ip.getf(index) < min) min = ip.getf(index); 
			}
		}
		System.out.println(String.format("Min and max: %.2f - %.2f", min, max));

		System.out.println("\nImage converted to ImgLib: "+img);

		String[] expressions = {
				"gauss(A,1)",
				"A * A",
				"2 * A",
				"A + A - 2*A",
				"(2*A-A) / A",
		};

		ImgLibParser<T> parser = new ImgLibParser<T>();
		parser.addFunction("gauss", new ImgLibGaussConv<T>());

		for (String expression : expressions) {

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
			max = Float.NEGATIVE_INFINITY;
			min = Float.POSITIVE_INFINITY;
			for (int i = 0; i < target_imp.getStackSize(); i++) {
				FloatProcessor fp = (FloatProcessor) target_imp.getStack().getProcessor(i+1);
				float[] arr = (float[]) fp.getPixels();
				for (int j = 0; j < arr.length; j++) {
					if (arr[j] > max) max = arr[j];
					if (arr[j] < min) min = arr[j];
				}
			}
			System.out.println(String.format("Min and max: %.2f - %.2f", min, max));
			
		}
	}
	
}
