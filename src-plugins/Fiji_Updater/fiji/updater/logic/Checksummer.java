package fiji.updater.logic;

import fiji.updater.logic.PluginObject.Status;

import fiji.updater.util.Progress;
import fiji.updater.util.Progressable;
import fiji.updater.util.Util;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.zip.ZipException;

/*
 * Checksummer's overall role is to be in charge of building of a plugin
 * list for interface usage.
 *
 * 1st step: Get information of local plugins (checksums and version)
 * 2nd step: Given XML file, get information of latest Fiji plugins (checksums
 * and version)
 * 3rd step: Build up list of "PluginObject" using both local and updates
 *
 * digests and dates hold checksums and versions of local plugins respectively
 * latestDigests and latestDates hold checksums and versions of latest Fiji
 * plugins
 */
public class Checksummer extends Progressable {
	int counter, total;

	public Checksummer(Progress progress) {
		addProgress(progress);
		setTitle("Checksumming");
	}

	static class StringPair {
		String path, realPath;
		StringPair(String path, String realPath) {
			this.path = path;
			this.realPath = realPath;
		}
	}

	protected List<StringPair> queue;
	protected String fijiRoot;

	public void queueDir(String[] dirs, String[] extensions) {
		Set<String> set = new HashSet<String>();
		for (String extension : extensions)
			set.add(extension);
		for (String dir : dirs)
			queueDir(dir, set);
	}

	public void queueDir(String dir, Set<String> extensions) {
		File file = new File(prefix(dir));
		if (!file.exists())
			return;
		for (String item : file.list()) {
			String path = dir + "/" + item;
			file = new File(prefix(path));
			if (file.isDirectory()) {
				if (!item.equals(".") && !item.equals(".."))
					queueDir(path, extensions);
				continue;
			}
			int dot = item.lastIndexOf('.');
			if (dot < 0 || !extensions.contains(item.substring(dot)))
				continue;
			queue(path, file.getAbsolutePath());
		}
	}

	protected void queueIfExists(String path) {
		String realPath = prefix(path);
		if (new File(realPath).exists())
			queue(path, realPath);
	}

	protected void queue(String path) {
		queue(path, prefix(path));
	}

	protected String prefix(String path) {
		return fijiRoot == null ? Util.prefix(path) : fijiRoot + path;
	}

	protected void queue(String path, String realPath) {
		queue.add(new StringPair(path, realPath));
	}

	protected void handle(StringPair pair) {
		String path = pair.path;
		String realPath = Util.prefix(pair.realPath);
		addItem(path);

		String checksum = null;
		long timestamp = 0;
		if (new File(realPath).exists()) try {
			checksum = Util.getDigest(path, realPath);
			timestamp = Util.getTimestamp(realPath);
		} catch (ZipException e) {
			System.err.println("Problem digesting " + realPath);
		} catch (Exception e) { e.printStackTrace(); }

		PluginCollection plugins = PluginCollection.getInstance();
		PluginObject plugin = plugins.getPlugin(path);
		if (plugin == null) {
			if (checksum == null)
				throw new RuntimeException("Tried to remove "
					+ path + ", which is not known to Fiji");
			if (fijiRoot == null)
				plugin = new PluginObject(path, checksum,
						timestamp, Status.NOT_FIJI);
			else {
				plugin = new PluginObject(path, null, 0,
						Status.OBSOLETE);
				plugin.addPreviousVersion(checksum, timestamp);
			}
			plugins.add(plugin);
		}
		else if (checksum != null) {
			plugin.setLocalVersion(checksum, timestamp);
			if (plugin.getStatus() == Status.OBSOLETE_UNINSTALLED)
				plugin.setStatus(Status.OBSOLETE);
			counter += (int)Util.getFilesize(realPath);
		}
		setItemCount(1, 1);
		setCount(counter, total);
	}

	protected void handleQueue() {
		total = 0;
		for (StringPair pair : queue)
			total += Util.getFilesize(pair.realPath);
		counter = 0;
		for (StringPair pair : queue)
			handle(pair);
		done();
	}

	public void updateFromLocal(List<String> files) {
		if (!Util.isDeveloper)
			throw new RuntimeException("Must be developer");
		queue = new ArrayList<StringPair>();
		for (String file : files)
			queue(file);
		handleQueue();
	}

	public void updateFromPreviousInstallation(String fijiRoot) {
		if (!Util.isDeveloper)
			throw new RuntimeException("Must be developer");
		this.fijiRoot = new File(fijiRoot).getAbsolutePath() + "/";
		updateFromLocal();
		for (PluginObject plugin : PluginCollection.getInstance())
			if (plugin.isLocallyModified())
				plugin.addPreviousVersion(plugin.newChecksum,
						plugin.newTimestamp);
	}

	public void updateFromLocal() {
		queue = new ArrayList<StringPair>();

		for (String launcher : Util.isDeveloper ?
					Util.launchers : Util.getLaunchers())
				queueIfExists(launcher);

		queue("ij.jar");

		queueDir(new String[] { "jars", "retro", "misc" },
				new String[] { ".jar", ".class" });
		queueDir(new String[] { "plugins" },
				new String[] { ".jar", ".class",
					".py", ".rb", ".clj", ".js", ".bsh",
					".txt", ".ijm" });
		queueDir(new String[] { "macros" },
				new String[] { ".txt", ".ijm" });
		queueDir(new String[] { "luts" }, new String[] { ".lut" });

		handleQueue();
	}
}
