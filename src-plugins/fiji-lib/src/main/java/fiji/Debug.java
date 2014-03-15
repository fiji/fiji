package fiji;

import fiji.debugging.Object_Inspector;
import imagej.patcher.LegacyEnvironment;
import imagej.patcher.LegacyInjector;

public class Debug {
	static {
		LegacyInjector.preinit();
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