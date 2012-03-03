package fiji;

import ij.IJ;
import ij.ImageJ;

import java.awt.Image;
import java.awt.image.ImageProducer;

import java.net.URL;

public class SetIcon {
	public static void run(String title, String iconPath) {
		try {
			ImageJ ij = IJ.getInstance();
			if (ij == null)
				return;
			if (title != null)
				ij.setTitle(title);
			URL url = new URL(iconPath);
			if (url==null)
				return;
			ImageProducer ip = (ImageProducer)url.getContent();
			Image img = ij.createImage(ip);
			if (img!=null)
				ij.setIconImage(img);
		} catch (Exception e) {
			IJ.error("Could not set the icon: '" + iconPath + "'");
			e.printStackTrace();
		}
	}
}
