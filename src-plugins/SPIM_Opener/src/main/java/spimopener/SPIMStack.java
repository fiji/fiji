package spimopener;

import ij.ImageStack;

import ij.process.ImageProcessor;

public abstract class SPIMStack extends ImageStack {

	public SPIMStack(int width, int height) {
		super(width, height);
	}

	public abstract void addSlice(String path);
	public abstract void addSlice(ImageProcessor ip);
	public abstract void setRange(int orgW, int orgH, int xOffs, int yOffs);
}
