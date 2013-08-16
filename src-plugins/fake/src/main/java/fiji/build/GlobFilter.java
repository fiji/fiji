package fiji.build;

import java.io.File;
import java.io.FilenameFilter;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Pattern;

public class GlobFilter implements FilenameFilter {
	Pattern pattern;
	long newerThan;
	String glob;
	String lastMatch;

	GlobFilter(String glob) {
		this(glob, 0);
	}

	GlobFilter(String glob, long newerThan) {
		this.glob = glob;
		String regex = "^" + replaceSpecials(glob) + "$";
		pattern = Pattern.compile(regex);
		this.newerThan = newerThan;
	}

	String replaceSpecials(String glob) {
		StringBuffer result = new StringBuffer();
		char[] array = glob.toCharArray();
		int len = array.length;
		for (int i = 0; i < len; i++) {
			char c = array[i];
			if (".^$".indexOf(c) >= 0)
				result.append("\\" + c);
			else if (c == '?')
				result.append("[^/]");
			else if (c == '*') {
				if (i + 1 >= len || array[i + 1] != '*')
					result.append("[^/]*");
				else {
					result.append(".*");
					i++;
					if (i + 1 < len && array[i + 1]
							== '/')
						i++;
				}
			} else
				result.append(c);
		}
		return result.toString();
	}

	public boolean accept(File dir, String name) {
		if (newerThan > 0 && newerThan > new File(dir, name)
				.lastModified())
			return false;
		if (pattern.matcher(name).matches()) {
			lastMatch = name;
			return true;
		}
		lastMatch = null;
		return false;
	}

	boolean wildcardContainsStarStar;
	int firstWildcardIndex = -1, suffixLength;
	String wildcardPattern;

	private void initReplace() throws FakeException {
		if (firstWildcardIndex >= 0)
			return;
		wildcardContainsStarStar = glob.indexOf("**") >= 0;
		int first = glob.indexOf('*');
		int first2 = glob.indexOf('?');
		if (first < 0 && first2 < 0)
			throw new FakeException("Expected glob: "
				+ glob);
		int last = glob.lastIndexOf('*');
		int last2 = glob.lastIndexOf('?');
		firstWildcardIndex = first < 0 ||
			(first2 >= 0 && first > first2) ?
			first2 : first;
		int lastWildcardIndex = last < 0 || last < last2 ?
			last2 : last;
		wildcardPattern = glob.substring(firstWildcardIndex,
			lastWildcardIndex + 1);
		suffixLength = glob.length() - lastWildcardIndex - 1;
	}

	public String replace(String name) throws FakeException {
		initReplace();
		int index = name.indexOf(wildcardPattern);
		while (!wildcardContainsStarStar && index >= 0 &&
				name.substring(index).startsWith("**"))
			index = name.indexOf(wildcardPattern,
				name.substring(index).startsWith("**/*")
				? index + 4 : index + 2);
		if (index < 0)
			return name;
		return replace(name.substring(0, index)
			+ lastMatch.substring(firstWildcardIndex,
				lastMatch.length() - suffixLength)
			+ name.substring(index
				+ wildcardPattern.length()));
	}

	public List<String> replace(List<String> names) throws FakeException {
		List<String> result = new ArrayList<String>();
		for (String string : names)
			result.add(replace(string));
		return result;
	}
}