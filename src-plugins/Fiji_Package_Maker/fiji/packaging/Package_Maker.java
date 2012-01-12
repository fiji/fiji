package fiji.packaging;

import fiji.updater.logic.Checksummer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;

import fiji.updater.ui.ij1.IJProgress;

import fiji.updater.util.Progress;
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

import java.util.ArrayList;
import java.util.List;


public class Package_Maker implements PlugIn {
	protected int count, total;

	public void run(String arg) {
		PluginCollection plugins = new PluginCollection();
		Progress progress = new IJProgress();
		Checksummer checksummer = new Checksummer(plugins, progress);
		checksummer.updateFromLocal();

		List<Packager> packagers = new ArrayList<Packager>();
		packagers.add(new ZipPackager());
		packagers.add(new TarGzPackager());
		packagers.add(new TarPackager());

		String[] types = new String[packagers.size()];
		for (int i = 0; i < types.length; i++)
			types[i] = packagers.get(i).getExtension().substring(1).toUpperCase();

		GenericDialogPlus gd = new GenericDialogPlus("Make Fiji Package");
		gd.addChoice("Type", types, types[IJ.isWindows() ? 0 : 1]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		Packager packager = packagers.get(gd.getNextChoiceIndex());

		String platform = Util.platform;
		String timestamp = Util.timestamp(System.currentTimeMillis());
		String extension = packager.getExtension();
		String fileName = "fiji-" + platform + "-" + timestamp;
		SaveDialog save = new SaveDialog("Make Fiji Package", fileName, extension);
		if (save.getFileName() == null)
			return;

		String path = save.getDirectory() + save.getFileName();
		try {
			packager.open(new FileOutputStream(path));
			addDefaultFiles(packager, plugins, progress);
			packager.close();
			IJ.showMessage("Wrote " + path);
		}
		catch (IOException e) {
			e.printStackTrace();
			IJ.error("Error writing " + path);
		}
	}

	protected void addDefaultFiles(Packager packager, PluginCollection plugins, Progress progress) throws IOException {
		if (progress != null) {
			progress.setTitle("Packaging");
			count = 0;
			total = 4 + plugins.size();
		}
		addFile(packager, "db.xml.gz", false, progress);
		// Maybe ImageJ or ImageJ.exe exist?
		addFile(packager, "ImageJ", true, progress);
		addFile(packager, "ImageJ.exe", true, progress);
		addFile(packager, "Contents/Info.plist", false, progress);
		plugins.sort();
		for (PluginObject plugin : plugins)
			addFile(packager, plugin.filename, isLauncher(plugin.filename), progress);
		if (progress != null)
			progress.done();
	}

	protected boolean addFile(Packager packager, String fileName, boolean executable, Progress progress) throws IOException {
		count++;
		if (fileName.equals("ImageJ-macosx") || fileName.equals("ImageJ-tiger"))
			fileName = "Contents/MacOS/" + fileName;
		File file = new File(Util.prefix(fileName));
		if (!file.exists())
			return false;
		if (progress != null) {
			progress.addItem(fileName);
			progress.setCount(count, total);
		}
		packager.putNextEntry("Fiji.app/" + fileName, executable, (int)file.length());
		packager.write(new FileInputStream(file));
		packager.closeEntry();
		if (progress != null)
			progress.itemDone(fileName);
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
		Progress progress = new StderrProgress();
		Checksummer checksummer = new Checksummer(plugins, progress);
		checksummer.updateFromLocal();

		try {
			packager.open(new FileOutputStream(path));
			new Package_Maker().addDefaultFiles(packager, plugins, progress);
			packager.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error writing " + path);
		}
	}
}