package fiji.scripting.completion;

import java.lang.Comparable;
import java.util.TreeSet;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClassName implements Item, Comparable {

	String key;
	String completeName;
	TreeSet<ClassMethod> methodNames = new TreeSet<ClassMethod>();


	public ClassName() {
	}

	public ClassName(String paramkey, String name) {
		key = paramkey;
		completeName = name;
	}

	public String getName() {
		return this.key;
	}

	public String getCompleteName() {
		return this.completeName.replace('/', '.');
	}

	public int compareTo(Object o) {
		Item tree = (Item)o;
		return(this.getName().compareTo(tree.getName()));
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

