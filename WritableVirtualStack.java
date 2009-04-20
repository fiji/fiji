package video2;

import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import ij.IJ;
import ij.ImageStack;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;

/** This class represents an array of disk-resident images. */
public class WritableVirtualStack extends ImageStack {

	private final String dir;
	private final ImageProcessor template;
	private int nSlices;
	private int highestIndex;
	private List<Integer> indices;

	public WritableVirtualStack(String dir, int w, int h) {
		this.dir = dir;
		open();
		if(nSlices != 0) {
			this.template = IJ.openImage(dir + "/" + getFileName(1))
						.getProcessor();
			if(template.getWidth() != w && template.getHeight() != h)
				throw new IllegalArgumentException(
					"Specified image dimensions do not fit to " +
					"existing images");
		} else {
			this.template = new ColorProcessor(w, h);
		}
	}

	public WritableVirtualStack(String dir) {
		this.dir = dir;
		open();
		if(nSlices == 0)
			throw new RuntimeException("Specified directory is empty, "
				+ "and no dimensions are specified");
		this.template = IJ.openImage(
			dir + "/" + getFileName(1)).getProcessor();
	}

	/**
	 * A directory containing an image sequence (images only contain
	 * numbers in their filename, with the ending "tif") and a file
	 * "indices", which specifies the ordering of the images.
	 */
	private void open() {
		highestIndex = 0;
		nSlices = 0;
		indices = new ArrayList<Integer>();

		File f = new File(dir);
		if(!f.exists())
			f.mkdir();

		// if index file does not exist, create it by sorting the
		// files alphabetically
		File indexfile = new File(f, "indices");
		if(!indexfile.exists())
			return;

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(indexfile));
		} catch(FileNotFoundException e) {}

		String line = null;
		try {
			while((line = reader.readLine()) != null) {
				if(line.trim().length() == 0)
					continue;
				int index = Integer.parseInt(line);
				if(index > highestIndex)
					highestIndex = index;
				indices.add(index);
				nSlices++;
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void writeIndicesFile() {
		File f = new File(dir, "indices");
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(f));
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		for(Integer i : indices)
			out.println(i);
		out.flush();
		out.close();
	}

	/** Adds an image to the end of the stack. */
	public void addSlice(String name) {
		addSlice(name, template.duplicate());
	}

	/** Does nothing. */
	public void addSlice(String sliceLabel, Object pixels) {
		ImageProcessor ip = template.duplicate();
		ip.setPixels(pixels);
		addSlice(sliceLabel, ip);
	}

	/** Does nothing.. */
	public void addSlice(String sliceLabel, ImageProcessor ip) {
		addSlice(sliceLabel, ip, nSlices);
	}
	
	/** Does noting. */
	public void addSlice(String sliceLabel, ImageProcessor ip, int n) {
		nSlices++;
		highestIndex++;
		indices.add(n, highestIndex);
		IJ.save(new ImagePlus("", ip), dir + "/" + highestIndex + ".tif");
	}

	/** Deletes the specified slice, were 1<=n<=nslices. */
	public void deleteSlice(int n) {
		if(n < 1 || n > nSlices)
			throw new IllegalArgumentException("Argument out of range: "+n);
		if (nSlices<1)
			return;

		int index = indices.get(n - 1);
		File f = new File(dir, getFileName(n));
		indices.remove(n);
		f.delete();
		nSlices--;
		if(index == highestIndex)
			highestIndex--;
	}
	
	/** Deletes the last slice in the stack. */
	public void deleteLastSlice() {
		if(nSlices > 0)
			deleteSlice(nSlices);
	}
	   
	/**
	 * Returns the pixel array for the specified slice,
	 * were 1<=n<=nslices.
	 */
	public Object getPixels(int n) {
		ImageProcessor ip = getProcessor(n);
		if (ip!=null)
			return ip.getPixels();
		else
			return null;
	}		
	
	 /** Assigns a pixel array to the specified slice,
		were 1<=n<=nslices. */
	public void setPixels(Object pixels, int n) {
		ImageProcessor ip = getProcessor(n);
		if(ip == null)
			return;
		ip.setPixels(pixels);
		IJ.save(new ImagePlus("", ip), dir + "/" + getFileName(n));
	}

	/**
	 * Returns an ImageProcessor for the specified slice,
	 * were 1 <= n <= nslices. Returns null if the stack is empty.
	 */
	public ImageProcessor getProcessor(int n) {
		ImagePlus imp = IJ.openImage(dir + "/" + getFileName(n));
		if (imp != null)
			return imp.getProcessor();
		return null;
	 }
 
	/** Returns the number of slices in this stack. */
	public int getSize() {
		return nSlices;
	}

	/** Returns the label of the Nth image. */
	public String getSliceLabel(int n) {
		return getFileName(n);
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
	
	/** Returns the path to the directory containing the images. */
	public String getDirectory() {
		return dir;
	}
		
	/**
	 * Returns the file name of the specified slice,
	 * were 1 <= n <= nslices.
	 */
	public String getFileName(int n) {
		return indices.get(n - 1) + ".tif";
	}
} 

