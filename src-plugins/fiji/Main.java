package fiji;

import ij.IJ;
import ij.ImageJ;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.image.ImageProducer;
import java.awt.Toolkit;

import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;

import java.net.URL;

import java.util.WeakHashMap;

public class Main implements AWTEventListener {
	protected WeakHashMap allFrames = new WeakHashMap();
	protected Image icon;
	protected boolean debug;

	public void eventDispatched(AWTEvent event) {
		if (debug)
			System.err.println("event " + event + " from source "
					+ event.getSource());
		Object source = event.getSource();
		// only interested in frames
		if (!(source instanceof Frame))
			return;
		Frame frame = (Frame)source;
		switch (event.getID()) {
		case WindowEvent.WINDOW_ACTIVATED:
			if (!allFrames.containsKey(source)) {
				if (source instanceof ij.ImageJ)
					frame.setTitle("Fiji");
				setIcon(frame);
				allFrames.put(source, null);
			}
			break;
		case WindowEvent.WINDOW_CLOSED:
			allFrames.remove(source);
			break;
		}
	}

	public Image setIcon(Frame frame) {
		URL url = null;
		if (icon == null) try {
			url = getClass().getResource("/icon.png");
			ImageProducer ip = (ImageProducer)url.getContent();
			icon = frame.createImage(ip);
		} catch (Exception e) {
			IJ.error("Could not set the icon: '" + url + "'");
			e.printStackTrace();
			return null;
		}
		if (frame != null)
			frame.setIconImage(icon);
		return icon;
	}

	public static void premain() {
		Toolkit.getDefaultToolkit().addAWTEventListener(new Main(), -1);
	}

	public static void scanUserPlugins() {
		if (IJ.getInstance() != null)
			new User_Plugins().run(null);
	}

	public static void postmain() { }

	public static void main(String[] args) {
		premain();
		// prepend macro call to scanUserPlugins()
		String[] newArgs = new String[args.length + 2];
		newArgs[0] = "-eval";
		newArgs[1] = "call('fiji.Main.scanUserPlugins');";
		if (args.length > 0)
			System.arraycopy(args, 0, newArgs, 2, args.length);
		ImageJ.main(newArgs);
		postmain();
	}
}
