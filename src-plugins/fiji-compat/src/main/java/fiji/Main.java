package fiji;

import fiji.gui.FileDialogDecorator;
import fiji.gui.JFileChooserDecorator;
import ij.IJ;
import ij.ImageJ;
import ij.Menus;
import ij.plugin.PlugIn;

import java.awt.Image;
import java.awt.Toolkit;
import java.lang.reflect.Field;

import org.scijava.Context;

public class Main {
	protected Image icon;
	protected boolean debug;

	static {
		new IJ1Patcher().run();
	}

	public static void runUpdater() {
		System.setProperty("fiji.main.checksUpdaterAtStartup", "true");
		runPlugInGently("fiji.updater.UptodateCheck", "quick");
	}

	/**
	 * Runs the command associated with a menu label if there is one.
	 *
	 * @param menuLabel the label of the menu item to run
	 */
	public static void runGently(String menuLabel) {
		runGently(menuLabel, "");
	}

	/**
	 * Runs the command associated with a menu label if there is one.
	 *
	 * @param menuLabel the label of the menu item to run
	 * @param arg the arg to pass to the plugin's run() (or setup()) method
	 */
	public static void runGently(String menuLabel, final String arg) {
		String className = (String)Menus.getCommands().get(menuLabel);
		if (className != null)
			IJ.runPlugIn(className, null);
	}

	/** @deprecated use {@link #runPlugInGently(String, String)} instead */
	public static void gentlyRunPlugIn(String className, String arg) {
		runPlugInGently(className, arg);
	}

	/**
	 * Runs a plug-in with an optional argument.
	 * 
	 * @param className the plugin class
	 * @param arg the argument (use "" if you do not want to pass anything)
	 */
	public static void runPlugInGently(String className, String arg) {
		try {
			Class<?> clazz = IJ.getClassLoader()
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
		runPlugInGently("fiji.util.Recent_Commands", "install");
	}

	private static boolean setAWTAppClassName(Class<?> appClass) {
		String headless = System.getProperty("java.awt.headless");
		if ("true".equalsIgnoreCase(headless))
			return false;
		try {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			if (toolkit == null)
				return false;
			Class<?> clazz = toolkit.getClass();
			if (!"sun.awt.X11.XToolkit".equals(clazz.getName()))
				return false;
			Field field = clazz.getDeclaredField("awtAppClassName");
			field.setAccessible(true);
			field.set(toolkit, appClass.getName().replace('.', '-'));
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}

	public static void premain() {
		FileDialogDecorator.registerAutomaticDecorator();
		JFileChooserDecorator.registerAutomaticDecorator();
		setAWTAppClassName(Main.class);
	}

	/*
	 * This method will be called after ImageJ was set up, but before the
	 * command line arguments are parsed.
	 */
	public static void setup() {
		runPlugInGently("fiji.util.RedirectErrAndOut", null);
		new MenuRefresher().run();
		final Runnable getImageJContext = new Runnable() {
			@Override
			public void run() {
				IJ.runPlugIn(Context.class.getName(), null);
			}
		};
		if (IJ.getInstance() == null) {
			new Thread(getImageJContext).start();
		} else {
			// create an ImageJ2 context
			java.awt.EventQueue.invokeLater(getImageJContext);
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
