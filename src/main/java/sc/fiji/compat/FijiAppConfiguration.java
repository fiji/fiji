/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2007 - 2025 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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
