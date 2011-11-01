package fiji;

/**
 * Patch ij.jar using Javassist, handle headless mode, too.
 *
 * @author Johannes Schindelin
 */

public class IJ1Patcher implements Runnable {
	@Override
	public void run() {
		String headless = System.getProperty("java.awt.headless");
		if ("true".equalsIgnoreCase(headless))
			new Headless().run();
		new IJHacker().run();
		try {
			JavassistHelper.defineClasses();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}