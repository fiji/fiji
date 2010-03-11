package fiji.gui;

import java.awt.AWTEvent;
import java.awt.FileDialog;
import java.awt.List;
import java.awt.Toolkit;

import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class FileDialogDecorator extends KeyAdapter {
	// the list is assumed to be sorted
	protected List list;
	protected long lastWhen;
	protected String prefix;

	long timeout = 300; // maybe there is a proper AWT/Swing property for this?

	public FileDialogDecorator(List list) {
		this.list = list;
	}

	public void select(int index) {
		if (index < 0 || index >= list.getItemCount())
			return;
		list.select(index);
		list.makeVisible(index);
	}

	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (keyCode < e.VK_SPACE)
			return;
		switch (keyCode) {
		case KeyEvent.VK_HOME:
			select(0);
			return;
		case KeyEvent.VK_END:
			select(list.getItemCount() - 1);
			return;
		case KeyEvent.VK_PAGE_DOWN:
			select(list.getSelectedIndex() + list.getRows());
			return;
		case KeyEvent.VK_PAGE_UP:
			select(list.getSelectedIndex() - list.getRows());
			return;
		}
		long when = e.getWhen();
		if (when - lastWhen > timeout)
			prefix = "" + e.getKeyChar();
		else
			prefix += e.getKeyChar();
		int index = findItemForPrefix(list, prefix);
		if (index >= 0)
			select(index);
		lastWhen = when;
	}

	static boolean isSmaller(String s1, String s2) {
		int prefixLen = Math.min(s1.length(), s2.length());
		return s1.substring(0, prefixLen).compareTo(s2) > 0;
	}

	public static int findItemForPrefix(List list, String prefix) {
		int index = list.getSelectedIndex() + 1;
		if (index >= list.getItemCount() ||
				isSmaller(list.getItem(index), prefix))
			index = 0;
		for (;;) {
			if (list.getItem(index).startsWith(prefix)) {
				return index;
			}
			if (++index >= list.getItemCount())
				return -1;
		}
	}

	/*
	 * automatic decorator: listen for all just-opened FileDialogs,
	 * and decorate them right away.
	 */
	static class AutomaticDecorator implements AWTEventListener {
		public void eventDispatched(AWTEvent event) {
			ContainerEvent e = (ContainerEvent)event;
			if (e.getID() == ContainerEvent.COMPONENT_ADDED &&
					(e.getSource() instanceof FileDialog) &&
					(e.getChild() instanceof List))
				e.getChild().addKeyListener(new FileDialogDecorator((List)e.getChild()));
		}
	}

	public static void registerAutomaticDecorator() {
		Toolkit.getDefaultToolkit().addAWTEventListener(new AutomaticDecorator(), AWTEvent.CONTAINER_EVENT_MASK);
	}

	/* convenience function to start it in the Script Editor */

	public static void main(String[] args) {
		registerAutomaticDecorator();
		report("FileDialog decorator started");
		report("========================");
	}

	public static void report(String message) {
		if (!message.endsWith("\n"))
			message += "\n";
		System.err.println(message);
	}
}
