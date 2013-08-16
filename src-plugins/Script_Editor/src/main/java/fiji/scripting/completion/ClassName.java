package fiji.scripting.completion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.Set;
import java.util.TreeSet;

public class ClassName implements Item, Comparable<Item> {
	protected String key;
	protected Set<ClassMethod> methodNames = new TreeSet<ClassMethod>();

	public ClassName(String name) {
		key = name;
	}

	public String getName() {
		return key;
	}

	public int compareTo(Item tree) {
		return getName().compareTo(tree.getName());
	}

	public void setMethodNames(Method[] methods) {
		for (Method m : methods) {
			// toString() and not getName(), to get the complete, class-qualified name
			methodNames.add(new ClassMethod(m.toString()));
		}
	}
	public void setFieldNames(Field[] fields) {
		for (Field f : fields)
			methodNames.add(new ClassMethod(f.toString()));
	}
}