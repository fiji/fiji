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
		assertDouble(902.3, "902,300");
		assertCommon();
	}

	@Test
	public void testGermany() {
		Locale.setDefault(Locale.GERMANY);
		assertDouble(902.3, "902,300");
		assertCommon();
	}

	@Test
	public void testUS() {
		Locale.setDefault(Locale.US);
		assertDouble(902300.0, "902,300");
		assertCommon();
	}

	private void assertCommon() {
		assertDouble(-203.9, "-203.9");
		assertDouble(902.3, "902.300");
		assertDouble(902300.0, "902300");
		assertDouble(Double.POSITIVE_INFINITY, "Infinity");
		assertDouble(Double.NEGATIVE_INFINITY, "-Infinity");
		final double value = NumberParser.parseDouble( "NaN" );
		assertTrue(Double.isNaN(value));
	}

	private void assertDouble(final double expect, final String string) {
		final double value = NumberParser.parseDouble(string);
		assertEquals(expect, value, 0.0);
	}
}
