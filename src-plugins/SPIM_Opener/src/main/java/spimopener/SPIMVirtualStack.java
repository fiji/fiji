package spimopener;

import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

public class SPIMVirtualStack extends SPIMStack {

	protected List<String> paths = new ArrayList<String>();
	private int x0, x1, y0, y1, orgW, orgH;
	private String tempdir = null;

	/** Creates a new, empty virtual stack. */
	public SPIMVirtualStack(int w, int h) {
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

	/** Adds an image to the end of the stack. */
	public void addSlice(String path) {
		if (path == null)
			throw new IllegalArgumentException("path is null!");

		paths.add(path);
	}

	public void addSlice(ImageProcessor ip) {
		String path = makeTempFilename();
		try {
			SPIMExperiment.saveRaw(ip, path);
		} catch(Exception e) {
			throw new RuntimeException("Cannot save tmp virtual file: " + path);
		}
		paths.add(path);
	}

	/** Does nothing. */
	public void addSlice(String sliceLabel, Object pixels) {
	}

	/** Does nothing.. */
	public void addSlice(String sliceLabel, ImageProcessor ip) {
	}

	/** Does noting. */
	public void addSlice(String sliceLabel, ImageProcessor ip, int n) {
	}

	/** Deletes the specified slice, were 1<=n<=nslices. */
	public void deleteSlice(int n) {
		if(n < 1 || n > paths.size())
			throw new IllegalArgumentException("Argument out of range: " + n);
		paths.remove(n - 1);
	}

	/** Deletes the last slice in the stack. */
	public void deleteLastSlice() {
		if(paths.size() > 0)
			deleteSlice(paths.size());
	}

	/** Returns the pixel array for the specified slice, were 1<=n<=nslices. */
	public Object getPixels(int n) {
		ImageProcessor ip = getProcessor(n);
		return ip == null ? null : ip.getPixels();
	}

	/**
	 * Assigns a pixel array to the specified slice,
	 * were 1<=n<=nslices.
	 */
	public void setPixels(Object pixels, int n) {
	}

	/**
	 * Returns an ImageProcessor for the specified slice,
	 *  were 1<=n<=nslices. Returns null if the stack is empty.
	 */
	public ImageProcessor getProcessor(int n) {
		ImageProcessor ip = null;
		try {
			ip = SPIMExperiment.openRaw(paths.get(n - 1), orgW, orgH, x0, x1, y0, y1);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		if(!(ip instanceof ShortProcessor))
			ip = ip.convertToShort(true);
		return ip;
	}

	/** Currently not implemented */
	public int saveChanges(int n) {
		return -1;
	}

	 /** Returns the number of slices in this stack. */
	public int getSize() {
		return paths.size();
	}

	/** Returns the label of the Nth image. */
	public String getSliceLabel(int n) {
		return "";
	}

	/** Returns null. */
	public Object[] getImageArray() {
		return null;
	}

	/** Does nothing. */
	public void setSliceLabel(String label, int n) {
	}

	/** Always return true. */
	public boolean isVirtual() {
		return true;
	}

	/** Does nothing. */
	public void trim() {
	}

	private void makeTempDir() {
		String tmp = System.getProperty("java.io.tmpdir");
		int i = 0;
		File f = null;
		while((f = new File(tmp, String.format("SPIM_MaxProjection_%05d", i))).exists())
			i++;
		f.mkdir();
		tempdir = f.getAbsolutePath();
	}

	private String makeTempFilename() {
		if(tempdir == null)
			makeTempDir();
		int i = 0;
		File f = null;
		while((f = new File(tempdir, String.format("ip%05d.dat", i))).exists())
			i++;
		return f.getAbsolutePath();
	}
}
