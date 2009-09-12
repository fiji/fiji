package fiji.scripting.completion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.fife.ui.autocomplete.FunctionCompletion;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

// TODO: rename to ObjectInstances
public class ObjStartCompletions {
	ArrayList<String> packageNames = new ArrayList<String>();
	TreeSet<ImportedClassObjects> objectSet = new TreeSet<ImportedClassObjects>();
	ClassNames names;
	// TODO: why not parser?
	ConstructorParser parser;
	String language;

	public ObjStartCompletions(ClassNames name, String lang, ArrayList pNames) {
		names = name;
		language = lang;
		this.packageNames = pNames;
		parser = new ConstructorParser(name, lang);
	}

	// TODO: RSyntaxTextArea or RSyntaxDocument?
	// TODO: lazy evaluation?
	public void setObjects(RSyntaxTextArea textArea, String text, DefaultProvider defaultProvider) {

		//System.out.println("The first package element "+packageNames.get(0));
		parser.setPackageNames(packageNames);
		parser.findConstructorObjects(textArea);
		objectSet = parser.getObjectSet();
		boolean dotAtLast = false;
		if (text.charAt(text.length() - 1) == '.') {
			dotAtLast = true;
		}
		String subtext = text.substring(text.indexOf('.') + 1);

		// TODO: avoid expensive re-calculations
		if (text.lastIndexOf(".") == text.indexOf(".") && text.indexOf(".") > 0) {
			String objname = text.substring(0, text.indexOf("."));
			// TODO: why the cast?
			TreeSet<ImportedClassObjects> set = (TreeSet<ImportedClassObjects>)objectSet.tailSet(new ImportedClassObjects(objname, ""));
			TreeSet<ClassMethod> methodSet = new TreeSet<ClassMethod>();
			TreeSet<ClassField> fieldSet = new TreeSet<ClassField>();
			for (ImportedClassObjects object : set) {
				if (object.name.equals(objname)) {
					String fullname = object.getCompleteClassName();
					try {
						Class clazz = getClass().getClassLoader().loadClass(fullname);
						for (Method m : clazz.getMethods()) {
							String fullMethodName = m.toString();
							methodSet.add(new ClassMethod(fullMethodName));
						}
						for (Field f : clazz.getFields()) {
							String fullFieldName = f.toString(); // something line "public boolean ij.ImagePlus.changes"
							fieldSet.add(new ClassField(fullFieldName));
						}
					} catch (ClassNotFoundException cnfe) {
						System.out.println("Class not found: " + fullname);
					} catch (java.lang.Error e) {
						System.out.println("An error ocurred for " + fullname);
						e.printStackTrace();
					}
					ArrayList listOfCompletions = new ArrayList();
					if (!dotAtLast) {
						methodSet = (TreeSet<ClassMethod>)methodSet.tailSet(new ClassMethod(text.substring(text.indexOf('.') + 1), true));
					}
					for (ClassMethod method : methodSet) {
						if ((!dotAtLast) && (!method.onlyName.startsWith(subtext)))
							break;
						if ((!method.isStatic) && method.isPublic) {
							listOfCompletions.add(new FunctionCompletion(defaultProvider, method.onlyName, method.returnType));    //currently basiccompletion can be changed to functioncompletion
						}
					}
					if (!dotAtLast) {
						fieldSet = (TreeSet<ClassField>)fieldSet.tailSet(new ClassField(text.substring(text.indexOf('.') + 1), true));
					}
					for (ClassField field : fieldSet) {
						if ((!dotAtLast) && (!field.onlyName.startsWith(subtext)))
							break;
						if ((!field.isStatic) && field.isPublic) {
							listOfCompletions.add(new FunctionCompletion(defaultProvider, field.onlyName, ""));
						}
					}
					defaultProvider.addCompletions(listOfCompletions);
				} else
					break;
			}
		}
	}

}
