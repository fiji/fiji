package spimopener;

import ij.process.Blitter;
import ij.process.ImageProcessor;

public class MaximumProjector implements Projector {

	private ImageProcessor ip = null;

	public void reset() {
		ip = null;
	}

	public void add(ImageProcessor ip) {
		if(this.ip == null)
			this.ip = ip;
		else
			this.ip.copyBits(ip, 0, 0, Blitter.MIN);
	}

	public ImageProcessor getProjection() {
		return ip;
	}
}
