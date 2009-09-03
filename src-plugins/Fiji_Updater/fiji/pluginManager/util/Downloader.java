package fiji.pluginManager.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
 * Direct responsibility: Download a list of files given their respective URLs to their
 * respective destinations. Updates its download status to its Observer as well.
 */
public class Downloader {
	private int downloadedBytes;
	private int downloadSize;
	private URLConnection connection;
	private List<DownloadListener> listeners;
	private InputStream in;
	private OutputStream out;
	private Iterator<FileDownload> sourceFiles;
	private FileDownload currentSource;
	private boolean cancelled; //stop download entirely

	public Downloader(Iterator<FileDownload> sourceFiles) {
		this.sourceFiles = sourceFiles;
		listeners = new ArrayList<DownloadListener>();
	}

	public synchronized void cancel() {
		cancelled = true;
	}

	public synchronized void start() {
		while (sourceFiles.hasNext() && !cancelled) {
			currentSource = sourceFiles.next();
			try {
				//Start connection
				connection = new URL(currentSource.getURL()).openConnection();
				connection.setUseCaches(false);
				downloadedBytes = 0; //start with nothing downloaded
				downloadSize = connection.getContentLength();
				if (downloadSize < 0)
					throw new Exception("Content Length is not known");
				notifyListenersUpdate(); //first notification starts from 0

				new File(currentSource.getDestination()).getParentFile().mkdirs();
				in = connection.getInputStream();
				out = new FileOutputStream(currentSource.getDestination());

				//Start actual downloading and writing to file
				byte[] buffer = new byte[65536];
				int count;
				while ((count = in.read(buffer)) >= 0 && !cancelled) {
					out.write(buffer, 0, count);
					downloadedBytes += count;
					notifyListenersUpdate();
				}
				//end connection once download done
				in.close();
				out.close();
				notifyListenersCompletion();

			} catch (FileNotFoundException e1) {
				notifyListenersError(e1);
			} catch (IOException e2) {
				notifyListenersError(e2);
			} catch (Exception e3) {
				notifyListenersError(e3);
			}
		}
	}

	private void notifyListenersUpdate() {
		for (DownloadListener listener : listeners) {
			listener.update(currentSource, downloadedBytes, downloadSize);
		}
	}

	private void notifyListenersCompletion() {
		for (DownloadListener listener : listeners) {
			listener.fileComplete(currentSource);
		}
	}

	private void notifyListenersError(Exception e) {
		for (DownloadListener listener : listeners) {
			listener.fileFailed(currentSource, e);
		}
	}

	public synchronized void addListener(DownloadListener listener) {
		listeners.add(listener);
	}

	public interface DownloadListener {
		public void update(FileDownload source, int bytesSoFar, int bytesTotal);
		public void fileComplete(FileDownload source);
		public void fileFailed(FileDownload source, Exception e);
	}

	public interface FileDownload {
		public String getDestination();
		public String getURL();
	}
}
