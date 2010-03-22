package fiji.util;

/**
 * Use this class to identify .jar files corresponding to given
 * menu items.
 *
 * For convenience, there is a function which you can pass a (long) string to
 * it that possibly ends in a menu entry's label.
 */

import ij.IJ;
import ij.Menus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuItemHelper {
	Map<String, List<String>> lastWord2Label;

	public MenuItemHelper() {
		populateMap();
	}

	protected void populateMap() {
		lastWord2Label = new HashMap<String, List<String>>();
		for (Object label : Menus.getCommands().keySet()) {
			String lastWord = getLastWord((String)label);
			List list = lastWord2Label.get(lastWord);
			if (list == null) {
				list = new ArrayList<String>();
				lastWord2Label.put(lastWord, list);
			}
			list.add(label);
		}
	}

	protected String getLastWord(String text) {
		int end = text.length();
		while (end > 0 && !Character.isLetterOrDigit(text.charAt(end - 1)))
			end--;
		if (end == 0)
			return null;
		int start = end - 1;
		while (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1)))
			start--;
		return text.substring(start, end);
	}

	public String getJarForItem(String label) {
		String className = (String)Menus.getCommands().get(label);
		int paren = className.indexOf('(');
		if (paren > 0)
			className = className.substring(0, paren);
		try {
			Class clazz = IJ.getClassLoader().loadClass(className);
			int dot = className.lastIndexOf('.');
			String fileName = className.substring(dot + 1) + ".class";
			String resource = clazz.getResource(fileName).toString();
			String path = className.substring(0, dot + 1).replace('.', '/') + fileName;
			if (resource.startsWith("jar:") &&
					resource.endsWith("!/" + path))
				resource = resource.substring(4, resource.length() - path.length() - 2);
			if (resource.startsWith("file:"))
				resource = resource.substring(5);
			String fijiDir = System.getProperty("fiji.dir");
			if (!fijiDir.endsWith("/"))
				fijiDir += "/";
			if (resource.startsWith(fijiDir))
				resource = resource.substring(fijiDir.length());
			return resource;
		} catch (Exception e) {
			IJ.handleException(e);
			return null;
		}
	}

	public String getJarForSuffix(String text) {
		String lastWord = getLastWord(text);
		List<String> list = lastWord2Label.get(lastWord);
		if (list == null)
			return null;
		for (String label : list) {
			int pos2 = label.lastIndexOf(lastWord);
			if (pos2 == 0)
				return getJarForItem(label);
			int pos1 = text.lastIndexOf(lastWord);
			if (pos1 >= pos2 && text.substring(pos1 - pos2, pos1).equals(label.substring(0, pos2)))
				return getJarForItem(label);
		}
		return null;
	}

	public static void main(String[] args) {
		MenuItemHelper helper = new MenuItemHelper();
		IJ.log("1: " + helper.getJarForSuffix("Hello Open..."));
		IJ.log("2: " + helper.getJarForSuffix("Hello Openn..."));
		IJ.log("3: " + helper.getJarForSuffix(" . ! ..."));
	}
}