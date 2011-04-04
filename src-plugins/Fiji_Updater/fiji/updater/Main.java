package fiji.updater;

import com.jcraft.jsch.UserInfo;

import fiji.updater.logic.Checksummer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginCollection.Filter;
import fiji.updater.logic.PluginCollection.UpdateSite;
import fiji.updater.logic.PluginObject;
import fiji.updater.logic.PluginUploader;

import fiji.updater.logic.PluginObject.Action;
import fiji.updater.logic.PluginObject.Status;

import fiji.updater.logic.XMLFileDownloader;

import fiji.updater.util.Downloader;
import fiji.updater.util.Progress;
import fiji.updater.util.StderrProgress;
import fiji.updater.util.UpdateJava;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * This class is the command-line interface into Fiji's Updater.
 */
public class Main {
	protected PluginCollection plugins;
	protected Progress progress;

	public Main() throws IOException, ParserConfigurationException, SAXException {
		plugins = new PluginCollection();
		try {
			plugins.read();
		} catch (FileNotFoundException e) { /* ignore */ }
		progress = new StderrProgress();
		XMLFileDownloader downloader = new XMLFileDownloader(plugins);
		downloader.addProgress(progress);
		downloader.start();
	}

	public void checksum() {
		checksum(null);
	}

	public void checksum(List<String> files) {
		Checksummer checksummer = new Checksummer(plugins, progress);
		if (files != null && files.size() > 0)
			checksummer.updateFromLocal(files);
		else
			checksummer.updateFromLocal();
	}

	protected class FileFilter implements Filter {
		protected Set<String> fileNames;

		public FileFilter(List<String> files) {
			if (files != null && files.size() > 0)
				fileNames = new HashSet<String>(files);
		}

		public boolean matches(PluginObject plugin) {
			if (fileNames != null &&
					!fileNames.contains(plugin.filename))
				return false;
			return plugin.getStatus() !=
				Status.OBSOLETE_UNINSTALLED;
		}
	}

	public void listCurrent(List<String> files) {
		for (PluginObject plugin : plugins.filter(new FileFilter(files)))
			System.out.println(plugin.filename + "-"
				+ plugin.getTimestamp());
	}

	public void list(List<String> files, Filter filter) {
		checksum(files);
		if (filter == null)
			filter = new FileFilter(files);
		else
			filter = plugins.and(new FileFilter(files), filter);
		for (PluginObject plugin : plugins.filter(filter))
			System.out.println(plugin.filename + "\t("
				+ plugin.getStatus() + ")\t"
				+ plugin.getTimestamp());
	}

	public void list(List<String> files) {
		list(files, null);
	}

	public void listUptodate(List<String> files) {
		list(files, plugins.is(Status.INSTALLED));
	}

	public void listNotUptodate(List<String> files) {
		list(files, plugins.not(plugins.is(Status.INSTALLED)));
	}

	public void listUpdateable(List<String> files) {
		list(files, plugins.is(Status.UPDATEABLE));
	}

	class OnePlugin implements Downloader.FileDownload {
		PluginObject plugin;

		OnePlugin(PluginObject plugin) {
			this.plugin = plugin;
		}

		public String getDestination() {
			return plugin.filename;
		}

		public String getURL() {
			return plugins.getURL(plugin);
		}

		public long getFilesize() {
			return plugin.filesize;
		}

		public String toString() {
			return plugin.filename;
		}
	}

	public void download(PluginObject plugin) {
		try {
			new Downloader(progress).start(new OnePlugin(plugin));
			System.err.println("Installed " + plugin.filename);
		} catch (IOException e) {
			System.err.println("IO error downloading "
				+ plugin.filename + ": " + e.getMessage());
		}
	}

	public void delete(PluginObject plugin) {
		if (new File(plugin.filename).delete())
			System.err.println("Deleted " + plugin.filename);
		else
			System.err.println("Failed to delete "
					+ plugin.filename);
	}

	public void update(List<String> files) {
		checksum(files);
		for (PluginObject plugin : plugins.filter(new FileFilter(files)))
			switch (plugin.getStatus()) {
			case UPDATEABLE:
			case MODIFIED:
			case NEW:
			case NOT_INSTALLED:
				download(plugin);
				break;
			case NOT_FIJI:
			case OBSOLETE:
			case OBSOLETE_MODIFIED:
				delete(plugin);
				break;
			default:
				System.err.println("Not updating " + plugin.filename + " (" + plugin.getStatus() + ")");
			}
		try {
			plugins.write();
		} catch (Exception e) {
			System.err.println("Could not write db.xml.gz:");
			e.printStackTrace();
		}
	}

	public void upload(List<String> files) {
		if (files == null || files.size() == 0)
			die("Which files do you mean to upload?");

		checksum(files);

		ConsoleUserInfo userInfo = new ConsoleUserInfo();
		String updateSite = null;
		for (String file : files) {
			PluginObject plugin = plugins.getPlugin(file);
			if (plugin == null)
				die("No plugin '" + file + "' found!");
			if (updateSite == null) {
				updateSite = plugin.updateSite;
				if (updateSite == null)
					updateSite = plugin.updateSite =
						chooseUploadSite(file, userInfo);
				if (updateSite == null)
					die("Canceled");
			}
			else if (plugin.updateSite == null) {
				System.err.println("Uploading new plugin '" + file + "' to  site '" + updateSite + "'");
				plugin.updateSite = updateSite;
			}
			else if (!plugin.updateSite.equals(updateSite))
				die("Cannot upload to multiple update sites ("
					+ files.get(0) + " to " + updateSite + " and "
					+ file + " to " + plugin.updateSite + ")");
			plugin.setAction(plugins, Action.UPLOAD);
		}
		UpdateSite site = plugins.getUpdateSite(updateSite);
		System.err.println("Uploading to " + getLongUpdateSiteName(updateSite));

		PluginUploader uploader = new PluginUploader(plugins, updateSite);
		String username = uploader.getDefaultUsername();
		if (username == null || username.equals(""))
			username = userInfo.getUsername("Login for " + getLongUpdateSiteName(updateSite));
		if (!uploader.setLogin(username, userInfo))
			die("Aborting");
		try {
			uploader.upload(progress);
			plugins.write();
		}
		catch (Throwable e) {
			e.printStackTrace();
			die("Error during upload: " + e);
		}
	}

	public String chooseUploadSite(String file, ConsoleUserInfo userInfo) {
		List<String> names = new ArrayList<String>();
		List<String> options = new ArrayList<String>();
		for (String name : plugins.getUpdateSiteNames()) {
			UpdateSite updateSite = plugins.getUpdateSite(name);
			if (updateSite.uploadDirectory == null || updateSite.uploadDirectory.equals(""))
				continue;
			names.add(name);
			options.add(getLongUpdateSiteName(name));
		}
		if (names.size() == 0) {
			System.err.println("No uploadable sites found");
			return null;
		}
		System.err.println("Choose upload site for plugin '" + file + "'");
		int index = userInfo.askChoice(options.toArray(new String[options.size()]));
		return index < 0 ? null : names.get(index);
	}

	public String getLongUpdateSiteName(String name) {
		UpdateSite site = plugins.getUpdateSite(name);
		return name + " ("
			+ (site.sshHost == null || site.equals("") ? "" : site.sshHost + ":")
			+ site.uploadDirectory + ")";
	}

	public static Main getInstance() {
		try {
			return new Main();
		} catch (Exception e) {
			System.err.println("Could not parse db.xml.gz: "
				+ e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public static List<String> makeList(String[] list, int start) {
		List<String> result = new ArrayList<String>();
		while (start < list.length)
			result.add(list[start++]);
		return result;
	}

	public static void die(String message) {
		System.err.println(message);
		System.exit(1);
	}

	public static void usage() {
		System.err.println("Usage: fiji.update.Main <command>\n"
			+ "\n"
			+ "Commands:\n"
			+ "\tlist [<files>]\n"
			+ "\tlist-uptodate [<files>]\n"
			+ "\tlist-not-uptodate [<files>]\n"
			+ "\tlist-updateable [<files>]\n"
			+ "\tlist-current [<files>]\n"
			+ "\tupdate [<files>]\n"
			+ "\tupdate-java");
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			usage();
			System.exit(0);
		}
		String command = args[0];
		if (command.equals("list"))
			getInstance().list(makeList(args, 1));
		else if (command.equals("list-current"))
			getInstance().listCurrent(makeList(args, 1));
		else if (command.equals("list-uptodate"))
			getInstance().listUptodate(makeList(args, 1));
		else if (command.equals("list-not-uptodate"))
			getInstance().listNotUptodate(makeList(args, 1));
		else if (command.equals("list-updateable"))
			getInstance().listUpdateable(makeList(args, 1));
		else if (command.equals("update"))
			getInstance().update(makeList(args, 1));
		else if (command.equals("update-java"))
			new UpdateJava().run(null);
		else if (command.equals("upload"))
			getInstance().upload(makeList(args, 1));
		else
			usage();
	}

	protected static class ConsoleUserInfo implements UserInfo {
		protected Console console = System.console();
		protected int count;

		@Override
		public String getPassword() {
			return new String(console.readPassword());
		}

		@Override
		public String getPassphrase() {
			return getPassword();
		}

		@Override
		public boolean promptPassword(String prompt) {
			if (++count > 3)
				return false;
			showPrompt(prompt);
			if (count > 1)
				System.err.print(" (try " + count + ")");
			return true;
		}

		@Override
		public boolean promptPassphrase(String prompt) {
			return promptPassword(prompt);
		}

		@Override
		public boolean promptYesNo(String message) {
			System.err.print(message);
			String line = console.readLine();
			return line.startsWith("y") || line.startsWith("Y");
		}

		@Override
		public void showMessage(String message) {
			System.err.println(message);
		}

		public void showPrompt(String prompt) {
			if (!prompt.endsWith(": "))
				prompt += ": ";
			System.err.print(prompt);
		}

		public String getUsername(String prompt) {
			showPrompt(prompt);
			return console.readLine();
		}

		public int askChoice(String[] options) {
			for (int i = 0; i < options.length; i++)
				System.err.println("" + (i + 1) + ": " + options[i]);
			for (;;) {
				System.err.print("Choice? ");
				String answer = console.readLine();
				if (answer.equals(""))
					return -1;
				try {
					return Integer.parseInt(answer) - 1;
				} catch (Exception e) { /* ignore */ }
			}
		}
	}
}
