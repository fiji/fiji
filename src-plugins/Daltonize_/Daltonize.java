/*
 * To "daltonize" an image means to simulate color blindness.
 *
 * This is the implementation of the algorithm presented by Onur Fidaner,
 * Poliang Lin and Nevran Ozguven at:
 *
 * http://www.stanford.edu/~ofidaner/psych221_proj/colorblindness_project.htm
 *
 * Paraphrased from this website:
 *
 * The idea is to simulate color blindness by assuming either the L cone
 * or the M cone is missing and deleting information related to the cone of
 * interest.  This image roughly represents the normal image perceived by
 * a dichromat.  Then, subtracting this from the original image, we find
 * the information lost when the image is seen by a dichromat.  We then
 * make a transformation on the error function so as to map it to
 * something that could be perceived by a dichromat.
 *
 * For that we take the red component of the information, which is likely
 * to be lost the most, and then using some weight function to add it to
 * the blue and the red component.  Finally we add this new modified error
 * function to the original image.
 *
 * This implementation is (C) 2008 Johannes E. Schindelin, licensed under
 * the GPLv2.
 */
import ij.IJ;
import ij.ImagePlus;

import ij.gui.GenericDialog;

import ij.plugin.filter.PlugInFilter;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class Daltonize implements PlugInFilter {
	ImagePlus image;

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_RGB | DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		String[] labels = { "Deuteranope", "Protanope", "Tritanope" };
		GenericDialog gd = new GenericDialog("Daltonize");
		gd.addChoice("mode", labels, labels[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int mode = gd.getNextChoiceIndex();
		/*
		 * read this in reverse: first, RGB is transformed into LMS,
		 * then the vision defect is applied, back to RGB, subtract
		 * that from the original, apply the error modification,
		 * and add it to the original image.
		 */
		float[] matrix = add(identity,
			mul(errMod,
			sub(identity,
			mul(mul(lms2rgb,
			mode == 1 ? lms2lmsp : mode == 2 ? lms2lmst : lms2lmsd),
			rgb2lms))));

		int[] pixels = (int[])ip.getPixels();
		for (int i = 0; i < pixels.length; i++)
			pixels[i] = mul(matrix, pixels[i]);
	}

	public static final float[] lms2lmsp = {
		0, 2.02344f, -2.52581f,
		0, 1, 0,
		0, 0, 1
	};
	public static final float[] lms2lmsd = {
		1, 0, 0,
		0.494207f, 0, 1.24827f,
		0, 0, 1
	};
	public static final float[] lms2lmst = {
		1, 0, 0,
		0, 1, 0,
		-0.395913f, 0.801109f, 0
	};
	public static final float[] identity = {
		1, 0, 0,
		0, 1, 0,
		0, 0, 1
	};
	public static final float[] errMod = {
		0, 0, 0,
		0.7f, 1, 0,
		0.7f, 0, 1
	};

	public static final float[] rgb2lms = {
		17.8824f, 43.5161f, 4.11935f,
		3.45565f, 27.1554f, 3.86714f,
		0.0299566f, 0.184309f, 1.46709f
	};
	public static final float[] lms2rgb = {
		0.08094445f, -0.13050441f, 0.11672109f,
		-0.010248534f, 0.05401933f, -0.113614716f,
		-3.6529682E-4f, -0.004121615f, 0.6935114f
	};

	// add two matrices
	static float[] add(float[] a, float[] b) {
		float[] result = new float[3 * 3];
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				result[i * 3 + j] += a[i * 3 + j]
					+ b[i * 3 + j];
		return result;
	}

	// subtract matrix b from matrix a
	static float[] sub(float[] a, float[] b) {
		float[] result = new float[3 * 3];
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				result[i * 3 + j] += a[i * 3 + j]
					- b[i * 3 + j];
		return result;
	}

	// multiply two matrices
	static float[] mul(float[] a, float[] b) {
		float[] result = new float[3 * 3];
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				for (int k = 0; k < 3; k++)
					result[i * 3 + j] += a[i * 3 + k]
						* b[k * 3 + j];
		return result;
	}

	static int toByte(float f) {
		return (int)Math.round(Math.max(0f, Math.min(255f, f)));
	}

	// multiply a matrix with a (bit-packed) vector
	static int mul(float[] a, int rgb) {
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >> 8) & 0xff;
		int b = rgb & 0xff;
		return toByte(a[0] * r + a[1] * g + a[2] * b) << 16 |
			toByte(a[3] * r + a[4] * g + a[5] * b) << 8 |
			toByte(a[6] * r + a[7] * g + a[8] * b);
	}

	static String toString(float[] a) {
		String result = "[";
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				result += a[i * 3 + j] + (j == 2 ? "; " : " ");
		return result;
	}
}
