package fiji.pluginManager.logic;
import ij.IJ;
import ij.Menus;

import ij.gui.GenericDialog;

import ij.plugin.PlugIn;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.Panel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import fiji.pluginManager.utilities.PluginData;

public class UpdateFiji implements PlugIn { //legacy code & utility class?
	Map dates, digests;
	String fijiPath;
	String currentDate;
	public boolean hasGUI = false;
	boolean forServer = false;

	public static final String defaultURL =
		"http://pacific.mpi-cbg.de/update/current.txt";
	public static final String defaultServerListPath =
		"/var/www/update/current.txt";
	public static final String updateDirectory = "update";

	protected final String macPrefix = "Contents/MacOS/";
	protected boolean useMacPrefix = false;

	public UpdateFiji() {
		dates = new TreeMap();
		digests = new TreeMap();
		currentDate = PluginData.timestamp(Calendar.getInstance());
	}

	public void run(String arg) {
		hasGUI = true;
		String url = defaultURL;
		if (IJ.debugMode) {
			GenericDialog gd = new GenericDialog("Update Fiji");
			gd.addStringField("URL", defaultURL, 50);
			gd.showDialog();
			if (gd.wasCanceled())
				return;
			url = gd.getNextString();
		}
		exec(url);
	}

	public void exec(String url) {
		String path = PluginData.getFijiRootPath();
		File ij_jar = new File(path, "ij.jar");
		if (ij_jar.exists() && !ij_jar.canWrite() &&
				!IJ.showMessageWithCancel("Fiji Updater",
						"Your ij.jar seems to be unwritable.\n"
						+ "Probably you need to run Fiji Update"
						+ " as administrator.\n"
						+ "Do you still want to continue?"))
			return;

		initialize(path);
		try {
			update(new URL(url));
		} catch (MalformedURLException e) {
			IJ.write("Invalid URL: " + url);
		}
	}

	public String getDefaultFijiPath() {
		String name = "/UpdateFiji.class";
		URL url = getClass().getResource(name);
		String path = URLDecoder.decode(url.toString());
		path = path.substring(0, path.length() - name.length());
		if (path.startsWith("jar:") && path.endsWith("!"))
			path = path.substring(4, path.length() - 5);
		if (path.startsWith("file:")) {
			path = path.substring(5);
			if (File.separator.equals("\\") && path.startsWith("/"))
				path = path.substring(1);
		}
		int slash = path.lastIndexOf('/');
		if (slash > 0) {
			slash = path.lastIndexOf('/', slash - 1);
			if (slash > 0)
				path = path.substring(0, slash);
		}
		return path;
	}

	public String prefix(String path) {
		return fijiPath + File.separator
			+ (forServer && path.startsWith("fiji-") ?
					"precompiled/" : "")
			+ path;
	}

	public void initializeFile(String path) {
		try {
			String fullPath = prefix(path);
			String digest = PluginData.getDigest(path, fullPath);
			long modified = new File(fullPath).lastModified();
			if (useMacPrefix && path.startsWith(macPrefix))
				path = path.substring(macPrefix.length());
			if (File.separator.equals("\\"))
				path = path.replace("\\", "/");
			dates.put(path, PluginData.timestamp(modified));
			digests.put(path, digest);
		} catch (Exception e) {
			if (e instanceof FileNotFoundException &&
					path.startsWith("fiji-"))
				return;
			System.err.println("Could not get digest: "
					+ prefix(path) + " (" + e + ")");
			e.printStackTrace();
		}
	}

	public void queueDirectory(List queue, String path) {
		File dir = new File(prefix(path));
		if (!dir.isDirectory())
			return;
		String[] list = dir.list();
		for (int i = 0; i < list.length; i++)
			if (list[i].equals(".") || list[i].equals(".."))
				continue;
			else if (list[i].endsWith(".jar"))
				queue.add(path + File.separator + list[i]);
			else
				queueDirectory(queue,
					path + File.separator + list[i]);
	}

	public void initialize(String fijiPath) {
		initialize(fijiPath, null);
	}

	public void initialize(String fijiPath, String[] only) {
		this.fijiPath = (fijiPath == null ?
			getDefaultFijiPath() : fijiPath);

		List queue = new ArrayList();

		if (only == null || only.length == 0) {
			String platform = PluginData.getPlatform();
			if (platform.equals("macosx")) {
				String macLauncher = macPrefix + "fiji-macosx";
				if (new File(prefix(macLauncher)).exists())
					useMacPrefix = true;
				queue.add((useMacPrefix ? macPrefix : "")
						+ "fiji-macosx");
				queue.add((useMacPrefix ? macPrefix : "")
						+ "fiji-tiger");
			} else
				queue.add("fiji-" + platform);

			queue.add("ij.jar");
			queueDirectory(queue, "plugins");
			queueDirectory(queue, "jars");
			queueDirectory(queue, "retro");
			queueDirectory(queue, "misc");
		} else
			for (int i = 0; i < only.length; i++)
				queue.add(only[i]);

		Iterator iter = queue.iterator();
		int i = 0, total = queue.size();
		while (iter.hasNext()) {
			String name = (String)iter.next();
			if (hasGUI)
				IJ.showStatus("Checksumming " + name + "...");
			initializeFile(name);
			if (hasGUI)
				IJ.showProgress(++i, total);
		}
		if (hasGUI)
			IJ.showStatus("");
	}

	public void initializeFromList(InputStream input) throws IOException {
		BufferedReader in =
			new BufferedReader(new InputStreamReader(input));
		String line;
		while ((line = in.readLine()) != null) {
			int space = line.indexOf(' ');
			if (space < 0)
				continue;
			String path = line.substring(0, space);
			int space2 = line.indexOf(' ', space + 1);
			if (space2 < 0)
				continue;
			String date = line.substring(space + 1, space2);
			int space3 = line.indexOf(' ', space2 + 1);
			String digest = space3 < 0 ? line.substring(space2 + 1)
				: line.substring(space2 + 1, space3);
			dates.put(path, date);
			digests.put(path, digest);
		}
		in.close();
	}

	public void print(PrintStream out) {
		if (dates == null)
			return;
		Iterator iter = dates.keySet().iterator();
		while (iter.hasNext()) {
			String path = (String)iter.next();
			out.println(path + " " + dates.get(path) + " " +
				digests.get(path));
		}
	}

	public void update(URL listFile) {
		UpdateFiji remote = new UpdateFiji();
		try {
			remote.initializeFromList(listFile.openStream());
		} catch (FileNotFoundException e) {
			IJ.showMessage("No updates found");
			return; /* nothing to do, please move along */
		} catch (Exception e) {
			IJ.error("Error getting current versions: " + e);
			return;
		}

		List list = new ArrayList();
		Iterator iter = remote.digests.keySet().iterator();
		while (iter.hasNext()) {
			String name = (String)iter.next();

			/* launcher is platform-specific */
			if (name.startsWith("fiji-")) {
				String platform = PluginData.getPlatform();
				if (!name.equals("fiji-" + platform) &&
						(!platform.equals("macosx") ||
						!name.startsWith("fiji-tiger")))
					continue;
			}

			Object digest = digests.get(name);
			Object remoteDigest = remote.digests.get(name);
			if (digest != null && remoteDigest.equals(digest))
				continue;
			String date = (String)dates.get(name);
			String remoteDate = (String)remote.dates.get(name);
			if (date != null && date.compareTo(remoteDate) > 0)
				continue; /* local modification */
			list.add(name);
		}

		if (list.size() == 0) {
			IJ.showMessage("Already up-to-date.");
			return;
		}

		boolean someAreNotWritable = false;
		for (int i = 0; i < list.size(); i++) {
			File file = new File((String)list.get(i));
			if (!file.exists() || file.canWrite())
				continue;
			IJ.log("Read-only file: " + list.get(i));
			someAreNotWritable = true;
			list.remove(i--);
		}

		if (someAreNotWritable) {
			String msg = " of the updateable files are writable.";
			if (list.size() == 0) {
				IJ.error("None" + msg);
				return;
			}
			IJ.showMessage("Some" + msg);
		}

		boolean[] ticks = new boolean[list.size()];
		for (int i = 0; i < ticks.length; i++)
			ticks[i] = true;

		if (hasGUI && !new SelectPackages(list, ticks).getTicks())
			return;

		int updated = 0, errors = 0;
		for (int i = 0; i < ticks.length; i++) {
			if (!ticks[i])
				continue;
			String name = (String)list.get(i);
			if (hasGUI)
				IJ.showStatus("Updating " + name);
			String fullPath = prefix(updateDirectory +
						File.separator + name);
			try {
				if (name.startsWith("fiji-")) {
					fullPath = prefix((useMacPrefix ?
						macPrefix : "") + name);
					File orig = new File(fullPath);
					orig.renameTo(new File(fullPath
								+ ".old"));
				}
				update(listFile, name,
					"-" + remote.dates.get(name), fullPath);
				String digest =
					(String)remote.digests.get(name);
				String realDigest = PluginData.getDigest(name, fullPath);
				if (!realDigest.equals(digest))
					throw new Exception("wrong checksum: "
						+ digest + " != " + realDigest);
				if (name.startsWith("fiji-") && !PluginData.getPlatform()
						.startsWith("win"))
					Runtime.getRuntime().exec(new String[] {
						"chmod", "0755", fullPath});
				updated++;
			} catch(Exception e) {
				try {
					new File(fullPath).delete();
				} catch (Exception e2) { }
				IJ.write("Could not update " + name
					+ ": " + e.getMessage());
				e.printStackTrace();
				errors++;
			}
			if (hasGUI)
				IJ.showProgress(i + 1, ticks.length);
		}
		if (hasGUI) {
			IJ.showProgress(1, 1);
			IJ.showStatus("");
		}
		if (updated > 0)
			IJ.showMessage("Updated Fiji"
					+ (errors > 0 ? " with errors" : "")
					+ ". Please restart Fiji!");
		else if (errors > 0)
			IJ.error("Could not update Fiji!");
	}

	protected static class SelectPackages implements ActionListener {
		protected String[] labels;
		protected boolean[] ticks;

		protected GenericDialog gd;
		protected int rows, columns, fullColumns;
		protected Button selectAll, selectNone;

		SelectPackages(List list, boolean[] ticks) {
			this.ticks = ticks;

			/*
			 * Try to have at most 6 columns, and if possible
			 * less than 20 rows.
			 */
			columns = Math.min(3, (int)Math.sqrt(ticks.length));
			rows = (ticks.length - 1) / columns + 1;
			fullColumns = columns -
				(rows * columns - ticks.length);

			labels = new String[ticks.length];
			for (int i = 0; i < labels.length; i++)
				labels[i] = (String)list.get(mirror(i));
		}

		boolean getTicks() {
			gd = new GenericDialog("Update Fiji Packages");
			gd.addMessage("Available updates:");
			gd.addCheckboxGroup(rows, columns, labels, ticks);

			// add the "Select all/none" buttons
			Panel buttons = new Panel();
			//buttons.setLayout(new FlowLayout(FlowLayout.CENTER,
			//			5, 0));
			selectNone = new Button(" Select none ");
			selectNone.addActionListener(this);

			selectAll = new Button(" Select all ");
			selectAll.addActionListener(this);

			buttons.add(selectNone);
			buttons.add(selectAll);
			gd.add(buttons);

			gd.showDialog();
			if (gd.wasCanceled())
				return false;

			for (int i = 0; i < ticks.length; i++)
				ticks[mirror(i)] = gd.getNextBoolean();
			return true;
		}

		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source != selectAll && source != selectNone)
				return;
			Iterator iter = gd.getCheckboxes().iterator();
			while (iter.hasNext())
				((Checkbox)iter.next()).setState(source
								 == selectAll);
		}

		/*
		 * Given a two-dimensional list which is indexed row-by-row,
		 * column-by-column, this function outputs the index for
		 * column-by-column row-by-row, indexing. */
		int mirror(int i) {
			int column = i % columns;
			int row = i / columns;
			if (column < fullColumns)
				return row + column * rows;
			return row + column * (rows - 1) + fullColumns;
		}

		int unmirror(int i) {
			int column = i / rows;
			int row = i % rows;
			if (column >= fullColumns) {
				i -= rows * fullColumns;
				column = fullColumns + (i / (rows - 1));
				row = i % (rows - 1);
			}
			return column + row * columns;
		}
	}

	public void update(URL baseURL, String fileName, String suffix,
				String targetPath)
			throws FileNotFoundException, IOException {
		new File(targetPath).getParentFile().mkdirs();
		copyFile(new URL(baseURL, fileName + suffix).openStream(),
				new FileOutputStream(targetPath));
	}

	public static void copyFile(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[65536];
		int count;
		while ((count = in.read(buffer)) >= 0)
			out.write(buffer, 0, count);
		in.close();
		out.close();
	}

	public static void copyFile(String sourcePath, String targetPath)
			throws IOException {
		new File(targetPath).getParentFile().mkdirs();
		copyFile(new FileInputStream(sourcePath),
			new FileOutputStream(targetPath));
	}

	public static void show(String listFilePath) {
		UpdateFiji updater = new UpdateFiji();
		if (listFilePath == null) {
			updater.initialize(null);
			updater.print(System.out);
		} else try {
			InputStream input = new FileInputStream(listFilePath);
			updater.initializeFromList(input);
			updater.print(System.out);
		} catch (Exception e) {
			System.err.println("Exception: " + e);
			e.printStackTrace();
		}
	}

	public static void updateServer(String[] args) {
		String listFilePath = defaultServerListPath;
		if (args.length > 0 && (args[0].startsWith("/") ||
					args[0].startsWith("."))) {
			listFilePath = args[0];
			args = shift(args);
		}

		UpdateFiji remote = new UpdateFiji();
		try {
			InputStream input = new FileInputStream(listFilePath);
			remote.initializeFromList(input);
		} catch (FileNotFoundException e) {
			/* ignore, is new */
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Could not update " + listFilePath
				+ ": " + e);
			System.exit(1);
		}

		UpdateFiji local = new UpdateFiji();
		local.forServer = true;
		local.initialize(null, args);

		if (args.length == 0) {
			String[] launchers = {
				"linux", "linux64",
				"macosx", "tiger",
				"win32.exe", "win64.exe"
			};
			for (int i = 0; i < launchers.length; i++)
				local.initializeFile("fiji-" + launchers[i]);
		} else {
			// Only update a few files, but do not remove the others
			Iterator iter = remote.digests.keySet().iterator();
			while (iter.hasNext()) {
				String name = (String)iter.next();
				if (local.digests.containsKey(name))
					continue;
				local.digests.put(name,
						remote.digests.get(name));
				local.dates.put(name,
						remote.dates.get(name));
			}
		}

		String remotePrefix = new File(listFilePath).getParent();

		Iterator iter = local.digests.keySet().iterator();
		while (iter.hasNext()) {
			String name = (String)iter.next();
			Object localDigest = local.digests.get(name);
			Object remoteDigest = remote.digests.get(name);
			if (localDigest.equals(remoteDigest)) {
				local.dates.put(name, remote.dates.get(name));
				continue;
			}
			String localDate = (String)local.dates.get(name);
			String remoteDate = (String)remote.dates.get(name);
			if (remoteDate != null &&
					localDate.compareTo(remoteDate) < 0)
				continue;
			String sourcePath = local.prefix(name);
			String targetPath = remotePrefix + File.separator
				+ name + "-" + localDate;
			try {
				copyFile(sourcePath, targetPath);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Could not copy "
					+ sourcePath + " to " + targetPath);
				System.exit(1);
			}
			System.err.println("Updated " + name);
		}

		try {
			PrintStream out = new PrintStream(listFilePath);
			local.print(out);
			out.close();
		} catch (Exception e) {
			System.err.println("Could not write " + listFilePath);
			System.exit(1);
		}

		iter = remote.digests.keySet().iterator();
		while (iter.hasNext()) {
			String name = (String)iter.next();
			if (!local.digests.containsKey(name))
				System.err.println("Warning: removed " + name);
		}
	}

	public static String[] shift(String[] list) {
		if (list.length < 1)
			return list;
		String[] result = new String[list.length - 1];
		System.arraycopy(list, 1, result, 0, result.length);
		return result;
	}

	public static void main(String[] args) {
		if (args.length == 0)
			show(null);
		else if (args.length == 2 && args[0].equals("show"))
			show(args[1]);
		else if (args.length > 0 && args[0].equals("update"))
			updateServer(shift(args));
		else {
			System.err.println("Usage: UpdateFiji "
				+ "[(show|update) list-url]");
		}
	}
}
