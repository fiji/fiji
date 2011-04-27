package results;

/**
 * A small container to name objects by overriding
 * the toString method.
 *
 */
public class NamedContainer<T> {
	T object;
	String name;

	public NamedContainer(T object, String name) {
		this.object = object;
		this.name = name;
	}

	public T getObject() {
		return object;
	}

	public String toString() {
		return name;
	}
}
