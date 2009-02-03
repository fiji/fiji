package fiji;

import ij.IJ;
import ij.ImageJ;

import java.awt.Image;
import java.awt.image.ImageProducer;

import java.net.URL;

public class SetIcon {
	public static void run(String title, String path) {
		try {
			URL url = new URL(path);
			if (url==null)
				return;
			ImageJ ij = IJ.getInstance();
			ImageProducer ip = (ImageProducer)url.getContent();
			Image img = ij.createImage(ip);
			if (img!=null) {
				ij.setTitle(title);
				ij.setIconImage(img);
			}
		} catch (Exception e) {
			IJ.error("Could not set the icon: '" + path + "'");
			e.printStackTrace();
		}
	}
}

