package fiji.scripting.completion;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * A parser for the complete classpath
 * list of trees having each part of the classnames path
 * as a node of the tree like for java.awt.List, the top level List
 * contains a Tree object with key "java" and one of its childList
 * element as awt which in turn has its childDList having its one childList
 * as Listwhich is infact also a leaf
 */

public class ClassNames {
	protected static Map<String, Package> packages;
	protected DefaultProvider defaultProvider;
	protected ImportStatementsParser importStatementsParser = new ImportStatementsParser();
	protected ObjStartCompletions obj;

	public ClassNames(DefaultProvider provider) {
		defaultProvider = provider;
		if (packages == null) {
			packages = new TreeMap<String, Package>();
			addPaths(System.getProperty("java.class.path").split(File.pathSeparator));
			addPaths(System.getProperty("sun.boot.class.path").split(File.pathSeparator));
		}
	}

	protected void addPaths(String[] args) {
		for (String arg : args)
			addPath(arg);
	}

	protected void addPath(String path) {
		File file = new File(path);
		if (file.isDirectory())
			addDirectory(file, "");
		else if (path.endsWith(".jar") && file.length() > 0) try {
				ZipFile jarFile = new ZipFile(file);
				Enumeration e = jarFile.entries();
				Package pkg = null;
				while (e.hasMoreElements()) {
					ZipEntry entry = (ZipEntry)e.nextElement();
					String name = entry.getName();
					if (name.endsWith(".class")) //ignore non-class files
						pkg = addClassName(pkg, stripClassSuffix(name).replace('/', '.'));
				}
			} catch (Exception e) {
				if (path.endsWith("/sunrsasign.jar") || path.endsWith("/jsfd.jar"))
					return;
				System.err.println("Exception while processing " + path);
				e.printStackTrace();
			}
	}

	protected void addDirectory(File file, String packageName) {
		Package pkg = getPackage(packageName);
		for (String name : file.list())
			if (name.endsWith(".class"))
				pkg.add(new ClassName(stripClassSuffix(name)));
			else if (name.indexOf('.') < 0) {
				File dir = new File(file, name);
				if (dir.isDirectory())
					addDirectory(dir, packageName.equals("") ? name : packageName + "." + name);
			}
	}

	protected final String stripClassSuffix(String name) {
		return name.substring(0, name.length() - 6);
	}

	protected Package addClassName(final Package previousPackage, String className) {
		int dot = className.lastIndexOf('.');
		String pkgName = dot < 0 ? "" : className.substring(0, dot);
		Package pkg = getPackage(previousPackage, pkgName);
		pkg.add(new ClassName(className.substring(dot + 1)));
		return pkg;
	}

	protected Package getPackage(Package previous, String name) {
		return previous != null && previous.getName().equals(name) ? previous : getPackage(name);
	}

	protected Package getPackage(String name) {
		Package pkg = packages.get(name);
		if (pkg == null) {
			pkg = new Package(name);
			packages.put(name, pkg);
		}
		return pkg;
	}

	private ArrayList<String> getPackageNamesImported(RSyntaxDocument document, String language) {
		importStatementsParser.objCompletionPackages(document, language);
		return(importStatementsParser.getPackageNames());
	}

	private void setObjectNameCompletions(String language, String text, RSyntaxTextArea textArea) {
		obj = new ObjStartCompletions(this, language);
		obj.setObjects(textArea, text, defaultProvider);
	}

	/* TODO: fix
	private Package getImportedClassCompletions(RSyntaxTextArea texArea) {
		Package importedClassSet = findImportedClassSet(root);
		return(findPrefixedSet(importedClassSet, text));
	}
	*/

	protected void setClassCompletions(RSyntaxTextArea textArea, String language) {
		// TODO
	}

	protected void findAndAddCompletions(String text, int index, int tempIndex, Package tempPackage, boolean isClassBeforeDot) {
		/*
		if (!isDotAtLast(text)) {
			if (isClassBeforeDot) {
				doClassStartCompletions(tempPackage, index-tempIndex);
			} else {
				tempPackage = findPrefixedSet(tempPackage, dotSeparatedTextParts[index-1]);
				defaultProvider.addCompletions(createListCompletions(tempPackage));
			}
		} else  {
			Item itemForSecondLastPart = firstMatchingClassForTextPart(tempPackage, index-tempIndex);
			if (itemForSecondLastPart instanceof ClassName) {
				if (itemForSecondLastPart.getName().equals(dotSeparatedTextParts[index-tempIndex])) {
					loadMethodNames((ClassName)itemForSecondLastPart);
					defaultProvider.addCompletions(createFunctionCompletion(((ClassName)itemForSecondLastPart).methodNames, true));
				}
			} else {
				tempPackage = (Package)itemForSecondLastPart;
				defaultProvider.addCompletions(createListCompletions(tempPackage));
			}
		}
		*/
	}

	public String getFullName(String pkgName, String className) {
		return (pkgName.equals("") ? "" : pkgName + ".") + className;
	}

	protected void loadMethodNames(String pkgName, ClassName clazz) {
		if (clazz.methodNames.size() > 0)
			return;
		String fullname = getFullName(pkgName, clazz.getName());
		try {
			Class clazz2 = getClass().getClassLoader().loadClass(fullname);
			clazz.setMethodNames(clazz2.getMethods());
			clazz.setFieldNames(clazz2.getFields());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected ArrayList createListCompletions(Package setOfCompletions) {
		ArrayList listOfCompletions = new ArrayList();

		for (Item i : setOfCompletions) {
			try {
				try {
					if (i instanceof ClassName) {
						String fullName = getFullName(setOfCompletions.getName(), ((ClassName)i).getName());
						Class clazz = getClass().getClassLoader().loadClass(fullName);
						Constructor[] ctor = clazz.getConstructors();

						for (Constructor c : ctor) {
							String cotrCompletion = createConstructorCompletion(c.toString());
							listOfCompletions.add(new BasicCompletion(defaultProvider, cotrCompletion));
						}
						listOfCompletions.add(new BasicCompletion(defaultProvider, i.getName() + "."));
					}
				} catch (NoClassDefFoundError e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			listOfCompletions.add(new BasicCompletion(defaultProvider, i.getName()));

		}
		return listOfCompletions;
	}

	protected String createConstructorCompletion(String cotr) {
		String[] bracketSeparated = cotr.split("\\(");
		int lastDotBeforeBracket = bracketSeparated[0].lastIndexOf(".");
		return(cotr.substring(lastDotBeforeBracket + 1));

	}

	protected ArrayList createFunctionCompletion(TreeSet<ClassMethod> setOfCompletions, boolean isStatic) {
		ArrayList listOfCompletions = new ArrayList();
		for (ClassMethod method : setOfCompletions) {
			if (method.isStatic && method.isPublic) {
				listOfCompletions.add(new FunctionCompletion(defaultProvider, method.onlyName, method.returnType));    //currently basiccompletion can be changed to functioncompletion
			}
		}

		return listOfCompletions;
	}

	public String isClassPresent(String name) {
		for (String pkgName : packages.keySet())
			if (packages.get(pkgName).contains(name))
				return getFullName(pkgName, name);
		return "";
	}

	public Collection<String> getPackageNames() {
		return packages.keySet();
	}

	public List<String> getFullPackageNames(String className) {
		List<String> result = new ArrayList<String>();
		for (String pkgName : packages.keySet())
			if (packages.get(pkgName).contains(className))
				result.add(pkgName);
		return result;
	}

	public List<String> getFullClassNames(String className) {
		List<String> result = new ArrayList<String>();
		for (String pkgName : packages.keySet())
			if (packages.get(pkgName).contains(className))
				result.add(getFullName(pkgName, className));
		return result;
	}
}