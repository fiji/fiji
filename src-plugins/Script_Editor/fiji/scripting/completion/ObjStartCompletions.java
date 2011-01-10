package fiji.scripting.completion;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.fife.ui.autocomplete.FunctionCompletion;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

// TODO: rename to ObjectInstances
public class ObjStartCompletions {
	protected List<String> packageNames;
	protected TreeSet<ImportedClassObjects> objectSet = new TreeSet<ImportedClassObjects>();
	protected ClassNames names;
	protected ConstructorParser parser;
	protected String language;

	public ObjStartCompletions(ClassNames names, String language) {
		this.names = names;
		this.language = language;
		packageNames = new ArrayList<String>(names.getPackageNames());
		parser = new ConstructorParser(names, language);
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
		// TODO: avoid expensive re-calculations
		if (text.lastIndexOf(".") == text.indexOf(".") && text.indexOf(".") > 0) {
			String objname = text.substring(0, text.indexOf("."));
			// TODO: why the cast?
			TreeSet<ImportedClassObjects> set = (TreeSet<ImportedClassObjects>)objectSet.tailSet(new ImportedClassObjects(objname, ""));
			TreeSet<ClassMethod> methodSet = new TreeSet<ClassMethod>();
			for (ImportedClassObjects object : set) {
				if (object.name.equals(objname)) {
					String fullname = object.getCompleteClassName();
					try {
						try {
							Class clazz = getClass().getClassLoader().loadClass(fullname);
							Method[] methods = clazz.getMethods();
							for (Method m : methods) {
								String fullMethodName = m.toString();
								methodSet.add(new ClassMethod(fullMethodName));
							}
						} catch (java.lang.Error e) {
							e.printStackTrace();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					ArrayList listOfCompletions = new ArrayList();
					if (!dotAtLast) {
						methodSet = (TreeSet<ClassMethod>)methodSet.tailSet(new ClassMethod(text.substring(text.indexOf(".") + 1), true));
					}
					for (ClassMethod method : methodSet) {
						if ((!dotAtLast) && (!method.onlyName.startsWith(text.substring(text.indexOf(".") + 1))))
							break;
						if ((!method.isStatic) && method.isPublic) {
							listOfCompletions.add(new FunctionCompletion(defaultProvider, method.onlyName, method.returnType));    //currently basiccompletion can be changed to functioncompletion
						}
					}
					defaultProvider.addCompletions(listOfCompletions);
				} else
					break;
			}
		}
	}

}
