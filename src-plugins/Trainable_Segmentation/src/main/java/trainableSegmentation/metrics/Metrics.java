package trainableSegmentation.metrics;

/**
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
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu)
 */

import ij.IJ;
import ij.ImagePlus;

/**
 * This is the mother class for 2D segmentation metrics 
 */
public abstract class Metrics 
{
	
	/** original labels (single 2D image or stack) */
	ImagePlus originalLabels;
	/** proposed new labels (single 2D image or stack of the same as as the original labels) */
	ImagePlus proposedLabels;
	
	/** boolean flag to set the level of detail on the standard output messages */
	protected boolean verbose = true;

	
	public Metrics(ImagePlus originalLabels, ImagePlus proposedLabels)
	{
		this.originalLabels = originalLabels;
		this.proposedLabels = proposedLabels;
	}
	
	public abstract double getMetricValue(double binaryThreshold);
	
    /**
     * Set verbose mode
     * @param verbose true to display more information in the standard output
     */
    public void setVerboseMode(boolean verbose) 
    {		
    	this.verbose = verbose;
	}
	
	/**
	 * Get the minimum metric value over a set of thresholds 
	 * 
	 * @param minThreshold minimum threshold value to binarize the input images
	 * @param maxThreshold maximum threshold value to binarize the input images
	 * @param stepThreshold threshold step value to use during binarization
	 * @return minimum value of the metric
	 */
	public double getMinimumMetricValue(
			double minThreshold,
			double maxThreshold,
			double stepThreshold )
	{
		double min = 1.0;
		double bestTh = minThreshold;
		for(double th =  minThreshold; th <= maxThreshold; th += stepThreshold)
		{
			if( verbose ) 
				IJ.log("  Calculating metric value for threshold " + String.format("%.3f", th) + "...");
			
			double error = getMetricValue( th );
			if( min > error )
			{
				min = error;;
				bestTh = th;
			}
			if( verbose )
				IJ.log("    Error = " + error);
		}
		
		if( verbose )
			IJ.log(" ** Minimum metric value = " + min + ", with threshold = " + bestTh + " **\n");
	    return min;
	}
	
	

}
