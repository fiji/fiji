package fiji;

/**
 * This class uses Javassist to provide headless mode operations
 *
 * At least on Linux, which is the most prevalent operating system on cluster nodes due to its
 * ease of deployment and licensing, OpenJDK/Sun Java has problems to instantiate certain GUI
 * components such as Menus and Windows. The symptom is a thrown HeadlessException.
 *
 * Therefore, this class can be used to override the superclass of ij.gui.GenericDialog and to
 * override the use of AWT menus in ij.Menus as well. For many batch mode tasks, this is
 * enough to ensure that no HeadlessException gets thrown.
 */

import ij.Macro;

import ij.gui.DialogListener;

import ij.plugin.filter.PlugInFilterRunner;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextArea;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javassist.CannotCompileException;
import javassist.ClassMap;
import javassist.CtConstructor;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

public class Headless extends JavassistHelper {
	/**
	 * Verbosity level
	 *
	 * Positive values make method stubs print something when they're called
	 */
	protected int verbose = 0;

	protected Map<CtClass, CtClass> classMap;
	protected ClassMap nameMap;

	public void instrumentClasses() throws CannotCompileException, NotFoundException {
		classMap = new HashMap<CtClass, CtClass>();
		nameMap = new ClassMap();

		CtClass clazz = get("ij.gui.GenericDialog");
		Set<String> override = new HashSet<String>(Arrays.asList(new String[] { "paint", "getInsets" }));
		for (CtMethod method : clazz.getMethods())
			if (override.contains(method.getName())) {
				CtMethod stub = makeStubMethod(clazz, method);
				method.setBody(stub, null);
			}
		CtClass originalSuperclass = clazz.getSuperclass();
		clazz.setSuperclass(pool.get("fiji.Headless$GenericDialog"));
		letSuperclassMethodsOverride(clazz);
		addMissingMethods(clazz, originalSuperclass);
		rewrite(clazz);
		for (CtConstructor ctor : clazz.getConstructors())
			ctor.instrument(new ExprEditor() {
				@Override
				public void edit(ConstructorCall call) throws CannotCompileException {
					if (call.getMethodName().equals("super"))
						call.replace("super();");
				}
			});

		clazz = get("ij.Menus");
		clazz.getMethod("installJarPlugin", "(Ljava/lang/String;Ljava/lang/String;)V")
			.insertBefore("int quote = $2.indexOf('\"');"
				+ "if (quote >= 0)"
				+ "  addPluginItem(null, $2.substring(quote));");
		rewrite(clazz);

		clazz = get("ij.plugin.HyperStackConverter");
		clazz.instrument(new ExprEditor() {
			@Override
			public void edit(NewExpr expr) throws CannotCompileException {
				if (expr.getClassName().equals("ij.gui.StackWindow"))
					expr.replace("$1.show(); $_ = null;");
			}
		});

		clazz = get("ij.plugin.Duplicator");
		clazz.instrument(new ExprEditor() {
			@Override
			public void edit(MethodCall call) throws CannotCompileException {
				String name = call.getMethodName();
				if (name.equals("addTextListener"))
					call.replace("");
				else if (name.equals("elementAt"))
					call.replace("$_ = $0 == null ? null : $0.elementAt($$);");
			}
		});
	}


	protected void addMissingMethods(CtClass fakeClass, CtClass originalClass) throws CannotCompileException, NotFoundException {
		if (verbose > 0)
			System.err.println("adding missing methods from " + originalClass.getName() + " to " + fakeClass.getName());
		Set<String> available = new HashSet<String>();
		for (CtMethod method : fakeClass.getMethods())
			available.add(stripPackage(method.getLongName()));
		for (CtMethod original : originalClass.getDeclaredMethods()) {
			if (available.contains(stripPackage(original.getLongName())))
				continue;

			CtMethod method = makeStubMethod(fakeClass, original);
			fakeClass.addMethod(method);
		}

		// interfaces
		Set<CtClass> availableInterfaces = new HashSet<CtClass>();
		for (CtClass iface : fakeClass.getInterfaces())
			availableInterfaces.add(iface);
		for (CtClass iface : originalClass.getInterfaces())
			if (!availableInterfaces.contains(iface))
				fakeClass.addInterface(iface);

		CtClass superClass = originalClass.getSuperclass();
		if (superClass != null && !superClass.getName().equals("java.lang.Object"))
			addMissingMethods(fakeClass, superClass);
	}

	protected CtMethod makeStubMethod(CtClass clazz, CtMethod original) throws CannotCompileException, NotFoundException {
		// add a stub
		String prefix = "";
		if (verbose > 0) {
			prefix = "System.err.println(\"Called " + original.getLongName() + "\\n\"";
			if (verbose > 1)
				prefix += "+ \"\\t(\" + fiji.Headless.toString($args) + \")\\n\"";
			prefix += ");";
		}

		CtClass type = original.getReturnType();
		String body = "{" +
			prefix +
			(type == CtClass.voidType ? "" : "return " + defaultReturnValue(type) + ";") +
			"}";
		CtClass[] types = original.getParameterTypes();
		return CtNewMethod.make(type, original.getName(), types, new CtClass[0], body, clazz);
	}

	protected String defaultReturnValue(CtClass type) {
		return (type == CtClass.booleanType ? "false" :
			(type == CtClass.byteType ? "(byte)0" :
			 (type == CtClass.charType ? "'\0'" :
			  (type == CtClass.doubleType ? "0.0" :
			   (type == CtClass.floatType ? "0.0f" :
			    (type == CtClass.intType ? "0" :
			     (type == CtClass.longType ? "0l" :
			      (type == CtClass.shortType ? "(short)0" : "null"))))))));
	}

	protected void letSuperclassMethodsOverride(CtClass clazz) throws CannotCompileException, NotFoundException {
		for (CtMethod method : clazz.getSuperclass().getDeclaredMethods()) {
			CtMethod method2 = clazz.getMethod(method.getName(), method.getSignature());
			if (method2.getDeclaringClass().equals(clazz)) {
				method2.setBody(method, null); // make sure no calls/accesses to GUI components are remaining
				method2.setName("narf" + method.getName());
			}
		}
	}

	protected void rewrite(String className) throws CannotCompileException, NotFoundException {
		rewrite(get(className));
	}

	protected void rewrite(CtClass clazz) throws CannotCompileException, NotFoundException {
		clazz.instrument(new ExprEditor() {
			@Override
			public void edit(NewExpr expr) throws CannotCompileException {
				String name = expr.getClassName();
				if (name.startsWith("java.awt.Menu") || name.equals("java.awt.PopupMenu") ||
						name.startsWith("java.awt.Checkbox") || name.equals("java.awt.Frame"))
					expr.replace("$_ = null;");
			}

			@Override
			public void edit(MethodCall call) throws CannotCompileException {
				String name = call.getClassName();
				if (name.startsWith("java.awt.Menu") || name.equals("java.awt.PopupMenu") ||
						name.startsWith("java.awt.Checkbox")) try {
					CtClass type = call.getMethod().getReturnType();
					if (type == CtClass.voidType)
						call.replace("");
					else
						call.replace("$_ = " + defaultReturnValue(type) + ";");
				} catch (NotFoundException e) {
					e.printStackTrace();
				}
				else if (call.getMethodName().equals("put") && call.getClassName().equals("java.util.Properties"))
					call.replace("if ($1 != null && $2 != null) $_ = $0.put($1, $2); else $_ = null;");
				else if (call.getMethodName().equals("get") && call.getClassName().equals("java.util.Properties"))
					call.replace("$_ = $1 != null ? $0.get($1) : null;");
				else if (name.equals("java.lang.Integer") && call.getMethodName().equals("intValue"))
					call.replace("$_ = $0 == null ? 0 : $0.intValue();");
			}
		});
	}

	protected static boolean getMacroParameter(String label, boolean defaultValue) {
		return getMacroParameter(label) != null || defaultValue;
	}

	protected static double getMacroParameter(String label, double defaultValue) {
		String value = Macro.getValue(Macro.getOptions(), label, null);
		return value != null ? Double.parseDouble(value) : defaultValue;
	}

	protected static String getMacroParameter(String label, String defaultValue) {
		return Macro.getValue(Macro.getOptions(), label, defaultValue);
	}

	protected static String getMacroParameter(String label) {
		return Macro.getValue(Macro.getOptions(), label, null);
	}

	protected static class GenericDialog {
		protected List<Double> numbers;
		protected List<String> strings;
		protected List<Boolean> checkboxes;
		protected List<String> choices;
		protected List<Integer> choiceIndices;
		protected List<Double> sliders;
		protected String textArea1, textArea2;

		protected int numberfieldIndex = 0, stringfieldIndex = 0, checkboxIndex = 0, choiceIndex = 0, textAreaIndex = 0;
		protected boolean invalidNumber, macro;
		protected String errorMessage;

		public GenericDialog() {
			if (Macro.getOptions() == null)
				throw new RuntimeException("Cannot instantiate headless dialog except in macro mode");
			numbers = new ArrayList<Double>();
			strings = new ArrayList<String>();
			checkboxes = new ArrayList<Boolean>();
			choices = new ArrayList<String>();
			choiceIndices = new ArrayList<Integer>();
			sliders = new ArrayList<Double>();
			macro = true;
		}

		public void addCheckbox(String label, boolean defaultValue) {
			checkboxes.add(getMacroParameter(label, defaultValue));
		}

		public void addCheckboxGroup(int rows, int columns, String[] labels, boolean[] defaultValues) {
			for (int i = 0; i < labels.length; i++)
				addCheckbox(labels[i], defaultValues[i]);
		}

		public void addCheckboxGroup(int rows, int columns, String[] labels, boolean[] defaultValues, String[] headings) {
			addCheckboxGroup(rows, columns, labels, defaultValues);
		}

		public void addChoice(String label, String[] items, String defaultItem) {
			String item = getMacroParameter(label, defaultItem);
			int index = 0;
			for (int i = 0; i < items.length; i++)
				if (items[i].equals(item)) {
					index = i;
					break;
				}
			choiceIndices.add(index);
			choices.add(items[index]);
		}

		public void addNumericField(String label, double defaultValue, int digits) {
			numbers.add(getMacroParameter(label, defaultValue));
		}

		public void addNumericField(String label, double defaultValue, int digits, int columns, String units) {
			addNumericField(label, defaultValue, digits);
		}

		public void addSlider(String label, double minValue, double maxValue, double defaultValue) {
			numbers.add(getMacroParameter(label, defaultValue));
		}

		public void addStringField(String label, String defaultText) {
			strings.add(getMacroParameter(label, defaultText));
		}

		public void addStringField(String label, String defaultText, int columns) {
			addStringField(label, defaultText);
		}

		public void addTextAreas(String text1, String text2, int rows, int columns) {
			textArea1 = text1;
			textArea2 = text2;
		}

		public boolean getNextBoolean() {
			return checkboxes.get(checkboxIndex++);
		}

		public String getNextChoice() {
			return choices.get(choiceIndex++);
		}

		public int getNextChoiceIndex() {
			return choiceIndices.get(choiceIndex++);
		}

		public double getNextNumber() {
			return numbers.get(numberfieldIndex++);
		}

		/** Returns the contents of the next text field. */
		public String getNextString() {
			return strings.get(stringfieldIndex++);
		}

		public String getNextText()  {
			switch (textAreaIndex++) {
			case 0:
				return textArea1;
			case 1:
				return textArea2;
			}
			return null;
		}

		public boolean invalidNumber() {
			boolean wasInvalid = invalidNumber;
			invalidNumber = false;
			return wasInvalid;
		}

		public void showDialog() {
			if (Macro.getOptions() == null)
				throw new RuntimeException("Cannot run dialog headlessly");
			numberfieldIndex = 0;
			stringfieldIndex = 0;
			checkboxIndex = 0;
			choiceIndex = 0;
			textAreaIndex = 0;
		}

		public boolean wasCanceled() {
			return false;
		}

		public boolean wasOKed() {
			return true;
		}

		// Needed for IJHacker (it adds a call to dispose() to the showDialog() method
		// which is then inherited into GenericDialog)
		public void dispose() {}

		public void addDialogListener(DialogListener dl) {}
		public void addHelp(String url) {}
		public void addMessage(String text) {}
		public void addMessage(String text, Font font) {}
		public void addPanel(Panel panel) {}
		public void addPanel(Panel panel, int contraints, Insets insets) {}
		public void addPreviewCheckbox(PlugInFilterRunner pfr) {}
		public void addPreviewCheckbox(PlugInFilterRunner pfr, String label) {}
		public void centerDialog(boolean b) {}
		public void enableYesNoCancel() {}
		public void enableYesNoCancel(String yesLabel, String noLabel) {}
		public Button[] getButtons() { return null; }
		public Vector getCheckboxes() { return null; }
		public Vector getChoices() { return null; }
		public String getErrorMessage() { return errorMessage; }
		public Insets getInsets() { return null; }
		public Component getMessage() { return null; }
		public Vector getNumericFields() { return null; }
		public Checkbox getPreviewCheckbox() { return null; }
		public Vector getSliders() { return null; }
		public Vector getStringFields() { return null; }
		public TextArea getTextArea1() { return null; }
		public TextArea getTextArea2() { return null; }
		public void hideCancelButton() {}
		public void previewRunning(boolean isRunning) {}
		public void setEchoChar(char echoChar) {}
		public void setHelpLabel(String label) {}
		public void setInsets(int top, int left, int bottom) {}
		public void setOKLabel(String label) {}
		protected void setup() {}
	}

	public static String toString(Object[] arguments) {
		return Arrays.toString(arguments);
	}

	public static void writeHeadlessJar(File path) throws IOException {
		Headless headless = new Headless();
		headless.run();
		headless.writeJar(path);
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		if (args.length == 0)
			args = new String[] { FijiTools.getFijiDir() + "/misc/headless.jar" };
		System.err.println("Writing " + args[0]);
		writeHeadlessJar(new File(args[0]));
	}
}