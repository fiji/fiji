package trainableSegmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.PlugInFilter;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


/**
 * A version of the Kuwahara filter that uses linear kernels rather than square ones.
 *
 * This plugin is licensed under the GNU Public License v2 or later.
 *
 * @author Christian Tischer
 */
public class Kuwahara  implements PlugInFilter 
{

	protected int imW, imH, kW, kH, nAngles=30, size=11;
	protected boolean showKernels=false;
	
	public static final int VARIANCE = 0;
	public static final int VARIANCE_DIV_MEAN = 1;
	public static final int VARIANCE_DIV_MEAN_SQUARE = 2;
	
	int criterionMethod = VARIANCE_DIV_MEAN;	

	public int setup(String arg, ImagePlus imp) {
		if (imp==null)
			return DONE;
		return DOES_8G | DOES_16 | DOES_32 | SUPPORTS_MASKING;
	}


	/**
	 * Run method for plugin filter
	 */
	public void run(ImageProcessor ipData) 
	{

		if (!showDialog())
			return;

		imW = ipData.getWidth();
		imH = ipData.getHeight();
		kW = size;
		kH = size;

		ImageStack imsKernels = createKernel(size, nAngles);
		filter(ipData, imsKernels);
	}

	/**
	 * Set the criterion method
	 * 
	 * @param criterion
	 */
	public void setCriterionMethod(int criterion)
	{
		if(criterion >= VARIANCE && criterion <= VARIANCE_DIV_MEAN_SQUARE)
			this.criterionMethod = criterion;
	}
	
	public void setNumberOfAngles(int nAngles)
	{
		this.nAngles = nAngles;
	}
	
	public void setSize(int size)
	{
		if(size % 2 != 0)
			this.size = size;
	}
	
	/**
	 * create the convolution kernel
	 */
	public ImageStack createKernel(int size, int nAngles) 
	{

		int x1,y1;

		int nB = 3; // ...
		int sizeTemp = size + 2*nB ; // ...

		// create an image with a line
		ImagePlus impLine = NewImage.createShortImage("imLine",sizeTemp,sizeTemp,1,NewImage.FILL_BLACK);
		ImageProcessor ipLine = impLine.getProcessor();

		x1 = (sizeTemp -1)/2;
		for (y1=0; y1<sizeTemp ; y1++) 
		{
			ipLine.putPixel(x1, y1, 1);
		}

		int iAngle;
		double rotationAngle = 180/nAngles;

		ImagePlus impLineRotated = NewImage.createShortImage("imLineRot",sizeTemp, sizeTemp ,1,NewImage.FILL_BLACK);
		ImageProcessor ipLineRotated = impLineRotated.getProcessor();

		// create an empty imStack that will contain pointers to rotLineStack
		ImageStack imsKernel = new ImageStack(size, size);

		// this is the place where the data is really stored
		float[][] rotLineStack = new float[nAngles][size*size];

		int i;
		for(iAngle=0; iAngle<nAngles; iAngle++) 
		{
			ipLineRotated.copyBits(ipLine,0,0,Blitter.COPY); // starting from original works better
			ipLineRotated.rotate(iAngle*rotationAngle);

			// save values in a 2D array and discard the boundaries
			i = 0;
			for(x1=nB ; x1<sizeTemp-nB ; x1++) 
			{
				for(y1=nB ; y1<sizeTemp-nB ; y1++) 
				{
					rotLineStack[iAngle][i] = (float) ipLineRotated.getPixel(x1, y1);
					i++;
				}
			}

			// set pointer in imsKernel to the value stored in rotLineStack
			imsKernel.addSlice("kernel", (float[]) rotLineStack[iAngle]);
		}

		// display kernels (just for checking)
		if (showKernels) 
		{
			ImagePlus impKernel = new ImagePlus("Kernels", imsKernel);
			impKernel.show();
		}

		return imsKernel; // basically a pointer array to 'rotLineStack'
	}


	/**
	 * Apply Kuwahara filter to input image
	 * 
	 * @param ipData
	 * @param imsKernels
	 */
	public void filter(ImageProcessor ipData, ImageStack imsKernels) 
	{

		float[][] im = new float[imW][imH];
		float[][] imSquare= new float[imW][imH];

		int x1, y1;
		float kernelSum = 0;

		ipData.resetMinAndMax();  // this is important because "ip.getMin();" returns the smallest displayed(!!) number.
		float imMin =  (float) ipData.getMin();

		// subtract the minimum and
		// store square and value of image in integer arrays
		for (x1=0; x1<imW; x1++) 
		{
			for (y1=0; y1<imH; y1++) 
			{
				im[x1][y1]=ipData.getf(x1, y1)-imMin; // substraction of the minimum (offset) is necessary for the poisson statistics
				imSquare[x1][y1]=im[x1][y1]*im[x1][y1];
			}
		}

		float[][] imSum = new float[imW][imH];
		float[][] imSumOfSquares = new float[imW][imH];
		float[][] value = new float[imW][imH];
		float[][] criterion = new float[imW][imH];
		float[][] result = new float[imW][imH];
		float[][] resultTemp = new float[imW][imH];
		float[][] resultCriterion = new float[imW][imH];
		float[][] resultCriterionTemp = new float[imW][imH];

		setFloatArray(result, 0);
		setFloatArray(resultCriterion, Float.MAX_VALUE);

		int nKernels = imsKernels.getSize();
/*		
		ImageStack is = new ImageStack(imW, imH);
		ImageStack isSquare = new ImageStack(imW, imH);
		ImageStack isValue = new ImageStack(imW, imH);
		ImageStack isCriterion = new ImageStack(imW, imH);
		ImageStack isResult = new ImageStack(imW, imH);
		ImageStack isResultCriterionTemp = new ImageStack(imW, imH);
	*/	
		// loop through the different line orientations
		for(int iKernel = 0; iKernel<nKernels; iKernel++) 
		{
			float[] pixelsKernel = (float[]) imsKernels.getProcessor(iKernel+1).getPixels();
			
			//final ImageProcessor ip = new FloatProcessor(im);
			//(new ImagePlus("im", ip)).show();
			//final ImageProcessor ipSquare = new FloatProcessor(imSquare);
			//(new ImagePlus("imSquare", ipSquare)).show();
			
			convolve2(im, imSquare, imSum, imSumOfSquares, pixelsKernel);
		
			//convolve(ip, ipSquare, pixelsKernel);
			
	//		is.addSlice("convolution "+iKernel, new FloatProcessor(imSum));
	//		isSquare.addSlice("square convolution "+iKernel, new FloatProcessor(imSumOfSquares));
						
			//(new ImagePlus("imSquare", ipSquare)).show();
			//(new ImagePlus("im", ip)).show();
			
									
			//imSum = ((FloatProcessor) ip).getFloatArray();
			//imSumOfSquares = ((FloatProcessor) ipSquare).getFloatArray();
			
			kernelSum=kernelSum(pixelsKernel);
			if (criterionMethod == 0)
				calculateCriterionVariance(imSum, imSumOfSquares, kernelSum, value, criterion);
			else if (criterionMethod == 1)
				calculateCriterionVarianceDivMean(imSum, imSumOfSquares, kernelSum, value, criterion);
			else if (criterionMethod == 2)
				calculateCriterionVarianceDivMean2(imSum, imSumOfSquares, kernelSum, value, criterion);						
			
	//		isCriterion.addSlice("criterion "+iKernel, new FloatProcessor(criterion));
	//		isValue.addSlice("value "+iKernel, new FloatProcessor(value));
			
			KuwaharaGM(value, criterion, pixelsKernel, resultTemp, resultCriterionTemp);
			
	//		isResult.addSlice("result temp " + iKernel, new FloatProcessor(resultTemp));
	//		isResultCriterionTemp.addSlice("result criterion temp " + iKernel, new FloatProcessor(resultCriterionTemp));
			
			setResultAndCriterion(result, resultTemp, resultCriterion, resultCriterionTemp);
			
			IJ.showProgress(iKernel + 1, nKernels);
		}
		
		/*
try{		
		(new ImagePlus ("convolutions", is)).show();
		(new ImagePlus ("square convolutions", isSquare)).show();
		(new ImagePlus ("criterion", isCriterion)).show();
		(new ImagePlus ("value", isValue)).show();
		(new ImagePlus ("result criterion temp", isResultCriterionTemp)).show();
		(new ImagePlus ("result temp", isResult)).show();
}catch(Exception ex)
{
	ex.printStackTrace();
}*/
		
		// put the result into the image
		putFloat2Image(ipData, result, imMin); // add also the minimum back to the image to avoid that the offset shifts between images of a stack
		ipData.resetMinAndMax();  // display the full range.
	}


	// convolves 2 images at the same time (for gain of speed)
	// normalises the results the sum of the kernel
	void convolve2(float[][] im1, float[][] im2, float[][] im1Conv, float[][] im2Conv, float[] pixelsKernel) 
	{

		int x1min,x1max,y1min,y1max;
		int x2min,x2max,y2min,y2max;

		x1min=0; //(kW-1)/2; 
		x1max=imW-1;// -(kW-1)/2-1;
		y1min=0; //(kH-1)/2; 
		y1max=imH-1; //-(kH-1)/2-1;

		float sum1=0,sum2=0;
		int i=0;
	
		for (int x1=x1min; x1<=x1max; x1++) 
		{
			for (int y1=y1min; y1<=y1max; y1++) 
			{
				x2min=x1-(kW-1)/2; x2max=x1+ (kW-1)/2;
				y2min=y1-(kH-1)/2; y2max=y1+ (kH-1)/2;
				sum1=0;
				sum2=0;
				i=0;
				for (int y2=y2min; y2<=y2max; y2++) 
				{
					for (int x2=x2min; x2<=x2max; x2++) 
					{
						sum1 += getPixel(x2, y2, im1, imW, imH)* pixelsKernel[i]; //im1[x2][y2]*pixelsKernel[i];
						sum2 += getPixel(x2, y2, im2, imW, imH)* pixelsKernel[i]; //im2[x2][y2]*pixelsKernel[i];
						i++;
					} // y2
				} // x2
				im1Conv[x1][y1]=sum1;
				im2Conv[x1][y1]=sum2;
			} // y1
		} // x1
	}

	private float getPixel(int x, int y, float[][] pixels, int width, int height) 
	{
		if (x<=0) x = 0;
		if (x>=width) x = width-1;
		if (y<=0) y = 0;
		if (y>=height) y = height-1;
		return pixels[x][y];
	}
	
	// convolves 2 images at the same time (for gain of speed)
	// normalises the results the sum of the kernel
	void convolve(ImageProcessor ip1, ImageProcessor ip2, float[] pixelsKernel) 
	{
		final Convolver c = new Convolver();
		
		//ImagePlus imp = new ImagePlus("ip", ip1);
		//imp.show();
		//(new ImagePlus("pixelKernel", new FloatProcessor(size, size, pixelsKernel, null))).show();
		
		boolean b = c.convolveFloat(ip1, pixelsKernel, size, size);
		
		if(b == false)
		{
			IJ.error("Error while convolving first image with kernel!");
			return;
		}
		
		//(new ImagePlus("convoluted ip1", ip1)).show();
		
		final Convolver c2 = new Convolver();		
		b = c2.convolveFloat(ip2, pixelsKernel, size, size);
		if(b == false)
		{
			IJ.error("Error while convolving second image with kernel!");
			return;
		}
	}	
	

	float kernelSum(float[] pixelsKernel) {
		float kernelSum=0;
		for (int i=0; i<kW*kH; i++) {
			kernelSum += pixelsKernel[i];
		}
		return(kernelSum);
	}



	// Generalised and Modified Kuwahara filter
	// - the criterion value that was used for selection is stored in "resultCriterion"
	// - this allows to compare the result with further, other Kuwahara filters

	void KuwaharaGM(
			float[][] value, 
			float[][] criterion, 
			float[] pixelsKernel, 
			float[][] result, 
			float[][] resultCriterion) 
	{
		int x1min,x1max,y1min,y1max;
		int x2min,x2max,y2min,y2max;

		x1min=0;//(kW-1)/2; 
		x1max=imW-1; //-(kW-1)/2-1;
		y1min=0; //(kH-1)/2; 
		y1max=imH-1; //-(kH-1)/2-1;

		int sum1,sum2,n,i;

		int x1minPos,y1minPos;
		float min;

		for (int x1=x1min; x1<=x1max; x1++) 
		{
			for (int y1=y1min; y1<=y1max; y1++) 
			{
				x2min=x1-(kW-1)/2; 
				x2max=x1+(kW-1)/2;
				y2min=y1-(kH-1)/2; 
				y2max=y1+(kH-1)/2;
				i=0;
				n=0;
				min = Float.MAX_VALUE; 
				x1minPos = x1; 
				y1minPos = y1;
				for (int y2=y2min; y2<=y2max; y2++) 
				{
					for (int x2=x2min; x2<=x2max; x2++) 
					{
						if( pixelsKernel[i++] > 0 ) 
						{    // searches for minimal criterion along the lines in the kernels (=shifting)
							final float criterionPixel = getPixel(x2, y2, criterion, imW, imH); 
							if( criterionPixel < min) 
							{
								min=criterionPixel;
								x1minPos=x2;
								y1minPos=y2;
								n++;
							}
						}
					} // y2
				} // x2
				result[x1][y1] = getPixel(x1minPos, y1minPos, value, imW, imH);
				resultCriterion[x1][y1] = min;
			} // y1
		} // x1
	}


	void setResultAndCriterion(float[][] result, float[][] resultTemp, float[][] resultCriterion, float[][] resultCriterionTemp) 
	{
		for (int x1=0; x1<imW; x1++) 
		{
			for (int y1=0; y1<imH; y1++) 
			{
				if(resultCriterionTemp[x1][y1] < resultCriterion[x1][y1]) 
				{
					resultCriterion[x1][y1]=resultCriterionTemp[x1][y1];
					//result[x1][y1]=100/resultCriterionTemp[x1][y1]; // show how the criterion looks like
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

	public final void calculateCriterionVariance(float[][] imSum, float[][] imSumOfSquares, float kernelSum, float[][] value, float[][] criterion) 
	{
		//IJ.log("calculateCriterionVariance");
		for (int x1=0; x1<imW; x1++) {
			for (int y1=0; y1<imH; y1++) {
				value[x1][y1] = imSum[x1][y1] / kernelSum;
				criterion[x1][y1] = imSumOfSquares[x1][y1] / kernelSum - value[x1][y1] * value[x1][y1];
			}
		}
	}

	public final void calculateCriterionVarianceDivMean(float[][] imSum, float[][] imSumOfSquares, float kernelSum, float[][] value, float[][] criterion) 
	{
	//	IJ.log("calculateCriterionVarianceDivMean");
		for (int x1=0; x1<imW; x1++) {
			for (int y1=0; y1<imH; y1++) {
				value[x1][y1] = imSum[x1][y1] / kernelSum;
				criterion[x1][y1] = (imSumOfSquares[x1][y1] / kernelSum - value[x1][y1] * value[x1][y1]) / (value[x1][y1] + Float.MIN_VALUE);
			}
		}
	}

	public final void calculateCriterionVarianceDivMean2(float[][] imSum, float[][] imSumOfSquares, float kernelSum, float[][] value, float[][] criterion) 
	{
		//IJ.log("calculateCriterionVarianceDivMean2");
		for (int x1=0; x1<imW; x1++) {
			for (int y1=0; y1<imH; y1++) {
				value[x1][y1] = imSum[x1][y1] / kernelSum;
				criterion[x1][y1] = (imSumOfSquares[x1][y1] / kernelSum - value[x1][y1] * value[x1][y1]) / (value[x1][y1] * value[x1][y1] + Float.MIN_VALUE);
			}
		}
	}

	void putFloat2Image(ImageProcessor ip, float[][] imFloat, float imMin) 
	{		
		for (int x1=0; x1<imW; x1++) 
		{
			for (int y1=0; y1<imH; y1++) 
			{
				final int x2=x1, y2=y1;  
				ip.setf(x1, y1, imFloat[x2][y2]+0.5f+imMin);
			}
		}

	}

	boolean showDialog() 
	{
		String[] criteria = {
			"Variance",
			"Variance / Mean",
			"Variance / Mean^2"
		};
		GenericDialog gd = new GenericDialog("Kuwahara Filter");
		// the higher the number, the better the results, but the slower
		gd.addNumericField("Number_of_angles", 30, 0);
		// must be ODD!! this is the length of the line along which the averaging takes place
		gd.addNumericField("Line_length", 11, 0);
		gd.addChoice("Criterion", criteria, criteria[0]);
		gd.addCheckbox("Show_kernels", false);
		gd.showDialog();
		if (gd.wasCanceled()) 
			return false;
		nAngles = (int)gd.getNextNumber();
		size = (int)gd.getNextNumber();
		criterionMethod = gd.getNextChoiceIndex();
		showKernels = gd.getNextBoolean();
		if ((size % 2) == 0) 
		{
			IJ.error("Line length must be odd!");
			return false;
		}
		return true;
	}

	/**
	 * Apply filter to input image (in place)
	 * @param inputImage input image
	 * @param size kernel size (it must be odd)
	 * @param nAngles number of angles
	 * @return false if error
	 */
	public boolean applyFilter(
			final ImageProcessor inputImage,
			final int size,
			final int nAngles,
			final int criterion)
	{
		if(null == inputImage || size % 2 == 0 )
			return false;
		
		this.imW = inputImage.getWidth();
		this.imH = inputImage.getHeight();
		this.size = kW = size;
		this.kH = size;
		this.nAngles = nAngles;
		this.criterionMethod = criterion;
		
		final ImageStack imsKernels = createKernel(size, nAngles);
		filter(inputImage, imsKernels);
		return true;
	}
	
	
	
}

