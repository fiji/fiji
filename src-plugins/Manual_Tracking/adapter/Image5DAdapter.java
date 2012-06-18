package adapter;

import i5d.Image5D;

import ij.ImagePlus;

public class Image5DAdapter extends ImageAdapter {
	@Override
	public void setChannel(ImagePlus image, int channel) {
		if (image instanceof Image5D) {
			Image5D i5d = (Image5D)image;
			i5d.setChannel(channel);
		}
		else
			super.setChannel(image, channel);
	}

	@Override
	public void setSlice(ImagePlus image, int slice) {
		if (image instanceof Image5D) {
			Image5D i5d = (Image5D)image;
			i5d.setSlice(slice);
		}
		else
			super.setSlice(image, slice);
	}

	@Override
	public void setFrame(ImagePlus image, int frame) {
		if (image instanceof Image5D) {
			Image5D i5d = (Image5D)image;
			i5d.setFrame(frame);
		}
		else
			super.setFrame(image, frame);
	}

	@Override
	public int getChannel(ImagePlus image) {
		if (image instanceof Image5D) {
			Image5D i5d = (Image5D)image;
			return i5d.getCurrentChannel();
		}
		return super.getChannel(image);
	}

	@Override
	public int getSlice(ImagePlus image) {
		if (image instanceof Image5D) {
			Image5D i5d = (Image5D)image;
			return i5d.getCurrentSlice();
		}
		return super.getSlice(image);
	}

	@Override
	public int getFrame(ImagePlus image) {
		if (image instanceof Image5D) {
			Image5D i5d = (Image5D)image;
			return i5d.getCurrentFrame();
		}
		return super.getFrame(image);
	}
}