package fiji.scripting;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;

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
