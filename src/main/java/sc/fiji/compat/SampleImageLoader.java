/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2007 - 2021 Fiji developers.
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

import ij.IJ;
import ij.ImagePlus;
import ij.Menus;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.plugin.URLOpener;

import java.awt.Menu;
import java.awt.MenuItem;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class SampleImageLoader implements PlugIn {
	protected final static String plugin = "ij.plugin.URLOpener";
	protected final static String thisPlugin = SampleImageLoader.class.getName();
	protected final static String menuPath = "File>Open Samples";
	protected final static String menuItemLabel = "Cache Sample Images";
	protected final static String ij1MenuItemLabel = "Cache Sample Images ";

	@Override
	public void run(String arg) {
		if ("cache".equals(arg)) {
			fetchSamples();
			return;
		}

		File cached = getCached(arg);
		if (cached != null) try {
			final ImagePlus imp = IJ.openImage(cached.getPath());
			if (imp != null) {
				switch (imp.getBitDepth()) {
				case 16:
					imp.resetDisplayRange();
					break;
				case 24:
					// *sigh* This is copied from the ImagePlus(String pathOrUrl) constructor:
					Opener.convertGrayJpegTo8Bits(imp);
					break;
				}
				imp.show();
				return;
			}
		} catch(Exception e) { e.printStackTrace(); }

		new URLOpener().run(arg);
	}

	protected static File getCached(String url) {
		return getCached(url, false);
	}

	protected static File getCached(String url, boolean evenIfNotExists) {
		String ijDir = System.getProperty("ij.dir");
		if (ijDir != null) {
			int slash = url.lastIndexOf('/');
			File file = new File(ijDir + "/samples",
					url.substring(slash + 1));
			if (evenIfNotExists || file.exists())
				return file;
		}
		return null;
	}

	protected interface SampleHandler {
		public void handle(String label, String url);
	}

	protected static void handleSamples(SampleHandler handler) {
		Menu menu = (Menu)FijiTools.getMenuItem(menuPath);
		if (menu == null)
			return;

		Hashtable<?, ?> commands = Menus.getCommands();
		for (int i = 0; i < menu.getItemCount(); i++) {
			String label = menu.getItem(i).getLabel();
			String command = (String)commands.get(label);
			String url = null;
			if (command != null && command.endsWith("\")") &&
					command.startsWith(plugin + "(\""))
				url = command.substring(plugin.length() + 2,
						command.length() - 2);
			else if (command != null && command.endsWith("\")") &&
					command.startsWith(thisPlugin + "(\""))
				url = command.substring(thisPlugin.length()
						+ 2, command.length() - 2);
			else
				continue;
			if (url.equals("cache"))
				continue;
			if (url.indexOf('/') < 0)
				url = IJ.URL + "/images/" + url;
			handler.handle(label, url);
		}
	}

	protected static class InstallHandler implements SampleHandler {
		protected Hashtable<String, String> commands;
		protected boolean hasUncached = false;

		public InstallHandler(Hashtable<String, String> commands) {
			this.commands = commands;
		}

		@Override
		public void handle(String label, String url) {
			commands.put(label, thisPlugin + "(\"" + url + "\")");
			 if (getCached(url) == null)
				hasUncached = true;
		}
	}

	public static void install() {
		@SuppressWarnings("unchecked")
		Hashtable<String, String> commands = Menus.getCommands();

		InstallHandler handler = new InstallHandler(commands);
		handleSamples(handler);

		if (!commands.containsKey(menuItemLabel)) {
			MenuItem item;
			if (commands.containsKey(ij1MenuItemLabel)) {
				item = FijiTools.getMenuItem(menuPath + ">" + ij1MenuItemLabel);
				commands.put(ij1MenuItemLabel, thisPlugin + "(\"cache\")");
				commands.put(menuItemLabel, thisPlugin + "(\"cache\")");
			}
			else {
				item = FijiTools.installPlugin(menuPath, menuItemLabel,
					thisPlugin + "(\"cache\")");
			}
			if (item != null)
				item.setEnabled(handler.hasUncached);
		}
	}

	public static void fetchSamples() {
		final List<String> urls = new ArrayList<>();
		handleSamples(new SampleHandler() {
			@Override
			public void handle(String label, String url) {
				if (getCached(url) == null)
					urls.add(url);
			}
		});

		if (urls.size() == 0)
			return;

		boolean logToStderr = IJ.getInstance() == null;
		try {
			for (int i = 0; i < urls.size(); i++)
				download(new URL(urls.get(i)).openConnection(),
					getCached(urls.get(i), true),
					i, urls.size(), logToStderr);
			FijiTools.getMenuItem(menuPath + ">" + menuItemLabel)
				.setEnabled(false);
		} catch (Exception e) {
			if (logToStderr)
				e.printStackTrace();
			else
				IJ.handleException(e);
		}
		if (logToStderr)
			System.err.println("");
		else
			IJ.showStatus("Downloaded " + urls.size() + " samples");
	}

	public static void download(URLConnection connection, File destination,
			int nr, int total, boolean logToStderr)
			throws IOException {
		String message = "Downloading " + (nr + 1) + "/" + total + ": "
			+ destination.getName();
		long length = connection.getContentLength(), totalRead = 0;
		if (logToStderr)
			System.err.print(message);
		else
			IJ.showStatus(message);
		byte[] buffer = new byte[16384];
		try (InputStream in = connection.getInputStream()) {
			File parent = destination.getParentFile();
			if (!parent.exists() && !parent.mkdirs())
				throw new IOException("Could not make directory " + parent);
			File tmp = File.createTempFile("sample-", ".sample", parent);
			try (FileOutputStream out = new FileOutputStream(tmp)) {
				for (;;) {
					int count = in.read(buffer);
					if (count < 0)
						break;
					out.write(buffer, 0, count);
					if (length < 0)
						continue;
					totalRead += count;
					if (logToStderr)
						System.err.print("\r" + message + " "
								+ totalRead + "/" + length);
					else
						IJ.showProgress((nr
								+ totalRead / (float)length) / total);
				}
			}
			if (destination.exists())
				destination.delete(); // bend over for Windows
			tmp.renameTo(destination);
		}
		if (!logToStderr)
			IJ.showProgress(nr + 1, total);
	}
}
