package fiji.scripting;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;

public class TokenFunctions {
	RSyntaxTextArea textArea;

	public TokenFunctions(RSyntaxTextArea textArea) {
		this.textArea = textArea;
	}

	public String getClassName() {
		boolean classSeen = false;
		for (int i = 0; i < textArea.getLineCount(); i++) {
			Token token = textArea.getTokenListForLine(i);
			while (token != null) {
				if (isClass(token))
					classSeen = true;
				else if (classSeen && isIdentifier(token))
					return getText(token);
				token = token.getNextToken();
			}
		}
		return null;
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

	public final static char[] classCharacters = {'c', 'l', 'a', 's', 's'};
	public static boolean isClass(Token token) {
		return tokenEquals(token, classCharacters);
	}

	public static boolean isIdentifier(Token token) {
		return token.type == token.IDENTIFIER;
	}

	public static String getText(Token token) {
		return new String(token.text,
				token.textOffset, token.textCount);
	}
}
