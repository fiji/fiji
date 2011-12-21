package fiji.gui;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
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

import java.io.File;

import java.util.WeakHashMap;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

public class JFileChooserDecorator implements DropTargetListener {
	JFileChooser fileChooser;
	protected static WeakHashMap<JFileChooser, JFileChooserDecorator> allJFileChoosers
		= new WeakHashMap<JFileChooser, JFileChooserDecorator>();

	protected JFileChooserDecorator(JFileChooser fileChooser) {
		this.fileChooser = fileChooser;

	}

	/*
	 * automatic decorator: listen for all just-opened JFileChoosers,
	 * and decorate them right away.
	 */
	static class AutomaticDecorator implements AWTEventListener {
		public void eventDispatched(AWTEvent e) {
			Object source = e.getSource();
			if (e.getID() != ContainerEvent.COMPONENT_ADDED ||
					!(source instanceof JFileChooser))
				return;

			((ContainerEvent)e).getChild().setDropTarget(null);
			synchronized(this) {
				if (allJFileChoosers.containsKey(source))
					return;
				allJFileChoosers.put((JFileChooser)source, null);
			}
			JFileChooserDecorator decorator = new JFileChooserDecorator((JFileChooser)source);
			new DropTarget((JFileChooser)source, decorator);
			allJFileChoosers.put((JFileChooser)source, decorator);
			decorator.removeDropTargetsWithDelay(8);
		}
	}

	public static void registerAutomaticDecorator() {
		Toolkit.getDefaultToolkit().addAWTEventListener(new AutomaticDecorator(), AWTEvent.CONTAINER_EVENT_MASK);
	}

	static void removeDropTargetsFromChildren(Container container) {
		for (Component component : container.getComponents()) {
			component.setDropTarget(null);
			if (component instanceof Container)
				removeDropTargetsFromChildren((Container)component);
		}
	}

	void removeDropTargetsWithDelay(final int count) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (count > 0) {
					removeDropTargetsWithDelay(count - 1);
					return;
				}
				removeDropTargetsFromChildren(fileChooser);
			}
		});
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

		/* seems not to occur on Linux
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
					fileChooser.setCurrentDirectory(file);
				else {
					fileChooser.setCurrentDirectory(file.getParentFile());
					fileChooser.setSelectedFile(file);
				}
				/*
				 * This is a hack. When the current directory
				 * is set using the setCurrentDirectory()
				 * method, Swing insists to set an
				 * (incompatible) DropTarget. It does that so
				 * late that we have to invokeLater() >4 times
				 * and remove that DropTarget only then.
				 */
				removeDropTargetsWithDelay(4);
			}
                } catch (Exception e) {
                        e.printStackTrace();
                }
	}
	public void dragOver(DropTargetDragEvent e) { }
	public void dragEnter(DropTargetDragEvent e) {
		removeDropTargetsWithDelay(8);
		e.acceptDrag(DnDConstants.ACTION_COPY);
	}
	public void dragExit(DropTargetEvent e) { }
	public void dropActionChanged(DropTargetDragEvent e) { }

	/* convenience function to start it in the Script Editor */

	public static void main(String[] args) {
		registerAutomaticDecorator();
		report("JFileChooser decorator started");
		report("========================");
		new JFileChooser().showOpenDialog(null);
	}

	public static void report(String message) {
		if (!message.endsWith("\n"))
			message += "\n";
		System.err.println(message);
	}
}
