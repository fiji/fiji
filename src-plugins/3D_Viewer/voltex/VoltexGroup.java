package voltex;

import java.awt.Polygon;

import javax.vecmath.Color3f;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.media.j3d.View;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;

import ij.ImagePlus;
import ij.IJ;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import ij3d.ContentInstant;
import ij3d.Content;
import ij3d.ContentNode;

import javax.vecmath.Tuple3d;
import vib.NaiveResampler;

/**
 * This class extends ContentNode to display a Content as a
 * Volume Rendering.
 *
 * @author Benjamin Schmid
 */
public class VoltexGroup extends ContentNode {

	/** The VolumeRenderer behind this VoltexGroup */
	protected VolumeRenderer renderer;

	/** Reference to the Content which holds this VoltexGroup */
	protected ContentInstant c;

	/** The volume of this VoltexGroup */
	private float volume;

	/** The minimum coordinate of this VoltexGroup */
	private Point3d min;

	/** The maximum coordinate of this VoltexGroup */
	private Point3d max;

	/** The center point of this VoltexGroup */
	private Point3d center;

	/**
	 * This constructor only exists to allow subclasses to access the super
	 * constructor of BranchGroup.
	 */
	protected VoltexGroup() {
		super();
	}

	/**
	 * Initialize this VoltexGroup with the specified Content.
	 * @param c
	 * @throws IllegalArgumentException if the specified Content has no
	 *         image.
	 */
	public VoltexGroup(Content c) {
		this(c.getCurrent());
	}

	/**
	 * Initialize this VoltexGroup with the specified ContentInstant.
	 * @param c
	 * @throws IllegalArgumentException if the specified ContentInstant has no image.
	 */
	public VoltexGroup(ContentInstant c) {
		super();
		if(c.getImage() == null)
			throw new IllegalArgumentException("VoltexGroup can only" +
				"be initialized from a ContentInstant that holds an image.");
		this.c = c;
		ImagePlus imp = c.getResamplingFactor() == 1
			? c.getImage()
			: NaiveResampler.resample(c.getImage(), c.getResamplingFactor());
		renderer = new VolumeRenderer(imp, c.getColor(),
				c.getTransparency(), c.getChannels());
		int[] rLUT = new int[256];
		int[] gLUT = new int[256];
		int[] bLUT = new int[256];
		int[] aLUT = new int[256];
		renderer.volume.getRedLUT(rLUT);
		renderer.volume.getGreenLUT(gLUT);
		renderer.volume.getBlueLUT(bLUT);
		renderer.volume.getAlphaLUT(aLUT);
		c.setLUT(rLUT, gLUT, bLUT, aLUT);
		renderer.fullReload();
		calculateMinMaxCenterPoint();
		addChild(renderer.getVolumeNode());
	}

	/**
	 * Update the volume rendering from the image
	 * (only if the resampling factor is 1.
	 */
	public void update() {
		if(c.getResamplingFactor() != 1)
			return;
		renderer.getVolume().updateData();
	}

	/**
	 * Get a reference VolumeRenderer which is used by this class
	 */
	public VolumeRenderer getRenderer() {
		return renderer;
	}

	public Mask createMask() {
		return renderer.createMask();
	}

	/**
	 * @see ContentNode#getMin(Tupe3d) getMin
	 */
	public void getMin(Tuple3d min) {
		min.set(this.min);
	}

	/**
	 * @see ContentNode#getMax (Tupe3d) getMax
	 */
	public void getMax(Tuple3d max) {
		max.set(this.max);
	}

	/**
	 * @see ContentNode#getCenter(Tupe3d) getCenter
	 */
	public void getCenter(Tuple3d center) {
		center.set(this.center);
	}

	/**
	 * @see ContentNode#thresholdUpdated() thresholdUpdated
	 */
	public void thresholdUpdated(int threshold) {
		renderer.setThreshold(threshold);
	}

	/**
	 * @see ContentNode#getVolume() getVolume
	 */
	public float getVolume() {
		return volume;
	}

	/**
	 * @see ContentNode#eyePtChanged(View view) eyePtChanged
	 */
	public void eyePtChanged(View view) {
		renderer.eyePtChanged(view);
	}

	/**
	 * @see ContentNode#channelsUpdated() channelsUpdated
	 */
	public void channelsUpdated(boolean[] channels) {
		renderer.setChannels(channels);
	}

	/**
	 * @see ContentNode#lutUpdated() lutUpdated
	 */
	public void lutUpdated(int[] r, int[] g, int[] b, int[] a) {
		renderer.setLUTs(r, g, b, a);
	}

	/**
	 * @see ContentNode#shadeUpdated() shadeUpdated
	 */
	public void shadeUpdated(boolean shaded) {
		// do nothing
	}

	/**
	 * @see ContentNode#colorUpdated() colorUpdated
	 */
	public void colorUpdated(Color3f color) {
		renderer.setColor(color);
	}

	/**
	 * @see ContentNode#transparencyUpdated() transparencyUpdated
	 */
	public void transparencyUpdated(float transparency) {
		renderer.setTransparency(transparency);
	}

	/**
	 * Stores the matrix which transforms this VoltexGroup to the
	 * image plate in the specified Transform3D.
	 * @param toImagePlate
	 */
	public void volumeToImagePlate(Transform3D toImagePlate) {
		Transform3D toVWorld = new Transform3D();
		renderer.getVolumeNode().getLocalToVworld(toVWorld);
		toImagePlate.mul(toVWorld);
	}

	/**
	 * Fills the projection of the specified ROI with the given fillValue.
	 * Does nothing if the given ROI is null.
	 * Works not only on the internally created image (the resampled one),
	 * but also on the original image.
	 * @param universe
	 * @param fillValue
	 */
	public void fillRoi(Canvas3D canvas, Roi roi, byte fillValue) {
		if(roi == null)
			return;

		Polygon p = roi.getPolygon();
		Transform3D volToIP = new Transform3D();
		canvas.getImagePlateToVworld(volToIP);
		volToIP.invert();
		volumeToImagePlate(volToIP);

		VoltexVolume vol = renderer.getVolume();
		Point2d onCanvas = new Point2d();
		for(int z = 0; z < vol.zDim; z++) {
			for(int y = 0; y < vol.yDim; y++) {
				for(int x = 0; x < vol.xDim; x++) {
					volumePointInCanvas(canvas, volToIP,
							x, y, z, onCanvas);
					if(p.contains(onCanvas.x, onCanvas.y)) {
						vol.setNoCheckNoUpdate(
							x, y, z, fillValue);
					}
				}
			}
			IJ.showStatus("Filling...");
			IJ.showProgress(z, vol.zDim);
		}
		vol.updateData();

		// also fill the original image
		ImagePlus image = c.getImage();
		int factor = c.getResamplingFactor();
		if(image == null || factor == 1)
			return;

		ij3d.Volume volu = new ij3d.Volume(image);
		for(int z = 0; z < volu.zDim; z++) {
			for(int y = 0; y < volu.yDim; y++) {
				for(int x = 0; x < volu.xDim; x++) {
					volumePointInCanvas(canvas,
							volToIP,
							x/factor,
							y/factor,
							z/factor,
							onCanvas);
					if(p.contains(onCanvas.x, onCanvas.y)) {
						volu.set(x, y, z, fillValue);
					}
				}
			}
			IJ.showStatus("Filling...");
			IJ.showProgress(z, volu.zDim);
		}
	}

	/**
	 * Returns the 3D coordinates of the given x, y, z position on the
	 * 3D canvas.
	 * @param canvas
	 * @param volToIP
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	private void volumePointInCanvas(Canvas3D canvas,
		Transform3D volToIP, int x, int y, int z, Point2d ret) {

		VoltexVolume vol = renderer.volume;
		double px = x * vol.pw;
		double py = y * vol.ph;
		double pz = z * vol.pd;
		Point3d locInImagePlate = new Point3d(px, py, pz);

		volToIP.transform(locInImagePlate);

		canvas.getPixelLocationFromImagePlate(locInImagePlate, ret);
	}

	/**
	 * Calculate the minimum, maximum and center coordinate, together with
	 * the volume.
	 */
	protected void calculateMinMaxCenterPoint() {
		ImagePlus imp = c.getImage();
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		Calibration cal = imp.getCalibration();
		min = new Point3d();
		max = new Point3d();
		center = new Point3d();
		min.x = w * (float)cal.pixelHeight;
		min.y = h * (float)cal.pixelHeight;
		min.z = d * (float)cal.pixelDepth;
		max.x = 0;
		max.y = 0;
		max.z = 0;

		float vol = 0;
		for(int zi = 0; zi < d; zi++) {
			float z = zi * (float)cal.pixelDepth;
			ImageProcessor ip = imp.getStack().getProcessor(zi+1);

			int wh = w * h;
			for(int i = 0; i < wh; i++) {
				float v = ip.getf(i);
				if(v == 0) continue;
				vol += v;
				float x = (i % w) * (float)cal.pixelWidth;
				float y = (i / w) * (float)cal.pixelHeight;
				if(x < min.x) min.x = x;
				if(y < min.y) min.y = y;
				if(z < min.z) min.z = z;
				if(x > max.x) max.x = x;
				if(y > max.y) max.y = y;
				if(z > max.z) max.z = z;
				center.x += v * x;
				center.y += v * y;
				center.z += v * z;
			}
		}
		center.x /= vol;
		center.y /= vol;
		center.z /= vol;

		volume = (float)(vol * cal.pixelWidth
				* cal.pixelHeight
				* cal.pixelDepth);

	}

	@Override
	public void swapDisplayedData(String path, String name) {
		renderer.volume.swap(path + ".tif");
		renderer.disableTextures();
	}

	@Override
	public void clearDisplayedData() {
		renderer.volume.clear();
		renderer.disableTextures();
	}

	@Override
	public void restoreDisplayedData(String path, String name) {
		renderer.volume.restore(path + ".tif");
		renderer.enableTextures();
	}
}
