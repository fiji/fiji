import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import java.text.NumberFormat;

/**
 * A version of the Kuwahara filter that uses linear kernels rather than square ones.
 *
 * This plugin is licensed under the GNU Public License v2 or later.
 *
 * @author Christian Tischer
 */
public class Kuwahara_LinearStructure_Filter_v3  implements PlugInFilter {
	
	protected int imW, imH, kW, kH, nAngles, size;
	protected boolean showKernels;
	
	public int setup(String arg, ImagePlus imp) {
		if (imp==null)
			return DONE;
		return DOES_8G | DOES_16 | DOES_32 | SUPPORTS_MASKING;
	}


	public void run(ImageProcessor ipData) {
		
		if (!showDialog())
			return;

		imW = ipData.getWidth();
		imH = ipData.getHeight();		
		kW = size;
		kH = size;
		
		ImageStack imsKernels = createKernel(size, nAngles);
		filter(ipData, imsKernels);
	}
	

	// create the convolution kernel  	
	ImageStack createKernel(int size, int nAngles) {
		
		int x1,y1;		

		int nB = 3; // ...		
		int sizeTemp = size + 2*nB ; // ...
			
		// create an image with a line
		ImagePlus impLine = NewImage.createShortImage("imLine",sizeTemp,sizeTemp,1,NewImage.FILL_BLACK);
		ImageProcessor ipLine = impLine.getProcessor();	
		
		x1 = (sizeTemp -1)/2;
		for (y1=0; y1<sizeTemp ; y1++) {
			ipLine.putPixel(x1, y1, 1); 
		}		

		int iAngle;
		double rotationAngle = 180/nAngles;	
		
		ImagePlus impLineRotated = NewImage.createShortImage("imLineRot",sizeTemp, sizeTemp ,1,NewImage.FILL_BLACK);
		ImageProcessor ipLineRotated = impLineRotated.getProcessor();
		
		// create an empty imStack that will contain pointers to rotLineStack
		ImageStack imsKernel = new ImageStack(size, size);
		
		// this is the place where the data is really stored
		short[][] rotLineStack = new short[nAngles][size*size];
		
		int i;
		for(iAngle=0; iAngle<nAngles; iAngle++) {  
			ipLineRotated.copyBits(ipLine,0,0,Blitter.COPY); // starting from original works better
			ipLineRotated.rotate(iAngle*rotationAngle);
			
			// save values in a 2D array and discard the boundaries
			i = 0;
			for(x1=nB ; x1<sizeTemp-nB ; x1++) {
				for(y1=nB ; y1<sizeTemp-nB ; y1++) {
					rotLineStack[iAngle][i] = (short) ipLineRotated.getPixel(x1, y1);
					i++;
				}
			}
			
			// set pointer in imsKernel to the value stored in rotLineStack 
			imsKernel.addSlice("kernel", (short[]) rotLineStack[iAngle]);
		}
		
		// display kernels (just for checking)
		if (showKernels) {
			ImagePlus impKernel = new ImagePlus("Kernels", imsKernel);
			impKernel.show();
		}
		
		return imsKernel; // bascially a pointer array to 'rotLineStack' 
	}


	public void filter(ImageProcessor ipData, ImageStack imsKernels) {
		
		int[][] im = new int[imW][imH];
		int[][] imSquare= new int[imW][imH];

		int sum, sum2, n, x1, y1, x2, y2, x2min, x2max, y2min, y2max, kernelSum;
		
		ipData.resetMinAndMax();  // this is important because "ip.getMin();" returns the smallest displayed(!!) number.
		int imMin = (int) ipData.getMin();
		NumberFormat nf = NumberFormat.getInstance();
		IJ.showStatus("image min = "+nf.format(imMin));		

		// subtract the minimum and
		// store square and value of image in integer arrays
		for (x1=0; x1<imW; x1++) {
			for (y1=0; y1<imH; y1++) {
				im[x1][y1]=ipData.getPixel(x1, y1)-imMin; // substraction of the minimum (offset) is necessary for the poisson statistics
				imSquare[x1][y1]=im[x1][y1]*im[x1][y1];
			}
		}		
		
		int[][] imSum = new int[imW][imH];
		int[][] imSumOfSquares = new int[imW][imH];
		float[][] value = new float[imW][imH];
		float[][] criterium = new float[imW][imH];
		float[][] result = new float[imW][imH];
		float[][] resultTemp = new float[imW][imH];
		float[][] resultCriterium = new float[imW][imH];
		float[][] resultCriteriumTemp = new float[imW][imH];
		
		setFloatArray(result,0);
		setFloatArray(resultCriterium,Float.MAX_VALUE);

		int nKernels = imsKernels.getSize();
		
		// loop through the different line orientations
		for(int iKernel=0;iKernel<nKernels;iKernel++) {
			short[] pixelsKernel = (short[]) imsKernels.getPixels(iKernel+1);
			convolve2(im, imSquare, imSum, imSumOfSquares, pixelsKernel);
			kernelSum=kernelSum(pixelsKernel);
			calcCiteriumAndValue(imSum, imSumOfSquares, kernelSum, value, criterium); 
			KuwaharaGM(value, criterium, pixelsKernel, resultTemp, resultCriteriumTemp); 
			setResultAndCriterium(result, resultTemp, resultCriterium, resultCriteriumTemp);
		}		

		// put the result into the image
		putFloat2Image(ipData, result, imMin); // add also the minimum back to the image to avoid that the offset shifts between images of a stack
		ipData.resetMinAndMax();  // display the full range.
	}
	

	// convolves 2 images at the same time (for gain of speed)
	// normalises the results the sum of the kernel
	void convolve2(int[][] im1, int[][] im2, int[][] im1Conv, int[][] im2Conv, short[] pixelsKernel) {
	
		int x1min,x1max,y1min,y1max;
		int x2min,x2max,y2min,y2max;
		
		x1min=(kW-1)/2; x1max=imW-(kW-1)/2-1;
		y1min=(kH-1)/2; y1max=imH-(kH-1)/2-1;
		
		int sum1=0,sum2=0,n=0,i=0;
		double number;
		
		for (int x1=x1min; x1<=x1max; x1++) {
			for (int y1=y1min; y1<=y1max; y1++) {
				x2min=x1-(kW-1)/2; x2max=x1+ (kW-1)/2;
				y2min=y1-(kH-1)/2; y2max=y1+ (kH-1)/2;
				sum1=0;sum2=0;n=0;i=0;
				for (int y2=y2min; y2<=y2max; y2++) {	
					for (int x2=x2min; x2<=x2max; x2++) {
						sum1 += im1[x2][y2]*pixelsKernel[i];
						sum2 += im2[x2][y2]*pixelsKernel[i];
						i++;
					} // y2
				} // x2
				im1Conv[x1][y1]=sum1;
				im2Conv[x1][y1]=sum2;
			} // y1
		} // x1
	}
	

	int kernelSum(short[] pixelsKernel) {
		int kernelSum=0;
		for (int i=0; i<kW*kH; i++) {
			kernelSum += pixelsKernel[i];
		}
		return(kernelSum);
	}
	

	
	// Generalised and Modified Kuwahara filter
	// - the criterium value that was used for selection is stored in "resultCriterium"
	// - this allows to compare the result with further, other Kuwahara filters
	
	void KuwaharaGM(float[][] value, float[][] criterium, short[] pixelsKernel, float[][] result, float[][] resultCriterium) {
		int x1min,x1max,y1min,y1max;
		int x2min,x2max,y2min,y2max;
		
		x1min=(kW-1)/2; x1max=imW-(kW-1)/2-1;
		y1min=(kH-1)/2; y1max=imH-(kH-1)/2-1;
		
		int sum1,sum2,n,i;
		
		int x1minPos,y1minPos;		
		float min;

		for (int x1=x1min; x1<=x1max; x1++) {
			for (int y1=y1min; y1<=y1max; y1++) {
				x2min=x1-(kW-1)/2; x2max=x1+(kW-1)/2;
				y2min=y1-(kH-1)/2; y2max=y1+(kH-1)/2;
				i=0;n=0;
				min = Float.MAX_VALUE; x1minPos = x1; y1minPos = y1; 
				for (int y2=y2min; y2<=y2max; y2++) {
					for (int x2=x2min; x2<=x2max; x2++) {
						if( pixelsKernel[i++] > 0 ) {    // searches for minimal criterium along the lines in the kernels (=shifting)
							if( criterium[x2][y2] < min) {
								min=criterium[x2][y2];
								x1minPos=x2; 
								y1minPos=y2;
								n++;
							}
						}
					} // y2
				} // x2
				result[x1][y1]=value[x1minPos][y1minPos];
				resultCriterium[x1][y1]=min;
			} // y1
		} // x1
	}


	void setResultAndCriterium(float[][] result, float[][] resultTemp, float[][] resultCriterium, float[][] resultCriteriumTemp) {
		for (int x1=kW; x1<imW-kW; x1++) {
			for (int y1=kH; y1<imH-kH; y1++) {
				if(resultCriteriumTemp[x1][y1] < resultCriterium[x1][y1]) {
					resultCriterium[x1][y1]=resultCriteriumTemp[x1][y1];
					//result[x1][y1]=100/resultCriteriumTemp[x1][y1]; // show how the criterium looks like
					result[x1][y1]=resultTemp[x1][y1];
				}
			}
		}
	}
	
	void setFloatArray(float[][] array,float val) {
		for (int x1=0; x1<imW; x1++) {
			for (int y1=0; y1<imH; y1++) {
				array[x1][y1] = val;
			}
		}
	}

	void calcCiteriumAndValue(int[][] imSum, int[][] imSumOfSquares, int kernelSum, float[][] value, float[][] criterium) {
		for (int x1=0; x1<imW; x1++) {
			for (int y1=0; y1<imH; y1++) {
				value[x1][y1] = (float) imSum[x1][y1] / kernelSum;
				// DIFFERENT CRITERIA
				// criterium[x1][y1] = (float) ((imSumOfSquares[x1][y1]/kernelSum - value[x1][y1]*value[x1][y1]));
				criterium[x1][y1]    = (float) ((imSumOfSquares[x1][y1]/kernelSum - value[x1][y1]*value[x1][y1]) /  (value[x1][y1]+0.0001));
				// criterium[x1][y1] = (float) ((imSumOfSquares[x1][y1]/kernelSum - value[x1][y1]*value[x1][y1]) /  (value[x1][y1]*value[x1][y1]+0.0001));
			}
		}
	}	
		
	void putFloat2Image(ImageProcessor ip, float[][] imFloat, int imMin) {
		int x2,y2;
		for (int x1=0; x1<imW; x1++) {
			for (int y1=0; y1<imH; y1++) {
				x2=x1; y2=y1;  // duplicate the last meaningful pixels to the boundaries
				if(x1< kW) {x2=kW;} 
				if(x1>= imW-kW) {x2=imW-kW-1;} 
				if(y1< kH) {y2=kH;} 
				if(y1>= imH-kH) {y2=imH-kH-1;} 
				ip.putPixel(x1, y1, (int)(imFloat[x2][y2]+0.5+imMin));
			}
		}
				
	}

	boolean showDialog() {
		GenericDialog gd = new GenericDialog("Kuwahara Filter");
		// the higher the number, the better the results, but the slower
		gd.addNumericField("Number_of_angles", 30, 0);
		// must be ODD!! this is the length of the line along which the averaging takes place
		gd.addNumericField("Line_length", 11, 0);
		gd.addCheckbox("Show_kernels", false);
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		nAngles = (int)gd.getNextNumber();
		size = (int)gd.getNextNumber();
		showKernels = gd.getNextBoolean();
		if ((size % 2) == 0) {
			IJ.error("Line length must be odd!");
			return false;
		}
		return true;
	}

}	

