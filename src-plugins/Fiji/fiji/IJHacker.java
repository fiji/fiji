package fiji;

/**
 * Modify some IJ1 quirks at runtime, thanks to Javassist
 */

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;

import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.Handler;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

public class IJHacker implements Runnable {
	public void run() {
		try {
			ClassPool pool = ClassPool.getDefault();
			CtClass clazz;
			CtMethod method;

			// Class ij.IJ
			clazz = pool.get("ij.IJ");

			// tell runUserPlugIn() to mention which class was not found if a dependency is missing
			method = clazz.getMethod("runUserPlugIn",
				"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/Object;");
			method.instrument(new ExprEditor() {
				@Override
				public void edit(Handler handler) throws CannotCompileException {
					try {
						if (handler.getType().getName().equals("java.lang.NoClassDefFoundError"))
							handler.insertBefore("String cause = $1.getMessage();"
							+ "int index = cause.indexOf('(') + 1;"
							+ "int endIndex = cause.indexOf(')', index);"
							+ "if (!suppressPluginNotFoundError && index > 0 && endIndex > index) {"
							+ "  String name = cause.substring(index, endIndex);"
							+ "  error(\"Did not find required class: \" + $1.getMessage());"
							+ "  return null;"
							+ "}");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			clazz.toClass();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (CannotCompileException e) {
			e.printStackTrace();
		}
	}
}