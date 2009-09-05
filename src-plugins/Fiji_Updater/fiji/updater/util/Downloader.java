package fiji.updater.util;

import java.io.File;
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
 * Direct responsibility: Download a list of files given their respective URLs
 * to their respective destinations. Updates its download status to its
 * Observer as well.
 */
public class Downloader {
	protected int count, total, itemCount, itemTotal;
	protected long lastModified;
	protected List<Progress> progress;

	protected String error;
	protected boolean cancelled;

	public Downloader() {
		progress = new ArrayList<Progress>();
	}

	public Downloader(Progress progress) {
		this();
		addProgress(progress);
	}

	public void addProgress(Progress progress) {
		this.progress.add(progress);
	}

	public void removeProgress(Progress progress) {
		this.progress.remove(progress);
	}

	public synchronized void cancel() {
		cancelled = true;
	}

	// TODO: refactor as OneItemIterable
	public static<T> Iterator<T> fakeIterator(final T justOne) {
		return new Iterator<T>() {
			boolean isFirst = true;

			public boolean hasNext() { return isFirst; }

			public T next() {
				isFirst = false;
				return justOne;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public synchronized void start(final FileDownload justOne)
			throws IOException {
		start(new Iterable<FileDownload>() {
			public Iterator<FileDownload> iterator() {
				return Downloader.fakeIterator(justOne);
			}
		});
	}

	public void start(Iterable<FileDownload> files) throws IOException {
		cancelled = false;

		count = total = itemCount = itemTotal = 0;
		for (FileDownload file : files) {
			total += file.getFilesize();
			itemTotal++;
		}

		for (Progress progress : this.progress)
			progress.setTitle("Downloading...");

		for (FileDownload current : files) {
			if (cancelled)
				return;
			download(current);
		}
	}

	protected synchronized void download(FileDownload current)
			throws IOException {
		URLConnection connection =
			new URL(current.getURL()).openConnection();
		connection.setUseCaches(false);
		lastModified = connection.getLastModified();
		int currentTotal = connection.getContentLength();
		if (currentTotal < 0)
			currentTotal = (int)current.getFilesize();

		String destination = current.getDestination();
		for (Progress progress : this.progress)
			progress.addItem(current);

		new File(destination).getParentFile().mkdirs();
		InputStream in = connection.getInputStream();
		OutputStream out = new FileOutputStream(destination);

		int currentCount = 0;
		int total = this.total;
		if (total == 0)
			total = (count + currentTotal) * itemTotal
				/ (itemCount + 1);

		byte[] buffer = new byte[65536];
		for (;;) {
			if (cancelled)
				break;
			int count = in.read(buffer);
			if (count < 0)
				break;
			out.write(buffer, 0, count);
			currentCount += count;
			this.count += count;
			for (Progress progress : this.progress) {
				progress.setCount(this.count, total);
				progress.setItemCount(currentCount,
						currentTotal);
			}

		}
		in.close();
		out.close();
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
