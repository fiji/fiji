package fiji.pluginManager.logic;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fiji.pluginManager.util.Downloader;
import fiji.pluginManager.util.PluginData;
import fiji.pluginManager.util.Downloader.FileDownload;

/*
 * UpdateTracker.java is for normal users in managing their plugins.
 *
 * This class' main role is to download selected files, as well as indicate those that
 * are marked for deletion. It is able to track the number of bytes downloaded.
 */
public class UpdateTracker extends PluginData implements Runnable, Downloader.DownloadListener {
	private volatile Thread downloadThread;
	private volatile Downloader downloader;
	private List<FileDownload> downloaderList;

	//Keeping track of status
	public PluginCollection changeList; //list of plugins specified to uninstall/download
	public PluginObject currentlyDownloading;
	private int totalBytes;
	private int completedBytesTotal; //bytes downloaded so far of all completed files
	private int currentBytesSoFar; //bytes downloaded so far of current file
	private boolean isDownloading;

	public UpdateTracker(PluginCollection pluginList) {
		//For downloading files, it is the same whether or not user is a developer
		super();
		changeList = pluginList.getNonUploadActions();
		changeList.resetChangeStatuses();
	}

	public int getBytesDownloaded() {
		return (completedBytesTotal + currentBytesSoFar); //return progress
	}

	public int getBytesTotal() {
		return totalBytes;
	}

	public boolean isDownloading() {
		return isDownloading;
	}

	//start processing on contents of Delete List (Mark them for deletion)
	public void markToDelete() {
		for (PluginObject plugin : changeList.getToUninstall()) {
			String filename = plugin.getFilename();
			try {
				//checking status of existing file
				File file = new File(prefix(filename));
				if (!file.canWrite()) //if unable to override existing file
					plugin.fail();
				else {
					if (!isFijiLauncher(filename)) {
						//If it's a normal plugin, write a 0-byte file
						String pluginPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY, filename);
						new File(pluginPath).getParentFile().mkdirs();
						new File(pluginPath).createNewFile();
					} else {
						//If it's a launcher (?!), try removing
						file.renameTo(new File(prefix(filename + ".old")));
					}
					plugin.success();
				}
			} catch (IOException e) {
				plugin.fail();
			}
		}
	}

	//start processing on contents of updateList
	public void startDownload() {
		isDownloading = true;
		downloadThread = new Thread(this);
		downloadThread.start();
	}

	//stop download
	public void stopDownload() {
		//thread will check if downloadThread is null, and stop action where necessary
		downloadThread = null;
		downloader.cancelDownload();
	}

	//Marking files for removal assumed finished here, thus begin download tasks
	public void run() {
		Thread thisThread = Thread.currentThread();
		downloaderList = new ArrayList<FileDownload>();
		for (PluginObject plugin : changeList.getToAddOrUpdate()) {
			//For each selected plugin, get target path to save to
			String name = plugin.getFilename();
			String saveToPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY, name);
			if (isFijiLauncher(name)) { //if downloading launcher, overwrite instead
				saveToPath = prefix((getUseMacPrefix() ? getMacPrefix() : "") + name);
				File orig = new File(saveToPath);
				orig.renameTo(new File(saveToPath + ".old")); //Save backup copy of older version
			}

			//For each selected plugin, get download URL
			String date = null;
			if (plugin.isInstallable()) {
				date = plugin.getTimestamp();
			} else if (plugin.isUpdateable()) {
				date = plugin.getNewTimestamp();
			}
			String downloadURL = PluginManager.TEMP_DOWNLOADURL + name + "-" + date;
			//String downloadURL = PluginManager.MAIN_URL + name + "-" + date; //TODO
			PluginDownload src = new PluginDownload(plugin, downloadURL, saveToPath);
			downloaderList.add(src);

			//Gets the total size of the downloads
			totalBytes += src.getRecordedFileSize();
		}

		downloader = new Downloader(downloaderList.iterator());
		downloader.addListener(this);
		downloader.startDownload(); //nothing happens if downloaderList is empty

		if (thisThread != downloadThread) {
			//if cancelled, remove any unfinished downloads
			for (PluginObject plugin : changeList.getNoSuccessfulChanges()) {
				String fullPath = getSaveToLocation(PluginManager.UPDATE_DIRECTORY, plugin.getFilename());
				try {
					new File(fullPath).delete(); //delete file, if it exists
				} catch (Exception e2) { }
			}
		}
		isDownloading = false;
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

	public boolean successfulChangesMade() {
		Iterator<PluginObject> iterator = changeList.getChangeSucceeded().iterator();
		return iterator.hasNext();
	}

	//Listener receives notification that download for file has finished
	public void fileComplete(FileDownload source) {
		PluginDownload src = (PluginDownload)source;
		currentlyDownloading = src.getPlugin();
		String filename = currentlyDownloading.getFilename();

		try {
			//Check filesize
			long recordedSize = src.getRecordedFileSize();
			long actualFilesize = getFilesizeFromFile(src.getDestination());
			if (recordedSize != actualFilesize)
				throw new Exception("Recorded filesize of " + filename + " is " +
						recordedSize + ". It is not equal to actual filesize of " +
						actualFilesize + ".");

			// verify checksum
			String recordedDigest = src.getRecordedDigest();
			String actualDigest = getDigest(filename, src.getDestination());
			if (!recordedDigest.equals(actualDigest))
				throw new Exception("Wrong checksum for " + filename +
						": Recorded Checksum " + recordedDigest + " != Actual Checksum " +
						actualDigest);

			//This involves non-Windows launcher only
			if (isFijiLauncher(filename) && !platform.startsWith("win"))
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

	public void fileFailed(FileDownload source, Exception e) {
		resolveDownloadError((PluginDownload)source, e);
	}
}
