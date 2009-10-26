package fiji.updater.logic;

import com.jcraft.jsch.JSchException;

import fiji.updater.Updater;

import fiji.updater.logic.FileUploader.SourceFile;

import fiji.updater.util.Compressor;
import fiji.updater.util.Progress;
import fiji.updater.util.Util;

import ij.IJ;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * This class is responsible for writing updates to server, upon given the
 * updated plugin records.
 *
 * Note: Plugins are uploaded differently
 * - Non-Fiji plugins & new versions of Fiji Plugins will have files AND
 *   details uploaded
 * - Uninstalled & up-to-date plugins will ONLY have their details uploaded
 *   (i.e.: XML file)
 */
public class PluginUploader {
	protected FileUploader uploader;

	// checking race condition:
	// if somebody else updated in the meantime, complain loudly.
	protected long xmlLastModified;
	public long newLastModified;
	List<SourceFile> files;
	String backup, compressed, text;

	// TODO: add a button to check for new db.xml.gz, and merge if necessary
	public PluginUploader(long xmlLastModified) {
		this.xmlLastModified = xmlLastModified;
		backup = Util.prefix(Updater.XML_BACKUP);
		compressed = Util.prefix(Updater.XML_COMPRESSED);
		text = Util.prefix(Updater.TXT_FILENAME);
	}

	public void setUploader(FileUploader uploader) {
		this.uploader = uploader;
	}

	public synchronized boolean setLogin(String username, String password) {
		try {
			uploader = new SSHFileUploader(username, password,
				Updater.UPDATE_DIRECTORY);
			return true;
		} catch (JSchException e) {
			IJ.error("Failed to login");
			return false;
		}
	}

	public void upload(Progress progress) throws Exception  {
		uploader.addProgress(progress);
		uploader.addProgress(new VerifyTimestamp());

		// TODO: rename "UpdateSource" to "Transferable", reuse!
		files = new ArrayList<SourceFile>();
		files.add(new UploadableFile(compressed,
					Updater.XML_LOCK, "C0444"));
		files.add(new UploadableFile(text,
					Updater.TXT_FILENAME, "C0644"));
		for (PluginObject plugin :
				PluginCollection.getInstance().toUpload())
			files.add(new UploadableFile(plugin));

		uploader.upload(files);

		// No errors thrown -> just remove temporary files
		new File(backup).delete();
		new File(Util.prefix(Updater.TXT_FILENAME)).delete();
		newLastModified = getCurrentLastModified();
	}

	protected void updateUploadTimestamp(long timestamp)
			throws Exception {
		for (SourceFile f : files) {
			if (!(f instanceof UploadableFile))
				continue;
			UploadableFile file = (UploadableFile)f;
			PluginObject plugin = file.plugin;
			if (plugin == null)
				continue;
			file.plugin.newTimestamp = timestamp;
			file.filename = plugin.filename + "-" + timestamp;
		}

		XMLFileWriter.writeAndValidate(backup);
		// TODO: only save _compressed_ backup, and not as db.bak!
		compress(backup, compressed);
		((UploadableFile)files.get(0)).updateFilesize();
		// TODO: do no save text file at all!
		saveTextFile(text);
		((UploadableFile)files.get(1)).updateFilesize();

		uploader.calculateTotalSize(files);
	}

	protected void compress(String uncompressed, String compressed)
			throws IOException {
		FileOutputStream out = new FileOutputStream(compressed);
		FileInputStream in = new FileInputStream(uncompressed);
		Compressor.compressAndSave(Compressor.readStream(in), out);
		out.close();
		in.close();
	}

	// TODO: in-memory only, please
	protected void saveTextFile(String path) throws FileNotFoundException {
		PrintStream out = new PrintStream(path);
		for (PluginObject plugin :
				PluginCollection.getInstance().forCurrentTXT())
			out.println(plugin.getFilename() + " "
					+ plugin.getTimestamp() + " "
					+ plugin.getChecksum());
		out.close();
	}

	/*
	 * This class serves two purposes:
	 *
	 * - after locking, it ensures that the timestamp of db.xml.gz is the
	 *   same as when it was last downloaded, to prevent race-conditions
	 *
	 * - it takes the timestamp of the lock file and updates the timestamps
	 *   of all files to be uploaded, so that local time skews do not
	 *   harm
	 */
	class VerifyTimestamp implements Progress {
		public void addItem(Object item) {
			if (item != files.get(0))
				return;
			verifyTimestamp();
		}

		public void setTitle(String string) {
			try {
				updateUploadTimestamp(uploader.timestamp);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Could not update "
					+ "the timestamps in db.xml.gz");
			}
		}

		public void setCount(int count, int total) {}
		public void itemDone(Object item) {}
		public void setItemCount(int count, int total) {}
		public void done() {}
	}

	protected long getCurrentLastModified() {
		try {
			URLConnection connection = new URL(Updater.MAIN_URL
				+ Updater.XML_COMPRESSED).openConnection();
			connection.setUseCaches(false);
			long lastModified = connection.getLastModified();
			connection.getInputStream().close();
			return lastModified;
		}
		catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	protected void verifyTimestamp() {
		if (xmlLastModified != getCurrentLastModified())
			throw new RuntimeException("db.xml.gz was "
				+ "changed in the meantime");
	}
}
