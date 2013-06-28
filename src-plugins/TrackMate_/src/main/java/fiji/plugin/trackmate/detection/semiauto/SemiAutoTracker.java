package fiji.plugin.trackmate.detection.semiauto;

import ij.ImagePlus;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.CropImgView;
import fiji.plugin.trackmate.util.TMUtils;

public class SemiAutoTracker<T extends RealType<T>  & NativeType<T>> extends AbstractSemiAutoTracker<T> {

	protected final ImgPlus<T> img;

	@SuppressWarnings("unchecked")
	public SemiAutoTracker(Model model, SelectionModel selectionModel, ImagePlus imp, Logger logger) {
		super(model, selectionModel, logger);
		this.img = TMUtils.rawWraps(imp);
	}

	@Override
	protected SpotNeighborhood<T> getNeighborhood(Spot spot, int frame) {
		double radius = spot.getFeature(Spot.RADIUS);

		/*
		 * Source, rai and transform
		 */

		int tindex = TMUtils.findTAxisIndex(img);
		if (frame >= img.dimension(tindex)) {
			logger.log("Spot: " + spot + ": No more time-points.\n");
			return null;
		}

		/*
		 * Extract scales
		 */

		double[] cal = TMUtils.getSpatialCalibration(img);
		double dx = cal[0];
		double dy = cal[1];
		double dz = cal[2];

		/*
		 * Determine neighborhood size
		 */
		
		double neighborhoodFactor = Math.max(NEIGHBORHOOD_FACTOR, distanceTolerance + 1);

		/*
		 * Extract source coords
		 */
		
		double[] location = new double[3];
		spot.localize(location);
		
		long x = Math.round(location[0] / dx);
		long y = Math.round(location[1] / dy);
		long z = Math.round(location[2] / dz);
		long r = (long) Math.ceil(neighborhoodFactor * radius / dx);
		long rz = (long) Math.ceil(neighborhoodFactor * radius / dz);
		
		/*
		 * Extract crop cube
		 */
		
		final int targetChannel = 0; // TODO when spot will store the channel they are created on, use it.
		final ImgPlus<T> imgC = HyperSliceImgPlus.fixChannelAxis(img, targetChannel);
		final ImgPlus<T> imgT = HyperSliceImgPlus.fixTimeAxis(imgC, frame);
		
		long width = imgT.dimension(0);
		long height = imgT.dimension(1);
		long depth = imgT.dimension(2);
		
		long x0 = Math.max(0, x - r);
		long y0 = Math.max(0, y - r);
		long z0 = Math.max(0, z - rz);
		
		long x1 = Math.min(width-1, x + r);
		long y1 = Math.min(height-1, y + r);
		long z1 = Math.min(depth-1, z + rz);
		
		long[] min;
		long[] max;
		if (img.dimension(TMUtils.findZAxisIndex(img)) > 1) {
			// 3D
			min = new long[] { x0, y0, z0 };
			max = new long[] { x1, y1, z1 };
		} else {
			// 2D
			min = new long[] { x0, y0 };
			max = new long[] { x1, y1 };
		}
		

		Img<T> cropimg = new CropImgView<T>(imgT, min, max, new ArrayImgFactory<T>());

		/*
		 * Give it a calibration
		 */

		AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
		ImgPlus<T> imgplus = new ImgPlus<T>(cropimg, "crop", axes, cal);
		
		SpotNeighborhood<T> sn = new SpotNeighborhood<T>();
		sn.neighborhood = imgplus;
		sn.topLeftCorner = min;
		sn.calibration = cal;
		
		return sn;
	}

}
