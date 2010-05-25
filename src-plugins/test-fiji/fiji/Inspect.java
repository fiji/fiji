package fiji;

/*
 * This is a helper class for debuggin, so setAccessible(true) is a necessity
 * rather than an ugliness here.
 */

import ij.IJ;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Inspect {
	public static Object get(Object object, String field) {
		Class clazz = object.getClass();
		try {
			Field f = clazz.getDeclaredField(field);
			f.setAccessible(true);
			return f.get(object);
		} catch (NoSuchFieldException e) {
			IJ.log("Class " + clazz.getName()
				+ " has no field called " + field
				+ ". Available fields:");
			for (Field f : clazz.getDeclaredFields())
				IJ.log(" " + f.getName());
		} catch (IllegalAccessException e) {
			IJ.log("Failed to make " + field
				+ " accessible");
		}
		return null;
	}

	public static void set(Object object, String field, Object value) {
		Class clazz = object.getClass();
		try {
			Field f = clazz.getDeclaredField(field);
			f.setAccessible(true);
			f.set(object, value);
			return;
		} catch (NoSuchFieldException e) {
			IJ.log("Class " + clazz.getName()
				+ " has no field called " + field
				+ ". Available fields:");
			for (Field f : clazz.getDeclaredFields())
				IJ.log(" " + f.getName());
		} catch (IllegalAccessException e) {
			IJ.log("Failed to make " + field
				+ " accessible");
		}
	}
}
