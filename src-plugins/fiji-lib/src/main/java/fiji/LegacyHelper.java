package fiji;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;

import java.io.File;

class LegacyHelper {

	/**
	 * Debug helper
	 *
	 * Call this function from your debugger to debug your plugin
	 *
	 * @param plugin the menu item label of the plugin to run
	 * @param pass the parameters as recorded by the Recorder, or null if dialogs should pop up
	 */
	static void run(String plugin, String parameters) {
		makeSureCallerIsInPluginsDir();
		if (parameters == null)
			IJ.run(plugin);
		else
			IJ.run(plugin, parameters);
		IJ.redirectErrorMessages(false);
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
	static void runFilter(String imagePath, String plugin, String parameters) {
		makeSureCallerIsInPluginsDir();
	    ImagePlus image = IJ.openImage(imagePath);
	    image.show();
	    WindowManager.addWindow(image.getWindow());
	    Debug.run(plugin, parameters);
	}

	static void makeSureCallerIsInPluginsDir() {
		if (IJ.getInstance() != null)
			return; // too late anyway
		// make sure that the calling .jar is found by ImageJ 1.x
		StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		if (stackTrace.length > 3) {
			String className = stackTrace[3].getClassName();
			String relativePath = className.replace('.', '/') + ".class";
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			if (classLoader == null)
				classLoader = Debug.class.getClassLoader();
			String directory = classLoader.getResource(relativePath).getPath();
			directory = directory.substring(0,  directory.length() - relativePath.length());
			if (directory.endsWith("/classes/"))
				directory = directory.substring(0, directory.length() - "/classes/".length());
			else if (directory.endsWith("/test-classes/"))
				directory = directory.substring(0, directory.length() - "/test-classes/".length());
			else if (directory.startsWith("file:") && directory.endsWith("!/"))
				directory = new File(directory.substring(5, directory.length() - 2)).getParent();
			System.setProperty("plugins.dir", directory);
		}
		IJ.redirectErrorMessages();
		new ImageJ();
	}

}
