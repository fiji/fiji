package fiji.scripting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;

public class TokenFunctions implements Iterable<Token> {
	RSyntaxTextArea textArea;

	public TokenFunctions(RSyntaxTextArea textArea) {
		this.textArea = textArea;
	}

	public static boolean tokenEquals(Token token, char[] text) {
		if (token.type != token.RESERVED_WORD ||
				token.textCount != text.length)
			return false;
		for (int i = 0; i < text.length; i++)
			if (token.text[token.textOffset + i] != text[i])
				return false;
		return true;
	}

	public static boolean isIdentifier(Token token) {
		if (token.type != token.IDENTIFIER)
			return false;
		if (!Character.isJavaIdentifierStart(token.text[token.textOffset]))
			return false;
		for (int i = 1; i < token.textCount; i++)
			if (!Character.isJavaIdentifierStart(token.text[token.textOffset + i]))
				return false;
		return true;
	}

	public static String getText(Token token) {
		if (token.text == null)
			return "";
		return new String(token.text,
				token.textOffset, token.textCount);
	}

	class TokenIterator implements Iterator<Token> {
		int line = -1;
		Token current, next;

		public boolean hasNext() {
			if (next == null)
				getNextToken();
			return next != null;
		}

		public Token next() {
			current = next;
			next = null;
			return current;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		void getNextToken() {
			if (current != null) {
				next = current.getNextToken();
				if (next != null)
					return;
			}

			while (next == null) {
				if (++line >= textArea.getLineCount())
					return;

				next = textArea.getTokenListForLine(line);
			}
		}
	}

	public Iterator<Token> iterator() {
		return new TokenIterator();
	}

	public static boolean isDot(Token token) {
		return token.type == token.IDENTIFIER && token.textCount == 1
			&& token.text[token.textOffset] == '.';
	}

	/* The following methods are Java-specific */

	public final static char[] classCharacters = {'c', 'l', 'a', 's', 's'};
	public static boolean isClass(Token token) {
		return tokenEquals(token, classCharacters);
	}

	public String getClassName() {
		boolean classSeen = false;
		for (Token token : this)
			if (isClass(token))
				classSeen = true;
			else if (classSeen && isIdentifier(token))
				return getText(token);
		return null;
	}

	class Import {
		int startOffset, endOffset;
		String classOrPackage;
		Import(int start, int end, String text) {
			startOffset = start;
			endOffset = end;
			classOrPackage = text;
		}
	}

	Token skipNonCode(TokenIterator iter, Token current) {
		for (;;) {
			switch (current.type) {
				case Token.COMMENT_DOCUMENTATION:
				case Token.COMMENT_EOL:
				case Token.COMMENT_MULTILINE:
				case Token.WHITESPACE:
					break;
				default:
					return current;
			}
			if (!iter.hasNext())
				return null;
			current = iter.next();
		}
	}

	int skipToEOL(TokenIterator iter, Token current) {
		int end = textArea.getDocument().getLength();
		for (;;) {
			if (current.type == current.NULL || !iter.hasNext())
				return end;
			end = current.offset + current.textCount;
			current = iter.next();
		}
	}

	public final char[] importChars = { 'i', 'm', 'p', 'o', 'r', 't' };
	public List<Import> getImports() {
		List<Import> result = new ArrayList<Import>();

		TokenIterator iter = new TokenIterator();
		while (iter.hasNext()) {
			Token token = iter.next();
			int offset = token.offset;
			token = skipNonCode(iter, token);
			if (tokenEquals(token, importChars)) {
				do {
					if (!iter.hasNext())
						return result;
					token = iter.next();
				} while (!isIdentifier(token));
				int start = token.offset, end = start;
				do {
					if (!iter.hasNext())
						return result;
					token = iter.next();
					if (isDot(token) && iter.hasNext())
						token = iter.next();
					end = token.offset + token.textCount;
				} while (isIdentifier(token));
				String identifier = getText(start, end);
				if (identifier.endsWith(";"))
					identifier = identifier.substring(0,
						identifier.length() - 1);
				end = skipToEOL(iter, token);
				result.add(new Import(offset, end, identifier));
			}
		}

		return result;
	}

	public String getText(int start, int end) {
		try {
			return textArea.getText(start, end - start);
		} catch (BadLocationException e) { /* ignore */ }
		return "";
	}

	public boolean emptyLineAt(int offset) {
		return getText(offset, offset + 2).equals("\n\n");
	}

	public boolean eolAt(int offset) {
		return getText(offset, offset + 1).equals("\n");
	}

	void removeImport(Import imp) {
		int start = imp.startOffset, end = imp.endOffset;
		if (emptyLineAt(start - 2) && emptyLineAt(end))
			end += 2;
		else if (eolAt(end))
			end++;
		textArea.replaceRange("", start, end);
	}

	public void removeUnusedImports() {
		Set<String> identifiers = getAllUsedIdentifiers();
		List<Import> imports = getImports();
		for (int i = imports.size() - 1; i >= 0; i--) {
			Import imp = imports.get(i);
			String clazz = imp.classOrPackage;
			if (clazz.endsWith(".*"))
				continue;
			int dot = clazz.lastIndexOf('.');
			if (dot >= 0)
				clazz = clazz.substring(dot + 1);
			if (!identifiers.contains(clazz))
				removeImport(imp);
		}
	}

	public Set<String> getAllUsedIdentifiers() {
		Set<String> result = new HashSet<String>();
		boolean classSeen = false;
		String className = null;
		for (Token token : this)
			if (isClass(token))
				classSeen = true;
			else if (classSeen && className == null &&
					isIdentifier(token))
				className = getText(token);
			else if (classSeen && isIdentifier(token))
				result.add(getText(token));
		return result;
	}
}
