package fiji.expressionparser.test;

import static fiji.expressionparser.test.TestUtilities.*;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.junit.Test;
import org.nfunk.jep.ParseException;

import fiji.expressionparser.test.TestUtilities.ExpectedExpression;


public class TestImgLibAlgorithms <T extends RealType<T>> {

	private final static int PULSE_VALUE = 1;
	private final static int WIDTH = 9;
	private final static int HEIGHT = 9;
	private final static int DEPTH = 9;
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
	private static final int[] TO_NORMALIZE = new int[] {
		0,	0,	0,	0,
		0,	50, 50, 0,
		0, 	0,	0,	0,
		0,	0,	0,	0	};
	
	private Image<UnsignedShortType> image_C;
	private Image<UnsignedShortType> image_D;
	private Map<String, Image<UnsignedShortType>> source_map; 
	{
		// Create source images
		ArrayContainerFactory cfact = new ArrayContainerFactory();
		UnsignedShortType type = new UnsignedShortType();
		ImageFactory<UnsignedShortType> ifact = new ImageFactory<UnsignedShortType>(type, cfact);		
		image_C = ifact.createImage(new int[] {(int) Math.sqrt(CONVOLVED.length), (int) Math.sqrt(CONVOLVED.length)}, "C"); // Spike 3D image
		LocalizableByDimCursor<UnsignedShortType> cc = image_C.createLocalizableByDimCursor();
		cc.setPosition(new int[] { (int) Math.sqrt(CONVOLVED.length)/2, (int) Math.sqrt(CONVOLVED.length)/2});
		cc.getType().set(PULSE_VALUE);
		cc.close();
		//
		image_D = ifact.createImage(new int[] {(int) Math.sqrt(TO_NORMALIZE.length), (int) Math.sqrt(TO_NORMALIZE.length)}, "D"); // Simple image to normalize
		LocalizableByDimCursor<UnsignedShortType> cd = image_D.createLocalizableByDimCursor();
		int[] pos = new int[2];
		for (int i = 0; i < TO_NORMALIZE.length; i++) {
			pos[0] =  i % (int) Math.sqrt(TO_NORMALIZE.length);
			pos[1] =  i / (int) Math.sqrt(TO_NORMALIZE.length);
			cd.setPosition(pos);
			cd.getType().set(TO_NORMALIZE[i]);
		}
		cd.close();
		//		
		source_map = new HashMap<String, Image<UnsignedShortType>>();
		source_map.put("A", image_A);
		source_map.put("C", image_C);
		source_map.put("D", image_D);
	}

	@Test(expected=ParseException.class)
	public void gaussianConvolutionTwoImages() throws ParseException {		
		// Two images -> Should generate an exception
		String expression = "gauss(C,A)";
		doTest(expression, source_map, new ExpectedExpression() {
			@Override
			public <R extends RealType<R>> float getExpectedValue(
					Map<String, LocalizableByDimCursor<R>> cursors) {
				return 0;
			}
		});
	}

	@Test(expected=ParseException.class)
	public void gaussianConvolutionBadOrder() throws ParseException {		
		// Bad order -> Should generate an exception
		String expression = "gauss(1,C)";
		doTest(expression, source_map, new ExpectedExpression() {
			@Override
			public <R extends RealType<R>> float getExpectedValue(
					Map<String, LocalizableByDimCursor<R>> cursors) {
				return 0;
			}
		});
	}

	@Test
	public void gaussianConvolution() throws ParseException {		
		// Should work
		String expression = "gauss(C," + SIGMA + ")" ;
		doTest(expression, source_map, new ExpectedExpression() {
			private int[] position = new int[2];
			@Override
			public <R extends RealType<R>> float getExpectedValue(final Map<String, LocalizableByDimCursor<R>> cursors) {
				final LocalizableByDimCursor<R> cursor = cursors.get("C");
				position = cursor.getPosition();
				final int index = ((int) Math.sqrt(CONVOLVED.length)) * position[1] + position[0];
				return (float) CONVOLVED[index];
			}
		});
	}

	@Test
	public void normalize() throws ParseException {		
		// Should work
		String expression = "normalize(D)" ;
		doTest(expression, source_map, new ExpectedExpression() {
			@Override
			public <R extends RealType<R>> float getExpectedValue(final Map<String, LocalizableByDimCursor<R>> cursors) {
				final LocalizableByDimCursor<R> cursor = cursors.get("D");
				return cursor.getType().getRealFloat() == 0? 0.0f : 0.5f; // since only 2 pixels have non-zero values, they should have the value 0.5 to sum up to 1.
			}
		});
	}
	
	@Test
	public void dither() throws ParseException {
		// Should work
		String expression = "dither(A)";
		Image<FloatType> result = getEvaluationResult(expression, source_map);
		Cursor<FloatType> cr = result.createCursor();
		while (cr.hasNext()) {
			cr.fwd();
			assertTrue(cr.getType().get() == 0f || cr.getType().get() == 1f); // we just check that is 0 or 1, as in a dithered image
		}
	}
	
	@Test
	public void ditherThreshold() throws ParseException {
		// Should work
		String expression = "dither(A,100)";
		Image<FloatType> result = getEvaluationResult(expression, source_map);
		Cursor<FloatType> cr = result.createCursor();
		while (cr.hasNext()) {
			cr.fwd();
			assertTrue(cr.getType().get() == 0f || cr.getType().get() == 1f); // we just check that is 0 or 1, as in a dithered image
		}
	}
	
	@Test(expected=ParseException.class)
	public void ditherBadOrder() throws ParseException {
		// Should NOT work
		String expression = "dither(100,A)";
		Image<FloatType> result = getEvaluationResult(expression, source_map);
		Cursor<FloatType> cr = result.createCursor();
		while (cr.hasNext()) {
			cr.fwd();
			assertTrue(cr.getType().get() == 0f || cr.getType().get() == 1f); // we just check that is 0 or 1, as in a dithered image
		}
	}
	
	@Test(expected=ParseException.class)
	public void ditherBadNumberOfArgs() throws ParseException {
		// Should NOT work
		String expression = "dither(A,100,10)";
		Image<FloatType> result = getEvaluationResult(expression, source_map);
		Cursor<FloatType> cr = result.createCursor();
		while (cr.hasNext()) {
			cr.fwd();
			assertTrue(cr.getType().get() == 0f || cr.getType().get() == 1f); // we just check that is 0 or 1, as in a dithered image
		}
	}
	
	@Test(expected=ParseException.class)
	public void ditherBadNumberOfArgs2() throws ParseException {
		// Should NOT work
		String expression = "dither()";
		Image<FloatType> result = getEvaluationResult(expression, source_map);
		Cursor<FloatType> cr = result.createCursor();
		while (cr.hasNext()) {
			cr.fwd();
			assertTrue(cr.getType().get() == 0f || cr.getType().get() == 1f); // we just check that is 0 or 1, as in a dithered image
		}
	}
	
	
	
	/*
	 * UTILS
	 */
	
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
