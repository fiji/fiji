/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import java.io.File;
import util.BatchOpener;
import vib.app.FileGroup;
import vib.app.gui.FileGroupDialog;

/*
 *   A simple plugin for creating averages (in the sense of arithmetic
 *   means) of one or more 8 bit images of identical dimensions.
 */

public class Average_Images implements PlugIn {

	float [][] cumulativeImage;
	int width, height, depth;
	int numberOfImages;
	float maxValue;

	public Average_Images() {
		reset();
	}

	public void reset() {
		cumulativeImage = null;
		width = -1;
		height = -1;
		depth = -1;
		numberOfImages = 0;
		maxValue = Float.MIN_VALUE;
	}

	public boolean add( ImagePlus imagePlus ) {

		if( imagePlus.getBitDepth() != 8 ) {
			IJ.error("All images must be 8 bit "+imagePlus+" doesn't.");
			return false;
		}

		int newWidth = imagePlus.getWidth();
		int newHeight = imagePlus.getHeight();
		int newDepth = imagePlus.getStackSize();

		if( width < 0 ) {

			width = newWidth;
			height = newHeight;
			depth = newDepth;
			cumulativeImage = new float[depth][];
			for (int z = 0; z < depth; ++z)
				cumulativeImage[z] = new float[width * height];

		} else {

			if(!((width == newWidth) &&
			     (height == newHeight) &&
			     (depth == newDepth)) ) {

				IJ.error("Dimensions of "+imagePlus+" didn't match.");
				return false;
			}
		}

		ImageStack stack=imagePlus.getStack();
		for( int z = 0; z < depth; ++z ) {
			byte[] pixels=(byte[])stack.getPixels(z+1);
			for( int y = 0; y < height; ++y )
				for( int x = 0; x < width; ++x ) {
					cumulativeImage[z][y*width+x] +=
						(pixels[y*width+x] & 0xFF);
					float newValue = cumulativeImage[z][y*width+x];
					if( newValue > maxValue )
						maxValue = newValue;
				}
		}

		++numberOfImages;

		return true;
	}

	public ImagePlus getAverageImage( boolean rescale ) {

		IJ.showStatus("Creating averaged image...");
		IJ.showProgress(0.0);

		ImageStack newStack = new ImageStack(width,height);
		for( int z = 0; z < depth; ++z ) {
			ByteProcessor bp = new ByteProcessor(width,height);
			byte [] slice = new byte[width*height];
			for( int y = 0; y < height; ++y )
				for( int x = 0; x < width; ++x ) {

					int averageValue;

					float value = cumulativeImage[z][y*width+x];

					if( rescale )
						averageValue = (int)((255*value)/(maxValue*numberOfImages));
					else
						averageValue = (int)(value/numberOfImages);

					if( averageValue < 0 )
						averageValue = 0;
					if( averageValue > 255 )
						averageValue = 255;

					slice[y*width+x] = (byte)averageValue;
				}

			IJ.showProgress((z + 1.0) / depth);

			bp.setPixels(slice);
			newStack.addSlice(null, bp);
		}

		IJ.showProgress(1);

		return new ImagePlus("average of "+numberOfImages+" images",newStack);
	}

	/* We might want to override this for using unusual openers or
	   somethign similar. */

	public void run(String ignored) {

		GenericDialog gd = new GenericDialog("Average Images");

		FileGroup fg = new FileGroup("foo");
		FileGroupDialog fgd = new FileGroupDialog(fg,false);

		gd.addPanel(fgd);
		gd.addNumericField("Index of channels to use (starting at 1): ", 1, 0);
		gd.addCheckbox("Rescale maximum to 255?", true);

		int channelToUse = (int) gd.getNextNumber();
		// ImageJ consistently 1-indexes things in its interface,
		// so turn this into a zero-indexed channel number.
		-- channelToUse;

		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		boolean rescale = gd.getNextBoolean();

		int n = fg.size();
		if (n < 1) {
			IJ.error("No image files selected");
		}

		File firstFile = fg.get(0);
		String path = firstFile.getAbsolutePath();
		ImagePlus[] firstChannels = BatchOpener.open(path);
		if (firstChannels == null) {
			IJ.error("Couldn't open " + firstFile);
			return;
		}

		if ((channelToUse < 0) || (channelToUse >= firstChannels.length)) {
			IJ.error("The image " + path + " doesn't have a channel " + channelToUse);
			return;
		}

		ImagePlus sourceImage = firstChannels[channelToUse];

		for (int i = 0; i < n; ++i) {

			if( i != 0 ) {

				File f = fg.get(i);
				path = f.getAbsolutePath();
				ImagePlus [] channels = BatchOpener.open(path);
				if (channels == null) {
					IJ.error("Couldn't open the file " + path);
					return;
				}
				if( (channelToUse < 0) || (channelToUse >= channels.length) ) {
					IJ.error("The image "+path+" doesn't have a channel "+channelToUse);
					return;
				}

				sourceImage = channels[channelToUse];
			}


			if( ! add(sourceImage) )
				return;
		}

		ImagePlus newImagePlus=getAverageImage(rescale);
		newImagePlus.show();
	}
}
