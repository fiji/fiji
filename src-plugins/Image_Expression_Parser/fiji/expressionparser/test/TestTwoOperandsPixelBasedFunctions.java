/**
 * 
 */
package fiji.expressionparser.test;


import static org.junit.Assert.assertEquals;

import java.io.PrintStream;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

import fiji.expressionparser.ImgLibParser;

/**
 * @author Jean-Yves Tinevez
 *
 */
public class TestTwoOperandsPixelBasedFunctions <T extends RealType<T>> {

	private final static int WIDTH = 9; 
	private final static int HEIGHT = 9; 
	/** 16-bit image */
	public static Image<UnsignedShortType> image_A, image_B, image_C;
	public ImgLibParser<T> parser;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Create source images
		ArrayContainerFactory cfact = new ArrayContainerFactory();
		UnsignedShortType type = new UnsignedShortType();
		ImageFactory<UnsignedShortType> ifact = new ImageFactory<UnsignedShortType>(type, cfact);
		
		image_A = ifact.createImage(new int[] {WIDTH, HEIGHT}, "A");
		image_B = ifact.createImage(new int[] {WIDTH, HEIGHT}, "B");
		
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
	}


	@Before
	public void setUp() throws Exception {
		parser = new ImgLibParser<T>();
		parser.addStandardFunctions();
		parser.addImgLibAlgorithms();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void atan2() throws ParseException {
		
		// Two images - we also check for division by 0
		String expression = "atan2(A,A)";
		parser.addVariable("A", image_A);
		Node root_node = parser.parse(expression);
		Image<T> result = (Image<T>) parser.evaluate(root_node);
		LocalizableCursor<T> rc = result.createLocalizableCursor();
		LocalizableByDimCursor<UnsignedShortType> ca = image_A.createLocalizableByDimCursor(); 
		try {
			while (rc.hasNext()) {
				rc.fwd();
				ca.setPosition(rc);
				if ( ca.getType().getRealFloat() == 0.0f) {
					assertEquals(0.0f, rc.getType().getRealFloat(), Float.MIN_VALUE);
				} else {
					// We expect to get 45º
					assertEquals(45*Math.PI/180f,	rc.getType().getRealFloat(), Float.MIN_VALUE);
				}
			}
		} catch (AssertionError ae) {
			System.out.println("Assertion failed on "+expression+" with result:");
			echoImage(result, System.out);
			int[] position = rc.getPosition();
			System.out.println(String.format("on position: x=%d, y=%d.\n", position[0], position[1]));
			System.out.println(String.format("Error magnitude: %e.\n", Math.abs(45*Math.PI/180f-rc.getType().getRealFloat())));
			throw (ae);
		} finally {
			rc.close();
			ca.close();
		}

		// Right-singleton expansion
		expression = "atan2(A,10)";
		root_node = parser.parse(expression);
		result = (Image<T>) parser.evaluate(root_node);
		rc = result.createLocalizableCursor();
		ca = image_A.createLocalizableByDimCursor();
		try {
			while (rc.hasNext()) {
				rc.fwd();
				ca.setPosition(rc);
				assertEquals(Math.atan(ca.getType().getRealFloat()/10.0f), rc.getType().getRealFloat(), Float.MIN_VALUE);
			}
		} catch (AssertionError ae) {
			System.out.println("Assertion failed on "+expression+" with result:");
			echoImage(result, System.out);
			throw (ae);
		} finally {
			ca.close();
			rc.close();
		}
			
		// Left-singleton expansion
		expression = "atan2(10,A)";
		root_node = parser.parse(expression);
		result = (Image<T>) parser.evaluate(root_node);
		rc = result.createLocalizableCursor();
		ca = image_A.createLocalizableByDimCursor();
		try {
			while (rc.hasNext()) {
				rc.fwd();
				ca.setPosition(rc);
				assertEquals(Math.atan(10/ca.getType().getRealFloat()), rc.getType().getRealFloat(), Float.MIN_VALUE);
			}
		} catch (AssertionError ae) {
			System.out.println("Assertion failed on "+expression+" with result:");
			echoImage(result, System.out);
			throw (ae);
		} finally {
			ca.close();
			rc.close();
		}
		
		// Numbers 
		expression = "atan2(1,0)";
		root_node = parser.parse(expression);
		FloatType number_result = (FloatType) parser.evaluate(root_node);
		assertEquals(Math.PI/2f, number_result.getRealFloat(), Float.MIN_VALUE);
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
	
	
}
