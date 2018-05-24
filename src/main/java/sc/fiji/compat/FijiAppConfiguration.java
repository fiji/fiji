package sc.fiji.compat;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import net.imagej.legacy.plugin.LegacyAppConfiguration;

import org.scijava.plugin.Plugin;
import org.scijava.util.AppUtils;

import fiji.Main;

@Plugin(type = LegacyAppConfiguration.class)
public class FijiAppConfiguration implements LegacyAppConfiguration {

	final private static String appName = "(Fiji Is Just) ImageJ";
	final private static URL iconURL;

	static {
		URL url;
		try {
			final File file = new File(AppUtils.getBaseDirectory(Main.class), "images/icon.png");
			url = file.exists() ? file.toURI().toURL() : null;
		} catch (MalformedURLException e) {
			url = null;
		} finally{}
		iconURL = url;
	}

	@Override
	public String getAppName() {
		return appName;
	}

	@Override
	public URL getIconURL() {
		return iconURL;
	}

}
