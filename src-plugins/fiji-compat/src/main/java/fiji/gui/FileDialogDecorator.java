package fiji.gui;

import ij.IJ;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.List;
import java.awt.Toolkit;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.io.File;

public class FileDialogDecorator extends KeyAdapter {
	// the list is assumed to be sorted
	protected List list;
	protected long lastWhen;
	protected String prefix;
	protected boolean reRequestFocusAfterEnter;

	long timeout = 300; // maybe there is a proper AWT/Swing property for this?

	public FileDialogDecorator(List list) {
		this.list = list;

		/* Try to regain focus in the directory list after a
		 * directory was entered.
		 *
		 * The directory list is identified by the component count 7,
		 * which is a hack, but then, this whole class is.
		 */
		if (((Container)list.getParent()).getComponentCount() == 7) {
			reRequestFocusAfterEnter = true;
			registerDropTarget(list.getParent());
		}
	}

	public void select(int index) {
		if (index < 0 || index >= list.getItemCount())
			return;
		list.select(index);
		list.makeVisible(index);
	}

	public void requestFocus() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				list.requestFocus();
			}
		});
	}

	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (keyCode < e.VK_SPACE) {
			if (reRequestFocusAfterEnter && keyCode == e.VK_ENTER)
				// Need to invoke later 2x to give Sun's
				// KeyEvent handler a chance to request focus
				// for the "Files" list.
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						requestFocus();
					}
				});
			return;
		}
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

	static class DropListener implements DropTargetListener {
		FileDialog fileDialog;

		DropListener(FileDialog fileDialog) {
			this.fileDialog = fileDialog;
		}

		String trim(String string) {
			int i;
			for (i = string.length(); i > 0; i--)
				if ("\r\n".indexOf(string.charAt(i - 1)) < 0)
					break;
			return string.substring(0, i);
		}

		public void drop(DropTargetDropEvent dtde) {
			dtde.acceptDrop(DnDConstants.ACTION_COPY);
			Transferable t = dtde.getTransferable();

			/* seems not to occor on Linux
			if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) try {
				Iterable list = (Iterable)t.getTransferData(DataFlavor.javaFileListFlavor);
				for (Object item : list) {
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			else
			*/

			if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) try {
                                String string = (String)t.getTransferData(DataFlavor.stringFlavor);
				if (string.startsWith("file://")) {
					string = trim(string.substring(7));
					File file = new File(string);
					if (file.isDirectory())
						fileDialog.setDirectory(string);
					else {
						fileDialog.setDirectory(file.getParent());
						fileDialog.setFile(file.getName());
					}
				}
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
		}
		public void dragOver(DropTargetDragEvent e) { }
		public void dragEnter(DropTargetDragEvent e) {
			e.acceptDrag(DnDConstants.ACTION_COPY);
		}
		public void dragExit(DropTargetEvent e) { }
		public void dropActionChanged(DropTargetDragEvent e) { }
	}

	public static void registerDropTarget(Component component) {
		if (component instanceof FileDialog)
			new DropTarget(component,
				new DropListener((FileDialog)component));
		else
			IJ.log("Warning: not a FileDialog: " + component);
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
