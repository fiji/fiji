package fiji.scripting.completion;

import org.fife.ui.autocomplete.BasicCompletion;

public class KeywordsCompletion {

	static int startIndex = -1;
	static int endIndex = -2;

	public static final String[] javaKeywords = {"abstract", "assert", "break", "case", "catch", "class", "const", "continue", "default", "do", "else", "enum",
	                "extends", "final", "finally", "for", "goto", "if", "implements", "import", "instanceof", "native", "new",
	                "null", "package", "private", "protected", "public", "return", "static", "strictfp", "super", "switch",
	                "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
	                                            };

	public static final String[] rubyKeywords = {"alias", "BEGIN", "begin", "case", "class", "def", "do", "else", "elsif", "END", "end", "ensure", "for", "if", "in",
	                "module", "next", "nil", "redo", "rescue", "retry", "return", "self", "super", "then", "undef", "unless", "until",
	                "when", "while", "yield"
	                                            };

	public static final String[] pythonKeywords = {"and", "as", "assert", "break", "class", "continue", "def", "del", "elif", "else", "except", "exec", "finally",
	                "for", "from", "global", "if", "import", "in", "is", "lambda", "not", "or", "pass", "print", "raise", "return",
	                "try", "while", "yield"
	                                              };

	public static final String[] matlabKeywords = {"break", "case", "catch", "continue", "else", "elseif", "end", "for", "function", "global", "if", "otherwise",
	                "persistence", "return", "switch", "try", "while"
	                                              };

	// TODO this list is EXTREMELY incomplete
	public static final String[] clojureKeywords = {"class", "cond", "def", "defmacro", "defn", "do", "fn", "if", "instance?", "let", "loop", "monitor-enter", "monitor-exit", "new",
	                "recur", "set!", "the-var", "thisfn", "throw", "try-finally"
	                                               };


	public static void completeKeywords(DefaultProvider provider, String language, String text) {
		if (text == "" || text == null)
			return;
		if (language.equals("Java") || language.equals("Javascript") || language.equals("BeanShell")) {
			calculateIndex(javaKeywords, text);
			addCompletions(provider, javaKeywords);
		}

		else if (language.equals("Ruby")) {
			calculateIndex(rubyKeywords, text);
			addCompletions(provider, javaKeywords);
		}

		else if (language.equals("Python")) {
			calculateIndex(pythonKeywords, text);
			addCompletions(provider, pythonKeywords);
		}

		else if (language.equals("Matlab")) {
			calculateIndex(matlabKeywords, text);
			addCompletions(provider, matlabKeywords);
		}

		else if (language.equals("Clojure")) {
			calculateIndex(clojureKeywords, text);
			addCompletions(provider, clojureKeywords);
		}

	}

	public static void calculateIndex(String[] array, String text) {

		startIndex = -1;
		endIndex = -2;
		int min = 0;
		int max = array.length - 1;
		int mid = 0;
		while (true) {
			mid = (min + max) / 2;
			if (array[mid].compareTo(text) < 0)
				min = mid + 1;
			else
				max = mid - 1;
			if (array[mid].startsWith(text) || min > max)
				break;
		}
		if (array[mid].startsWith(text)) {
			startIndex = mid;
			endIndex = mid;
			try {
				while (true) {
					if (array[startIndex-1].startsWith(text))
						startIndex--;
					else
						break;
				}
				while (true) {
					if (array[endIndex+1].startsWith(text))
						endIndex++;
					else
						break;
				}
			} catch (ArrayIndexOutOfBoundsException e) {}
		}

	}

	public static void addCompletions(DefaultProvider provider, String[] array) {

		for (int i = startIndex; i <= endIndex; i++) {
			provider.addCompletion(new BasicCompletion(provider, array[i]));
		}
	}

}




