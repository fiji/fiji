package fiji.scripting.completion;

import java.awt.List;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
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

/****This class generates and prints the
list of trees having each part of the classnames path
as a node of the tree like for java.awt.List, the top level List
contains a Tree object with key "java" and one of its childList
element as awt which in turn has its childDList having its one childList
as Listwhich is infact also a leaf ***********/

public class ClassNames {

	static List list = new List();

	static Package root = new Package();
	DefaultProvider defaultProvider;
	Enumeration list1;
	Package toReturnClassPart = new Package();
	ImportStatementsParser importStatementsParser = new ImportStatementsParser();
	ObjStartCompletions obj;
	ArrayList<String> packageNames = new ArrayList<String>();
	String[] dotSeparatedTextParts = new String[10];

	public ClassNames(DefaultProvider provider) {
		defaultProvider = provider;
	}

	public void run(String[] args) {
		for (String arg : args)
			setPathTree(arg);
	}

	public Package getRoot() {
		return root;
	}

	public void setPathTree(String path) {
		File file = new File(path);
		if (file.isDirectory())
			setDirTree(path);
		else if (path.endsWith(".jar")) try {
				ZipFile jarFile = new ZipFile(file);
				list1 = jarFile.entries();
				while (list1.hasMoreElements()) {
					ZipEntry entry = (ZipEntry)list1.nextElement();
					String name = entry.getName();
					if (!name.endsWith(".class"))		//ignoring the non class files
						continue;
					addToTree(splitZipEntryForClassName(name), root, 0);
				}
			} catch (Exception e) {
				if (path.endsWith("/sunrsasign.jar") || path.endsWith("/jsfd.jar"))
					return;
				System.err.println("Exception while processing "
					+ path);
				e.printStackTrace();
			}
	}

	private String splitZipEntryForClassName(String name) {
		String[] classname1 = name.split("\\\\");
		return(classname1[classname1.length-1]);
	}

	public void setDirTree(String path) {
		File file = new File(path);
		if (file.isDirectory()) {
			if (!path.endsWith(File.separator))
				path += File.separator;
			String[] list = file.list();
			for (int i = 0; i < list.length; i++)
				setDirTree(path + list[i]);	//recursively adding the classnames to the list
		} else if ((path.endsWith(".class"))) {
			String[] classname1 = path.split("\\\\");
			String justClassName = classname1[classname1.length-1];
			addToTree(justClassName, root, 0);
		}
	}

	public void addToTree(String fullName, Package toAdd, int i) {
		String name = new String(fullName);                       //No splitting now
		if (fullName.endsWith(".class")) {
			for (;;) {
				int slash = name.indexOf("/");
				if (slash < 0)
					break;
				Package item = new Package(name.substring(0, slash) + ".");
				toAdd.add(item);
				toAdd = (Package)toAdd.tailSet(item).first();
				name = name.substring(slash + 1);
			}
			Item item = new ClassName(name.substring(0, name.length() - 6), fullName.substring(0, fullName.length() - 6));
			toAdd.add(item);
		}
	}


	private boolean isDotInEnteredText(String text) {
		return text.lastIndexOf('.') > 0;
	}

	private ArrayList<String> getPackageNamesImported(RSyntaxDocument document, String language) {
		importStatementsParser.objCompletionPackages(document, language);
		return(importStatementsParser.getPackageNames());
	}

	private void setObjectNameCompletions(String language, String text, RSyntaxTextArea textArea) {
		obj = new ObjStartCompletions(this, language, packageNames);
		obj.setObjects(textArea, text, defaultProvider);
	}

	private Package getImportedClassCompletions(Package root,String text) {
		Package importedClassSet = findImportedClassSet(root);
		return(findPrefixedSet(importedClassSet, text));
	}

	public void setClassCompletions(Package root, RSyntaxTextArea textArea, String language) {
		String text = defaultProvider.getEnteredText(textArea);
		if (!(0 == text.length() || null == text)) {
			packageNames = getPackageNamesImported((RSyntaxDocument)textArea.getDocument(), language);
			if (!(isDotInEnteredText(text))) {
				Package completionSet = findPrefixedSet(root, text);
				completionSet.addAll(getImportedClassCompletions(root, text));
				defaultProvider.addCompletions(createListCompletions(completionSet));
			}

			else {
				dotSeparatedTextParts = text.split("\\.");
				classStartCompletions(text, dotSeparatedTextParts);
				setObjectNameCompletions(language, text, textArea);
				boolean isClassBeforeDot = false;
				boolean isPackageBeforeDot = false;
				int index = dotSeparatedTextParts.length;
				Package tempPackage = root;
				int tempIndex = index;
				boolean isSomethingToComplete = true;
				while (tempIndex > 1) {
					Item itemBeforeDot = firstMatchingClassForTextPart(tempPackage, index-tempIndex);
					if (itemBeforeDot instanceof ClassName) {

						if (itemBeforeDot.getName().equals(dotSeparatedTextParts[index-tempIndex])) {
							isClassBeforeDot = true;
						} else {
							isSomethingToComplete = false;
						}
						break;                                                    //here I am assuming only one dot after a className that is why breaking the loop irrespective of isSomethingToComplete
					}
					itemBeforeDot = firstMatchingPackageForTextPart(tempPackage, index-tempIndex);
					if (itemBeforeDot instanceof Package) {
						if (!(itemBeforeDot.getName().equals(dotSeparatedTextParts[index-tempIndex] + "."))) {//looks if topLevel contains the first part of the package part
							isSomethingToComplete = false;
							break;
						} else {
							tempPackage = (Package)findTailSet(tempPackage, dotSeparatedTextParts[index-tempIndex] + ".").first();
						}
					}
					tempIndex--;
				}
				if (isSomethingToComplete) {
					findAndAddCompletions(text, index, tempIndex, tempPackage, isClassBeforeDot);
				}
			}
		}
	}

	public void findAndAddCompletions(String text, int index, int tempIndex, Package tempPackage, boolean isClassBeforeDot) {
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
					loadMethodAndFieldNames((ClassName)itemForSecondLastPart);
					defaultProvider.addCompletions(createFunctionCompletion(((ClassName)itemForSecondLastPart).methodNames, true));
				}
			} else {
				tempPackage = (Package)itemForSecondLastPart;
				defaultProvider.addCompletions(createListCompletions(tempPackage));
			}
		}
	}

	private void doClassStartCompletions(Package tempPackage, int index1) {
		ClassName name = (ClassName)firstMatchingClassForTextPart(tempPackage, index1);
		loadMethodAndFieldNames(name);
		generateClassRelatedCompletions(name, dotSeparatedTextParts);
	}

	private boolean isDotAtLast(String text) {
		return(text.charAt(text.length() - 1) == '.');
	}

	private Item firstMatchingPackageForTextPart(Package package1, int index) {
		return(findTailSet(package1, dotSeparatedTextParts[index] + ".").first());
	}

	private Item firstMatchingClassForTextPart(Package package1, int index) {
		return(findTailSet(package1, dotSeparatedTextParts[index]).first());
	}

	public void classStartCompletions(String text,String[] classParts) {


		if ((classParts.length > 1 && isDotAtLast(text)) || (classParts.length > 2 && !isDotAtLast(text)))
			return;
		Package classItemBeforeDot = findPrefixedSet(findImportedClassSet(root), classParts[0]);
		if (classItemBeforeDot.size() > 0) {
			if (classItemBeforeDot.first().getName().equals(classParts[0])) {
				ClassName className = (ClassName)classItemBeforeDot.first();
				loadMethodAndFieldNames(className);

				if (isDotAtLast(text)) {
					defaultProvider.addCompletions(createFunctionCompletion(className.methodNames, true));
				} else {
					generateClassRelatedCompletions(className, classParts);
				}
			}
		}
	}


	public void loadMethodAndFieldNames(ClassName tempPackage) {
		if (tempPackage.methodNames.size() <= 0) {
			String fullName = tempPackage.getCompleteName();
			try {
				Class clazz = getClass().getClassLoader().loadClass(fullName);
				tempPackage.setMethodNames(clazz.getMethods());
				tempPackage.setFieldNames(clazz.getFields());
			} catch (ClassNotFoundException cnfe) {
				System.out.println("Class not found: " + fullName);
			} catch (Exception e) {
				System.out.println("An error ocurred for " + fullName);
				e.printStackTrace();
			}
		}
	}

	Package findTailSet(Package parent, String text) {
		Package item = new Package(text);
		Package tail = new Package();
		for (Item i : parent.tailSet(item)) {
			tail.add(i);
		}
		return tail;
	}

	private Package findHeadSet(Package parent, String text) {
		Package item = new Package(text);
		Package tail = new Package();
		for (Item i : parent) {
			if(i.getName().startsWith(text)) {
				tail.add(i);
			}
			else {
				break;
			}
		}
		return tail;
	}

	private Package findPrefixedSet(Package parent, String text) {               //it returns the set whose elements name are prefixed by text
		Item item = new Package();
		Package tail = findTailSet(parent, text);
		try {
			if (tail.last().getName().startsWith(text)) {
				return(tail);
			} else {
				return(findHeadSet(tail, text));
			}
		} catch (Exception e) {
			return tail;
		}
	}

	private Package findImportedClassSet(Package root) {
		toReturnClassPart.clear();
		for (String s : packageNames) {
			String[] parts = s.split("\\.");
			Item current = findPackage(parts, root);                                 //to create this function
			if (current instanceof Package) {
				try {
					for (Item i: (Package)current) {
						if (i instanceof ClassName) {
							toReturnClassPart.add(i);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (current instanceof ClassName) {
				toReturnClassPart.add(current);
			}

		}
		return toReturnClassPart;
	}



	public Item findPackage(String[] splitPart, Package p) {
		for (int i = 0; i < splitPart.length - 1; i++) {
			p = (Package)findTailSet(p, splitPart[i]).first();
		}
		if (splitPart[splitPart.length-1].equals("*")) {
			return (Item)p;
		} else {
			return findTailSet(p, splitPart[splitPart.length-1]).first();
		}
	}

	public ArrayList createListCompletions(Package setOfCompletions) {
		ArrayList listOfCompletions = new ArrayList();

		for (Item i : setOfCompletions) {
			try {
				try {
					if (i instanceof ClassName) {

						String fullName = ((ClassName)i).getCompleteName();
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

	public String createConstructorCompletion(String cotr) {

		String[] bracketSeparated = cotr.split("\\(");
		int lastDotBeforeBracket = bracketSeparated[0].lastIndexOf(".");
		return(cotr.substring(lastDotBeforeBracket + 1));

	}

	public ArrayList createFunctionCompletion(TreeSet<ClassMethod> setOfCompletions, boolean isStatic) {
		ArrayList listOfCompletions = new ArrayList();
		for (ClassMethod method : setOfCompletions) {
			if (method.isStatic && method.isPublic) {
				listOfCompletions.add(new FunctionCompletion(defaultProvider, method.onlyName, method.returnType));    //currently basiccompletion can be changed to functioncompletion
			}
		}

		return listOfCompletions;
	}

	public void generateClassRelatedCompletions(ClassName className, String[] parts) {

		TreeSet<ClassMethod> set = (TreeSet<ClassMethod>)className.methodNames.tailSet(new ClassMethod(parts[parts.length-1], true));
		for (ClassMethod c : set) {

			if (!c.onlyName.startsWith(parts[parts.length-1])) {
				break;
			} else {
				if (c.isStatic && c.isPublic)
					defaultProvider.addCompletion(new FunctionCompletion(defaultProvider, c.onlyName, c.returnType));
			}
		}

	}
}
