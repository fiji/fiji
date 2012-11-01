package leica;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.io.SaveDialog;
import ij.io.TiffEncoder;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Leica_SP_Writer implements PlugInFilter {
	ImagePlus image;
	int channels;

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_8C | DOES_8G | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		// get channel count
		GenericDialog gd = new GenericDialog("Number of channels");
		gd.addNumericField("channels", 2, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		channels = (int)gd.getNextNumber();

		// get channel images
		int w = image.getWidth(), h = image.getHeight(), d;
		d = image.getStack() != null ? image.getStack().getSize() : 1;
		ArrayList images = new ArrayList();
		int imageCount = WindowManager.getImageCount();
		for (int i = 0; i < imageCount; i++) {
			ImagePlus image2 = WindowManager.getImage(i + 1);
			if (image2 == image) {
				if (i > 0) {
					images.add(images.get(0));
					images.set(0, image2);
				} else
					images.add(image2);
				continue;
			}
			ImageStack stack2 = image2.getStack();
			if (image2.getWidth() == w && image2.getHeight() == h
					&& ((stack2 == null && d == 1) ||
						stack2.getSize() == d))
				images.add(image2);
		}
		String[] imageNames = new String[images.size()];
		for (int i = 0; i < imageNames.length; i++) {
			ImagePlus image2 = (ImagePlus)images.get(i);
			imageNames[i] = image2.getTitle();
		}

		GenericDialog gd2 = new GenericDialog("Channels");
		for (int i = 0; i < channels; i++)
			gd2.addChoice("image" + i, imageNames, imageNames[0]);
		gd2.showDialog();
		if (gd2.wasCanceled())
			return;

		// get filename
		SaveDialog sd = new SaveDialog("Save Leica SP",
				imageNames[0], ".tif");
		if (sd.getFileName() == null)
			return;
		String path = sd.getDirectory() + File.separator
			+ sd.getFileName();

		// create fake stack
		ImageStack stack = new ImageStack(w, h);
		Calibration cal = image.getCalibration();
		for (int i = 0; i < channels; i++) {
			int imageIndex = gd2.getNextChoiceIndex();
			ImagePlus image2 = (ImagePlus)images.get(imageIndex);
			Calibration cal2 = image2.getCalibration();
			if (cal.pixelWidth != cal2.pixelWidth ||
					cal.pixelHeight != cal2.pixelHeight ||
					cal.pixelDepth != cal2.pixelDepth ||
					cal.xOrigin != cal2.xOrigin ||
					cal.yOrigin != cal2.yOrigin ||
					cal.zOrigin != cal2.zOrigin)
				if (!IJ.showMessageWithCancel("Mismatch",
							"The dimensions of " +
							image2.getTitle() +
							"do not match. " +
							"Continue?"))
					return;
			ImageStack stack2 = image2.getStack();
			for (int j = 0; j < d; j++)
				stack.addSlice("", stack2.getProcessor(j + 1));
		}

		// save it
		ImagePlus result = new ImagePlus("", stack);
		result.setCalibration(cal);
		FileInfo fi = result.getFileInfo();
                fi.nImages = d * channels;
		fi.description = "[GLOBAL]\n" +
			"[FILTERSETTING1]\n" +
			"NumOfVisualisations=" + channels + "\n" +
			"NumOfFrames=" + d + "\n" +
			"VoxelSizeX=" + cal.pixelWidth + "\n" +
			"VoxelSizeY=" + cal.pixelHeight + "\n" +
			"VoxelSizeZ=" + cal.pixelDepth + "\n" +
			(fi.description == null ? "" : fi.description);
                Object info = image.getProperty("Info");
                if (info!=null && (info instanceof String))
                        fi.info = (String)info;
                try {
                        TiffEncoder file = new TiffEncoder(fi);
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
						new FileOutputStream(path)));
                        file.write(out);
                        out.close();
                }
                catch (IOException e) {
                        IJ.error("Could not write " + path);
                        return;
                }
        }
}
