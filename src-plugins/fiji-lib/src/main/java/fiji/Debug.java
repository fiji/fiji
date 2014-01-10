package fiji;

import fiji.debugging.Object_Inspector;

public class Debug {
	static {
		// if ij-legacy is in the class path, run the preinit() method
		try {
			final ClassLoader loader = Thread.currentThread().getContextClassLoader();
			final Class<?> clazz = loader.loadClass("imagej.legacy.DefaultLegacyService");
			clazz.getMethod("preinit").invoke(null);
		} catch (Exception e) {
			// ignore if it is not in the class path
			if (!(e instanceof ClassNotFoundException)) {
				e.printStackTrace();
			}
		}
	}

	public static void show(Object object) {
		Object_Inspector.openFrame("" + object, object);
	}

	public static Object get(Object object, String fieldName) {
		return Object_Inspector.get(object, fieldName);
	}

	/**
	 * Debug helper
	 *
	 * Call this function from your debugger to debug your plugin
	 *
	 * @param plugin the menu item label of the plugin to run
	 * @param pass the parameters as recorded by the Recorder, or null if dialogs should pop up
	 */
	public static void run(String plugin, String parameters) {
		LegacyHelper.run(plugin, parameters);
	}

	/**
	 * Debug helper
	 *
	 * Call this function from your debugger to debug your filter plugin
	 *
	 * @param imagePath the path to the example image to test with
	 * @param plugin the menu item label of the plugin to run
	 * @param pass the parameters as recorded by the Recorder, or null if dialogs should pop up
	 */
	public static void runFilter(String imagePath, String plugin, String parameters) {
		LegacyHelper.runFilter(imagePath, plugin, parameters);
	}
}