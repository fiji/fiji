package fiji.scripting;

import fiji.scripting.Languages.Language;

import ij.IJ;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextArea;

import javax.swing.text.BadLocationException;
import javax.swing.text.Position;


public class ErrorHandler {
	List<Error> list = new ArrayList<Error>();
	int current = -1;
	JTextArea textArea;
	int currentOffset;
	Parser parser;

	public ErrorHandler(JTextArea textArea) {
		this.textArea = textArea;
	}

	public ErrorHandler(Language language, JTextArea textArea,
			int startOffset) {
		this(textArea);
		if (language.menuLabel.equals("Java"))
			parser = new JavacErrorParser();
		else
			return;

		currentOffset = startOffset;

		try {
			parseErrors();
		} catch (BadLocationException e) {
			IJ.handleException(e);
		}
	}

	public boolean nextError(boolean forward) {
		if (forward) {
			if (current + 1 >= list.size())
				return false;
			current++;
		}
		else {
			if (current - 1 < 0)
				return false;
			current--;
		}
		return true;
	}

	public String getPath() {
		return list.get(current).path;
	}

	public int getLine() {
		return list.get(current).line;
	}

	public Position getPosition() {
		return list.get(current).position;
	}

	public void markLine() throws BadLocationException {
		int offset = getPosition().getOffset();
		int line = textArea.getLineOfOffset(offset);
		int start = textArea.getLineStartOffset(line);
		int end = textArea.getLineEndOffset(line);
		textArea.setCaretPosition(end);
		textArea.moveCaretPosition(start);
	}

	static class Error {
		String path;
		int line;
		Position position;

		public Error(String path, int line) {
			this.path = path;
			this.line = line;
		}
	}

	interface Parser {
		Error getError(String line);
	}

	void parseErrors() throws BadLocationException {
		int line = textArea.getLineOfOffset(currentOffset);
		int lineCount = textArea.getLineCount();
		for (;;) {
			if (++line >= lineCount)
				return;
			int start = textArea.getLineStartOffset(line);
			int end = textArea.getLineEndOffset(line);
			String text = textArea.getText(start, end - start);
			Error error = parser.getError(text);
			if (error != null) try {
				error.position = textArea.getDocument()
					.createPosition(start);
				list.add(error);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	class JavacErrorParser implements Parser {
		public Error getError(String line) {
			if (!line.startsWith("/"))
				return null;
			int colon = line.indexOf(':');
			if (colon <= 0)
				return null;
			int next = line.indexOf(':', colon + 1);
			if (next < colon + 2)
				return null;
			int lineNumber;
			try {
				lineNumber = Integer.parseInt(line
					.substring(colon + 1, next));
			} catch (NumberFormatException e) {
				return null;
			}
			return new Error(line.substring(0, colon), lineNumber);
		}
	}
}
