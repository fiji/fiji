package fiji;

/**
 * Patch ij.jar using Javassist, handle headless mode, too.
 *
 * @author Johannes Schindelin
 */

public class IJ1Patcher implements Runnable {
	private static boolean alreadyPatched;

	@Override
	public void run() {
		if (alreadyPatched || "false".equals(System.getProperty("patch.ij1"))) return;
		alreadyPatched = true;
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