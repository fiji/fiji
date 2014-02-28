package fiji;

import fiji.gui.FileDialogDecorator;
import fiji.gui.JFileChooserDecorator;
import fiji.patches.FijiInitializer;
import ij.IJ;
import ij.ImageJ;
import imagej.patcher.LegacyEnvironment;

import java.awt.Image;
import java.awt.Toolkit;
import java.lang.reflect.Field;

import org.scijava.Context;

/**
 * Main entry point into Fiji.
 * 
 * @author Johannes Schindelin
 */
public class Main {
	protected Image icon;
	protected boolean debug;

	static {
		new IJ1Patcher().run();
	}

	/**
	 * @deprecated Use {@link FijiTools#runUpdater()} instead
	 */
	public static void runUpdater() {
		FijiTools.runUpdater();
	}

	/**
	 * Runs the command associated with a menu label if there is one.
	 *
	 * @param menuLabel the label of the menu item to run
	 * @deprecated Use {@link FijiTools#runGently(String)} instead
	 */
	public static void runGently(String menuLabel) {
		FijiTools.runGently(menuLabel);
	}

	/**
	 * Runs the command associated with a menu label if there is one.
	 *
	 * @param menuLabel the label of the menu item to run
	 * @param arg the arg to pass to the plugin's run() (or setup()) method
	 * @deprecated Use {@link FijiTools#runGently(String,String)} instead
	 */
	public static void runGently(String menuLabel, final String arg) {
		FijiTools.runGently(menuLabel, arg);
	}

	/** @deprecated use {@link FijiTools#runPlugInGently(String, String)} instead */
	public static void gentlyRunPlugIn(String className, String arg) {
		FijiTools.runPlugInGently(className, arg);
	}

	/**
	 * Runs a plug-in with an optional argument.
	 * 
	 * @param className the plugin class
	 * @param arg the argument (use "" if you do not want to pass anything)
	 * @deprecated Use {@link FijiTools#runPlugInGently(String,String)} instead
	 */
	public static void runPlugInGently(String className, String arg) {
		FijiTools.runPlugInGently(className, arg);
	}

	public static void installRecentCommands() {
		FijiTools.runPlugInGently("fiji.util.Recent_Commands", "install");
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

	/**
	 * Runs before {@code ImageJ.main()} is called.
	 * 
	 * @deprecated use a {@code LegacyInitializer} instead.
	 */
	public static void premain() {
		FileDialogDecorator.registerAutomaticDecorator();
		JFileChooserDecorator.registerAutomaticDecorator();
		setAWTAppClassName(Main.class);
	}

	/**
	 * This method is called after ImageJ was set up, but before the
	 * command line arguments are parsed.
	 * 
	 * @deprecated this task is performed by {@link fiji.patches.FijiInitializer} now.
	 */
	public static void setup() {
		FijiTools.runPlugInGently("fiji.util.RedirectErrAndOut", null);
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

					FijiTools.runUpdater();
				}
			}.start();
			new IJ_Alt_Key_Listener().run();
		}
	}

	@Deprecated
	public static void postmain() { }

	public static void main(String[] args) {
		if (IJ1Patcher.ij1PatcherFound) try {
			System.setProperty("ij1.patcher.initializer", FijiInitializer.class.getName());
			LegacyEnvironment.getPatchedImageJ1().main(args);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} else {
			legacyMain(args);
		}
	}

	/**
	 * The legacy way to start Fiji.
	 * 
	 * @param args the main args
	 * 
	 * @deprecated see {@link LegacyEnvironment}
	 */
	public static void legacyMain(String[] args) {
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
