/**
 * 
 */
package fiji.expressionparser.test;


import static fiji.expressionparser.test.TestUtilities.doTest;
import static fiji.expressionparser.test.TestUtilities.doTestNumbers;
import static fiji.expressionparser.test.TestUtilities.image_A;

import java.util.HashMap;
import java.util.Map;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;

import org.junit.Test;
import org.nfunk.jep.ParseException;

import fiji.expressionparser.test.TestUtilities.ExpectedExpression;


/**
 * @author Jean-Yves Tinevez
 *
 */
public class TestTwoOperandsPixelBasedFunctions {
	
	private Map<String, Image<UnsignedShortType>> source_map; 
	{
		source_map = new HashMap<String, Image<UnsignedShortType>>();
		source_map.put("A", image_A);
	}
	
	
	@Test
	public void atan2TwoImage() throws ParseException {
		String expression = "atan2(A,A)";;	
		ExpectedExpression ee = new ExpectedExpression() {
			@Override
			public final <T extends RealType<T>> float getExpectedValue(Map<String, LocalizableByDimCursor<T>> cursors) {
				final LocalizableByDimCursor<T> source = cursors.get("A");
				return source.getType().getRealFloat() == 0f? 0f : (float) (45*Math.PI/180);
			}
		};
		doTest(expression, source_map, ee);
	}
	
	@Test
	public void atan2RightSingletonExpansion() throws ParseException {
		String expression = "atan2(A,10)";
		ExpectedExpression ee = new ExpectedExpression() {
			@Override
			public final <T extends RealType<T>>  float getExpectedValue(Map<String, LocalizableByDimCursor<T>> cursors) {
				final LocalizableByDimCursor<T> source = cursors.get("A");
				return (float) Math.atan(source.getType().getRealFloat() / 10 );
			}
		};
		doTest(expression, source_map, ee);
	}

	@Test
	public void atan2LeftSingletonExpansion() throws ParseException {
		String expression = "atan2(100, A)";
		ExpectedExpression ee = new ExpectedExpression() {
			@Override
			public final <T extends RealType<T>>  float getExpectedValue(Map<String, LocalizableByDimCursor<T>> cursors) {
				final LocalizableByDimCursor<T> source = cursors.get("A");
				return (float) Math.atan(100 / source.getType().getRealFloat() );
			}
		};
		doTest(expression, source_map, ee);
	}
	
	@Test
	public void atan2TwoNumbers() throws ParseException {
		String expression = "atan2(1.14, 1.14)";
		doTestNumbers(expression, (float) (45*Math.PI/180));
	}
		
	
	
}
