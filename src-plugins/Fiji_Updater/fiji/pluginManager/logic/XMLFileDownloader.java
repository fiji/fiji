package fiji.pluginManager.logic;

import fiji.pluginManager.utilities.Downloader;
import fiji.pluginManager.utilities.Compressor;
import fiji.pluginManager.utilities.Downloader.FileDownload;
import fiji.pluginManager.utilities.PluginData;

import ij.Prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.List;

/*
 * Directly in charge of downloading and saving start-up files (i.e.: XML file and related).
 */
public class XMLFileDownloader extends PluginDataObservable implements Downloader.DownloadListener {
	private List<FileDownload> sources;
	private long xmlLastModified;
	private byte[] data;
	private PluginData util;
	private String url;

	public XMLFileDownloader(PluginData util) {
		this(util, PluginManager.MAIN_URL);
	}

	public XMLFileDownloader(PluginData util, String url) {
		this.util = util;
		this.url = url;
	}

	public void startDownload() throws IOException {
		sources = new ArrayList<FileDownload>();

		//Get last recorded date of XML file's modification
		long dateRecorded = 0;
		try {
			dateRecorded = Long.parseLong(Prefs.get(PluginManager.PREFS_XMLDATE, "0"));
		} catch (Exception e) {
			System.out.println("Warning: " + PluginManager.PREFS_XMLDATE + " contains invalid value.");
			dateRecorded = 0;
		}
		//From server, record modified date of XML for uploading purposes (Check lock conflict)
		String xml_url = url + PluginManager.XML_COMPRESSED;
		try {
			URLConnection myConnection = new URL(xml_url).openConnection();
			myConnection.setUseCaches(false);
			xmlLastModified = myConnection.getLastModified();
		} catch (Exception ex) {
			throw new Error("Failed to get last modified date of XML document");
		}

		//Download XML file either when it has been modified again, or if local version does not exist
		if (dateRecorded != xmlLastModified ||
				!new File(util.prefix(PluginManager.XML_COMPRESSED)).exists()) {
			//Record new last modified date of XML, and then select to download XML
			Prefs.set(PluginManager.PREFS_XMLDATE, "" + xmlLastModified);
			addToDownload(xml_url, PluginManager.XML_COMPRESSED);
		}

		//Start downloading the required files
		Downloader downloader = new Downloader(sources.iterator());
		downloader.addListener(this);
		downloader.startDownload();

		//Uncompress the XML file
		String compressedFileLocation = util.prefix(PluginManager.XML_COMPRESSED);
		data = Compressor.getDecompressedData(
				new FileInputStream(compressedFileLocation));

		setStatusComplete(); //indicate to observer there's no more tasks
	}

	//should be called only after all tasks in class are done
	public byte[] getXMLFileData() {
		return data;
	}

	//should be called only after all tasks in class are done
	public long getXMLLastModified() {
		return xmlLastModified;
	}

	private void addToDownload(String url, String filename) {
		System.out.println("To download: " + filename);
		sources.add(new InformationSource(filename, url, util.prefix(filename)));
	}

	private class InformationSource implements FileDownload {
		private String destination;
		private String url;
		private String filename;

		public InformationSource(String filename, String url, String destination) {
			this.filename = filename;
			this.destination = destination;
			this.url = url;
		}

		public String getDestination() {
			return destination;
		}

		public String getURL() {
			return url;
		}

		public String getFilename() {
			return filename;
		}
	}

	public void fileComplete(FileDownload source) {}

	public void update(FileDownload source, int bytesSoFar, int bytesTotal) {
		InformationSource src = (InformationSource)source;
		changeStatus(src.getFilename(), bytesSoFar, bytesTotal);
	}

	public void fileFailed(FileDownload source, Exception e) {
		try {
			new File(source.getDestination()).delete();
		} catch (Exception e2) { }
		throw new Error("Failed to save from " + source.getURL() + ", " +
				e.getLocalizedMessage());
	}
}
