package fiji.scripting;

import fiji.SimpleExecuter;

import fiji.SimpleExecuter.LineHandler;

import ij.IJ;

import java.awt.Color;

import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class DiffView extends JScrollPane implements LineHandler {
	protected JPanel panel;
	protected SimpleAttributeSet normal, italic, red, green;
	protected Document document;

	public DiffView() {
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		getViewport().setView(panel);

		normal = getStyle(Color.black, false, false, "Courier", 12);
		italic = getStyle(Color.black, true, false, "Courier", 12);
		red = getStyle(Color.red, false, false, "Courier", 12);
		green = getStyle(new Color(0, 128, 32), false, false, "Courier", 12);

		JTextPane current = new JTextPane();
		current.setEditable(false);
		document = current.getDocument();
		panel.add(current);
	}

	public static SimpleAttributeSet getStyle(Color color, boolean italic,
			boolean bold, String fontName, int fontSize) {
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setForeground(style, color);
		StyleConstants.setItalic(style, italic);
		StyleConstants.setBold(style, bold);
		StyleConstants.setFontFamily(style, fontName);
		StyleConstants.setFontSize(style, fontSize);
		return style;
	}

	public void styled(String text, AttributeSet set) {
		try {
			document.insertString(document.getLength(), text, set);
		} catch (BadLocationException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void handleLine(String line) {
		if (line.startsWith("diff"))
			styled(line, normal);
		else if (line.startsWith(" "))
			styled(line, normal);
		else if (line.startsWith("+"))
			styled(line, green);
		else if (line.startsWith("-"))
			styled(line, red);
		else
			styled(line, italic);
		styled("\n", normal);
	}

	protected static class IJLog implements LineHandler {
		public void handleLine(String line) {
			IJ.log(line);
		}
	}

	public static void main(String[] args) {
		DiffView diff = new DiffView();
		try {
			SimpleExecuter e = new SimpleExecuter(new String[] {
					"git", "show"
				}, diff, new IJLog());
		} catch (IOException e) {
			IJ.handleException(e);
		}

		JFrame frame = new JFrame("git show");
		frame.setSize(640, 480);
		frame.getContentPane().add(diff);
		frame.pack();
		frame.setVisible(true);
	}
}
