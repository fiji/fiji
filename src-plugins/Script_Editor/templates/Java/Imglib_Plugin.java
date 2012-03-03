import ij.ImagePlus;

import ij.gui.GenericDialog;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

import mpicbg.imglib.cursor.Cursor;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;

import mpicbg.imglib.image.display.imagej.ImageJFunctions;

import mpicbg.imglib.type.numeric.RealType;

/**
 * @deprecated Use ImgLib2 instead
 */
public class Imglib_Plugin<T extends RealType<T>> implements PlugInFilter {
	protected ImagePlus image;

	/**
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_ALL;
	}

	/**
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(ImageProcessor ip) {
		run(image);
		image.updateAndDraw();
	}

	/**
	 * This should have been the method declared in PlugInFilter...
	 */
	public void run(ImagePlus image) {
		Image<T> img = ImagePlusAdapter.wrap(image);
		add(img, 20);
	}

	/**
	 * The actual processing is done here.
	 */
	public static<T extends RealType<T>> void add(Image<T> img, float value) {
		final Cursor<T> cursor = img.createCursor();
		final T summand = cursor.getType().createVariable();

		summand.setReal(value);

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.getType().add(summand);
		}
		cursor.close();
	}
}
