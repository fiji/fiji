package fiji.expressionparser.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

import fiji.expressionparser.ImgLibParser;

public class TestUtilities <T extends RealType<T>> {
	
	private final static int WIDTH = 9; 
	private final static int HEIGHT = 9;
	private final static float ERROR_TOLERANCE = Float.MIN_VALUE;
	/** 16-bit image */
	public static Image<UnsignedShortType> image_A, image_B; 
	static {	
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
	
	
	public static interface ExpectedExpression {
		public <T extends RealType<T>> float getExpectedValue(final Map<String, LocalizableByDimCursor<T>> cursors);
	}
	
	
	public static final <T extends RealType<T>> void doTest(String expression, Map<String, Image<T>> source_map, ExpectedExpression ee) throws ParseException {
		// Get result
		Image<FloatType> result = getEvaluationResult(expression, source_map);
		// Prepare expected image		
		Image<FloatType> expected = buildExpectedImage(source_map, ee);		
		// Compare
		Image<FloatType> error = buildErrorImage(expected, result);
		boolean passed = checkErrorImage(error);
		try {
			assertTrue(passed);		
		} catch (AssertionError ae) {
			System.out.println("\n---");
			System.out.println("Assertion failed on "+expression+" with error image:");
			echoImage(error, System.out);
			throw (ae);
		}
	}
	
	public static final  void doTestNumbers(String expression, float expected) throws ParseException {
		ImgLibParser<FloatType> parser = new ImgLibParser<FloatType>();
		parser.addStandardFunctions();
		parser.addImgLibAlgorithms();
		Node root_node = parser.parse(expression);
		FloatType result = (FloatType) parser.evaluate(root_node);
		assertEquals(result.get(), expected, ERROR_TOLERANCE);
	}
		
	@SuppressWarnings("unchecked")
	public static final <T extends RealType<T>> Image<FloatType> getEvaluationResult(final String expression, final Map<String, Image<T>> source_map) throws ParseException {
		ImgLibParser<T>  parser = new ImgLibParser<T>();
		parser.addStandardFunctions();
		parser.addImgLibAlgorithms();
		for (String key : source_map.keySet()) {
			parser.addVariable(key, source_map.get(key));
		}
		Node root_node = parser.parse(expression);
		Image<FloatType> result = (Image<FloatType>) parser.evaluate(root_node);
		result.setName(expression);
		return result;
	}
	
	public static final <T extends RealType<T>> Image<FloatType> buildExpectedImage(final Map<String, Image<T>> source_map, final ExpectedExpression expression) {
		Image<T> source = source_map.get(source_map.keySet().toArray()[0]);
		Image<FloatType> expected = new ImageFactory<FloatType>(new FloatType(), source.getContainerFactory())
			.createImage(source.getDimensions(), "Expected image");
		// Prepare cursors
		LocalizableCursor<FloatType> ec = expected.createLocalizableCursor();
		Map<String, LocalizableByDimCursor<T>> cursor_map = new HashMap<String, LocalizableByDimCursor<T>>(source_map.size());
		for ( String key : source_map.keySet()) {
			cursor_map.put(key, source_map.get(key).createLocalizableByDimCursor());
		}
		// Set target value by looping over pixels
		while (ec.hasNext()) {
			ec.fwd();
			for (String key : cursor_map.keySet()) {
				cursor_map.get(key).setPosition(ec);
			}
			ec.getType().set(expression.getExpectedValue(cursor_map));
		}		
		// Close cursors
		ec.close();
		for (String key : cursor_map.keySet()) {
			cursor_map.get(key).close();
		}
		// Return
		return expected;		
	}
		
	public static final Image<FloatType> buildErrorImage(Image<FloatType> expected, Image<FloatType> actual) {
		Image<FloatType> result = expected.createNewImage();
		result.setName("Error on "+actual.getName());
		LocalizableCursor<FloatType> rc = result.createLocalizableCursor();
		LocalizableByDimCursor<FloatType> ec = expected.createLocalizableByDimCursor();
		LocalizableByDimCursor<FloatType> ac = actual.createLocalizableByDimCursor();
		while (rc.hasNext()) {
			rc.fwd();
			ec.setPosition(rc);
			ac.setPosition(rc);
			rc.getType().set(Math.abs(ec.getType().get()- ac.getType().get()));
		}
		rc.close();
		ac.close();
		ec.close();		
		return result;
	}
	
	public static final boolean checkErrorImage(Image<FloatType> error) {
		boolean ok = true;
		Cursor<FloatType> c = error.createCursor();
		while (c.hasNext()) {
			c.fwd();
			if (c.getType().get() > ERROR_TOLERANCE) {
				ok = false;
				break;
			}
		}
		c.close();		
		return ok;
	}
	
	
	public static final <T extends RealType<T>> void echoImage(Image<T> img, PrintStream logger) {
		LocalizableByDimCursor<T> lc = img.createLocalizableByDimCursor();
		int[] dims = lc.getDimensions();

		logger.append(img.toString() + "\n");
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
