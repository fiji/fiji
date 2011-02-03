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
public class Downloader extends Progressable {
	protected int count, total, itemCount, itemTotal;
	protected long lastModified;

	protected String error;
	protected boolean cancelled;

	public Downloader() { }

	public Downloader(Progress progress) {
		this();
		addProgress(progress);
	}

	public synchronized void cancel() {
		cancelled = true;
	}

	public synchronized void start(final FileDownload justOne)
			throws IOException {
		start(new OneItemIterable<FileDownload>(justOne));
	}

	public void start(Iterable<FileDownload> files) throws IOException {
		Util.useSystemProxies();
		cancelled = false;

		count = total = itemCount = itemTotal = 0;
		for (FileDownload file : files) {
			total += file.getFilesize();
			itemTotal++;
		}

		setTitle("Downloading...");

		for (FileDownload current : files) {
			if (cancelled)
				break;
			download(current);
		}
		done();
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
		addItem(current);

		File parentDirectory = new File(destination).getParentFile();
		if (parentDirectory != null)
			parentDirectory.mkdirs();
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
			setCount(this.count, total);
			setItemCount(currentCount, currentTotal);
		}
		in.close();
		out.close();
		itemDone(current);
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
