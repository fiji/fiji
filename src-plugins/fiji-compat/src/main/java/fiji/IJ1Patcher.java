package fiji;

import imagej.legacy.DefaultLegacyService;

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
				// make sure that ImageJ2's LegacyInjector runs
				DefaultLegacyService.preinit();
			} catch (final NoClassDefFoundError e) {
				e.printStackTrace();
				// ImageJ2 not installed
				System.err.println("Did not find DefaultLegacyService class: " + e);
			}
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