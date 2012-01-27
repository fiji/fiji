package spimopener;

import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class SPIMRegularStack extends SPIMStack {

	private int x0, x1, y0, y1, orgW, orgH;

	public SPIMRegularStack(int w, int h) {
		super(w, h);
		this.x0 = 0;
		this.x1 = w - 1;
		this.y0 = 0;
		this.y1 = h - 1;
		this.orgW = w;
		this.orgH = h;
	}

	public void setRange(int orgW, int orgH, int xOffs, int yOffs) {
		this.orgW = orgW;
		this.orgH = orgH;
		this.x0 = xOffs;
		this.x1 = xOffs + getWidth() - 1;
		this.y0 = yOffs;
		this.y1 = yOffs + getHeight() - 1;
	}

	public void addSlice(String path) {
		ImageProcessor ip = null;
		try {
			ip = SPIMExperiment.openRaw(path, orgW, orgH, x0, x1, y0, y1);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}
		addSlice(ip);
	}

	public void addSlice(ImageProcessor ip) {
		if(!(ip instanceof ShortProcessor))
			ip = ip.convertToShort(true);
		super.addSlice("", ip);
	}
}
