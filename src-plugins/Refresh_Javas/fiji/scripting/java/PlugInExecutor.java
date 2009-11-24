package fiji.scripting.java;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;

import ij.plugin.PlugIn;

import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;

import ij.text.TextWindow;

import ij.util.Tools;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
 * This class should have been public instead of being hidden in
 * ij/plugin/Compiler.java.
 */
public class PlugInExecutor {

	/** Create a new object that runs the specified plugin
		in a separate thread. */
	public void runThreaded(final String plugin) {
		Thread thread = new Thread() {
			public void run() {
				PlugInExecutor.this.run(plugin);
			}
		};
		thread.setPriority(Math.max(thread.getPriority()-2,
					Thread.MIN_PRIORITY));
		thread.start();
	}

	public void run(String plugin) {
		run(plugin, "");
	}

	public void run(String plugin, String arg) {
		try {
			IJ.resetEscape();
			ClassLoader classLoader = getClassLoader();
			Class clazz = classLoader.loadClass(plugin);
			Object object = clazz.newInstance();
			if (object instanceof PlugIn)
                                ((PlugIn)object).run(arg);
                        else if (object instanceof PlugInFilter)
                                new PlugInFilterRunner(object, plugin, arg);
			else
				runMain(object, arg);
		} catch(Throwable e) {
			IJ.showStatus("");
			IJ.showProgress(1.0);
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp!=null) imp.unlock();
			String msg = e.getMessage();
			if (e instanceof RuntimeException && msg!=null &&
					msg.equals(Macro.MACRO_CANCELED))
				return;
			CharArrayWriter caw = new CharArrayWriter();
			PrintWriter pw = new PrintWriter(caw);
			e.printStackTrace(pw);
			String s = caw.toString();
			if (IJ.isMacintosh())
				s = Tools.fixNewLines(s);
			new TextWindow("Exception", s, 350, 250);
		}
	}

	ClassLoader getClassLoader() {
		return IJ.getClassLoader();
	}

	void runMain(Object object, String arg) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		String[] args = new String[] { arg };
		Method main = object.getClass().getMethod("main",
				new Class[] { args.getClass() });
		main.invoke(object, (Object)args);
	}
}
