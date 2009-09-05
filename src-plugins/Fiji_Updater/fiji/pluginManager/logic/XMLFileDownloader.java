package fiji.pluginManager.logic;

import fiji.pluginManager.util.Downloader;
import fiji.pluginManager.util.Compressor;
import fiji.pluginManager.util.Downloader.FileDownload;
import fiji.pluginManager.util.Progress;
import fiji.pluginManager.util.Util;

import ij.Prefs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Observable;
import java.util.Observer;

/*
 * Directly in charge of downloading and saving start-up files (i.e.: XML file
 * and related).
 */
public class XMLFileDownloader extends Downloader {
	protected long xmlLastModified;
	protected byte[] data;
	protected String destination, url;

	public XMLFileDownloader() {
		this(PluginManager.MAIN_URL);
	}

	public XMLFileDownloader(String urlPrefix) {
		url = urlPrefix + PluginManager.XML_COMPRESSED;
		destination = Util.prefix(PluginManager.XML_COMPRESSED);
	}

	class LastModifiedSetter implements Progress {
		public void addItem(Object item) {
			xmlLastModified = getLastModified();
		}

		public void setTitle(String title) {}
		public void setCount(int count, int total) {}
		public void setItemCount(int count, int total) {}
	}
			
	public void start() throws IOException {
		addProgress(new LastModifiedSetter());
		start(new FileDownload() {
			public String toString() {
				return "Fiji Plugin Database";
			}

			public String getDestination() {
				return destination;
			}

			public String getURL() {
				return url;
			}

			public long getFilesize() {
				return 0;
			}
		});
		data = Compressor.decompress(new FileInputStream(destination));
		Prefs.set(PluginManager.PREFS_XMLDATE, "" + xmlLastModified);
	}

	public InputStream getInputStream() {
		return new ByteArrayInputStream(data);
	}

	public long getXMLLastModified() {
		return xmlLastModified;
	}
}
