package video;

import java.io.File;
import java.util.Vector;
import java.util.Arrays;
import java.text.DecimalFormat;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.io.FileSaver;

public class VideoStack {

	static final int DIGITS = 8;
	static final String LAST = "99999999";

	private String dir;
	private String basename;
	private String ending;
	private int w_org, h_org, w_prev, h_prev, d;
	private ImageStack preview;

	
	public void open(String dir, String basename, String ending) {
		this.basename = basename;
		this.ending = ending;
		this.dir = dir;

		File prev = new File(dir, "Preview.tif");
		if(prev.exists()) {
System.out.println("Loading existing preview stack");
			preview = IJ.openImage(dir + "/Preview.tif").getStack();
			w_prev = preview.getWidth();
			h_prev = preview.getHeight();
			String one = preview.getSliceLabel(1);
			ImagePlus tmp = IJ.openImage(dir + "/" + one);
			w_org = tmp.getWidth();
			h_org = tmp.getHeight();
			d = preview.getSize();
			return;
		}

		String[] files = new File(dir).list();
		if(files.length == 0)
			return;

		Arrays.sort(files);
		d = files.length;
System.out.println("Loading " + d + " files");
		for(int i = 0; i < d; i++) {
			if(!files[i].startsWith(basename))
				continue;
			if(!files[i].endsWith(ending))
				continue;
System.out.println("opening " + files[i]);
			ImageProcessor ip = IJ.openImage(dir + "/" + files[i])
				.getProcessor();
			if(preview != null) {
				ip = ip.resize(64).convertToByte(true);
				preview.addSlice(files[i], ip);
				continue;
			}
			w_org = ip.getWidth();
			h_org = ip.getHeight();
			ip = ip.resize(64).convertToByte(true);
			w_prev = ip.getWidth();
			h_prev = ip.getHeight();
			preview = new ImageStack(w_prev, h_prev);
			preview.addSlice(files[i], ip);
		}
		d = preview.getSize();
	}

	public ImageProcessor getProcessor(int index) {
		return IJ.openImage(dir + "/" + preview.getSliceLabel(index)).
			getProcessor();
	}

	public void setSlice(int index, ImageProcessor ip) {
		String name = preview.getSliceLabel(index);
		ImagePlus imp = new ImagePlus(name, ip);
		new FileSaver(imp).saveAsPng(dir + "/" + name);

		ip = ip.resize(w_prev).convertToByte(true);
		preview.setPixels(ip.getPixels(), index);
	}

	
	DecimalFormat df = new DecimalFormat("00000000");
	/*
	 * index: the index AFTER which the slice is inserted
	 */
	public boolean addSlice(int index, ImageProcessor ip) {
		if(ip == null)
			ip = new ColorProcessor(w_org, h_org);
		String name_b = preview.getSliceLabel(index);
		name_b = name_b.substring(basename.length(),
						basename.length() + DIGITS);
		String name_a = preview.getSliceLabel(index + 1);
		name_a = name_a.substring(basename.length(),
						basename.length() + DIGITS);
		int dig = DIGITS;
		String name = "";
		int a = Integer.parseInt(name_a.substring(0, dig));
		int b = Integer.parseInt(name_b.substring(0, dig));
		if(a > b+1)
			name = df.format(b+1);

		if(name == null || name.equals("")) {
			try {
				if(!rebaseImages())
					return false;
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
			// have to do the stuff again
			return addSlice(index, ip);
		}

		while(name.length() < DIGITS)
			name += '0';
		name = basename + name + "." + ending;
		ImagePlus imp = new ImagePlus(name, ip);
		new FileSaver(imp).saveAsPng(dir + "/" + name);

		ip = ip.resize(w_prev).convertToByte(true);
		preview.addSlice(name, ip, index);
		return true;
	}

	public boolean rebaseImages() {
System.out.println("rebase images");
		int N = preview.getSize();
		// create tmp directory
		File tmpdir = new File(dir, "tmp");
		if(tmpdir.exists()) {
			IJ.error("Cannot rebase images, since temporary\n" +
				"directory already exists");
			return false;
		}
		if(!tmpdir.mkdir()) {
			IJ.error("Cannot create temporary");
			return false;
		}
		for(int z = 0; z < N; z++) {
			String name = preview.getSliceLabel(z+1);
			new FileSaver(IJ.openImage(dir + "/" + name)).
				saveAsPng(
					tmpdir.getAbsolutePath() + "/" + name);
			new File(dir, name).delete();
		}

		// find out how many digits are needed
		int num = 1, digitsNeeded = 1;
		String format = "0";
		while(N / num >= 10) {
			digitsNeeded++;
			format += '0';
			num *= 10;
		}
		DecimalFormat df = new DecimalFormat(format);

		// find out how many digits we must append
		String app = "";
		for(int i = 0; i < DIGITS - digitsNeeded; i++)
			app += '0';

		// finally save the files
		for(int z = 0; z < N; z++) {
			String oldname = preview.getSliceLabel(z+1);
			String newname = basename + df.format(z+1)
				+ app + "." + ending;
			ImagePlus imp = IJ.openImage(
				tmpdir.getAbsolutePath() + "/" + oldname);
			// save under new name
			new FileSaver(imp).saveAsPng(dir + "/" + newname);
			preview.setSliceLabel(newname, z+1);
		}
		
		// eventually, we should now delete the tmp folder
		File[] tmpfiles = tmpdir.listFiles();
		for(int i = 0; i < tmpfiles.length; i++)
			tmpfiles[i].delete();
		tmpdir.delete();
System.out.println("rebased");
		return true;
	}

	public void deleteSlice(int index) {
		File f = new File(dir + "/" + preview.getSliceLabel(index));
		f.delete();
		preview.deleteSlice(index);

	}

	public ImageStack getPreview() {
		return preview;
	}

	public int getPreviewWidth() {
		return w_prev;
	}

	public int getPreviewHeight() {
		return h_prev;
	}

	public int getWidth() {
		return w_org;
	}

	public int getHeight() {
		return h_org;
	}

	public String getDir() {
		return dir;
	}
}
