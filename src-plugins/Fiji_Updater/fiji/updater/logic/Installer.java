package fiji.updater.logic;

import fiji.updater.util.Downloader;
import fiji.updater.util.Downloader.FileDownload;
import fiji.updater.util.Progress;
import fiji.updater.util.Util;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Installer extends Downloader {
	protected PluginCollection plugins;

	public Installer(PluginCollection plugins, Progress progress) {
		this.plugins = plugins;
		addProgress(progress);
		addProgress(new VerifyFiles());
	}

	class Download implements FileDownload {
		PluginObject plugin;
		String url, destination;

		Download(PluginObject plugin, String url, String destination) {
			this.plugin = plugin;
			this.url = url;
			this.destination = destination;
		}

		public String toString() {
			return plugin.getFilename();
		}

		public String getDestination() {
			return destination;
		}

		public String getURL() {
			return url;
		}

		public long getFilesize() {
			return plugin.filesize;
		}
	}

	public synchronized void start() throws IOException {
		// mark for removal
		for (PluginObject plugin : plugins.toUninstall()) try {
			plugin.stageForUninstall();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not mark '"
					+ plugin + "' for removal");
		}

		List<FileDownload> list = new ArrayList<FileDownload>();
		for (PluginObject plugin : plugins.toInstallOrUpdate()) {
			String name = plugin.filename;
			String saveTo = Util.prefixUpdate(name);
			if (Util.isLauncher(name)) {
				saveTo = Util.prefix(name);
				File orig = new File(saveTo);
				File old = new File(saveTo + ".old");
				if (old.exists())
					old.delete();
				orig.renameTo(old);
			}

			String url = plugins.getURL(plugin);
			Download file = new Download(plugin, url, saveTo);
			list.add(file);
		}

		start(list);
	}

	class VerifyFiles implements Progress {
		public void itemDone(Object item) {
				verify((Download)item);
		}

		public void setTitle(String title) {}
		public void setCount(int count, int total) {}
		public void addItem(Object item) {}
		public void setItemCount(int count, int total) {}
		public void done() {}
	}


	public void verify(Download download) {
		String fileName = download.getDestination();
		long size = download.getFilesize();
		long actualSize = Util.getFilesize(fileName);
		if (size != actualSize)
			throw new RuntimeException("Incorrect file size for "
				+ fileName + ": " + actualSize
				+ " (expected " + size + ")");

		PluginObject plugin = download.plugin;
		String digest = download.plugin.getChecksum(), actualDigest;
		try {
			actualDigest = Util.getDigest(plugin.getFilename(),
					fileName);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not verify checksum "
				+ "for " + fileName);
		}

		if (!digest.equals(actualDigest))
			throw new RuntimeException("Incorrect checksum "
					+ "for " + fileName + ":\n"
					+ actualDigest
					+ "\n(expected " + digest + ")");

		plugin.setLocalVersion(digest, plugin.getTimestamp());
		plugin.setStatus(PluginObject.Status.INSTALLED);

		if (Util.isLauncher(fileName) &&
				!Util.platform.startsWith("win")) try {
			Runtime.getRuntime().exec(new String[] {
				"chmod", "0755", download.destination
			});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not mark "
				+ fileName + " as executable");
		}
	}
}
