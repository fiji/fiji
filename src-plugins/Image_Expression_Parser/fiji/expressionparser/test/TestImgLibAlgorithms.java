package fiji.expressionparser.test;

import static fiji.expressionparser.test.TestUtilities.doTest;
import static fiji.expressionparser.test.TestUtilities.image_A;

import java.util.HashMap;
import java.util.Map;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;

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
	
	private Image<UnsignedShortType> image_C;
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
		source_map = new HashMap<String, Image<UnsignedShortType>>();
		source_map.put("A", image_A);
		source_map.put("C", image_C);
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
		// Bad order -> Should generate an exception
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
