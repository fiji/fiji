package fiji.updater.logic;

import fiji.updater.logic.PluginObject.Status;

import fiji.updater.util.Progress;
import fiji.updater.util.Progressable;
import fiji.updater.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
	Map<String, PluginObject.Version> cachedChecksums;

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

	public Map<String, PluginObject.Version> getCachedChecksums() {
		return cachedChecksums;
	}

	protected List<StringPair> queue;
	protected String fijiRoot;

	/* follows symlinks */
	protected boolean exists(File file) {
		try {
			return file.getCanonicalFile().exists();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void queueDir(String[] dirs, String[] extensions) {
		Set<String> set = new HashSet<String>();
		for (String extension : extensions)
			set.add(extension);
		for (String dir : dirs)
			queueDir(dir, set);
	}

	public void queueDir(String dir, Set<String> extensions) {
		File file = new File(prefix(dir));
		if (!exists(file))
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
			if (exists(file))
				queue(path, file.getAbsolutePath());
		}
	}

	protected void queueIfExists(String path) {
		String realPath = prefix(path);
		if (exists(new File(realPath)))
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
			timestamp = Util.getTimestamp(realPath);
			checksum = getDigest(path, realPath, timestamp);
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
				// for re-upload
				plugin.newChecksum = checksum;
				plugin.newTimestamp = timestamp;
			}
			plugins.add(plugin);
		}
		else if (checksum != null) {
			plugin.setLocalVersion(checksum, timestamp);
			if (plugin.getStatus() == Status.OBSOLETE_UNINSTALLED)
				plugin.setStatus(Status.OBSOLETE);
		}
		counter += (int)Util.getFilesize(realPath);
		itemDone(path);
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
		writeCachedChecksums();
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

	public static final String[][] directories = {
		{ "jars", "retro", "misc" }, { ".jar", ".class" },
		{ "plugins" }, { ".jar", ".class", ".txt", ".ijm",
			".py", ".rb", ".clj", ".js", ".bsh" },
		{ "scripts" }, { ".py", ".rb", ".clj", ".js", ".bsh" },
		{ "macros" }, { ".txt", ".ijm" },
		{ "luts" }, { ".lut" }
	};

	static final Map<String, Set<String>> extensions;

	static {
		extensions = new HashMap<String, Set<String>>();
		for (int i = 0; i < directories.length; i += 2) {
			Set<String> set = new HashSet<String>();
			for (String extension : directories[i + 1])
				set.add(extension);
			for (String dir : directories[i + 1])
				extensions.put(dir, set);
		}
	}

	public static boolean isCandidate(String path) {
		path = path.replace('\\', '/'); // Microsoft time toll
		int slash = path.indexOf('/');
		if (slash < 0)
			return Util.isLauncher(path);
		Set<String> exts = extensions.get(path.substring(0, slash));
		int dot = path.lastIndexOf('.');
		return exts == null || dot < 0 ?
			false : exts.contains(path.substring(dot));
	}

	protected void initializeQueue() {
		queue = new ArrayList<StringPair>();

		for (String launcher : Util.isDeveloper ?
					Util.launchers : Util.getLaunchers())
				queueIfExists(launcher);

		for (int i = 0; i < directories.length; i += 2)
			queueDir(directories[i], directories[i + 1]);
	}

	public void updateFromLocal() {
		initializeQueue();
		handleQueue();
	}

	protected void readCachedChecksums() {
		cachedChecksums = new TreeMap<String, PluginObject.Version>();
		File file = new File(Util.prefix(".checksums"));
		if (!file.exists())
			return;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) try {
				int space = line.indexOf(' ');
				if (space < 0)
					continue;
				String checksum = line.substring(0, space);
				int space2 = line.indexOf(' ', space + 1);
				if (space2 < 0)
					continue;
				long timestamp = Long.parseLong(line.substring(space + 1, space2));
				String filename = line.substring(space2 + 1);
				cachedChecksums.put(filename, new PluginObject.Version(checksum, timestamp));
			} catch (NumberFormatException e) {
				/* ignore line */
			}
			reader.close();
		} catch (IOException e) {
			// ignore
		}
	}

	protected void writeCachedChecksums() {
		if (cachedChecksums == null)
			return;
		File file = new File(Util.prefix(".checksums"));
		// file.canWrite() not applicable, as the file need not exist
		try {
			Writer writer = new FileWriter(file);
			for (String filename : cachedChecksums.keySet())
				if (new File(Util.prefix(filename)).exists()) {
					PluginObject.Version version = cachedChecksums.get(filename);
					writer.write(version.checksum + " " + version.timestamp + " " + filename + "\n");
				}
			writer.close();
		} catch (IOException e) {
			// ignore
		}
	}

	protected String getDigest(String path, String realPath, long timestamp) throws IOException, NoSuchAlgorithmException, ZipException {
		if (cachedChecksums == null)
			readCachedChecksums();
		PluginObject.Version version = cachedChecksums.get(path);
		if (version != null && timestamp == version.timestamp)
			return version.checksum;
		String checksum = Util.getDigest(path, realPath);
		cachedChecksums.put(path, new PluginObject.Version(checksum, timestamp));
		return checksum;
	}
}
