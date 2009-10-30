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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import fiji.util.gui.OverlayedImageCanvas;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import javax.swing.BoxLayout;
import javax.swing.JRadioButton;

import org.siox.SioxSegmentator;

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
	
	private SioxSegmentator siox;
	private JRadioButton lastButton;
	protected Roi foreground, background;
	protected RoiOverlay foreground_overlay, background_overlay;
	protected ControlJPanel control_panel;
	ImageProcessor ip;
	ImageProcessor original_image;
	
	
	//-----------------------------------------------------------------
	/**
	 * Create Graphical User Interface for SIOX segmentation.
	 * 
	 * @param imp input image
	 */
	public SegmentationGUI(ImagePlus imp) 
	{
		super(imp);
		
		ic = new OverlayedImageCanvas(imp);
		foreground_overlay = new RoiOverlay();
		background_overlay = new RoiOverlay();
		
		((OverlayedImageCanvas)ic).addOverlay(foreground_overlay);
		((OverlayedImageCanvas)ic).addOverlay(background_overlay);
		
		foreground_overlay.setColor(Color.GREEN);
		background_overlay.setColor(Color.RED);
		
		foreground_overlay.setComposite(AlphaComposite.getInstance(AlphaComposite.XOR));
		background_overlay.setComposite(AlphaComposite.getInstance(AlphaComposite.XOR));
		
		
		
		// Take snapshot of initial pixels
		ip = imp.getProcessor();
		//original_image = ip.duplicate();
		//ip.snapshot();
		
		
		this.setTitle("SIOX Segmentation ");
		// Image panel
		Panel image_panel = new Panel();
		image_panel.add(ic);
		
		// Control panel
		control_panel = new ControlJPanel(imp);
		control_panel.bgJRadioButton.addActionListener(this);
		lastButton = control_panel.fgJRadioButton;
		control_panel.fgJRadioButton.addActionListener(this);
		control_panel.segmentateJButton.addActionListener(this);
		control_panel.resetJButton.addActionListener(this);
		
		Panel all = new Panel();
		BoxLayout box = new BoxLayout(all, BoxLayout.X_AXIS);
		all.setLayout(box);
  	    all.add(image_panel);
  	    all.add(control_panel);
  	    
  	    add(all);
		
	    this.pack();
	    this.setVisible(true);
	    	   
	}


	
	@Override
	public synchronized void actionPerformed(ActionEvent e) 
	{
		if (e.getSource() == control_panel.bgJRadioButton && lastButton != control_panel.bgJRadioButton) {
			foreground = setNewRoi(background, true);
			lastButton = control_panel.bgJRadioButton;
		}
		else if (e.getSource() == control_panel.fgJRadioButton && lastButton != control_panel.fgJRadioButton) {
			background = setNewRoi(foreground, false);
			lastButton = control_panel.fgJRadioButton;
		}
		else if (e.getSource() == control_panel.segmentateJButton) {
			segmentate();
		}
		else if (e.getSource() == control_panel.resetJButton) {
			reset();
		}

	}

	private void reset() {
		//ip.reset();
		//ip.snapshot();
		imp.changes = true;
		imp.updateAndDraw();		
	}



	/**
	 * Paint ROIs in the current image.
	 * @param newRoi
	 * @param isBackground
	 * @return
	 */
	synchronized Roi setNewRoi(Roi newRoi, boolean isBackground) 
	{
		final RoiOverlay overlay_to_paint = isBackground ? background_overlay : foreground_overlay;
		final RoiOverlay overlay_to_clear = isBackground ? foreground_overlay : background_overlay;
		
		Roi oldRoi = imp.getRoi();
		if (newRoi == null)
			imp.killRoi();
		else
			imp.setRoi(newRoi);
		// Paint old ROI
		//ip.reset();
		if(oldRoi != null)
		{		
			overlay_to_paint.setRoi(oldRoi);
			//ColorProcessor cp = new ColorProcessor(imp.getWidth(), imp.getHeight(), (int[]) ip.getPixels());
			//cp.setColor(paintColor);
			//cp.fill(oldRoi);
		}
		
		overlay_to_clear.setRoi(newRoi);
		
		imp.changes = true;
		imp.updateAndDraw();
		return oldRoi;
	}
	
	/**
	 * Segmentate image based on the current foreground and background ROIs
	 */
	private synchronized void segmentate() 
	{

		//ColorProcessor cp2 = new ColorProcessor(imp.getWidth(), imp.getHeight(), (int[])ip.getSnapshotPixels());		new ImagePlus("test", cp2).show();

		if (control_panel.bgJRadioButton.isSelected())
			background = imp.getRoi();
		else
			foreground = imp.getRoi();

		if (foreground == null) {
			IJ.error("Siox Segmentation", "ERROR: no foreground selected!");
			return;
		}
		
		/*
		// Reset		
		ip.reset();
		imp.changes = true;
		imp.updateAndDraw();
		*/
		
		// Create confidence matrix and initialize to unknown region of confidence
		final FloatProcessor confMatrix = new FloatProcessor(imp.getWidth(), imp.getHeight());
		final float[] imgData = (float[])confMatrix.getPixels();
		confMatrix.add( SioxSegmentator.UNKNOWN_REGION_CONFIDENCE );
		
		// Set foreground ROI
		if(foreground != null)
		{
			confMatrix.setValue(SioxSegmentator.CERTAIN_FOREGROUND_CONFIDENCE);
			confMatrix.fill(foreground);
		}
		
		// Set background ROI
		if(background != null)
		{
			confMatrix.setValue(SioxSegmentator.CERTAIN_BACKGROUND_CONFIDENCE);
			confMatrix.fill(background);
		}
		else {
			// crappy: select border pixels which are not foreground as background if no background was specified.
			int w = imp.getWidth(), h = imp.getHeight();
			for (int i = 0; i < w; i++) {
				if (imgData[i] < 0.8f)
					imgData[i] = 0;
				if (imgData[i + w * (h - 1)] < 0.8f)
					imgData[i + w * (h - 1)] = 0;
			}
			for (int i = 0; i < h; i++) {
				if (imgData[w * i] < 0.8f)
					imgData[w * i] = 0;
				if (imgData[w - 1 + w * i] < 0.8f)
					imgData[w - 1 + w * i] = 0;
			}
		}
		
		// Call SIOX segmentation method
		int[] pixels = (int[]) ip.getSnapshotPixels();
		if (pixels != null)
			pixels = Arrays.copyOf(pixels, pixels.length);
		else
			pixels =(int[]) original_image.getPixels();
		
		//new ImagePlus("confMat", confMatrix).show();
		
		final int smoothes = control_panel.smoothness.getValue();
				
		siox = new SioxSegmentator(imp.getWidth(), imp.getHeight(), null);
		boolean success = siox.segmentate(pixels, imgData, smoothes, control_panel.multipart.isSelected()?4:0);
		//IJ.log(" smoothness = " +  control_panel.smoothness.getValue() + " " + control_panel.multipart.isSelected());
		
		if(!success)		
			IJ.error("Siox Segmentation", "The segmentation failed!");
		
										
		//ip.reset();
		
		final ImagePlus result = new ImagePlus("result", confMatrix.convertToRGB());
		//result.show();
		IJ.run(result, "Divide...", "value=255.000");
		//IJ.run("Image Calculator...", "image1="+imp.getTitle()+" operation=Multiply image2="+result.getTitle());
		ip.copyBits(result.getProcessor(), 0, 0, Blitter.MULTIPLY);
		
		imp.changes = true;
		imp.updateAndDraw();
									
		
		//result.show();
		
		//ip.snapshot();
	
		//new ImagePlus("test", new ColorProcessor(imp.getWidth(), imp.getHeight(), pixels)).show();
		//new ImagePlus("test", new ColorProcessor(imp.getWidth(), imp.getHeight(), (int[]) ip.getSnapshotPixels())).show();

	}
	

	
}// end class SegmentationGUI
