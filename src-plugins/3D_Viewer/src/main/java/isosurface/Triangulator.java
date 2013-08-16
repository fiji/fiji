package isosurface;

import java.util.List;
import ij.ImagePlus;

public interface Triangulator {
	/**
	 * This method must return a list of elements of class Point3f.
	 * Three subsequent points specify one triangle.
	 * @param image the ImagePlus to be displayed
	 * @param threshold the isovalue of the surface to be generated.
	 * @param channels an array containing 3 booleans, indicating which
	 *                 of red, green and blue to use for the Triangulation.
	 * @param resamplingF resampling factor
	 */
	public List getTriangles(ImagePlus image, int threshold, 
						boolean[] channels, int resamplingF);
}
