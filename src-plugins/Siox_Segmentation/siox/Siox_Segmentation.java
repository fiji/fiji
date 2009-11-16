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
import ij.WindowManager;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * Fiji plugin to run SIOX: Simple Input Object Segmentation.
 * 
 * @author Ignacio Arganda-Carreras (ignacio.arganda at gmail.com)
 *
 */
public class Siox_Segmentation implements PlugInFilter
{
	/** input image to be segmented */
	private ImagePlus inputImage = null;

	
public static void main(String[] args) {
	ij.ImageJ.main(args);
	IJ.run("Leaf (36K)");
	
	Siox_Segmentation s = new Siox_Segmentation();
	s.setup(null, WindowManager.getCurrentImage());
	s.run(null);
}



	@Override
	public void run(ImageProcessor ip) 
	{
		new SegmentationGUI(this.inputImage);
		
	}

	@Override
	public int setup(String arg, ImagePlus imp) 
	{
		this.inputImage = imp;
		
		if ("about".equals(arg)) 
		{
			showAbout();
			return DONE;
		}

		return DOES_RGB; 
	}

	private void showAbout() {
		IJ.showMessage(
				"About Siox Segmentation...",
				"This plug-in filter segmentates color images based on SIOX: Simple Interactive Object Extraction\n");
		
	}
	
	

}// end class Siox_Segmentation
