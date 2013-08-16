import ij.ImagePlus;

import ij.gui.GenericDialog;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

import net.imglib2.Cursor;

import net.imglib2.img.Img;
import net.imglib2.img.ImagePlusAdapter;

import net.imglib2.img.display.imagej.ImageJFunctions;

import net.imglib2.type.NativeType;

import net.imglib2.type.numeric.RealType;

public class ImgLib2_Plugin<T extends RealType<T> & NativeType<T>> implements PlugInFilter {
	protected ImagePlus image;

	/**
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(String arg, ImagePlus imp) {
		image = imp;
		// does not handle RGB, since the wrapped type is ARGBType (not a RealType)
		return DOES_8G | DOES_8C | DOES_16 | DOES_32;
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
		Img<T> img = ImagePlusAdapter.wrap(image);
		add(img, 20);
	}

	/**
	 * The actual processing is done here.
	 */
	public static<T extends RealType<T>> void add(Img<T> img, float value) {
		final Cursor<T> cursor = img.cursor();
		final T summand = cursor.get().createVariable();

		summand.setReal(value);

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.get().add(summand);
		}
	}
}
