package fiji.updater;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URL;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import ij.IJ;
import ij.plugin.PlugIn;

public class Updater implements PlugIn {
	public final static String UPDATER_CLASS_NAME = "imagej.updater.gui.ImageJUpdater";
	public final static String UPTODATE_CLASS_NAME = "imagej.updater.core.UpToDate";
	public final static String REMOTE_URL = "http://update.imagej.net/bootstrap.js";
	public final static String RHINO_CLASS_NAME = "org.mozilla.javascript.Context";

	public void run(String arg) {
		if ("check".equals(arg)) {
			check();
			return;
		}

		try {
			@SuppressWarnings("unchecked")
			final Class<Runnable> runnable = (Class<Runnable>) IJ.getClassLoader().loadClass(UPDATER_CLASS_NAME);
			runnable.newInstance().run();
		} catch (Throwable t) {
			t.printStackTrace();
			runRemote();
		}
	}

	private void check() {
		try {
			final Class<?> clazz = IJ.getClassLoader().loadClass(UPTODATE_CLASS_NAME);
			final Method check = clazz.getMethod("check");
			final Object result = check.invoke(null);
			if (result != null && "UPDATEABLE".equals(result.toString())) {
				if (IJ.showMessageWithCancel("Updates available",
						"There are updates available. Run the updater?")) {
					run("");
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			if (IJ.showMessageWithCancel("Updater problem",
					"There was a problem checking whether everything is up-to-date.\n" +
					"Start the updater?")) {
				runRemote();
			}
		}
	}

	private void runRemote() {
		try {
			System.err.println("Falling back to remote updater at " + REMOTE_URL);
			final URL url = new URL(REMOTE_URL);
			final Reader reader = new InputStreamReader(url.openStream());
			try {
				ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
				ScriptEngine engine = scriptEngineManager.getEngineByName("ECMAScript");
				engine.eval("importPackage(Packages.java.lang);");
				engine.eval(reader);
			} catch (final Throwable t) {
				t.printStackTrace();
				IJ.run("URL...", "url=[" + REMOTE_URL + "]");
			}
		} catch (final Throwable t) {
			IJ.handleException(t);
		}
	}

	public static void main(String[] args) {
		new Updater().run("");
	}
}