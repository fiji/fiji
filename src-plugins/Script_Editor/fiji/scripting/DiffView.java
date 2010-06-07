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
	protected SimpleAttributeSet normal, bigBold, bold, italic, red, green;
	protected Document document;
	protected int adds, removes;
	protected boolean inHeader = true;

	public DiffView() {
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		getViewport().setView(panel);

		normal = getStyle(Color.black, false, false, "Courier", 12);
		bigBold = getStyle(Color.blue, false, true, "Courier", 15);
		bold = getStyle(Color.black, false, true, "Courier", 12);
		italic = getStyle(Color.black, true, false, "Courier", 12);
		red = getStyle(Color.red, false, false, "Courier", 12);
		green = getStyle(new Color(0, 128, 32), false, false, "Courier", 12);

		JTextPane current = new JTextPane();
		current.setEditable(false);
		document = current.getDocument();
		panel.add(current);

		getVerticalScrollBar().setUnitIncrement(10);
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
		if (line.startsWith("diff")) {
			styled(line, bold);
			inHeader = false;
		}
		else if (line.startsWith(" "))
			styled(line, inHeader && line.startsWith("    ") ? bigBold : normal);
		else if (line.startsWith("+")) {
			adds++;
			styled(line, green);
		}
		else if (line.startsWith("-")) {
			removes++;
			styled(line, red);
		}
		else {
			if (line.startsWith("commit"))
				inHeader = true;
			styled(line, italic);
		}
		styled("\n", normal);
	}

	public int getAdds() {
		return adds;
	}

	public int getRemoves() {
		return removes;
	}

	public int getChanges() {
		return adds + removes;
	}

	public static class IJLog implements LineHandler {
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
