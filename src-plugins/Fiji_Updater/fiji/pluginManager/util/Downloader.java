package fiji.pluginManager.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URLConnection;
import java.net.URL;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

/*
 * Direct responsibility: Download a list of files given their respective URLs to their
 * respective destinations. Updates its download status to its Observer as well.
 */
public class Downloader extends Observable {
	protected int downloadedBytes, totalBytes;
	protected long lastModified;
	protected FileDownload current;

	protected String error;
	protected boolean cancelled, complete;

	public Downloader() { }

	public Downloader(Observer observer) {
		addObserver(observer);
	}

	public synchronized void cancel() {
		cancelled = true;
	}

	public synchronized void start(Iterable<FileDownload> files) {
		cancelled = false;
		error = null;

		downloadedBytes = totalBytes = 0;
		for (FileDownload file : files)
			totalBytes += file.getFilesize();

		try {
			for (FileDownload current : files) {
				this.current = current;
				if (cancelled)
					return;
				downloadCurrent();
			}
		} catch (Exception error) {
			error(error.getMessage());
		}
	}

	public static Iterator<FileDownload>
			iterator(final FileDownload justOne) {
		return new Iterator<FileDownload>() {
			boolean isFirst = true;

			public boolean hasNext() { return isFirst; }

			public FileDownload next() {
				isFirst = false;
				return justOne;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public synchronized void start(final FileDownload justOne) {
		start(new Iterable<FileDownload>() {
			public Iterator<FileDownload> iterator() {
				return Downloader.iterator(justOne);
			}
		});
	}

	protected synchronized void downloadCurrent() throws Exception {
		complete = false;
		URLConnection connection =
			new URL(current.getURL()).openConnection();
		connection.setUseCaches(false);
		lastModified = connection.getLastModified();

		String destination = current.getDestination();
		new File(destination).getParentFile().mkdirs();
		InputStream in = connection.getInputStream();
		OutputStream out = new FileOutputStream(destination);

		byte[] buffer = new byte[65536];
		for (;;) {
			if (cancelled)
				break;
			int count = in.read(buffer);
			if (count < 0)
				break;
			out.write(buffer, 0, count);
			downloadedBytes += count;
			setChanged();
			notifyObservers();
		}
		complete = true;
		setChanged();
		notifyObservers();
		in.close();
		out.close();
	}

	protected void error(String message) {
		new File(current.getDestination()).delete();
		error = message;
		setChanged();
		notifyObservers();
	}

	public boolean hasError() {
		return error != null;
	}

	public String getError() {
		return error;
	}

	public boolean isFileComplete() {
		return complete;
	}

	public FileDownload getCurrent() {
		return current;
	}

	public int getDownloadedBytes() {
		return downloadedBytes;
	}

	public int getTotalBytes() {
		return totalBytes;
	}

	public long getLastModified() {
		return lastModified;
	}

	public interface FileDownload {
		public String getDestination();
		public String getURL();
		public long getFilesize();
	}
}
