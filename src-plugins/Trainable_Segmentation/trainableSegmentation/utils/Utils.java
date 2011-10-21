package trainableSegmentation.utils;

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

import java.awt.Color;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import trainableSegmentation.metrics.ClassificationStatistics;
import util.FindConnectedRegions;
import util.FindConnectedRegions.Results;

/**
 * This class implements useful methods for the Weka Segmentation library.
 */
public class Utils {
	
	/**
	 * Connected components based on Find Connected Regions (from Mark Longair)
	 * @param im input image
	 * @param adjacency number of neighbors to check (4, 8...)
	 * @return list of images per region, all-regions image and regions info
	 */
	public static Results connectedComponents(final ImagePlus im, final int adjacency)
	{
		if( adjacency != 4 && adjacency != 8 )
			return null;

		final boolean diagonal = adjacency == 8 ? true : false;

		FindConnectedRegions fcr = new FindConnectedRegions();
		try {
			final Results r = fcr.run( im,
				 diagonal,
				 false,
				 true,
				 false,
				 false,
				 false,
				 false,
				 0,
				 1,
				 -1,
				 true /* noUI */ );
			return r;

		} catch( IllegalArgumentException iae ) {
			IJ.error(""+iae);
			return null;
		}

	}
	
	/**
	 * Plot the precision-recall curve
	 * @param stats classification statistics
	 */
	public static void plotPrecisionRecall(
			ArrayList< ClassificationStatistics > stats)
	{
		// Extract precision and recall values
		float[] precision = new float[ stats.size() ];
		float[] recall = new float[ stats.size() ];
		
		for(int i = 0; i < precision.length; i++)
		{
			precision[i] = (float) stats.get(i).precision;
			recall[i] = (float) stats.get(i).recall;
		}

		Plot pl = new Plot("Precision-Recall curve", "Recall [tp / (tp + fn)]", "Precision [tp / (tp+fp)]", recall, precision);		
		pl.setLimits(0, 1, 0, 1);
		pl.setSize(540, 512);
		pl.setColor(Color.GREEN);
		pl.show();
	}
	
	
	/**
	 * Create plot with the precision-recall curve
	 * @param stats classification statistics
	 * @return precision-recall plot
	 */
	public static Plot createPrecisionRecallPlot(
			ArrayList< ClassificationStatistics > stats)
	{
		// Extract precision and recall values		
		float[] precision = new float[ stats.size() ];
		float[] recall = new float[ stats.size() ];
		
		for(int i = 0; i < precision.length; i++)
		{
			precision[i] = (float) stats.get(i).precision;
			recall[i] = (float) stats.get(i).recall;
		}

		Plot pl = new Plot("Precision-Recall curve", "Recall [tp / (tp + fn)]", "Precision [tp / (tp+fp)]", recall, precision);		
		pl.setLimits(0, 1, 0, 1);
		pl.setSize(540, 512);
		pl.setColor(Color.GREEN);
		return pl;
	}

}
