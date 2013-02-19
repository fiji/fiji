import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/*

  This plugin implements the "triangle algorithm" of Zack et al,

  Zack, G. W., Rogers, W. E. and Latt, S. A., 1977,
  Automatic Measurement of Sister Chromatid Exchange Frequency,
  Journal of Histochemistry and Cytochemistry 25 (7), pp. 741-753

*/

public class Triangle_Algorithm implements PlugInFilter {
	protected ImagePlus image;

	public void run(ImageProcessor ip) {
		int[] histogram = getHistogram(ip);
		int split = triangleAlgorithm(histogram);
		ip.setThreshold(split, 256, ImageProcessor.RED_LUT);
		image.updateAndDraw();
	}

	public int setup(String args, ImagePlus imp) {
		this.image = imp;
		return DOES_8G | NO_CHANGES;
	}

	int[] getHistogram(ImageProcessor ip) {
		int w = ip.getWidth(), h = ip.getHeight();
		byte[] pixels = (byte[])ip.getPixels();

		int[] result = new int[256];
		for (int i = 0; i < w * h; i++)
			result[pixels[i] & 0xff]++;

		return result;
	}

	int triangleAlgorithm(int[] histogram) {
		// find min and max
		int min = 0, max = 0;
		for (int i = 1; i < histogram.length; i++)
			if (histogram[min] > histogram[i])
				min = i;
			else if (histogram[max] < histogram[i])
				max = i;

		if (min == max)
			return min;

		// describe line by nx * x + ny * y - d = 0
		double nx, ny, d;
		nx = histogram[max] - histogram[min];
		ny = min - max;
		d = Math.sqrt(nx * nx + ny * ny);
		nx /= d;
		ny /= d;
		d = nx * min + ny * histogram[min];

		// find split point
		int split = min;
		double splitDistance = 0;
		for (int i = min + 1; i <= max; i++) {
			double newDistance = nx * i + ny * histogram[i] - d;
			if (newDistance > splitDistance) {
				split = i;
				splitDistance = newDistance;
			}
		}

		return split;
	}
}
