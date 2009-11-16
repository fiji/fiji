package fiji.updater.logic;

import fiji.updater.Updater;

import fiji.updater.util.Downloader;
import fiji.updater.util.Compressor;
import fiji.updater.util.Downloader.FileDownload;
import fiji.updater.util.Progress;
import fiji.updater.util.Util;

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
	protected long xmlLastModified, previousLastModified;
	protected byte[] data;
	protected String destination, url;

	public XMLFileDownloader() {
		this(Updater.MAIN_URL);
	}

	public XMLFileDownloader(String urlPrefix) {
		url = urlPrefix + Updater.XML_COMPRESSED;
		destination = Util.prefix(Updater.XML_COMPRESSED);
		try {
			previousLastModified =
				new File(destination).lastModified();
		} catch (Exception e) {
			previousLastModified = 0;
		}
	}

	class LastModifiedSetter implements Progress {
		public void addItem(Object item) {
			xmlLastModified = getLastModified();
		}

		public void setTitle(String title) {}
		public void setCount(int count, int total) {}
		public void setItemCount(int count, int total) {}
		public void itemDone(Object item) {}
		public void done() {}
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
		new File(destination).setLastModified(xmlLastModified);
	}

	public InputStream getInputStream() {
		return new ByteArrayInputStream(data);
	}

	public long getXMLLastModified() {
		return xmlLastModified;
	}

	public long getPreviousLastModified() {
		return previousLastModified;
	}
}
