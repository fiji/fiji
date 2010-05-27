package trainableSegmentation;
/** 
 * This class is intended for the Trainable_Segmentation plugin. It creates and holds
 * different feature images for the classification. Possible filters include:
 * - Gaussianblur
 * - Gradientmagnitude
 * - Hessian
 * - Difference of Gaussian
 * - Orientation filter to detect membranes and then its projection
 * - Mean
 * - Variance
 * - Minimum
 * - Maximum
 * 
 * filters to come:
 * - make use of color channels
 * - membraneFilters faster
 * - histogram patch
 * - BEL type edge detector
 * 
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors: Verena Kaynig (verena.kaynig@inf.ethz.ch), Ignacio Arganda-Carreras (iarganda@mit.edu)
 *          Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import stitching.FloatArray2D;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import ij.IJ;
import ij.ImageStack;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.plugin.ZProjector;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.RankFilters;



public class FeatureStack 
{
	private ImagePlus originalImage = null;
	private ImageStack wholeStack = null;
	private int width = 0;
	private int height = 0;
	
	private static final int MAX_SIGMA = 16;
	
	public static final int GAUSSIAN 	= 0;
	public static final int SOBEL 		= 1;
	public static final int HESSIAN 	= 2; 
	public static final int DOG			= 3;
	public static final int MEMBRANE	= 4;
	public static final int VARIANCE	= 5;
	public static final int MEAN		= 6;
	public static final int MINIMUM		= 7;
	public static final int MAXIMUM		= 8;
	public static final int MEDIAN		= 9;
	
	public static final String[] availableFeatures 
		= new String[]{	"Gaussian_blur", "Sobel_filter", "Hessian", "Difference_of_gaussians", 
					   	"Membrane_projections","Variance","Mean", "Minimum", "Maximum", "Median"};
	
	private boolean[] enableFeatures = new boolean[]{true, true, true, true, true, false, false, false, false, false};
	
	private boolean normalize = false;
	
	/**
	 * Construct object to store stack of image features
	 * @param image original image
	 */
	public FeatureStack(ImagePlus image)
	{
		originalImage = new ImagePlus("original image", image.getProcessor().convertToFloat());
		width = image.getWidth();
		height = image.getHeight();
		wholeStack = new ImageStack(width, height);
		wholeStack.addSlice("original", originalImage.getProcessor().duplicate());
	}

	public void show(){
		ImagePlus showStack = new ImagePlus("featureStack", wholeStack);
		showStack.show();
	}
	
	public int getSize(){
		return wholeStack.getSize();
	}
	
	public String getSliceLabel(int index){
		return wholeStack.getSliceLabel(index);
	}
	
	public int getHeight(){
		return wholeStack.getHeight();
	}
	
	public int getWidth(){
		return wholeStack.getWidth();
	}
	
	public void addGaussianBlur(float sigma){
		ImageProcessor ip = originalImage.getProcessor().duplicate();
		GaussianBlur gs = new GaussianBlur();
		gs.blur(ip, sigma);
		wholeStack.addSlice(availableFeatures[GAUSSIAN] + "_" + sigma, ip);
	}
	
	public void addVariance(float radius)
	{
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final RankFilters filter = new RankFilters();
		filter.rank(ip, radius, RankFilters.VARIANCE);
		wholeStack.addSlice(availableFeatures[VARIANCE]+ "_"  + radius, ip);
	}
	
	public void addMean(float radius)
	{
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final RankFilters filter = new RankFilters();
		filter.rank(ip, radius, RankFilters.MEAN);
		wholeStack.addSlice(availableFeatures[MEAN]+ "_"  + radius, ip);
	}
	
	public void addMin(float radius)
	{
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final RankFilters filter = new RankFilters();
		filter.rank(ip, radius, RankFilters.MIN);
		wholeStack.addSlice(availableFeatures[MINIMUM]+ "_"  + radius, ip);
	}
	
	public void addMax(float radius)
	{
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final RankFilters filter = new RankFilters();
		filter.rank(ip, radius, RankFilters.MAX);
		wholeStack.addSlice(availableFeatures[MAXIMUM]+ "_"  + radius, ip);
	}
	
	public void addMedian(float radius)
	{
		final ImageProcessor ip = originalImage.getProcessor().duplicate();
		final RankFilters filter = new RankFilters();
		filter.rank(ip, radius, RankFilters.MEDIAN);
		wholeStack.addSlice(availableFeatures[MEDIAN]+ "_"  + radius, ip);
	}
	
	public void writeConfigurationToFile(String filename)
	{
		try{
			BufferedWriter out = new BufferedWriter(
					new OutputStreamWriter(
							new FileOutputStream(filename) ) );
			try{	
				for (int i=1; i <= wholeStack.getSize(); i++)
				{
					out.write(wholeStack.getSliceLabel(i));
					out.newLine();
				}
				out.close();
			}
			catch(IOException e){System.out.println("IOException");}
		}
		catch(FileNotFoundException e){System.out.println("File not found!");}
	}
	
	public void addGradient(float sigma){
		GaussianBlur gs = new GaussianBlur();
		ImageProcessor ip_x = originalImage.getProcessor().duplicate();
		gs.blur(ip_x, sigma);
		Convolver c = new Convolver();
		float[] sobelFilter_x = {1f,2f,1f,0f,0f,0f,-1f,-2f,-1f};
		c.convolveFloat(ip_x, sobelFilter_x, 3, 3);
		
		ImageProcessor ip_y = originalImage.getProcessor().duplicate();
		gs.blur(ip_y, sigma);
		c = new Convolver();
		float[] sobelFilter_y = {1f,0f,-1f,2f,0f,-2f,1f,0f,-1f};
		c.convolveFloat(ip_y, sobelFilter_y, 3, 3);
		
		ImageProcessor ip = new FloatProcessor(width, height);
		
		for (int x=0; x<width; x++){
			for (int y=0; y<height; y++){
				float s_x = ip_x.getf(x,y);
				float s_y = ip_y.getf(x,y);
				ip.setf(x,y, (float) Math.sqrt(s_x*s_x + s_y*s_y));
			}
		}
		
		//ip.add(-ip.getMin());
		wholeStack.addSlice(availableFeatures[SOBEL]+ "_"  +sigma, ip);
	}
	
	public void addHessian(float sigma)
	{
		float[] sobelFilter_x = {1f,2f,1f,0f,0f,0f,-1f,-2f,-1f};
		float[] sobelFilter_y = {1f,0f,-1f,2f,0f,-2f,1f,0f,-1f};
		Convolver c = new Convolver();				
		GaussianBlur gs = new GaussianBlur();
		
		ImageProcessor ip_x = originalImage.getProcessor().duplicate();
		gs.blur(ip_x, sigma);		
		c.convolveFloat(ip_x, sobelFilter_x, 3, 3);		
		
		ImageProcessor ip_y = originalImage.getProcessor().duplicate();
		gs.blur(ip_y, sigma);
		c = new Convolver();
		c.convolveFloat(ip_y, sobelFilter_y, 3, 3);
		
		ImageProcessor ip_xx = ip_x.duplicate();
		//gs.blur(ip_xx, sigma);		
		c.convolveFloat(ip_xx, sobelFilter_x, 3, 3);		
		
		ImageProcessor ip_xy = ip_x.duplicate();
		//gs.blur(ip_xy, sigma);		
		c.convolveFloat(ip_xy, sobelFilter_y, 3, 3);		
		
		ImageProcessor ip_yy = ip_y.duplicate();
		//gs.blur(ip_yy, sigma);		
		c.convolveFloat(ip_yy, sobelFilter_y, 3, 3);		
		
		ImageProcessor ip = new FloatProcessor(width, height);
		ImageProcessor ipTr = new FloatProcessor(width, height);
		ImageProcessor ipDet = new FloatProcessor(width, height);
				
		for (int x=0; x<width; x++){
			for (int y=0; y<height; y++){
				float s_xx = ip_xx.getf(x,y);
				float s_xy = ip_xy.getf(x,y);
				float s_yy = ip_yy.getf(x,y);
				ip.setf(x,y, (float) Math.sqrt(s_xx*s_xx + s_xy*s_xy+ s_yy*s_yy));
				ipTr.setf(x,y, (float) s_xx + s_yy);
				ipDet.setf(x,y, (float) s_xx*s_yy-s_xy*s_xy);
			}
		}
		
		wholeStack.addSlice(availableFeatures[HESSIAN] + "_"  + sigma, ip);
		wholeStack.addSlice(availableFeatures[HESSIAN]+ "_Trace_"+sigma, ipTr);
		wholeStack.addSlice(availableFeatures[HESSIAN]+ "_Determinant_"+sigma, ipDet);
	}
	
	public void addDoG(float sigma1, float sigma2)
	{
		GaussianBlur gs = new GaussianBlur();
		ImageProcessor ip_1 = originalImage.getProcessor().duplicate();
		gs.blur(ip_1, sigma1);
		ImageProcessor ip_2 = originalImage.getProcessor().duplicate();
		gs.blur(ip_2, sigma2);
		
		ImageProcessor ip = new FloatProcessor(width, height);
		
		for (int x=0; x<width; x++){
			for (int y=0; y<height; y++){
				float v1 = ip_1.getf(x,y);
				float v2 = ip_2.getf(x,y);
				ip.setf(x,y, v2-v1);
			}
		}
		
		wholeStack.addSlice(availableFeatures[DOG]+ "_"+sigma1+"_"+sigma2, ip);
	}
	
	public void addMembraneFeatures(int patchSize, int membraneSize){
		//create membrane patch
		ImageProcessor membranePatch = new FloatProcessor(patchSize, patchSize);
		int middle = Math.round(patchSize / 2);
		int startX = middle - (int) Math.floor(membraneSize/2.0);
		int endX = middle + (int) Math.ceil(membraneSize/2.0);
		
		for (int x=startX; x<=endX; x++){
			for (int y=0; y<patchSize; y++){
				membranePatch.setf(x, y, 1f);
			}
		}

		
		ImageStack is = new ImageStack(width, height);
		ImageProcessor rotatedPatch;
		
		// Rotate kernel 15 degrees up to 180
		for (int i=0; i<12; i++)
		{
			rotatedPatch = membranePatch.duplicate();
			rotatedPatch.invert();
			rotatedPatch.rotate(15*i);
			rotatedPatch.invert();
			Convolver c = new Convolver();				
	
			float[] kernel = (float[]) rotatedPatch.getPixels();
			ImageProcessor ip = originalImage.getProcessor().duplicate();		
			c.convolveFloat(ip, kernel, patchSize, patchSize);		

			is.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip);
		//	wholeStack.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip.convertToByte(true));
		}
		
		ImagePlus projectStack = new ImagePlus("membraneStack",is);
		//projectStack.show();
		
		ZProjector zp = new ZProjector(projectStack);
		zp.setStopSlice(is.getSize());
		for (int i=0;i<6; i++){
			zp.setMethod(i);
			zp.doProjection();
			wholeStack.addSlice(availableFeatures[MEMBRANE] + "_" +i+"_"+patchSize+"_"+membraneSize, zp.getProjection().getChannelProcessor());
		}
	}
	
	
	public void addTest()
	{
		FloatArray2D fftImage = new FloatArray2D((float[]) originalImage.getProcessor().getPixels(),
													originalImage.getWidth(), originalImage.getHeight());
		//int fftSize = FftReal.nfftFast(Math.max(width, height));
		
		//FloatArray2D fftImagePadded = CommonFunctions.zeroPad(fftImage, fftSize, fftSize);
		
		//fftImage = CommonFunctions.computeFFT(fftImage);
		//float[] xcorr = CommonFunctions.multiply(fftImage.data, fftImage.data, false);
		
		//FloatArray2D xcorrImage = new FloatArray2D(xcorr, fftImage.width, fftImage.height);
		//xcorrImage = CommonFunctions.com
		
		//float[] pcm = CommonFunctions.computePhaseCorrelationMatrix(fftImagePadded.data, fftImagePadded.data, false);

		FloatProcessor blah = new FloatProcessor(width, height);
		blah.setPixels(fftImage.data);
		
		ImagePlus foo = new ImagePlus("test", blah);
		foo.show();
		IJ.log("min Value " + blah.getMin() + " max: " + blah.getMax());
		foo.setProcessor("test", blah.convertToByte(true));
		foo.show();
		IJ.log("min Value " + blah.convertToByte(true).getMin() + " max: " + blah.convertToByte(true).getMax());
		
	}

	
	public ImageProcessor getProcessor(int index) {
		return wholeStack.getProcessor(index);
	}
	
	/**
	 * Create the instances for the whole stack
	 * 
	 * @param classes list of classes names
	 * 
	 * @return whole stack set of instances
	 */
	public Instances createInstances(ArrayList<String> classes)
	{
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (int i=1; i<=wholeStack.getSize(); i++){
			String attString = wholeStack.getSliceLabel(i) + " numeric";
			attributes.add(new Attribute(attString));
		}
		
		attributes.add(new Attribute("class", classes));
		
		Instances data =  new Instances("segment", attributes, width*height);
		
		Object[] pixelData = wholeStack.getImageArray();
		
		for (int y=0; y<wholeStack.getHeight(); y++)
		{
			IJ.showProgress(y, wholeStack.getHeight());
			for (int x=0; x<wholeStack.getWidth(); x++)
			{
				double[] values = new double[wholeStack.getSize()+1];
				for (int z=1; z<=wholeStack.getSize(); z++)
				{
					values[z-1] = ((float[]) pixelData[z-1])[y*width+x];
					//System.out.println("" + wholeStack.getProcessor(z).getPixelValue(x, y) + " * " + values[z-1]);
				}
				values[wholeStack.getSize()] = 0.0;
				data.add(new DenseInstance(1.0, values));
			}
		}
		IJ.showProgress(1.0);
		return data;
	}
	
	/**
	 * Add the default features to the feature stack
	 */
	public void addDefaultFeatures()
	{
		int counter = 1;
		for (float i=1.0f; i<FeatureStack.MAX_SIGMA; i*=2){
			IJ.showStatus("Creating feature stack...   " + counter);
			this.addGaussianBlur(i); counter++;
			IJ.showStatus("Creating feature stack...   " + counter);			
			this.addGradient(i); counter++;
			IJ.showStatus("Creating feature stack...   " + counter);			
			this.addHessian(i); counter++;
			for (float j=1.0f; j<i; j*=2){
				IJ.showStatus("Creating feature stack...   " + counter);				
				this.addDoG(i, j); counter++;
			}
		}
		this.addMembraneFeatures(19, 1);
		
		IJ.showProgress(1.0);
	}
	
	/**
	 * Update features with current list
	 */
	public void updateFeatures()
	{
		wholeStack = new ImageStack(width, height);
		wholeStack.addSlice("original", originalImage.getProcessor().duplicate());

		int counter = 1;
		for (float i=1.0f; i<= FeatureStack.MAX_SIGMA; i*=2)
		{
			// Gaussian blur
			if(enableFeatures[GAUSSIAN])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addGaussianBlur(i); 
				counter++;
			}
			// Sobel
			if(enableFeatures[SOBEL])
			{
				IJ.showStatus("Creating feature stack...   " + counter);			
				this.addGradient(i); 
				counter++;
			}
			// Hessian
			if(enableFeatures[HESSIAN])
			{
				IJ.showStatus("Creating feature stack...   " + counter);			
				this.addHessian(i); 
				counter++;
			}
			// Difference of gaussians
			if(enableFeatures[DOG])
			{
				for (float j=1.0f; j<i; j*=2)
				{
					IJ.showStatus("Creating feature stack...   " + counter);				
					this.addDoG(i, j); 
					counter++;
				}
			}
			// Variance
			if(enableFeatures[VARIANCE])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addVariance(i); 
				counter++;
			}
			// Mean
			if(enableFeatures[MEAN])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addMean(i); 
				counter++;
			}
			
			// Min
			if(enableFeatures[MINIMUM])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addMin(i); 
				counter++;
			}
			// Max
			if(enableFeatures[MAXIMUM])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addMax(i); 
				counter++;
			}
			
			// Median
			if(enableFeatures[MEDIAN])
			{
				IJ.showStatus("Creating feature stack...   " + counter);
				this.addMedian(i); 
				counter++;
			}
			
		}
		// Membrane projections
		if(enableFeatures[MEMBRANE])
			this.addMembraneFeatures(19, 1);
		
		if(normalize)
		{
			IJ.showStatus("Normalizing stack...");
			final ImagePlus imp = new ImagePlus("", this.wholeStack);
			IJ.run(imp, "Enhance Contrast", "saturated=0.1 normalize_all");
		}
		
		IJ.showProgress(1.0);
		IJ.showStatus("Features stack is updated now!");
	}
	

	public void setEnableFeatures(boolean[] enableFeatures) {
		this.enableFeatures = enableFeatures;
	}

	public boolean[] getEnableFeatures() {
		return enableFeatures;
	}
	
	public boolean isEmpty()
	{
		return (null == this.wholeStack || this.wholeStack.getSize() < 2);
	}

	public void setNormalize(boolean normalize) {
		this.normalize = normalize;
	}

	public boolean isNormalized() {
		return normalize;
	}
	
	/**
	 * Save current whole stack as TIFF
	 * @param filename destination path and file name
	 * @return false if fails
	 */
	public boolean saveStackAsTiff(final String filename)
	{
		final ImagePlus ip = new ImagePlus("feature-stack", this.wholeStack);
		//ip.show();
		//IJ.log("path = " + filename);
		final FileSaver fs = new FileSaver(ip);
		return fs.saveAsTiffStack(filename);
	}
	
}
