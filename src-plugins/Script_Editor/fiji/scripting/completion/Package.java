package fiji.scripting.completion;

/**
 * A container for class names
 *
 * The key is the package name, the contents are the class names without the package name.
 */

import java.util.TreeSet;

public class Package extends TreeSet<Item> implements Item, Comparable<Item> {
	String key;

	public Package(String key) {
		this.key = key;
	}

	public String getName() {
		return key;
	}

	public int compareTo(Item tree) {
		return(getName().compareTo(tree.getName()));
	}

	public boolean contains(String name) {
		return contains(new ClassName(name));
	}
}