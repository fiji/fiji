package fiji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.junit.Test;

/**
 * Verifies that the {@link Debug} class' {@code run} methods work as advertised.
 * 
 * @author Johannes Schindelin
 */
public class DebugTest {
	@Test
	public void runFilterTest() throws Exception {
		final File image = new File("../../images/fiji-logo-1.0-128x128.png");
		assumeTrue(image.exists());
		final File tmp = File.createTempFile("fiji-debug-", ".txt");
		Debug.runFilter(image.getAbsolutePath(), "Dimension Test PlugInFilter", "output=[" + tmp.getAbsolutePath() + "]", true);

		final BufferedReader reader = new BufferedReader(new FileReader(tmp));
		try {
			final String line = reader.readLine();
			assertEquals("[128, 128, 1, 1, 1]", line);
			assertNull(reader.readLine());
		} finally {
			reader.close();
		}
	}
}
