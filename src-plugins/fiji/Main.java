package fiji;

import ij.IJ;
import ij.ImageJ;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;

import java.awt.image.ImageProducer;

import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;

import java.net.URL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

public class Main implements AWTEventListener {
	protected static HashMap<Window, Object> all =
		new HashMap<Window, Object>();
	protected Image icon;
	protected boolean debug;

	public void eventDispatched(AWTEvent event) {
		if (debug)
			System.err.println("event " + event + " from source "
					+ event.getSource());
		Object source = event.getSource();
		// only interested in windows
		if (!(source instanceof Window))
			return;

		synchronized (all) {
			Window window = (Window)source;
			switch (event.getID()) {
			case WindowEvent.WINDOW_ACTIVATED:
				if (!all.containsKey(window)) {
					if (window instanceof ij.ImageJ)
						((Frame)window)
							.setTitle("Fiji");
					setIcon(window);
					all.put(window, this);
					notify(getTitle(window));
				}
				break;
			case WindowEvent.WINDOW_CLOSED:
				all.remove(source);
				break;
			}
		}
	}

	private static void notify(String windowTitle) {
		Iterator<String> iter = waiters.iterator();
		while (iter.hasNext()) {
			String title = iter.next();
			if (title.equals(windowTitle)) {
				synchronized (title) {
					title.notifyAll();
				}
				iter.remove();
			}
		}
	}

	private static String getTitle(Window window) {
		if (window instanceof Frame)
			return ((Frame)window).getTitle();
		if (window instanceof Dialog)
			return ((Dialog)window).getTitle();
		return null;
	}

	private static Set<String> waiters = new HashSet<String>();

	public static Window waitForWindow(String title) {
		return waitForWindow(title, -1);
	}

	public static Window getWindow(String title) {
		for (Window window : all.keySet())
			if (getTitle(window).equals(title))
				return window;
		return null;
	}

	public static Window waitForWindow(String title, long timeout) {
		synchronized (title) {
			synchronized (all) {
				Window window = getWindow(title);
				if (window != null)
					return window;
				waiters.add(title);
			}
			try {
				if (timeout < 0)
					title.wait();
				else
					title.wait(timeout);
				return getWindow(title);
			} catch (InterruptedException e) {
				return null;
			}
		}
	}

	public Image setIcon(Window window) {
		URL url = null;
		if (icon == null) try {
			url = getClass().getResource("/icon.png");
			ImageProducer ip = (ImageProducer)url.getContent();
			icon = window.createImage(ip);
		} catch (Exception e) {
			IJ.error("Could not set the icon: '" + url + "'");
			e.printStackTrace();
			return null;
		}
		if (window != null)
			window.setIconImage(icon);
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
