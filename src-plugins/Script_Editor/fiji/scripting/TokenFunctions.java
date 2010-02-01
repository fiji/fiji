package fiji.scripting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
}
