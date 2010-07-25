/* Retinex_.java Using ImageJ Gaussian Filter
 * Retinex filter algorithm based on the plugin for GIMP.
 * 
 *@author Jimenez-Hernandez Francisco <jimenezf@fi.uaemex.mx>
 *Developed at Birmingham University, School of Dentistry. Supervised by Gabriel Landini
 *@version 1.0
 *
 * 8 July 2010
 *
 * This version uses ImageJ Gaussian blurring instead of GIMP's linear Gaussian
 * because there is a bug in GIMP's implementation that shifts the results of the blurring 
 * to the right of the image when using more than 3 scales.
 *
 * Based on:
 * MSRCR Retinex
 * (Multi-Scale Retinex with Color Restoration)
 *  2003 Fabien Pelisson <Fabien.Pelisson@inrialpes.fr>
 * Retinex GIMP plug-in
 *
 * Copyright (C) 2009 MAO Y.B
 *               2009. 3. 3 
 *               Visual Information Processing (VIP) Group, NJUST
 * 
 * D. J. Jobson, Z. Rahman, and G. A. Woodell. A multi-scale 
 * Retinex for bridging the gap between color images and the 
 * human observation of scenes. IEEE Transactions on Image Processing,
 * 1997, 6(7): 965-976
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import java.util.Vector;
import ij.plugin.filter.GaussianBlur;

public class Retinex_ implements PlugIn {
	/*Global vars*/
	int levelValue=0, scaleValue=0, scaleDivValue=0, alpha=128, offset=0;

	int scale=0, nscales=0, scales_mode=0;
	float cvar=0f, gain=1f;

	/*gauss3_coef*/
	int N;
	float sigma;
	double B;
	double [] b= new double[4];

	float dynamicValue=0f;
	public static final int RETINEX_UNIFORM=0, RETINEX_LOW=1, RETINEX_HIGH=2;
	public static final int MAX_RETINEX_SCALES = 8;
	public static final int MIN_GAUSSIAN_SCALE = 16;
	public static final int MAX_GAUSSIAN_SCALE = 250;

	float[] RetinexScales= new float [MAX_RETINEX_SCALES];
	float mean=0f, var=0f;

	public void run(String arg) {
		// 1 - Obtain the currently active image:
		ImagePlus imp = IJ.getImage();
		if (null == imp) return;
		int stackSize = imp.getStackSize();
 		if(imp.getBitDepth() != 24) {
			IJ.showMessage("Only RGB images are supported.");
			return;
		}
		// 2 - Ask for parameters:
		GenericDialog gd = new GenericDialog("Retinex params", IJ.getInstance());

		String []level= {"Uniform", "Low", "High"};
		gd.addChoice("Level:", level, "Uniform");
		gd.addSlider("Scale:", 16.0, 250.0, 240.0);
		gd.addSlider("Scale_division:", 1.0, 8.0, 3.0);
		gd.addSlider ("Dynamic:",0.05, 4.0, 1.2);
//		gd.addMessage("Options");
//		gd.addSlider("Alpha:", 0, 255.0, 128);
//		gd.addSlider("Gain:", 0.001, 1000.0, 1.0);
//		gd.addSlider("Offset:", 0, 255, 0);

		if (stackSize>1) 
			gd.addCheckbox("Stack",false);

		gd.showDialog();
		if (gd.wasCanceled())
			return;

		// 3 - Retrieve parameters from the dialog
		levelValue =(int) gd.getNextChoiceIndex();
		Vector sliderValues = gd.getSliders();
		scaleValue= (int)gd.getNextNumber();
		scaleDivValue= (int)gd.getNextNumber();
		dynamicValue= (float)gd.getNextNumber();

//		alpha= (int)gd.getNextNumber();
//		gain= (float)gd.getNextNumber();
//		offset= (int)gd.getNextNumber();

		boolean doIstack=false;
		if (stackSize>1) 
			 doIstack = gd.getNextBoolean ();

		// 4 - Execute!
		Object[] result=null;
		if (doIstack) {
			for (int j=1; j<=stackSize; j++){
				imp.setSlice(j);
				 result = exec(imp, scaleValue, scaleDivValue, levelValue, dynamicValue);
				if(null!=result){
					ImagePlus scaled = (ImagePlus) result[1];
					scaled.show();
				}
			}
			imp.setSlice(1);
			IJ.run(imp, "Images to Stack", "name=Stack_Retinex title=Retinex_");
		}
		else 
			 result = exec(imp, scaleValue, scaleDivValue, levelValue, dynamicValue);

		// 5 - If all went well, show the image:
		if(null!=result){
			ImagePlus scaled = (ImagePlus) result[1];
			scaled.show();
		}
	}

	public Object[] exec(ImagePlus imp, int scale, int scaleDiv, int level, float dynamic){

		// Assumes an RGB image, we do not test this
		IJ.showStatus("Performing Retinex..."+"Scale:"+ scale+"  Scale Div:"+ scaleDiv+"  Dynamic:"+dynamic);
		IJ.showProgress(0.0);
		ImageProcessor ip = imp.getProcessor();
		int width, height, i, i3, offset, c;
		int[] inputPixels;

		//Using GIMP default params
		//rvals = new RetinexParams(240, 3, RETINEX_UNIFORM, 1.2f);
		this.scale= scale;
		this.nscales= scaleDiv;
		this.scales_mode=level;
		this.cvar=dynamic;
		width = ip.getWidth();
		height = ip.getHeight();

		inputPixels= new int[3*height*width];

		for (int row = 0; row < height; row++){
			offset = row*width;
			for (int col = 0; col < width; col++) {
					i = (offset + col) * 3;
					c = ip.get(col, row);
					inputPixels[i] = (int)(c&0x0000ff);//B
					inputPixels[i+1] = (int)((c&0x00ff00)>>8);//G
					inputPixels[i+2] = (int)((c&0xff0000)>>16);//R
			}
		}

		IJ.showProgress(0.2);
		//Main Process
		try{
			inputPixels=MSRCR(inputPixels, width, height,3);
			//Building an array for showing the output
			int[] output= new int[height*width];
			for (int row = 0; row < height; row++){
				offset = row*width;
				for (int col = 0; col < width; col++) {
					i = offset + col;
					i3 = i * 3;
					output[i] = ((inputPixels[i3+2]& 0xff)<<16)+((inputPixels[i3+1]& 0xff)<<8) + (inputPixels[i3]& 0xff);
				}        
			}
			ImagePlus imProc= NewImage.createRGBImage("Retinex_", width, height, 1, NewImage.FILL_BLACK);
			ImageProcessor imProcP = imProc.getProcessor();
			imProcP.setPixels(output);
			IJ.showProgress(1.0);
			return new Object[]{"Retinex_"+imp.getTitle(), imProc};
		}catch(Exception e){IJ.log("Error: "+ e.getMessage()); return null;}
	}

	 float clip(float val, int minv, int maxv){
		return (( val = (val < minv ? minv : val ) ) > maxv ? maxv : val );
	}

	/*
	* MSRCR = MultiScale Retinex with Color Restoration
	*/
	int [] MSRCR(int[] src, int width, int height, int bytes){
		int scale, row, col;
		int i, j;
		int size;
		int pos;
		int channel;
		float []dst;
		float []in, out;
		float []pdst;
		float []psrc;
		int channelsize;
		float weight;
		float mini, range, maxi;
		float progress=0.2f;
		/* Allocating all the memory needed for the algorithm*/
		size=width*height*bytes;
		dst = new float[size];

		channelsize = width*height;

		in= new float[channelsize];
		out = new float [channelsize];
		float [][]BGR= new float[3][channelsize];
		/*
		* Calculate the scales of filtering according to the
		* number of filter and their distribution.
		*/
		RetinexScales = retinex_scales_distribution( RetinexScales,this.nscales, this.scales_mode, this.scale );

		/*
		Filtering according to the various scales.
		Summarize the results of the various filters according to a
		specific weight(here equivalent for all).
		*/
		weight = 1f/(float)nscales; 
		/*
		* Here we changed the recursive filtering algorithm for ImageJ's gaussian blurring
		* as the recursive filter results drift to the right of the image, possibly a bug
		*/
		ImagePlus []imBGR= new ImagePlus[3];
		ImageProcessor []imProcBGR= new ImageProcessor[3];
		GaussianBlur blurImage= new GaussianBlur();
		pos = 0;
		for(channel = 0; channel < 3; channel++){
			IJ.showProgress(progress+=0.2f);
			for(i = 0, pos = channel; i < channelsize; i++, pos +=bytes ){
				in[i]=(float)(src[pos]+1.0);
				BGR[channel][i]=in[i];
			}
			imBGR[channel]= NewImage.createFloatImage(""+channel, width, height, 1, NewImage.FILL_BLACK);
			imProcBGR[channel]= imBGR[channel].getProcessor();
			imProcBGR[channel].setPixels(BGR[channel]);

			for(scale=0; scale <nscales; scale++){
				blurImage= new GaussianBlur();
				blurImage.blur(imProcBGR[channel],(double)RetinexScales[scale]*2.5); //2.5 is the difference between IJ blur and photoshop's see ImageJ API
				out=(float[]) imProcBGR[channel].getPixelsCopy();
				/*
				*Summarize the filtered values.
				*In fact one calculates a ratio between the original values and the filtered values.
				*/
				for ( i = 0, pos = channel; i < channelsize; i++, pos += bytes )
					dst[pos] += weight * (float)( Math.log(src[pos] +1f) - Math.log(out[i]) );
			}
		}

		/*
		* Final calculation with original value and cumulated filter values.
		* The parameters gain, alpha and offset are constants.
		*/
		/* Ci(x,y)=log[a Ii(x,y)]-log[ Ei=1-s Ii(x,y)] */
		// alpha = 128f;
		// gain =1f;
		// offset = 0f;

		psrc= new float[size];
		pdst= new float[size];
		for(i=0; i<size;i++){
			psrc[i]=(float)src[i];
			pdst[i]= (float)dst[i];
		}

		for(i=0; i<size; i+= bytes){
			float logl;
			logl = (float)Math.log((float)psrc[i] + (float)psrc[i+1] + (float)psrc[i+2] + 3f);

			pdst[i] = gain * ((float)(Math.log(alpha * (psrc[i]+1.0f)) - logl) * pdst[i]) + offset;
			pdst[i+1] = gain * ((float)(Math.log(alpha * (psrc[i+1]+1.0f)) - logl) * pdst[i+1]) + offset;
			pdst[i+2] = gain * ((float)(Math.log(alpha * (psrc[i+2]+1.0f)) - logl) * pdst[i+2]) + offset;
		}

		compute_mean_var(pdst, size, bytes);
		mini = mean -cvar*var;
		maxi = mean + cvar*var;
		range = maxi - mini;

		if(range==0) range=1f;
		int [] result= new  int[size];
		for(i=0; i<size; i+=bytes){
			for(j=0; j<3; j++){
				float c = 255f* (pdst[i+j] - mini)/ range;
				psrc[i+j]= clip(c, 0, 255);
				result[i+j]= (int)psrc[i+j];
			}
		}
		return result;
	}

	/*
	* Calculate the mean and variance.
	*/
	void compute_mean_var(float[] src, int size, int bytes){
		float vsquared=0f;
		int i, j;
		float [] psrc;

		mean=0f;	
		for(i=0; i<size; i+=bytes){
			for(j=0; j<3; j++){
				mean+= src[i+j];
				vsquared+=src[i+j]*src[i+j];
			}
		}
		mean /= (float)size;
		vsquared /=  (float)size;
		var=(vsquared-(mean*mean));
		var= (float)Math.sqrt(var);
	}

	/*
	* Calculate scale values for desired distribution.
	*/
	float [] retinex_scales_distribution( float[] scales, int nscales, int mode, int s) {
		if(nscales ==1)
			scales[0]= (float)s/2f;
		else if(nscales==2){
			scales[0]= (float)s/2f;
			scales[1]= (float)s;
		}
		else {
			float size_step = (float)s / (float)nscales;
			int i;
			switch(mode){
				case RETINEX_UNIFORM:
				for(i=0;i<nscales; ++i)
					scales[i]= 2f + (float)i*size_step;
				break;

				case RETINEX_LOW:	
				size_step= (float)Math.log(s - 2f)/(float)nscales;
				for(i=0; i<nscales; ++i)
					scales[i] =  2f + (float)Math.pow(10, (i*size_step) / Math.log(10));
				break;

				case RETINEX_HIGH:
				size_step = (float)Math.log(s-2f)/(float)nscales;
				for(i=0; i<nscales; ++i)
					scales[i]= s - (float)Math.pow(10, (i*size_step)/Math.log(10));
				break;

				default:
				break;
			}
		}
		return scales;
	}
}
