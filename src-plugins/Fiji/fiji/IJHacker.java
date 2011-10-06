package fiji;

/**
 * Modify some IJ1 quirks at runtime, thanks to Javassist
 */

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.Handler;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

public class IJHacker implements Runnable {
	public final static String appName = "Fiji";

	protected String replaceAppName = ".replace(\"ImageJ\", \"" + appName + "\")";

	public void run() {
		try {
			ClassPool pool = ClassPool.getDefault();
			CtClass clazz;
			CtMethod method;
			CtField field;

			// Class ij.ImagePlus
			clazz = pool.get("ij.ImagePlus");

			// add back the (deprecated) killProcessor(), and overlay methods
			method = CtNewMethod.make("public void killProcessor() {}", clazz);
			clazz.addMethod(method);
			method = CtNewMethod.make("public void setDisplayList(java.util.Vector list) {"
				+ "  getCanvas().setDisplayList(list);"
				+ "}", clazz);
			clazz.addMethod(method);
			method = CtNewMethod.make("public java.util.Vector getDisplayList() {"
				+ "  return getCanvas().getDisplayList();"
				+ "}", clazz);
			clazz.addMethod(method);
			method = CtNewMethod.make("public void setDisplayList(ij.gui.Roi roi, java.awt.Color strokeColor, int strokeWidth, java.awt.Color fillColor) {"
				+ "  setOverlay(roi, strokeColor, strokeWidth, fillColor);"
				+ "}", clazz);
			clazz.addMethod(method);

			clazz.toClass();

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
			// tell the error() method to use "Fiji" as window title
			method = clazz.getMethod("error",
				"(Ljava/lang/String;Ljava/lang/String;)V");
			method.insertBefore("if ($1 == null || $1.equals(\"ImageJ\")) $1 = \"" + appName + "\";");
			// make sure that ImageJ has been initialized in batch mode
			method = clazz.getMethod("runMacro", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
			method.insertBefore("if (ij==null && ij.Menus.getCommands()==null) init();");

			clazz.toClass();

			// Class ij.gui.GenericDialog
			clazz = pool.get("ij.gui.GenericDialog");

			// make sure that the dialog is disposed in macro mode
			method = clazz.getMethod("showDialog", "()V");
			method.insertBefore("if (macro) dispose();");

			clazz.toClass();

			// Class ij.gui.NonBlockingGenericDialog
			clazz = pool.get("ij.gui.NonBlockingGenericDialog");

			// make sure not to wait in macro mode
			method = clazz.getMethod("showDialog", "()V");
			method.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) throws CannotCompileException {
					if (call.getMethodName().equals("wait"))
						call.replace("if (isShowing()) wait();");
				}
			});

			clazz.toClass();

			// Class ij.ImageJ
			clazz = pool.get("ij.ImageJ");

			// tell the superclass java.awt.Frame that the window title is "Fiji"
			for (CtConstructor ctor : clazz.getConstructors())
				ctor.instrument(new ExprEditor() {
					@Override
					public void edit(ConstructorCall call) throws CannotCompileException {
						if (call.getMethodName().equals("super"))
							call.replace("super(\"" + appName + "\");");
					}
				});
			// tell the version() method to prefix the version with "Fiji/"
			method = clazz.getMethod("version", "()Ljava/lang/String;");
			method.insertAfter("$_ = \"" + appName + "/\" + $_;");
			// tell the run() method to use "Fiji" instead of "ImageJ" in the Quit dialog
			method = clazz.getMethod("run", "()V");
			replaceAppNameInNew(method, "ij.gui.GenericDialog", 1, 2);
			replaceAppNameInCall(method, "addMessage", 1, 1);
			// use our icon
			method = clazz.getMethod("setIcon", "()V");
			method.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) throws CannotCompileException {
					if (call.getMethodName().equals("getResource"))
						call.replace("$_ = $0.getResource(\"/icon.png\");");
				}
			});
			clazz.getConstructor("(Ljava/applet/Applet;I)V").insertBeforeBody("if ($2 != ij.ImageJ.NO_SHOW) setIcon();");

			clazz.toClass();

			// Class ij.Prefs
			clazz = pool.get("ij.Prefs");

			// use Fiji instead of ImageJ
			clazz.getField("vistaHint").setName("originalVistaHint");
			field = new CtField(pool.get("java.lang.String"), "vistaHint", clazz);
			field.setModifiers(Modifier.STATIC | Modifier.PUBLIC | Modifier.FINAL);
			clazz.addField(field, "originalVistaHint" + replaceAppName + ";");
			// do not use the current directory as IJ home on Windows
			String prefsDir = System.getenv("IJ_PREFS_DIR");
			if (prefsDir == null && System.getProperty("os.name").startsWith("Windows"))
				prefsDir = System.getenv("user.home");
			if (prefsDir != null) {
				final String replace = "prefsDir = \"" + prefsDir + "\";";
				method = clazz.getMethod("load", "(Ljava/lang/Object;Ljava/applet/Applet;)Ljava/lang/String;");
				method.instrument(new ExprEditor() {
					@Override
					public void edit(FieldAccess access) throws CannotCompileException {
						if (access.getFieldName().equals("prefsDir") && access.isWriter())
							access.replace(replace);
					}
				});
			}

			clazz.toClass();

			// Class ij.gui.YesNoCancelDialog
			clazz = pool.get("ij.gui.YesNoCancelDialog");

			// use Fiji as window title in the Yes/No dialog
			for (CtConstructor ctor : clazz.getConstructors())
				ctor.instrument(new ExprEditor() {
					@Override
					public void edit(ConstructorCall call) throws CannotCompileException {
						if (call.getMethodName().equals("super"))
							call.replace("super($1, \"ImageJ\".equals($2) ? \"" + appName + "\" : $2, $3);");
					}
				});

			clazz.toClass();

			// Class ij.gui.Toolbar
			clazz = pool.get("ij.gui.Toolbar");

			// use Fiji/ImageJ in the status line
			method = clazz.getMethod("showMessage", "(I)V");
			method.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) throws CannotCompileException {
					if (call.getMethodName().equals("showStatus"))
						call.replace("if ($1.startsWith(\"ImageJ \")) $1 = \"" + appName + "/\" + $1;"
							+ "ij.IJ.showStatus($1);");
				}
			});
			// tool names can be prefixes of other tools, watch out for that!
			method = clazz.getMethod("getToolId", "(Ljava/lang/String;)I");
			method.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) throws CannotCompileException {
					if (call.getMethodName().equals("startsWith"))
						call.replace("$_ = $0.equals($1) || $0.startsWith($1 + \"-\") || $0.startsWith($1 + \" -\");");
				}
			});

			clazz.toClass();

			// Class ij.plugin.CommandFinder
			clazz = pool.get("ij.plugin.CommandFinder");

			// use Fiji in the window title
			method = clazz.getMethod("export", "()V");
			replaceAppNameInNew(method, "ij.text.TextWindow", 1, 5);

			clazz.toClass();

			// Class ij.plugin.Hotkeys
			clazz = pool.get("ij.plugin.Hotkeys");

			// Replace application name in removeHotkey()
			method = clazz.getMethod("removeHotkey", "()V");
			replaceAppNameInCall(method, "addMessage", 1, 1);
			replaceAppNameInCall(method, "showStatus", 1, 1);

			clazz.toClass();

			// Class ij.plugin.Options
			clazz = pool.get("ij.plugin.Options");

			// Replace application name in restart message
			method = clazz.getMethod("appearance", "()V");
			replaceAppNameInCall(method, "showMessage", 2, 2);

			clazz.toClass();

			// Class JavaScriptEvaluator
			clazz = pool.get("JavaScriptEvaluator");

			// make sure Rhino gets the correct class loader
			method = clazz.getMethod("run", "()V");
			method.insertBefore("Thread.currentThread().setContextClassLoader(ij.IJ.getClassLoader());");

			clazz.toClass();

			// Class ij.CompositeImage
			clazz = pool.get("ij.CompositeImage");

			// ImageJA had this public method
			method = CtNewMethod.make("public ij.ImagePlus[] splitChannels(boolean closeAfter) {"
				+ "  ij.ImagePlus[] result = ij.plugin.ChannelSplitter.split(this);"
				+ "  if (closeAfter) close();"
				+ "  return result;"
				+ "}", clazz);
			clazz.addMethod(method);

			clazz.toClass();

			// Class ij.plugin.filter.RGBStackSplitter
			clazz = pool.get("ij.plugin.filter.RGBStackSplitter");

			// add back the splitChannesToArray() method
			method = CtNewMethod.make("public static ij.ImagePlus[] splitChannelsToArray(ij.ImagePlus imp, boolean closeAfter) {"
				+ "  if (!imp.isComposite()) {"
				+ "    ij.IJ.error(\"splitChannelsToArray was called on a non-composite image\");"
				+ "    return null;"
				+ "  }"
				+ "  ij.ImagePlus[] result = ij.plugin.ChannelSplitter.split(imp);"
				+ "  if (closeAfter)"
				+ "    imp.close();"
				+ "  return result;"
				+ "}", clazz);
			clazz.addMethod(method);

			clazz.toClass();

			// Class ij.io.Opener
			clazz = pool.get("ij.io.Opener");

			// make sure that the check for Bio-Formats is correct
			clazz.getClassInitializer().instrument(new ExprEditor() {
				@Override
				public void edit(FieldAccess access) throws CannotCompileException {
					if (access.getFieldName().equals("bioformats") && access.isWriter())
						access.replace("bioformats = ij.IJ.getClassLoader().loadClass(\"loci.plugins.LociImporter\") != null;");
				}
			});

			clazz.toClass();

			// Class ij.macro.Interpreter
			clazz = pool.get("ij.macro.Interpreter");

			// make sure no dialog is opened in headless mode
			method = clazz.getMethod("showError", "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V");
			method.insertBefore("if (ij.IJ.getInstance() == null) {"
				+ "  java.lang.System.err.println($1 + \": \" + $2);"
				+ "  return;"
				+ "}");

			clazz.toClass();

			// Class ij.plugin.DragAndDrop
			clazz = pool.get("ij.plugin.DragAndDrop");

			// make sure that symlinks are _not_ resolved (because then the parent info in the FileInfo would be wrong)
			method = clazz.getMethod("openFile", "(Ljava/io/File;)V");
			method.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) throws CannotCompileException {
					if (call.getMethodName().equals("getCanonicalPath"))
						call.replace("$_ = $0.getAbsolutePath();");
				}
			});

			clazz.toClass();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (CannotCompileException e) {
			System.err.println(e.getMessage() + "\n" + e.getReason());
			e.printStackTrace();
			Throwable cause = e.getCause();
			if (cause != null)
				cause.printStackTrace();
		}
	}

	/**
	 * Replace the application name in the given method in the given parameter to the given constructor call
	 */
	public void replaceAppNameInNew(final CtMethod method, final String name, final int parameter, final int parameterCount) throws CannotCompileException {
		final String replace = getReplacement(parameter, parameterCount);
		method.instrument(new ExprEditor() {
			@Override
			public void edit(NewExpr expr) throws CannotCompileException {
				if (expr.getClassName().equals(name))
					expr.replace("$_ = new " + name + replace + ";");
			}
		});
	}

	/**
	 * Replace the application name in the given method in the given parameter to the given method call
	 */
	public void replaceAppNameInCall(final CtMethod method, final String name, final int parameter, final int parameterCount) throws CannotCompileException {
		final String replace = getReplacement(parameter, parameterCount);
		method.instrument(new ExprEditor() {
			@Override
			public void edit(MethodCall call) throws CannotCompileException {
				if (call.getMethodName().equals(name))
					call.replace("$0." + name + replace + ";");
			}
		});
	}

	private String getReplacement(int parameter, int parameterCount) {
		final StringBuilder builder = new StringBuilder();
		builder.append("(");
		for (int i = 1; i <= parameterCount; i++) {
			if (i > 1)
				builder.append(", ");
			builder.append("$").append(i);
			if (i == parameter)
				builder.append(replaceAppName);
		}
		builder.append(")");
		return builder.toString();
	}
}