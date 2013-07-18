package fiji.plugin.trackmate.detection.semiauto;

import ij.ImagePlus;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.CropImgView;
import fiji.plugin.trackmate.util.TMUtils;

public class SemiAutoTracker<T extends RealType<T> & NativeType<T>> extends AbstractSemiAutoTracker<T> {

	protected final ImgPlus<T> img;

	@SuppressWarnings("unchecked")
	public SemiAutoTracker(final Model model, final SelectionModel selectionModel, final ImagePlus imp, final Logger logger) {
		super(model, selectionModel, logger);
		this.img = TMUtils.rawWraps(imp);
	}

	@Override
	protected SpotNeighborhood<T> getNeighborhood(final Spot spot, final int frame) {
		final double radius = spot.getFeature(Spot.RADIUS);

		/*
		 * Source, rai and transform
		 */

		final int tindex = TMUtils.findTAxisIndex(img);
		if (frame >= img.dimension(tindex)) {
			logger.log("Spot: " + spot + ": No more time-points.\n");
			return null;
		}

		/*
		 * Extract scales
		 */

		final double[] cal = TMUtils.getSpatialCalibration(img);
		final double dx = cal[0];
		final double dy = cal[1];
		final double dz = cal[2];

		/*
		 * Determine neighborhood size
		 */

		final double neighborhoodFactor = Math.max(NEIGHBORHOOD_FACTOR, distanceTolerance + 1);

		/*
		 * Extract source coords
		 */

		final double[] location = new double[3];
		spot.localize(location);

		final long x = Math.round(location[0] / dx);
		final long y = Math.round(location[1] / dy);
		final long z = Math.round(location[2] / dz);
		final long r = (long) Math.ceil(neighborhoodFactor * radius / dx);
		final long rz = (long) Math.ceil(neighborhoodFactor * radius / dz);

		/*
		 * Extract crop cube
		 */

		final int targetChannel = 0; // TODO when spot will store the channel they are created on, use it.
		final ImgPlus<T> imgC = HyperSliceImgPlus.fixChannelAxis(img, targetChannel);
		final ImgPlus<T> imgT = HyperSliceImgPlus.fixTimeAxis(imgC, frame);

		final long width = imgT.dimension(0);
		final long height = imgT.dimension(1);
		final long depth = imgT.dimension(2);

		final long x0 = Math.max(0, x - r);
		final long y0 = Math.max(0, y - r);
		final long z0 = Math.max(0, z - rz);

		final long x1 = Math.min(width - 1, x + r);
		final long y1 = Math.min(height - 1, y + r);
		final long z1 = Math.min(depth - 1, z + rz);

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

		final Img<T> cropimg = new CropImgView<T>(imgT, min, max, new ArrayImgFactory<T>());

		/*
		 * The transform that will put back the global coordinates. In our case
		 * it is just a translation.
		 */

		final AffineTransform3D transform = new AffineTransform3D();
		for (int i = 0; i < max.length; i++) {
			transform.set(min[i] * cal[i], i, 3);
		}

		/*
		 * Give it a calibration
		 */

		final AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
		final ImgPlus<T> imgplus = new ImgPlus<T>(cropimg, "crop", axes, cal);

		final SpotNeighborhood<T> sn = new SpotNeighborhood<T>();
		sn.neighborhood = imgplus;
		sn.transform = transform;

		return sn;
	}

	@Override
	protected void exposeSpot(final Spot newSpot, final Spot previousSpot) {
	}

}
