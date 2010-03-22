package fiji;

import fiji.gui.FileDialogDecorator;
import fiji.gui.JFileChooserDecorator;

import ij.IJ;
import ij.ImageJ;

import ij.plugin.PlugIn;

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

import java.lang.reflect.Method;

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
			if (window != null && getTitle(window).equals(title))
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

	/* Unfortunately, we have to support Java 1.5 because of MacOSX... */
	protected static Method setIconImage;
	static {
		try {
			Class window = Class.forName("java.awt.Window");
			Class image = Class.forName("java.awt.Image");
			setIconImage = window.getMethod("setIconImage",
				new Class[] { image });
		} catch (Exception e) { /* ignore, this is Java < 1.6 */ }
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
		if (window != null && setIconImage != null) try {
			setIconImage.invoke(window, icon);
		} catch (Exception e) { e.printStackTrace(); }
		return icon;
	}

	public static void runUpdater() {
		System.setProperty("fiji.main.checksUpdaterAtStartup", "true");
		gentlyRunPlugIn("fiji.updater.UptodateCheck", "quick");
	}

	public static void gentlyRunPlugIn(String className, String arg) {
		try {
			Class clazz = IJ.getClassLoader()
				.loadClass(className);
			if (clazz != null) {
				PlugIn plugin = (PlugIn)clazz.newInstance();
				plugin.run(arg);
			}
		}
		catch (ClassNotFoundException e) { }
		catch (InstantiationException e) { }
		catch (IllegalAccessException e) { }
	}

	public static void installRecentCommands() {
		gentlyRunPlugIn("fiji.util.Recent_Commands", "install");
	}

	public static void premain() {
		Toolkit.getDefaultToolkit().addAWTEventListener(new Main(), -1);
		FileDialogDecorator.registerAutomaticDecorator();
		JFileChooserDecorator.registerAutomaticDecorator();
	}

	/*
	 * This method will be called after ImageJ was set up, but before the
	 * command line arguments are parsed.
	 */
	public static void setup() {
		if (IJ.getInstance() != null) {
			new User_Plugins().run(null);
			SampleImageLoader.install();
			installRecentCommands();
			new Thread() {
				public void run() {
					runUpdater();
				}
			}.start();
			new IJ_Alt_Key_Listener().run();
		}
	}

	public static void postmain() { }

	public static void main(String[] args) {
		premain();
		// prepend macro call to scanUserPlugins()
		String[] newArgs = new String[args.length + 2];
		newArgs[0] = "-eval";
		newArgs[1] = "call('fiji.Main.setup');";
		if (args.length > 0)
			System.arraycopy(args, 0, newArgs, 2, args.length);
		ImageJ.main(newArgs);
		postmain();
	}
}
