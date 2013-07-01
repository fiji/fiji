package fiji;

/**
 * Modify some IJ1 quirks at runtime, thanks to Javassist
 */

import imagej.legacy.LegacyExtensions;
import imagej.util.AppUtils;

import java.io.File;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

public class IJHacker extends JavassistHelper {

	@Override
	public void instrumentClasses() throws BadBytecode, CannotCompileException, NotFoundException {
		CtClass clazz;
		CtMethod method;

		// Class ij.io.Opener
		clazz = get("ij.io.Opener");

		// open text in the Fiji Editor
		method = clazz.getMethod("open", "(Ljava/lang/String;)V");
		method.insertBefore("if (isText($1) && fiji.FijiTools.maybeOpenEditor($1)) return;");

		boolean scriptEditorStuff = true;
		if (!scriptEditorStuff) {
			// Class ij.plugin.frame.Recorder
			clazz = get("ij.plugin.frame.Recorder");

			// create new macro in the Script Editor
			method = clazz.getMethod("createMacro", "()V");
			dontReturnWhenEditorIsNull(method.getMethodInfo());
			method.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) throws CannotCompileException {
					if (call.getMethodName().equals("createMacro"))
						call.replace("if ($1.endsWith(\".txt\"))"
							+ "  $1 = $1.substring($1.length() - 3) + \"ijm\";"
							+ "boolean b = fiji.FijiTools.openEditor($1, $2);"
							+ "return;");
					else if (call.getMethodName().equals("runPlugIn"))
						call.replace("$_ = null;");
				}
			});
			// create new plugin in the Script Editor
			clazz.addField(new CtField(pool.get("java.lang.String"), "nameForEditor", clazz));
			method = clazz.getMethod("createPlugin", "(Ljava/lang/String;Ljava/lang/String;)V");
			method.insertBefore("this.nameForEditor = $2;");
			method.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) throws CannotCompileException {
					if (call.getMethodName().equals("runPlugIn"))
						call.replace("$_ = null;"
							+ "new ij.plugin.NewPlugin().createPlugin(this.nameForEditor, ij.plugin.NewPlugin.PLUGIN, $2);"
							+ "return;");
				}
			});

			// Class ij.plugin.NewPlugin
			clazz = get("ij.plugin.NewPlugin");

			// open new plugin in Script Editor
			method = clazz.getMethod("createMacro", "(Ljava/lang/String;)V");
			method.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) throws CannotCompileException {
					if (call.getMethodName().equals("create"))
						call.replace("if ($1.endsWith(\".txt\"))"
							+ "  $1 = $1.substring(0, $1.length() - 3) + \"ijm\";"
							+ "if ($1.endsWith(\".ijm\")) {"
							+ "  fiji.FijiTools.openEditor($1, $2);"
							+ "  return;"
							+ "}"
							+ "int options = (monospaced ? ij.plugin.frame.Editor.MONOSPACED : 0)"
							+ "  | (menuBar ? ij.plugin.frame.Editor.MENU_BAR : 0);"
							+ "new ij.plugin.frame.Editor(rows, columns, 0, options).create($1, $2);");
					else if (call.getMethodName().equals("runPlugIn"))
						call.replace("$_ = null;");
				}

				@Override
				public void edit(NewExpr expr) throws CannotCompileException {
					if (expr.getClassName().equals("ij.plugin.frame.Editor"))
						expr.replace("$_ = null;");
				}
			});
			// open new plugin in Script Editor
			method = clazz.getMethod("createPlugin", "(Ljava/lang/String;ILjava/lang/String;)V");
			dontReturnWhenEditorIsNull(method.getMethodInfo());
			method.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall call) throws CannotCompileException {
					if (call.getMethodName().equals("create"))
						call.replace("boolean b = fiji.FijiTools.openEditor($1, $2);"
							+ "return;");
					else if (call.getMethodName().equals("runPlugIn"))
						call.replace("$_ = null;");
				}

			});
		}

		LegacyExtensions.setAppName("(Fiji Is Just) ImageJ");
		LegacyExtensions.setIcon(new File(AppUtils.getBaseDirectory(), "images/icon.png"));
	}

	private void dontReturnWhenEditorIsNull(MethodInfo info) throws CannotCompileException {
		CodeIterator iterator = info.getCodeAttribute().iterator();
	        while (iterator.hasNext()) try {
	                int pos = iterator.next();
			int c = iterator.byteAt(pos);
			if (c == Opcode.IFNONNULL && iterator.byteAt(pos + 3) == Opcode.RETURN) {
				iterator.writeByte(Opcode.POP, pos);
				iterator.writeByte(Opcode.NOP, pos + 1);
				iterator.writeByte(Opcode.NOP, pos + 2);
				iterator.writeByte(Opcode.NOP, pos + 3);
				return;
			}
		}
		catch (BadBytecode e) {
			throw new CannotCompileException(e);
		}
		throw new CannotCompileException("Check not found");
	}

}
