package adapter;

import ij.ImagePlus;

public class ImageAdapter {
	public void setChannel(ImagePlus image, int channel) {
		image.setPosition(channel, image.getSlice(), image.getFrame());
	}

	public void setSlice(ImagePlus image, int slice) {
		image.setPosition(image.getChannel(), slice, image.getFrame());
	}

	public void setFrame(ImagePlus image, int frame) {
		image.setPosition(image.getChannel(), image.getSlice(), frame);
	}

	public int getChannel(ImagePlus image) {
		return image.getChannel();
	}

	public int getSlice(ImagePlus image) {
		return image.getSlice();
	}

	public int getFrame(ImagePlus image) {
		return image.getFrame();
	}
}