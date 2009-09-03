package fiji.pluginManager.ui;
import ij.plugin.BrowserLauncher;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Iterator;
import java.util.List;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import fiji.pluginManager.logic.Dependency;
import fiji.pluginManager.logic.UpdateTracker;
import fiji.pluginManager.logic.PluginCollection;
import fiji.pluginManager.logic.PluginObject;

public class TextPaneDisplay extends JTextPane {
	private AttributeSet italic;
	private AttributeSet bold;
	private AttributeSet normal;
	private AttributeSet title;
	private final String LINK_ATTRIBUTE = "URLSOURCE";

	public TextPaneDisplay() {
		italic = getStyle(Color.black, true, false, "Verdana", 12);
		bold = getStyle(Color.black, false, true, "Verdana", 12);
		normal =  getStyle(Color.black, false, false, "Verdana", 12);
		title = getStyle(Color.black, false, false, "Impact", 18);

		final JTextPane textPane = this;
		addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				try {
					AttributeSet as = getAttributes(textPane, e);
					//execute the url if it exists
					String url = (String)as.getAttribute(LINK_ATTRIBUTE);
					if (url != null)
						BrowserLauncher.openURL(url);
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}

			public void mouseEntered(MouseEvent e) { }

			public void mouseExited(MouseEvent e) { }

			public void mousePressed(MouseEvent e) { }

			public void mouseReleased(MouseEvent e) { }
		});
		addMouseMotionListener(new MouseMotionListener() {
			public void mouseMoved(MouseEvent e) {
				if (getAttributes(textPane, e).getAttribute(LINK_ATTRIBUTE) != null)
					textPane.setCursor(new Cursor(Cursor.HAND_CURSOR));
				else
					textPane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}

			public void mouseDragged(MouseEvent e) { }
		});
		setEditable(false);
	}

	private AttributeSet getAttributes(JTextPane textPane, MouseEvent e) { //mouse event use
		StyledDocument document = textPane.getStyledDocument();
		return document.getCharacterElement(
				textPane.viewToModel(e.getPoint())).getAttributes();
	}

	private AttributeSet getLinkElement(String url) {
		SimpleAttributeSet style = (SimpleAttributeSet)
			getStyle(Color.blue, false, false, "Verdana", 12);
		style.addAttribute(LINK_ATTRIBUTE, url);
		return style;
	}

	public static AttributeSet getStyle(Color color, boolean italic,
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
			throw new RuntimeException(e); //This is an internal error
		}
	}

	public void link(String url) {
		styled(url, getLinkElement(url));
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
	public void insertDependenciesList(Iterable<Dependency> list) {
		StringBuilder text = new StringBuilder();
		for (Dependency dependency : list)
			text.append((text.length() > 0 ? ",\n" : "")
					+ dependency.filename + " ("
					+ dependency.timestamp + ")");
		normal("\n" + (text.length() > 0 ? text.toString() : "None"));
	}

	//appends plugin description to existing text
	public void insertDescription(String description) {
		if (description == null || description.trim().equals("")) {
			normal("\nNo description available.");
		} else {
			normal("\n" + description);
		}
	}

	public void insertAuthors(Iterable<String> authors) {
		StringBuilder text = new StringBuilder();
		for (String author : authors)
			text.append((text.length() > 0 ? ", " : "") + author);
		normal(text.length() > 0 ? text.toString() : "Not specified");
	}

	public void insertLinks(Iterable<String> links) {
		for (String link : links) {
			normal("\n- ");
			link(link);
		}
	}

	//appends list of plugin names to existing text
	public void insertPluginNamelist(String title, PluginCollection myList) {
		title(title);
		for (PluginObject plugin : myList)
			normal("\n" + plugin.getFilename());
	}

	//appends list of plugin names and each of their descriptions to existing text
	public void insertPluginDescriptions(String title, PluginCollection myList) {
		title(title);
		insertBlankLine();
		for (PluginObject plugin : myList) {
			bold(plugin.getFilename());
			insertDescription(plugin.description);
			insertBlankLine();
		}
	}

	//inserts blank new line
	public void insertBlankLine() {
		normal("\n\n");
	}

	//rewrite the entire textpane with details of a plugin
	public void showPluginDetails(PluginObject plugin) {
		setText("");
		//Display plugin data, text with different formatting
		title(plugin.getFilename());
		if (plugin.isUpdateable())
			italic("\n(Update is available)");
		if (plugin.isFiji() && plugin.newChecksum != null) {
			insertBlankLine();
			bold("Warning: ");
			italic("This version is not in Fiji's records.");
		}
		insertBlankLine();
		bold("Date: ");
		normal("" + plugin.current.timestamp);
		insertBlankLine();
		bold("Author(s): ");
		insertAuthors(plugin.getAuthors());
		insertBlankLine();
		bold("Description");
		insertDescription(plugin.getDescription());
		insertBlankLine();
		bold("Reference Link(s):");
		insertLinks(plugin.getLinks());
		insertBlankLine();
		bold("Dependency");
		insertDependenciesList(plugin.getDependencies());
		insertBlankLine();
		bold("Checksum");
		normal("\n" + plugin.getChecksum());
		insertBlankLine();
		bold("Is Fiji Plugin: ");
		normal(plugin.isFiji() ? "Yes" : "No");
		if (plugin.newChecksum != null) {
			insertBlankLine();
			title("Locally modified:");
			insertBlankLine();
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
