package fiji.pluginManager.logic;

import fiji.pluginManager.util.Downloader.FileDownload;

/*
 * This class' role is to provide download details of a given Plugin for the Downloader
 * utility class to refer to - Through implementing the required SourceFile interface.
 */
public class PluginDownload implements FileDownload {
	private String destination;
	private String url;
	private PluginObject plugin;

	public PluginDownload(PluginObject plugin, String url, String destination) {
		if (plugin == null || url == null || destination == null)
			throw new Error("PluginDownload constructor parameters cannot be null");
		this.destination = destination;
		this.url = url;
		this.plugin = plugin;
	}

	public PluginObject getPlugin() {
		return plugin;
	}

	public long getFileSize() {
		return plugin.filesize;
	}

	public String getDigest() {
		return plugin.getChecksum();
	}

	public long getTimestamp() {
		return plugin.getTimestamp();
	}

	public String getDestination() { //implemented by SourceFile
		return destination;
	}

	public String getURL() { //implemented by SourceFile
		return url;
	}
}
