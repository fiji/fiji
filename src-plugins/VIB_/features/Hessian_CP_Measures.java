/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package features;

import ij.ImageStack;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.measure.Calibration;
import ij.IJ;
import ij.plugin.PlugIn;

/* At the suggestion of Ting Zhao, this class is for trying out
   image filters based on the coefficients of the characteristic
   polynomial of the Hessian, rather than its eigenvalues and
   eigenvectors. */

public class Hessian_CP_Measures implements PlugIn, GaussianGenerationCallback {
	
	public void proportionDone(double d) {
		IJ.showProgress(d);
	}

	public void run(String ignore) {

		ImagePlus original = IJ.getImage();
		if( original == null ) {
			IJ.error("No images open");
			return;
		}
		
		Calibration calibration=original.getCalibration();
		boolean useCalibration = true;
		
		float sepX = 1, sepY = 1, sepZ = 1;
		if( useCalibration && (calibration!=null) ) {
			sepX = (float)calibration.pixelWidth;
			sepY = (float)calibration.pixelHeight;
			sepZ = (float)calibration.pixelDepth;
		}

		double minimumSeparation = Math.min(sepX,
						    Math.min(sepY,sepX));
		
		double sigma = minimumSeparation;

		ComputeCurvatures curvatures = new ComputeCurvatures(original, sigma, this, useCalibration);
		IJ.showStatus("Convolving with Gaussian \u03C3="+sigma+" (min. pixel separation: "+minimumSeparation+")...");
		curvatures.run();
		
		int width = original.getWidth();
		int height = original.getHeight();
		int depth = original.getStackSize();
		
		ImageStack constantStack = new ImageStack(width, height);
		ImageStack traceStack = new ImageStack(width, height);
		ImageStack newTubeStack = new ImageStack(width, height);
		
		float minConstantResult = Float.MAX_VALUE;
		float maxConstantResult = Float.MIN_VALUE;
		
		float minTraceResult = Float.MAX_VALUE;
		float maxTraceResult = Float.MIN_VALUE;

		float minNewTubeResult = Float.MAX_VALUE;
		float maxNewTubeResult = Float.MIN_VALUE;		
		
		for (int z = 0; z < depth; ++z) {
			
			float[] sliceConstant = new float[width * height];
			float[] sliceTrace = new float[width * height];
			float[] sliceNewTube = new float[width * height];
			
			if ((z >= 1) && (z < depth - 1)) {
				for (int y = 1; y < height - 1; ++y) {
					for (int x = 1; x < width - 1; ++x) {
						
						// m is the Hessian matrix:
						float [][] m = curvatures.computeHessianMatrix3DFloat(
							null,
							x, y, z,
							sigma,
							sepX, sepY, sepZ);

						float a = -1;
						float b = 
							+ m[0][0]  // A
							+ m[1][1]  // D
							+ m[2][2]; // F
						float c =
							+ m[0][1] * m[0][1]  // + BB
							+ m[0][2] * m[0][2]  // + CC
							+ m[1][2] * m[1][2]  // + EE
							- m[0][0] * m[1][1]  // - AD
							- m[0][0] * m[2][2]  // - AF
							- m[1][1] * m[2][2]; // - DF

						// ADF - AEE - BBF + 2BCE - CCD
						
						float d = 
							+ m[0][0] * m[1][1] * m[2][2]     //  ADF
							- m[0][0] * m[1][2] * m[1][2]     // -AEE
							- m[0][1] * m[0][1] * m[2][2]     // -BBF
							+ 2 * m[0][1] * m[0][2] * m[1][2] // 2BCE
							- m[0][2] * m[0][2] * m[1][1];    // -CCD
						
						/* c should approximate Math.sqrt(lamba_2*lambda_3) in the
						   right circumstances (when d is small) */

						float newTube = c;

						float constantValue = Math.abs(d);

						float traceValue = m[0][0] + m[1][1] + m[2][2];

						int index = y * width + x;

						sliceConstant[index] = constantValue;
						if( constantValue < minConstantResult )
							minConstantResult = constantValue;
						if( constantValue > maxConstantResult )
							maxConstantResult = constantValue;
						
						sliceTrace[index] = traceValue;
						if( traceValue < minTraceResult )
							minTraceResult = traceValue;
						if( traceValue > maxTraceResult )
							maxTraceResult = traceValue;

						sliceNewTube[index] = newTube;
						if( newTube < minNewTubeResult )
							minNewTubeResult = newTube;
						if( newTube > maxNewTubeResult )
							maxNewTubeResult = newTube;
					}
				}
			}
			
			FloatProcessor fpConstant = new FloatProcessor(width, height);
			fpConstant.setPixels(sliceConstant);
			constantStack.addSlice(null, fpConstant);

			FloatProcessor fpTrace = new FloatProcessor(width, height);
			fpTrace.setPixels(sliceTrace);
			traceStack.addSlice(null, fpTrace);

			FloatProcessor fpNewTube = new FloatProcessor(width, height);
			fpNewTube.setPixels(sliceNewTube);
			newTubeStack.addSlice(null, fpNewTube);

			IJ.showProgress(z / (double) depth);
		}

		IJ.showProgress(1.0);
			
		ImagePlus constantResult=new ImagePlus("abs(constant term) " + original.getTitle(), constantStack);
		constantResult.setCalibration(calibration);			
		constantResult.getProcessor().setMinAndMax(minConstantResult,maxConstantResult);
		constantResult.updateAndDraw();
		constantResult.show();

		ImagePlus traceResult=new ImagePlus("trace " + original.getTitle(), traceStack);
		traceResult.setCalibration(calibration);			
		traceResult.getProcessor().setMinAndMax(minTraceResult,maxTraceResult);
		traceResult.updateAndDraw();
		traceResult.show();

		ImagePlus newTubeResult=new ImagePlus("newTube " + original.getTitle(), newTubeStack);
		newTubeResult.setCalibration(calibration);			
		newTubeResult.getProcessor().setMinAndMax(minNewTubeResult,maxNewTubeResult);
		newTubeResult.updateAndDraw();
		newTubeResult.show();
	}
}
