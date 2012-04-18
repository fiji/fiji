package emblcmci;
/**
 * Algorithm "Match_To_Image_Histogram.java"
 * (see http://www.imagingbook.com)
 * applied to bleach correction.
 * original package from above site "histogram2" is
 * required for this plugin (already included in .jar of this program).
 *
 * contact: Kota Miura (CMCI, EMBL Heidelberg, miura@embl.de)
 *
 * works with 8bit and 16 bit stacks.
 * this correction algorithm is not appropriate for intensity measurements.
 * use only for segmentation.
 *
 * Copyright © 2010 Kota Miura
 * License: GPL 2
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import histogram2.HistogramMatcher;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;

public class BleachCorrection_MH {// implements PlugIn {
	ImagePlus imp;
	Roi curROI = null;
	/**
	 * @param imp
	 */
	public BleachCorrection_MH(ImagePlus imp) {
		super();
		this.imp = imp;
	}
	/** This constructor might not going to be used.
	 * 	Does not mean much to select a spedific region for the reference histogram.
	 * @param imp
	 * @param curROI
	 */
	public BleachCorrection_MH(ImagePlus imp, Roi curROI) {
		super();
		this.imp = imp;
		this.curROI = curROI;
	}

	public void doCorrection(){

		int histbinnum = 0;
		if (imp.getBitDepth()==8) histbinnum = 256;
			else if (imp.getBitDepth()==16) histbinnum = 65536;//65535;

		boolean is3DT = false;
		int zframes = 1;
		int timeframes = 1;
		int[] impdimA = imp.getDimensions();
		IJ.log("slices"+Integer.toString(impdimA[3])+"  -- frames"+Integer.toString(impdimA[4]));
		//IJ.log(Integer.toString(imp.getNChannels())+":"+Integer.toString(imp.getNSlices())+":"+ Integer.toString(imp.getNFrames()));
		if (impdimA[3]>1 && impdimA[4]>1){	// if slices and frames are both more than 1
			is3DT =true;
			zframes = impdimA[3];
			timeframes = impdimA[4];
			if ((zframes*timeframes) != imp.getStackSize()){
				IJ.showMessage("slice and time frames do not match with the length of the stack. Please correct!");
				return;
			}
		}

		ImageStack stack = imp.getStack();
		ImageProcessor ipA = null;
		ImageProcessor ipB = null;
		HistogramMatcher m = new HistogramMatcher();
		int[] hA = new int[histbinnum];
		int[] hB = new int[histbinnum];
		int[] F = new int[histbinnum];
		int[] histB = null;	//for each slice
		int[] histA = null;
		//IJ.log(Integer.toString(stack.getSize()));
		int i =0;
		int j =0;
		int k =0;
		/* in case of 3D, stack histogram of the first time point is measured, and then
		 *  this stack histogram is used as reference (hB) for the rest of time points.
		 */
		if (is3DT){
			//should implement here,
			for (i =0; i< timeframes; i++){
				if (i==0){
					for (j=0; j<zframes; j++){
						ipB = stack.getProcessor(i*zframes+j+1);
						histB = ipB.getHistogram();
						for (k=0; k<histbinnum; k++) hB[k] += histB[k];
					}
				} else {
					for (k=0; k<histbinnum; k++) hA[k] = 0;
					for (j=0; j<zframes; j++){
						ipA = stack.getProcessor(i*zframes+j+1);
						histA = ipA.getHistogram();
						for (k=0; k<histbinnum; k++) hA[k] += histA[k];
					}
					F = m.matchHistograms(hA, hB);
					for (j=0; j<zframes; j++){
						ipA = stack.getProcessor(i*zframes+j+1);
						ipA.applyTable(F);
					}
					IJ.log("corrected time point: "+Integer.toString(i+1));
				}
			}

		} else {		//2D case.
			for (i=0; i<stack.getSize(); i++){
				if (i==0) {
					ipB = stack.getProcessor(i+1);
					hB = ipB.getHistogram();
				}
				else {
					ipA = stack.getProcessor(i+1);
					hA = ipA.getHistogram();
					F = m.matchHistograms(hA, hB);
					ipA.applyTable(F);
					IJ.log("corrected frame: "+Integer.toString(i+1));
				}
			}
		}
		//imp.show();
	}



}
