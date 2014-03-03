package fiji.patches;

import imagej.legacy.plugin.LegacyAppConfiguration;
import imagej.util.AppUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.scijava.plugin.Plugin;

@Plugin(type = LegacyAppConfiguration.class)
public class FijiAppConfiguration implements LegacyAppConfiguration {

	final private static String appName = "(Fiji Is Just) ImageJ";
	final private static URL iconURL;

	static {
		URL url;
		try {
			final File file = new File(AppUtils.getBaseDirectory(), "images/icon.png");
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
