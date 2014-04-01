package fiji.util;

import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
		final double value = NumberParser.parseDouble("902300");
		assertEquals(902300.0, value, 0.0);
	}

	@Test
	public void testGermany() {
		Locale.setDefault(Locale.GERMANY);
		final double value = NumberParser.parseDouble("902.300");
		assertEquals(902300.0, value, 0.0);
	}

	@Test
	public void testUS() {
		Locale.setDefault(Locale.US);
		final double value = NumberParser.parseDouble("902,300");
		assertEquals(902300.0, value, 0.0);
	}

}
