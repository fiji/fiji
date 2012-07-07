import ij.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;

/** This plugin uses the Helmholtz equation to estimage the wavelength of a harmonic image.
	Bob Dougherty 8/27/2005.  Formerly named Muscle Tone.
	Version 1 Oct. 12, 2011 Fixed a bug that caused bad averaging and possibly index out of bounds
	                        for non-square images
	Version 2 Oct. 15, 2011 Hardwired choices for filtering radii and averaging tolerances.
	                        Added checkboxes to select outputs.
	                        Added support for ROIs.
	                        Added progress bar for averaging.
	                        Improved handling on invalid points.
	                        Weighted average for phi.
 */
 
 /*	License:
	Copyright (c) 2005, OptiNav, Inc.
	All rights reserved.

	Redistribution and use in source and binary forms, with or without
	modification, are permitted provided that the following conditions
	are met:

		Redistributions of source code must retain the above copyright
	notice, this list of conditions and the following disclaimer.
		Redistributions in binary form must reproduce the above copyright
	notice, this list of conditions and the following disclaimer in the
	documentation and/or other materials provided with the distribution.
		Neither the name of OptiNav, Inc. nor the names of its contributors
	may be used to endorse or promote products derived from this software
	without specific prior written permission.

	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
	"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
	LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
	A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
	EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
	PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
	LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


public class Helmholtz_Analysis implements PlugInFilter {

	ImagePlus imp;
	boolean canceled = false;
	static int[] kernelX = new int[]{-1,0,1,-2,0,2,-1,0,1};
	static int[] kernelY = new int[]{1,2,1,0,0,0,-1,-2,-1};
	static int[] kernelL = new int[]{0,1,0,1,-4,1,0,1,0};
	//static int[] kernelL = new int[]{1,1,1,1,-8,1,1,1,1};
	//static float[] kernelL = new float[]{0,0,1,0,0,
	//									 0,1,2,1,0,
	//									 1,2,-16,2,1,
	//									 0,1,2,1,0,
	//									 0,0,1,0,0};
	static float[] average = new float[]{0,0,1,0,0,
										 0,1,2,1,0,
										 1,2,16,2,1,
										 0,1,2,1,0,
										 0,0,1,0,0};
	//static float[] kernelL = new float[]{1,1,1,1,1,
	//									 1,1,1,1,1,
	//									 1,1,-24,1,1,
	//									 1,1,1,1,1,
	//									 1,1,1,1,1};
	double lambdaMin,lambdaMax,preFilter,trendFilter;
	double sigmaMinLambda,sigmaMinPhi;
	int rAv;
	boolean showRawLambda,showAvLambda,showRawPhi,showAvPhi;

	public int setup(String arg, ImagePlus imp) {
		if (IJ.versionLessThan("1.17j"))
			return DONE;
		this.imp = imp;
		return DOES_ALL-DOES_RGB;
	}

	public void run(ImageProcessor ip) {

		int numSlices = imp.getStackSize();
		ImageStack stack = imp.getStack();
		//Get the range of wavelengths;
		if(!getScale())return;
		imp.unlock();

        ImageStack imsLambda = null;
        ImageStack imsPhi = null;
        ImageStack imsLambdaAv = null;
        ImageStack imsPhiAv = null;
        float[] weight = null;
        GaussianBlur gb = new GaussianBlur();

        //Go through the slices of the input stack.
		for (int i = 0; i < numSlices; i++){
			IJ.showStatus("Processing slice "+(i+1)+"/"+(numSlices));
			imp.setSlice(i+1);
			IJ.run("Duplicate...", "title=tempMT");
			IJ.run("32-bit");
			IJ.run("Gaussian Blur...", "radius="+preFilter);
			IJ.run("Bandpass Filter...", "filter_large="
						+lambdaMax+" filter_small="
						+lambdaMin+" suppress=None tolerance=5 autoscale saturate");

			ImagePlus impTemp = WindowManager.getCurrentImage();
			IJ.run("Duplicate...", "title=tempMT2");
			ImagePlus impTemp2 = WindowManager.getCurrentImage();
			IJ.run("Gaussian Blur...", "radius="+trendFilter);
			IJ.run("Image Calculator...", "image1=tempMT operation=Subtract image2=tempMT2 create 32-bit");
			ImagePlus impFiltered = WindowManager.getCurrentImage();
			int w = impFiltered.getWidth();
			int h = impFiltered.getHeight();
			if(i == 0){
				//Create new stacks for the output.
				imsLambda = new ImageStack(w,h);
				imsPhi = new ImageStack(w,h);
				imsLambdaAv = new ImageStack(w,h);
				imsPhiAv = new ImageStack(w,h);
				weight = new float[w*h];
			}
			impTemp.hide();
			impTemp2.hide();
			ImageProcessor ipSlice = impFiltered.getProcessor();
 			float[] gradX = (float[])ipSlice.getPixelsCopy();
  			float[] gradY = (float[])ipSlice.getPixelsCopy();
 			float[] laplace = (float[])ipSlice.getPixelsCopy();
 			float[] data = (float[])ipSlice.getPixels();

 			//Put the pixel arrays into image processors for the convolutions.
 			ImageProcessor ipGradX = new FloatProcessor(w,h);
 			ipGradX.setPixels(gradX);
  			ImageProcessor ipGradY = new FloatProcessor(w,h);
 			ipGradY.setPixels(gradY);
  			ImageProcessor ipLaplace = new FloatProcessor(w,h);
 			ipLaplace.setPixels(laplace);

  			//Evaluate the gradients.
  			ipGradX.convolve3x3(kernelX);
  			ipGradY.convolve3x3(kernelY);
  			ipLaplace.convolve3x3(kernelL);
 			//ipLaplace.convolve(kernelL,5,5);
			//ipSlice.convolve(average,5,5);

			//Make places for the results.
			FloatProcessor ipLambda = new FloatProcessor(w,h);
			FloatProcessor ipPhi = new FloatProcessor(w,h);
			float[] lambda = (float[])ipLambda.getPixels();
			float[] phi = (float[])ipPhi.getPixels();

			FloatProcessor ipLambdaAv = new FloatProcessor(w,h);
			FloatProcessor ipPhiAv = new FloatProcessor(w,h);
			float[] lambdaAv = (float[])ipLambdaAv.getPixels();
			float[] phiAv = (float[])ipPhiAv.getPixels();

			double gx,gy,k2=0,k=0,d,wavelength,phiRad;
			double k2Max = 2*Math.PI/lambdaMin;
			k2Max *= k2Max;
			for (int y = 0; y < h; y++){
				for (int x = 0; x < w; x++){
					int index = x + w*y;
					d = data[index];
					if(d == 0){
						k2 = 0;
					}else{
						//k2 = laplace[index]/(3*d);
						k2 = laplace[index]/d;
						//k2 = laplace[index]/(8*d);
						//k2 = laplace[index]/(25*d);
					}
					if((k2 >= 0)){
						lambda[index] = Float.NaN;
						phi[index] = Float.NaN;
						weight[index] = 0;
					}else{
						k = Math.sqrt(-k2);
						wavelength = (float)(2*Math.PI/k);
						if((wavelength >= lambdaMin)&&(wavelength <= lambdaMax)){
							lambda[index] = (float)wavelength;
							gx = gradX[index];
							gy = gradY[index];
							//phiRad = Math.atan2(gy,gx);
							phiRad = Math.atan2(gx,gy);
							if(phiRad < 0)phiRad += Math.PI;
							phi[index] = (float)(180*phiRad/Math.PI);
							weight[index] = (float)Math.sqrt(gx*gx + gy*gy);
						}else{
							lambda[index] = Float.NaN;
							phi[index] = Float.NaN;
							weight[index] = 0;
						}
					}
				}
			}
			impFiltered.hide();
			//Put the results in the destination stacks.
			ipLambda.setMinAndMax(lambdaMin,lambdaMax);
			ipPhi.setMinAndMax(0,180);
			if(showRawLambda)imsLambda.addSlice("Lambda",ipLambda);
			if(showRawPhi)imsPhi.addSlice("Phi",ipPhi);
			if(showAvLambda){
				average(w,h,rAv,sigmaMinLambda,lambda,lambdaAv);
				ipLambdaAv.setMinAndMax(lambdaMin,lambdaMax);
				imsLambdaAv.addSlice("LambdaAv",ipLambdaAv);
			}
			if(showAvPhi){
				average(w,h,rAv,sigmaMinPhi,phi,weight,phiAv);
				ipPhiAv.setMinAndMax(0,180);
				imsPhiAv.addSlice("PhiAv",ipPhiAv);
			}
		}
		IJ.showStatus("Done");

		// Display the new stacks.
		if(showRawLambda){
			ImagePlus impLambda = new ImagePlus("Lambda",imsLambda);
			impLambda.show();
			IJ.run("Fire");
		}
		if(showRawPhi){
			ImagePlus impPhi = new ImagePlus("Phi",imsPhi);
			impPhi.show();
			IJ.run("Fire");
		}
		if(showAvLambda){
			ImagePlus impLambdaAv = new ImagePlus("Lambda_Av",imsLambdaAv);
			impLambdaAv.show();
			IJ.run("Fire");
		}
		if(showAvPhi){
			ImagePlus impPhiAv = new ImagePlus("Phi_Av",imsPhiAv);
			impPhiAv.show();
			IJ.run("Fire");
		}
	}
	void average(int w, int h, int rAv, double sigmaMin, float[] data, float[] av){
		int ind;
		double varMin = sigmaMin*sigmaMin;
		double mu, var, sum, sumSq;
		float d;

		int nROI, nValid, i,j;
		int wMask = (int)(2*rAv + 1);
		boolean[] mask = new boolean[wMask*wMask];
		int indMask = 0;
		for (int jm = - rAv; jm <= rAv; jm++){
			for (int im = -rAv; im <= rAv; im++){
				mask[indMask++] = Math.sqrt(im*im + jm*jm) <= rAv;
			}
		}
		for (int jCent = 0; jCent < h; jCent++){
			IJ.showProgress((float)jCent/h);
			for (int iCent = 0; iCent < w; iCent++){
				nROI = 0;
				nValid = 0;
				sum = 0;
				sumSq = 0;
				indMask = 0;
				for (int jm = - rAv; jm <= rAv; jm++){
					for (int im = -rAv; im <= rAv; im++){
						if(mask[indMask++]){
							j = jCent + jm;
							i = iCent + im;
							if((j >= 0)&&(j < h)&&(i >= 0)&&(i < w)){
								ind = i + j*w;
								nROI++;
								d = data[ind];
								if(d == d){
									nValid++;
									sum += d;
									sumSq += d*d;
								}
							}//if in image
						}//if in circular mask
					}//im
				}//jm
				ind = iCent + jCent*w;
				av[ind] = Float.NaN;
				//Possibly the factor 2/3 below should be an input variable.
				if(nValid >= 2*nROI/3){
					sum /= nValid;
					sumSq /= nValid;
					var = sumSq - sum*sum;
					if(var <= varMin){
						av[ind] = (float)sum;
					}
				}
			}//iCent
		}//jCent
		IJ.showProgress((float)1);
	}
	void average(int w, int h, int rAv, double sigmaMin, float[] data, float[] weight, float[] av){
		int ind;
		double varMin = sigmaMin*sigmaMin;
		double mu, var, sum, wSum, sumSq;
		float d;
		int nMax = 2*(rAv+1)*2*(rAv+1);
		float[] x = new float[nMax];
		float[] wt = new float[nMax];

		int nROI, nValid, i,j;
		int wMask = (int)(2*rAv + 1);
		boolean[] mask = new boolean[wMask*wMask];
		int indMask = 0;
		for (int jm = - rAv; jm <= rAv; jm++){
			for (int im = -rAv; im <= rAv; im++){
				mask[indMask++] = Math.sqrt(im*im + jm*jm) <= rAv;
			}
		}
		for (int jCent = 0; jCent < h; jCent++){
			IJ.showProgress((float)jCent/h);
			for (int iCent = 0; iCent < w; iCent++){
				nROI = 0;
				nValid = 0;
				indMask = 0;
				for (int jm = - rAv; jm <= rAv; jm++){
					for (int im = -rAv; im <= rAv; im++){
						if(mask[indMask++]){
							j = jCent + jm;
							i = iCent + im;
							if((j >= 0)&&(j < h)&&(i >= 0)&&(i < w)){
								ind = i + j*w;
								nROI++;
								d = data[ind];
								if((d == d)&&(weight[ind]>0)){
									x[nValid] = d;
									wt[nValid++] = weight[ind];
								}
							}//if in image
						}//if in circular mask
					}//im
				}//jm
				ind = iCent + jCent*w;
				av[ind] = Float.NaN;
				//Possibly the factor 2/3 below should be an input variable.
				if(nValid >= 2*nROI/3){
					sum = 0;
					wSum = 0;
					for(int is = 0; is < nValid; is++){
						sum += wt[is]*x[is];
						wSum += wt[is];
					}
					mu = sum/wSum;
					sumSq = 0;
					for(int is = 0; is < nValid; is++){
						sumSq += wt[is]*(x[is]-mu)*(x[is]-mu);
					}
					var = sumSq/wSum;
					if(var <= varMin){
						av[ind] = (float)mu;
					}
				}
			}//iCent
		}//jCent
		IJ.showProgress((float)1);
	}
	boolean getScale() {
		//preFilter = Prefs.get("helmholtzanalysis.prefilter", 2);
		//trendFilter = Prefs.get("helmholtzanalysis.trendfilter", 12);
		lambdaMin = Prefs.get("helmholtzanalysis.lambdamin", 2);
		lambdaMax = Prefs.get("helmholtzanalysis.lambdamax", 12);
		rAv = (int)Prefs.get("helmholtzanalysis.rav", 10);
		//sigmaMinLambda = Prefs.get("helmholtzanalysis.sigmaminlambda", 2);
		//sigmaMinPhi = Prefs.get("helmholtzanalysis.sigmaminphi", 20);
		showRawLambda = Prefs.get("helmholtzanalysis.showRawLambda", true);
		showAvLambda = Prefs.get("helmholtzanalysis.showAvLambda", true);
		showRawPhi = Prefs.get("helmholtzanalysis.showRawPhi", true);
		showAvPhi = Prefs.get("helmholtzanalysis.showAvPhi", true);
		String[] cbls = new String[]{"Raw lambda","Averaged lambda","Raw phi","Averaged phi"};
		boolean[] cbdef = new boolean[]{showRawLambda,showAvLambda,showRawPhi,showAvPhi};
		GenericDialog gd = new GenericDialog("Helmholtz Analysis...", IJ.getInstance());
		//gd.addNumericField("preFilter, low pass, pixels (ignore features smaller than this)", preFilter, 1);
		//gd.addNumericField("trendFilter,  pixels (remove trends larger than this)", trendFilter, 1);
		gd.addNumericField("minimum_lambda, pixels", lambdaMin, 1);
		gd.addNumericField("maximum_lambda, pixels", lambdaMax, 1);
		gd.addNumericField("rAv, pixels: radius of postprocessing averaging circle (perhaps maximum lambda)", rAv, 0);
		//gd.addNumericField("lambda_tolerance, pixels: acceptable variability in average computation of lambda", sigmaMinLambda, 1);
		//gd.addNumericField("phi_tolerance, deg.: acceptable variability in average computation of phi", sigmaMinPhi, 1);
		gd.addMessage("Show:");
		gd.addCheckboxGroup(1,4,cbls,cbdef);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}
		//preFilter = gd.getNextNumber();
		//trendFilter = gd.getNextNumber();
		lambdaMin = gd.getNextNumber();
		lambdaMax = gd.getNextNumber();
		trendFilter = lambdaMax;
		preFilter = lambdaMin/2;
		rAv = (int)gd.getNextNumber();
		//sigmaMinLambda = gd.getNextNumber();
		sigmaMinLambda = lambdaMin;
		//sigmaMinPhi = gd.getNextNumber();
		sigmaMinPhi = 15;//Somewhat arbitrary choice of 15 degrees.
		showRawLambda = gd.getNextBoolean();
		showAvLambda = gd.getNextBoolean();
		showRawPhi = gd.getNextBoolean();
		showAvPhi = gd.getNextBoolean();
		//Prefs.set("helmholtzanalysis.prefilter", preFilter);
		//Prefs.set("helmholtzanalysis.trendfilter", trendFilter);
		Prefs.set("helmholtzanalysis.lambdamin", lambdaMin);
		Prefs.set("helmholtzanalysis.lambdamax", lambdaMax);
		Prefs.set("helmholtzanalysis.rav", rAv);
		//Prefs.set("helmholtzanalysis.sigmaminlambda", sigmaMinLambda);
		//Prefs.set("helmholtzanalysis.sigmaminphi", sigmaMinPhi);
		Prefs.set("helmholtzanalysis.showRawLambda", showRawLambda);
		Prefs.set("helmholtzanalysis.showAvLambda", showAvLambda);
		Prefs.set("helmholtzanalysis.showRawPhi", showRawPhi);
		Prefs.set("helmholtzanalysis.showAvPhi", showAvPhi);
		return true;
	}
}


