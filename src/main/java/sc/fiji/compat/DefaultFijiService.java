package sc.fiji.compat;

import ij.IJ;
import ij.ImageJ;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.lang.reflect.Field;

import org.scijava.event.EventHandler;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.service.event.ServicesLoadedEvent;

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

	public void actuallyInitialize() {
		FileDialogDecorator.registerAutomaticDecorator();
		JFileChooserDecorator.registerAutomaticDecorator();
		setAWTAppClassName("fiji-Main");
		final ImageJ ij = IJ.getInstance();
		if (ij != null) {
			new MenuRefresher().run();
			new Thread() {
				@Override
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
				}
			}.start();
		}
	}

	@EventHandler
	protected void onEvent(@SuppressWarnings("unused") ServicesLoadedEvent evt) {
		actuallyInitialize();
	}

	private static boolean setAWTAppClassName(String appName) {
		if (!GraphicsEnvironment.isHeadless())  try {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			if (toolkit == null)
				return false;
			Class<?> clazz = toolkit.getClass();
			if (!"sun.awt.X11.XToolkit".equals(clazz.getName()))
				return false;
			Field field = clazz.getDeclaredField("awtAppClassName");
			field.setAccessible(true);
			field.set(toolkit, appName);
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}
}
