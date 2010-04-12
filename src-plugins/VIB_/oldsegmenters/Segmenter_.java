/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package oldsegmenters;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.*;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import events.RoiEvent;
import events.RoiListener;
import events.RoiWatcher;
import events.SliceEvent;
import events.SliceListener;
import events.SliceWatcher;
import adt.Connectivity2D;
import adt.Points;

import amira.AmiraParameters;
import amira.AmiraMeshEncoder;
import amira.AmiraMeshDecoder;
import vib.SegmentationViewerCanvas;

import gui.GuiBuilder;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.plugin.MacroInstaller;

class ChoicesDialog extends Dialog implements ActionListener {

	private Button[] buttons;
	private boolean[] chosen;
	
	public ChoicesDialog(Frame parent, String title, String msg, String[] options) {

		super(parent, title, true);

		setLayout(new BorderLayout());
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
		MultiLineLabel message = new MultiLineLabel(msg);
		message.setFont(new Font("Dialog", Font.BOLD, 12));
		panel.add(message);
		add("North", panel);

		panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 8));

		buttons = new Button[options.length];
		chosen = new boolean[options.length];

		for (int i=0;i<options.length;++i) {
			buttons[i]=new Button(options[i]);
			buttons[i].addActionListener(this);
			panel.add(buttons[i]);
		}
		add("South", panel);
		if (ij.IJ.isMacintosh())
			setResizable(false);
		pack();
		GUI.center(this);
		show();
	}

	public void actionPerformed(ActionEvent e) {
		Object source=e.getSource();
		for (int i=0;i<buttons.length;++i)
			if(source==buttons[i])
				chosen[i]=true;
		setVisible(false);
		dispose();
	}

	public boolean optionChosen(int optionIndex) {
		return chosen[optionIndex];
	}
}


public class Segmenter_ extends JFrame implements PlugIn {
	
	private static final String SAVE_LABELS = "save labels";
	private static final String LABEL_CURRENT = "label current";
	
	private static final String THRESHOLD = "threshold";
	private static final String THRESHOLD_UNDO = "undo";
	
	private static final String INTERPOLATOR = "interpolate";
	
	private static final String DILATE = "dilate";
	private static final String ERODE = "erode";
	private static final String OPEN = "open";
	private static final String CLOSE = "close";
	private static final String CLEAN = "clean";
	
	JList labelList;
	DefaultListModel labelListModel;
	
	JSpinner minThreshold;
	JSpinner maxThreshold;
	
	JLabel markingLabel;
	
	public Segmenter_() {
		super("VIB Manual Segmenter");
		
		Controllor controllor = new Controllor();
		
		MacroInstaller installer = new ij.plugin.MacroInstaller();
		installer.install(LabelBrush_.MACRO_CMD);
		
		ImagePlus.addImageListener(controllor);
		
		
		//the stup method may be called AFTER images have been opened,
		//we need listeners attached to all image windows though, to track focus
		for (Frame frame : ImageWindow.getFrames()) {
			if (frame instanceof ImageWindow) {
				controllor.imageOpened(((ImageWindow) frame).getImagePlus());
			}
		}
		
		
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		labelList = addLabelList(this);
		labelListModel = (DefaultListModel) labelList.getModel();
		
		labelList.addListSelectionListener(controllor);       
		
		markingLabel = new JLabel("current image: <no current image>");
		Box b = new Box(BoxLayout.X_AXIS);
		b.add(markingLabel);
		this.add(b);
		
		GuiBuilder.add2Command(this, LABEL_CURRENT, LABEL_CURRENT, SAVE_LABELS, SAVE_LABELS, controllor);

		minThreshold = GuiBuilder.addLabeledNumericSpinner(this, "min", 45, 0, 255, controllor);
		maxThreshold = GuiBuilder.addLabeledNumericSpinner(this, "max", 255, 0, 255, controllor);

		GuiBuilder.add2Command(this, THRESHOLD, THRESHOLD, THRESHOLD_UNDO, THRESHOLD_UNDO, controllor);

		GuiBuilder.addCommand(this, INTERPOLATOR, INTERPOLATOR, controllor);

		GuiBuilder.add2Command(this, DILATE, DILATE, ERODE, ERODE, controllor);
		GuiBuilder.add2Command(this, OPEN,OPEN,CLOSE,CLOSE, controllor);
		GuiBuilder.addCommand(this, CLEAN, CLEAN, controllor);


		pack();
	}

	public void run(String arg0) {
		setVisible(!isVisible());
	}


	public void clearLabelsList() {
		labelListModel.clear();
	}


	public void populateLabelList(AmiraParameters params) {
		clearLabelsList();
		if (params == null) return;
		for (int id = 0; id < params.getMaterialCount(); id++) {
			labelListModel.addElement(params.getMaterial(id));
		}
	}


	public AmiraParameters.Material getCurrentMaterial() {
		int selectedIndex = labelList.getSelectedIndex();
		if (selectedIndex == -1)
			return null;
		else {
			return (AmiraParameters.Material) labelListModel.get(selectedIndex);
		}
	}


	private void threshold() {
		LabelThresholder_.min = ((SpinnerNumberModel) minThreshold.getModel()).getNumber().intValue();
		LabelThresholder_.max = ((SpinnerNumberModel) maxThreshold.getModel()).getNumber().intValue();

		IJ.runPlugIn("LabelThresholder_", "");
	}


	public static JList addLabelList(Container c) {
		final DefaultListModel model = new DefaultListModel();
		final JList list = new JList(model);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel("labels..."), BorderLayout.NORTH);

		JPanel controlPanel = new JPanel(new GridLayout(1, 2));
		panel.add(new JScrollPane(list));

		c.add(panel);

		return list;
	}

	private class Controllor implements ActionListener, ImageListener, WindowFocusListener, SliceListener, RoiListener, ListSelectionListener, ChangeListener {

		ImagePlus currentImage;

		public void saveLabelFile() {

			FileInfo info = currentImage.getOriginalFileInfo();
			if( info == null ) {
				IJ.error("There's no original file name associated with this image.");
				return;
			}
			String fileName = info.fileName;
			String directory = info.directory;
			
			String suggestedSaveFilename;

			suggestedSaveFilename = fileName+".labels";

			SaveDialog sd = new SaveDialog("Save label annotation as...",
						       directory,
						       suggestedSaveFilename,
						       ".labels");

			String savePath;
			if(sd.getFileName()==null)
				return;
			else {
				savePath = sd.getDirectory()+sd.getFileName();
			}

			File file = new File(savePath);
			if ((file!=null)&&file.exists()) {
				if (!IJ.showMessageWithCancel(
					    "Save label annotation file...", "The file "+
					    savePath+" already exists.\n"+
					    "Do you want to replace it?"))
					return;
			}

			IJ.showStatus("Saving label annotations to "+savePath);

			AmiraMeshEncoder e=new AmiraMeshEncoder(savePath);
			if(!e.open()) {
				IJ.error("Could not write to "+savePath);
				return;
			}
			
			if(e.write(new SegmentatorModel(currentImage).getLabelImagePlus()))
				IJ.showStatus("Label file saved.");
			else
				IJ.error("Error writing to: "+savePath);

		}
		
		public void actionPerformed(ActionEvent e) {
			//IJ.showMessage(e.getActionCommand());
			
			if (e.getActionCommand().equals(SAVE_LABELS)) {
				saveLabelFile();
			} else if (e.getActionCommand().equals(LABEL_CURRENT)) {
				labelCurrent();
			} else if (e.getActionCommand().equals(THRESHOLD)) {
				threshold();
			} else if (e.getActionCommand().equals(THRESHOLD_UNDO)) {
				LabelThresholder_.rollback();
			} else if (e.getActionCommand().equals(INTERPOLATOR)) {
				IJ.runPlugIn("LabelInterpolator_", "");
			} else if (e.getActionCommand().equals(DILATE)) {
				SegmentatorModel model = new SegmentatorModel(currentImage);
				
				if (model.getCurrentMaterial() != null) {
					LabelBinaryOps.dilate(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							      , currentImage.getRoi(), (byte) model.getCurrentMaterial().id);
					model.updateSlice(currentImage.getCurrentSlice());
				} else {
					IJ.showMessage("please select a label first");
				}
			} else if (e.getActionCommand().equals(ERODE)) {
				SegmentatorModel model = new SegmentatorModel(currentImage);
				System.out.println("eroding");
				if (model.getCurrentMaterial() != null) {
					LabelBinaryOps.erode(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							     , currentImage.getRoi(), (byte) model.getCurrentMaterial().id);
					
					model.updateSlice(currentImage.getCurrentSlice());
				} else {
					IJ.showMessage("please select a label first");
				}
			} else if (e.getActionCommand().equals(CLOSE)) {
				SegmentatorModel model = new SegmentatorModel(currentImage);
				
				if (model.getCurrentMaterial() != null) {
					LabelBinaryOps.close(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							     , currentImage.getRoi(), (byte) model.getCurrentMaterial().id);
					model.updateSlice(currentImage.getCurrentSlice());
				} else {
					IJ.showMessage("please select a label first");
				}
			} else if (e.getActionCommand().equals(OPEN)) {
				SegmentatorModel model = new SegmentatorModel(currentImage);
				
				if (model.getCurrentMaterial() != null) {
					LabelBinaryOps.open(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							    , currentImage.getRoi(), (byte) model.getCurrentMaterial().id);
					model.updateSlice(currentImage.getCurrentSlice());
				} else {
					IJ.showMessage("please select a label first");
				}
			} else if (e.getActionCommand().equals(CLEAN)) {
				SegmentatorModel model = new SegmentatorModel(currentImage);
				
				if (model.getCurrentMaterial() != null) {
					LabelBinaryOps.close(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							     , currentImage.getRoi(), (byte) model.getCurrentMaterial().id);
					LabelBinaryOps.open(model.getLabelImagePlus().getStack().getProcessor(currentImage.getCurrentSlice())
							    , currentImage.getRoi(), (byte) model.getCurrentMaterial().id);
					model.updateSlice(currentImage.getCurrentSlice());
				} else {
					IJ.showMessage("please select a label first");
				}
			}
			
			if (currentImage!=null)
				currentImage.updateAndDraw();
		}
				
		private void loadDefaultMaterials() {			
			populateLabelList(AmiraParameters.defaultMaterials());
		}
				
		public void imageOpened(ImagePlus ip) {
			//when a new image is opened in the environement we need to listen to it gaining foces
			ip.getWindow().addWindowFocusListener(this);
		}
		
		public void imageClosed(ImagePlus ip) {
// when a new image is closed we need to tidy up the listeners
			ip.getWindow().removeWindowFocusListener(this);
		}
		
		public void imageUpdated(ImagePlus ip) {
			//System.out.println("image Updated");
		}
		
		public void windowGainedFocus(WindowEvent e) {
			updateCurrent(IJ.getImage());
		}
		
		
		public void windowLostFocus(WindowEvent e) {
			//clearLabelsList(); removes labels when toolbar is highlighted
		}
		
		private void updateCurrent(ImagePlus newCurrent) {
			
			if (newCurrent == currentImage)
				return;
			
			else {
				
				if (currentImage != null) {
					new SliceWatcher(currentImage).removeSliceListener(this);
					//new RoiWatcher(currentImage).removeRoiListener(this);
				}
				
				if (newCurrent == null) {
					
					markingLabel.setText("current image: <no current image>"); 
					
				} else {
					
					markingLabel.setText("current image: \""+newCurrent.getTitle()+"\"");
					
					// System.out.println("newCurrent = " + newCurrent);
					new SliceWatcher(newCurrent).addSliceListener(this);
					//new RoiWatcher(newCurrent).addRoiListener(this);
					
					SegmentatorModel currentModel = new SegmentatorModel(newCurrent);

					AmiraParameters newMaterialParams = currentModel.getMaterialParams();
					
					//we temporaritly turn of list events while repopulating
					labelList.removeListSelectionListener(this);
					
					if( newMaterialParams == null )
						clearLabelsList();
					else
						populateLabelList(newMaterialParams);
					
					labelList.addListSelectionListener(this);
				}
				currentImage = newCurrent;
			}
			
		}
		
		public void labelCurrent() {
			
			if(currentImage==null) {
				IJ.error("There's no current image.");
				return;
			}

			if(currentImage.getType()!=ImagePlus.GRAY8) {
				IJ.error("You can only label 8 bit images at the moment.");
				return;
			}

			FileInfo info = currentImage.getOriginalFileInfo();
			if( info == null ) {
				IJ.error("There's no original file name associated with this image. Please save it first.");
				return;
			}

			SegmentatorModel currentModel = new SegmentatorModel(currentImage);
			
			if( currentModel.getLabelImagePlus() != null ) {
				IJ.error("The current image ("+currentImage.getTitle()+
					 ") already has a label field associated with it.");
				return;
			}

			ImagePlus labelImage = null;

			{
				String fileName = info.fileName;
				String directory = info.directory;

				File possibleLoadFile = new File(directory,fileName+".labels");

				if(possibleLoadFile.exists()) {

					String[] choices = {"Load corresponding label file","Start with an empty label field"};
					ChoicesDialog dialog=new ChoicesDialog(IJ.getInstance(),
									       "Load default label file?",
									       "There's a label file with the expected corresponding name to this file: '"+
									       possibleLoadFile.getName()+"'.\nDo you want to load it, or start again with an empty label field?",
									       choices);

					if(dialog.optionChosen(0)) {

						AmiraMeshDecoder d=new AmiraMeshDecoder();
						if(d.open(possibleLoadFile.getPath())) {
						
							labelImage=new ImagePlus();
						
							FileInfo fi=new FileInfo();
							fi.fileName=possibleLoadFile.getName();
							fi.directory=possibleLoadFile.getParent();
							labelImage.setFileInfo(fi);
							labelImage.setStack("labels in "+possibleLoadFile.getName(),
									    d.getStack());
							d.parameters.setParameters(labelImage);
						}
						
					}

				}
				
			}
			

			if(labelImage==null) {

				// Then this image hasn't got an associated
				// label field ImagePlus, so create one...
				
				labelImage = IJ.createImage("labels for "+currentImage.getTitle(),
								      "8-bit",
								      currentImage.getWidth(),
								      currentImage.getHeight(),
								      currentImage.getStackSize());
			
				labelImage.setProperty(AmiraParameters.INFO,AmiraParameters.defaultMaterialsString);
			}

			AmiraParameters parameters=new AmiraParameters(labelImage);
			
			//we temporaritly turn of list events while repopulating
			labelList.removeListSelectionListener(this);
			populateLabelList(parameters);
			labelList.addListSelectionListener(this);
			
			labelImage.getProcessor().setColorModel(parameters.getColorModel());
			
			currentModel.setLabelImagePlus(labelImage);
                        
			SegmentationViewerCanvas canvas = new SegmentationViewerCanvas(
				currentImage,
				labelImage);
			
			currentModel.setLabelCanvas(canvas);
			
			if (currentImage.getStackSize() > 1)
				new StackWindow(currentImage, canvas);
			else
				new ImageWindow(currentImage, canvas);
			
			//after a new window is constructed. the old one is
			//cloased and the listener tidied up
			//so we need to make sure we add a new one
			//we do not need to do this for ROIs becuase
			//they work by polling
			new SliceWatcher(currentImage).addSliceListener(this);
			
			IJ.runPlugIn("ij.plugin.LutLoader","grays");
			
			currentImage.getWindow().addWindowFocusListener(this);
			//new RoiWatcher(currentImage).addRoiListener(this);
			
		}
		
		public void sliceNumberChanged(SliceEvent e) {
			LabelThresholder_.commit(); //commit any thesholdings if any
			//System.out.println(e.getSource().getCurrentSlice());
			//drawLabels(currentImage);
			//drawLabels(currentImage, currentLabels, canvas);
		}
		
		public void roiChanged(RoiEvent e) {
			//setLabel(currentImage);
		}
		
		public void valueChanged(ListSelectionEvent e) {
			new SegmentatorModel(currentImage).setCurrentMaterial(getCurrentMaterial());
		}
		
		public void stateChanged(ChangeEvent e) {
			if (e.getSource().equals(minThreshold) || e.getSource().equals(maxThreshold)) {
				//the spinners have changed. We will try a live update
				//if we have an active ROI at the time
				if (currentImage.getRoi() != null) {
					LabelThresholder_.rollback();
					threshold();
				}
			}
		}
	}	
}
