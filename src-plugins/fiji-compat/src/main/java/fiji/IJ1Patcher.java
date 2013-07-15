package fiji;

import imagej.legacy.DefaultLegacyService;
import imagej.legacy.LegacyExtensions;
import imagej.legacy.LegacyExtensions.LegacyEditorPlugin;
import imagej.util.AppUtils;

import java.io.File;

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
			LegacyExtensions.setAppName("(Fiji Is Just) ImageJ");
			LegacyExtensions.setIcon(new File(AppUtils.getBaseDirectory(), "images/icon.png"));
			LegacyExtensions.setLegacyEditor(new LegacyEditorPlugin() {

				@Override
				public boolean open(File path) {
					return FijiTools.openFijiEditor(path);
				}

				@Override
				public boolean create(String title, String body) {
					return FijiTools.openFijiEditor(title, body);
				}
			});
			// make sure to run some Fiji-specific stuff after Help>Refresh Menus, e.g. installing all scripts into the menu
			LegacyExtensions.runAfterRefreshMenus(new MenuRefresher());

			try {
				// make sure that ImageJ2's LegacyInjector runs
				DefaultLegacyService.preinit();
			} catch (final NoClassDefFoundError e) {
				e.printStackTrace();
				// ImageJ2 not installed
				System.err.println("Did not find DefaultLegacyService class: " + e);
			}
		} catch (NoClassDefFoundError e) {
			// Deliberately ignored - in some cases
			// javassist or ImageJ2 can not be found, and we should
			// continue anyway.
		}
	}
}