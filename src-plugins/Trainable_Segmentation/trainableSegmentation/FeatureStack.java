package trainableSegmentation;
/* This class is intended for the Simple_Segmentation plugin. It creates and holds
 * different feature images for the classification. Possible filters include:
 * - Gaussianblur
 * - Gradientmagnitude
 * - Hessian
 * - Difference of Gaussian
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
 * Author: Verena Kaynig (verena.kaynig@inf.ethz.ch)
 */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import ij.IJ;
import ij.ImageStack;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.FHT;
import ij.plugin.FFT;
import ij.plugin.ZProjector;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.Convolver;

import edu.mines.jtk.dsp.*;
import stitching.*;


public class FeatureStack {
	private ImagePlus originalImage;
	private ImageStack wholeStack;
	private int width;
	private int height;
	
	public FeatureStack(ImagePlus image){
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
		wholeStack.addSlice("GaussianBlur_" + sigma, ip);
	}
	
	public void writeConfigurationToFile(String filename){
		try{
			BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(
				new FileOutputStream(filename) ) );
				try{	
					for (int i=1; i <= wholeStack.getSize(); i++){
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
		wholeStack.addSlice("SobelFilter_"+sigma, ip);
	}
	
	public void addHessian(float sigma){
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
		
		wholeStack.addSlice("Hessian_"+sigma, ip);
		wholeStack.addSlice("HessianTrace_"+sigma, ipTr);
		wholeStack.addSlice("HessianDeterminant_"+sigma, ipDet);
	}
	
	public void addDoG(float sigma1, float sigma2){
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
		
		wholeStack.addSlice("DoG_"+sigma1+"_"+sigma2, ip);
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
		for (int i=0; i<9; i++){
			rotatedPatch = membranePatch.duplicate();
			rotatedPatch.invert();rotatedPatch.rotate(20*i);rotatedPatch.invert();
			Convolver c = new Convolver();				
	
			float[] kernel = (float[]) rotatedPatch.getPixels();
			ImageProcessor ip = originalImage.getProcessor().duplicate();		
			c.convolveFloat(ip, kernel, patchSize, patchSize);		

			is.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip);
		//	wholeStack.addSlice("Membrane_"+patchSize+"_"+membraneSize, ip.convertToByte(true));
		}
		
		ImagePlus projectStack = new ImagePlus("membraneStack",is);
		ZProjector zp = new ZProjector(projectStack);
		zp.setStopSlice(is.getSize());
		for (int i=0;i<6; i++){
			zp.setMethod(i);
			zp.doProjection();
			wholeStack.addSlice("Membrane_Projection_"+i+"_"+patchSize+"_"+membraneSize, zp.getProjection().getChannelProcessor());
		}
	}
	
	
	public void addTest(){
		FloatArray2D fftImage = new FloatArray2D((float[]) originalImage.getProcessor().getPixels(),originalImage.getWidth(), originalImage.getHeight());
		int fftSize = FftReal.nfftFast(Math.max(width, height));
		FloatArray2D fftImagePadded = CommonFunctions.zeroPad(fftImage, fftSize, fftSize);
		
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
	
	public Instances createInstances(){
		FastVector attributes = new FastVector();
		for (int i=1; i<=wholeStack.getSize(); i++){
			String attString = wholeStack.getSliceLabel(i) + " numeric";
			attributes.addElement(new Attribute(attString));
		}
		FastVector classes = new FastVector();
		classes.addElement("foreground");
		classes.addElement("background");
		attributes.addElement(new Attribute("class", classes));
		
		Instances data =  new Instances("segment", attributes, width*height);
		
		Object[] pixelData = wholeStack.getImageArray();
		
		for (int y=0; y<wholeStack.getHeight(); y++){
			IJ.showProgress(y, wholeStack.getHeight());
			for (int x=0; x<wholeStack.getWidth(); x++){
				double[] values = new double[wholeStack.getSize()+1];
				for (int z=1; z<=wholeStack.getSize(); z++){
					//values[z-1] = wholeStack.getProcessor(z).getPixelValue(x, y);
					//values[z-1] = 0xff & ((byte[]) pixelData[z-1])[y*width+x];
					values[z-1] = ((float[]) pixelData[z-1])[y*width+x];
					//System.out.println("" + wholeStack.getProcessor(z).getPixelValue(x, y) + " * " + values[z-1]);
				}
				values[wholeStack.getSize()] = 0.0;
				data.add(new Instance(1.0, values));
			}
		}
		
		return data;
	}
	
	public void addDefaultFeatures(){
		int counter = 1;
		for (float i=1.0f; i<17; i*=2){
			IJ.showStatus("creating feature stack   " + counter);
			this.addGaussianBlur(i); counter++;
			IJ.showStatus("creating feature stack   " + counter);			
			this.addGradient(i); counter++;
			IJ.showStatus("creating feature stack   " + counter);			
			this.addHessian(i); counter++;
			for (float j=1.0f; j<i; j*=2){
				IJ.showStatus("creating feature stack   " + counter);				
				this.addDoG(i, j); counter++;
			}
		}
		this.addMembraneFeatures(19, 1);
	}
}
