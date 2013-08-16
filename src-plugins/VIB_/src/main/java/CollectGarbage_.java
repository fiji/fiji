import ij.ImageListener;
import ij.ImagePlus;

import ij.plugin.PlugIn;

public class CollectGarbage_ implements PlugIn {
	public void run(String arg) {
		if ("onclose".equals(arg))
			registerCloseListener();
		else
			run();
	}

	public static void registerCloseListener() {
		ImagePlus.addImageListener(new ImageListener() {
			public void imageOpened(ImagePlus image) {}
			public void imageUpdated(ImagePlus image) {}
			public void imageClosed(ImagePlus image) {
				run();
			}
		});
	}

	public static void run() {
		System.gc();
		System.gc();
	}
}

