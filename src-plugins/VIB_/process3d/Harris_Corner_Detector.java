package process3d;

import ij.IJ;
import ij.ImageStack;
import ij.ImagePlus;
import ij.process.StackConverter;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.plugin.filter.PlugInFilter;
import ij.measure.Calibration;

import vib.*;

public class Harris_Corner_Detector implements PlugInFilter {

	private ImagePlus image;
	private ImagePlus XX, YY, ZZ, XY, XZ, YZ;

	public static final float HARRIS_THRESHOLD = 20f;
	public static final float K = 0.14f;


	public Harris_Corner_Detector() {
	}

	public Harris_Corner_Detector(ImagePlus img) {
		this.image = img;
	}

	public void run(ImageProcessor ip) {
		findCorners().show();
	}

	public ImagePlus findCorners() {
		makeDerivatives();
		ImagePlus imp = createCRF();
		imp.show();
		imp = suppressNonMaximum(imp);
		return imp;
	}

	public ImagePlus createCRF() {
		IJ.showStatus("Create crf");
		InterpolatedImage 
			iXX = new InterpolatedImage(XX),
			iXY = new InterpolatedImage(XY),
			iXZ = new InterpolatedImage(XZ),
			iYY = new InterpolatedImage(YY),
			iYZ = new InterpolatedImage(YZ),
			iZZ = new InterpolatedImage(ZZ),
			out = iXX.cloneDimensionsOnly();
		InterpolatedImage.Iterator it = iXX.iterator();
		float tr, det, v;
		while(it.next() != null) {
			float gxx = iXX.getNoCheckFloat(it.i, it.j, it.k);
			float gxy = iXY.getNoCheckFloat(it.i, it.j, it.k);
			float gxz = iXZ.getNoCheckFloat(it.i, it.j, it.k);
			float gyy = iYY.getNoCheckFloat(it.i, it.j, it.k);
			float gyz = iYZ.getNoCheckFloat(it.i, it.j, it.k);
			float gzz = iZZ.getNoCheckFloat(it.i, it.j, it.k);

			tr = gxx + gyy + gzz;
			det = gxx * (gyy * gyy - gyz * gyz)
				- gxy * (gxy * gzz - gyz * gxz)
				+ gxz * (gxy * gyz - gyy * gxz);
// 			v = tr == 0 ? 0 : det / tr;
			v = det - K * tr * tr;
			out.setFloat(it.i, it.j, it.k, v);
		}
		XX = XY = XZ = YY = YZ = ZZ = null;
		System.gc();
		return out.getImage();
	}

	public void makeDerivatives() {
		IJ.showStatus("Calculate derivatives");
		float sigma = 2f;
		float[] H = new float[] {-1f/2, 0, 1f/2};
		ImagePlus smooth = Smooth_.smooth(image, true, sigma, false);
		ImagePlus dx = Convolve3d.convolveX(smooth, H);
		ImagePlus dy = Convolve3d.convolveY(smooth, H);
		ImagePlus dz = Convolve3d.convolveZ(smooth, H);

		XX = Smooth_.smooth(mul(dx, dx), true, sigma, false);
		XY = Smooth_.smooth(mul(dx, dy), true, sigma, false);
		XZ = Smooth_.smooth(mul(dx, dz), true, sigma, false);
		YY = Smooth_.smooth(mul(dy, dy), true, sigma, false);
		YZ = Smooth_.smooth(mul(dy, dz), true, sigma, false);
		ZZ = Smooth_.smooth(mul(dz, dz), true, sigma, false);
	}

	public static final ImagePlus mul(ImagePlus imp1, ImagePlus imp2) {
		InterpolatedImage ii1 = new InterpolatedImage(imp1);
		InterpolatedImage ii2 = new InterpolatedImage(imp2);
		InterpolatedImage out = ii1.cloneDimensionsOnly();
		InterpolatedImage.Iterator it = ii1.iterator();
		while(it.next() != null) {
			out.setFloat(it.i, it.j, it.k,
				ii1.getNoCheckFloat(it.i, it.j, it.k) * 
				ii2.getNoCheckFloat(it.i, it.j, it.k));
		}
		return out.getImage();
	}

	public static ImagePlus suppressNonMaximum(ImagePlus img) {
		IJ.showStatus("Suppress non-maximum points");
		InterpolatedImage ii = new InterpolatedImage(img);
		InterpolatedImage out = ii.cloneDimensionsOnly();
		InterpolatedImage.Iterator it = ii.iterator();

		int mask_n = 27;
		int m_x, m_y, m_z, xi, yi, zi;
		float v;
		while(it.next() != null) {
			v = ii.getNoInterpolFloat(it.i, it.j, it.k);
			if(v < HARRIS_THRESHOLD)
				continue;
 			out.setFloat(it.i, it.j, it.k, 100f);
// 			out.setFloat(it.i, it.j, it.k, v);
			for(int i = 0; i < mask_n; i++) {
				if(i == mask_n/2)
					continue;
				m_z = i / 9 - 3 / 2;
				m_y = (i % 9) / 3 - 3 / 2;
				m_x = (i % 9) % 3 - 3 / 2;

				xi = it.i + m_x;
				yi = it.j + m_y;
				zi = it.k + m_z;

				if(ii.getNoInterpolFloat(xi, yi, zi) >= v) {
					out.setFloat(it.i, it.j, it.k, 0f);
					break;
				}
			}
		}
		return out.getImage();
	}

// 	public static PointList selectLMs(ImagePlus image) {
// 		InterpolatedImage ii = new InterpolatedImage(image);
// 
// 		int n = (ii.w / SUBV_W) * (ii.h / SUBV_H) * (ii.d / SUBV_D);
// 		float[] max = new float[n];
// 		BenesNamedPoint[] lms = new BenesNamedPoint[n];
// 		for(int i = 0; i < n; i++) {
// 			max[i] = Float.MIN_VALUE;
// 			lms[i] = new BenesNamedPoint("point" + i);
// 		}
// 
// 		int ix, iy, iz, i;
// 		for(int z = 0; z < ii.d; z++) {
// 			iz = z / SUBV_D;
// 			for(int y = 0; y < ii.h; y++) {
// 				iy = y / SUBV_H;
// 				for(int x = 0; x < ii.w; x++) {
// 					ix = x / SUBV_W;
// 					i = (iz * (ii.h / SUBV_H) + iy) 
// 						* (ii.w / SUBV_W) + ix;
// 					float v = ii.getNoCheckFloat(x, y, z);
// 					if(v > HARRIS_THRESHOLD && v > max[i]) {
// 						max[i] = v;
// 						lms[i].set(x, y, z);
// 					}
// 				}
// 			}
// 		}
// 		Calibration cal = image.getCalibration();
// 		PointList pl = new PointList();
// 		for(i = 0; i < n; i++) {
// 			lms[i].x *= cal.pixelWidth;
// 			lms[i].y *= cal.pixelHeight;
// 			lms[i].z *= cal.pixelDepth;
// 			pl.add(lms[i]);
// 		}
// 		return pl;
// 	}

// 	public static void normalize(ImagePlus image) {
// 		InterpolatedImage ii = new InterpolatedImage(image);
// 		InterpolatedImage.Iterator it = ii.iterator();
// 		int N = ii.w * ii.h * ii.d;
// 		// calculate mean
// 		double mean = 0;
// 		while(it.next() != null)
// 			mean += ii.getNoCheckFloat(it.i, it.j, it.k);
// 		mean /= N;
// 
// 		it = ii.iterator();
// 		// calculate standard deviation
// 		double stddev = 0;
// 		while(it.next() != null) {
// 			float v = ii.getNoCheckFloat(it.i, it.j, it.k);
// 			stddev += (v - mean) * (v - mean);
// 		}
// 		stddev = Math.sqrt(stddev / N);
// 
// 		// calculate Z statistics
// 		it = ii.iterator();
// 		while(it.next() != null) {
// 			float v = (float)(ii.getNoCheckFloat(it.i,it.j,it.k)
// 				- mean) / (float)stddev;
// 			ii.setFloat(it.i, it.j, it.k, v);
// 		}
// 	}

	public int setup(String arg, ImagePlus imp) {
		this.image = new InterpolatedImage(imp).cloneImage().getImage();
		new StackConverter(image).convertToGray32();
		return DOES_8G | DOES_32;
	}
}
