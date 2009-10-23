package siox;

import java.awt.Panel;

import ij.ImagePlus;
import ij.gui.ImageWindow;

import javax.swing.BoxLayout;

//-----------------------------------------------------------------
/**
 * Segmentation Graphical User Interface
 * 
 * @author Ignacio Arganda-Carreras (ignacio.arganda at gmail.com)
 *
 */
public class SegmentationGUI extends ImageWindow
{
	
	
	
	public SegmentationGUI(ImagePlus imp) 
	{
		super(imp);
		
		this.setTitle("SIOX Segmentation ");
		// Image panel
		Panel image_panel = new Panel();
		image_panel.add(this.getCanvas());
		
		// Control panel
		ControlJPanel control_panel = new ControlJPanel();						
		
		Panel all = new Panel();
		BoxLayout box = new BoxLayout(all, BoxLayout.X_AXIS);
		all.setLayout(box);
  	    all.add(image_panel);
  	    all.add(control_panel);
  	    
  	    add(all);
		
	    this.pack();
	    this.setVisible(true);
	    	   
	}
}
