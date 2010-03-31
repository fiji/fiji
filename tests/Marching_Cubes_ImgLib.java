/** Albert Cardona 2010-03-16 at EMBL */

import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImagePlus;
import java.awt.Color;
import javax.vecmath.Color3f;
import javax.media.j3d.Transform3D;
import marchingcubes.MCTriangulator;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.NumericType;
import ij3d.Image3DUniverse;
import ij3d.Content;
import java.util.List;

/** Test VIB marching cubes usage of the imglib with 8-bit and 16-bit images,
 *  then display both in 3D. */
public class Marching_Cubes_ImgLib implements PlugIn {

	public void run(String arg) {
		// Fetch ImagePlus and wrap in an Image
		ImagePlus imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/bat-cochlea-volume.zip");

		if (null == imp) {
			IJ.log("FAILED to fetch batc cochlea volume!");
			return;
		}

		Image3DUniverse univ = new Image3DUniverse(512, 512);
		univ.show(); // must be show before calling addMesh, otherwise menu for "Select" doesn't list the items

		Image<? extends NumericType> img = ImagePlusAdapter.wrap(imp);
		show3D(img, imp.getTitle() + " 8-bit red", Color.red, univ);

		// try in 16-bit, which should be read as unsigned as well:
		IJ.run(imp, "16-bit", "");
		Image<? extends NumericType> img16 = ImagePlusAdapter.wrap(imp);
		Content c = show3D(img16, imp.getTitle() + " 16-bit blue", Color.blue, univ);
		if (null != c) {
			// 50% scaled
			c.applyTransform(new Transform3D(new float[] {0.5f, 0, 0, 0,
								      0, 0.5f, 0, 0,
								      0, 0, 0.5f, 0,
								      0, 0, 0, 1}));
		}
		
	}

	private Content show3D(final Image<? extends NumericType> img, final String title, final Color color, final Image3DUniverse univ) {
		// Generate triangles
		try {
			List list = new MCTriangulator().getTriangles(img, 1, new float[3]);

			if (0 == list.size()) {
				IJ.log("Empty mesh!");
			} else {
				return univ.addMesh(list, new Color3f(color), title , 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
