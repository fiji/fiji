package fiji.packaging;

import fiji.updater.Adapter;

import ij.IJ;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

public abstract class Packager {
	static {
		Adapter.ensureIJDirIsSet();
	}
	protected File ijDir = new File(System.getProperty("ij.dir"));
	protected Collection<String> files;
	protected int count, total;

	protected byte[] buffer = new byte[16384];

	public abstract String getExtension();

	public abstract void open(OutputStream out) throws IOException;
	public abstract void putNextEntry(String name, boolean executable, int size) throws IOException;
	public abstract void write(byte[] b, int off, int len) throws IOException;
	public abstract void closeEntry() throws IOException;
	public abstract void close() throws IOException;

	public void write(InputStream in) throws IOException {
		for (;;) {
			int count = in.read(buffer);
			if (count < 0)
				break;
			write(buffer, 0, count);
		}
		in.close();
	}

	public void initialize() throws Exception {
		files = new Adapter(IJ.getInstance() != null).getFileList();
	}

	protected void addDefaultFiles() throws IOException {
		addFile("db.xml.gz", false);
		// Maybe ImageJ or ImageJ.exe exist?
		addFile("ImageJ", true);
		addFile("ImageJ.exe", true);
		addFile("Contents/Info.plist", false);
		for (String fileName : files)
			addFile(fileName, false);
	}

	protected boolean addFile(String fileName, boolean executable) throws IOException {
		count++;
		if (fileName.equals("ImageJ-macosx") || fileName.equals("ImageJ-tiger"))
			fileName = "Contents/MacOS/" + fileName;
		File file = new File(ijDir, fileName);
		if (!file.exists())
			return false;
		putNextEntry("Fiji.app/" + fileName, executable || file.canExecute(), (int)file.length());
		write(new FileInputStream(file));
		closeEntry();
		return true;
	}

	protected static String getPlatform() {
		final boolean is64bit = System.getProperty("os.arch", "").indexOf("64") >= 0;
		final String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux"))
			return "linux" + (is64bit ? "64" : "32");
		if (osName.equals("Mac OS X"))
			return "macosx";
		if (osName.startsWith("Windows"))
			return "win" + (is64bit ? "64" : "32");
		// System.err.println("Unknown platform: " + osName);
		return osName.toLowerCase();
	}

	public static void main(String[] args) {
		Packager packager = null;
		if (args.length == 1) {
			if (args[0].endsWith(".zip"))
				packager = new ZipPackager();
			else if (args[0].endsWith(".tar"))
				packager = new TarPackager();
			else if (args[0].endsWith(".tar.gz") || args[0].endsWith(".tgz"))
				packager = new TarGzPackager();
			else if (args[0].endsWith(".tar.bz2") || args[0].endsWith(".tbz")) try {
				packager = new TarBz2Packager();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(1);
			}
			else {
				System.err.println("Unsupported archive format: " + args[0]);
				System.exit(1);
			}
		}
		else {
			System.err.println("Usage: Package_Maker <filename>");
			System.exit(1);
		}
		String path = args[0];

		try {
			packager.initialize();
			packager.open(new FileOutputStream(path));
			packager.addDefaultFiles();
			packager.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error writing " + path);
		}
	}
}