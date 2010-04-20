package fiji.expressionparser.test;

import static org.junit.Assert.assertEquals;

import java.io.PrintStream;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

import fiji.expressionparser.ImgLibParser;


public class TestFunctions <T extends RealType<T>>{
	
	
	private final static int WIDTH = 9; 
	private final static int HEIGHT = 9; 
	/** 16-bit image */
	public static Image<UnsignedShortType> image_A, image_B;
	public ImgLibParser<T> parser;
	
	
	@BeforeClass
	public static void setup() {
		
		
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
//		echoImage(image_A, System.out);
//		echoImage(image_B, System.out);		
	}
	
	@Before
	public void setupParser() {
		parser = new ImgLibParser<T>();
	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void add() throws ParseException {
		
		// Addition of two images
		String expression = "A+B";
		parser.addVariable("A", image_A);
		parser.addVariable("B", image_B);
		Node root_node = parser.parse(expression);
		Image<T> result = (Image<T>) parser.evaluate(root_node);
		Cursor<T> rc = result.createCursor();
		try {
			while (rc.hasNext()) {
				rc.fwd();
				assertEquals(256.0f, rc.getType().getRealFloat(), Float.MIN_VALUE);
			}
		} catch (AssertionError ae) {
			System.out.println("Assertion failed on "+expression+" with result:");
			echoImage(result, System.out);
			throw (ae);
		} finally {
			rc.close();
		}

		// Singleton expansion
		expression = "A+256";
		root_node = parser.parse(expression);
		result = (Image<T>) parser.evaluate(root_node);
		LocalizableByDimCursor<T> rclbd = result.createLocalizableByDimCursor();
		LocalizableCursor<UnsignedShortType> ca = image_A.createLocalizableCursor();
		try {
			while (ca.hasNext()) {
				ca.fwd();
				rclbd.setPosition(ca);
				assertEquals(256.0f+ca.getType().getRealFloat(), rclbd.getType().getRealFloat(), Float.MIN_VALUE);
			}
		} catch (AssertionError ae) {
			System.out.println("Assertion failed on "+expression+" with result:");
			echoImage(result, System.out);
			throw (ae);
		} finally {
			ca.close();
			rclbd.close();
		}
		
		// Numbers addition
		expression = "256+256";
		root_node = parser.parse(expression);
		Number number_result = (Number) parser.evaluate(root_node);
		assertEquals(512.0f, number_result.floatValue(), Float.MIN_VALUE);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void subtract() throws ParseException {
		
		// Two images
		String expression = "A-A";
		parser.addVariable("A", image_A);
		Node root_node = parser.parse(expression);
		Image<T> result = (Image<T>) parser.evaluate(root_node);
		Cursor<T> rc = result.createCursor();
		try {
			while (rc.hasNext()) {
				rc.fwd();
				assertEquals(0.0f, rc.getType().getRealFloat(), Float.MIN_VALUE);
			}
		} catch (AssertionError ae) {
			System.out.println("Assertion failed on "+expression+" with result:");
			echoImage(result, System.out);
			throw (ae);
		} finally {
			rc.close();
		}

		// Right-singleton expansion
		expression = "A-256";
		root_node = parser.parse(expression);
		result = (Image<T>) parser.evaluate(root_node);
		LocalizableByDimCursor<T> rclbd = result.createLocalizableByDimCursor();
		LocalizableCursor<UnsignedShortType> ca = image_A.createLocalizableCursor();
		try {
			while (ca.hasNext()) {
				ca.fwd();
				rclbd.setPosition(ca);
				assertEquals(ca.getType().getRealFloat()-256.0f, rclbd.getType().getRealFloat(), Float.MIN_VALUE);
			}
		} catch (AssertionError ae) {
			System.out.println("Assertion failed on "+expression+" with result:");
			echoImage(result, System.out);
			throw (ae);
		} finally {
			ca.close();
			rclbd.close();
		}
		
		// Left-singleton expansion
		expression = "256-A";
		root_node = parser.parse(expression);
		result = (Image<T>) parser.evaluate(root_node);
		rclbd = result.createLocalizableByDimCursor();
		ca = image_A.createLocalizableCursor();
		try {
			while (ca.hasNext()) {
				ca.fwd();
				rclbd.setPosition(ca);
				assertEquals(256.0f-ca.getType().getRealFloat(), rclbd.getType().getRealFloat(), Float.MIN_VALUE);
			}
		} catch (AssertionError ae) {
			System.out.println("Assertion failed on "+expression+" with result:");
			echoImage(result, System.out);
			throw (ae);
		} finally {
			ca.close();
			rclbd.close();
		}
		
		// Numbers 
		expression = "256-128";
		root_node = parser.parse(expression);
		Number number_result = (Number) parser.evaluate(root_node);
		assertEquals(128.0f, number_result.floatValue(), Float.MIN_VALUE);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void multiply() throws ParseException {
		
		// Two images
		String expression = "A*B";
		parser.addVariable("A", image_A);
		parser.addVariable("B", image_B);
		Node root_node = parser.parse(expression);
		Image<T> result = (Image<T>) parser.evaluate(root_node);
		LocalizableCursor<T> rc = result.createLocalizableCursor();
		LocalizableByDimCursor<UnsignedShortType> ca = image_A.createLocalizableByDimCursor(); 
		LocalizableByDimCursor<UnsignedShortType> cb = image_B.createLocalizableByDimCursor(); 
		try {
			while (rc.hasNext()) {
				rc.fwd();
				ca.setPosition(rc);
				cb.setPosition(rc);
				assertEquals(ca.getType().getRealFloat() * cb.getType().getRealFloat(), 
						rc.getType().getRealFloat(), Float.MIN_VALUE);
			}
		} catch (AssertionError ae) {
			System.out.println("Assertion failed on "+expression+" with result:");
			echoImage(result, System.out);
			throw (ae);
		} finally {
			rc.close();
			ca.close();
			cb.close();
		}

		// Right-singleton expansion
		expression = "A*10";
		root_node = parser.parse(expression);
		result = (Image<T>) parser.evaluate(root_node);
		rc = result.createLocalizableCursor();
		ca = image_A.createLocalizableByDimCursor();
		try {
			while (rc.hasNext()) {
				rc.fwd();
				ca.setPosition(rc);
				assertEquals(ca.getType().getRealFloat()*10.0f, rc.getType().getRealFloat(), Float.MIN_VALUE);
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
		expression = "10*A";
		root_node = parser.parse(expression);
		result = (Image<T>) parser.evaluate(root_node);
		rc = result.createLocalizableCursor();
		ca = image_A.createLocalizableByDimCursor();
		try {
			while (rc.hasNext()) {
				rc.fwd();
				ca.setPosition(rc);
				assertEquals(ca.getType().getRealFloat()*10.0f, rc.getType().getRealFloat(), Float.MIN_VALUE);
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
		expression = "256*10";
		root_node = parser.parse(expression);
		Number number_result = (Number) parser.evaluate(root_node);
		assertEquals(2560.0f, number_result.floatValue(), Float.MIN_VALUE);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void divide() throws ParseException {
		
		// Two images - we also check for division by 0
		String expression = "A/A";
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
					assertEquals(Float.NaN, rc.getType().getRealFloat(), Float.MIN_VALUE);
				} else {
					assertEquals(1.0f,	rc.getType().getRealFloat(), Float.MIN_VALUE);
				}
			}
		} catch (AssertionError ae) {
			System.out.println("Assertion failed on "+expression+" with result:");
			echoImage(result, System.out);
			throw (ae);
		} finally {
			rc.close();
			ca.close();
		}

		// Right-singleton expansion
		expression = "A/10";
		root_node = parser.parse(expression);
		result = (Image<T>) parser.evaluate(root_node);
		rc = result.createLocalizableCursor();
		ca = image_A.createLocalizableByDimCursor();
		try {
			while (rc.hasNext()) {
				rc.fwd();
				ca.setPosition(rc);
				assertEquals(ca.getType().getRealFloat()/10.0f, rc.getType().getRealFloat(), Float.MIN_VALUE);
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
		expression = "10/A";
		root_node = parser.parse(expression);
		result = (Image<T>) parser.evaluate(root_node);
		rc = result.createLocalizableCursor();
		ca = image_A.createLocalizableByDimCursor();
		try {
			while (rc.hasNext()) {
				rc.fwd();
				ca.setPosition(rc);
				assertEquals(10/ca.getType().getRealFloat(), rc.getType().getRealFloat(), Float.MIN_VALUE);
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
		expression = "256/10";
		root_node = parser.parse(expression);
		Number number_result = (Number) parser.evaluate(root_node);
		assertEquals(25.6f, number_result.floatValue(), Float.MIN_VALUE);
	}
	
		
	/*
	 * UTILS
	 */
	
	public static final <T extends RealType<T>> void echoImage(Image<T> img, PrintStream logger) {
		LocalizableByDimCursor<T> lc = img.createLocalizableByDimCursor();
		int[] dims = lc.getDimensions();
		
		logger.append(img.toString() + "\n");
		logger.append("        ");
		for (int i =0; i<dims[0]; i++) {
			logger.append(String.format("%6d.", i) );				
		}
		logger.append('\n');
		logger.append('\n');
		
		for (int j = 0; j<dims[1]; j++) {
			
			lc.setPosition(j, 1);
			logger.append(String.format("%2d.  -  ", j) );				
			for (int i =0; i<dims[0]; i++) {
				lc.setPosition(i, 0);
				logger.append(String.format("%7.1f", lc.getType().getRealFloat()));				
			}
			logger.append('\n');
		}
		lc.close();
		
	}

}
