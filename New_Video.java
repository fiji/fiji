package video2;

import java.awt.Panel;
import java.awt.FlowLayout;
import java.awt.Button;
import java.awt.TextField;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.ImageListener;
import ij.plugin.PlugIn;
import ij.io.DirectoryChooser;
import ij.gui.GenericDialog;


public class New_Video implements PlugIn {
	
	public static final int DEF_WIDTH  = 512;
	public static final int DEF_HEIGHT = 512;

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("New Video");
		gd.addNumericField("Width", DEF_WIDTH, 0);
		gd.addNumericField("Height", DEF_HEIGHT, 0);

		Panel p = new Panel(new FlowLayout());
		Button b = new Button("Select dir");
		p.add(b);
		gd.addPanel(p);

		gd.addStringField("Directory", "", 30);

		final TextField tf = (TextField)gd.getStringFields().get(0);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DirectoryChooser dc = new DirectoryChooser(
					"Select an empty directory for video frames");
				String dir = dc.getDirectory();
				if(dir != null)
					tf.setText(new File(dir).getAbsolutePath());
			}
		});

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int w = (int)gd.getNextNumber();
		int h = (int)gd.getNextNumber();
		String dir = gd.getNextString();

		final WritableVirtualStack stack = createNewVideoStack(w, h, dir);
		if(stack == null)
			return;

		final ImagePlus imp = new ImagePlus("Video stack", stack);
		imp.show();
		ImagePlus.addImageListener(new ImageListener() {
			public void imageOpened(ImagePlus image) {}
			public void imageUpdated(ImagePlus image) {
				/*
				 * prevent from showing 'do you want to save the
				 * changes... . It's saved already anyway.
				 */
				if(imp == image)
					imp.changes = false;
			}
			public void imageClosed(ImagePlus image) {
				if(imp == image) {
					/*
					 * Save the currently edited frame, in case
					 * there was no scrolling during the last
					 * editing.
					 */
					stack.setPixels(imp.getProcessor().getPixels(),
						imp.getCurrentSlice());
					System.out.println("Saving indices file");
					stack.saveIndicesFile();
				}
			}
		});
	}

	public static WritableVirtualStack createNewVideoStack(
							int w, int h, String dir) {

		File folder = new File(dir);
		if(!folder.exists())
			folder.mkdir();

		if(folder.list().length != 0) {
			IJ.error("Empty directory required");
			return null;
		}

		WritableVirtualStack stack = new WritableVirtualStack(dir, w, h);
		stack.addSlice("");
		return stack;
	}
}
