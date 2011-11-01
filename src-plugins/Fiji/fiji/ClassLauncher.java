package fiji;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ClassLauncher {
	/**
	 * Patch ij.jar and launch the class given as first argument passing on the remaining arguments
	 *
	 * @param arguments A list containing the name of the class whose main() method is to be called
	 *        with the remaining arguments.
	 */
	public static void main(String[] arguments) {
		ClassLoaderPlus classLoader = ClassLoaderPlus.getInFijiDirectory("jars/fiji-compat.jar", "jars/ij.jar", "jars/javassist.jar");
		try {
			patchIJ1(classLoader);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String[] stripped = new String[arguments.length - 1];
		if (stripped.length > 0)
			System.arraycopy(arguments, 1, stripped, 0, stripped.length);
		launch(classLoader, arguments[0], stripped);
	}

	protected static void patchIJ1(ClassLoader classLoader) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		Class<Runnable> clazz = (Class<Runnable>)classLoader.loadClass("fiji.IJ1Patcher");
		Runnable ij1Patcher = clazz.newInstance();
		ij1Patcher.run();
	}

	protected static void launch(ClassLoader classLoader, String className, String[] arguments) {
		Class main = null;
		try {
			main = classLoader.loadClass(className.replace('/', '.'));
		} catch (ClassNotFoundException e) {
			System.err.println("Class '" + className + "' was not found");
			System.exit(1);
		}
		Class[] argsType = new Class[] { arguments.getClass() };
		Method mainMethod = null;
		try {
			mainMethod = main.getMethod("main", argsType);
		} catch (NoSuchMethodException e) {
			System.err.println("Class '" + className + "' does not have a main() method.");
			System.exit(1);
		}
		Integer result = new Integer(1);
		try {
                        result = (Integer)mainMethod.invoke(null,
                                        new Object[] { arguments });
                } catch (IllegalAccessException e) {
                        System.err.println("The main() method of class '" + className + "' is not public.");
                } catch (InvocationTargetException e) {
                        System.err.println("Error while executing the main() " + "method of class '" + className + "':");
                        e.getTargetException().printStackTrace();
                }
		if (result != null)
			System.exit(result.intValue());
	}
}
