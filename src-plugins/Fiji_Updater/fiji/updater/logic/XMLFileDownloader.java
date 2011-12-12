package fiji.updater.logic;

import fiji.updater.logic.PluginCollection.UpdateSite;

import fiji.updater.util.Downloader;
import fiji.updater.util.Compressor;
import fiji.updater.util.Downloader.FileDownload;
import fiji.updater.util.Progress;
import fiji.updater.util.Progressable;
import fiji.updater.util.Util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.net.URLConnection;

import java.util.Collection;
import java.util.Observable;
import java.util.Observer;

import java.util.zip.GZIPInputStream;

/*
 * Directly in charge of downloading and saving start-up files (i.e.: XML file
 * and related).
 */
public class XMLFileDownloader extends Progressable {
	protected PluginCollection plugins;
	protected Collection<String> updateSites;
	protected String warnings;

	public XMLFileDownloader(PluginCollection plugins) {
		this(plugins, plugins.getUpdateSiteNames());
	}

	public XMLFileDownloader(PluginCollection plugins, Collection<String> updateSites) {
		this.plugins = plugins;
		this.updateSites = updateSites;
	}

	public void start() throws IOException {
		setTitle("Updating the Fiji database");
		XMLFileReader reader = new XMLFileReader(plugins);
		int current = 0, total = updateSites.size();
		warnings = "";
		for (String name : updateSites) {
			UpdateSite updateSite = plugins.getUpdateSite(name);
			String title = "Updating from " + (name.equals("") ? "main" : name) + " site";
			addItem(title);
			setCount(current, total);
			try {
				URLConnection connection = new URL(updateSite.url + Util.XML_COMPRESSED).openConnection();
				long lastModified = connection.getLastModified();
				int fileSize = (int)connection.getContentLength();
				InputStream in = getInputStream(new GZIPInputStream(connection.getInputStream()), fileSize);
				reader.read(name, in, updateSite.timestamp);
				updateSite.setLastModified(lastModified);
			} catch (Exception e) {
				if (e instanceof FileNotFoundException)
					updateSite.setLastModified(0); // it was deleted
				e.printStackTrace();
				warnings += "Could not update from site '" + name + "': " + e;
			}
			itemDone(title);
		}
		done();
		warnings += reader.getWarnings();
	}

	public String getWarnings() {
		return warnings;
	}

	public InputStream getInputStream(final InputStream in, final int fileSize) {
		return new InputStream() {
			int current = 0;

			public int read() throws IOException {
				int result = in.read();
				setItemCount(++current, fileSize);
				return result;
			}

			public int read(byte[] b) throws IOException {
				int result = in.read(b);
				if (result > 0) {
					current += result;
					setItemCount(current, fileSize);
				}
				return result;
			}

			public int read(byte[] b, int off, int len) throws IOException {
				int result = in.read(b, off, len);
				if (result > 0) {
					current += result;
					setItemCount(current, fileSize);
				}
				return result;
			}
		};
	}
}
