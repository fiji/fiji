package fiji.expressionparser.test;

import static fiji.expressionparser.test.TestUtilities.*;

import java.util.HashMap;
import java.util.Map;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;

import org.junit.Test;
import org.nfunk.jep.ParseException;

import fiji.expressionparser.test.TestUtilities.ExpectedExpression;

public class TestSingleOperandPixelBasedOperators  <T extends RealType<T>> {

	private Map<String, Image<UnsignedShortType>> source_map; 
	{
		source_map = new HashMap<String, Image<UnsignedShortType>>();
		source_map.put("A", image_A);
	}


	@Test
	public void uMinus() throws ParseException {
		String expression = "-A" ;
		doTest(expression, source_map, new ExpectedExpression() {
			@Override
			public <R extends RealType<R>> float getExpectedValue(final Map<String, LocalizableByDimCursor<R>> cursors) {
				final LocalizableByDimCursor<R> cursor = cursors.get("A");
				return -cursor.getType().getRealFloat();
			}
		});
	}	

	@Test
	public void not() throws ParseException {
		String expression = "!A" ;
		doTest(expression, source_map, new ExpectedExpression() {
			@Override
			public <R extends RealType<R>> float getExpectedValue(final Map<String, LocalizableByDimCursor<R>> cursors) {
				LocalizableByDimCursor<R> cursor = cursors.get("A");
				return cursor.getType().getRealFloat() == 0f ? 1.0f : 0.0f;
			}
		});
	}	

}
