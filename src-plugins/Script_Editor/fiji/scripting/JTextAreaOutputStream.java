package fiji.scripting;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;

import javax.swing.text.BadLocationException;

public class JTextAreaOutputStream extends OutputStream {
	JTextArea textArea;

	public JTextAreaOutputStream(JTextArea textArea) {
		this.textArea = textArea;
	}

	public void write(int i) {
		write(Character.toString((char)i));
	}

	public void write(byte[] buffer) {
		write(new String(buffer));
	}

	public void write(byte[] buffer, int off, int len) {
		write(new String(buffer, off, len));
	}

	public synchronized void write(String string) {
		int lineCount = textArea.getLineCount();
		if (lineCount > 1000) try {
			textArea.replaceRange("", 0,
				textArea.getLineEndOffset(lineCount - 1000));
		} catch (BadLocationException e) { e.printStackTrace(); }
		textArea.append(string);
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	public synchronized void flush() {
		textArea.repaint();
	}

	public void close() {
		flush();
	}
}
