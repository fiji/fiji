package fiji.scripting.completion;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;
import org.fife.ui.autocomplete.AbstractCompletionProvider;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.ParameterizedCompletion;



public class DefaultProvider extends AbstractCompletionProvider {

	protected Segment seg;

	/**
	 * Used to speed up {@link #getCompletionsAt(JTextComponent, Point)}.
	 */
	private String lastCompletionsAtText;

	/**
	 * Used to speed up {@link #getCompletionsAt(JTextComponent, Point)},
	 * since this may be called multiple times in succession (this is usually
	 * called by <tt>JTextComponent.getToolTipText()</tt>, and if the user
	 * wiggles the mouse while a tool tip is displayed, this method gets
	 * repeatedly called.  It can be costly so we try to speed it up a tad).
	 */
	private List lastParameterizedCompletionsAt;

	/**
	 * Constructor.  The returned provider will not be aware of any completions.
	 *
	 * @see #addCompletion(Completion)
	 */
	public DefaultProvider() {
		init();
	}


	/**
	 * Creates a completion provider that provides completion for a simple
	 * list of words.
	 *
	 * @param words The words to offer as completion suggestions.  If this is
	 *        <code>null</code>, no completions will be known.
	 * @see BasicCompletion
	 */
	public DefaultProvider(String[] words) {
		init();
		addWordCompletions(words);
	}


	/**
	 * Returns the text just before the current caret position that could be
	 * the start of something auto-completable.<p>
	 *
	 * This method returns all characters before the caret that are matched
	 * by  {@link #isValidChar(char)}.
	 *
	 * @param comp The text component.
	 * @return The text.
	 */
	public String getAlreadyEnteredText(JTextComponent comp) {

		Document doc = comp.getDocument();

		int dot = comp.getCaretPosition();
		Element root = doc.getDefaultRootElement();
		int index = root.getElementIndex(dot);
		Element elem = root.getElement(index);
		int start = elem.getStartOffset();
		int len = dot - start;
		try {
			doc.getText(start, len, seg);
		} catch (BadLocationException ble) {
			ble.printStackTrace();
			return EMPTY_STRING;
		}

		int segEnd = seg.offset + len;
		start = segEnd - 1;
		while (start >= seg.offset && isValidChar(seg.array[start])) {
			start--;
		}
		start++;

		len = segEnd - start;
		return len == 0 ? EMPTY_STRING : new String(seg.array, start, len);

	}

	public String getEnteredText(JTextComponent comp) {                  //this includes the "." in the total returned string

		Document doc = comp.getDocument();

		int dot = comp.getCaretPosition();
		Element root = doc.getDefaultRootElement();
		int index = root.getElementIndex(dot);
		Element elem = root.getElement(index);
		int start = elem.getStartOffset();
		int len = dot - start;
		try {
			doc.getText(start, len, seg);
		} catch (BadLocationException ble) {
			ble.printStackTrace();
			return EMPTY_STRING;
		}

		int segEnd = seg.offset + len;
		start = segEnd - 1;
		while (start >= seg.offset && isAcceptableChar(seg.array[start])) {
			start--;
		}
		start++;

		len = segEnd - start;
		return len == 0 ? EMPTY_STRING : new String(seg.array, start, len);

	}


	public List getCompletionsAt(JTextComponent tc, Point p) {

		int offset = tc.viewToModel(p);
		if (offset < 0 || offset >= tc.getDocument().getLength()) {
			lastCompletionsAtText = null;
			return lastParameterizedCompletionsAt = null;
		}

		Segment s = new Segment();
		Document doc = tc.getDocument();
		Element root = doc.getDefaultRootElement();
		int line = root.getElementIndex(offset);
		Element elem = root.getElement(line);
		int start = elem.getStartOffset();
		int end = elem.getEndOffset() - 1;

		try {

			doc.getText(start, end - start, s);

			// Get the valid chars before the specified offset.
			int startOffs = s.offset + (offset - start) - 1;
			while (startOffs >= s.offset && isValidChar(s.array[startOffs])) {
				startOffs--;
			}

			// Get the valid chars at and after the specified offset.
			int endOffs = s.offset;
			while (endOffs < s.offset + s.count && isValidChar(s.array[endOffs])) {
				endOffs++;
			}

			int len = endOffs - startOffs - 1;
			if (len <= 0) {
				return lastParameterizedCompletionsAt = null;
			}
			String text = new String(s.array, startOffs + 1, len);

			if (text.equals(lastCompletionsAtText)) {
				return lastParameterizedCompletionsAt;
			}

			// Get a list of all Completions matching the text.
			List list = getCompletionByInputText(text);
			lastCompletionsAtText = text;
			return lastParameterizedCompletionsAt = list;

		} catch (BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}

		lastCompletionsAtText = null;
		return lastParameterizedCompletionsAt = null;

	}


	public List getParameterizedCompletions(JTextComponent tc) {

		List list = null;

		// If this provider doesn't support parameterized completions,
		// bail out now.
		char paramListStart = getParameterListStart();
		if (paramListStart == 0) {
			return list; // null
		}

		int dot = tc.getCaretPosition();
		Segment s = new Segment();
		Document doc = tc.getDocument();
		Element root = doc.getDefaultRootElement();
		int line = root.getElementIndex(dot);
		Element elem = root.getElement(line);
		int offs = elem.getStartOffset();
		int len = dot - offs - 1/*paramListStart.length()*/;
		if (len <= 0) { // Not enough chars on line for a method.
			return list; // null
		}

		try {

			doc.getText(offs, len, s);

			// Get the identifier preceding the '(', ignoring any whitespace
			// between them.
			offs = s.offset + len - 1;
			while (offs >= s.offset && Character.isWhitespace(s.array[offs])) {
				offs--;
			}
			int end = offs;
			while (offs >= s.offset && isValidChar(s.array[offs])) {
				offs--;
			}

			String text = new String(s.array, offs + 1, end - offs);

			// Get a list of all Completions matching the text, but then
			// narrow it down to just the ParameterizedCompletions.
			List l = getCompletionByInputText(text);
			if (l != null && !l.isEmpty()) {
				for (int i = 0; i < l.size(); i++) {
					Object o = l.get(i);
					if (o instanceof ParameterizedCompletion) {
						if (list == null) {
							list = new ArrayList(1);
						}
						list.add(o);
					}
				}
			}

		} catch (BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}

		return list;

	}


	/**
	 * Initializes this completion provider.
	 */
	protected void init() {
		completions = new ArrayList();
		seg = new Segment();
	}


	/**
	 * Returns whether the specified character is valid in an auto-completion.
	 * The default implementation is equivalent to
	 * "<code>Character.isLetterOrDigit(ch) || ch=='_'</code>".  Subclasses
	 * can override this method to change what characters are matched.
	 *
	 * @param ch The character.
	 * @return Whether the character is valid.
	 */
	protected boolean isValidChar(char ch) {
		return (Character.isLetterOrDigit(ch) || ch == '_');
	}

	protected boolean isAcceptableChar(char ch) {
		return (Character.isLetterOrDigit(ch) || ch == '_' || ch == '.');
	}




}
