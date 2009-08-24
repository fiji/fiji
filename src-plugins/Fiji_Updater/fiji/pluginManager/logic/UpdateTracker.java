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

/*
 * UpdateTracker.java is for normal users in managing their plugins.
 *
 * This class' main role is to download selected files, as well as indicate
 * those that are marked for deletion. It is able to track the number of bytes
 * downloaded.
 */
public class UpdateTracker implements Runnable, Downloader.DownloadListener {
	private volatile Thread downloadThread;
	private volatile Downloader downloader;
	private List<FileDownload> downloaderList;

	public PluginCollection plugins;
	public PluginObject currentlyDownloading;
	// TODO: unify in ProgressListener interface
	private int totalBytes;
	private int completedBytesTotal;
	private int currentBytesSoFar;

	public UpdateTracker(PluginCollection plugins) {
		this.plugins = plugins;
	}

	public int getBytesDownloaded() {
		return (completedBytesTotal + currentBytesSoFar); //return progress
	}

	public int getBytesTotal() {
		return totalBytes;
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

	public synchronized void stopDownload() {
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

		downloaderList = new ArrayList<FileDownload>();
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
			PluginDownload src = new PluginDownload(plugin,
					downloadURL, saveTo);
			downloaderList.add(src);

			totalBytes += src.getFileSize();
		}

		downloader = new Downloader(downloaderList.iterator());
		downloader.addListener(this);
		downloader.start();
	}

	private void resolveDownloadError(PluginDownload src, Exception e) {
		//try to delete the file
		try {
			new File(src.getDestination()).delete();
		} catch (Exception e1) { }
		src.getPlugin().fail();
		System.out.println("Could not update " + src.getPlugin().getFilename() +
				": " + e.getLocalizedMessage());
		currentlyDownloading = null;
	}

	//Listener receives notification that download for file has finished
	public void fileComplete(FileDownload source) {
		PluginDownload src = (PluginDownload)source;
		currentlyDownloading = src.getPlugin();
		String filename = currentlyDownloading.getFilename();

		try {
			//Check filesize
			long recordedSize = src.getFileSize();
			long actualFilesize = Util.getFilesize(src.getDestination());
			if (recordedSize != actualFilesize)
				throw new Exception("Recorded filesize of " + filename + " is " +
						recordedSize + ". It is not equal to actual filesize of " +
						actualFilesize + ".");

			// verify checksum
			String recordedDigest = src.getDigest();
			String actualDigest = Util.getDigest(filename, src.getDestination());
			if (!recordedDigest.equals(actualDigest))
				throw new Exception("Wrong checksum for " + filename +
						": Recorded Checksum " + recordedDigest + " != Actual Checksum " +
						actualDigest);

			//This involves non-Windows launcher only
			if (Util.isLauncher(filename) && !Util.platform.startsWith("win"))
				Runtime.getRuntime().exec(new String[] {
					"chmod", "0755", source.getDestination()});

			currentlyDownloading.success();
			System.out.println(currentlyDownloading.getFilename() + " finished download.");
			currentlyDownloading = null;

		} catch (Exception e) {
			resolveDownloadError(src, e);
			currentlyDownloading = null;
		}
		completedBytesTotal += currentBytesSoFar;
		currentBytesSoFar = 0;
	}

	public void update(FileDownload source, int bytesSoFar, int bytesTotal) {
		PluginDownload src = (PluginDownload)source;
		currentlyDownloading = src.getPlugin();
		currentBytesSoFar = bytesSoFar;
	}

	// TODO: stop altogether when a download failed
	public void fileFailed(FileDownload source, Exception e) {
		resolveDownloadError((PluginDownload)source, e);
	}

	public static void touch(String target) throws IOException {
                long now = new Date().getTime();
                new File(target).setLastModified(now);
        }
}
