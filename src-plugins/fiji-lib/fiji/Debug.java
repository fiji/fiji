package fiji;

import fiji.debugging.Object_Inspector;

public class Debug {
	public static void show(Object object) {
		Object_Inspector.openFrame("" + object, object);
	}

	public static Object get(Object object, String fieldName) {
		return Object_Inspector.get(object, fieldName);
	}
}