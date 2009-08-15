package fiji.pluginManager.logic;
import fiji.pluginManager.utilities.Downloader.FileDownload;

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

	public long getRecordedFileSize() {
		return plugin.getFilesize(); //always return the latest version's size
	}

	public String getRecordedDigest() {
		if (plugin.toUpdate())
			return plugin.getNewMd5Sum();
		return plugin.getmd5Sum();
	}

	public String getRecordedTimestamp() {
		if (plugin.toUpdate())
			return plugin.getNewTimestamp();
		return plugin.getTimestamp();
	}

	public String getDestination() { //implemented by SourceFile
		return destination;
	}

	public String getURL() { //implemented by SourceFile
		return url;
	}
}