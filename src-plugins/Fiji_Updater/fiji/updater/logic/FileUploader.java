package fiji.updater.logic;

import ij.IJ;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.List;

/*
 * This FileUploader is highly specialized to upload plugins and XML
 * information over to Pacific. There is a series of steps to follow. Any
 * exception means entire upload process is considered invalid.
 *
 * 1.) Set db.xml.gz to read-only
 * 2.) Verify db.xml.gz has not been modified, if not, upload process cancelled
 * 3.) Upload db.xml.gz.lock (Lock file, prevent others from writing it ATM)
 * 4.) Upload plugin files and current.txt
 * 5.) If all goes well, force rename db.xml.gz.lock to db.xml.gz
 */
public class FileUploader {
	protected final String uploadDir;
	protected List<UploadListener> listeners;
	protected SourceFile currentUpload;
	protected long uploadedBytes;
	protected long uploadSize;
	protected OutputStream out;
	protected InputStream in;

	public FileUploader() {
		this("/var/www/update/");
	}

	public FileUploader(String uploadDir) {
		this.uploadDir = uploadDir;
		listeners = new ArrayList<UploadListener>();
	}

	protected void setupTotalSize(List<SourceFile> sources) {
		uploadSize = 0;
		for (SourceFile source : sources)
			uploadSize += source.getFilesize();
	}

	//Steps to accomplish entire upload task
	public synchronized void upload(long xmlLastModified,
			List<SourceFile> sources) throws IOException {
		setupTotalSize(sources);

		File lock = null;
		File db = new File(uploadDir + PluginManager.XML_COMPRESSED);
		byte[] buffer = new byte[65536];
		for (SourceFile source : sources) {
			currentUpload = source;
			File file = new File(uploadDir + source.getFilename());
			File dir = file.getParentFile();
			if (!dir.exists())
				dir.mkdirs();
			out = new FileOutputStream(file);

			// The first file is special; it is the lock file
			if (lock == null) {
				lock = file;
				if (!lock.setReadOnly())
					throw new IOException("Could not mark "
						+ source.getFilename()
						+ " read-only!");
				if (xmlLastModified != db.lastModified()) {
					// TODO: SSHFileUploader must delete the lock here, too
					lock.delete();
					throw new IOException("Conflict: "
						+ db.getName()
						+ " has been modified");
				}
			}

			in = source.getInputStream();
			for (;;) {
				int count = in.read(buffer);
				if (count < 0)
					break;
				out.write(buffer, 0, count);
				uploadedBytes += count;
				notifyListenersUpdate();
			}
			in.close();
			out.close();
			notifyListenersFileComplete();
		}

		File backup = new File(db.getAbsolutePath() + ".old");
		if (backup.exists())
			backup.delete();
		db.renameTo(backup);
		lock.renameTo(db);
		notifyListenersCompletionAll();
	}

	protected void notifyListenersUpdate() {
		for (UploadListener listener : listeners)
			listener.update(currentUpload, uploadedBytes, uploadSize);
	}

	protected void notifyListenersCompletionAll() {
		for (UploadListener listener : listeners)
			listener.uploadProcessComplete();
	}

	protected void notifyListenersFileComplete() {
		for (UploadListener listener : listeners)
			listener.uploadFileComplete(currentUpload);
	}

	public synchronized void addListener(UploadListener listener) {
		listeners.add(listener);
	}

	public interface UploadListener {
		public void update(SourceFile source, long bytesSoFar, long bytesTotal);
		public void uploadFileComplete(SourceFile source);
		public void uploadProcessComplete();
	}

	public interface SourceFile {
		public String getFilename();
		public String getPermissions();
		public long getFilesize();
		public InputStream getInputStream() throws IOException;
	}
}
