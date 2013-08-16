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

import java.util.ArrayList;

import javax.vecmath.Point3f;

/**
 * Results from simple point warping (2D)
 *
 */
public class WarpingResults{
	/** warped source image after 2D simple point relaxation */
	public ImagePlus warpedSource;
	/** warping error */
	public double warpingError;

	public ArrayList<Point3f> mismatches;
	
	public ImagePlus classifiedMismatches = null;
}
