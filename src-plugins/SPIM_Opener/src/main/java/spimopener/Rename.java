package spimopener;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;


public class Rename implements PlugIn {
	@Override
	public void run(String arg) {
		GenericDialogPlus gd = new GenericDialogPlus("Rename");
		gd.addDirectoryField("Directory", "");
		gd.addStringField("File name", "");
		gd.addStringField("Replacement", "");
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		String dir = gd.getNextString();
		String name = gd.getNextString();
		String repl = gd.getNextString();

		final ArrayList<File> src = new ArrayList<File>();
		final ArrayList<File> dst = new ArrayList<File>();
		collect(new File(dir), name, repl, src, dst);
		print(src, dst);

		final GenericDialog gd2 = new GenericDialog("Rename");
		gd2.addMessage("Rename all files");
		gd2.setModal(false);
		gd2.showDialog();
		gd2.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				System.out.println("Closing");
				if(gd2.wasOKed()) {
					rename(src, dst);
					IJ.showMessage("Done");
				}
			}
		});
	}

	public void collect(File folder, String filename, String replacement, ArrayList<File> src, ArrayList<File> dst) {
		IJ.showStatus(folder.getAbsolutePath());
		if(folder.getName().equals(filename)) {
			src.add(folder);
			dst.add(new File(folder.getParentFile(), replacement));
		}
		File[] children = folder.listFiles();
		if(children == null || children.length == 0)
			return;
		for(File child : children) {
			collect(child, filename, replacement, src, dst);
		}
	}

	public void print(ArrayList<File> src, ArrayList<File> dst) {
		int N = src.size();
		for(int i = 0; i < N; i++) {
			IJ.log(src.get(i).getAbsolutePath() + " -> " + dst.get(i).getAbsolutePath());
			IJ.showProgress(i + 1, N);
		}
	}

	public void rename(ArrayList<File> src, ArrayList<File> dst) {
		int N = src.size();
		// folder.rename(new File(folder.getParentFile(), replacement));

		for(int i = 0; i < N; i++) {
			if(dst.get(i).exists()) {
				IJ.log("Not renaming "  + src.get(i).getAbsolutePath() + " because target file exists alreadz.");
				continue;
			}
			if(!src.get(i).renameTo(dst.get(i)))
				IJ.log("Cannot rename " + src.get(i).getAbsolutePath() + " -> " + dst.get(i).getAbsolutePath());
			IJ.showProgress(i + 1, N);
		}
	}
}
