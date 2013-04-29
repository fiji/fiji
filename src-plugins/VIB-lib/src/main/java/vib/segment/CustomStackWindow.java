package vib.segment;

import java.awt.*;
import java.awt.event.*;

import vib.BinaryInterpolator;

import ij.IJ;
import ij.measure.Calibration;
import ij.gui.StackWindow;
import ij.gui.Roi;
import ij.gui.ImageLayout;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.plugin.filter.ThresholdToSelection;

public class CustomStackWindow extends StackWindow
				 implements AdjustmentListener, 
						KeyListener, 
						ActionListener, 
						MouseMotionListener,
						MouseWheelListener {
	
	private Roi[] savedRois;
	private int oldSlice;
	private boolean roisLocked;

	/* Listener for the ok button, to get informed when 
	 * labelling is finished */
	private ActionListener al;

	private Sidebar sidebar;
	private CustomCanvas cc;
	private Button ok;

	

	public CustomStackWindow(ImagePlus imp) {
		super(imp, new CustomCanvas(imp));
		this.cc = (CustomCanvas)getCanvas();
		
		savedRois = new Roi[imp.getStack().getSize() + 1];
		if (sliceSelector == null)
			sliceSelector = new Scrollbar(Scrollbar.HORIZONTAL,
				1, 1, 1, 2);
		oldSlice = sliceSelector.getValue();
		roisLocked = false;
		sliceSelector.addAdjustmentListener(this);
		
		// Remove ij from the key listeners to avoid zooming 
		// when pressing + or -
		cc.removeKeyListener(ij);
		cc.addKeyListener(this);
		cc.addMouseMotionListener(this);
		
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		setBackground(Color.LIGHT_GRAY);
		remove(sliceSelector);
		remove(cc);
		sidebar = new Sidebar(cc, this);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridx = c.gridy = 0;
		c.weightx = 0; c.weighty = 0.5;
		c.gridheight = 2;
		gridbag.setConstraints(sidebar, c);
		add(sidebar, c);

		c.gridx = 1; c.gridy = 1;
		c.weightx = 1; c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(sliceSelector, c);
		add(sliceSelector, c);

		Container slideAndImage = new Container();
		slideAndImage.setLayout(new ImageLayout(cc));
		slideAndImage.add(cc);
		
		c.gridx = 1; c.gridy = 1;
		c.weightx = 1; c.weighty = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		gridbag.setConstraints(slideAndImage, c);
		add(slideAndImage, c); 

		Panel buttonPanel = new Panel(new FlowLayout());
		ok = new Button("Ok");
		ok.addActionListener(this);
		buttonPanel.add(ok);
		c.gridx = 0; c.gridy = 2;
		c.weightx = 0.5; c.weighty = 0;
		c.gridwidth = 2;
		gridbag.setConstraints(buttonPanel, c);
		add(buttonPanel, c);

		pack();
		cc.requestFocus();
	}

	public void cleanUp() {
		roisLocked = false;
		savedRois = null;
		al = null;
		sidebar.getMaterials().labels = null;
		sidebar = null;
		ok = null;
		cc.releaseImage();
		cc = null;
		if (imp != null) {
			imp.close();
			imp = null;
		}
	}

	public ImagePlus getLabels() {
		return cc.getLabels();
	}

	public void setLabels(ImagePlus labels) {
		sidebar.setLabelImage(labels);
	}

	public void addActionListener(ActionListener al) {
		this.al = al;
	}
	
	public Sidebar getSidebar() {
		return sidebar;
	}

	public CustomCanvas getCustomCanvas() {
		return cc;
	}

	public Dimension getMinimumSize() {
		return getSize();
	}

	public void processPlusButton(){
		if(roisLocked)
			return;
		int currentSlice = cc.getImage().getCurrentSlice();
		Roi roi = cc.getImage().getRoi();
		assignSliceTo(currentSlice,roi,sidebar.currentMaterialID());	
		cc.getImage().killRoi();
		if(sidebar.is3d()){
			for(int i=0;i<savedRois.length;i++){
				roi = savedRois[i];
				if(roi != null){
					assignSliceTo(i,roi,sidebar.currentMaterialID());
					savedRois[i] = null;
				}
			}
		}
		cc.getImage().setSlice(currentSlice);
		cc.getLabels().setSlice(currentSlice);
		cc.getImage().updateAndDraw();
		cc.getLabels().updateAndDraw();
		cc.requestFocus();
	}
	
	public void processMinusButton(){
		if(roisLocked)
			return;
		int currentSlice = cc.getImage().getCurrentSlice();
		Roi roi = cc.getImage().getRoi();
		releaseSliceFrom(currentSlice, roi, sidebar.currentMaterialID());
		cc.getImage().killRoi();
		if(sidebar.is3d()){
			for(int i=0;i<savedRois.length;i++){
				roi = savedRois[i];
				if(roi != null){
					releaseSliceFrom(i,roi,sidebar.currentMaterialID());
					savedRois[i] = null;
				}
			}
		}
		cc.getImage().setSlice(currentSlice);
		cc.getLabels().setSlice(currentSlice);
		cc.getImage().updateAndDraw();
		cc.getLabels().updateAndDraw();
		cc.requestFocus();
	}

	public void processInterpolateButton() {
		if(roisLocked)
			return;
		updateRois();
		roisLocked = true;
		new Thread(new Runnable() {
			public void run() {
				setCursor(Cursor.WAIT_CURSOR);
				new BinaryInterpolator().run(cc.getImage(), savedRois);
				// Also undo any unvoluntary changes which happened
				// during the interpolation
				transferRois(savedRois, true);
				roisLocked = false;
				setCursor(Cursor.DEFAULT_CURSOR);
			}
		}).start();
		cc.requestFocus();
	}

	public void processThresholdButton() {
		new Thresholder(this).run();
	}

	public void processCloseButton() {
		// Convert to mask
		ImagePlus image = cc.getImage();
		ImageProcessor ip = image.getProcessor();
		ImageProcessor newip = new ByteProcessor(
						ip.getWidth(), ip.getHeight());
		newip.setBackgroundValue(0);
		newip.setRoi(image.getRoi());
		newip.setValue(255);
		newip.fill(newip.getMask());

		// open
		newip.dilate();
		newip.erode();
		
		// convert back to selection
		newip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
		ImagePlus tmp = new ImagePlus(" ", newip);
		ThresholdToSelection ts = new ThresholdToSelection();
		ts.setup("", tmp);
		ts.run(newip);
		newip.resetThreshold();
		image.setRoi(tmp.getRoi());
	}
	
	public void processOpenButton() {
		// Convert to mask
		ImagePlus image = cc.getImage();
		ImageProcessor ip = image.getProcessor();
		ImageProcessor newip = new ByteProcessor(
						ip.getWidth(), ip.getHeight());
		newip.setBackgroundValue(0);
		newip.setRoi(image.getRoi());
		newip.setValue(255);
		newip.fill(newip.getMask());

		// open
		newip.erode();
		newip.dilate();
		
		// convert back to selection
		newip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
		ImagePlus tmp = new ImagePlus(" ", newip);
		ThresholdToSelection ts = new ThresholdToSelection();
		ts.setup("", tmp);
		ts.run(newip);
		newip.resetThreshold();
		image.setRoi(tmp.getRoi());
	}
	
	public void assignSliceTo(int slice, Roi roi, int materialID){
		ImagePlus grey = cc.getImage();
		ImagePlus labels = cc.getLabels();
		if (grey == null || labels == null)
			return;			
		if (roi == null)
			return;
		int w = labels.getWidth(), h = labels.getHeight();
		ImageProcessor labP = labels.getStack().getProcessor(slice);
		labP.setRoi(roi);
		Rectangle bounds = roi.getBounds();
		int x1 = bounds.x > 0 ? bounds.x : 0;
		int y1 = bounds.y > 0 ? bounds.y : 0;
		int x2 = bounds.x + bounds.width  <= w ? bounds.x + bounds.width  : w;
		int y2 = bounds.y + bounds.height <= h ? bounds.y + bounds.height : h;

		ImageProcessor maskP = roi.getMask();
		if(maskP == null) {
			// This case is fast anyways
			for(int i = x1; i < x2; i++){
				for(int j = y1; j < y2; j++) {
					if(roi.contains(i,j)) {
						int oldID = labP.get(i, j);
						if(!sidebar.getMaterials().isLocked(oldID))
							labP.set(i,j,materialID);
					}
				}
			}
		} else {
			int maskX = (int)Math.round(roi.getBounds().getX());
			int maskY = (int)Math.round(roi.getBounds().getY());
			for(int i = x1; i < x2; i++){
				for(int j = y1; j < y2; j++) {
					if(maskP.get(i - maskX, j - maskY) == 255) {
						int oldID = labP.get(i, j);
						if(!sidebar.getMaterials().isLocked(oldID))
							labP.set(i,j,materialID);
					}
				}
			}
		}
		cc.updateSlice(slice);
	}

	public void releaseSliceFrom(int slice, Roi roi, int materialID){
		ImagePlus grey = cc.getImage();
		ImagePlus labels = cc.getLabels();
		if (grey == null || labels == null)
			return;			
		if (roi == null)
			return;
		if (sidebar.getMaterials().isLocked(materialID))
			return;
		int w = labels.getWidth(), h = labels.getHeight();
		ImageProcessor labP = labels.getStack().getProcessor(slice);
		labP.setRoi(roi);
		Rectangle bounds = roi.getBounds();

		int x1 = bounds.x > 0 ? bounds.x : 0;
		int y1 = bounds.y > 0 ? bounds.y : 0;
		int x2 = bounds.x + bounds.width  <= w ? bounds.x + bounds.width  : w;
		int y2 = bounds.y + bounds.height <= h ? bounds.y + bounds.height : h;

		int defaultMaterialID = sidebar.getMaterials().getDefaultMaterialID();
		ImageProcessor maskP = roi.getMask();
		if(maskP == null) {
			// This case is fast anyways
			for(int i = x1; i < x2; i++){
				for(int j = y1; j < y2; j++) {
					if(roi.contains(i, j) && labP.get(i, j)==materialID){
						labP.set(i, j, defaultMaterialID);
					}
				}
			}
		} else {
			int maskX = (int)Math.round(roi.getBounds().getX());
			int maskY = (int)Math.round(roi.getBounds().getY());
			for(int i = x1; i < x2; i++){
				for(int j = y1; j < y2; j++) {
					if(maskP.get(i - maskX, j - maskY) == 255 &&
					   labP.get(i, j)==materialID){
						labP.set(i, j, defaultMaterialID);
					}
				}
			}
		}
		cc.updateSlice(slice);
	}

	public boolean areAllRoisEmpty() {
		if (cc.getImage().getRoi() != null)
			return false;
		for (int i = 0; i < savedRois.length; i++)
			if (i != oldSlice && savedRois[i] != null)
				return false;
		return true;
	}

	public Roi getRoi(int slice) {
		if (slice == oldSlice)
			return cc.getImage().getRoi();
		return savedRois[slice];
	}

	public void transferRois(Roi[] rois){
		transferRois(rois, false);
	}

	public void transferRois(Roi[] rois, boolean overrideLocking){
		for(int i = 0; i < rois.length; ++i) {
			setRoi(i, rois[i], overrideLocking);
		}
	}

	public void setRoi(int slice, Roi roi) {
		setRoi(slice, roi, false);
	}

	public void setRoi(int slice, Roi roi, boolean overrideLocking) {
		if(roisLocked && !overrideLocking)
			return;
		if (slice != oldSlice)
			savedRois[slice] = roi;
		else if (roi == null)
			cc.getImage().killRoi();
		else
			cc.getImage().setRoi(roi);
	}

	public void setCurrentSlice(int slice) {
		cc.getImage().setSlice(slice + 1);
		updateRois(slice);
	}

	/*
	 * MouseMotionListener interface
	 */
	public void mouseDragged(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {

		int x = cc.offScreenX(e.getX());
		int y = cc.offScreenY(e.getY());

		double posX, posY, posZ;
		int voxelValue;
		int materialID;
		String materialName;

		if(x<imp.getWidth() && y<imp.getHeight()) {
			Calibration cal = imp.getCalibration();
			posX = cal.getX(x);
			posX = Double.valueOf(IJ.d2s(posX)).doubleValue();

			posY = cal.getY(y);
			posY = Double.valueOf(IJ.d2s(posY)).doubleValue();
			int z = imp.getCurrentSlice()-1;
			posZ = cal.getZ(z);
			posZ = Double.valueOf(IJ.d2s(posZ)).doubleValue();

			voxelValue = imp.getProcessor().get(x, y);
			
			materialID = cc.getLabels().getStack().getProcessor(z+1).get(x,y);
			materialName = sidebar.getMaterials()
								.params.getMaterialName(materialID);
			
			IJ.showStatus("x=" + posX + ", y=" + posY + ", z=" + posZ + 
					", value=" + voxelValue + ", material=" + materialName);
		}
	}	
	
	/*
	 * overridden in order to fix the problem of drawing a rectangle 
	 * close to the ImageCanvas
	 */
	public void paint(Graphics g) {
		//super.paint(g);
		drawInfo(g);
	}
	
	/*
	 * ActionListener interface
	 */
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("zoomin")) {
			cc.zoomIn(cc.getWidth()/2, cc.getHeight()/2);
			cc.requestFocus();
		} else if (command.equals("zoomout")) {
			cc.zoomOut(cc.getWidth()/2, cc.getHeight()/2);
			cc.requestFocus();
		} else if (command.equals("plus")) {
			processPlusButton();
		} else if (command.equals("minus")) {
			processMinusButton();
		} else if (command.equals("interpolate")) {
			processInterpolateButton();
		} else if (command.equals("threshold")) {
			processThresholdButton();
		} else if (command.equals("open")) {
			processOpenButton();
		} else if (command.equals("close")) {
			processCloseButton();
		} else if (command.equals("Ok")) {
			// call the action listener before destroying the window
			if(al != null)
				al.actionPerformed(e);
			if(getImagePlus() != null) {
				new StackWindow(getImagePlus());
			}
		}
	}

	/*
	 * AdjustmentListener interface
	 */
	public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		super.adjustmentValueChanged(e);
		updateRois();
	}

	public synchronized void updateRois() {
		updateRois(sliceSelector.getValue());
	}

	public synchronized void updateRois(int newSlice) {
		if(roisLocked)
			return;
		savedRois[oldSlice] = imp.getRoi();
		oldSlice = newSlice;
		if (savedRois[oldSlice] == null)
			imp.killRoi();
		else
			imp.setRoi(savedRois[oldSlice]);
		repaint();
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		super.mouseWheelMoved(e);
		updateRois();
	}
	
	/*
	 * KeyListener interface
	 */
	public void keyTyped(KeyEvent e) {}
	
	public void keyPressed(KeyEvent e) {}

	public void keyReleased(KeyEvent e) {
		int c = e.getKeyCode();
        char ch = e.getKeyChar();
		if(c == KeyEvent.VK_DOWN || c == KeyEvent.VK_RIGHT || ch == '>'){
			imp.setSlice(oldSlice + 1);
			adjustmentValueChanged(new AdjustmentEvent(
						sliceSelector,
						AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						AdjustmentEvent.BLOCK_INCREMENT,
						oldSlice+1));
		} else if (c == KeyEvent.VK_UP || c == KeyEvent.VK_LEFT || ch == '<'){
			imp.setSlice(oldSlice - 1);
			adjustmentValueChanged(new AdjustmentEvent(
						sliceSelector,
						AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						AdjustmentEvent.BLOCK_DECREMENT,
						oldSlice-1));
		} else if (c == KeyEvent.VK_PAGE_UP){
			imp.setSlice(oldSlice - 5);
			adjustmentValueChanged(new AdjustmentEvent(
						sliceSelector,
						AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						AdjustmentEvent.BLOCK_DECREMENT,
						oldSlice-5));
		} else if (c == KeyEvent.VK_PAGE_DOWN){
			imp.setSlice(oldSlice + 5);
			adjustmentValueChanged(new AdjustmentEvent(
						sliceSelector,
						AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						AdjustmentEvent.BLOCK_DECREMENT,
						oldSlice+5));
		} else if (ch == '+' || ch == '='){
			processPlusButton();
		} else if (ch == '-'){
			processMinusButton();
		} else if (ch == 'i') {
			processInterpolateButton();
		} else if (ch == 't') {
			processThresholdButton();
		} else if (ch == 'o') {
			processOpenButton();
		} else if (ch == 'c') {
			processCloseButton();
		}
	}
}
