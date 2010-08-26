package ij3d;

import ij.ImageStack;
import ij.ImagePlus;
import ij.IJ;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.StackConverter;

import java.io.File;
import java.util.TreeMap;
import java.util.Arrays;

import customnode.CustomMesh;
import customnode.CustomMultiMesh;
import customnode.CustomMeshNode;

import javax.vecmath.Color3f;

public class ContentCreator {

	public static Content createContent(
				String name,
				ImagePlus image,
				int type) {
		int resf = Content.getDefaultResamplingFactor(image, type);
		return createContent(name, image, type, resf, -1);
	}

	public static Content createContent(
				String name,
				ImagePlus image,
				int type,
				int resf) {
		return createContent(name, image, type, resf, -1);
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
		for(ImagePlus imp : images) {
			ContentInstant content = new ContentInstant(name);
			content.image = imp;
			content.color = color;
			content.threshold = thresh;
			content.channels = channels;
			content.resamplingF = resf;
			content.showCoordinateSystem(UniverseSettings.
					showLocalCoordinateSystemsByDefault);
			content.displayAs(type);
			content.compile();
			instants.put(tp++, content);
		}
		return new Content(name, instants);
	}

	public static Content createContent(CustomMesh mesh, String name) {
		return createContent(mesh, name, -1);
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
		return createContent(node, name, -1);
	}

	public static Content createContent(CustomMultiMesh node, String name, int tp) {
		Content c = new Content(name, tp);
		ContentInstant content = c.getInstant(tp);
		content.color = null;
		content.transparency = 0f;
		content.shaded = false;
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

		int nChannels = imp.getNChannels();
		int nSlices = imp.getNSlices();
		int nFrames = imp.getNFrames();

		// setSliceWithoutUpdate() does not update the position!
		int channel = imp.getChannel();
		int slice = imp.getSlice();
		int frame = imp.getFrame();

		ImagePlus[] ret = new ImagePlus[nFrames];
		int w = imp.getWidth(), h = imp.getHeight();

		ImageStack oldStack = imp.getStack();
		String oldTitle = imp.getTitle();
		for(int i = 0; i < nFrames; i++) {
			ImageStack newStack = new ImageStack(w, h);
			for(int j = 0; j < nSlices; j++) {
				int index = imp.getStackIndex(1, j+1, i+1);
				Object pixels;
				if (nChannels > 1) {
					imp.setPositionWithoutUpdate(1, j+1, i+1);
					pixels = new ColorProcessor(imp
						.getImage()).getPixels();
				}
				else
					pixels = oldStack.getPixels(index);
				newStack.addSlice(
					oldStack.getSliceLabel(index),
					pixels);
			}
			ret[i] = new ImagePlus(oldTitle
				+ " (frame " + i + ")", newStack);
			ret[i].setCalibration(imp.getCalibration().copy());
		}
		if (nChannels > 1)
			imp.setPositionWithoutUpdate(channel, slice, frame);
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
		if(!file.isDirectory()) {
			ImagePlus image = IJ.openImage(
				file.getAbsolutePath());
			return image == null ? null : getImages(image);
		}
		// get the file names
		String[] names = file.list();
		if (names.length == 0)
			return null;
		Arrays.sort(names);
		ImagePlus[] ret = new ImagePlus[names.length];
		for(int i = 0, j = 0; i < ret.length; i++) {
			File f = new File(file, names[i]);
			ImagePlus imp = IJ.openImage(f.getAbsolutePath());
			if(imp != null)
				ret[j++] = imp;
		}
		return ret;
	}

	public static void convert(ImagePlus image) {
		int imaget = image.getType();
		if(imaget == ImagePlus.GRAY8 || imaget == ImagePlus.COLOR_256)
			return;
		int s = image.getStackSize();
		switch(imaget) {
			case ImagePlus.COLOR_256:
				if(s == 1)
					new ImageConverter(image).
						convertToRGB();
				else
					new StackConverter(image).
						convertToRGB();
				break;
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
}

