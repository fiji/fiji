package fiji;

import java.io.IOException;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.JarFile;

public class JarLauncher {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Missing argument");
			System.exit(1);
		}
		String[] shifted = new String[args.length - 1];
		System.arraycopy(args, 1, shifted, 0, shifted.length);
		launchJar(args[0], shifted);
	}

	// helper to launch .jar files (by inspecting their Main-Class
	// attribute).
	public static void launchJar(String jarPath, String[] arguments) {
		JarFile jar = null;
		try {
			jar = new JarFile(jarPath);
		} catch (IOException e) {
			System.err.println("Could not read '" + jarPath + "'.");
			System.exit(1);
		}
		Manifest manifest = null;
		try {
			manifest = jar.getManifest();
		} catch (IOException e) { }
		if (manifest == null) {
			System.err.println("No manifest found in '"
					+ jarPath + "'.");
			System.exit(1);
		}
		Attributes attributes = manifest.getMainAttributes();
		String className = attributes == null ? null :
			attributes.getValue("Main-Class");
		if (className == null) {
			System.err.println("No main class attribute found in '"
					+ jarPath + "'.");
			System.exit(1);
		}
		Class main = null;
		try {
			main = Class.forName(className);
		} catch (ClassNotFoundException e) {
			System.err.println("Class '" + className
					+ "' was not found in '"
					+ jarPath + "'.");
			System.exit(1);
		}
		Class[] argsType = new Class[] { arguments.getClass() };
		Method mainMethod = null;
		try {
			mainMethod = main.getMethod("main", argsType);
		} catch (NoSuchMethodException e) {
			System.err.println("Class '" + className
					+ "' in '" + jarPath
					+ "' does not have a main() method.");
			System.exit(1);
		}
		Integer result = new Integer(1);
		try {
                        result = (Integer)mainMethod.invoke(null,
                                        new Object[] { arguments });
                } catch (IllegalAccessException e) {
                        System.err.println("The main() method of class '"
                                        + className + "' in '" + jarPath
                                        + "' is not public.");
                } catch (InvocationTargetException e) {
                        System.err.println("Error while executing the main() "
                                        + "method of class '" + className
                                        + "' in '" + jarPath + "':");
                        e.getTargetException().printStackTrace();
                }
		if (result != null)
			System.exit(result.intValue());
	}
}
