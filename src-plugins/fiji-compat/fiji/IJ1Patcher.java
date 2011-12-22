package fiji;

/**
 * Patch ij.jar using Javassist, handle headless mode, too.
 *
 * @author Johannes Schindelin
 */

public class IJ1Patcher implements Runnable {
	@Override
	public void run() {
		try {
			String headless = System.getProperty("java.awt.headless");
			if ("true".equalsIgnoreCase(headless))
				new Headless().run();
			new IJHacker().run();
			try {
				JavassistHelper.defineClasses();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (NoClassDefFoundError e) {
			// Deliberately ignored - in some cases
			// javassist can not be found, and we should
			// continue anyway.
		}
	}
}