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

/**
 * This class represents an array of disk-resident images.
 * In the same folder as the images, a file 'indices' is stored, which
 * holds the order in which the indices occur. The names of the image
 * files are restricted to be in the format int + ".tif".
 */
public class WritableVirtualStack extends ImageStack {

	/** The directory containing the images. */
	private final String dir;

	/** An ImageProcessor serving as template for creating new slides. */
	private final ImageProcessor template;

	/** The number of slices of this stack. */
	private int nSlices;

	/** The highest integer (regarding file names). */
	private int highestIndex;

	/** A list of integers, storing the order of the files. */
	private List<Integer> indices;

	/**
	 * Creates a new Writable Stack, using the given directory
	 * as a folder for the individual images.
	 *
	 * If the folder is empty, a new stack is created, using
	 * a ColorProcessor of the given width and height as template.
	 * If the given directory does not exist, it is created.
	 *
	 * If the given directory is not empty, and the containing
	 * images' dimensions do not match the specified width and
	 * height, an exception is thrown.
	 */
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

	/**
	 * Creates a new Writable Stack, using the given directory
	 * as a folder for the individual images.
	 *
	 * If the folder is empty, or does not exist, an exception
	 * is thrown, because the required image dimensions are not
	 * specified; use WritableVirtualStack(dir, w, h) in this case).
	 *
	 */
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
	 * Initializes this WritableVirtualStack by reading the 'indices'
	 * file of the images' directory.
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

	/**
	 * Returns the width of this stack.
	 */
	public int getWidth() {
		return template.getWidth();
	}

	/**
	 * Returns the height of this stack.
	 */
	public int getHeight() {
		return template.getHeight();
	}

	/**
	 * Saves the 'indices' file to the image directory.
	 */
	public void saveIndicesFile() {
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

	/**
	 * Adds a slice to the end of the stack.
	 * @param name The name of the slice. This parameter exists only for
	 *             compatibility reasons with ImageStack, but is not used
	 *             here.
	 */
	public void addSlice(String name) {
		addSlice(name, template.duplicate());
	}

	/**
	 * Adds a slice with the given pixels to the end of the stack.
	 * @param name   The name of the slice. This parameter exists only for
	 *               compatibility reasons with ImageStack, but is not used
	 *               here.
	 * @param pixels The pixel array for the new slice.
	 */
	public void addSlice(String name, Object pixels) {
		ImageProcessor ip = template.duplicate();
		ip.setPixels(pixels);
		addSlice(name, ip);
	}

	/**
	 * Adds the given ImageProcessor as a slice to the end of the stack.
	 * @param name The name of the slice. This parameter exists only for
	 *             compatibility reasons with ImageStack, but is not used
	 *             here.
	 * @param ip   The ImageProcessor for the new slice.
	 */
	public void addSlice(String name, ImageProcessor ip) {
		addSlice(name, ip, nSlices);
	}

	/**
	 * Adds the given ImageProcessor as a slice at the specified
	 * position in the stack.
	 * @param name The name of the slice. This parameter exists only for
	 *             compatibility reasons with ImageStack, but is not used
	 *             here.
	 * @param ip   The ImageProcessor for the new slice.
	 * @param n    The position of the slice, 0 to add it at the beginning.
	 */
	public void addSlice(String name, ImageProcessor ip, int n) {
		nSlices++;
		highestIndex++;
		indices.add(n, highestIndex);
		IJ.save(new ImagePlus("", ip), dir + "/" + highestIndex + ".tif");
	}

	/**
	 * Deletes the specified slice.
	 * @param n The position of the slice to delete, were 1 <= n <= nslices.
	 */
	public void deleteSlice(int n) {
		if(n < 1 || n > nSlices)
			return;

		int index = indices.get(n - 1);
		File f = new File(dir, getFileName(n));
		indices.remove(n - 1);
		f.delete();
		nSlices--;
		if(index == highestIndex)
			highestIndex--;
	}

	/**
	 * Deletes the last slice in the stack.
	 */
	public void deleteLastSlice() {
		if(nSlices > 0)
			deleteSlice(nSlices);
	}

	/**
	 * Returns the pixel array for the specified slice,
	 * were 1 <= n <= nslices.
	 */
	public Object getPixels(int n) {
		ImageProcessor ip = getProcessor(n);
		if (ip!=null)
			return ip.getPixels();
		else
			return null;
	}

	/**
	 * Assigns a pixel array to the specified slice.
	 * @param pixels The pixel array to be assigned.
	 * @param n      The slice index, were 1 <= n <= nslices.
	 */
	public void setPixels(Object pixels, int n) {
		ImageProcessor ip = getProcessor(n);
		if(ip == null)
			return;
		ip.setPixels(pixels);
		IJ.save(new ImagePlus("", ip), dir + "/" + getFileName(n));
	}

	/**
	 * Returns an ImageProcessor for the specified slice.
	 * @param n The slice index, where 1 <= n <= nSlices.
	 * @return null if the stack is empty.
	 */
	public ImageProcessor getProcessor(int n) {
		ImagePlus imp = IJ.openImage(dir + "/" + getFileName(n));
		if (imp != null)
			return imp.getProcessor();
		return null;
	 }

	/**
	 * Returns the number of slices in this stack.
	 */
	public int getSize() {
		return nSlices;
	}

	/**
	 * Returns the label of the Nth image, where 1 <= n <= nSlices.
	 */
	public String getSliceLabel(int n) {
		return getFileName(n);
	}

	/**
	 * Returns null.
	 */
	public Object[] getImageArray() {
		return null;
	}

	/**
	 * Does nothing.
	 */
	public void setSliceLabel(String label, int n) {
	}

	/**
	 * Always return true.
	 */
	public boolean isVirtual() {
		return true;
	}

	/**
	 * Does nothing.
	 */
	public void trim() {
	}

	/**
	 * Returns the path to the directory containing the images.
	 */
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

