import fiji.JNI;

import ij.ImagePlus;
import ij.ImageStack;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

import org.scijava.util.POM;

public class JNI_Example implements PlugInFilter {
	protected String arg;
	protected ImagePlus image;

	public int setup(String arg, ImagePlus image) {
		this.arg = arg;
		this.image = image;
		return DOES_ALL | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		int[] dimensions = image.getDimensions();

		final String version = POM.getPOM(getClass(), "sc.fiji", "JNI_Example").getVersion();
		JNI.loadLibrary("JNI_Example-" + version);
		run(arg, image.getTitle(),
			dimensions[0], dimensions[1], dimensions[2], dimensions[3], dimensions[4],
			getPixels(image));
	}

	// TODO: maybe JNI would be fast enough to access pixels via a per-plane method call?
	protected static Object[] getPixels(ImagePlus image) {
		ImageStack stack = image.getStack();
		Object[] pixels = new Object[stack.getSize()];
		for (int i = 0; i < pixels.length; i++)
			pixels[i] = stack.getProcessor(i + 1).getPixels();
		return pixels;
	}

	public native static Object run(String arg, String title,
		int width, int height, int channels, int slices, int frames,
		Object[] pixels);
}
