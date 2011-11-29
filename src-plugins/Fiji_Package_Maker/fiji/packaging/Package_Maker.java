package fiji.packaging;

import fiji.updater.logic.Checksummer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;

import fiji.updater.ui.ij1.IJProgress;

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
		String fileName = "fiji-" + platform + "-" + timestamp;
		SaveDialog save = new SaveDialog("Make Fiji Package", fileName, extension);
		if (save.getFileName() == null)
			return;

		String path = save.getDirectory() + save.getFileName();
		try {
			packager.open(new FileOutputStream(path));
			int count = 0;
			for (PluginObject plugin : plugins)
				count++;
			addFile(packager, "db.xml.gz");
			// Maybe fiji or fiji.exe exist?
			addFile(packager, "fiji");
			addFile(packager, "fiji.exe");
			int i = 0;
			for (PluginObject plugin : plugins) {
				addFile(packager, plugin.filename);
				IJ.showProgress(i++, count);
			}
			packager.close();
			IJ.showMessage("Wrote " + path);
		}
		catch (IOException e) {
			e.printStackTrace();
			IJ.error("Error writing " + path);
		}
	}

	protected static boolean addFile(Packager packager, String fileName) throws IOException {
		if (fileName.equals("fiji-macosx") || fileName.equals("fiji-tiger"))
			fileName = "Contents/MacOS/" + fileName;
		File file = new File(Util.prefix(fileName));
		if (!file.exists())
			return false;
		packager.putNextEntry("Fiji.app/" + fileName, (int)file.length());
		packager.write(new FileInputStream(file));
		packager.closeEntry();
		return true;
	}
}