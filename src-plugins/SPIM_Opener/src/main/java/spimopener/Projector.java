package spimopener;

import ij.process.ImageProcessor;

public interface Projector {

	public void reset();

	public void add(ImageProcessor ip);

	public ImageProcessor getProjection();
}
