package fiji;

import java.io.File;

public class FijiTools {
	public static String getFijiDir() throws ClassNotFoundException {
		String path = System.getProperty("fiji.dir");
		if (path != null)
			return path;
		final String prefix = "file:";
		final String suffix = "/jars/Fiji.jar!/fiji/FijiTools.class";
		path = Class.forName("fiji.FijiTools")
			.getResource("FijiTools.class").getPath();
		if (path.startsWith(prefix))
			path = path.substring(prefix.length());
		if (path.endsWith(suffix))
			path = path.substring(0,
				path.length() - suffix.length());
		return path;
	}

	public static boolean isFijiDeveloper() {
		try {
			return new File(getFijiDir(), "fiji.c").exists();
		} catch (Exception e) {
			return false;
		}
	}
}
