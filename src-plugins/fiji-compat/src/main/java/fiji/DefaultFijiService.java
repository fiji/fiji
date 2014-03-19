package fiji;

import static fiji.FijiTools.runPlugInGently;
import static fiji.FijiTools.runUpdater;
import ij.IJ;
import ij.ImageJ;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.lang.reflect.Field;

import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import fiji.gui.FileDialogDecorator;
import fiji.gui.JFileChooserDecorator;

/**
 * The default initializer for the Fiji legacy application.
 * <p>
 * This class initializes all the Fiji-specific hacks such as decorating the
 * FileDialog on Linux to allow easy keyboard navigation, supporting the Fiji
 * scripting framework, etc.
 * </p>
 * 
 * @author Johannes Schindelin
 */
@Plugin(type = Service.class)
public class DefaultFijiService extends AbstractService implements FijiService {

	@Override
	public void initialize() {
		FileDialogDecorator.registerAutomaticDecorator();
		JFileChooserDecorator.registerAutomaticDecorator();
		setAWTAppClassName(Main.class);
		runPlugInGently("fiji.util.RedirectErrAndOut", null);
		new MenuRefresher().run();
		final ImageJ ij = IJ.getInstance();
		if (ij != null) {
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

	private static boolean setAWTAppClassName(Class<?> appClass) {
		if (!GraphicsEnvironment.isHeadless())  try {
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
		}
		return false;
	}
}
