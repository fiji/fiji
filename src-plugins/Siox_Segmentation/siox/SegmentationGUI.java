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
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import fiji.util.gui.OverlayedImageCanvas;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.ShapeRoiHelper;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
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
 * @author Ignacio Arganda-Carreras (iarganda at mit.edu)
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
	
	final Composite transparency050 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f );	
	final Composite transparency075 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f );
	final Composite transparency100 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.00f );		
	
	//-----------------------------------------------------------------
	/**
	 * Create Graphical User Interface for SIOX segmentation.
	 * 
	 * @param imp input image
	 */
	public SegmentationGUI(ImagePlus imp) 
	{
		super(imp, new OverlayedImageCanvas(imp) );					
		
		while(ic.getWidth() > 800 && ic.getHeight() > 600)
			IJ.run(imp, "Out","");
		
		roiOverlay = new RoiOverlay();		
		resultOverlay = new ImageOverlay();
				
		((OverlayedImageCanvas)ic).addOverlay(roiOverlay);
		((OverlayedImageCanvas)ic).addOverlay(resultOverlay);	
		
		roiOverlay.setComposite( transparency050 );		
		resultOverlay.setComposite( transparency075);
		
		
		// Take snapshot of initial pixels
		ip = imp.getProcessor();
			
		this.setTitle("SIOX Segmentation");
		// Image panel
		Panel imagePanel = new Panel();		
		imagePanel.add(ic);

		
		// Control panel
		controlPanel = new ControlJPanel(imp);
		controlPanel.bgJRadioButton.addActionListener(this);
		lastButton = controlPanel.fgJRadioButton;
		controlPanel.fgJRadioButton.addActionListener(this);
		controlPanel.segmentJButton.addActionListener(this);
		controlPanel.resetJButton.addActionListener(this);
		controlPanel.createMaskJButton.addActionListener(this);
		controlPanel.saveSegmentatorJButton.addActionListener(this);
		controlPanel.addJRadioButton.addActionListener(this);
		controlPanel.subJRadioButton.addActionListener(this);
		controlPanel.refineJButton.addActionListener(this);		
		
		Panel all = new Panel();
		BoxLayout box = new BoxLayout(all, BoxLayout.X_AXIS);
		all.setLayout(box);
  	    all.add(imagePanel);
  	    all.add(controlPanel);
  	    
  	    add(all);  	      	      	   
		
	    this.pack();	 	    
	    this.setVisible(true);    	   
	}
    

	
	//@Override
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
		else if (e.getSource() == controlPanel.addJRadioButton && lastButton != controlPanel.addJRadioButton) {			
			roiOverlay.setColor(Color.RED);
			subRoi = setNewRoi(addRoi, roiOverlay);
			lastButton = controlPanel.addJRadioButton;
		}
		else if (e.getSource() == controlPanel.subJRadioButton && lastButton != controlPanel.subJRadioButton) {			
			roiOverlay.setColor(Color.GREEN);
			addRoi = setNewRoi(subRoi, roiOverlay);
			lastButton = controlPanel.subJRadioButton;
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
		else if (e.getSource() == controlPanel.saveSegmentatorJButton) {
			saveSegmentator();
		}
			
	}

	/**
	 * Save current segmentator into a file
	 */
	private void saveSegmentator() 
	{
		if ( controlPanel.status != ControlJPanel.SEGMENTED_STATUS )
		{
			IJ.error("No segmentator found!");
			return;
		}
		
		String currentDirectory = (OpenDialog.getLastDirectory() == null) ? 
				 OpenDialog.getDefaultDirectory() : OpenDialog.getLastDirectory();
				 
		if(null == currentDirectory)
			currentDirectory = ".";
		
		SaveDialog sd = new SaveDialog("Save segmentator", currentDirectory, 
				"segmentator-"+this.imp.getTitle(), ".siox");
		
		String filename = sd.getFileName();
		
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try
		{
			fos = new FileOutputStream(sd.getDirectory() + filename);
			out = new ObjectOutputStream(fos);
			out.writeObject(
					new SegmentationInfo(siox.getBgSignature(), 
							siox.getFgSignature(), controlPanel.smoothness.getValue(), 
							controlPanel.multipart.isSelected()?4:0));
			out.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
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
			result.multiply(255);
			// Set background color based on the Process > Binary > Options 
			if(!Prefs.blackBackground)
				result.invert();
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
		controlPanel.status = ControlJPanel.FG_ADDED_STATUS;			
		lastButton = controlPanel.fgJRadioButton.isSelected() ? 
				controlPanel.fgJRadioButton : controlPanel.bgJRadioButton;
		 
		roiOverlay.setColor( controlPanel.fgJRadioButton.isSelected() ? Color.RED : Color.GREEN);
		 
		roiOverlay.setComposite( transparency050 );
		
		if (controlPanel.bgJRadioButton.isSelected()) {
			imp.setRoi(backgroundRoi);
			roiOverlay.setRoi(foregroundRoi);
		}
		else {
			imp.setRoi(foregroundRoi);
			roiOverlay.setRoi(backgroundRoi);
		}
		
		controlPanel.updateComponentEnabling();
		
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
		if ( controlPanel.status != ControlJPanel.SEGMENTED_STATUS )
			return;
		
		if(controlPanel.addJRadioButton.isSelected())
			addRoi = imp.getRoi();
		else
			subRoi = imp.getRoi();
		
		if (null != addRoi )
		{			
			final float alpha = controlPanel.addThreshold.getValue() / 100.0f;
			final Shape shape = ShapeRoiHelper.getShape(new ShapeRoi(addRoi));
			final AffineTransform trans = new AffineTransform();
			trans.translate(addRoi.getBounds().getX(), addRoi.getBounds().getY());
			final Area area = new Area(shape);
			area.transform(trans);
			siox.subpixelRefine(area, SioxSegmentator.ADD_EDGE, alpha, (float[]) confMatrix.getPixels());
		}
		if (null != subRoi )
		{			
			final float alpha = controlPanel.subThreshold.getValue() / 100.0f;
			final Shape shape = ShapeRoiHelper.getShape(new ShapeRoi(subRoi));
			final AffineTransform trans = new AffineTransform();
			trans.translate(subRoi.getBounds().getX(), subRoi.getBounds().getY());
			final Area area = new Area(shape);
			area.transform(trans);
			siox.subpixelRefine(area, SioxSegmentator.SUB_EDGE, alpha, (float[]) confMatrix.getPixels());
		}
				
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
		final float[] confMatrixArray = (float[])confMatrix.getPixels();
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
				if (confMatrixArray[i] < 0.8f)
					confMatrixArray[i] = 0;
				if (confMatrixArray[i + w * (h - 1)] < 0.8f)
					confMatrixArray[i + w * (h - 1)] = 0;
			}
			for (int i = 0; i < h; i++) {
				if (confMatrixArray[w * i] < 0.8f)
					confMatrixArray[w * i] = 0;
				if (confMatrixArray[w - 1 + w * i] < 0.8f)
					confMatrixArray[w - 1 + w * i] = 0;
			}
		}
		
		// Call SIOX segmentation method
		int[] pixels = (int[]) ip.getPixels();
				
		final int smoothes = controlPanel.smoothness.getValue();
				
		siox = new SioxSegmentator(imp.getWidth(), imp.getHeight(), null);
		
		boolean multipleObjects = controlPanel.multipart.isSelected();
		
		if(!multipleObjects)
		{
			// Check if multiple foreground ROIs.
			if(foregroundRoi instanceof ShapeRoi)
			{
				Roi[] rois = ((ShapeRoi) foregroundRoi).getRois();
				if (rois.length > 1)
				{
					// Multiple foreground ROIs involve multiple objects
					multipleObjects = true;
					controlPanel.multipart.setSelected(true);
				}
			}
		}
		
		boolean success = false;
		
		try{
			success = siox.segmentate(pixels, confMatrixArray, smoothes, multipleObjects ? 4:0);
		}catch(IllegalStateException ex){
			IJ.error("Siox Segmentation", "ERROR: foreground signature does not exist.");
		}
		
		if(!success)		
			IJ.error("Siox Segmentation", "The segmentation failed!");										
		
		updateResult();
		
		// Set status flag to segmented
		controlPanel.status = ControlJPanel.SEGMENTED_STATUS;
		controlPanel.updateComponentEnabling();
		
		roiOverlay.setComposite( transparency100 );
		
		// Set up next panel components
		controlPanel.subJRadioButton.setSelected(true);
		controlPanel.addJRadioButton.setSelected(false);
		lastButton = controlPanel.subJRadioButton; 
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
	
	/**
	 * Overwrite windowClosing to display the input image after closing GUI
	 */
	public void windowClosing(WindowEvent e) 
	{		
		final ImagePlus img = new ImagePlus(super.imp.getTitle(), super.imp.getProcessor().duplicate());
		img.changes = super.imp.changes;
		img.show();
		super.imp.changes = false;
		super.windowClosing(e);		
		
	}
	
}// end class SegmentationGUI
