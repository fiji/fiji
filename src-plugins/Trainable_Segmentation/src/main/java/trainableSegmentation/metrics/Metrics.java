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
	
	public Metrics(ImagePlus originalLabels, ImagePlus proposedLabels)
	{
		this.originalLabels = originalLabels;
		this.proposedLabels = proposedLabels;
	}
	
	public abstract double getMetricValue(double binaryThreshold);

}
