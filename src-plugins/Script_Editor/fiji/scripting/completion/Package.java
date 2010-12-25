package fiji.scripting.completion;

import java.util.TreeSet;

public class Package extends TreeSet<Item> implements Item, Comparable<Item> {
	String key;

	public Package() {}

	public Package(String key) {
		this.key = key;
	}

	public String getName() {
		return key;
	}

	public int compareTo(Item tree) {
		return(getName().compareTo(tree.getName()));
	}
}