package vib;

import ij.ImagePlus;
import vib.InterpolatedImage;

/*

 This plugin implements the Bilateral Filter, described in

  C. Tomasi and R. Manduchi, "Bilateral Filtering for Gray and Color Images",
  Proceedings of the 1998 IEEE International Conference on Computer Vision,
  Bombay, India.

 Basically, it does a Gaussian blur taking into account the intensity domain
 in addition to the spatial domain (i.e. pixels are smoothed when they are
 close together _both_ spatially and by intensity.

*/
public class BilateralFilter {

	public static ImagePlus filter(
				ImagePlus image,
				double spatialRadius,
				double rangeRadius) {

		final InterpolatedImage orig = new InterpolatedImage(image);
		InterpolatedImage res = orig.cloneDimensionsOnly();
		final float[] spatial = makeKernel(spatialRadius);
		final float[] range = makeKernel(rangeRadius);
		res.image.setTitle(orig.image.getTitle()
				+ "-" + spatialRadius + "-" + rangeRadius);

		InterpolatedImage.Iterator iter = res.iterator(true);
		InterpolatedImage o = orig;
		float[] s = spatial;
		int sc = spatial.length / 2;
		float[] r = range;
		int rc = range.length / 2;

		while (iter.next() != null) {
			int v0 = o.getNoInterpol(iter.i, iter.j, iter.k);
			float v = 0, total = 0;
			for (int n = 0; n < s.length; n++)
				for (int m = 0; m < s.length; m++) {
					int v1 = o.getNoInterpol(
							iter.i + m - sc,
							iter.j + n - sc,
							iter.k);
					if (Math.abs(v1 - v0) > rc)
						continue;
					float w = s[m] * s[n]
						* r[v1 - v0 + rc];
					v += v1 * w;
					total += w;
				}
			res.set(iter.i, iter.j, iter.k, (int)(v / total));
		}

		return res.image;
	}

	public static float[] makeKernel(double radius) {
		radius += 1;
		int size = (int)radius*2-1;
		float[] kernel = new float[size];
		float total = 0;
		for (int i=0; i<size; i++) {
			double x = (i + 1 - radius) / (radius * 2) / 0.2;
			float v = (float)Math.exp(-0.5 * x * x);
			kernel[i] = v;
			total += v;
		}
		if (total <= 0.0)
			for (int i = 0; i < size; i++)
				kernel[i] = 1.0f / size;
		else if (total != 1.0)
			for (int i = 0; i < size; i++)
				kernel[i] /= total;
		return kernel;
	}
}

