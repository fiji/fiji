package fiji.packaging;

import fiji.updater.ui.ij1.IJProgress;

import fiji.updater.util.Util;

import fiji.util.gui.GenericDialogPlus;

import ij.IJ;

import ij.io.SaveDialog;

import ij.plugin.PlugIn;

import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

public class Package_Maker implements PlugIn {
	public void run(String arg) {
		List<Packager> packagers = new ArrayList<Packager>();
		packagers.add(new ZipPackager());
		try { packagers.add(new TarBz2Packager()); } catch (Exception e) { /* ignore */ }
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
			packager.initialize(new IJProgress());
			packager.open(new FileOutputStream(path));
			packager.addDefaultFiles();
			packager.close();
			IJ.showMessage("Wrote " + path);
		}
		catch (IOException e) {
			e.printStackTrace();
			IJ.error("Error writing " + path);
		}
	}
}