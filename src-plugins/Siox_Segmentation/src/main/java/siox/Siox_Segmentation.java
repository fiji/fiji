/**
 * Siox_Segmentation plug-in for ImageJ and Fiji.
 * 2009 Ignacio Arganda-Carreras, Johannes Schindelin, Stephan Saalfeld 
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


package siox;


import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

/**
 * Fiji plugin to run SIOX: Simple Interactive Object Extraction.
 * 
 * @author Ignacio Arganda-Carreras (ignacio.arganda at gmail.com)
 *
 */
public class Siox_Segmentation implements PlugIn
{
	/** input image to be segmented */
	private ImagePlus inputImage = null;

	
public static void main(String[] args) {
	ij.ImageJ.main(args);
	IJ.run("Leaf (36K)");
	
	Siox_Segmentation s = new Siox_Segmentation();
	s.run(null);
}

	//@Override
	public void run(String args) 
	{
		this.inputImage = IJ.getImage();
		
		if(inputImage.getType() != ImagePlus.COLOR_RGB)
		{
			IJ.error("SIOX Segmentation", "SIOX works only on RGB images");
			return;
		}
		
		if(inputImage.getNSlices() > 1)
		{
			IJ.error("SIOX Segmentation", "SIOX does not support stacks");
			return;
		}
		
		new SegmentationGUI(this.inputImage);
		
	}


	

}// end class Siox_Segmentation
