package fiji.packaging;

import fiji.updater.logic.Checksummer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;

import fiji.updater.ui.ij1.IJProgress;

import fiji.updater.util.StderrProgress;
import fiji.updater.util.Util;

import fiji.util.gui.GenericDialogPlus;

import ij.IJ;

import ij.io.SaveDialog;

import ij.plugin.PlugIn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Package_Maker implements PlugIn {
	public void run(String arg) {
		PluginCollection plugins = new PluginCollection();
		IJProgress progress = new IJProgress();
		Checksummer checksummer = new Checksummer(plugins, progress);
		checksummer.updateFromLocal();

		String[] types = { "ZIP", "TGZ" };
		Packager[] packagers = { new ZipPackager(), new TarGzPackager() };
		GenericDialogPlus gd = new GenericDialogPlus("Make Fiji Package");
		gd.addChoice("Type", types, types[IJ.isWindows() ? 0 : 1]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		Packager packager = packagers[gd.getNextChoiceIndex()];

		String platform = Util.platform;
		String timestamp = Util.timestamp(System.currentTimeMillis());
		String extension = packager.getExtension();
		String fileName = "ImageJ-" + platform + "-" + timestamp;
		SaveDialog save = new SaveDialog("Make Fiji Package", fileName, extension);
		if (save.getFileName() == null)
			return;

		String path = save.getDirectory() + save.getFileName();
		try {
			packager.open(new FileOutputStream(path));
			addDefaultFiles(packager, plugins, true);
			packager.close();
			IJ.showMessage("Wrote " + path);
		}
		catch (IOException e) {
			e.printStackTrace();
			IJ.error("Error writing " + path);
		}
	}

	protected static void addDefaultFiles(Packager packager, PluginCollection plugins, boolean showProgress) throws IOException {
		int count = 4 + plugins.size();
		addFile(packager, "db.xml.gz", false);
		// Maybe ImageJ or ImageJ.exe exist?
		addFile(packager, "ImageJ", true);
		addFile(packager, "ImageJ.exe", true);
		addFile(packager, "Contents/Info.plist", false);
		plugins.sort();
		int i = 4;
		for (PluginObject plugin : plugins) {
			addFile(packager, plugin.filename, isLauncher(plugin.filename));
			if (showProgress)
				IJ.showProgress(i++, count);
		}
	}

	protected static boolean addFile(Packager packager, String fileName, boolean executable) throws IOException {
		if (fileName.equals("ImageJ-macosx") || fileName.equals("ImageJ-tiger"))
			fileName = "Contents/MacOS/" + fileName;
		File file = new File(Util.prefix(fileName));
		if (!file.exists())
			return false;
		packager.putNextEntry("Fiji.app/" + fileName, executable, (int)file.length());
		packager.write(new FileInputStream(file));
		packager.closeEntry();
		return true;
	}

	protected static boolean isLauncher(String fileName) {
		if (fileName.startsWith("Fiji.app/"))
			fileName = fileName.substring(9);
		if (fileName.startsWith("Contents/MacOS/"))
			fileName = fileName.substring(15);
		if (fileName.endsWith(".exe"))
			fileName = fileName.substring(0, fileName.length() - 4);
		return fileName.equals("ImageJ") || fileName.equals("fiji") ||
			fileName.startsWith("ImageJ-") || fileName.startsWith("fiji-");
	}

	public static void main(String[] args) {
		Packager packager = null;
		if (args.length == 1) {
			if (args[0].endsWith(".zip"))
				packager = new ZipPackager();
			else if (args[0].endsWith(".tar.gz"))
				packager = new TarGzPackager();
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

		PluginCollection plugins = new PluginCollection();
		StderrProgress progress = new StderrProgress();
		Checksummer checksummer = new Checksummer(plugins, progress);
		checksummer.updateFromLocal();

		try {
			packager.open(new FileOutputStream(path));
			addDefaultFiles(packager, plugins, false);
			packager.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error writing " + path);
		}
	}
}