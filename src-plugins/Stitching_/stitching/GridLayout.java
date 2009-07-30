/**
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
 * An execption is the FFT implementation of Dave Hale which we use as a library,
 * wich is released under the terms of the Common Public License - v1.0, which is 
 * available at http://www.eclipse.org/legal/cpl-v10.html  
 *
 * @author Stephan Preibisch
 */
package stitching;

import java.util.ArrayList;

public class GridLayout
{
	public ArrayList<ImageInformation> imageInformationList;
	public int sizeX, sizeY, dim;
	public String fusionMethod, handleRGB, rgbOrder, arrangement;
	public double alpha = 1.5;
	public double thresholdR = 0.3;
	public double thresholdDisplacementRelative = 2.5;
	public double thresholdDisplacementAbsolute = 3.5;
}
