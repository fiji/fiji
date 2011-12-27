/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package features;

import ij.ImageJ;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;

public abstract class HessianEvalueProcessor implements GaussianGenerationCallback {

	public void proportionDone(double d) {
		IJ.showProgress(d);
	}

	public abstract float measureFromEvalues2D( float [] evalues );
	public abstract float measureFromEvalues3D( float [] evalues );

	protected boolean normalize = false;
	protected double sigma = 1.0;
	protected boolean useCalibration = false;

	public void setSigma( double newSigma ) {
		sigma = newSigma;
	}

	public ImagePlus generateImage(ImagePlus original) {

		Calibration calibration=original.getCalibration();

		float sepX = 1, sepY = 1, sepZ = 1;
		if( useCalibration && (calibration!=null) ) {
			sepX = (float)calibration.pixelWidth;
			sepY = (float)calibration.pixelHeight;
			sepZ = (float)calibration.pixelDepth;
		}

		double minimumSeparation = Math.min(sepX,
						    Math.min(sepY,sepX));

		ComputeCurvatures c = new ComputeCurvatures(original, sigma, this, useCalibration);
		IJ.showStatus("Convolving with Gaussian \u03C3="+sigma+" (min. pixel separation: "+minimumSeparation+")...");
		c.run();

		int width = original.getWidth();
		int height = original.getHeight();
		int depth = original.getStackSize();

		ImageStack stack = new ImageStack(width, height);

		float[] evalues = new float[3];

		IJ.showStatus("Calculating Hessian eigenvalues at each point...");

		float minResult = Float.MAX_VALUE;
		float maxResult = Float.MIN_VALUE;

		if( depth == 1 ) {

			float[] slice = new float[width * height];

			for (int y = 1; y < height - 1; ++y) {
				for (int x = 1; x < width - 1; ++x) {

					boolean real = c.hessianEigenvaluesAtPoint2D(x, y,
										     true, // order absolute
										     evalues,
										     normalize,
										     false,
										     sepX,
										     sepY);
					int index = y * width + x;
					float value = 0;
					if( real )
						value = measureFromEvalues2D(evalues);
					slice[index] = value;
					if( value < minResult )
						minResult = value;
					if( value > maxResult )
						maxResult = value;

				}
				IJ.showProgress(1 / (double) height);
			}

			FloatProcessor fp = new FloatProcessor(width, height);
			fp.setPixels(slice);
			stack.addSlice(null, fp);

		} else {

			for (int z = 0; z < depth; ++z) {

				float[] slice = new float[width * height];

				if ((z >= 1) && (z < depth - 1)) {
					for (int y = 1; y < height - 1; ++y) {
						for (int x = 1; x < width - 1; ++x) {

							boolean real = c.hessianEigenvaluesAtPoint3D(x, y, z,
												     true, // order absolute
												     evalues,
												     normalize,
												     false,
												     sepX,
												     sepY,
												     sepZ);
							int index = y * width + x;
							float value = 0;
							if( real )
								value = measureFromEvalues3D(evalues);
							slice[index] = value;
							if( value < minResult )
								minResult = value;
							if( value > maxResult )
								maxResult = value;
						}
					}
				}

				FloatProcessor fp = new FloatProcessor(width, height);
				fp.setPixels(slice);
				stack.addSlice(null, fp);
				IJ.showProgress(z / (double) depth);
			}
		}

		IJ.showProgress(1.0);

		ImagePlus result=new ImagePlus("processed " + original.getTitle(), stack);
		result.setCalibration(calibration);


		result.getProcessor().setMinAndMax(minResult,maxResult);
		result.updateAndDraw();

		return result;
	}
}
