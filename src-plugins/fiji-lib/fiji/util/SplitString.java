package fiji.util;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitString {
	/**
	 * Split a command line into single parts, respecting quoted
	 * arguments.
	 *
	 * Example:
	 *
	 *   hello "this 'is'" 'an example'
	 *
	 * would be split into the three parts
	 *
	 *   hello
	 *   this 'is'
	 *   an example
	 *
	 * In other words, it respects quoting inside quoted arguments.
	 */
	public static List<String> splitCommandLine(String commandLine) throws ParseException {
		List<String> result = new ArrayList<String>();
		if (commandLine == null)
			return result;
		int len = commandLine.length();
		StringBuffer current = new StringBuffer();

		for (int i = 0; i < len; i++) {
			char c = commandLine.charAt(i);
			if (isQuote(c)) {
				int i2 = findClosingQuote(commandLine, c, i + 1, len);
				current.append(commandLine.substring(i + 1, i2));
				i = i2;
				continue;
			}
			if (Character.isWhitespace(c)) {
				if (current.length() == 0)
					continue;
				result.add(current.toString());
				current.setLength(0);
			} else
				current.append(c);
		}
		if (current.length() > 0)
			result.add(current.toString());
		return result;
	}

	protected static int findClosingQuote(String s, char quote, int index, int len) throws ParseException {
		for (int i = index; i < len; i++) {
			char c = s.charAt(i);
			if (c == quote)
				return i;
			if (isQuote(c))
				i = findClosingQuote(s, c, i + 1, len);
		}
		throw new ParseException("Unclosed quote: " + s, index);
	}

	protected static boolean isQuote(char c) {
		return c == '"' || c == '\'';
	}

	/**
	 * Split a macro-type option string into single parts, respecting brackets.
	 *
	 * Example:
	 *
	 *   path=[C:\Documents and Settings\ImageJ\Desktop\My Beautiful Image.jpg] radius=5
	 *
	 * would be split into the two parts
	 *
	 *   path -> C:\Documents and Settings\ImageJ\Desktop\My Beautiful Image.jpg
	 *   radius -> 5
	 *
	 * In other words, it splits by white space, however it keeps arguments intact that
	 * are enclosed in brackets.
	 */
	public static Map<String, String> splitMacroOptions(String options) throws ParseException {
		Map<String, String> result = new HashMap<String, String>();
		int len = options.length();
		StringBuffer current = new StringBuffer();

		for (int i = 0; i < len; i++) {
			char c = options.charAt(i);
			if (c == '[') {
				int i2 = i + 1;
				while (i2 < len && options.charAt(i2) != ']')
					i2++;
				current.append(options.substring(i + 1, i2));
				i = i2;
				continue;
			}
			if (Character.isWhitespace(c)) {
				if (current.length() == 0)
					continue;
				putPair(result, current.toString());
				current.setLength(0);
				while (i + 1 < len && Character.isWhitespace(options.charAt(i + 1)))
					i++;
			} else
				current.append(c);
		}
		if (current.length() > 0)
			putPair(result, current.toString());
		return result;
	}

	protected static void putPair(Map<String, String> map, String arg) throws ParseException {
		int equal = arg.indexOf('=');
		if (equal < 0)
			throw new ParseException("Missing '=': " + arg, 0);
		map.put(arg.substring(0, equal), arg.substring(equal + 1));
	}

	public static void main(String[] args) {
		if (args == null || args.length == 0 || (args.length == 1 && args[0].equals("")))
			args = new String[] { "path=[C:\\Documents and Settings\\ImageJ\\Desktop\\My Beautiful Image.jpg] radius=5" };
		for (String arg : args) try {
			Map<String, String> map = splitMacroOptions(arg);
			System.out.println("The string " + arg + " is split into:");
			for (String key : map.keySet())
				System.out.println("\t" + key + " -> " + map.get(key));
			System.out.println("");
		} catch (ParseException e) {
			System.err.println("There was a parse exception for " + arg + ": " + e.getMessage());
		}
	}
}
