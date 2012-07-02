package emblcmci;

/** Bleach Correction Algorithm with Simple Ratio Calculation.
 *  Migrated from 2D algorithm of ImageJ Macro written by Jens Rietdorf
 *  Original macro could be found at http://www.embl.de/eamnet/html/bleach_correction.html
 *
 *  This correction algorithm resembles the method by Phair.
 *
 *  This plugin works on 2D and 3D time series (for 3D time series, it should be a hyperstack).
 *  in case of 3D times series, mean intensity in the first time point stack becomes the reference.
 *  @author Kota Miura (miura@embl.de)
 *
 * Copyright © 2004, 2005, 2010 Jens Rietdorf, Kota Miura
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

import ij.IJ;
import ij.ImagePlus;
//import ij.gui.Roi;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

public class BleachCorrection_SimpleRatio {
	ImagePlus imp;
	double referenceInt = 0;
	double baselineInt = 0;
	Roi curROI = null;

	/**
	 * @param imp ImagePlus instance
	 */
	public BleachCorrection_SimpleRatio(ImagePlus imp) {
		super();
		this.imp = imp;
	}
	public BleachCorrection_SimpleRatio(ImagePlus imp, double baselineInt) {
		super();
		this.imp = imp;
		this.baselineInt = baselineInt;
	}
	/**
	 * @param imp
	 * @param curROI
	 */
	public BleachCorrection_SimpleRatio(ImagePlus imp, Roi curROI) {
		super();
		this.imp = imp;
		this.curROI = curROI;
	}

	public boolean showDialogAskBaseline()	{
		GenericDialog gd = new GenericDialog("Bleach Correction");
		gd.addNumericField("Background Intensity", baselineInt, 1) ;
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		baselineInt = gd.getNextNumber();
		return true;

	}

	public ImagePlus correctBleach(){

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
				return null;
			}
		}

		ImageStatistics imgstat = new ImageStatistics();
		ImageProcessor curip = null;
		double currentInt = 0.0;
		double ratio = 1.0;
		if (curROI == null) curROI = new Roi(0, 0, imp.getWidth(), imp.getHeight());
		if (!is3DT) {
			for (int i = 0; i < imp.getStackSize(); i++){
				curip = imp.getImageStack().getProcessor(i+1);
				curip.setRoi(curROI);
				imgstat = curip.getStatistics();

				curip.setRoi(0, 0, imp.getWidth(), imp.getHeight());
				if (i == 0) {
					referenceInt = imgstat.mean - baselineInt;
					curip.add(-1 * baselineInt);
					System.out.println("ref intensity=" + imgstat.mean);
				} else {
					currentInt = imgstat.mean - baselineInt;
					ratio = referenceInt / currentInt;
					curip.add(-1 * baselineInt);
					curip.multiply(ratio);
					System.out.println("frame"+i+1+ "mean int="+ currentInt +  " ratio=" + ratio);
				}

			}
		} else {
			for (int i = 0; i < timeframes; i++){
				currentInt = 0.0;
				for (int j = 0; j < zframes; j++) {
					curip = imp.getImageStack().getProcessor(i * zframes + j + 1);
					curip.setRoi(curROI);
					imgstat = curip.getStatistics();
					currentInt += imgstat.mean;
					curip.setRoi(0, 0, imp.getWidth(), imp.getHeight());
					curip.add(-1 * baselineInt);
				}
				currentInt /= zframes;
				currentInt -= baselineInt;

				if (i == 0) {
					referenceInt = currentInt;
				} else {
					ratio = referenceInt / currentInt;
					for (int j = 0; j < zframes; j++) {
						curip = imp.getImageStack().getProcessor(i * zframes + j + 1);
						curip.setRoi(0, 0, imp.getWidth(), imp.getHeight());
						curip.multiply(ratio);
					}
					System.out.println("frame"+i+1+ ": mean int="+ currentInt +  " ratio=" + ratio);
				}
			}
		}

		return imp;
	}


}
