package fiji.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

/**
 * Tests the {@link NumberParser}.
 * <p>
 * It is a dirty set of tests because each test overrides the JVM's default
 * locale.
 * </p>
 *
 * @author Curtis Rueden
 */
public class NumberParserTest {

	@Test
	public void testFrance() {
		Locale.setDefault(Locale.FRANCE);
		final double value = NumberParser.parseDouble("902.300");
		assertEquals(902.0, value, 0.0); // !!
		final double value2 = NumberParser.parseDouble("902,300");
		assertEquals(902.3, value2, 0.0);
		final double value3 = NumberParser.parseDouble("902300");
		assertEquals(902300.0, value3, 0.0);
		final double value4 = NumberParser.parseDouble( "Infinity" );
		assertEquals( Double.POSITIVE_INFINITY, value4, 0.0 );
		final double value5 = NumberParser.parseDouble( "-Infinity" );
		assertEquals( Double.NEGATIVE_INFINITY, value5, 0.0 );
		final double value6 = NumberParser.parseDouble( "NaN" );
		assertTrue( Double.isNaN( value6 ) );
	}

	@Test
	public void testGermany() {
		Locale.setDefault(Locale.GERMANY);
		final double value = NumberParser.parseDouble("902.300");
		assertEquals(902300.0, value, 0.0);
		final double value2 = NumberParser.parseDouble("902,300");
		assertEquals(902.3, value2, 0.0);
		final double value3 = NumberParser.parseDouble("902300");
		assertEquals(902300.0, value3, 0.0);
		final double value4 = NumberParser.parseDouble( "Infinity" );
		assertEquals( Double.POSITIVE_INFINITY, value4, 0.0 );
		final double value5 = NumberParser.parseDouble( "-Infinity" );
		assertEquals( Double.NEGATIVE_INFINITY, value5, 0.0 );
		final double value6 = NumberParser.parseDouble( "NaN" );
		assertTrue( Double.isNaN( value6 ) );
	}

	@Test
	public void testUS() {
		Locale.setDefault(Locale.US);
		final double value = NumberParser.parseDouble("902.300");
		assertEquals(902.3, value, 0.0);
		final double value2 = NumberParser.parseDouble("902,300");
		assertEquals(902300.0, value2, 0.0);
		final double value3 = NumberParser.parseDouble("902300");
		assertEquals(902300.0, value3, 0.0);
		final double value4 = NumberParser.parseDouble( "Infinity" );
		assertEquals( Double.POSITIVE_INFINITY, value4, 0.0 );
		final double value5 = NumberParser.parseDouble( "-Infinity" );
		assertEquals( Double.NEGATIVE_INFINITY, value5, 0.0 );
		final double value6 = NumberParser.parseDouble( "NaN" );
		assertTrue( Double.isNaN( value6 ) );
	}

}
