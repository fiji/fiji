package fiji;

import fiji.gui.FileDialogDecorator;
import fiji.gui.JFileChooserDecorator;

import ij.IJ;
import ij.ImageJ;

import ij.plugin.PlugIn;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Label;
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
			if (window != null && title.equals(getTitle(window)))
				return window;
		return null;
	}

	public static Window waitForWindow(String title, long timeout) {
		synchronized (title) {
			synchronized (all) {
				Window window = getWindow(title);
				if (window != null || timeout == 0)
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

	/**
	 * For a given (visible) component, return a "component path"
	 *
	 * The path describes recursively in which container the component is,
	 * possibly taking labels into account. A typical path looks like this:
	 *
	 *	Some Dialog>java.awt.Panel[2]>java.awt.TextField{Name:}
	 *
	 * If the component cannot be described (e.g. when the window is not
	 * visible yet, or when the component is in the process of being added
	 * to its container, this function returns null.
	 */
	public static String getPath(Component component) {
		String path = "";
		for (;;) {
			if (component instanceof Window)
				return getTitle((Window)component) + path;

			Container parent = component.getParent();
			if (parent == null)
				return null;
			Class componentClass = component.getClass();
			int index = 0, sameComponent = 0;
			Set<String> labels = new HashSet<String>();
			Label lastLabel = null;
			for (Component item : parent.getComponents()) {
				if (item instanceof Label) {
					lastLabel = (Label)item;
					String txt = lastLabel.getText();
					if (labels.contains(txt) ||
							txt.indexOf('>') >= 0 ||
							txt.indexOf("}") >= 0 ||
							txt.startsWith("["))
						lastLabel = null;
					else
						labels.add(txt);
					sameComponent = 1;
				}
				if (item == component) {
					path = ">" + componentClass.getName() +
						(lastLabel == null ?
						 "[" + index + "]" :
						 "{" + lastLabel.getText() + "}"
						 + (sameComponent > 1 ?
							 "[" + sameComponent
							 + "]" : "")) + path;
					component = parent;
					break;
				}
				if (item.getClass() == componentClass) {
					index++;
					sameComponent++;
				}
			}
			if (parent != component)
				return null;
		}
	}

	public static Component getComponent(String path) {
		return getComponent(path, -1);
	}

	/**
	 * Return a component for a given component path
	 *
	 * If the respective window is not visible, wait for the given number
	 * of milliseconds (or forever, if timeout == -1)
	 */
	public static Component getComponent(String path, long timeout) {
		if (path == null)
			return null;
		String[] list = path.split(">");
		Component component = waitForWindow(list[0], timeout);
		if (component == null)
			return null;

		for (int i = 1; i < list.length; i++) {
			Container parent = (Container)component;

			int bracket = list[i].indexOf('[');
			int bracket2 = list[i].indexOf('{');
			if (bracket == -1 || (bracket2 != -1 && bracket2 < bracket))
				bracket = bracket2;

			String componentClass = list[i].substring(0, bracket);
			if (bracket == bracket2) {
				int end = list[i].indexOf('}', bracket2);
				String txt = list[i].substring(bracket2 + 1, end);
				int sameComponent = 1;
				if (end + 1 < list[i].length()) {
					if (list[i].charAt(end + 1) != '[' ||
							!list[i].endsWith("]"))
						throw new RuntimeException(
							"Internal error");
					String num = list[i].substring(end + 2,
						list[i].length() - 1);
					sameComponent = Integer.parseInt(num);
				}
				component = null;
				for (Component item : parent.getComponents()) {
					if (txt != null) {
						if ((item instanceof Label) &&
								((Label)item)
								.getText()
								.equals(txt))
							txt = null;
					}
					if (!componentClass.equals(item
							.getClass().getName()) ||
							--sameComponent > 0)
						continue;
					component = item;
					break;
				}
			}
			else {
				int end = list[i].indexOf(']', bracket);
				int index = Integer.parseInt(list[i]
					.substring(bracket + 1, end));

				for (Component item : parent.getComponents()) {
					if (!componentClass.equals(
							item.getClass().getName()))
						continue;

					if (index > 0)
						index--;
					else {
						component = item;
						break;
					}
				}
			}

			if (component == null) {
				throw new RuntimeException("Component "
					+ path + " not found");
			}
		}
		return component;
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
			System.err.println("Could not set the icon: '"
					+ url + "'");
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
		new User_Plugins().run(null);
		if (IJ.getInstance() != null) {
			new Thread() {
				public void run() {
					/*
					 * Do not run updater when command line
					 * parameters were specified.
					 * Fiji automatically adds -eval ...
					 * and -port7, so there should be at
					 * least 3 parameters anyway.
					 */
					String[] ijArgs = ImageJ.getArgs();
					if (ijArgs != null && ijArgs.length > 3)
						return;

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
