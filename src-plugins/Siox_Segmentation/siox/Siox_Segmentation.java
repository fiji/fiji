/**
 * Siox_Segmentation plug-in for ImageJ(C) and Fiji.
 * Copyright (C) 2009 Ignacio Arganda-Carreras 
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


import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

/**
 * Fiji plugin to run SIOX: Simple Input Object Segmentation.
 * 
 * @author Ignacio Arganda-Carreras (ignacio.arganda at gmail.com)
 *
 */
public class Siox_Segmentation implements PlugIn
{
	/** input image to be segmented */
	private ImagePlus inputImage = null;
	/** segmentation gui */
	private SegmentationGUI gui = null;
	
public static void main(String[] args) {
	ij.ImageJ.main(args);
	IJ.run("Clown (14K)");
	new Siox_Segmentation().run(null);
}
	//-----------------------------------------------------------------
	/**
	 * Main plugin method.
	 * 
	 * @param arg plugin arguments
	 * @Override 
	 */
	public void run(String arg) 
	{
		// Get current image
		this.inputImage  = WindowManager.getCurrentImage();
		if (null == inputImage) 
		{
			this.inputImage = IJ.openImage();
			if (null == this.inputImage) 
				return; // user canceled open dialog
		}
		// Check if it is a color image
		if(this.inputImage.getType() != ImagePlus.COLOR_RGB)
		{
			IJ.error("Siox Segmentation only works on RGB color images, please convert.");
			return;
		}
		
		// Create gui
		this.gui = new SegmentationGUI(this.inputImage);
		// Hide input image window
		//this.inputImage.getWindow().setVisible(false);
		
	}// end run method
	
	//-----------------------------------------------------------------
	/**
	 * Segmentation Graphical User Interface
	 * 
	 * @author Ignacio Arganda-Carreras (ignacio.arganda at gmail.com)
	 *
	 */
	private class SegmentationGUI extends JFrame
	{
		
		
		
		public SegmentationGUI(ImagePlus imp) 
		{
			
			this.setTitle("SIOX Segmentation ");
			// Image panel
			JImagePanelPlus image_panel = new JImagePanelPlus(imp);
			
			// Control panel
			ControlJPanel control_panel = new ControlJPanel();						
			
			JPanel all = new JPanel();
			BoxLayout box = new BoxLayout(all, BoxLayout.X_AXIS);
			all.setLayout(box);
	  	    all.add(image_panel);
	  	    all.add(control_panel);
	  	    
	  	    this.getContentPane().add(all);
			
 	      	this.pack();
 	      	this.setVisible(true);
		}
	}

}// end class Siox_Segmentation
