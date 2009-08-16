package fiji.pluginManager.utilities;
import ij.Menus;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Arrays;

/*
 * Class functionality:
 * Extend from it if you need to
 * - Calculate timestamps of files
 * - Calculate the Md5 sums of files
 * - Get the absolute path (prefix()) of Fiji main directory
 * - Copy a file over to a particular location
 * - Get details of the Operating System Fiji application is on
 */
public abstract class PluginData {
	private final String macPrefix = "Contents/MacOS/";
	private boolean useMacPrefix;
	private String fijiPath;
	protected String[] launchers = {
			"fiji-linux", "fiji-linux64",
			"fiji-macosx", "fiji-tiger",
			"fiji-win32.exe", "fiji-win64.exe"
	};
	protected String platform;

	//default (For developers, local files' path may be different), only crucial for uploading purposes
	private boolean isDeveloper = false;

	public PluginData() {
		fijiPath = getFijiRootPath();
		isDeveloper = fileExists("fiji.cxx");
		platform = getPlatform(); //gets the platform string value

		//useMacPrefix initially is false, set to true if macLauncher exist
		useMacPrefix = false;
		String macLauncher = macPrefix + "fiji-macosx";
		if (platform.equals("macosx") && new File(prefix(macLauncher)).exists())
			useMacPrefix = true;

		Arrays.sort(launchers);
	}

	public static String getFijiRootPath() {
		return stripSuffix(stripSuffix(Menus.getPlugInsPath(),
				File.separator), "plugins");
	}

	public static String stripSuffix(String string, String suffix) {
		if (!string.endsWith(suffix))
			return string;
		return string.substring(0, string.length() - suffix.length());
	}

	public boolean isDeveloper() {
		return isDeveloper;
	}

	public String getMacPrefix() {
		return macPrefix;
	}

	public static String getPlatform() {
		boolean is64bit =
			System.getProperty("os.arch", "").indexOf("64") >= 0;
		String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux"))
			return "linux" + (is64bit ? "64" : "");
		if (osName.equals("Mac OS X"))
			return "macosx";
		if (osName.startsWith("Windows"))
			return "win" + (is64bit ? "64" : "32") + ".exe";
		System.err.println("Unknown platform: " + osName);
		return osName;
	}

	public boolean getUseMacPrefix() {
		return useMacPrefix;
	}

	//get digest of the file as according to fullPath
	public static String getDigest(String path, String fullPath)
	throws NoSuchAlgorithmException, FileNotFoundException,
	IOException, UnsupportedEncodingException {
		if (path.endsWith(".jar"))
			return getJarDigest(fullPath);
		MessageDigest digest = getDigest();
		digest.update(path.getBytes("ASCII"));
		updateDigest(new FileInputStream(fullPath), digest);
		return toHex(digest.digest());
	}

	public static MessageDigest getDigest() throws NoSuchAlgorithmException {
		return MessageDigest.getInstance("SHA-1");
	}

	public static void updateDigest(InputStream input, MessageDigest digest)
			throws IOException {
		byte[] buffer = new byte[65536];
		DigestInputStream digestStream =
			new DigestInputStream(input, digest);
		while (digestStream.read(buffer) >= 0)
			; /* do nothing */
		digestStream.close();
	}

	public final static char[] hex = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};

	public static String toHex(byte[] bytes) {
		char[] buffer = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			buffer[i * 2] = hex[(bytes[i] & 0xf0) >> 4];
			buffer[i * 2 + 1] = hex[bytes[i] & 0xf];
		}
		return new String(buffer);
	}

	public static String getJarDigest(String path)
			throws FileNotFoundException, IOException {
		MessageDigest digest = null;
		try {
			digest = getDigest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		JarFile jar = new JarFile(path);
		List list = new ArrayList();
		Enumeration entries = jar.entries();
		while (entries.hasMoreElements())
			list.add(entries.nextElement());
		Collections.sort(list, new JarEntryComparator());

		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			JarEntry entry = (JarEntry)iter.next();
			digest.update(entry.getName().getBytes("ASCII"));
			updateDigest(jar.getInputStream(entry), digest);
		}
		return toHex(digest.digest());
	}

	private static class JarEntryComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			String name1 = ((JarEntry)o1).getName();
			String name2 = ((JarEntry)o2).getName();
			return name1.compareTo(name2);
		}

		public boolean equals(Object o1, Object o2) {
			String name1 = ((JarEntry)o1).getName();
			String name2 = ((JarEntry)o2).getName();
			return name1.equals(name2);
		}
	}

	//Gets the location of specified file when inside of saveDirectory
	protected String getSaveToLocation(String saveDirectory, String filename) {
		return prefix(saveDirectory + File.separator + filename);
	}

	public String getTimestampFromFile(String filename) {
		String fullPath = prefix(filename);
		long modified = new File(fullPath).lastModified();
		return timestamp(modified);
	}

	public static String timestamp(long millis) {
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(millis);
		return timestamp(date);
	}

	public static String timestamp(Calendar date) {
		DecimalFormat format = new DecimalFormat("00");
		int month = date.get(Calendar.MONTH) + 1;
		int day = date.get(Calendar.DAY_OF_MONTH);
		int hour = date.get(Calendar.HOUR_OF_DAY);
		int minute = date.get(Calendar.MINUTE);
		int second = date.get(Calendar.SECOND);
		return "" + date.get(Calendar.YEAR) +
			format.format(month) + format.format(day) +
			format.format(hour) + format.format(minute) +
			format.format(second);
	}

	public long getFilesizeFromFile(String filename) {
		return new File(filename).length();
	}

	public String getDigestFromFile(String filename) {
		try {
			return getDigest(filename, prefix(filename));
		} catch (Exception e) {
			throw new Error("Could not get digest: " + prefix(filename) + " (" + e + ")");
		}
	}

	public String prefix(String path) {
		return fijiPath + (isDeveloper && path.startsWith("fiji-") ?
				"precompiled/" : "") + path;
	}

	public String initializeFilename(String filename) {
		if (getUseMacPrefix() && filename.startsWith(getMacPrefix()))
			filename = filename.substring(getMacPrefix().length());
		if (File.separator.equals("\\"))
			filename = filename.replace("\\", "/");
		return filename;
	}

	public boolean fileExists(String filename) {
		return new File(prefix(filename)).exists();
	}

	public String[] getLaunchers() {
		return launchers;
	}

	public boolean isFijiLauncher(String filename) {
		if (Arrays.binarySearch(launchers, filename) >= 0)
			return true;
		return false;
	}

	public String[] getRelevantLaunchers() {
		int index = Arrays.binarySearch(launchers, "fiji-" + platform);
		if (index < 0)
			throw new Error("Failed to get Fiji launcher.");

		String[] relevantLaunchers = new String[1];
		//some platform may have more than 1 launcher
		if (platform.equals("macosx")) {
			relevantLaunchers = new String[2];
			relevantLaunchers[0] = launchers[index];
			relevantLaunchers[1] = "fiji-tiger";
		} else { //otherwise it is only 1 default launcher
			relevantLaunchers[0] = launchers[index];
		}
		Arrays.sort(relevantLaunchers);
		return relevantLaunchers;
	}
}
