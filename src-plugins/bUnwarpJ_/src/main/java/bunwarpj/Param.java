package bunwarpj;

import ij.gui.GenericDialog;

/**
 * bUnwarpJ plugin for ImageJ(C).
 * Copyright (C) 2005-2010 Ignacio Arganda-Carreras and Jan Kybic 
 *
 * More information at http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
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
 */

/**
 * This class stores the parameters to apply the consistent elastic registration
 * and it is independent of the primary GUI.
 */
public class Param {

	/** mode accuracy mode (0 - Fast, 1 - Accurate, 2 - Mono) */
	public int mode = 2;
	/** image subsampling factor (from 0 to 7, representing 2^0=1 to 2^7 = 128) */
	public int img_subsamp_fact = 0;
	/** minimum scale deformation (0 - Very Coarse, 1 - Coarse, 2 - Fine, 3 - Very Fine) */
	public int min_scale_deformation = 0;
	/** maximum scale deformation (0 - Very Coarse, 1 - Coarse, 2 - Fine, 3 - Very Fine, 4 - Super Fine) */
	public int max_scale_deformation = 2;
	/** divergence weight */
	public double  divWeight = 0;
	/** curl weight */
	public double  curlWeight= 0;
	/** landmark weight */
	public double  landmarkWeight = 0;
	/** image similarity weight */
	public double  imageWeight = 1;
	/** consistency weight */
	public double  consistencyWeight = 10;
	/** stopping threshold */
	public double  stopThreshold = 0.01;
	/** available registration modes */
	protected final String[] sRegistrationModes = { "Fast", "Accurate", "Mono" };
	/** minimum scale deformation choices */
	protected final String[] sMinScaleDeformationChoices = { "Very Coarse", "Coarse", "Fine", "Very Fine" };
	/** maximum scale deformation choices */
	protected String[] sMaxScaleDeformationChoices = { "Very Coarse", "Coarse", "Fine", "Very Fine", "Super Fine" };
	
	// Initial affine matrix pre-process
	/** percentage of shear correction in initial matrix */
	private double shearCorrection = 0.0;
	/** percentage of scale correction in initial matrix */
	private double scaleCorrection = 0.0;
	/** percentage of anisotropy correction in initial matrix */
	private double anisotropyCorrection = 0.0;
	
	/**
	 * Empty constructor
	 */
	public Param(){}
	
	/**
	 * Full constructor
	 * 
	 * @param mode mode accuracy mode (0 - Fast, 1 - Accurate, 2 - Mono)
	 * @param img_subsamp_fact image subsampling factor (from 0 to 7, representing 2^0=1 to 2^7 = 128)
	 * @param min_scale_deformation minimum scale deformation (0 - Very Coarse, 1 - Coarse, 2 - Fine, 3 - Very Fine)
	 * @param max_scale_deformation maximum scale deformation (0 - Very Coarse, 1 - Coarse, 2 - Fine, 3 - Very Fine, 4 - Super Fine)
	 * @param divWeight divergence weight
	 * @param curlWeight curl weight
	 * @param landmarkWeight landmark weight
	 * @param imageWeight image similarity weight
	 * @param consistencyWeight consistency weight
	 * @param stopThreshold stopping threshold
	 */
	public Param(int mode,
			int img_subsamp_fact,
			int min_scale_deformation,
			int max_scale_deformation,
			double  divWeight,
			double  curlWeight,
			double  landmarkWeight,
			double  imageWeight,
			double  consistencyWeight,
			double  stopThreshold)
	{
		this.mode = mode;
		this.img_subsamp_fact = img_subsamp_fact;
		this.min_scale_deformation = min_scale_deformation;
		this.max_scale_deformation = max_scale_deformation;
		this.divWeight = divWeight;
		this.curlWeight = curlWeight;
		this.landmarkWeight = landmarkWeight;
		this.imageWeight = imageWeight;
		this.consistencyWeight = consistencyWeight;
		this.stopThreshold = stopThreshold;		
	}
	
	/**
	 * Show modal dialog to collect bUnwarpJ parameters
	 * 
	 * @return false if the dialog was canceled or true if it was not.
	 */
	public boolean showDialog()
	{
		GenericDialog gd = new GenericDialog("Elastic Registration");
		
		gd.addChoice("Registration Mode", sRegistrationModes, sRegistrationModes[2]);
		
		// Maximum image pyramid resolution
		gd.addSlider("Image_Subsample_Factor", 0, 7, 0);
		
		// Advanced Options
		gd.addMessage("------ Advanced Options ------");
		
		gd.addChoice("Initial_Deformation :", sMinScaleDeformationChoices, sMinScaleDeformationChoices[this.min_scale_deformation]);		
				
		gd.addChoice("Final_Deformation :", sMaxScaleDeformationChoices, sMaxScaleDeformationChoices[this.max_scale_deformation]);		
		
		gd.addNumericField("Divergence_Weight :", this.divWeight, 1);
		gd.addNumericField("Curl_Weight :", this.curlWeight, 1);
		gd.addNumericField("Landmark_Weight :", this.landmarkWeight, 1);
		gd.addNumericField("Image_Weight :", this.imageWeight, 1);
		gd.addNumericField("Consistency_Weight :", this.consistencyWeight, 1);						
		
		gd.addNumericField("Stop_Threshold :", this.stopThreshold, 2);
		
		// Show generic dialog
		gd.showDialog();

		if (gd.wasCanceled()) 
			return false;
		
		// Fast or accurate mode
		this.mode = gd.getNextChoiceIndex();
		// Image subsampling factor at highest resolution level		
		this.img_subsamp_fact = (int) gd.getNextNumber();
		  
		// Min and max scale deformation level
		this.min_scale_deformation = gd.getNextChoiceIndex();
		this.max_scale_deformation = gd.getNextChoiceIndex();
				  
		// Weights
		this.divWeight  			= gd.getNextNumber();
		this.curlWeight 			= gd.getNextNumber();
		this.landmarkWeight 		= gd.getNextNumber();
		this.imageWeight			= gd.getNextNumber();
		this.consistencyWeight		= gd.getNextNumber();
		this.stopThreshold			= gd.getNextNumber();
				
		return true;
	} // end method showDialog
	
	/**
	 * Print parameters into a String
	 */
	public String toString()
	{
		return new String("Registration mode: " + sRegistrationModes[mode] + "\n" + 
						  "Image Sub-sampling factor:  " + img_subsamp_fact + " => " + Math.pow(2.0,img_subsamp_fact) + "\n" +
						  "Minimum scale factor = " + sMinScaleDeformationChoices[min_scale_deformation] + "\n" + 
						  "Maximum scale factor = " + sMaxScaleDeformationChoices[max_scale_deformation] + "\n" + 
						  "Divergence weight = " + divWeight + "\n" + 
						  "Curl weight = " + curlWeight + "\n" +
						  "Landmark weight = " + landmarkWeight + "\n" +
						  "Image weight = " + imageWeight + "\n" +
						  "Consistency weight = " + consistencyWeight + "\n" +
						  "Stopping threshold = " + stopThreshold + "\n" +
						  "Shear correction = " + shearCorrection + "\n" +
						  "Scale correction = " + scaleCorrection + "\n" +
						  "Anisotropy correction = " + anisotropyCorrection + "\n");
	}

	public void setShearCorrection(double shearCorrection) {
		this.shearCorrection = shearCorrection;
	}

	public double getShearCorrection() {
		return shearCorrection;
	}

	public void setScaleCorrection(double scaleCorrection) {
		this.scaleCorrection = scaleCorrection;
	}

	public double getScaleCorrection() {
		return scaleCorrection;
	}

	public void setAnisotropyCorrection(double anisotropyCorrection) {
		this.anisotropyCorrection = anisotropyCorrection;
	}

	public double getAnisotropyCorrection() {
		return anisotropyCorrection;
	}
	
	
} // end class Param
