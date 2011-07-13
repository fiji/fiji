package fiji.color;

import ij.IJ;

import ij.plugin.PlugIn;

import ij.process.ColorProcessor;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.awt.image.BufferedImage;

/**
 * Convert all reds to magentas in the system clipboard (to help red-green blind viewers)
 */
public class Convert_Red_To_Magenta_Clipboard implements PlugIn {
	public void run(String arg) {
		processSystemClipboard();
	}

	public static void processSystemClipboard() {
		ColorProcessor ip = getClipboardImage();
		if (ip == null)
			return;
		Convert_Red_To_Magenta.process(ip);
		copyImageToClipboard(ip);
	}

	public static ColorProcessor getClipboardImage() {
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable transferable = clipboard.getContents(null);
			boolean imageSupported = transferable.isDataFlavorSupported(DataFlavor.imageFlavor);
			if (!imageSupported)
				return null;
			Image img = (Image)transferable.getTransferData(DataFlavor.imageFlavor);
			if (img == null)
				return null;
			int width = img.getWidth(null);
			int height = img.getHeight(null);
			BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics g = bi.createGraphics();
			g.drawImage(img, 0, 0, null);
			g.dispose();
			return new ColorProcessor(bi);
		} catch (Throwable e) {
			IJ.handleException(e);
			return null;
		}
	}

	public static void copyImageToClipboard(final ColorProcessor ip) {
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new Transferable() {
				public DataFlavor[] getTransferDataFlavors() {
					return new DataFlavor[] { DataFlavor.imageFlavor };
				}

				public boolean isDataFlavorSupported(DataFlavor flavor) {
					return DataFlavor.imageFlavor.equals(flavor);
				}

				public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
					if (!isDataFlavorSupported(flavor))
						throw new UnsupportedFlavorException(flavor);
					int w = ip.getWidth();
					int h = ip.getHeight();
					Image img = IJ.getInstance().createImage(w, h);
					Graphics g = img.getGraphics();
					g.drawImage(ip.createImage(), 0, 0, null);
					g.dispose();
					return img;
				}
			}, null);
		} catch (Throwable e) {
			IJ.handleException(e);
		}
	}
}