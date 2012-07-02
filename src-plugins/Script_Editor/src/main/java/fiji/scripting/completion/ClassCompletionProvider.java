
package fiji.scripting.completion;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.JTextComponent;
import java.io.File;
import java.util.TreeSet;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.CompletionProviderBase;


public class ClassCompletionProvider extends CompletionProviderBase
			implements ToolTipSupplier {

	/**
	 * The provider to use when no provider is assigned to a particular token
	 * type.
	 */
	private DefaultProvider defaultProvider;

	/**
	 * The provider to use when completing in a string.
	 */
	private CompletionProvider stringCompletionProvider;

	RSyntaxTextArea textArea;
	/**
	 * The provider to use when completing in a comment.
	 */
	private CompletionProvider commentCompletionProvider;

	/**
	 * The provider to use while in documentation comments.
	 */
	private CompletionProvider docCommentCompletionProvider;

	protected String language;
	protected ClassNames names;
	protected Thread classNamesThread;

	public ClassNames getClassNames() {
		synchronized (classNamesThread) {
			return names;
		}
	}

	/**
	 * Constructor.
	 *
	 * @param defaultProvider The provider to use when no provider is assigned
	 *        to a particular token type.  This cannot be <code>null</code>.
	 */
	public ClassCompletionProvider(CompletionProvider provider, RSyntaxTextArea textArea, String language) {
		setDefaultProvider(provider);
		this.textArea = textArea;
		this.language = language;
		classNamesThread = new Thread() {
			public synchronized void run() {
				names = new ClassNames(defaultProvider);
			}
		};
		classNamesThread.start();
	}


	/**
	 * Calling this method will result in an
	 * {@link UnsupportedOperationException} being thrown.  To set the
	 * parameter completion parameters, do so on the provider returned by
	 * {@link #getDefaultCompletionProvider()}.
	 *
	 * @throws UnsupportedOperationException Always.
	 * @see #setParameterizedCompletionParams(char, String, char)
	 */
	public void clearParameterizedCompletionParams() {
		throw new UnsupportedOperationException();
	}


	/**
	 * {@inheritDoc}
	 */
	public String getAlreadyEnteredText(JTextComponent comp) {
		if (!(comp instanceof RSyntaxTextArea)) {
			return EMPTY_STRING;
		}
		CompletionProvider provider = getProviderFor(comp);
		return provider.getAlreadyEnteredText(comp);
	}


	/**
	 * Returns the completion provider to use for comments.
	 *
	 * @return The completion provider to use.
	 * @see #setCommentCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getCommentCompletionProvider() {
		return commentCompletionProvider;
	}


	/**
	 * {@inheritDoc}
	 */
	public List getCompletionsAt(JTextComponent tc, Point p) {
		return defaultProvider == null ? null :
		       defaultProvider.getCompletionsAt(tc, p);
	}

	/**
	 * Does the dirty work of creating a list of completions.
	 *
	 * @param comp The text component to look in.
	 * @return The list of possible completions, or an empty list if there
	 *         are none.
	 */
	protected List getCompletionsImpl(JTextComponent comp) {
		if (!(comp instanceof RSyntaxTextArea)) {
			return new ArrayList(0);
		}
		CompletionProvider provider = getProviderFor(comp);
		return provider != null ? provider.getCompletions(comp) :
		       new ArrayList(0);
	}


	/**
	 * Returns the completion provider used when one isn't defined for a
	 * particular token type.
	 *
	 * @return The completion provider to use.
	 * @see #setDefaultCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getDefaultProvider() {
		defaultProvider.clear();
		KeywordsCompletion.completeKeywords(defaultProvider, language, defaultProvider.getEnteredText(textArea));
		names.setClassCompletions(textArea, language);
		return defaultProvider;
	}

	public void setProviderLanguage(String lang) {
		language = lang;
	}



	/**
	 * Returns the completion provider to use for documentation comments.
	 *
	 * @return The completion provider to use.
	 * @see #setDocCommentCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getDocCommentCompletionProvider() {
		return docCommentCompletionProvider;
	}


	/**
	 * {@inheritDoc}
	 */
	public List getParameterizedCompletions(JTextComponent tc) {
		// Parameterized completions can only come from the "code" completion
		// provider.  We do not do function/method completions while editing
		// strings or comments.
		CompletionProvider provider = getProviderFor(tc);
		/*return provider==defaultProvider ?
				provider.getParameterizedCompletions(tc) : null;*/
		return null;
	}


	/**
	 * {@inheritDoc}
	 */
	public char getParameterListEnd() {
		return defaultProvider.getParameterListEnd();
	}


	/**
	 * {@inheritDoc}
	 */
	public String getParameterListSeparator() {
		return defaultProvider.getParameterListSeparator();
	}


	/**
	 * {@inheritDoc}
	 */
	public char getParameterListStart() {
		return defaultProvider.getParameterListStart();
	}


	/**
	 * Returns the completion provider to use at the current caret position in
	 * a text component.
	 *
	 * @param comp The text component to check.
	 * @return The completion provider to use.
	 */
	private CompletionProvider getProviderFor(JTextComponent comp) {

		RSyntaxTextArea rsta = (RSyntaxTextArea)comp;
		RSyntaxDocument doc = (RSyntaxDocument)rsta.getDocument();
		int line = rsta.getCaretLineNumber();
		Token t = doc.getTokenListForLine(line);
		if (t == null) {
			return getDefaultProvider();
		}

		int dot = rsta.getCaretPosition();
		Token curToken = RSyntaxUtilities.getTokenAtOffset(t, dot);

		if (curToken == null) { // At end of the line

			int type = doc.getLastTokenTypeOnLine(line);
			if (type == Token.NULL) {
				Token temp = t.getLastPaintableToken();
				if (temp == null) {
					return getDefaultProvider();
				}
				type = temp.type;
			}

			switch (type) {
			case Token.ERROR_STRING_DOUBLE:
				return getStringCompletionProvider();
			case Token.COMMENT_EOL:
			case Token.COMMENT_MULTILINE:
				return getCommentCompletionProvider();
			case Token.COMMENT_DOCUMENTATION:
				return getDocCommentCompletionProvider();
			default:
				return getDefaultProvider();
			}

		}

		// FIXME: This isn't always a safe assumption.
		if (dot == curToken.offset) { // At the very beginning of a new token
			// Need to check previous token for its type before deciding.
			// Previous token may also be on previous line!
			return getDefaultProvider();
		}

		switch (curToken.type) {
		case Token.LITERAL_STRING_DOUBLE_QUOTE:
		case Token.ERROR_STRING_DOUBLE:
			return getStringCompletionProvider();
		case Token.COMMENT_EOL:
		case Token.COMMENT_MULTILINE:
			return getCommentCompletionProvider();
		case Token.COMMENT_DOCUMENTATION:
			return getDocCommentCompletionProvider();
		case Token.NULL:
		case Token.WHITESPACE:
		case Token.IDENTIFIER:
		case Token.VARIABLE:
		case Token.PREPROCESSOR:
		case Token.DATA_TYPE:
		case Token.FUNCTION:
			return getDefaultProvider();
		}

		return null; // In a token type we can't auto-complete from.

	}


	/**
	 * Returns the completion provider to use for strings.
	 *
	 * @return The completion provider to use.
	 * @see #setStringCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getStringCompletionProvider() {
		return stringCompletionProvider;
	}


	/**
	 * Sets the comment completion provider.
	 *
	 * @param provider The provider to use in comments.
	 * @see #getCommentCompletionProvider()
	 */
	public void setCommentCompletionProvider(CompletionProvider provider) {
		this.commentCompletionProvider = provider;
	}


	/**
	 * Sets the default completion provider.
	 *
	 * @param provider The provider to use when no provider is assigned to a
	 *        particular token type.  This cannot be <code>null</code>.
	 * @see #getDefaultCompletionProvider()
	 */
	public void setDefaultProvider(CompletionProvider provider) {
		if (provider == null) {
			throw new IllegalArgumentException("provider cannot be null");
		}
		this.defaultProvider = (DefaultProvider)provider;
	}


	/**
	 * Sets the documentation comment completion provider.
	 *
	 * @param provider The provider to use in comments.
	 * @see #getDocCommentCompletionProvider()
	 */
	public void setDocCommentCompletionProvider(CompletionProvider provider) {
		this.docCommentCompletionProvider = provider;
	}


	/**
	 * Calling this method will result in an
	 * {@link UnsupportedOperationException} being thrown.  To set the
	 * parameter completion parameters, do so on the provider returned by
	 * {@link #getDefaultCompletionProvider()}.
	 *
	 * @throws UnsupportedOperationException Always.
	 * @see #clearParameterizedCompletionParams()
	 */
	public void setParameterizedCompletionParams(char listStart,
	                String separator, char listEnd) {
		throw new UnsupportedOperationException();
	}


	/**
	 * Sets the completion provider to use while in a string.
	 *
	 * @param provider The provider to use.
	 * @see #getStringCompletionProvider()
	 */
	public void setStringCompletionProvider(CompletionProvider provider) {
		stringCompletionProvider = provider;
	}


	/**
	 * Returns the tool tip to display for a mouse event.<p>
	 *
	 * For this method to be called, the <tt>RSyntaxTextArea</tt> must be
	 * registered with the <tt>javax.swing.ToolTipManager</tt> like so:
	 *
	 * <pre>
	 * ToolTipManager.sharedInstance().registerComponent(textArea);
	 * </pre>
	 *
	 * @param textArea The text area.
	 * @param e The mouse event.
	 * @return The tool tip text, or <code>null</code> if none.
	 */
	public String getToolTipText(RTextArea textArea, MouseEvent e) {

		String tip = null;

		List completions = getCompletionsAt(textArea, e.getPoint());
		if (completions != null && completions.size() > 0) {
			// Only ever 1 match for us in C...
			Completion c = (Completion)completions.get(0);
			tip = c.getToolTipText();
		}

		return tip;

	}


}
