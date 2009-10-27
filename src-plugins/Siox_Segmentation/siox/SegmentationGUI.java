/**
 * Siox_Segmentation plug-in for ImageJ and Fiji.
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

import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Roi;

import javax.swing.BoxLayout;

//-----------------------------------------------------------------
/**
 * SIOX segmentation Graphical User Interface
 * 
 * @author Ignacio Arganda-Carreras (ignacio.arganda at gmail.com)
 *
 */
public class SegmentationGUI extends ImageWindow implements ActionListener
{

	/** Generated serial version UID */
	private static final long serialVersionUID = -326288432966353440L;
	protected Roi foreground, background;
	protected ControlJPanel control_panel;
	
	//-----------------------------------------------------------------
	/**
	 * Create Graphical User Interface for SIOX segmentation.
	 * 
	 * @param imp input image
	 */
	public SegmentationGUI(ImagePlus imp) 
	{
		super(imp);
		
		// Take snapshot of initial pixels
		imp.getProcessor().snapshot();
		
		this.setTitle("SIOX Segmentation ");
		// Image panel
		Panel image_panel = new Panel();
		image_panel.add(this.getCanvas());
		
		// Control panel
		control_panel = new ControlJPanel(imp);
		control_panel.bgJRadioButton.addActionListener(this);
		control_panel.fgJRadioButton.addActionListener(this);
		
		Panel all = new Panel();
		BoxLayout box = new BoxLayout(all, BoxLayout.X_AXIS);
		all.setLayout(box);
  	    all.add(image_panel);
  	    all.add(control_panel);
  	    
  	    add(all);
		
	    this.pack();
	    this.setVisible(true);
	    	   
	}

	Roi setNewRoi(Roi newRoi, boolean isBackground) 
	{
		Roi oldRoi = imp.getRoi();
		if (newRoi == null)
			imp.killRoi();
		else
			imp.setRoi(newRoi);
		// Paint old ROI
		if(oldRoi != null)
		{
			imp.getProcessor().reset();
			final int[] pix = (int[]) imp.getProcessor().getPixels();		
			for(int l = 0, i = 0; i < imp.getHeight(); i++)
				for(int j = 0; j < imp.getWidth(); j++, l++)
					if(oldRoi.contains(j, i))
					{
						pix[l] = 0;
					}
		}
		imp.changes = true;
		imp.updateAndDraw();
		return oldRoi;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() == control_panel.bgJRadioButton) {
			foreground = setNewRoi(background, true);
		}
		else if (e.getSource() == control_panel.fgJRadioButton) {
			background = setNewRoi(foreground, false);
		}
		else if (e.getSource() == control_panel.segmentateJButton) {
			//segmentate();
		}
	}
	

	
}// end class SegmentationGUI
