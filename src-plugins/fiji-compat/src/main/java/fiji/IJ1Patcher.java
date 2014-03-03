package fiji;

import java.awt.GraphicsEnvironment;

import imagej.patcher.LegacyEnvironment;
import imagej.patcher.LegacyInjector;
import imagej.util.AppUtils;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;

/**
 * Patch ij.jar using Javassist, handle headless mode, too.
 *
 * @author Johannes Schindelin
 */

public class IJ1Patcher implements Runnable {
	private static boolean alreadyPatched;
	static boolean ij1PatcherFound;

	@Override
	public void run() {
		if (alreadyPatched || "false".equals(System.getProperty("patch.ij1"))) return;
		try {
			LegacyInjector.preinit();
			new LegacyEnvironment(getClass().getClassLoader(), GraphicsEnvironment.isHeadless());
			ij1PatcherFound = true;
		} catch (NoClassDefFoundError e) {
			fallBackToPreviousPatcher();
		} catch (ClassNotFoundException e) {
			fallBackToPreviousPatcher();
		}
		alreadyPatched = true;
	}

	private void fallBackToPreviousPatcher() {
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			final ClassPool pool = ClassPool.getDefault();

			CtClass clazz = pool.makeClass("fiji.$TransientFijiEditor");
			clazz.addInterface(pool.get("imagej.legacy.LegacyExtensions$LegacyEditorPlugin"));
			clazz.addConstructor(CtNewConstructor.make(new CtClass[0], new CtClass[0], clazz));
			clazz.addMethod(CtNewMethod.make("public boolean open(java.io.File path) {"
					+ "  return fiji.FijiTools.openFijiEditor(path);"
					+ "}", clazz));
			clazz.addMethod(CtNewMethod.make("public boolean create(java.lang.String title, java.lang.String body) {"
					+ "  return fiji.FijiTools.openFijiEditor(title, body);"
					+ "}",  clazz));
			clazz.toClass();

			clazz = pool.makeClass("fiji.$TransientFijiPatcher");
			clazz.addInterface(pool.get("java.lang.Runnable"));
			clazz.addMethod(CtNewMethod.make("public void run() {"
					+ "  imagej.legacy.LegacyExtensions.setAppName(\"(Fiji Is Just) ImageJ\");"
					+ "  imagej.legacy.LegacyExtensions.setIcon(new java.io.File(\"" + AppUtils.getBaseDirectory() + "/images/icon.png\"));"
					+ "  imagej.legacy.LegacyExtensions.setLegacyEditor(new fiji.$TransientFijiEditor());"
					+ "  /* make sure to run some Fiji-specific stuff after Help>Refresh Menus, e.g. installing all scripts into the menu */"
					+ "  imagej.legacy.LegacyExtensions.runAfterRefreshMenus(new fiji.MenuRefresher());"
					+ "  /* make sure that ImageJ2's LegacyInjector runs */"
					+ "  imagej.legacy.DefaultLegacyService.preinit();"
					+ "}"
					, clazz));
			Runnable run = (Runnable)clazz.toClass().newInstance();
			run.run();
			return;
		} catch (NoClassDefFoundError e) {
			// ignore: probably have newer ImageJ2 in class path
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}