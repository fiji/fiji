package fiji.expressionparser.test;

import static org.junit.Assert.assertEquals;

import java.io.PrintStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

import fiji.expressionparser.ImgLibParser;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;

public class TestImgLibAlgorithms <T extends RealType<T>> {

	
	private final static int WIDTH = 9; 
	private final static int HEIGHT = 9; 
	private final static int DEPTH = 9;
	/** 16-bit image */
	public static Image<UnsignedShortType> image_A, image_B, image_C;
	public ImgLibParser<T> parser;
	
	private final static int PULSE_VALUE = 1;
	private final static float SIGMA = 0.84089642f;
	private static final double[] CONVOLVED = new double[] {
			0.0,			0.0,			0.0,			0.0,			0.0,			0.0,			0.0,			0.0,			0.0,  
			0.0,			0.00000067, 	0.00002292, 	0.00019117, 	0.00038771, 	0.00019117, 	0.00002292, 	0.00000067,			0.0,
			0.0,			0.00002292, 	0.00078633, 	0.00655965, 	0.01330373, 	0.00655965, 	0.00078633, 	0.00002292,			0.0,
			0.0,			0.00019117, 	0.00655965, 	0.05472157, 	0.11098164, 	0.05472157, 	0.00655965, 	0.00019117,			0.0,
			0.0,			0.00038771, 	0.01330373, 	0.11098164, 	0.22508352, 	0.11098164, 	0.01330373, 	0.00038771,			0.0,
			0.00,			0.00019117, 	0.00655965, 	0.05472157, 	0.11098164, 	0.05472157, 	0.00655965, 	0.00019117,			0.0,
			0.0,			0.00002292, 	0.00078633, 	0.00655965, 	0.01330373, 	0.00655965, 	0.00078633, 	0.00002292,			0.0,
			0.0,			0.00000067, 	0.00002292, 	0.00019117, 	0.00038771, 	0.00019117, 	0.00002292, 	0.00000067,			0.0,
			0.0,			0.0,			0.0,			0.0,			0.0,			0.0,			0.0,			0.0,										0.0 
	};
	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Create source images
		ArrayContainerFactory cfact = new ArrayContainerFactory();
		UnsignedShortType type = new UnsignedShortType();
		ImageFactory<UnsignedShortType> ifact = new ImageFactory<UnsignedShortType>(type, cfact);
		
		image_A = ifact.createImage(new int[] {WIDTH, HEIGHT}, "A");
		image_B = ifact.createImage(new int[] {WIDTH, HEIGHT}, "B");
		image_C = ifact.createImage(new int[] {(int) Math.sqrt(CONVOLVED.length), (int) Math.sqrt(CONVOLVED.length)}, "C"); // Spike 3D image
		
		LocalizableCursor<UnsignedShortType> ca = image_A.createLocalizableCursor();
		LocalizableByDimCursor<UnsignedShortType> cb = image_B.createLocalizableByDimCursor();

		int[] pos = ca.createPositionArray();
		while (ca.hasNext()) {
			ca.fwd();
			ca.getPosition(pos);
			cb.setPosition(ca);
			ca.getType().set( pos[0] * (WIDTH-1-pos[0]) * pos[1] * (HEIGHT-1-pos[1]) );
			cb.getType().set( 256 - (pos[0] * (WIDTH-1-pos[0]) * pos[1] * (HEIGHT-1-pos[1]) ) );
		}
		ca.close();
		cb.close();
		
		LocalizableByDimCursor<UnsignedShortType> cc = image_C.createLocalizableByDimCursor();
		cc.setPosition(new int[] { (int) Math.sqrt(CONVOLVED.length)/2, (int) Math.sqrt(CONVOLVED.length)/2});
		cc.getType().set(PULSE_VALUE);
		cc.close();
		
	}


	@Before
	public void setUp() throws Exception {
		parser = new ImgLibParser<T>();
		parser.addStandardFunctions();
		parser.addImgLibAlgorithms();
	}
	
	
	@SuppressWarnings("unchecked")
	@Test(expected=ParseException.class)
	public void gaussianConvolutionTwoImages() throws ParseException {		
		// Two images -> Should generate an exception
		String expression = "gauss(C,A)";
		parser.addVariable("A", image_A);
		parser.addVariable("C", image_C);
		Node node = parser.parse(expression);
		Image<T> result = (Image<T>) parser.evaluate(node); // Never reach this code
		System.out.println("Assertion failed on "+expression+" with result:");
		echoImage(result, System.out);
	}

	@SuppressWarnings("unchecked")
	@Test(expected=ParseException.class)
	public void gaussianConvolutionTwoNumbers() throws ParseException {		
		// Two images -> Should generate an exception
		String expression = "gauss(1.0,5)";
		parser.addVariable("A", image_A);
		parser.addVariable("C", image_C);
		Node node = parser.parse(expression);
		Image<T> result = (Image<T>) parser.evaluate(node); // Never reach this code
		System.out.println("Assertion failed on "+expression+" with result:");
		echoImage(result, System.out);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void gaussianConvolution() throws ParseException {		
		// Two images -> Should generate an exception
		String expression = "gauss(C,"+SIGMA+")";
		parser.addVariable("C", image_C);
		Node node = parser.parse(expression);
		Image<T> result = (Image<T>) parser.evaluate(node);
		LocalizableCursor<T> rc = result.createLocalizableCursor();
		int[] position = rc.createPositionArray();
		float tv;
		try {
			int index = 0;
			while (rc.hasNext()) {
				rc.fwd();
				rc.getPosition(position);
				tv = (float) CONVOLVED[index];
				assertEquals( tv, rc.getType().getRealFloat(), tv/1e3);
			}
		} catch (AssertionError ae) {
			System.out.println("Assertion failed on "+expression+" with result:");
			echoImage(result, System.out);
			System.out.println(String.format("on position: x=%d, y=%d.\n", position[0], position[1]));
			throw (ae);
		} finally {
			rc.close();
		}
	}

	/*
	 * UTILS
	 */
	
	
	public static final <T extends RealType<T>> void echoImage(Image<T> img, PrintStream logger) {
		LocalizableByDimCursor<T> lc = img.createLocalizableByDimCursor();
		int[] dims = lc.getDimensions();

		logger.append(img.toString() + "\n");
		logger.append("      ");

		logger.append('\n');

		logger.append("        ");
		for (int i =0; i<dims[0]; i++) {
			logger.append(String.format("%9d.", i) );				
		}
		logger.append('\n');

		for (int j = 0; j<dims[1]; j++) {

			lc.setPosition(j, 1);
			logger.append(String.format("%2d.  -  ", j) );				
			for (int i =0; i<dims[0]; i++) {
				lc.setPosition(i, 0);
				logger.append(String.format("%10.1e", lc.getType().getRealFloat()));				
			}
			logger.append('\n');
		}

		lc.close();

	}
	
	public static final float theroeticalGaussianConv(final int[] position) {
		final int x = position[0];
		final int y = position[1];
		final int z = position[2];
		if ( Math.abs(x-WIDTH/2) > 3*SIGMA || x == WIDTH || y == 0 || y == HEIGHT ) {
			return 0.0f;
		}
		final double zval = Math.exp( -(z-DEPTH/2) * (z-DEPTH/2) / 2*SIGMA*SIGMA);
		final double yval = Math.exp( -(y-HEIGHT/2) * (y-HEIGHT/2) / 2*SIGMA*SIGMA);
		final double xval = Math.exp( -(x-WIDTH/2) * (x-WIDTH/2) / 2*SIGMA*SIGMA);
		return (float) (PULSE_VALUE * xval * yval * zval / Math.pow(Math.sqrt(2*Math.PI)*SIGMA, 3));	
	}
	
}
