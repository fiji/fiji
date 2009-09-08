package fiji.updater.ui;

import ij.IJ;

import ij.plugin.BrowserLauncher;

import fiji.updater.logic.Dependency;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;

import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;

import java.util.Iterator;
import java.util.List;

import javax.swing.JTextPane;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class PluginDetails extends JTextPane {
	private final static AttributeSet bold, italic, normal, title;
	private final static Cursor hand, defaultCursor;
	private final static String LINK_ATTRIBUTE = "URL";

	static {
		italic = getStyle(Color.black, true, false, "Verdana", 12);
		bold = getStyle(Color.black, false, true, "Verdana", 12);
		normal =  getStyle(Color.black, false, false, "Verdana", 12);
		title = getStyle(Color.black, false, false, "Impact", 18);

		hand = new Cursor(Cursor.HAND_CURSOR);
		defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
	}

	public PluginDetails() {
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				String url = getLinkAt(e.getPoint());
				try {
					if (url != null)
						BrowserLauncher.openURL(url);
				} catch(Exception exception) {
					exception.printStackTrace();
					IJ.error("Could not open " + url + ": "
						+ exception.getMessage());
				}
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				setCursor(e.getPoint());
			}
		});

		setEditable(false); // TODO: if this prevents copy-n-paste, that's not good.
	}

	private String getLinkAt(Point p) {
		StyledDocument document = getStyledDocument();
		Element e = document.getCharacterElement(viewToModel(p));
		return (String)e.getAttributes().getAttribute(LINK_ATTRIBUTE);
	}

	protected void setCursor(Point p) {
		setCursor(getLinkAt(p) == null ? defaultCursor : hand);
	}

	private AttributeSet getLinkAttribute(String url) {
		// TODO: Verdana?  Java is platform-independent, if this introduces a platform dependency, it needs to be thrown out, quickly!
		SimpleAttributeSet style =
			getStyle(Color.blue, false, false, "Verdana", 12);
		style.addAttribute(LINK_ATTRIBUTE, url);
		return style;
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
		Document document = getDocument();
		try {
			document.insertString(document.getLength(), text, set);
		} catch (BadLocationException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void link(String url) {
		styled(url, getLinkAttribute(url));
	}

	public void italic(String text) {
		styled(text, italic);
	}

	public void bold(String text) {
		styled(text, bold);
	}

	public void normal(String text) {
		styled(text, normal);
	}

	public void title(String text) {
		styled(text, title);
	}

	//appends list of dependencies to existing text
	public void dependencies(Iterable<Dependency> list) {
		StringBuilder text = new StringBuilder();
		for (Dependency dependency : list)
			text.append((text.length() > 0 ? ",\n" : "")
					+ dependency.filename + " ("
					+ dependency.timestamp + ")");
		normal("\n" + (text.length() > 0 ? text.toString() : "None"));
	}

	public void description(String description) {
		if (description == null || description.trim().equals(""))
			description = "No description available.";
		normal("\n" + description);
	}

	public String join(Iterable<String> list, String delimiter) {
		if (list == null)
			return "";
		boolean first = true;
		StringBuilder result = new StringBuilder();
		for (String item : list) {
			if (first)
				first = false;
			else
				result.append(delimiter);
			result.append(item);
		}
		return result.toString();
	}

	public String join(Iterable<String> list, String delimiter,
			String defaultString) {
		String result = join(list, delimiter);
		return result.equals("") ? defaultString : result;
	}

	public void authors(Iterable<String> authors) {
		StringBuilder text = new StringBuilder();
		for (String author : authors)
			text.append((text.length() > 0 ? ", " : "") + author);
		normal(text.length() > 0 ? text.toString() : "Not specified");
	}

	public void links(Iterable<String> links) {
		for (String link : links) {
			normal("\n- ");
			link(link);
		}
	}

	//appends list of plugin names to existing text
	public void pluginNamelist(String title, PluginCollection myList) {
		title(title);
		for (PluginObject plugin : myList)
			normal("\n" + plugin.getFilename());
	}

	//appends list of plugin names and each of their descriptions to existing text
	public void pluginDescriptions(String title, PluginCollection myList) {
		title(title);
		blankLine();
		for (PluginObject plugin : myList) {
			bold(plugin.getFilename());
			description(plugin.description);
			blankLine();
		}
	}

	public void blankLine() {
		normal("\n\n");
	}

	public void showPluginDetails(PluginObject plugin) {
		if (!getText().equals(""))
			blankLine();
		//Display plugin data, text with different formatting
		title(plugin.getFilename());
		if (plugin.isUpdateable())
			italic("\n(Update is available)");
		if (plugin.isFiji() && plugin.newChecksum != null) {
			blankLine();
			bold("Warning: ");
			italic("This version is not in Fiji's records.");
		}
		blankLine();
		bold("Date: ");
		normal("" + plugin.current.timestamp);
		blankLine();
		bold("Author(s): ");
		authors(plugin.getAuthors());
		blankLine();
		bold("Description");
		description(plugin.getDescription());
		blankLine();
		bold("Reference Link(s):");
		links(plugin.getLinks());
		blankLine();
		bold("Dependency");
		dependencies(plugin.getDependencies());
		blankLine();
		bold("Checksum");
		normal("\n" + plugin.getChecksum());
		blankLine();
		bold("Is Fiji Plugin: ");
		normal(plugin.isFiji() ? "Yes" : "No");
		if (plugin.newChecksum != null) {
			blankLine();
			title("Locally modified:");
			blankLine();
			bold("New Checksum");
			normal(plugin.newChecksum + "\n");
			bold("Timestamp: ");
			normal("" + plugin.newTimestamp);
		}

		//ensure first line of text is always shown (i.e.: scrolled to top)
		scrollToTop();
	}

	// TODO: no.  I said a million times that this is wrong.
	public void scrollToTop() {
		setSelectionStart(0);
		setSelectionEnd(0);
	}
}
