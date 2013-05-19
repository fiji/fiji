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


import ij.ImagePlus;
import stitching.model.*;

public class ImageInformation extends Tile implements Comparable<ImageInformation>
{
	public ImageInformation(int dim, int id, Model model)
	{
		super (1, 1, model);
		
		offset = new float[dim];
		size = new float[dim];
		position = new float[dim];
		this.id = id;
		this.dim = dim;
	}
	
	public Object[] imageStack;
	public int w, d, h;

	public int imageType;
	
	// -1 means ignore the series number, it is needed when loading
	// stacks that are in one file
	public int seriesNumber = -1;
	
	public String imageName;
	public ImagePlus imp = null, maxIntensity = null, tmp = null;
	public boolean overlaps = false;
	public boolean invalid = false;
	final public float[] offset;
	final public float[] size;
	public boolean closeAtEnd = false;
	public float[] position;
	final public int id, dim;
	
	public String toString()
	{
		String out =  "Image: '" + imageName + "' Imp: '" + imp + "' Offset: (";
		
		for (int i = 0; i < offset.length; i++)
		{
			if (i < offset.length - 1)					
				out += offset[i] + ", ";
			else
				out += offset[i] + ")";
		}
		return out;
	}

	public int compareTo(ImageInformation o)
	{
		if (id < o.id)
			return -1;
		else if (id > o.id)
			return 1;
		else
			return 0;
	}
}
