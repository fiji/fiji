package fiji.pluginManager.logic;

import fiji.pluginManager.util.Downloader.FileDownload;

/*
 * This class' role is to provide download details of a given Plugin for the
 * Downloader utility class to refer to by implementing the required
 * SourceFile interface.
 */
public class PluginDownload implements FileDownload {
	private String destination;
	private String url;
	private PluginObject plugin;

	public PluginDownload(PluginObject plugin, String url, String destination) {
		if (plugin == null || url == null || destination == null)
			throw new RuntimeException("null parameters!");
		this.destination = destination;
		this.url = url;
		this.plugin = plugin;
	}

	public PluginObject getPlugin() {
		return plugin;
	}

	public long getFilesize() {
		return plugin.filesize;
	}

	public String getDigest() {
		return plugin.getChecksum();
	}

	public long getTimestamp() {
		return plugin.getTimestamp();
	}

	public String getDestination() {
		return destination;
	}

	public String getURL() {
		return url;
	}
}
