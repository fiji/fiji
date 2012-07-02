package fiji.updater.logic;

import fiji.updater.logic.FileUploader.SourceFile;

import fiji.updater.logic.PluginCollection.UpdateSite;

import fiji.updater.util.Progress;
import fiji.updater.util.Util;
import fiji.updater.util.UserInterface;

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

	protected String siteName;
	protected UpdateSite site;
	protected List<SourceFile> files;
	protected String compressed;

	// TODO: add a button to check for new db.xml.gz, and merge if necessary
	public PluginUploader(PluginCollection plugins, String updateSite) {
		this.plugins = plugins;
		siteName = updateSite;
		site = plugins.getUpdateSite(updateSite);
		compressed = Util.XML_COMPRESSED;
		if (site.sshHost == null || site.sshHost.equals(""))
			uploader = new FileUploader(site.uploadDirectory);
	}

	public boolean hasUploader() {
		return uploader != null;
	}

	public String getUploadProtocol() {
		String host = site.sshHost;
		int at = host.indexOf('@');
		int colon = host.indexOf(':');
		if (colon > 0 && colon < at)
			return host.substring(0, colon);
		return null;
	}

	public String getDefaultUsername() {
		String host = site.sshHost;
		if (host.startsWith("sftp:"))
			host = host.substring(5);
		int at = host.indexOf('@');
		if (at > 0)
			return host.substring(0, at);
		String name = UserInterface.get().getPref(Util.PREFS_USER);
		if (name == null)
			return "";
		return name;
	}

	public String getUploadHost() {
		return site.sshHost.substring(site.sshHost.indexOf('@') + 1);
	}

	public String getUploadDirectory() {
		return site.uploadDirectory;
	}

	public void setUploader(FileUploader uploader) {
		this.uploader = uploader;
	}

	protected class DbXmlFile implements SourceFile {
		public byte[] bytes;

		public String getFilename() {
			return compressed + ".lock";
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

		public String toString() {
			return compressed;
		}
	}

	public void upload(Progress progress) throws Exception  {
		uploader.addProgress(progress);
		uploader.addProgress(new VerifyTimestamp());

		// TODO: rename "UpdateSource" to "Transferable", reuse!
		files = new ArrayList<SourceFile>();
		List<String> locks = new ArrayList<String>();
		files.add(new DbXmlFile());
		for (PluginObject plugin : plugins.toUpload(siteName))
			files.add(new UploadableFile(plugin));

		// must be last lock
		locks.add(Util.XML_COMPRESSED);

		// verify that the files have not changed in the meantime
		for (SourceFile file : files)
			verifyUnchanged(file, true);

		uploader.upload(files, locks);

		site.setLastModified(getCurrentLastModified());
	}

	protected void verifyUnchanged(SourceFile file, boolean checkTimestamp) {
		if (!(file instanceof UploadableFile))
			return;
		UploadableFile uploadable = (UploadableFile)file;
		if (uploadable.filesize != Util.getFilesize(uploadable.sourceFilename))
			throw new RuntimeException("File size of "
				+ uploadable.plugin.filename + " changed since being checksummed (was " + uploadable.filesize + " but is " + Util.getFilesize(uploadable.sourceFilename) + ")!");
		if (checkTimestamp) {
			long stored = uploadable.plugin.getStatus() == PluginObject.Status.NOT_FIJI ?
				uploadable.plugin.current.timestamp :
				uploadable.plugin.newTimestamp;
			if (stored != Util.getTimestamp(uploadable.sourceFilename))
				throw new RuntimeException("Timestamp of "
					+ uploadable.plugin.filename + " changed since being checksummed (was " + stored + " but is " + Util.getTimestamp(uploadable.sourceFilename) + ")!");
		}
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

		XMLFileWriter writer = new XMLFileWriter(PluginCollection.clone(plugins.forUpdateSite(siteName)));
		if (plugins.size() > 0)
			writer.validate(false);
		((DbXmlFile)files.get(0)).bytes = writer.toCompressedByteArray(false);

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
	protected class VerifyTimestamp implements Progress {
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

		public void itemDone(Object item) {
			if (item instanceof UploadableFile)
				verifyUnchanged((UploadableFile)item, false);
		}

		public void setCount(int count, int total) {}
		public void setItemCount(int count, int total) {}
		public void done() {}
	}

	protected long getCurrentLastModified() {
		try {
			URLConnection connection;
			try {
				connection = new URL(site.url + Util.XML_COMPRESSED).openConnection();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				Thread.sleep(500);
				connection = new URL(site.url + Util.XML_COMPRESSED).openConnection();
			}
			connection.setUseCaches(false);
			long lastModified = connection.getLastModified();
			connection.getInputStream().close();
			UserInterface.get().debug("got last modified " + lastModified + " = timestamp " + Util.timestamp(lastModified));
			return lastModified;
		}
		catch (Exception e) {
			UserInterface.get().debug(e.getMessage());
			if (plugins.size() == 0)
				return -1; // assume initial upload
			e.printStackTrace();
			return 0;
		}
	}

	protected void verifyTimestamp() {
		long lastModified = getCurrentLastModified();
		if (!site.isLastModified(lastModified))
			throw new RuntimeException("db.xml.gz was "
				+ "changed in the meantime (was "
				+ site.timestamp + " but now is "
				+ Util.timestamp(lastModified) + ")");
	}
}
