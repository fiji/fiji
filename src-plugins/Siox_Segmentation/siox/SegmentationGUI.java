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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Panel;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Area;

import fiji.util.gui.OverlayedImageCanvas;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.ShapeRoiHelper;
import ij.process.Blitter;
import ij.process.ByteProcessor;
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
	
	/** Confidence matrix */
	FloatProcessor confMatrix = null;
	
	private SioxSegmentator siox;
	private JRadioButton lastButton;
	protected Roi foregroundRoi, backgroundRoi;	
	protected RoiOverlay roiOverlay;
	protected Roi addRoi, subRoi;
	protected ImageOverlay resultOverlay;
	protected ControlJPanel controlPanel;
	ImageProcessor ip;
	ImageProcessor originalImage;
	
	final Composite transparency050 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f );	
	final Composite transparency075 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f );
	final Composite transparency100 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.00f );
	final Composite clear100 = AlphaComposite.getInstance(AlphaComposite.CLEAR, 1.00f );
	
	
	//-----------------------------------------------------------------
	/**
	 * Create Graphical User Interface for SIOX segmentation.
	 * 
	 * @param imp input image
	 */
	public SegmentationGUI(ImagePlus imp) 
	{
		super(imp, new OverlayedImageCanvas(imp) );
				 
		roiOverlay = new RoiOverlay();		
		resultOverlay = new ImageOverlay();
				
		((OverlayedImageCanvas)ic).addOverlay(roiOverlay);
		((OverlayedImageCanvas)ic).addOverlay(resultOverlay);	
		
		roiOverlay.setComposite( transparency050 );		
		resultOverlay.setComposite( transparency075);
		
		
		// Take snapshot of initial pixels
		ip = imp.getProcessor();
			
		this.setTitle("SIOX Segmentation ");
		// Image panel
		Panel image_panel = new Panel();
		image_panel.add(ic);
		
		// Control panel
		controlPanel = new ControlJPanel(imp);
		controlPanel.bgJRadioButton.addActionListener(this);
		lastButton = controlPanel.fgJRadioButton;
		controlPanel.fgJRadioButton.addActionListener(this);
		controlPanel.segmentJButton.addActionListener(this);
		controlPanel.resetJButton.addActionListener(this);
		controlPanel.createMaskJButton.addActionListener(this);
		controlPanel.addJRadioButton.addActionListener(this);
		controlPanel.subJRadioButton.addActionListener(this);
		controlPanel.refineJButton.addActionListener(this);
		
		Panel all = new Panel();
		BoxLayout box = new BoxLayout(all, BoxLayout.X_AXIS);
		all.setLayout(box);
  	    all.add(image_panel);
  	    all.add(controlPanel);
  	    
  	    add(all);
		
	    this.pack();
	    this.setVisible(true);
	    	   
	}


	
	@Override
	public synchronized void actionPerformed(ActionEvent e) 
	{
		if (e.getSource() == controlPanel.bgJRadioButton && lastButton != controlPanel.bgJRadioButton) {
			roiOverlay.setColor(Color.GREEN);			
			foregroundRoi = setNewRoi(backgroundRoi, roiOverlay);			
			lastButton = controlPanel.bgJRadioButton;
		}
		else if (e.getSource() == controlPanel.fgJRadioButton && lastButton != controlPanel.fgJRadioButton) {
			roiOverlay.setColor(Color.RED);
			backgroundRoi = setNewRoi(foregroundRoi, roiOverlay);		
			lastButton = controlPanel.fgJRadioButton;
		}
		else if (e.getSource() == controlPanel.segmentJButton) {
			segment();
		}
		else if (e.getSource() == controlPanel.addJRadioButton) {
			controlPanel.subJRadioButton.setSelected(false);
			controlPanel.updateComponentEnabling();
			roiOverlay.setColor(Color.RED);
			subRoi = setNewRoi(addRoi, roiOverlay);			
		}
		else if (e.getSource() == controlPanel.subJRadioButton) {
			controlPanel.addJRadioButton.setSelected(false);
			controlPanel.updateComponentEnabling();
			roiOverlay.setColor(Color.GREEN);
			addRoi = setNewRoi(subRoi, roiOverlay);
		}
		else if (e.getSource() == controlPanel.refineJButton) {
			refine();
		}
		else if (e.getSource() == controlPanel.resetJButton) {
			reset();
		}
		else if (e.getSource() == controlPanel.createMaskJButton) {
			createBinaryMask();
		}

	}

	/**
	 * Produce a binary image based on the current confidence matrix
	 */
	private void createBinaryMask() 
	{
		if (null != confMatrix)
		{
			final ByteProcessor result = (ByteProcessor) confMatrix.convertToByte(false);
			result.setMinAndMax(0, 1);
			new ImagePlus("Mask", result).show();
		}
				
	}



	/**
	 * Reset overlays and set plugin to initial status
	 */
	private void reset() 
	{
		roiOverlay.setRoi(null);	
		resultOverlay.setImage(null);
		confMatrix = null;
		
		// Set initial status
		controlPanel.status = foregroundRoi != null ? 
				ControlJPanel.FG_ADDED_STATUS : ControlJPanel.ROI_DEFINED_STATUS;
		controlPanel.updateComponentEnabling();		
		lastButton = controlPanel.fgJRadioButton;
		roiOverlay.setColor(Color.RED);
		roiOverlay.setComposite( transparency050 );
		
		if (controlPanel.bgJRadioButton.isSelected()) {
			imp.setRoi(backgroundRoi);
			roiOverlay.setRoi(foregroundRoi);
		}
		else {
			imp.setRoi(foregroundRoi);
			roiOverlay.setRoi(backgroundRoi);
		}
		
		imp.changes = true;		
		imp.updateAndDraw();		
				
	}



	/**
	 * Paint ROIs in the current image.
	 * 
	 * @param newRoi current ROI
	 * @param isBackground true if the current ROI is background, false otherwise
	 * @return old ROI
	 */
	synchronized Roi setNewRoi(Roi newRoi, final RoiOverlay overlayToPaint) 
	{
		
		Roi oldRoi = imp.getRoi();
		if (newRoi == null)
			imp.killRoi();
		else
			imp.setRoi(newRoi);		
		
		overlayToPaint.setRoi(oldRoi);		
		
		imp.changes = true;
		imp.updateAndDraw();
		return oldRoi;
	}	
	
	/**
	 * Refine segmentation with ADD/SUB regions of interests
	 */
	private synchronized void refine()
	{
		if ( controlPanel.status != ControlJPanel.SEGMENTATED_STATUS )
			return;
		
		if(controlPanel.addJRadioButton.isSelected())
			addRoi = imp.getRoi();
		else
			subRoi = imp.getRoi();
		
		if (null != addRoi )
		{			
			final float alpha = controlPanel.addThreshold.getValue() / 100.0f;
			final Shape shape = ShapeRoiHelper.getShape(new ShapeRoi(addRoi));
			final Area area = new Area(shape);
			siox.subpixelRefine(area, SioxSegmentator.ADD_EDGE, alpha, (float[]) confMatrix.getPixels());
		}
		if (null != subRoi )
		{			
			final float alpha = controlPanel.subThreshold.getValue() / 100.0f;
			final Shape shape = ShapeRoiHelper.getShape(new ShapeRoi(subRoi));
			final Area area = new Area(shape);
			siox.subpixelRefine(area, SioxSegmentator.SUB_EDGE, alpha, (float[]) confMatrix.getPixels());
		}
		
		//new ImagePlus ("Conf matrix ", confMatrix).show();
		
		updateResult();
	}
	
	
	/**
	 * Segment image based on the current foreground and background ROIs
	 */
	private synchronized void segment() 
	{
		if (controlPanel.bgJRadioButton.isSelected())
			backgroundRoi = imp.getRoi();
		else
			foregroundRoi = imp.getRoi();

		if (foregroundRoi == null) {
			IJ.error("Siox Segmentation", "ERROR: no foreground selected!");
			return;
		}
		
	
		// Create confidence matrix and initialize to unknown region of confidence
		confMatrix = new FloatProcessor(imp.getWidth(), imp.getHeight());
		final float[] imgData = (float[])confMatrix.getPixels();
		confMatrix.add( SioxSegmentator.UNKNOWN_REGION_CONFIDENCE );
		
		// Set foreground ROI
		if(foregroundRoi != null)
		{
			confMatrix.setValue(SioxSegmentator.CERTAIN_FOREGROUND_CONFIDENCE);
			confMatrix.fill(foregroundRoi);
		}
		
		// Set background ROI
		if(backgroundRoi != null)
		{
			confMatrix.setValue(SioxSegmentator.CERTAIN_BACKGROUND_CONFIDENCE);
			confMatrix.fill(backgroundRoi);
		}
		else {
			// Workaround: select border pixels which are not foreground as background if no background was specified.
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
		int[] pixels = (int[]) ip.getPixels();
				
		final int smoothes = controlPanel.smoothness.getValue();
				
		siox = new SioxSegmentator(imp.getWidth(), imp.getHeight(), null);
		boolean success = siox.segmentate(pixels, imgData, smoothes, controlPanel.multipart.isSelected()?4:0);
		
		if(!success)		
			IJ.error("Siox Segmentation", "The segmentation failed!");										
		
		updateResult();
		
		// Set status flag to segmented
		controlPanel.status = ControlJPanel.SEGMENTATED_STATUS;
		controlPanel.updateComponentEnabling();
		
		roiOverlay.setComposite( transparency100 );
	}


	/**
	 * Update the result overlay with the current matrix of confidence
	 */
	private void updateResult() {
		imp.killRoi();
		roiOverlay.setRoi(null);		
		
		ImageProcessor cp = confMatrix.convertToRGB();
		cp.multiply(1.0/255.0);
		cp.copyBits(ip, 0, 0, Blitter.MULTIPLY);				
		
		resultOverlay.setImage(cp);
		
		imp.changes = true;
		imp.updateAndDraw();		
	}
	

	
}// end class SegmentationGUI
