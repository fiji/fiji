package fiji.updater.ui;

import ij.IJ;

import ij.plugin.BrowserLauncher;

import fiji.updater.logic.Dependency;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;

import fiji.updater.util.Util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;

import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JTextPane;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class PluginDetails extends JTextPane implements UndoableEditListener {
	private final static AttributeSet bold, italic, normal, title;
	private final static Cursor hand, defaultCursor;
	private final static String LINK_ATTRIBUTE = "URL";
	SortedMap<Position, EditableRegion> editables;
	Position dummySpace;
	UpdaterFrame updaterFrame;

	static {
		italic = getStyle(Color.black, true, false, "Verdana", 12);
		bold = getStyle(Color.black, false, true, "Verdana", 12);
		normal =  getStyle(Color.black, false, false, "Verdana", 12);
		title = getStyle(Color.black, false, false, "Impact", 18);

		hand = new Cursor(Cursor.HAND_CURSOR);
		defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
	}

	public PluginDetails(UpdaterFrame updaterFrame) {
		this.updaterFrame = updaterFrame;
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

		reset();
		if (Util.isDeveloper)
			getDocument().addUndoableEditListener(this);
	}

	public void reset() {
		setEditable(false);
		setText("");
		Comparator<Position> comparator = new Comparator<Position>() {
			public int compare(Position p1, Position p2) {
				return p1.getOffset() - p2.getOffset();
			}

			public boolean equals(Comparator<Position> comparator) {
				return false;
			}
		};

		editables = new TreeMap<Position, EditableRegion>(comparator);
		dummySpace = null;
	}

	public void setEditableForDevelopers() {
		if (!Util.isDeveloper)
			return;
		removeDummySpace();
		setEditable(true);
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

	public void description(String description, PluginObject plugin) {
		if (!Util.isDeveloper && (description == null ||
					description.trim().equals("")))
			return;
		blankLine();
		bold("Description:\n");
		int offset = getCaretPosition();
		normal(description);
		addEditableRegion(offset, "Description", plugin);
	}

	public void list(String label, boolean showLinks,
			Iterable items, String delim, PluginObject plugin) {
		List list = new ArrayList();
		for (Object object : items)
			list.add(object);

		if (!Util.isDeveloper && list.size() == 0)
			return;

		blankLine();
		String tag = label;
		if (list.size() > 1 && label.endsWith("y"))
			label = label.substring(0, label.length() - 1) + "ie";
		bold(label + (list.size() > 1 ? "s" : "") + ":\n");
		int offset = getCaretPosition();
		String delimiter = "";
		for (Object object : list) {
			normal(delimiter);
			delimiter = delim;
			if (showLinks)
				link(object.toString());
			else
				normal(object.toString());
		}
		addEditableRegion(offset, tag, plugin);
	}

	public void blankLine() {
		int offset = getCaretPosition();
		try {
			if (offset > 1 && getText(offset - 2, 2)
					.equals("\n ")) {
				normal("\n");
				return;
			}
		}
		catch (BadLocationException e) { e.printStackTrace(); }
		normal("\n\n");
	}

	final String[] months = { "Zero",
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};

	String prettyPrintTimestamp(long timestamp) {
		String t = "" + timestamp;
		return t.substring(6, 8) + " "
			+ months[Integer.parseInt(t.substring(4, 6))] + " "
			+ t.substring(0, 4);
	}

	public void showPluginDetails(PluginObject plugin) {
		if (!getText().equals(""))
			blankLine();
		title(plugin.getFilename());
		if (plugin.isUpdateable())
			italic("\n(Update available)");
		else if (!plugin.isFiji())
			italic("(Not in Fiji)");
		if (plugin.isLocallyModified()) {
			blankLine();
			bold("Warning: ");
			italic("This file was locally modified.");
		}
		blankLine();
		if (plugin.current == null)
			bold("This file is no longer needed by Fiji");
		else {
			bold("Release date:\n");
			normal(prettyPrintTimestamp(plugin.current.timestamp));
		}
		description(plugin.getDescription(), plugin);
		list("Author", false, plugin.getAuthors(), ", ", plugin);
		if (Util.isDeveloper)
			list("Platform", false, plugin.getPlatforms(), ", ",
				plugin);
		list("Category", false, plugin.getCategories(), ", ", plugin);
		list("Link", true, plugin.getLinks(), "\n", plugin);
		list("Dependency", false, plugin.getDependencies(), ",\n",
				plugin);

		// scroll to top
		scrollRectToVisible(new Rectangle(0, 0, 1, 1));
	}

	class EditableRegion implements Comparable<EditableRegion> {
		PluginObject plugin;
		String tag;
		Position start, end;

		public EditableRegion(PluginObject plugin, String tag,
				Position start, Position end) {
			this.plugin = plugin;
			this.tag = tag;
			this.start = start;
			this.end = end;
		}

		public int compareTo(EditableRegion other) {
			return start.getOffset() - other.start.getOffset();
		}

		public String toString() {
			return "EditableRegion(" + tag
				+ ":" + start.getOffset()
				+ "-" + (end == null ? "null" : end.getOffset())
				+ ")";
		}
	}

	void addEditableRegion(int startOffset, String tag,
			PluginObject plugin) {
		int endOffset = getCaretPosition();
		try {
			// make sure end position does not move further
			normal(" ");
			Position start, end;
			start = getDocument().createPosition(startOffset - 1);
			end = getDocument().createPosition(endOffset);

			editables.put(start,
				new EditableRegion(plugin, tag, start, end));
			removeDummySpace();
			dummySpace = end;
		}
		catch (BadLocationException e) { e.printStackTrace(); }
	}

	void removeDummySpace() {
		if (dummySpace != null) try {
			getDocument().remove(dummySpace.getOffset(), 1);
			dummySpace = null;
		}
		catch (BadLocationException e) { e.printStackTrace(); }
	}

	boolean handleEdit() {
		EditableRegion editable;
		try {
			int offset = getCaretPosition();
			Position current =
				getDocument().createPosition(offset);
			Position last = editables.headMap(current).lastKey();
			editable = editables.get(last);
			if (offset > editable.start.getOffset() &&
					offset > editable.end.getOffset())
				return false;
		}
		catch(NoSuchElementException e) { return false; }
		catch(BadLocationException e) { return false; }

		int start = editable.start.getOffset() + 1;
		int end = editable.end.getOffset();

		String text;
		try { text = getDocument().getText(start, end + 1 - start); }
		catch (BadLocationException e) { return false; }

		if (editable.tag.equals("Description")) {
			editable.plugin.description = text;
			return true;
		}
		String[] list = text.split(editable.tag.equals("Link") ?
				"\n" : ",");
		editable.plugin.replaceList(editable.tag, list);
		return true;
	}

	// Do not process key events when on bold parts of the text
	// or when not developer
	public void undoableEditHappened(UndoableEditEvent e) {
		if (isEditable()) {
			if (!handleEdit())
				e.getEdit().undo();
			else
				updaterFrame.markUploadable();
		}
	}
}
