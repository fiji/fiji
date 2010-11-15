import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.LUT;
import imagescience.image.Image;
import imagescience.utility.I5DResource;
import imagescience.utility.ImageScience;
import java.awt.image.ColorModel;

public final class RJ {

	private static final String NAME = "RandomJ";
	private static final String VERSION = "1.5.0";
	private static final String MINIJVERSION = "1.44a";
	private static final String MINISVERSION = "2.4.0";

	public static String name() { return NAME; }

	public static String version() { return VERSION; }

	static boolean libcheck() {

		if (IJ.getVersion().compareTo(MINIJVERSION) < 0) {
			error("This plugin requires ImageJ version "+MINIJVERSION+" or higher");
			return false;
		}

		try {
			if (ImageScience.version().compareTo(MINISVERSION) < 0)
			throw new IllegalStateException();
		} catch (Throwable e) {
			error("This plugin requires ImageScience version "+MINISVERSION+" or higher");
			return false;
		}

		return true;
	}

	static ImagePlus imageplus() {

		final ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)  {
			error("There are no images open");
			return null;
		}

		final int type = imp.getType();
		if (type != ImagePlus.GRAY8 && type != ImagePlus.GRAY16 && type != ImagePlus.GRAY32) {
			error("The image is not a gray-scale image");
			return null;
		}

		return imp;
	}

	static void show(final Image img, final ImagePlus imp) {

		ImagePlus newimp = img.imageplus();
		transfer(imp,newimp);
		final double[] minmax = img.extrema();
		final double min = minmax[0], max = minmax[1];
		if (!RJ_Options.adopt) newimp.setDisplayRange(min,max);

		switch (type(imp)) {

			case IMAGE5D: {
				newimp = I5DResource.convert(newimp,true);
				I5DResource.transfer(imp,newimp);
				if (!RJ_Options.adopt)
					I5DResource.minmax(newimp,min,max);
				break;
			}
			case COMPOSITEIMAGE: {
				final CompositeImage cimp = (CompositeImage)imp;
				final CompositeImage newcimp = new CompositeImage(newimp,cimp.getMode());
				newcimp.copyLuts(cimp);
				if (!RJ_Options.adopt) {
					final int nc = newcimp.getNChannels();
					for (int c=1; c<=nc; ++c) {
						final LUT lut = newcimp.getChannelLut(c);
						lut.min = min; lut.max = max;
					}
				}
				newimp = newcimp;
				break;
			}
			case HYPERSTACK: {
				newimp.setOpenAsHyperStack(true);
				break;
			}
		}

		newimp.changes = RJ_Options.save;

		log("Showing result image");
		newimp.show();

		if (RJ_Options.close) {
			log("Closing input image");
			imp.close();
		}
	}

	static void transfer(final ImagePlus src, final ImagePlus dst) {

		// Calibration:
		dst.setCalibration(src.getCalibration());

		// Color model:
		final ColorModel cm = src.getProcessor().getColorModel();
		dst.getProcessor().setColorModel(cm);
		dst.getStack().setColorModel(cm);

		// Min and max displaying:
		dst.setDisplayRange(src.getDisplayRangeMin(),src.getDisplayRangeMax());
	}

	static final int SINGLEIMAGE=1, IMAGESTACK=2, HYPERSTACK=3, COMPOSITEIMAGE=4, IMAGE5D=5;

	static int type(final ImagePlus imp) {

		int type = SINGLEIMAGE;
		boolean i5dexist = false;
		try { Class.forName("i5d.Image5D"); i5dexist = true; } catch (Throwable e) { }
		if (i5dexist && I5DResource.instance(imp)) type = IMAGE5D;
		else if (imp.isComposite()) type = COMPOSITEIMAGE;
		else if (imp.isHyperStack()) type = HYPERSTACK;
		else if (imp.getImageStackSize() > 1) type = IMAGESTACK;
		return type;
	}

	static void error(final String message) {

		IJ.showMessage(NAME+": Error",message+".");
		IJ.showProgress(1);
		IJ.showStatus("");
	}

	static void log(final String message) {

		if (RJ_Options.log) IJ.log(message);
	}

}
