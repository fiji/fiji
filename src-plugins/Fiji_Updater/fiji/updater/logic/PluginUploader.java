package fiji.updater.logic;

import com.jcraft.jsch.JSchException;

import fiji.updater.Updater;

import fiji.updater.logic.FileUploader.SourceFile;

import fiji.updater.util.Progress;
import fiji.updater.util.Util;

import ij.IJ;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
	protected PluginCollection plugins;
	protected FileUploader uploader;

	// checking race condition:
	// if somebody else updated in the meantime, complain loudly.
	protected long xmlLastModified;
	public long newLastModified;
	List<SourceFile> files;
	String backup, compressed;

	// TODO: add a button to check for new db.xml.gz, and merge if necessary
	public PluginUploader(PluginCollection plugins, long xmlLastModified) {
		this.plugins = plugins;
		this.xmlLastModified = xmlLastModified;
		backup = Util.prefix(Updater.XML_BACKUP);
		compressed = Util.prefix(Updater.XML_COMPRESSED);
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

	protected class DbXmlFile implements SourceFile {
		public byte[] bytes;

		public String getFilename() {
			return compressed;
		}
		
		public String getPermissions() {
			return "C0444";
		}

		public long getFilesize() {
			return bytes.length;
		}

		public InputStream getInputStream() {
			return new ByteArrayInputStream(bytes);
		}
	}

	public void upload(Progress progress) throws Exception  {
		uploader.addProgress(progress);
		uploader.addProgress(new VerifyTimestamp());

		// TODO: rename "UpdateSource" to "Transferable", reuse!
		files = new ArrayList<SourceFile>();
		List<String> locks = new ArrayList<String>();
		files.add(new DbXmlFile());
		for (PluginObject plugin : plugins.toUpload())
			files.add(new UploadableFile(plugin));

		// must be last lock
		locks.add(Updater.XML_COMPRESSED);

		uploader.upload(files, locks);

		// No errors thrown -> just remove temporary files
		new File(backup).delete();
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
			plugin.filesize = file.filesize =
				Util.getFilesize(plugin.filename);
			plugin.newTimestamp = timestamp;
			file.filename = plugin.filename + "-" + timestamp;
			if (plugin.getStatus() ==
					PluginObject.Status.NOT_FIJI) {
				plugin.setStatus(PluginObject.Status.INSTALLED);
				plugin.current.timestamp = timestamp;
			}
		}

		XMLFileWriter writer = new XMLFileWriter(plugins);
		writer.validate();
		((DbXmlFile)files.get(0)).bytes = writer.toCompressedByteArray();

		uploader.calculateTotalSize(files);
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
