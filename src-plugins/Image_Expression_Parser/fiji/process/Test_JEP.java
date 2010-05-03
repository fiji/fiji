package fiji.process;

import fiji.expressionparser.ImgLibParser;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;

import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

public class Test_JEP {

	public static <T extends RealType<T>> void main(String[] args) {
		System.out.println("Testing JEP extension");
		
//		System.out.println("\nLoading image");
//		ImagePlus imp = ij.IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		
		System.out.println("\nCreating point image.");
		ImagePlus imp = ij.gui.NewImage.createFloatImage("Point", 128, 128, 32, ij.gui.NewImage.FILL_BLACK);
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
				"A * A",
				"2 * A",
				"A + A - 2*A",
				"(2*A-A) / A",
				"gauss(A,1.5)",
				"gauss(A,3) > A",
				"gauss(A,3) == A",
				"A == A"
				
		};

		ImgLibParser<T> parser = new ImgLibParser<T>();
		parser.addStandardFunctions();
		parser.addImgLibAlgorithms();

		for (String expression : expressions) {

			System.err.flush();
			System.out.println("\nTrying expression: "+expression);		
			parser.addVariable("A", img);
			
			try {
			
				Node root_node = parser.parse(expression);

				System.out.flush();
				Image<?> result = (Image<?>) parser.evaluate(root_node);
				System.out.println("Checking for errors: "+parser.getErrorInfo());		
				System.out.println("Resut is: "+result);		
				ImagePlus target_imp = ImageJFunctions.copyToImagePlus(result);
				target_imp.show();

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
				target_imp.setDisplayRange(min, max);
				target_imp.updateAndDraw();
				System.out.println(String.format("Min and max: %.2f - %.2f", min, max));
				
			} catch (ParseException e) {
				
				System.out.flush();
				System.err.println("Cound not evaluate expression: "+expression);
				System.err.println("Reason: "+e.getErrorInfo());

			}
			
			System.err.flush();
			System.out.flush();

		}
	}
	
}
