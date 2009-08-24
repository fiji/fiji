package fiji.pluginManager.logic;

import fiji.pluginManager.util.Downloader;
import fiji.pluginManager.util.Downloader.FileDownload;
import fiji.pluginManager.util.Util;

import ij.IJ;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/*
 * UpdateTracker.java is for normal users in managing their plugins.
 *
 * This class' main role is to download selected files, as well as indicate
 * those that are marked for deletion. It is able to track the number of bytes
 * downloaded.
 */
public class UpdateTracker implements Runnable, Observer {
	private volatile Thread downloadThread;
	private volatile Downloader downloader;
	private List<FileDownload> downloadList;

	public PluginCollection plugins;
	public PluginObject currentPlugin;

	public UpdateTracker(PluginCollection plugins) {
		this.plugins = plugins;
	}

	public boolean isDownloading() {
		return downloadThread != null;
	}

	public synchronized void start() {
		if (downloadThread != null)
			downloader.cancel();
		downloadThread = new Thread(this);
		downloadThread.start();
	}

	public synchronized void stop() {
		downloadThread = null;
		downloader.cancel();
	}

	public void run() {
		// mark for removal
		for (PluginObject plugin : plugins.toUninstall()) try {
			touch(Util.prefixUpdate(plugin.filename));
		} catch (IOException e) {
			e.printStackTrace();
			IJ.error("Could not mark '" + plugin + "' for removal");
			return;
		}

		downloadList = new ArrayList<FileDownload>();
		for (PluginObject plugin : plugins.toInstallOrUpdate()) {
			String name = plugin.filename;
			String saveTo = Util.prefixUpdate(name);
			if (Util.isLauncher(name)) {
				saveTo = Util.prefix(name);
				File orig = new File(saveTo);
				orig.renameTo(new File(saveTo + ".old"));
			}

			String downloadURL = PluginManager.MAIN_URL + name
				+ "-" + plugin.getTimestamp();
			PluginDownload file =
				new PluginDownload(plugin, downloadURL, saveTo);
			downloadList.add(file);
		}

		downloader = new Downloader();
		downloader.addObserver(this);
		try {
			downloader.start(downloadList);
		} catch (RuntimeException e) {
			// TODO: remove current file, tell user
		}
		downloadThread = null;
	}

	public void update(Observable observable, Object arg) {
		if (downloader.hasError())
			throw new RuntimeException("Failed!");

		PluginDownload file = (PluginDownload)downloader.getCurrent();
		String fileName = file.getDestination();
		IJ.showStatus("Downloading " + fileName + "...");
		IJ.showProgress(downloader.getDownloadedBytes(),
			downloader.getTotalBytes());
		if (!downloader.isFileComplete())
			return;

		long size = file.getFilesize();
		long actualSize = Util.getFilesize(fileName);
		if (size != actualSize)
			throw new RuntimeException("Incorrect file size for "
				+ fileName + ": " + actualSize
				+ " (expected " + size + ")");

		try {
			String digest = file.getDigest();
			String actualDigest = Util.getDigest(fileName);
			if (!digest.equals(actualDigest))
				throw new RuntimeException("Incorrect checksum "
						+ "for " + fileName + ": "
						+ actualDigest
						+ " (expected " + digest + ")");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not verify checksum "
				+ "for " + fileName);
		}

		if (Util.isLauncher(fileName) &&
				!Util.platform.startsWith("win")) try {
			Runtime.getRuntime().exec(new String[] {
				"chmod", "0755", file.getDestination()
			});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not mark "
				+ fileName + " as executable");
		}
	}

	public static void touch(String target) throws IOException {
                long now = new Date().getTime();
                new File(target).setLastModified(now);
        }
}
