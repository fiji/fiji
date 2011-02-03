package fiji.scripting.completion;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.text.Element;

public class ConstructorParser {
	protected TreeSet<ImportedClassObjects> objectSet = new TreeSet<ImportedClassObjects>();
	protected ClassNames names;
	protected Collection<String> packageNames = new ArrayList<String>();
	protected RSyntaxDocument doc;
	protected String language;

	public ConstructorParser(ClassNames name, String lang) {
		names = name;
		language = lang;
	}

	public TreeSet<ImportedClassObjects> getObjectSet() {
		return objectSet;
	}

	public void findConstructorObjects(RSyntaxTextArea textArea) {

		doc = (RSyntaxDocument)textArea.getDocument();
		Element map = doc.getDefaultRootElement();
		int startLine = map.getElementIndex(0);
		int endLine = map.getElementIndex(doc.getLength());
		for (int i = startLine; i <= endLine; i++) {

			Token token = doc.getTokenListForLine(i);
			Token prev = null;
			Token prevToPrev = null;

			for (; token != null && token.type != 0; token = token.getNextToken()) {
				System.out.println(token.getLexeme());
				if (token.type == 16 || token.type == 3 || token.type == 2 || token.type == 1) {      //for white space and comments
					continue;
				}

				if (language.equals("Java") || language.equals("Javascript") || language.equals("Clojure")) {
					if (token.getLexeme().equals("new")) {
						setObjectSetIfImported(prevToPrev, prev, getNextNonWhitespaceToken(token));
						setExplicitObjectSet("JJC", i);
					}
				}

				else if (language.equals("Python") || language.equals("Matlab")) {
					if (token.getNextToken().type != 0) {
						if (token.getNextToken().getLexeme().equals("(")) {
							setObjectSetIfImported(prevToPrev, prev, token);
							setExplicitObjectSet("PM", i);
						}
					}
				}

				else if (language.equals("Ruby")) {
					if (token.getNextToken().getNextToken().type != 0) {
						if (token.getNextToken().getNextToken().getLexeme().equals("new")) {
							setObjectSetIfImported(prevToPrev, prev, token);
							setExplicitObjectSet("R", i);
						}
					} else
						break;
				}

				else if (!token.isWhitespace()) {
					prevToPrev = prev;
					prev = token;
				}
			}
		}
	}

	// TODO: langs is not intuitive at all.  If there is a feature set,
	// we need to use booleans, maybe wrapped into a class "LangFeatures"
	public void setExplicitObjectSet(String langs, int line) {
		Token token = doc.getTokenListForLine(line);
		boolean found = false;
		Token prev = null;
		Token prevToPrev = null;
		String name = "";
		String fullClassName = "";
		for (; token != null && token.type != 0; token = token.getNextToken()) {
			if (token.type == 16 || token.type == 3 || token.type == 2 || token.type == 1) {      //for white space and comments
				continue;
			}

			if (langs.equals("JJC")) {
				if (token.getLexeme().equals("new")) {
					if (prevToPrev.type == 15 && prev.getLexeme().equals("=")) {
						name = prevToPrev.getLexeme();
						found = true;
						continue;
					} else
						break;

				}

				if (found) {

					if (token.type == 15 || token.getLexeme().equals(".")) {
						fullClassName += token.getLexeme();
					} else if (token.getLexeme().equals("(")) {
						if (!(prev.type == 15))
							found = false;
						break;
					} else {
						found = false;
						break;
					}
				}
			}
			if (langs.equals("PM")) {
				if (token.getLexeme().equals("=")) {
					if (prev.type == 15) {
						name = prev.getLexeme();
						found = true;
						continue;
					} else
						break;
				}
				if (found) {
					if (token.type == 15 || token.getLexeme().equals(".")) {
						fullClassName += token.getLexeme();
					} else if (token.getLexeme().equals("(")) {
						if (!(prev.type == 15))
							found = false;
						break;
					} else {
						found = false;
						break;
					}
				}
			}
			if (langs.equals("R")) {
				if (token.getLexeme().equals("=")) {
					if (prev.type == 15) {
						name = prev.getLexeme();
						found = true;
						continue;
					} else
						break;
				}
				if (found) {
					if (token.getNextToken().type != 0) {
						if (token.getNextToken().getLexeme().equals("new"))
							break;
					}

					if (token.type == 15 || token.getLexeme().equals(".")) {
						fullClassName += token.getLexeme();
					} else {
						found = false;
						break;
					}
				}
			}

			if (!token.isWhitespace()) {
				prevToPrev = prev;
				prev = token;
			}
		}
		if (found) {
			int i = fullClassName.lastIndexOf(".");
			if (i > 0) {
				ImportedClassObjects obj = new ImportedClassObjects(name, fullClassName.substring(i + 1), fullClassName, true);
				objectSet.add(obj);
			}
		}
	}




	public void setPackageNames(Collection<String> names) {
		packageNames = names;
	}

	public void setObjectSetIfImported(Token prevToPrev, Token prev, Token classNameToken) {
		if (prev.getLexeme().equals("=")) {
			if (prevToPrev.type == 15) {
				String temp = classNameToken.getLexeme();
				if (classNameToken.getNextToken().getLexeme().equals("(") || classNameToken.getNextToken().getLexeme().equals(")")) {
					String temp2 = names.isClassPresent(temp);
					if (!temp2.equals("")) {
						ImportedClassObjects obj = new ImportedClassObjects(prevToPrev.getLexeme(), temp, temp2, true);
						objectSet.add(obj);
					}
				}
			}
		}
	}

	/* TODO: fix
	public Item findPackage(String[] splitPart, Package p) {
		return null;
	}
	*/

	public Token getNextNonWhitespaceToken(Token t) {
		Token toReturn = t.getNextToken();
		while (toReturn != null) {
			if (toReturn.getNextToken().isWhitespace()) {
				toReturn = toReturn.getNextToken();
			} else {
				return(toReturn.getNextToken());
			}
		}
		return toReturn;
	}
}
