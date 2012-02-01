package spimopener;

import fiji.util.gui.GenericDialogPlus;

import ij.IJ;

import ij.plugin.PlugIn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Combine_SPIM_Projects implements PlugIn {
	
	public void run(String arg) {
		GenericDialogPlus gd = new GenericDialogPlus("Combine SPIM Projects");
		gd.addMessage("This copies all files from <source> \nto <target> if they do not exist yet");
		gd.addDirectoryField("Source Folder", "");
		gd.addDirectoryField("Target Folder", "");
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		File source = new File(gd.getNextString());
		File target = new File(gd.getNextString());

		if(!source.exists()) {
			IJ.error(source + " does not exist");
			return;
		}
		
		if(!target.exists()) {
			if(!target.mkdir()) {
				IJ.error(target + " does not exist and cannot be created");
				return;
			}
		}
		try {
			copy(source, target);
		} catch(Exception e) {
			IJ.showMessage("Error copying");
			e.printStackTrace();
		}
	}

	public void copy(File source, File target) throws IOException {
		File[] files = source.listFiles();
		if(files == null)
			return;
		for(File f : files) {
			File newf = new File(target, f.getName());
			if(f.isDirectory()) {
				if(!newf.exists())
					newf.mkdir();
				copy(f, newf);
			}
			else {
				if(newf.exists()) {
					IJ.log("Not copying " + newf + " because is exists already");
					continue;
				}
				copySingleFile(f, newf);
			}
		}
	}

	public void copySingleFile(File from, File to) throws IOException {
		int length = (int)from.length();
		byte[] contents = new byte[length];
		int read = 0;
		FileInputStream in = new FileInputStream(from);
		while(read < contents.length) {
			read += in.read(contents, read, contents.length - read);
		}
		in.close();
		FileOutputStream out = new FileOutputStream(to);
		out.write(contents);
		out.close();
	}
}
