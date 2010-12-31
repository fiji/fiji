package fiji.tool;

import ij.ImagePlus;

public interface SliceListener {
	public void sliceChanged(ImagePlus image);
}