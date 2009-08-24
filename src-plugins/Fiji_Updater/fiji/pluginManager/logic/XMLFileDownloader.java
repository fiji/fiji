package fiji.pluginManager.logic;

import fiji.pluginManager.util.Downloader;
import fiji.pluginManager.util.Compressor;
import fiji.pluginManager.util.Downloader.FileDownload;
import fiji.pluginManager.util.Util;

import ij.Prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Observable;
import java.util.Observer;

/*
 * Directly in charge of downloading and saving start-up files (i.e.: XML file
 * and related).
 */
public class XMLFileDownloader extends PluginDataObservable implements Observer, Downloader.FileDownload {
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

	public void start() throws IOException {
		Downloader downloader = new Downloader(this);
		downloader.start(this);
		data = Compressor.decompress(new FileInputStream(destination));
		done();
	}

	public String getDestination() {
		return destination;
	}

	public String getURL() {
		return url;
	}

	public long getFilesize() {
		return -1;
	}

	public byte[] getXMLFileData() {
		return data;
	}

	public long getXMLLastModified() {
		return xmlLastModified;
	}

	public void update(Observable observable, Object arg) {
		Downloader downloader = (Downloader)observable;
		if (downloader.hasError())
			throw new RuntimeException(downloader.getError());

		xmlLastModified = downloader.getLastModified();
		Prefs.set(PluginManager.PREFS_XMLDATE, "" + xmlLastModified);

		Downloader.FileDownload current = downloader.getCurrent();
		progress(current.getDestination(),
				downloader.getDownloadedBytes(),
				downloader.getTotalBytes());
	}
}
