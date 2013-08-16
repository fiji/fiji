package ij3d;

import ij.ImageStack;
import ij.ImagePlus;
import ij.IJ;
import ij.io.FileInfo;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.StackConverter;

import java.io.File;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

import customnode.CustomMesh;
import customnode.CustomMultiMesh;
import customnode.CustomMeshNode;

import javax.vecmath.Color3f;

public class ContentCreator {

	private static final boolean SWAP_TIMELAPSE_DATA = false;

	public static Content createContent(
				String name,
				ImagePlus image,
				int type) {
		int resf = Content.getDefaultResamplingFactor(image, type);
		return createContent(name, image, type, resf, 0);
	}

	public static Content createContent(
				String name,
				ImagePlus image,
				int type,
				int resf) {
		return createContent(name, image, type, resf, 0);
	}

	public static Content createContent(
				String name,
				ImagePlus image,
				int type,
				int resf,
				int tp) {
		int thr = Content.getDefaultThreshold(image, type);
		return createContent(name, image, type, resf, tp, null, thr, new boolean[] {true, true, true});
	}

	public static Content createContent(
				String name,
				ImagePlus image,
				int type,
				int resf,
				int tp,
				Color3f color,
				int thresh,
				boolean[] channels) {

		return createContent(name, getImages(image),
			type, resf, tp, color, thresh, channels);
	}

	public static Content createContent(
				String name,
				File file,
				int type,
				int resf,
				int tp,
				Color3f color,
				int thresh,
				boolean[] channels) {

		return createContent(name, getImages(file),
			type, resf, tp, color, thresh, channels);
	}

	public static Content createContent(
				String name,
				ImagePlus[] images,
				int type,
				int resf,
				int tp,
				Color3f color,
				int thresh,
				boolean[] channels) {

		TreeMap<Integer, ContentInstant> instants =
			new TreeMap<Integer, ContentInstant>();
		boolean timelapse = images.length > 1;
		boolean shouldSwap = SWAP_TIMELAPSE_DATA && timelapse;
		for(ImagePlus imp : images) {
			ContentInstant content = new ContentInstant(name);
			content.image = imp;
			content.color = color;
			content.threshold = thresh;
			content.channels = channels;
			content.resamplingF = resf;
			content.timepoint = tp;
			content.showCoordinateSystem(UniverseSettings.
					showLocalCoordinateSystemsByDefault);
			content.displayAs(type);
			content.compile();
			if(shouldSwap) {
				content.clearOriginalData();
				content.swapDisplayedData();
			}
			instants.put(tp++, content);
		}
		return new Content(name, instants, shouldSwap);
	}

	public static Content createContent(CustomMesh mesh, String name) {
		return createContent(mesh, name, 0);
	}

	public static Content createContent(CustomMesh mesh, String name, int tp) {
		Content c = new Content(name, tp);
		ContentInstant content = c.getInstant(tp);
		content.color = mesh.getColor();
		content.transparency = mesh.getTransparency();
		content.shaded = mesh.isShaded();
		content.showCoordinateSystem(
			UniverseSettings.showLocalCoordinateSystemsByDefault);
		content.display(new CustomMeshNode(mesh));
		return c;
	}

	public static Content createContent(CustomMultiMesh node, String name) {
		return createContent(node, name, 0);
	}

	public static Content createContent(CustomMultiMesh node, String name, int tp) {
		Content c = new Content(name, tp);
		ContentInstant content = c.getInstant(tp);
		content.color = null;
		content.transparency = 0f;
		content.shaded = true;
		content.showCoordinateSystem(
			UniverseSettings.showLocalCoordinateSystemsByDefault);
		content.display(node);
		return c;
	}

	/**
	 * Get an array of images of the specified image; if the image is a
	 * hyperstack, it is splitted into several individual images, otherwise,
	 * it the returned array contains the given image only.
	 * @param imp
	 * @return
	 */
	public static ImagePlus[] getImages(ImagePlus imp) {
		ImagePlus[] ret = new ImagePlus[imp.getNFrames()];
		int i = 0;
		for(ImagePlus frame : HyperStackIterator.getIterable(imp))
			ret[i++] = frame;
		return ret;
	}

	/**
	 * If <code>file</code> is a regular file, it is opened using IJ.openImage(),
	 * and then given to getImages(ImagePlus);
	 * If <code>file</code> however is a directory, all the files in it are sorted
	 * alphabetically and then loaded, failing silently if an image
	 * can not be opened by IJ.openImage().
	 * @param dir
	 * @return
	 */
	public static ImagePlus[] getImages(File file) {
		ArrayList<ImagePlus> images = new ArrayList<ImagePlus>();
		for(ImagePlus frame : FileIterator.getIterable(file))
			images.add(frame);
		return images.toArray(new ImagePlus[] {});
	}

	public static void convert(ImagePlus image) {
		int imaget = image.getType();
		if(imaget == ImagePlus.GRAY8 || imaget == ImagePlus.COLOR_256)
			return;
		int s = image.getStackSize();
		switch(imaget) {
			case ImagePlus.GRAY16:
			case ImagePlus.GRAY32:
				if(s == 1)
					new ImageConverter(image).
						convertToGray8();
				else
					new StackConverter(image).
						convertToGray8();
				break;
		}
	}

	private static class FileIterator implements Iterator<ImagePlus>, Iterable<ImagePlus> {

		public static Iterable<ImagePlus> getIterable(File file) {
			if(!file.isDirectory()) {
				return HyperStackIterator.getIterable(
					IJ.openImage(file.getAbsolutePath()));
			}
			return new FileIterator(file);
		}

		private final String directory;
		private final String[] names;
		private int nextIndex = 0;

		/**
		 * file may be a single file or a directory.
		 */
		private FileIterator(File file) {
			if(!file.isDirectory()) {
				names = new String[] { file.getName() };
				directory = file.getParentFile().getAbsolutePath();
				return;
			}
			// get the file names
			directory = file.getAbsolutePath();
			names = file.list();
			Arrays.sort(names);
		}

		public Iterator<ImagePlus> iterator() {
			return this;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public boolean hasNext() {
			return nextIndex < names.length;
		}

		public ImagePlus next() {
			if(nextIndex == names.length)
				return null;

			File f = new File(directory, names[nextIndex]);
			nextIndex++;
			try {
				return IJ.openImage(f.getAbsolutePath());
			} catch(Exception e) {
				return null;
			}
		}
	}

	private static class HyperStackIterator implements Iterator<ImagePlus>, Iterable<ImagePlus> {

		public static Iterable<ImagePlus> getIterable(ImagePlus image) {
			return new HyperStackIterator(image);
		}

		private final ImagePlus image;
		private final int nChannels;
		private final int nSlices;
		private final int nFrames;
		private final int w;
		private final int h;
		private int nextFrame = 0;

		private HyperStackIterator(ImagePlus image) {
			this.image = image;
			nChannels = image.getNChannels();
			nSlices = image.getNSlices();
			nFrames = image.getNFrames();
			System.out.println("nFrames = " + nFrames);
			w = image.getWidth();
			h = image.getHeight();
		}

		public Iterator<ImagePlus> iterator() {
			return this;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public boolean hasNext() {
			return nextFrame < nFrames;
		}

		public ImagePlus next() {
			if(nextFrame == nFrames)
				return null;

			ImageStack oldStack = image.getStack();
			String oldTitle = image.getTitle();
			FileInfo fi = image.getFileInfo();
			ImageStack newStack = new ImageStack(w, h);
			newStack.setColorModel(oldStack.getColorModel());
			for(int j = 0; j < nSlices; j++) {
				int index = image.getStackIndex(1, j + 1, nextFrame + 1);
				Object pixels;
				if (nChannels > 1) {
					image.setPositionWithoutUpdate(1, j + 1, nextFrame + 1);
					pixels = new ColorProcessor(image.getImage()).getPixels();
				} else {
					pixels = oldStack.getPixels(index);
				}
				newStack.addSlice(
					oldStack.getSliceLabel(index),
					pixels);
			}
			ImagePlus ret = new ImagePlus(oldTitle
				+ " (frame " + nextFrame + ")", newStack);
			ret.setCalibration(image.getCalibration().copy());
			ret.setFileInfo((FileInfo)fi.clone());
			nextFrame++;
			return ret;
		}
	}
}

