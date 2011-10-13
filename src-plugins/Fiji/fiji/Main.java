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

public class Main {
	protected Image icon;
	protected boolean debug;

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
		String headless = System.getProperty("java.awt.headless");
		if ("true".equalsIgnoreCase(headless))
			new Headless().run();
		new IJHacker().run();
		try {
			JavassistHelper.defineClasses();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
